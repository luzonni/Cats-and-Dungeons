import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Estica o woosh até ele caber no meio de uma briga.
 *
 * O QUE ESTAVA ERRADO, E O QUE NÃO ESTAVA.
 *
 * O encanamento do som do portal sempre esteve certo — a instrumentação mostrou
 * cinquenta e três disparos, sem exceção, com panorâmica em faixa válida. O que
 * falhava era o ARQUIVO: o woosh.wav é um sopro de meio segundo, quase todo em
 * agudo chiado, e meio segundo de chiado é exatamente o material que desaparece
 * debaixo de espada, passo e guincho de rato. Ele não estava sendo engolido por
 * um bug; estava sendo mascarado.
 *
 * A resposta anterior — trocar pelo crack — resolveu o volume e perdeu o som. O
 * estalo corta a briga, mas é cascalho: descreve pedra quebrando, não uma
 * passagem se abrindo. Este arquivo faz o contrário: mantém o woosh e dá a ele as
 * três coisas que faltavam para ele ser ouvido.
 *
 * AS TRÊS CAMADAS, e por que cada uma existe:
 *
 *   1. O ORIGINAL, intacto e por cima. É o ataque, e é o que faz o som continuar
 *      sendo reconhecível como aquele woosh. Nada aqui o substitui.
 *
 *   2. UMA CÓPIA GRAVE, lida a 0,55x — quase uma oitava abaixo e quase o dobro de
 *      comprimento. Ela é o corpo. O combate do jogo é denso em médio e agudo
 *      (metal, guincho, passo), e é justamente lá embaixo que sobra espaço no
 *      espectro: um som grave se ouve num ambiente cheio sem precisar ser mais
 *      alto que ele, porque não disputa faixa com ninguém.
 *
 *   3. UMA CAUDA, três repetições atrasadas e cada vez mais fracas da camada
 *      grave. É o que estica o som de meio segundo para cerca de um e meio. A
 *      duração é o terceiro fator de audibilidade e o mais esquecido: um evento
 *      curto pode passar inteiro dentro do intervalo em que o ouvido está ocupado
 *      com outro; um evento longo não tem como.
 *
 * O ganho final também sobe, mas ele é o menor dos três ajustes — subir volume
 * sem mexer em faixa nem em duração é o que já não tinha funcionado.
 *
 * Uso: java tools/GenSomDoPortal.java
 */
public class GenSomDoPortal {

    private static final String AUDIO =
            "src/main/resources/com/retronova/resources/audio/";

    /** De onde sai, e como fica chamado. */
    private static final String ORIGEM = "woosh.wav";
    private static final String DESTINO = "portal.wav";

    /**
     * Quanto a camada de corpo é lida mais devagar.
     *
     * 0,55x é dez semitons e meio abaixo — perto de uma oitava. Menos que isso e
     * ela ainda disputa faixa com o original em vez de sustentá-lo; muito mais e o
     * sopro vira ronco e deixa de ser o mesmo som.
     */
    private static final double RAZAO_DO_CORPO = 0.55;

    /** O quanto a camada grave entra, em relação ao original. */
    private static final double GANHO_DO_CORPO = 0.75;

    /** Atraso entre as repetições da cauda, em segundos. */
    private static final double ATRASO = 0.11;

    /** Quanto cada repetição perde em relação à anterior. */
    private static final double DECAIMENTO = 0.55;

    /** Quantas repetições. Três levam a cauda a pouco mais de um segundo. */
    private static final int REPETICOES = 3;

    /**
     * Pico final, em dBFS.
     *
     * -4 e não -11, que é onde as vozes dos gatos ficam. Aquele valor existe para
     * o miado não gritar mais alto que o resto do jogo numa tela de menu silenciosa;
     * aqui o problema é o oposto — este som só toca durante o combate, e concorre
     * com tudo o que está tocando junto.
     */
    private static final double PICO_ALVO = -4;

    public static void main(String[] args) throws Exception {
        File origem = new File(AUDIO + ORIGEM);
        AudioInputStream in = AudioSystem.getAudioInputStream(origem);
        AudioFormat fmt = in.getFormat();
        if (fmt.getSampleSizeInBits() != 16 || fmt.isBigEndian()) {
            System.err.println("Esperado WAV 16 bits little-endian.");
            System.exit(1);
        }
        byte[] bruto = in.readAllBytes();
        in.close();

        double[][] fonte = ler(bruto, fmt.getChannels());
        System.out.println("Som do portal");
        medir("  origem  " + ORIGEM, fonte, fmt.getSampleRate());
        medir("  vizinho crack.wav", ler(bytesDe(AUDIO + "crack.wav"), fmt.getChannels()),
                fmt.getSampleRate());

        double[][] corpo = reamostrar(fonte, RAZAO_DO_CORPO);
        int atrasoEmQuadros = (int) (ATRASO * fmt.getSampleRate());
        int canais = fonte.length;
        int comprimento = Math.max(fonte[0].length,
                corpo[0].length + atrasoEmQuadros * REPETICOES);

        double[][] saida = new double[canais][comprimento];
        somar(saida, fonte, 0, 1.0);
        somar(saida, corpo, 0, GANHO_DO_CORPO);
        double ganho = GANHO_DO_CORPO * DECAIMENTO;
        for (int r = 1; r <= REPETICOES; r++) {
            somar(saida, corpo, atrasoEmQuadros * r, ganho);
            ganho *= DECAIMENTO;
        }

        // A ABERTURA E O FECHO existem para o alto-falante, não para o ouvido: um
        // salto de amplitude no primeiro ou no último quadro vira um clique, e um
        // clique é justamente o defeito que se ouve melhor que o som inteiro.
        rampa(saida, (int) (0.004 * fmt.getSampleRate()), true);
        rampa(saida, (int) (0.18 * fmt.getSampleRate()), false);
        normalizar(saida, PICO_ALVO);

        gravar(saida, fmt, new File(AUDIO + DESTINO));
        medir("  saida   " + DESTINO, saida, fmt.getSampleRate());
    }

    private static byte[] bytesDe(String caminho) throws Exception {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new File(caminho))) {
            return in.readAllBytes();
        }
    }

    private static double[][] ler(byte[] bruto, int canais) {
        int quadros = bruto.length / (2 * canais);
        double[][] v = new double[canais][quadros];
        for (int q = 0; q < quadros; q++) {
            for (int c = 0; c < canais; c++) {
                int i = (q * canais + c) * 2;
                v[c][q] = (short) ((bruto[i] & 0xFF) | (bruto[i + 1] << 8)) / 32768.0;
            }
        }
        return v;
    }

    /**
     * Pico, energia média e duração.
     *
     * OS TRÊS JUNTOS, porque sozinho nenhum deles explica ser ouvido ou não. O pico
     * diz o quanto o som chega a subir; a energia média diz o quanto dele existe de
     * fato — é ela que separa um estalo de um sopro com o mesmo pico; a duração diz
     * por quanto tempo ele tem chance de aparecer numa brecha do combate.
     */
    private static void medir(String rotulo, double[][] v, float taxa) {
        double pico = 0;
        double soma = 0;
        int n = 0;
        for (double[] canal : v) {
            for (double x : canal) {
                pico = Math.max(pico, Math.abs(x));
                soma += x * x;
                n++;
            }
        }
        double rms = Math.sqrt(soma / Math.max(1, n));
        System.out.printf("%-28s pico %6.1f dBFS   medio %6.1f dBFS   %.2f s%n",
                rotulo, db(pico), db(rms), v[0].length / taxa);
    }

    private static double db(double x) {
        return x <= 0 ? -99 : 20 * Math.log10(x);
    }

    private static void somar(double[][] destino, double[][] fonte, int em, double ganho) {
        for (int c = 0; c < destino.length; c++) {
            double[] dc = destino[c];
            double[] fc = fonte[Math.min(c, fonte.length - 1)];
            for (int i = 0; i < fc.length; i++) {
                int j = i + em;
                if (j >= dc.length) {
                    break;
                }
                dc[j] += fc[i] * ganho;
            }
        }
    }

    /** Abre (ou fecha) o som ao longo de n quadros. */
    private static void rampa(double[][] v, int n, boolean abrindo) {
        if (n <= 0) {
            return;
        }
        for (double[] canal : v) {
            for (int i = 0; i < n && i < canal.length; i++) {
                double f = i / (double) n;
                if (abrindo) {
                    canal[i] *= f;
                } else {
                    canal[canal.length - 1 - i] *= f;
                }
            }
        }
    }

    /** Mesma leitura em outra velocidade, com interpolação linear. Ver GenVozes. */
    private static double[][] reamostrar(double[][] fonte, double razao) {
        int canais = fonte.length;
        int n = (int) (fonte[0].length / razao);
        double[][] o = new double[canais][n];
        for (int c = 0; c < canais; c++) {
            for (int i = 0; i < n; i++) {
                double pos = i * razao;
                int p = (int) pos;
                double f = pos - p;
                double a = fonte[c][Math.min(p, fonte[c].length - 1)];
                double b = fonte[c][Math.min(p + 1, fonte[c].length - 1)];
                o[c][i] = a + (b - a) * f;
            }
        }
        return o;
    }

    private static void normalizar(double[][] v, double alvoDbfs) {
        double pico = 0;
        for (double[] canal : v) {
            for (double s : canal) {
                pico = Math.max(pico, Math.abs(s));
            }
        }
        if (pico == 0) {
            return;
        }
        double ganho = Math.pow(10, alvoDbfs / 20.0) / pico;
        for (double[] canal : v) {
            for (int i = 0; i < canal.length; i++) {
                canal[i] *= ganho;
            }
        }
    }

    private static void gravar(double[][] v, AudioFormat fmt, File destino)
            throws Exception {
        int canais = v.length, n = v[0].length;
        byte[] bytes = new byte[n * canais * 2];
        for (int q = 0; q < n; q++) {
            for (int c = 0; c < canais; c++) {
                int s = (int) Math.round(v[c][q] * 32767);
                s = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, s));
                int i = (q * canais + c) * 2;
                bytes[i] = (byte) (s & 0xFF);
                bytes[i + 1] = (byte) ((s >> 8) & 0xFF);
            }
        }
        try (AudioInputStream out = new AudioInputStream(
                new ByteArrayInputStream(bytes), fmt, n)) {
            AudioSystem.write(out, AudioFileFormat.Type.WAVE, destino);
        }
    }
}
