import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Os "blips" de fala dos diálogos: a voz de cada personagem enquanto o texto
 * escreve.
 *
 * É A TÉCNICA DO UNDERTALE E DO ANIMAL CROSSING: em vez de voz gravada, um som
 * curto e tonal por letra (aqui, a cada duas), com timbre e altura próprios de
 * cada personagem. O ouvido lê a altura como o "jeito de falar": o vendedor é
 * um gato de meia-idade, então a voz é média e redonda — um miado curto que
 * desce, e não um bipe de máquina.
 *
 * TRÊS VARIAÇÕES, sorteadas a cada blip: a mesma nota repetida dez vezes por
 * segundo vira zumbido; três alturas próximas soam como sílabas.
 *
 * Cada blip tem 55 ms, ataque de 2 ms e queda exponencial: cabe inteiro entre
 * duas sílabas na velocidade rápida e não empilha. O volume final é acertado
 * pelo GenMixagem (camada de fundo, -34 dB RMS), que deve rodar depois deste.
 *
 * Uso: java tools/GenVozesDeDialogo.java
 */
public class GenVozesDeDialogo {

    static final String AUDIO = "src/main/resources/com/retronova/resources/audio/";
    static final float TAXA = 44100f;
    static final double DURACAO = 0.055;

    public static void main(String[] args) throws Exception {
        // Vendedor: miado curto, de 520 a 620 Hz, descendo meio tom no fim.
        double[] alturas = {520, 568, 620};
        for (int i = 0; i < alturas.length; i++) {
            gravar(miado(alturas[i]), new File(AUDIO + "voz_vendedor_" + (i + 1) + ".wav"));
            System.out.println("  audio/voz_vendedor_" + (i + 1) + ".wav");
        }
    }

    /**
     * Um miado curto: a fundamental com dois harmônicos (timbre de voz, e não de
     * apito), a altura descendo 6% ao longo do som e um "formante" simples que
     * abre no começo e fecha no fim, que é o que dá a cara de vogal.
     */
    static double[] miado(double altura) {
        int n = (int) (TAXA * DURACAO);
        double[] v = new double[n];
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = altura * (1 - 0.06 * t);
            fase += 2 * Math.PI * f / TAXA;
            double abertura = Math.sin(Math.PI * Math.min(1, t * 1.4));
            double s = Math.sin(fase) + 0.45 * abertura * Math.sin(2 * fase) + 0.2 * Math.sin(3 * fase);
            double ataque = Math.min(1, i / (TAXA * 0.002));
            double queda = Math.exp(-3.2 * t);
            v[i] = s * ataque * queda * 0.5;
        }
        return v;
    }

    static void gravar(double[] v, File destino) throws Exception {
        byte[] b = new byte[v.length * 2];
        for (int i = 0; i < v.length; i++) {
            int s = (int) Math.round(Math.max(-1, Math.min(1, v[i])) * 32767);
            b[2 * i] = (byte) (s & 0xFF);
            b[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        AudioFormat fmt = new AudioFormat(TAXA, 16, 1, true, false);
        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(b), fmt, v.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, destino);
        }
    }
}
