import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Corta os sons elementais e mecânicos em versões de IMPACTO.
 *
 * ---------------------------------------------------------------------------
 * O PROBLEMA NÃO ERA VOLUME, ERA DURAÇÃO.
 *
 * Os arquivos originais — fire, ice, water, wind, earth, thunder — estavam em
 * resources sem uso desde sempre, e quando foram ligados ao golpe o resultado foi
 * insuportável. A medição desfaz a suspeita óbvia: em pico eles são mais QUIETOS
 * que a espada (fogo a -13 dB contra -8,6 dB). O que os torna intoleráveis é que
 * duram de dois a quase três segundos.
 *
 * A espada golpeia a cada 0,3 s. Um som de dois segundos disparado a cada 0,3 s
 * põe SETE cópias tocando ao mesmo tempo, cada uma somando à anterior — e som
 * somado sobe seis decibéis a cada dobra. Não é um efeito alto; é o mesmo efeito
 * empilhado sete vezes.
 *
 * Isso diz o que eles são: ambiência, feita para tocar sozinha e em loop, e não
 * retorno de golpe. Um som de impacto tem transiente e acaba.
 *
 * ---------------------------------------------------------------------------
 * O QUE ESTE GERADOR FAZ
 *
 * Recorta os primeiros 350 ms A PARTIR DO ATAQUE — não do começo do arquivo, que
 * pode ter silêncio antes — e fecha com uma queda suave. Trezentos e cinquenta
 * milissegundos limitam a cauda; Sound/LimiteDeVozes limita a sobreposição.
 *
 * O nível alvo é -28 dB RMS, a camada de rotina de GenMixagem. O começo precisa
 * ter ataque de verdade: cruzar o limiar de silêncio não basta num arquivo que
 * cresce lentamente. O gerador procura a primeira frente forte no trecho e
 * preserva uma margem de 2 ms, com abertura e queda suaves para não estalar.
 * As armas elementais usam o som do elemento; as comuns usam o recorte mecânico.
 *
 * Os originais ficam onde estão. Eles continuam bons para o que foram feitos — se
 * um dia houver ambiência de sala elemental, estão prontos.
 *
 * Uso: java tools/GenSonsElementais.java
 */
public class GenSonsElementais {

    private static final String AUDIO =
            "src/main/resources/com/retronova/resources/audio/";

    /** Quanto do original vira impacto. */
    private static final double DURACAO = 0.35;
    private static final double DURACAO_LAMINA = 0.22, DURACAO_MAGIA = 0.20,
            DURACAO_ARREMESSO = 0.18, DURACAO_IMPACTO = 0.22, DURACAO_AURA = 0.20;

    /** A mesma camada de rotina de GenMixagem, sem precisar de ffmpeg. */
    private static final double RMS_ALVO = -28;
    private static final double TETO_PICO = -1.5;
    /** Primeiro ataque forte, com apenas 2 ms de margem antes dele. */
    private static final double FRACAO_DO_ATAQUE = 0.5, MARGEM_ATAQUE = 0.002;

    /** Acima disto o som já começou. Serve para pular silêncio inicial. */
    private static final double LIMIAR = 0.02;

    private static final String[] ELEMENTOS = {
            "fire", "ice", "water", "wind", "earth", "thunder"};

    public static void main(String[] args) throws Exception {
        System.out.println("Impactos elementais");
        for (String nome : ELEMENTOS) {
            gerar(nome, "hit_" + nome, DURACAO);
        }
        // RECORTES DE ARMA: os originais também servem a cenas e inimigos.
        // Preservá-los evita encurtar esses eventos ao corrigir o tempo do golpe.
        gerar("sword", "weapon_blade", DURACAO_LAMINA);
        gerar("laser", "weapon_magic", DURACAO_MAGIA);
        gerar("woosh", "weapon_throw", DURACAO_ARREMESSO);
        gerar("crack", "weapon_impact", DURACAO_IMPACTO);
        gerar("poison", "weapon_aura", DURACAO_AURA);
        gerar("explosion", "weapon_explosion", DURACAO);
    }

    private static void gerar(String nome, String destino, double duracao) throws Exception {
        File origem = new File(AUDIO + nome + ".wav");
        if (!origem.exists()) {
            System.err.println("  faltando: " + nome + ".wav");
            throw new IllegalStateException("Fonte de áudio ausente: " + origem);
        }
        AudioInputStream in = AudioSystem.getAudioInputStream(origem);
        AudioFormat fmt = in.getFormat();
        byte[] bruto = in.readAllBytes();
        in.close();

        double[] mono = paraMono(bruto, fmt);
        double[] impacto = recortar(mono, fmt.getSampleRate(), duracao);
        normalizar(impacto, RMS_ALVO);

        // SAI EM 16 BITS MONO. Os originais sao 24 bits estereo, que e formato
        // de biblioteca de ambiencia; para um efeito de trezentos milissegundos
        // isso e o triplo de bytes por nada, e o resto do arsenal do jogo ja e
        // 16 bits.
        AudioFormat saida = new AudioFormat(fmt.getSampleRate(), 16, 1, true, false);
        gravar(impacto, saida, new File(AUDIO + destino + ".wav"));
        System.out.printf("  %-22s %.2fs -> %.2fs%n", destino + ".wav",
                (bruto.length / (double) fmt.getFrameSize()) / fmt.getSampleRate(),
                impacto.length / fmt.getSampleRate());
    }

    private static double[] paraMono(byte[] b, AudioFormat fmt) {
        int bytes = fmt.getSampleSizeInBits() / 8;
        int canais = fmt.getChannels();
        int quadros = b.length / (bytes * canais);
        double[] v = new double[quadros];
        for (int q = 0; q < quadros; q++) {
            double soma = 0;
            for (int c = 0; c < canais; c++) {
                int i = (q * canais + c) * bytes;
                soma += bytes == 2
                        ? (short) ((b[i] & 0xFF) | (b[i + 1] << 8)) / 32768.0
                        : ((b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8) | (b[i + 2] << 16))
                                / 8388608.0;
            }
            v[q] = soma / canais;
        }
        return v;
    }

    /**
     * Pega o trecho a partir de onde o som de fato comeca.
     *
     * COMECAR NO ARQUIVO E NAO NO ATAQUE seria arriscar um impacto que comeca com
     * silencio — e num efeito de trezentos milissegundos, cem de silencio sao um
     * terco do som jogado fora, e o golpe soaria atrasado em relacao ao que se ve.
     */
    private static double[] recortar(double[] fonte, float taxa, double duracao) {
        int inicio = 0;
        while (inicio < fonte.length && Math.abs(fonte[inicio]) < LIMIAR) {
            inicio++;
        }
        if (inicio >= fonte.length) {
            inicio = 0;
        }
        // SILÊNCIO NÃO É O ÚNICO ATRASO: sword e hit_earth já tinham amostras
        // baixas no começo, mas o ataque percebido vinha 180–200 ms depois.
        // Procura a primeira frente forte dentro do trecho, antes de recortar.
        int fimDaBusca = Math.min(fonte.length, inicio + (int) (DURACAO * taxa));
        double picoDoTrecho = 0;
        for (int i = inicio; i < fimDaBusca; i++) {
            picoDoTrecho = Math.max(picoDoTrecho, Math.abs(fonte[i]));
        }
        while (inicio < fimDaBusca && Math.abs(fonte[inicio]) < picoDoTrecho * FRACAO_DO_ATAQUE) {
            inicio++;
        }
        inicio = Math.max(0, inicio - (int) (MARGEM_ATAQUE * taxa));
        int quantos = Math.min((int) (duracao * taxa), fonte.length - inicio);
        double[] saida = new double[quantos];
        System.arraycopy(fonte, inicio, saida, 0, quantos);
        // A QUEDA E LONGA, um terco do trecho. Cortar seco criaria um clique, e
        // clique e o unico defeito de audio que se ouve melhor que o proprio som.
        int queda = Math.max(1, quantos / 3);
        for (int i = 0; i < queda; i++) {
            saida[quantos - 1 - i] *= i / (double) queda;
        }
        // E uma abertura curtissima, pelo mesmo motivo do outro lado.
        int abertura = Math.max(1, (int) (0.003 * taxa));
        for (int i = 0; i < abertura && i < quantos; i++) {
            saida[i] *= i / (double) abertura;
        }
        return saida;
    }

    private static void normalizar(double[] v, double alvoDbfs) {
        double pico = 0;
        double energia = 0;
        for (double x : v) {
            pico = Math.max(pico, Math.abs(x));
            energia += x * x;
        }
        if (pico == 0) {
            return;
        }
        double ganho = Math.min(Math.pow(10, alvoDbfs / 20.0) / Math.sqrt(energia / v.length),
                Math.pow(10, TETO_PICO / 20.0) / pico);
        for (int i = 0; i < v.length; i++) {
            v[i] *= ganho;
        }
    }

    private static void gravar(double[] v, AudioFormat fmt, File destino) throws Exception {
        byte[] bytes = new byte[v.length * 2];
        for (int i = 0; i < v.length; i++) {
            int s = (int) Math.round(v[i] * 32767);
            s = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, s));
            bytes[i * 2] = (byte) (s & 0xFF);
            bytes[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        try (AudioInputStream out = new AudioInputStream(
                new ByteArrayInputStream(bytes), fmt, v.length)) {
            AudioSystem.write(out, AudioFileFormat.Type.WAVE, destino);
        }
    }
}
