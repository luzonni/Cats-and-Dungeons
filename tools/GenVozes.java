import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Da uma voz propria a cada gato, a partir do miado que ja existe.
 *
 * O PROBLEMA. Os tres jogaveis miavam com o mesmo arquivo, entao trocar de gato
 * na tela de selecao nao soava como trocar de personagem — soava como apertar o
 * mesmo botao tres vezes. Voz e a primeira coisa que separa um bicho de outro
 * antes de qualquer texto ser lido.
 *
 * COMO. Reamostragem simples do cat.wav, que e a tecnica de sempre para tirar
 * varias vozes de uma gravacao so: ler a mesma onda mais devagar abaixa o tom e
 * alonga, ler mais depressa levanta e encurta. Aqui isso e uma vantagem e nao um
 * efeito colateral — gato grande mia grave e arrastado, gato pequeno mia agudo e
 * curto, entao tom e duracao andando juntos e exatamente o que se quer. Por isso
 * nao ha correcao de tempo nenhuma.
 *
 * As tres razoes abrem cerca de dez semitons entre a voz mais grave e a mais
 * aguda. Menos que isso e o ouvido le como o mesmo gato com ruido.
 *
 *   Muffin   0,92x   um tom abaixo, um pouco mais longo — a voz media do trio
 *   Azrael   0,74x   cinco semitons abaixo e arrastado — o gato de coroa
 *   Finn     1,30x   quatro semitons e meio acima e curto — o gato da sorte
 *
 * Todos saem no mesmo pico do original, -11 dBFS, para nenhum gato gritar mais
 * alto que os irmaos so por ter sido processado.
 *
 * Uso: java tools/GenVozes.java
 */
public class GenVozes {

    private static final String AUDIO =
            "src/main/resources/com/retronova/resources/audio/";

    /**
     * Pico alvo. E o pico da PROPRIA origem, medido na hora.
     *
     * Fixar -11 dBFS servia enquanto so o cat.wav era processado. Com o gemido
     * de dano entrando na mesma esteira, um numero fixo levantaria ou abaixaria
     * uma familia inteira em relacao ao resto do jogo — o gemido tem de ficar no
     * volume em que ja estava, so com o tom de cada gato.
     */
    private static double picoDbfs(double[][] v) {
        double pico = 0;
        for (double[] canal : v) {
            for (double x : canal) {
                pico = Math.max(pico, Math.abs(x));
            }
        }
        return pico <= 0 ? -11 : 20 * Math.log10(pico);
    }

    record Voz(String sufixo, double razao) { }

    /**
     * As tres vozes. As mesmas razoes valem para TODO som de gato.
     *
     * Se o miado do Azrael fosse grave mas o gemido dele saisse na altura do
     * Finn, seriam dois bichos: o que amarra a identidade e a razao ser a mesma
     * em todas as falas do personagem, e nao cada som ter o proprio tempero.
     */
    private static final Voz[] VOZES = {
            new Voz("muffin", 0.92),
            new Voz("azrael", 0.74),
            new Voz("finn", 1.30),
    };

    /**
     * De que arquivo sai cada familia de sons, e com que nome ela e gravada.
     *
     * O miado veio primeiro; o gemido de dano entrou depois pelo mesmo motivo, e
     * e o som que mais se ouve numa corrida — levar pancada acontece muito mais
     * que escolher personagem. Deixar os tres gemendo igual era desperdicar a
     * unica hora em que a voz do gato aparece durante o jogo.
     */
    record Familia(String origem, String prefixo) { }

    private static final Familia[] FAMILIAS = {
            new Familia("cat.wav", "cat_"),
            new Familia("damage_cat.wav", "damage_"),
    };

    public static void main(String[] a) throws Exception {
        for (Familia f : FAMILIAS) {
            gerar(f);
        }
    }

    private static void gerar(Familia familia) throws Exception {
        File origem = new File(AUDIO + familia.origem());
        AudioInputStream in = AudioSystem.getAudioInputStream(origem);
        AudioFormat fmt = in.getFormat();
        if (fmt.getSampleSizeInBits() != 16 || fmt.isBigEndian()) {
            System.err.println("Esperado WAV 16 bits little-endian.");
            System.exit(1);
        }
        byte[] bruto = in.readAllBytes();
        in.close();

        int canais = fmt.getChannels();
        int quadros = bruto.length / (2 * canais);
        double[][] fonte = new double[canais][quadros];
        for (int q = 0; q < quadros; q++) {
            for (int c = 0; c < canais; c++) {
                int i = (q * canais + c) * 2;
                fonte[c][q] = (short) ((bruto[i] & 0xFF) | (bruto[i + 1] << 8)) / 32768.0;
            }
        }

        double alvo = picoDbfs(fonte);
        for (Voz v : VOZES) {
            double[][] saida = reamostrar(fonte, v.razao());
            normalizar(saida, alvo);
            String nome = familia.prefixo() + v.sufixo() + ".wav";
            gravar(saida, fmt, new File(AUDIO + nome));
            System.out.printf("  %-20s %.2fx  %+.1f semitons  %.2f s%n",
                    nome, v.razao(),
                    12 * Math.log(v.razao()) / Math.log(2),
                    saida[0].length / fmt.getSampleRate());
        }
    }

    /**
     * Le a onda numa velocidade diferente, com interpolacao linear.
     *
     * Linear e nao algo melhor de proposito: o material e um miado de menos de um
     * segundo deslocado no maximo cinco semitons, e nessa faixa o alias que a
     * interpolacao linear deixa passar fica abaixo do ruido da propria gravacao.
     */
    static double[][] reamostrar(double[][] fonte, double razao) {
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

    static void normalizar(double[][] v, double alvoDbfs) {
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

    static void gravar(double[][] v, AudioFormat fmt, File destino) throws Exception {
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
