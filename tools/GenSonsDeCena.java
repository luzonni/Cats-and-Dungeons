import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Os dois sons que o jogo não tinha de jeito nenhum.
 *
 * O RESTO DO ARSENAL JÁ EXISTIA. Fogo, gelo, água, vento e trovão estavam em
 * resources desde sempre, sem uma linha de código os referenciando — eram
 * arquivos órfãos. Estes dois não existiam em lugar nenhum, e por isso são
 * sintetizados aqui em vez de baixados: nenhum pacote CC0 traz exatamente "gato
 * roncando" no timbre deste jogo, e uma conquista é um jingle curto que se
 * escreve em nota, não se procura.
 *
 * ---------------------------------------------------------------------------
 * CONQUISTA — o som de "isto que você acabou de fazer importa".
 *
 * É um arpejo maior ascendente de quatro notas, com o último grau dobrado uma
 * oitava acima. A escolha não é estética: arpejo ASCENDENTE em modo MAIOR é o
 * vocabulário que o jogador já associa a recompensa desde os anos oitenta, e
 * reconhecer sem aprender é exatamente o que um som de conquista precisa fazer.
 * Descendente leria como perda, e menor como ameaça.
 *
 * O timbre é uma senoide com um terceiro harmônico fraco e ataque instantâneo com
 * cauda longa — a forma de um sino. Sino soa premiado; onda quadrada soaria como
 * menu.
 *
 * ---------------------------------------------------------------------------
 * RONCO — derivado do próprio miado do gato.
 *
 * Mesma técnica das vozes: reamostrar o cat.wav muito devagar. A 0,35x ele cai
 * quase três oitavas e o miado perde a articulação, virando um rolar grave — que
 * é exatamente o que um ronco é. Sair da arte que já existe garante que o gato
 * dormindo soe como O MESMO bicho que mia na tela de seleção.
 *
 * Por cima vem uma modulação lenta de volume, a quatro ciclos por segundo, que é
 * a pulsação do ronco de gato de verdade. Sem ela seria um zumbido.
 *
 * Uso: java tools/GenSonsDeCena.java
 */
public class GenSonsDeCena {

    private static final String AUDIO =
            "src/main/resources/com/retronova/resources/audio/";

    private static final float TAXA = 44100;

    public static void main(String[] args) throws Exception {
        AudioFormat fmt = new AudioFormat(TAXA, 16, 1, true, false);
        gravar(conquista(), fmt, new File(AUDIO + "achievement.wav"));
        System.out.println("  audio/achievement.wav");
        gravar(ronco(), fmt, new File(AUDIO + "purr.wav"));
        System.out.println("  audio/purr.wav");
    }

    // ------------------------------------------------------------ conquista

    /**
     * Dó, Mi, Sol, Dó — o arpejo maior, subindo.
     *
     * As três primeiras são curtas e a última é longa: é a última que fica, e
     * segurá-la é o que faz o conjunto terminar em vez de simplesmente parar.
     */
    private static final double[] NOTAS = {523.25, 659.25, 783.99, 1046.50};
    private static final double[] DURACOES = {0.09, 0.09, 0.09, 0.55};

    private static double[] conquista() {
        double total = 0;
        for (int i = 0; i < NOTAS.length; i++) {
            total += i < NOTAS.length - 1 ? DURACOES[i] : DURACOES[i];
        }
        double[] saida = new double[(int) (TAXA * (total + 0.2))];
        double inicio = 0;
        for (int n = 0; n < NOTAS.length; n++) {
            // AS NOTAS SE SOBREPOEM. Cada uma soa por mais tempo do que dura o
            // passo ate a seguinte, entao a anterior ainda esta morrendo quando a
            // proxima entra. E o que separa um arpejo de quatro bipes soltos.
            int base = (int) (inicio * TAXA);
            double duracao = n < NOTAS.length - 1 ? DURACOES[n] * 4 : DURACOES[n];
            int quadros = (int) (duracao * TAXA);
            for (int i = 0; i < quadros && base + i < saida.length; i++) {
                double t = i / TAXA;
                double env = Math.exp(-t * (n < NOTAS.length - 1 ? 7 : 4));
                double onda = Math.sin(2 * Math.PI * NOTAS[n] * t)
                        + 0.25 * Math.sin(2 * Math.PI * NOTAS[n] * 3 * t);
                saida[base + i] += onda * env * 0.32;
            }
            inicio += DURACOES[n];
        }
        rampa(saida, (int) (0.003 * TAXA), true);
        rampa(saida, (int) (0.06 * TAXA), false);
        normalizar(saida, -5);
        return saida;
    }

    // ---------------------------------------------------------------- ronco

    private static double[] ronco() throws Exception {
        double[] miado = ler(AUDIO + "cat.wav");
        double[] grave = reamostrar(miado, 0.35);

        // UM SEGUNDO E MEIO, E NAO O MIADO INTEIRO ESTICADO.
        //
        // O miado a 0,35x dura dois segundos e meio, e tres voltas dele passavam de
        // sete segundos — mais que o intervalo entre dois "Z", entao os roncos se
        // empilhariam uns sobre os outros ate virar um zumbido continuo. Um ronco e
        // um SOPRO curto que se repete; a repeticao quem faz e o jogo, nao o
        // arquivo.
        int quadros = Math.min(grave.length, (int) (1.5 * TAXA));
        double[] saida = new double[quadros];
        System.arraycopy(grave, 0, saida, 0, quadros);
        // A PULSACAO. Quatro por segundo e o ritmo do ronco de gato; e ela que
        // transforma um som grave continuo em respiracao.
        for (int i = 0; i < saida.length; i++) {
            double t = i / TAXA;
            saida[i] *= 0.55 + 0.45 * (0.5 + 0.5 * Math.sin(2 * Math.PI * 4 * t));
        }
        rampa(saida, (int) (0.05 * TAXA), true);
        rampa(saida, (int) (0.15 * TAXA), false);
        // BAIXO DE PROPOSITO. E uma piada de fundo, nao um aviso: tem de ser
        // notado por quem para para ouvir, e ignorado por quem esta jogando.
        normalizar(saida, -16);
        return saida;
    }

    // --------------------------------------------------------------- ajuda

    private static double[] ler(String caminho) throws Exception {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new File(caminho))) {
            AudioFormat f = in.getFormat();
            byte[] bruto = in.readAllBytes();
            int canais = f.getChannels();
            int quadros = bruto.length / (2 * canais);
            double[] v = new double[quadros];
            for (int q = 0; q < quadros; q++) {
                // Mistura os canais: o resultado e mono, e a media evita que o
                // material estereo perca metade do corpo ao ser cortado.
                double soma = 0;
                for (int c = 0; c < canais; c++) {
                    int i = (q * canais + c) * 2;
                    soma += (short) ((bruto[i] & 0xFF) | (bruto[i + 1] << 8)) / 32768.0;
                }
                v[q] = soma / canais;
            }
            return v;
        }
    }

    /** Le a onda em outra velocidade, com interpolacao linear. Ver GenVozes. */
    private static double[] reamostrar(double[] fonte, double razao) {
        int n = (int) (fonte.length / razao);
        double[] o = new double[n];
        for (int i = 0; i < n; i++) {
            double pos = i * razao;
            int p = (int) pos;
            double f = pos - p;
            double a = fonte[Math.min(p, fonte.length - 1)];
            double b = fonte[Math.min(p + 1, fonte.length - 1)];
            o[i] = a + (b - a) * f;
        }
        return o;
    }

    private static void rampa(double[] v, int n, boolean abrindo) {
        for (int i = 0; i < n && i < v.length; i++) {
            double f = i / (double) n;
            if (abrindo) {
                v[i] *= f;
            } else {
                v[v.length - 1 - i] *= f;
            }
        }
    }

    private static void normalizar(double[] v, double alvoDbfs) {
        double pico = 0;
        for (double x : v) {
            pico = Math.max(pico, Math.abs(x));
        }
        if (pico == 0) {
            return;
        }
        double ganho = Math.pow(10, alvoDbfs / 20.0) / pico;
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
