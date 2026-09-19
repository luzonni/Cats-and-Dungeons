import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Põe os 50 arquivos de áudio na mesma régua.
 *
 * ---------------------------------------------------------------------------
 * O PROBLEMA NÃO ERA DOS CONTROLES. ERA DOS ARQUIVOS.
 *
 * A queixa era um laço: subo a música e deixo de ouvir um efeito; subo o efeito
 * e todos os outros estouram. A medição explica por quê, e não tem nada a ver
 * com quantos sliders existem — a dispersão está DENTRO de cada categoria, e um
 * slider move a categoria inteira junto.
 *
 *   Músicas:  fight_boss a -6,9 LUFS contra menu_ambient a -33,8 LUFS.
 *             VINTE E SETE decibéis. Como dez decibéis são o dobro do volume
 *             percebido, o chefe soa seis vezes e meia mais alto que o menu.
 *
 *   Efeitos:  laser a -13,0 dB RMS contra woosh a -43,6 dB RMS.
 *             TRINTA E UM decibéis, ou trinta e quatro vezes a amplitude.
 *
 * Não existe posição certa do slider quando a diferença mora dentro da
 * categoria. Por isso o primeiro conserto é aqui, e não na tela de opções: sem
 * isto, qualquer controle novo continua sendo um controle para um problema que
 * ele não alcança.
 *
 * Havia ainda onze arquivos em 0 dBFS ou acima — fight_boss chega a +2,8 —, isto
 * é, já gravados clipados, sem folga nenhuma para o mixer somar dois sons.
 *
 * ---------------------------------------------------------------------------
 * NORMALIZAR NÃO É ACHATAR
 *
 * Pôr tudo no mesmo número seria trocar um defeito por outro: o passo do gato
 * DEVE ser mais baixo que a explosão. O que estava errado não é haver diferença,
 * é a diferença ser acidental — herdada de quem gravou cada arquivo, e não
 * escolhida.
 *
 * Então são CAMADAS, e a distância entre elas é o que decide o que o jogador
 * percebe primeiro:
 *
 *   FUNDO (-34 dB)     o passo, o ponteiro passando, o ronco. Tocam o tempo
 *                      todo; existem para dar textura e não para serem notados.
 *   ROTINA (-28 dB)    o golpe a golpe: espada, flecha, laser, os impactos
 *                      elementais. É o som que mais se repete numa corrida, e
 *                      seis decibéis acima do fundo é o que o deixa presente sem
 *                      cansar em cinco minutos.
 *   EVENTO (-24 dB)    moeda, portal, grito de bicho. Acontecem de vez em
 *                      quando e carregam informação.
 *   MARCANTE (-20 dB)  levar dano, explosão, conquista. O jogador PRECISA notar,
 *                      e catorze decibéis acima do fundo garantem que note.
 *
 * As músicas seguem a mesma ideia com duas camadas: as trilhas em -18 LUFS e o
 * LEITO AMBIENTE do menu nove decibéis abaixo, porque ele é colchão debaixo da
 * música e não uma faixa — a -33,8 ele não estava abaixo, estava inaudível.
 *
 * ---------------------------------------------------------------------------
 * POR QUE RMS PARA EFEITO E LUFS PARA MÚSICA
 *
 * LUFS é a medida certa de loudness percebida, mas o algoritmo da EBU R128
 * trabalha em blocos de 400 ms com portas de silêncio: num arquivo de 30 ms ele
 * devolve -70, que é o valor de "não medi". Metade dos efeitos daqui é mais
 * curta que um bloco. Para eles o RMS sobre o arquivo inteiro é a medida honesta.
 *
 * NORMALIZAR POR PICO SERIA O ERRO CLÁSSICO, e é mais ou menos o estado atual:
 * pico ignora quanta energia existe atrás dele, então um estalo seco e um acorde
 * sustentado com o mesmo pico soam completamente diferentes. É exatamente por
 * isso que o coin, com pico em +0,7, parece baixo perto do laser.
 *
 * ---------------------------------------------------------------------------
 * RODAR DUAS VEZES NÃO FAZ MAL
 *
 * O alvo é ABSOLUTO, não um ganho relativo: o gerador mede, calcula o quanto
 * falta e aplica. Na segunda passada falta zero, e ele não escreve. Isso importa
 * de verdade para as músicas, que são Vorbis: reescrever um arquivo com perda
 * custa qualidade a cada geração, e a faixa que já está no alvo nem é aberta.
 *
 * É por isso também que ele deve rodar DEPOIS dos outros geradores de áudio
 * (GenVozes, GenSonsDeCena, GenSonsElementais). Se algum deles rodar de novo e
 * devolver um arquivo fora da régua, basta rodar este aqui outra vez.
 *
 * Uso: java tools/GenMixagem.java        (músicas precisam de ffmpeg no PATH)
 */
public class GenMixagem {

    private static final String AUDIO =
            "src/main/resources/com/retronova/resources/audio/";

    /** Teto de pico. A folga extra é para o mixer poder somar dois sons. */
    private static final double TETO_DBFS = -1.5;

    /** Abaixo disto a correção é inaudível e não vale reescrever o arquivo. */
    private static final double MINIMO_WAV = 0.5;

    /** Para Vorbis o limite é maior: reescrever custa uma geração de perda. */
    private static final double MINIMO_OGG = 1.0;

    private static final Map<String, Double> EFEITOS = new LinkedHashMap<>();
    private static final Map<String, Double> MUSICAS = new LinkedHashMap<>();

    static {
        camada(-34, "walking", "hover", "purr");
        camada(-28, "sword", "bowshoot", "laser", "bones", "button", "woosh",
                "hit_fire", "hit_ice", "hit_water", "hit_wind", "hit_earth",
                "hit_thunder");
        camada(-24, "coin", "slime", "zombie", "vampire", "mousesquire", "poison",
                "crack", "portal", "earth", "cat", "cat_muffin", "cat_azrael",
                "cat_finn");
        camada(-20, "damage_cat", "damage_muffin", "damage_azrael", "damage_finn",
                "explosion", "achievement");

        for (String faixa : new String[] {"geral", "menu_principal", "dungeon_hall",
                "fight", "fight_spooky", "fight_8bit", "fight_rpg", "fight_boss",
                "game_over"}) {
            MUSICAS.put(faixa, -18d);
        }
        MUSICAS.put("menu_ambient", -27d);
    }

    private static void camada(double alvo, String... nomes) {
        for (String nome : nomes) {
            EFEITOS.put(nome, alvo);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Mixagem");
        System.out.println("\n  EFEITOS  (RMS alvo, teto " + TETO_DBFS + " dBFS)");
        int mexidos = 0;
        for (Map.Entry<String, Double> e : EFEITOS.entrySet()) {
            if (efeito(e.getKey(), e.getValue())) {
                mexidos++;
            }
        }
        System.out.println("\n  MUSICAS  (LUFS integrado)");
        for (Map.Entry<String, Double> e : MUSICAS.entrySet()) {
            if (musica(e.getKey(), e.getValue())) {
                mexidos++;
            }
        }
        System.out.println("\n  " + mexidos + " arquivo(s) ajustado(s).");
    }

    // ======================================================== efeitos, em WAV

    private static boolean efeito(String nome, double alvo) throws Exception {
        File arquivo = new File(AUDIO + nome + ".wav");
        if (!arquivo.isFile()) {
            System.err.printf("    %-16s FALTANDO%n", nome);
            return false;
        }
        AudioInputStream in = AudioSystem.getAudioInputStream(arquivo);
        AudioFormat fmt = in.getFormat();
        byte[] bruto = in.readAllBytes();
        in.close();

        int bytes = fmt.getSampleSizeInBits() / 8;
        if (bytes != 2 && bytes != 3) {
            System.err.printf("    %-16s %d bits nao suportado%n", nome, bytes * 8);
            return false;
        }
        double[] amostras = ler(bruto, bytes);
        double rms = rms(amostras);
        double pico = pico(amostras);
        if (rms <= 0) {
            System.err.printf("    %-16s silencioso%n", nome);
            return false;
        }

        double ganho = alvo - db(rms);
        // O TETO MANDA NO ALVO, e nao o contrario. Um som muito "de pico" — um
        // estalo, uma moeda — tem pouca energia atras de um pico altissimo; se o
        // alvo de RMS pedisse um ganho que jogasse o pico acima do teto, o que
        // sairia dali seria distorcao, e distorcao e pior do que estar fora da
        // regua. Nesses casos o arquivo fica um pouco abaixo da camada, e esta
        // certo que fique.
        double sobra = TETO_DBFS - db(pico);
        if (ganho > sobra) {
            ganho = sobra;
        }
        // O TETO SOZINHO JA E MOTIVO PARA REESCREVER. Um arquivo pode estar na
        // camada certa e mesmo assim estourar: e o caso de quem foi normalizado
        // por PICO na origem e chegou aqui raspando o zero. Deixar passar por
        // "o loudness ja esta bom" manteria exatamente a falta de folga que faz
        // dois sons somados distorcerem.
        if (Math.abs(ganho) < MINIMO_WAV && db(pico) <= TETO_DBFS) {
            System.out.printf("    %-16s %6.1f dB RMS  ja esta na regua%n", nome, db(rms));
            return false;
        }

        double fator = Math.pow(10, ganho / 20);
        for (int i = 0; i < amostras.length; i++) {
            amostras[i] = Math.max(-1, Math.min(1, amostras[i] * fator));
        }
        gravar(amostras, fmt, bytes, arquivo);
        System.out.printf("    %-16s %6.1f -> %5.1f dB RMS  (%+5.1f dB)%n",
                nome, db(rms), db(rms) + ganho, ganho);
        return true;
    }

    private static double[] ler(byte[] b, int bytes) {
        int n = b.length / bytes;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            int p = i * bytes;
            v[i] = bytes == 2
                    ? (short) ((b[p] & 0xFF) | (b[p + 1] << 8)) / 32768.0
                    : ((b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | (b[p + 2] << 16))
                            / 8388608.0;
        }
        return v;
    }

    private static void gravar(double[] v, AudioFormat fmt, int bytes, File destino)
            throws Exception {
        byte[] saida = new byte[v.length * bytes];
        double escala = bytes == 2 ? 32767 : 8388607;
        for (int i = 0; i < v.length; i++) {
            int s = (int) Math.round(v[i] * escala);
            int p = i * bytes;
            saida[p] = (byte) (s & 0xFF);
            saida[p + 1] = (byte) ((s >> 8) & 0xFF);
            if (bytes == 3) {
                saida[p + 2] = (byte) ((s >> 16) & 0xFF);
            }
        }
        int quadros = v.length / fmt.getChannels();
        try (AudioInputStream out = new AudioInputStream(
                new ByteArrayInputStream(saida), fmt, quadros)) {
            AudioSystem.write(out, AudioFileFormat.Type.WAVE, destino);
        }
    }

    private static double rms(double[] v) {
        double soma = 0;
        for (double x : v) {
            soma += x * x;
        }
        return Math.sqrt(soma / v.length);
    }

    private static double pico(double[] v) {
        double p = 0;
        for (double x : v) {
            p = Math.max(p, Math.abs(x));
        }
        return p;
    }

    private static double db(double amplitude) {
        return amplitude <= 0 ? -120 : 20 * Math.log10(amplitude);
    }

    // ======================================================== musicas, em OGG

    /**
     * Mede com ebur128 e aplica o ganho reencodando no mesmo bitrate.
     *
     * O FFMPEG E EXIGIDO SO AQUI, e nao no resto do gerador. Vorbis nao se
     * decodifica com o que vem no JDK — quem faz isso no jogo e um Service
     * Provider declarado no build, que uma ferramenta solta nao tem. Faltando o
     * ffmpeg, os efeitos sao normalizados do mesmo jeito e as musicas ficam para
     * depois, avisando.
     *
     * REENCODA NO BITRATE ORIGINAL de propósito. Subir a qualidade para
     * "compensar" a geração perdida engordaria o repositório sem devolver o que
     * a primeira compressão já jogou fora.
     */
    private static boolean musica(String nome, double alvo) throws Exception {
        File arquivo = new File(AUDIO + nome + ".ogg");
        if (!arquivo.isFile()) {
            System.err.printf("    %-16s FALTANDO%n", nome);
            return false;
        }
        Double lufs = medirLufs(arquivo);
        if (lufs == null) {
            System.out.printf("    %-16s sem ffmpeg, pulando%n", nome);
            return false;
        }
        double ganho = alvo - lufs;
        double pico = medirPico(arquivo);
        if (Math.abs(ganho) < MINIMO_OGG && pico <= TETO_DBFS) {
            System.out.printf("    %-16s %6.1f LUFS  ja esta na regua%n", nome, lufs);
            return false;
        }
        int bitrate = bitrate(arquivo);
        File temporario = new File(AUDIO + nome + ".mix.ogg");
        int codigo = rodar("ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error",
                "-y", "-i", arquivo.getPath(),
                // O level=disabled NAO E OPCIONAL. O alimiter do ffmpeg vem com
                // auto-nivelamento LIGADO, e auto-nivelar significa empurrar a saida
                // ate o teto — o que desfaz o ganho que acabamos de calcular e
                // devolve toda faixa a -1,5 dBFS, alta de novo e todas iguais. Era o
                // que fazia o gerador nunca convergir: cada passada tirava mais 1,5
                // dB e a seguinte reclamava do mesmo jeito. Aqui o limiter serve so
                // de rede contra pico, que e o unico papel que ele deveria ter.
                "-af", String.format(java.util.Locale.ROOT,
                        "volume=%.2fdB,alimiter=limit=%.4f:level=disabled", ganho,
                        Math.pow(10, TETO_DBFS / 20)),
                "-c:a", "libvorbis", "-b:a", bitrate + "", temporario.getPath());
        if (codigo != 0 || !temporario.isFile()) {
            System.err.printf("    %-16s ffmpeg falhou (%d)%n", nome, codigo);
            temporario.delete();
            return false;
        }
        java.nio.file.Files.move(temporario.toPath(), arquivo.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.printf("    %-16s %6.1f -> %5.1f LUFS  (%+5.1f dB)%n",
                nome, lufs, lufs + ganho, ganho);
        return true;
    }

    /**
     * O pico DE AMOSTRA da faixa, para o teto valer mesmo com o loudness no alvo.
     *
     * DE AMOSTRA, E NAO TRUE PEAK, e a distincao nao e preciosismo. O true peak
     * reconstroi o sinal ENTRE as amostras e por isso mede mais alto — costuma dar
     * uns dois decimos de decibel a mais. O alimiter, que e quem aplica o teto,
     * trabalha em amostras. Medindo com uma regua e cortando com a outra, uma faixa
     * pode ficar eternamente "acima do teto" logo depois de ter sido limitada, e o
     * gerador reescreveria o Vorbis a cada execucao — uma geracao de perda por
     * rodada, para sempre. Medir com a regua de quem corta e o que garante parar.
     */
    private static double medirPico(File arquivo) throws Exception {
        String saida = ffmpeg(arquivo, "astats");
        if (saida == null) {
            return -120;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Peak level dB:\\s*(-?\\d+\\.?\\d*)").matcher(saida);
        double ultimo = -120;
        while (m.find()) {
            ultimo = Double.parseDouble(m.group(1));
        }
        return ultimo;
    }

    private static Double medirLufs(File arquivo) throws Exception {
        String saida = ffmpeg(arquivo, "ebur128");
        if (saida == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("I:\\s*(-?\\d+\\.?\\d*) LUFS").matcher(saida);
        Double ultimo = null;
        while (m.find()) {
            ultimo = Double.parseDouble(m.group(1));
        }
        return ultimo;
    }

    /** Roda um filtro de analise e devolve o relatorio, ou null sem ffmpeg. */
    private static String ffmpeg(File arquivo, String filtro) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-nostdin", "-hide_banner",
                "-i", arquivo.getPath(), "-af", filtro, "-f", "null", "-");
        pb.redirectErrorStream(true);
        Process p;
        try {
            p = pb.start();
        } catch (java.io.IOException semFfmpeg) {
            return null;
        }
        String saida = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        return saida;
    }

    private static int bitrate(File arquivo) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "error",
                "-show_entries", "format=bit_rate", "-of", "csv=p=0", arquivo.getPath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String saida = new String(p.getInputStream().readAllBytes()).trim();
        p.waitFor();
        try {
            return Integer.parseInt(saida);
        } catch (NumberFormatException e) {
            return 128000;
        }
    }

    private static int rodar(String... comando) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String saida = new String(p.getInputStream().readAllBytes());
        int codigo = p.waitFor();
        if (codigo != 0) {
            System.err.println(saida);
        }
        return codigo;
    }
}
