import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

/**
 * Gera o áudio da abertura do baú (AberturaDoBau). Tudo sintetizado aqui — sem
 * arquivo de fora, sem licença a conferir.
 *
 * ---------------------------------------------------------------------------
 * O QUE CADA UM FAZ NO MOMENTO
 *
 *   bau_tremor.wav          o baú CHACOALHA antes de abrir: batidas curtas de
 *                           madeira com um tilintar de ferragem. É a antecipação
 *                           — dura o mesmo que o tremor da tela.
 *   bau_abre.wav            a TAMPA: um rangido curto de dobradiça, o baque da
 *                           tampa batendo e a lufada que sai de dentro.
 *   bau_premio_<grau>.wav   a REVELAÇÃO, um por grau, em escada: o comum é um sino
 *                           só; o raro, duas notas; o épico, um arpejo de três com
 *                           brilho; o lendário, quatro notas, brilho mais longo e
 *                           um grave por baixo. A escada é a mesma do brilho da
 *                           raridade (Raridade.brilho): o ouvido sabe o grau antes
 *                           de o olho ler o nome, como a cor do baú no Enter the
 *                           Gungeon diz a qualidade antes de abrir.
 *
 * ---------------------------------------------------------------------------
 * NÍVEIS: a régua do jogo (ver docs/ARQUITETURA.md, Áudio). Abertura e revelação
 * são eventos marcantes, -20 dB RMS; o tremor é antecipação, -24 dB RMS, para não
 * roubar o golpe da tampa. Teto de pico de -1,5 dBFS. Tocam pelo Sound, que
 * aplica Master e Sound effects das configurações.
 *
 * SÃO PROVISÓRIOS NO TIMBRE, não no papel: senoides e ruído cumprem a função e
 * casam com o chiptune das trilhas, mas quem faz som faria melhor. Os nomes dos
 * arquivos e os pontos onde tocam ficam iguais — basta trocar os arquivos.
 *
 *     java tools/GenSonsDoBau.java
 */
public class GenSonsDoBau {

    private static final String AUDIO = "src/main/resources/com/retronova/resources/audio/";
    private static final float TAXA = 44100f;
    private static final double TETO_DBFS = -1.5;
    private static final Random R = new Random(0xBA0);

    public static void main(String[] args) throws Exception {
        gravar(normalizado(tremor(), -24), "bau_tremor");
        gravar(normalizado(abre(), -20), "bau_abre");
        String[] graus = {"comum", "raro", "epico", "lendario"};
        for (int g = 0; g < graus.length; g++) {
            gravar(normalizado(premio(g), -20), "bau_premio_" + graus[g]);
        }
    }

    // ------------------------------------------------------------ os sons

    /** 0,3 s de batidas de madeira em ritmo irregular, com tilintar de ferro. */
    private static double[] tremor() {
        double[] v = new double[amostras(0.30)];
        double t = 0;
        int k = 0;
        while (t < 0.27) {
            int i = (int) (t * TAXA);
            // Batida de madeira: ruído curto, grave, com um corpo em 180 Hz.
            for (int j = 0; j < amostras(0.035) && i + j < v.length; j++) {
                double tt = j / TAXA;
                double env = Math.exp(-tt * 120);
                v[i + j] += env * (0.6 * (R.nextDouble() * 2 - 1) * 0.5 + 0.8 * Math.sin(2 * Math.PI * 180 * tt));
            }
            // Ferragem: um tilintar agudo, uma batida sim, outra não.
            if (k % 2 == 0) {
                for (int j = 0; j < amostras(0.05) && i + j < v.length; j++) {
                    double tt = j / TAXA;
                    v[i + j] += 0.25 * Math.exp(-tt * 60) * Math.sin(2 * Math.PI * 2400 * tt);
                }
            }
            t += 0.045 + R.nextDouble() * 0.02;
            k++;
        }
        return passaBaixa(v, 0.35);
    }

    /** Rangido, baque e lufada: 0,5 s. */
    private static double[] abre() {
        double[] v = new double[amostras(0.50)];
        // Rangido: uma serra de 90 a 140 Hz modulada, nos primeiros 0,12 s.
        double fase = 0;
        for (int i = 0; i < amostras(0.12); i++) {
            double t = i / TAXA;
            double f = 90 + 50 * (t / 0.12) + 12 * Math.sin(2 * Math.PI * 23 * t);
            fase += f / TAXA;
            double serra = 2 * (fase - Math.floor(fase + 0.5));
            v[i] += 0.35 * serra * Math.sin(Math.PI * t / 0.12);
        }
        // Baque: a tampa batendo, em 0,12 s.
        int baque = amostras(0.12);
        for (int j = 0; j < amostras(0.2) && baque + j < v.length; j++) {
            double t = j / TAXA;
            double f = 110 * Math.exp(-t * 8) + 50;
            v[baque + j] += Math.exp(-t * 22) * Math.sin(2 * Math.PI * f * t);
            v[baque + j] += 0.3 * Math.exp(-t * 60) * (R.nextDouble() * 2 - 1);
        }
        // Lufada: ruído que abre e some, de 0,12 a 0,5 s.
        double[] ruido = new double[v.length];
        for (int i = baque; i < v.length; i++) {
            double t = (i - baque) / TAXA;
            double env = Math.min(1, t / 0.05) * Math.exp(-t * 7);
            ruido[i] = 0.35 * env * (R.nextDouble() * 2 - 1);
        }
        ruido = passaBaixa(ruido, 0.12);
        for (int i = 0; i < v.length; i++) {
            v[i] += ruido[i];
        }
        return v;
    }

    /**
     * A revelação de um grau, de 0 (comum) a 3 (lendário): notas de sino em
     * arpejo maior, mais notas e mais brilho a cada grau.
     */
    private static double[] premio(int grau) {
        double[] notas = {659.25, 830.61, 987.77, 1318.51};    // Mi maior: mi, sol#, si, mi
        int quantas = grau + 1;
        double passo = 0.07;
        double dur = 0.55 + 0.15 * grau;
        double[] v = new double[amostras(dur + passo * quantas)];
        for (int n = 0; n < quantas; n++) {
            int inicio = amostras(n * passo);
            sino(v, inicio, notas[n], 0.9, 0.4 + 0.1 * grau);
        }
        // Brilho: um chiado agudo cintilante, do raro para cima.
        if (grau >= 1) {
            for (int i = 0; i < v.length; i++) {
                double t = i / TAXA;
                double env = Math.exp(-t * (6 - grau)) * (0.04 + 0.03 * grau);
                double cintila = 0.5 + 0.5 * Math.sin(2 * Math.PI * 14 * t);
                v[i] += env * cintila * Math.sin(2 * Math.PI * (2637 + 400 * Math.sin(2 * Math.PI * 3 * t)) * t);
            }
        }
        // Grave do lendário: a mesma tônica duas oitavas abaixo, por baixo de tudo.
        if (grau == 3) {
            for (int i = 0; i < v.length; i++) {
                double t = i / TAXA;
                v[i] += 0.5 * Math.exp(-t * 3.5) * Math.sin(2 * Math.PI * 164.81 * t);
            }
        }
        return v;
    }

    /** Uma nota de sino: fundamental e parciais inarmônicos que decaem rápido. */
    private static void sino(double[] v, int inicio, double f, double forca, double decai) {
        double[] parciais = {1, 2.76, 5.4};
        double[] pesos = {1, 0.35, 0.12};
        for (int i = inicio; i < v.length; i++) {
            double t = (i - inicio) / TAXA;
            double s = 0;
            for (int p = 0; p < parciais.length; p++) {
                s += pesos[p] * Math.exp(-t * (3 + 4 * p) / decai) * Math.sin(2 * Math.PI * f * parciais[p] * t);
            }
            v[i] += forca * s * Math.min(1, t / 0.003);
        }
    }

    // ------------------------------------------------------------ utilidades

    private static int amostras(double segundos) {
        return (int) Math.round(segundos * TAXA);
    }

    /** Passa-baixa de um polo: alfa pequeno é mais escuro. */
    private static double[] passaBaixa(double[] v, double alfa) {
        double[] s = new double[v.length];
        double y = 0;
        for (int i = 0; i < v.length; i++) {
            y += alfa * (v[i] - y);
            s[i] = y;
        }
        return s;
    }

    private static double[] normalizado(double[] v, double alvoRms) {
        double soma = 0, pico = 0;
        for (double x : v) {
            soma += x * x;
            pico = Math.max(pico, Math.abs(x));
        }
        double rms = Math.sqrt(soma / v.length);
        double ganho = Math.pow(10, alvoRms / 20) / Math.max(1e-9, rms);
        double teto = Math.pow(10, TETO_DBFS / 20);
        if (pico * ganho > teto) {
            ganho = teto / pico;
        }
        double[] saida = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            saida[i] = v[i] * ganho;
        }
        System.out.printf("  %.1f dB RMS, pico %.1f dBFS%n",
                20 * Math.log10(rms * ganho), 20 * Math.log10(pico * ganho));
        return saida;
    }

    private static void gravar(double[] v, String nome) throws Exception {
        byte[] b = new byte[v.length * 2];
        for (int i = 0; i < v.length; i++) {
            int s = (int) Math.round(Math.max(-1, Math.min(1, v[i])) * 32767);
            b[2 * i] = (byte) s;
            b[2 * i + 1] = (byte) (s >> 8);
        }
        AudioFormat fmt = new AudioFormat(TAXA, 16, 1, true, false);
        File destino = new File(AUDIO + nome + ".wav");
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(b), fmt, v.length),
                AudioFileFormat.Type.WAVE, destino);
        System.out.println(nome + ".wav  (" + destino.length() / 1024 + " KB)");
    }
}
