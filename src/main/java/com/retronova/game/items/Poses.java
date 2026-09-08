package com.retronova.game.items;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Onde cada arma fica na mao do gato, ajustado a mao no editor.
 *
 * POR QUE ISTO EXISTE. A pose vinha de {@link Item.Porte}: quatro numeros no
 * codigo, iguais para todas as armas do mesmo tipo. Isso funciona enquanto as
 * armas tem a mesma forma, e parou de funcionar quando cada elemento passou a ter
 * um desenho proprio — um machado de duas laminas nao se segura no mesmo ponto
 * que uma machadinha. Acertar isso por calculo e screenshot e lento e erra: quem
 * ve a arma na mao do gato sabe na hora se esta certo, e quem faz a conta nao.
 *
 * Entao a pose virou DADO, e nao codigo. O arquivo e escrito por
 * {@code tools/EditorDePose.java}, onde da para arrastar a arma no lugar, e lido
 * aqui. Arma sem linha no arquivo continua usando o Porte de sempre — o editor e
 * um ajuste fino por cima do padrao, e nao um substituto obrigatorio.
 *
 * RECARGA A QUENTE. Em desenvolvimento o arquivo e lido de {@code src/main} e
 * reconferido de meio em meio segundo, entao salvar no editor com o jogo aberto
 * muda a pose na hora — sem isso o ciclo seria "salva, fecha, compila, abre", que
 * e lento demais para ajustar a olho. No jogo empacotado nao ha essa pasta e a
 * leitura cai no recurso de dentro do jar, uma vez so.
 */
public final class Poses {

    /**
     * dx e dy em pixels de arte; graus ja e o angulo final; punho em pixels do
     * sprite; espelhar vira o desenho na horizontal.
     *
     * ESPELHAR e um campo e nao um giro porque as duas coisas nao se substituem:
     * girar 180 graus poe a arma de cabeca para baixo, e o que faltava era o gume
     * do machado apontar para o outro lado sem deitar o cabo. O pacote desenha
     * todas as armas viradas para o mesmo lado, entao quem segura com a outra mao
     * precisa do espelho.
     */
    public record Pose(int dx, int dy, int graus, int punhoX, int punhoY, boolean espelhar,
                       int golpeGraus, int golpePx, int bocaX, int bocaY) { }

    /** Sem boca definida: -1. O tiro nasce no miolo do desenho. */
    public static final int SEM_BOCA = -1;

    /** O quanto a arma gira e avanca no auge do golpe, quando a pose nao diz. */
    public static final int GOLPE_GRAUS = 80, GOLPE_PX = 4;

    /**
     * OS CAMPOS QUEREM DIZER COISAS DIFERENTES conforme a arma seja segurada ou
     * apontada, e isso e de proposito.
     *
     * Arma de MAO — espada, machado, varinha:
     *   dx, dy   deslocamento a partir da mao, em pixels de arte
     *   graus    inclinacao da arma parada
     *
     * Arma de MIRA — arco, laser:
     *   dx       distancia do centro do gato, na direcao do alvo
     *   dy       desvio para o lado, perpendicular a mira
     *   graus    giro a mais, somado a direcao do tiro
     *   bocaX/Y  o pixel do DESENHO de onde o tiro sai — a boca da arma. Gira
     *            junto com ela, como tem de ser: a ponta do cano acompanha o cano.
     *
     * Reaproveitar os tres campos, em vez de criar outros tres, mantem o arquivo
     * e o editor com uma tabela so. O que muda e a leitura, e quem le sabe que
     * tipo de arma esta lendo.
     */
    public static boolean deMira(String sprite) {
        return sprite.startsWith("bow") || sprite.equals("laser");
    }

    private static final String RECURSO = "/com/retronova/resources/poses.properties";
    private static final Path EM_DISCO =
            Path.of("src", "main", "resources", "com", "retronova", "resources", "poses.properties");

    private static final long INTERVALO = 500;

    private static Map<String, Pose> poses = new HashMap<>();
    private static long carimbo = -1;
    private static long proximaConferida;
    private static boolean carregado;

    private Poses() {
    }

    /** A pose desta arma, ou null quando ela nao foi ajustada no editor. */
    public static Pose de(String sprite) {
        conferir();
        return poses.get(sprite);
    }

    private static synchronized void conferir() {
        long agora = System.currentTimeMillis();
        if (carregado && agora < proximaConferida) {
            return;
        }
        proximaConferida = agora + INTERVALO;
        try {
            if (Files.isRegularFile(EM_DISCO)) {
                long visto = Files.getLastModifiedTime(EM_DISCO).toMillis();
                if (visto == carimbo) {
                    return;
                }
                carimbo = visto;
                try (InputStream in = Files.newInputStream(EM_DISCO)) {
                    ler(in);
                }
                carregado = true;
                return;
            }
        } catch (IOException naoDeu) {
            System.err.println("Poses: nao consegui ler " + EM_DISCO + ": " + naoDeu);
        }
        if (carregado) {
            return;                     // ja veio do jar; nao ha o que reconferir
        }
        carregado = true;
        try (InputStream in = Poses.class.getResourceAsStream(RECURSO)) {
            if (in != null) {
                ler(in);
            }
        } catch (IOException naoDeu) {
            System.err.println("Poses: nao consegui ler o recurso: " + naoDeu);
        }
    }

    private static void ler(InputStream in) throws IOException {
        Properties p = new Properties();
        p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        Map<String, Pose> novo = new HashMap<>();
        for (String chave : p.stringPropertyNames()) {
            String[] campos = p.getProperty(chave).split(",");
            // Cinco campos e o formato antigo, de antes do espelho. Aceitar os
            // dois evita ter de reescrever o arquivo inteiro a cada campo novo.
            if (campos.length != 5 && campos.length != 6
                    && campos.length != 8 && campos.length != 10) {
                System.err.println("Poses: linha invalida em " + chave);
                continue;
            }
            try {
                novo.put(chave.trim(), new Pose(
                        Integer.parseInt(campos[0].trim()),
                        Integer.parseInt(campos[1].trim()),
                        Integer.parseInt(campos[2].trim()),
                        Integer.parseInt(campos[3].trim()),
                        Integer.parseInt(campos[4].trim()),
                        campos.length >= 6 && Integer.parseInt(campos[5].trim()) != 0,
                        campos.length >= 8 ? Integer.parseInt(campos[6].trim()) : GOLPE_GRAUS,
                        campos.length >= 8 ? Integer.parseInt(campos[7].trim()) : GOLPE_PX,
                        campos.length == 10 ? Integer.parseInt(campos[8].trim()) : SEM_BOCA,
                        campos.length == 10 ? Integer.parseInt(campos[9].trim()) : SEM_BOCA));
            } catch (NumberFormatException naoEhNumero) {
                System.err.println("Poses: numero invalido em " + chave);
            }
        }
        poses = novo;
    }
}
