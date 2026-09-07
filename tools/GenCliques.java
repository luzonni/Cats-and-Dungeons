import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

/**
 * Sintetiza os dois cliques da interface: passar o mouse e acionar.
 *
 * O QUE ESTAVA ERRADO. O button.wav antigo tinha 1,11 s e pico de -3,4 dBFS —
 * era o som mais alto do jogo depois da moeda. E ele nao tocava so no clique:
 * tocava tambem a cada vez que o ponteiro entrava num botao. Passar o mouse pela
 * coluna de tres botoes disparava tres sons de um segundo, quase no fundo de
 * escala, empilhados uns nos outros.
 *
 * O QUE A REFERENCIA DIZ. Som de microinteracao tem de ser MUITO curto — 30 a 70
 * ms para um clique, 300 ms como teto absoluto — e mais baixo que o resto do
 * audio do jogo, porque a funcao dele e confirmar que a entrada foi registrada,
 * nao chamar atencao. Passar o mouse e acionar precisam de sons DIFERENTES, com
 * o de passagem mais discreto que o de acionamento. Na faixa de frequencia, 200
 * a 500 Hz da peso de confirmacao e 1 a 5 kHz da clareza informativa. E o
 * material tem de pertencer ao mundo do jogo: batidinha seca de madeira ou pedra
 * numa masmorra, nunca bipe digital.
 *
 * COMO SAO FEITOS. Cada clique e uma batida amortecida: um transiente curtissimo
 * de ruido filtrado — a unha batendo — mais tres parciais inarmonicos que
 * decaem rapido, que sao o corpo do material. Parciais inarmonicos e decaimento
 * rapido sao o que separa "madeira" de "sino"; harmonicos exatos ou decaimento
 * longo dariam nota musical, e nota musical numa interface briga com a trilha.
 *
 *   passagem  28 ms, pico -26 dBFS, corpo em 1,4 kHz — clareza, sem peso
 *   clique    58 ms, pico -14 dBFS, corpo em 380 Hz  — peso de confirmacao
 *
 * SINTETIZADO, E NAO BAIXADO, por dois motivos: um tique seco de 30 ms e curto
 * demais para haver diferenca audivel entre sintese e gravacao, e assim a origem
 * fica sem duvida de licenca — o arquivo nasce no repositorio.
 *
 * Uso: java tools/GenCliques.java
 */
public class GenCliques {

    private static final String AUDIO =
            "src/main/resources/com/retronova/resources/audio/";
    private static final String ORIGINAIS = "tools/assets/audio_original/";

    private static final float SR = 44100f;

    /** Uma batida: duracao, pico alvo e os parciais que dao o material. */
    record Batida(String nome, double ms, double picoDbfs, double[] parciais,
                  double[] pesos, double decaimento, double brilho) { }

    private static final Batida[] BATIDAS = {
            // Passagem: so o transiente e um corpo agudo e curto. Tem de ser
            // notado sem ser ouvido — se der para descrever o som, esta alto.
            new Batida("hover", 28, -26,
                    new double[]{1420, 2360, 3910},
                    new double[]{1.0, 0.45, 0.20}, 150, 0.55),
            // Acionamento: mesma familia, uma oitava e meia abaixo e com mais
            // corpo. E o peso na faixa de 200 a 500 Hz que faz ler como "feito".
            new Batida("button", 58, -14,
                    new double[]{380, 610, 1480},
                    new double[]{1.0, 0.5, 0.28}, 70, 0.85),
    };

    public static void main(String[] a) throws Exception {
        new File(ORIGINAIS).mkdirs();
        for (Batida b : BATIDAS) {
            File destino = new File(AUDIO + b.nome() + ".wav");
            preservar(destino);
            gravar(sintetizar(b), destino);
            System.out.printf("  %-12s %.0f ms  pico %.0f dBFS%n",
                    b.nome() + ".wav", b.ms(), b.picoDbfs());
        }
    }

    /** Guarda o som antigo antes de sobrescrever. So na primeira vez. */
    static void preservar(File atual) throws Exception {
        File guardado = new File(ORIGINAIS + atual.getName());
        if (atual.exists() && !guardado.exists()) {
            java.nio.file.Files.copy(atual.toPath(), guardado.toPath());
        }
    }

    static short[] sintetizar(Batida b) {
        int n = (int) (SR * b.ms() / 1000.0);
        double[] v = new double[n];
        Random r = new Random(b.nome().hashCode());   // mesmo som a cada geracao

        // Transiente: 2 ms de ruido passa-baixa. E a unha tocando o material,
        // antes de o material responder.
        double anterior = 0;
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double ruido = r.nextDouble() * 2 - 1;
            anterior += (ruido - anterior) * b.brilho();     // passa-baixa de 1 polo
            v[i] += anterior * Math.exp(-t * 900) * 0.6;
        }

        // Corpo: parciais inarmonicos amortecidos. Os agudos morrem antes dos
        // graves, como em qualquer batida em material solido.
        for (int p = 0; p < b.parciais().length; p++) {
            double f = b.parciais()[p];
            double peso = b.pesos()[p];
            double decai = b.decaimento() * (1 + p * 0.9);
            for (int i = 0; i < n; i++) {
                double t = i / SR;
                v[i] += peso * Math.sin(2 * Math.PI * f * t) * Math.exp(-t * decai);
            }
        }

        // Rampa de saida: sem ela o corte no fim do arquivo vira um estalo, que
        // e justamente o defeito que este som existe para nao ter.
        int rampa = (int) (SR * 0.004);
        for (int i = 0; i < rampa && i < n; i++) {
            v[n - 1 - i] *= i / (double) rampa;
        }

        return normalizar(v, b.picoDbfs());
    }

    /** Leva o pico exatamente ao alvo, em dBFS. */
    static short[] normalizar(double[] v, double alvoDbfs) {
        double pico = 0;
        for (double s : v) {
            pico = Math.max(pico, Math.abs(s));
        }
        double alvo = Math.pow(10, alvoDbfs / 20.0);
        double ganho = pico > 0 ? alvo / pico : 0;
        short[] o = new short[v.length];
        for (int i = 0; i < v.length; i++) {
            o[i] = (short) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE, Math.round(v[i] * ganho * 32767)));
        }
        return o;
    }

    static void gravar(short[] amostras, File destino) throws Exception {
        byte[] bytes = new byte[amostras.length * 2];
        for (int i = 0; i < amostras.length; i++) {
            bytes[i * 2] = (byte) (amostras[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((amostras[i] >> 8) & 0xFF);
        }
        AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
        try (AudioInputStream in = new AudioInputStream(
                new ByteArrayInputStream(bytes), fmt, amostras.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, destino);
        }
    }
}
