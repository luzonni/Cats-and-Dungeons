import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

/**
 * Gera o áudio da escolha de elemento: a trilha, o stinger e uma confirmação por
 * elemento. Tudo sintetizado aqui — sem arquivo de fora, sem licença a conferir.
 *
 * ---------------------------------------------------------------------------
 * O QUE CADA UM FAZ NO MOMENTO
 *
 *   elemento_revelacao.wav  o STINGER da entrada: um impacto grave que cai, um
 *                           acorde claro que abre devagar e um chiado de prato.
 *                           Toca junto com o pulso que sai do gato: é o "isto não
 *                           é um level up" dito pelo ouvido.
 *   escolha_elemento.wav    a TRILHA enquanto se escolhe, em loop de 16 s: um
 *                           bordão grave, um acorde que respira (Lá menor com
 *                           nona, depois Fá com sétima) e sinos em pentatônica.
 *                           Suspensa de propósito — nem briga, nem calma —, e
 *                           diferente da arena e do level up, que não troca a
 *                           música. Entra e sai por desvanecimento (Sound).
 *   elemento_<x>.wav        a CONFIRMAÇÃO de cada elemento: o mesmo sino claro em
 *                           cima (a "assinatura" da escolha) e por baixo uma camada
 *                           que soa como o elemento — estalos de fogo, cristais de
 *                           gelo, bolhas, um baque de terra, uma rajada de vento.
 *
 * ---------------------------------------------------------------------------
 * NÍVEIS: a régua do jogo (ver docs/ARQUITETURA.md, Áudio). Stinger e
 * confirmações são eventos marcantes, -20 dB RMS; a trilha é leito, -22 dB RMS
 * (por volta de -18 LUFS para este tipo de som contínuo — ver GenMixagem, que mede
 * músicas com ffmpeg). Teto de pico de -1,5 dBFS.
 *
 * SÃO PROVISÓRIOS NO TIMBRE, não no papel: sintetizado em senoides e ruído, o som
 * cumpre a função e casa com o chiptune das trilhas de arena, mas uma versão
 * definitiva feita por quem faz som ficaria melhor. Os nomes dos arquivos e os
 * pontos onde tocam ficam iguais — basta trocar os arquivos.
 *
 * Uso: java tools/GenSonsDaEscolha.java
 */
public class GenSonsDaEscolha {

    private static final String AUDIO = "src/main/resources/com/retronova/resources/audio/";
    private static final float TAXA = 44100f;
    /** A trilha em metade da taxa: é um leito suave, e cabe em metade do disco. */
    private static final float TAXA_DA_TRILHA = 22050f;
    private static final double TETO_DBFS = -1.5;

    public static void main(String[] args) throws Exception {
        gravar(normalizado(revelacao(), -20), TAXA, "elemento_revelacao");
        gravar(normalizado(trilha(), -22), TAXA_DA_TRILHA, "escolha_elemento");
        String[] nomes = {"fogo", "gelo", "agua", "terra", "ar"};
        for (int i = 0; i < nomes.length; i++) {
            gravar(normalizado(confirmacao(i), -20), TAXA, "elemento_" + nomes[i]);
        }
    }

    // ------------------------------------------------------------ stinger

    private static double[] revelacao() {
        int n = (int) (TAXA * 2.2);
        double[] v = new double[n];
        Random rnd = new Random(7);
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            // Impacto grave que CAI de 90 para 38 Hz: o peso de algo chegando.
            double f = 38 + 52 * Math.exp(-t * 4);
            fase += 2 * Math.PI * f / TAXA;
            double impacto = Math.sin(fase) * Math.exp(-t * 3.2);
            // Acorde claro que abre (ataque de 120 ms) e se demora.
            double env = Math.min(1, t / 0.12) * Math.exp(-t * 1.4);
            double acorde = 0;
            for (double nota : new double[]{440, 554.37, 659.25, 880, 1318.5}) {
                acorde += Math.sin(2 * Math.PI * nota * t + 0.3 * Math.sin(2 * Math.PI * 5 * t))
                        + 0.2 * Math.sin(2 * Math.PI * nota * 2 * t);
            }
            // Prato: ruído que some rápido, só o brilho do golpe.
            double prato = (rnd.nextDouble() * 2 - 1) * Math.exp(-t * 7);
            v[i] = impacto * 0.9 + acorde * env * 0.12 + prato * 0.18;
        }
        rampa(v, (int) (0.002 * TAXA), true);
        rampa(v, (int) (0.3 * TAXA), false);
        return v;
    }

    // ------------------------------------------------------------- trilha

    private static double[] trilha() {
        double dur = 16;
        int n = (int) (TAXA_DA_TRILHA * dur);
        double[] v = new double[n];
        double[][] acordes = {
                {220, 261.63, 329.63, 493.88},       // Lá menor com nona
                {174.61, 220, 261.63, 329.63}        // Fá com sétima maior
        };
        for (int i = 0; i < n; i++) {
            double t = i / TAXA_DA_TRILHA;
            // Bordão em Lá, com um respiro que fecha o ciclo em 16 s (sem emenda).
            double respiro = 0.75 + 0.25 * Math.sin(2 * Math.PI * t / 16);
            double bordao = (Math.sin(2 * Math.PI * 55 * t) + 0.5 * Math.sin(2 * Math.PI * 110 * t)) * respiro;
            // Os dois acordes, 8 s cada, cruzando em cosseno — também em ciclo.
            double mistura = 0.5 - 0.5 * Math.cos(2 * Math.PI * t / 16);
            double pad = 0;
            for (int c = 0; c < 2; c++) {
                double peso = c == 0 ? 1 - mistura : mistura;
                for (double nota : acordes[c]) {
                    pad += peso * (Math.sin(2 * Math.PI * nota * t) + 0.3 * Math.sin(2 * Math.PI * nota * 2.0 * t));
                }
            }
            v[i] = bordao * 0.35 + pad * 0.09;
        }
        // Sinos em pentatônica de Lá menor, um por segundo, num padrão fixo. A
        // cauda de cada um que passaria do fim volta para o começo: o loop não
        // tem corte.
        double[] escala = {440, 523.25, 587.33, 659.25, 783.99, 880};
        Random rnd = new Random(11);
        int cauda = (int) (TAXA_DA_TRILHA * 1.6);
        for (int s = 0; s < 16; s++) {
            double nota = escala[rnd.nextInt(escala.length)];
            int inicio = (int) (s * TAXA_DA_TRILHA);
            double forca = 0.10 + rnd.nextDouble() * 0.06;
            for (int k = 0; k < cauda; k++) {
                double t = k / TAXA_DA_TRILHA;
                double sino = Math.sin(2 * Math.PI * nota * t) + 0.35 * Math.sin(2 * Math.PI * nota * 2.76 * t);
                v[(inicio + k) % n] += sino * forca * Math.exp(-t * 2.6) * Math.min(1, t / 0.004);
            }
        }
        return v;
    }

    // -------------------------------------------------------- confirmações

    /** 0 fogo, 1 gelo, 2 água, 3 terra, 4 ar. */
    private static double[] confirmacao(int elemento) {
        int n = (int) (TAXA * 1.3);
        double[] v = new double[n];
        Random rnd = new Random(100 + elemento);
        double filtro = 0, filtro2 = 0;
        double[] bolhas = new double[10];
        for (int b = 0; b < bolhas.length; b++) {
            bolhas[b] = 0.05 + rnd.nextDouble() * 0.7;
        }
        for (int i = 0; i < n; i++) {
            double t = i / TAXA;
            // A ASSINATURA: o mesmo sino claro em toda confirmação.
            double sino = (Math.sin(2 * Math.PI * 1318.5 * t) + 0.6 * Math.sin(2 * Math.PI * 1975.5 * t))
                    * Math.exp(-t * 3) * 0.16;
            double ruido = rnd.nextDouble() * 2 - 1;
            double camada = 0;
            switch (elemento) {
                case 0 -> {     // fogo: chiado que sobe e estalos soltos
                    filtro += (ruido - filtro) * 0.08;
                    double env = Math.min(1, t / 0.08) * Math.exp(-t * 2.2);
                    double estalo = rnd.nextDouble() < 0.004 * Math.exp(-t * 2) ? (rnd.nextDouble() * 2 - 1) * 0.9 : 0;
                    camada = filtro * env * 1.6 + estalo * 0.5;
                }
                case 1 -> {     // gelo: cristais agudos, um depois do outro
                    double[] notas = {2093, 2637, 3136, 4186};
                    for (int k = 0; k < notas.length; k++) {
                        double tk = t - k * 0.05;
                        if (tk > 0) {
                            camada += Math.sin(2 * Math.PI * notas[k] * tk) * Math.exp(-tk * 6) * 0.35;
                        }
                    }
                    camada += ruido * Math.exp(-t * 40) * 0.6;          // o estalo do gelo
                }
                case 2 -> {     // água: bolhas que sobem de tom, sobre um respingo
                    for (double b : bolhas) {
                        double tb = t - b;
                        if (tb > 0 && tb < 0.06) {
                            double f = 400 + 900 * (tb / 0.06);
                            camada += Math.sin(2 * Math.PI * f * tb) * Math.exp(-tb * 40) * 0.8;
                        }
                    }
                    filtro += (ruido - filtro) * 0.2;
                    camada += filtro * Math.exp(-t * 6) * 0.8;
                }
                case 3 -> {     // terra: baque grave e ronco de pedra
                    camada = Math.sin(2 * Math.PI * (45 + 30 * Math.exp(-t * 8)) * t) * Math.exp(-t * 4) * 1.2;
                    filtro += (ruido - filtro) * 0.03;
                    camada += filtro * Math.exp(-t * 3) * 2.2;
                }
                default -> {    // ar: rajada — ruído cujo filtro abre e fecha
                    double abre = Math.sin(Math.PI * Math.min(1, t / 0.9));
                    filtro += (ruido - filtro) * (0.02 + 0.25 * abre);
                    filtro2 += (filtro - filtro2) * 0.5;
                    camada = (filtro - filtro2 * 0.5) * abre * 1.5;
                }
            }
            v[i] = sino + camada;
        }
        rampa(v, (int) (0.002 * TAXA), true);
        rampa(v, (int) (0.25 * TAXA), false);
        return v;
    }

    // ---------------------------------------------------------- utilidades

    private static void rampa(double[] v, int amostras, boolean abrindo) {
        amostras = Math.min(amostras, v.length);
        for (int i = 0; i < amostras; i++) {
            double g = i / (double) amostras;
            if (abrindo) {
                v[i] *= g;
            } else {
                v[v.length - 1 - i] *= g;
            }
        }
    }

    /** Leva ao RMS alvo; se o pico passar do teto, o teto manda. */
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
        double novoRms = Math.sqrt(soma / v.length) * ganho;
        System.out.printf("  %.1f dB RMS, pico %.1f dBFS%n", 20 * Math.log10(novoRms), 20 * Math.log10(pico * ganho));
        return saida;
    }

    private static void gravar(double[] v, float taxa, String nome) throws Exception {
        byte[] b = new byte[v.length * 2];
        for (int i = 0; i < v.length; i++) {
            int s = (int) Math.round(Math.max(-1, Math.min(1, v[i])) * 32767);
            b[2 * i] = (byte) s;
            b[2 * i + 1] = (byte) (s >> 8);
        }
        AudioFormat fmt = new AudioFormat(taxa, 16, 1, true, false);
        File destino = new File(AUDIO + nome + ".wav");
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(b), fmt, v.length),
                AudioFileFormat.Type.WAVE, destino);
        System.out.println(nome + ".wav  (" + destino.length() / 1024 + " KB)");
    }
}
