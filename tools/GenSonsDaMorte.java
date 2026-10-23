import javax.sound.sampled.*;
import java.io.*;

/**
 * Despedida derivada dos miados do acervo, sem fonte externa: voz descendente
 * sobre um impacto grave curto. O título recebe uma nota de encerramento.
 * Reproduzir: java tools/GenSonsDaMorte.java
 */
public class GenSonsDaMorte {
    private static final String AUDIO = "src/main/resources/com/retronova/resources/audio/";
    private static final int TAXA = 44100;
    private static final double ATAQUE = 0.003, CAUDA = 0.090;
    private static final double MAXIMO = 1.2, TETO = -1.5;
    private static final double TITULO = 0.32;

    public static void main(String[] args) throws Exception {
        for (String gato : new String[]{"muffin", "azrael", "finn"}) {
            double[] fonte = ler("cat_" + gato);
            double pico = 0;
            for (double v : fonte) pico = Math.max(pico, Math.abs(v));
            int inicio = 0;
            while (inicio < fonte.length - 1 && Math.abs(fonte[inicio]) < pico * 0.12) inicio++;
            inicio = Math.max(0, inicio - (int) (ATAQUE * TAXA));
            double duracao = Math.min(MAXIMO, (fonte.length - inicio) / (TAXA * 0.8));
            double[] som = new double[(int) (duracao * TAXA)];
            double posicao = inicio;
            for (int i = 0; i < som.length; i++) {
                double t = i / (double) TAXA;
                int p = Math.min(fonte.length - 2, (int) posicao);
                double fracao = Math.min(1, posicao - p);
                double voz = fonte[p] + (fonte[p + 1] - fonte[p]) * fracao;
                posicao += 0.95 - 0.30 * t / duracao;
                double impacto = 0.18 * Math.sin(2 * Math.PI * 65 * t) * Math.exp(-t * 35);
                som[i] = (voz + impacto) * Math.min(1, t / ATAQUE)
                        * Math.min(1, (duracao - t) / CAUDA);
            }
            gravar("death_" + gato, som, -20);
        }
        double[] titulo = new double[(int) (TITULO * TAXA)];
        for (int i = 0; i < titulo.length; i++) {
            double t = i / (double) TAXA;
            double nota = Math.sin(2 * Math.PI * 146.83 * t)
                    + 0.30 * Math.sin(2 * Math.PI * 220 * t)
                    + 0.15 * Math.sin(2 * Math.PI * 293.66 * t);
            titulo[i] = nota * Math.exp(-t * 13) * Math.min(1, t / ATAQUE)
                    * Math.min(1, (TITULO - t) / CAUDA);
        }
        gravar("run_over", titulo, -24);
    }

    private static double[] ler(String nome) throws Exception {
        try (var original = AudioSystem.getAudioInputStream(new File(AUDIO + nome + ".wav"))) {
            AudioFormat fmt = new AudioFormat(TAXA, 16, original.getFormat().getChannels(), true, false);
            try (var in = AudioSystem.getAudioInputStream(fmt, original)) {
                byte[] bytes = in.readAllBytes();
                int canais = fmt.getChannels();
                double[] mono = new double[bytes.length / (2 * canais)];
                for (int i = 0; i < mono.length; i++) {
                    for (int c = 0; c < canais; c++) {
                        int j = (i * canais + c) * 2;
                        mono[i] += (short) ((bytes[j] & 255) | bytes[j + 1] << 8) / (32768.0 * canais);
                    }
                }
                return mono;
            }
        }
    }

    private static void gravar(String nome, double[] som, double alvo) throws Exception {
        double pico = 0, energia = 0;
        for (double v : som) { pico = Math.max(pico, Math.abs(v)); energia += v * v; }
        double ganho = Math.min(Math.pow(10, alvo / 20) / Math.sqrt(energia / som.length),
                Math.pow(10, TETO / 20) / pico);
        byte[] pcm = new byte[som.length * 2];
        for (int i = 0; i < som.length; i++) {
            int v = (int) Math.round(som[i] * ganho * 32767);
            pcm[2 * i] = (byte) v; pcm[2 * i + 1] = (byte) (v >> 8);
        }
        try (var in = new AudioInputStream(new ByteArrayInputStream(pcm),
                new AudioFormat(TAXA, 16, 1, true, false), som.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, new File(AUDIO + nome + ".wav"));
        }
        System.out.printf("%s: %.0f ms, %.0f dB RMS%n", nome, som.length * 1000.0 / TAXA, alvo);
    }
}
