import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Gera as folhas 9-slice dos botões de menu, na paleta do jogo.
 *
 * Rodar da raiz do repositório (precisa apenas de JDK 21+, sem compilar nada):
 *
 *     java tools/GenUiAssets.java
 *
 * Saída: src/main/resources/com/retronova/resources/ui/
 *
 * Célula de 24x22 pixels nativos — orelhas nas linhas 0..5, corpo nas 6..21.
 * Recortes 9-slice usados pelo código: esquerda 8, direita 8, topo 12, base 6.
 * Estados lado a lado no eixo X: 0 normal, 1 hover, 2 pressionado.
 *
 * A arte é procedural de propósito: qualquer artista pode repintar os PNGs
 * depois sem tocar em código, desde que preserve as dimensões e os recortes.
 */
public class GenUiAssets {

    static final int CELL_W = 24, CELL_H = 22, STATES = 3;
    static final int EAR_H = 5, EAR_W = 7, EAR_TIP = 2, BODY_TOP = 6;

    // Paleta do jogo, a mesma de engine/graphics/Palette.java. E uma COPIA, e ja
    // envelheceu uma vez: quando a paleta do jogo passou do vermelho para as cores
    // medidas na arte oficial, estes quatro numeros ficaram para tras e a CI
    // acusou os PNG fora de sincronia. Mexeu la, mexe aqui.
    static final int OUTLINE = 0xFF211A2B;
    static final int DEEP    = 0xFF3E4160;
    static final int MAIN    = 0xFF525779;
    static final int LIGHT   = 0xFF7E849C;

    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/resources/com/retronova/resources/ui");
        if (!dir.isDirectory()) {
            System.err.println("Rode a partir da raiz do repositório. Não achei " + dir.getPath());
            System.exit(1);
        }
        write(new File(dir, "button.png"), MAIN, LIGHT, DEEP);       // primário
        write(new File(dir, "button_dark.png"), DEEP, MAIN, OUTLINE); // secundário
        icone(new File("src/main/resources/com/retronova/resources/sprites/objects/player/player_muffin_idle.png"),
              new File(dir, "icon.png"));
        System.out.println("ok: button.png, button_dark.png, icon.png");
    }

    /**
     * Recorta o retrato do gato do sprite atual e grava como ícone da janela.
     *
     * O ícone antigo trazia um desenho de personagem que não existe mais no jogo;
     * derivá-lo do sprite garante que os dois nunca mais se separem — refez o
     * sprite, roda este gerador de novo.
     */
    static void icone(File sprite, File saida) throws Exception {
        if (!sprite.isFile()) {
            System.err.println("icone: sprite não encontrado, pulando: " + sprite);
            return;
        }
        BufferedImage folha = ImageIO.read(sprite);
        BufferedImage quadro = folha.getSubimage(0, 0, 16, 16);

        // Linhas 0..8 são a cabeça; da 9 em diante começa o verde do lenço.
        final int CABECA = 9;
        // Corta a saliência de 1px dos bigodes (x=1 e x=15, presente em só duas
        // linhas): sem ela a cabeça cabe proporcionalmente maior no crachá, que é
        // o que dá presença no tamanho de barra de tarefas.
        final int ESQ = 2, LARG = 13;
        // Lado ÍMPAR de propósito. A cabeça é simétrica em torno de uma coluna
        // única, então tem largura ímpar (13); centralizá-la num crachá de lado
        // par exigiria margem de 1,5px de cada lado, e a divisão inteira jogava
        // 1px à esquerda e 2px à direita — a folga que aparecia na direita.
        final int LADO = 17;

        BufferedImage img = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);

        // Fundo na mesma linguagem dos botões: preenchimento carmesim, contorno
        // navy e realce claro na primeira linha interna. O navy sozinho puxava
        // para azul e destoava do resto da interface.
        boolean[][] crachá = new boolean[LADO][LADO];
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                int distanciaAoCanto = Math.min(x, LADO - 1 - x) + Math.min(y, LADO - 1 - y);
                crachá[x][y] = distanciaAoCanto >= 2;
            }
        }
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                if (crachá[x][y]) {
                    img.setRGB(x, y, bordaDoCrachá(crachá, x, y) ? OUTLINE : MAIN);
                }
            }
        }
        for (int x = 0; x < LADO; x++) {
            if (crachá[x][1] && !bordaDoCrachá(crachá, x, 1)) {
                img.setRGB(x, 1, LIGHT);
            }
        }

        // A cabeça é centralizada contando uma linha extra de queixo (ver abaixo).
        int esquerda = (LADO - LARG) / 2;
        int topo = (LADO - (CABECA + 1)) / 2;

        for (int y = 0; y < CABECA; y++) {
            for (int x = 0; x < LARG; x++) {
                int argb = quadro.getRGB(ESQ + x, y);
                if ((argb >>> 24) != 0) {
                    img.setRGB(esquerda + x, topo + y, argb);
                }
            }
        }

        // No sprite a cabeça não tem contorno inferior — quem fecha o desenho é o
        // lenço logo abaixo. Recortada sozinha, ela ficava aberta no queixo, com
        // o pelo cinza vazando no fundo. Fecha-se a linha sob os pixels de pelo.
        for (int x = 0; x < LARG; x++) {
            int acima = quadro.getRGB(ESQ + x, CABECA - 1);
            boolean pelo = (acima >>> 24) != 0 && (acima & 0xFFFFFF) != 0x000000;
            if (pelo) {
                img.setRGB(esquerda + x, topo + CABECA, 0xFF000000);
            }
        }
        ImageIO.write(img, "png", saida);
    }

    /** Pixel do crachá encostando no vazio ou na moldura: vira contorno. */
    static boolean bordaDoCrachá(boolean[][] m, int x, int y) {
        int lado = m.length;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] v : d) {
            int nx = x + v[0], ny = y + v[1];
            if (nx < 0 || ny < 0 || nx >= lado || ny >= lado) {
                return true;
            }
            if (!m[nx][ny]) {
                return true;
            }
        }
        return false;
    }

    static void write(File out, int fill, int light, int shadow) throws Exception {
        BufferedImage img = new BufferedImage(CELL_W * STATES, CELL_H, BufferedImage.TYPE_INT_ARGB);
        for (int s = 0; s < STATES; s++) {
            cell(img, s * CELL_W, s, fill, light, shadow);
        }
        ImageIO.write(img, "png", out);
    }

    static void cell(BufferedImage img, int ox, int state, int fill, int light, int shadow) {
        boolean[][] sil = new boolean[CELL_W][CELL_H];   // silhueta inteira
        boolean[][] ear = new boolean[CELL_W][CELL_H];   // só as orelhas

        boolean pressed = state == 2;
        int lift = state == 1 ? 1 : (pressed ? -1 : 0);  // hover levanta a orelha
        int top = BODY_TOP + (pressed ? 1 : 0);          // pressionado afunda o corpo
        int bottom = CELL_H - 1 - (pressed ? 1 : 0);     // e perde a linha de sombra

        for (int y = top; y <= bottom; y++) {
            for (int x = 0; x < CELL_W; x++) {
                boolean canto = (x == 0 || x == CELL_W - 1) && (y == top || y == bottom);
                if (!canto) {
                    sil[x][y] = true;
                }
            }
        }

        orelha(sil, ear, 1, top, EAR_H + lift, false);   // esquerda
        orelha(sil, ear, 22, top, EAR_H + lift, true);   // direita

        for (int y = 0; y < CELL_H; y++) {
            for (int x = 0; x < CELL_W; x++) {
                if (!sil[x][y]) {
                    continue;
                }
                int c;
                if (contorno(sil, x, y)) {
                    c = OUTLINE;
                } else if (ear[x][y]) {
                    c = fill;
                } else if (y <= top + (state == 1 ? 2 : 1)) {
                    c = light;                                   // realce do topo
                } else if (y >= bottom - 1) {
                    c = shadow;                                  // sombra da base
                } else {
                    c = fill;
                }
                img.setRGB(ox + x, y, c);
            }
        }

        miolo(img, ox, sil, ear, 2, top, light);
        miolo(img, ox, sil, ear, 21, top, light);
    }

    /**
     * Triângulo de orelha. {@code xBorda} é a coluna do cateto vertical, na borda
     * externa do botão; a hipotenusa desce em direção ao centro.
     */
    static void orelha(boolean[][] sil, boolean[][] ear, int xBorda, int baseY, int altura, boolean direita) {
        for (int i = 0; i < altura; i++) {
            int y = baseY - 1 - i;
            if (y < 0) {
                continue;
            }
            int larg = Math.round(EAR_W + (EAR_TIP - EAR_W) * (i / (float) Math.max(1, altura - 1)));
            for (int k = 0; k < larg; k++) {
                int x = direita ? xBorda - k : xBorda + k;
                if (x < 0 || x >= CELL_W) {
                    continue;
                }
                sil[x][y] = true;
                ear[x][y] = true;
            }
        }
    }

    /** Miolo da orelha, um tom acima do preenchimento. */
    static void miolo(BufferedImage img, int ox, boolean[][] sil, boolean[][] ear, int cx, int baseY, int light) {
        for (int i = 1; i <= 2; i++) {
            int y = baseY - 1 - i;
            if (y < 0 || cx < 0 || cx >= CELL_W) {
                continue;
            }
            if (ear[cx][y] && !contorno(sil, cx, y)) {
                img.setRGB(ox + cx, y, light);
            }
        }
    }

    /** Pixel da silhueta encostando no vazio: vira contorno. */
    static boolean contorno(boolean[][] m, int x, int y) {
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] v : d) {
            int nx = x + v[0], ny = y + v[1];
            if (nx < 0 || ny < 0 || nx >= CELL_W || ny >= CELL_H) {
                return true;
            }
            if (!m[nx][ny]) {
                return true;
            }
        }
        return false;
    }
}
