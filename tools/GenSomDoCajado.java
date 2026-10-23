import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

/**
 * SOPRO ARCANO E CRISTAIS, para o cajado e o grimório. O laser recortado tinha
 * ataque rápido, mas continuava sendo um disparo de ficção científica. Aqui o
 * corpo é um sopro curto e grave, com ressonâncias de sino de madeira/vidro.
 * Frequências estáveis, sem a varredura aguda de uma pistola laser.
 *
 * Uma única nota curta por disparo: não forma melodias nem acumula caudas longas
 * na cadência do cajado. Fonte sintetizada, determinística, sem assets externos.
 * Uso: java tools/GenSomDoCajado.java
 */
public class GenSomDoCajado {
    private static final float TAXA = 44100;
    private static final double DURACAO = 0.24;
    private static final double ATAQUE = 0.003, CAUDA = 0.060;
    private static final double RMS_ALVO = -28, TETO_PICO = -1.5;
    private static final double[] FREQUENCIAS = {293.66, 440.0, 587.32, 880.0};
    private static final double[] PESOS = {0.65, 0.22, 0.18, 0.06};
    private static final String DESTINO =
            "src/main/resources/com/retronova/resources/audio/spell_arcane.wav";

    public static void main(String[] args) throws Exception {
        double[] som = new double[(int) (TAXA * DURACAO)];
        Random ruido = new Random(0x52554E464F524345L);
        double sopro = 0, pico = 0, energia = 0;
        for (int i = 0; i < som.length; i++) {
            double t = i / (double) TAXA;
            // Ruído filtrado: ar passando pela ponta, em vez de chiado metálico.
            sopro += 0.12 * (ruido.nextDouble() * 2 - 1 - sopro);
            double cristais = 0;
            for (int n = 0; n < FREQUENCIAS.length; n++) {
                double ressonancia = Math.sin(2 * Math.PI * FREQUENCIAS[n] * t);
                cristais += PESOS[n] * ressonancia * Math.exp(-t * (15 + n * 4));
            }
            double corpo = 0.22 * Math.sin(2 * Math.PI * 146.83 * t) * Math.exp(-t * 24);
            double envelope = Math.min(1, t / ATAQUE)
                    * Math.min(1, (DURACAO - t) / CAUDA);
            som[i] = (cristais + corpo + sopro * 1.1 * Math.exp(-t * 32)) * envelope;
            energia += som[i] * som[i];
            pico = Math.max(pico, Math.abs(som[i]));
        }
        double ganho = Math.min(Math.pow(10, RMS_ALVO / 20) / Math.sqrt(energia / som.length),
                Math.pow(10, TETO_PICO / 20) / pico);
        byte[] pcm = new byte[som.length * 2];
        for (int i = 0; i < som.length; i++) {
            int valor = (int) Math.round(som[i] * ganho * 32767);
            pcm[2 * i] = (byte) valor;
            pcm[2 * i + 1] = (byte) (valor >> 8);
        }
        AudioFormat formato = new AudioFormat(TAXA, 16, 1, true, false);
        try (var in = new AudioInputStream(new ByteArrayInputStream(pcm), formato, som.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, new File(DESTINO));
        }
        System.out.println("spell_arcane.wav: 240 ms, -28 dB RMS");
    }
}
