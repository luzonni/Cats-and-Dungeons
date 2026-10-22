import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

/**
 * Gera o áudio da abertura do jogo (menus/Abertura). Tudo sintetizado aqui — sem
 * arquivo de fora, sem licença a conferir.
 *
 * ---------------------------------------------------------------------------
 * O QUE CADA UM FAZ NO MOMENTO
 *
 *   abertura_vento.wav    o AR da masmorra por baixo de tudo: ruído filtrado que
 *                         sobe e desce devagar e um grave em duas notas que
 *                         batem entre si. Entra com o fade do preto e vai embora
 *                         quando a ambiência da tela inicial assume.
 *   abertura_credito.wav  o crédito da equipe: duas notas graves e macias, um
 *                         "aqui começa" que não disputa com o que vem depois.
 *   abertura_logo.wav     a logo: sinos subindo em arpejo (Lá menor com nona,
 *                         a mesma cor da trilha da escolha de elemento) sobre um
 *                         baque grave e macio, e um brilho que se abre.
 *
 * Nada de música: a música é a da tela inicial, que entra por desvanecimento no
 * fim da abertura (Sound.desvanecer) — a abertura é o ar antes dela.
 *
 * ---------------------------------------------------------------------------
 * NÍVEIS: a régua do jogo (ver docs/ARQUITETURA.md, Áudio). A logo é o evento
 * marcante, -20 dB RMS; o crédito, -24; o vento é leito, -28. Teto de pico de
 * -1,5 dBFS. Tocam pelo Sound, que aplica Master e Sound effects desde o primeiro
 * som (as configurações carregam antes de qualquer áudio).
 *
 * SÃO PROVISÓRIOS NO TIMBRE, não no papel: quem faz som faria melhor. Os nomes e
 * os pontos onde tocam ficam iguais — basta trocar os arquivos.
 *
 *     java tools/GenSonsDaAbertura.java
 */
public class GenSonsDaAbertura {

    private static final String AUDIO = "src/main/resources/com/retronova/resources/audio/";
    private static final float TAXA = 44100f;
    private static final double TETO_DBFS = -1.5;
    private static final Random R = new Random(0xAB3);

    public static void main(String[] args) throws Exception {
        gravar(normalizado(vento(), -28), "abertura_vento");
        gravar(normalizado(credito(), -24), "abertura_credito");
        gravar(normalizado(logo(), -20), "abertura_logo");
    }

    /** 4,5 s de ar: ruído escuro que respira e um grave em Lá batendo com Mi. */
    private static double[] vento() {
        int n = amostras(4.5);
        double[] ruido = new double[n];
        for (int i = 0; i < n; i++) {
            ruido[i] = R.nextDouble() * 2 - 1;
        }
        ruido = passaBaixa(passaBaixa(ruido, 0.02), 0.05);
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            double env = Math.min(1, t / 1.2) * Math.min(1, (4.5 - t) / 1.5);
            double sopro = 0.7 + 0.3 * Math.sin(2 * Math.PI * 0.35 * t);
            double grave = 0.35 * Math.sin(2 * Math.PI * 55 * t) + 0.22 * Math.sin(2 * Math.PI * 82.4 * t);
            v[i] = env * (6 * ruido[i] * sopro + grave);
        }
        return v;
    }

    /** Duas notas graves e macias (Lá e Mi), quase um sino abafado: 1,4 s. */
    private static double[] credito() {
        double[] v = new double[amostras(1.4)];
        sino(v, 0, 220.00, 0.8, 0.9);
        sino(v, amostras(0.16), 329.63, 0.5, 0.9);
        return passaBaixa(v, 0.25);
    }

    /** Arpejo de sinos subindo, brilho que abre e um baque macio: 2,2 s. */
    private static double[] logo() {
        double[] v = new double[amostras(2.2)];
        double[] notas = {440.00, 523.25, 659.25, 987.77, 1318.51};   // Lá menor com nona, subindo
        for (int k = 0; k < notas.length; k++) {
            sino(v, amostras(k * 0.09), notas[k], 0.55 - 0.05 * k, 1.1);
        }
        // Baque grave quando a logo assenta, na última nota.
        int baque = amostras(0.36);
        for (int j = 0; j < amostras(1.0) && baque + j < v.length; j++) {
            double t = j / TAXA;
            v[baque + j] += 0.9 * Math.exp(-t * 4.5) * Math.sin(2 * Math.PI * (55 + 20 * Math.exp(-t * 10)) * t);
        }
        // Brilho: chiado agudo que abre e some, sem estalo.
        double[] brilho = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            brilho[i] = R.nextDouble() * 2 - 1;
        }
        double anterior = 0;
        for (int i = 0; i < v.length; i++) {
            double t = i / TAXA;
            double agudo = brilho[i] - anterior;   // passa-alta simples
            anterior = brilho[i];
            double env = Math.min(1, t / 0.5) * Math.exp(-Math.max(0, t - 0.5) * 2.5);
            v[i] += 0.05 * env * agudo;
        }
        return v;
    }

    /** Uma nota de sino: fundamental e parciais inarmônicos que decaem rápido. */
    private static void sino(double[] v, int inicio, double f, double forca, double decai) {
        double[] parciais = {1, 2.76, 5.4};
        double[] pesos = {1, 0.3, 0.1};
        for (int i = inicio; i < v.length; i++) {
            double t = (i - inicio) / TAXA;
            double s = 0;
            for (int p = 0; p < parciais.length; p++) {
                s += pesos[p] * Math.exp(-t * (2 + 4 * p) / decai) * Math.sin(2 * Math.PI * f * parciais[p] * t);
            }
            v[i] += forca * s * Math.min(1, t / 0.004);
        }
    }

    private static int amostras(double segundos) {
        return (int) Math.round(segundos * TAXA);
    }

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
