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
    /**
     * Icone da janela: o gato, e mais nada.
     *
     * Era um crachao com so a CABECA recortada dentro dele, e tinha dois
     * problemas. O gato aparecia deslocado para a esquerda porque o recorte era
     * fixo nas colunas 2 a 14 — centro na coluna 8 — e o gato redesenhado e
     * centrado na 7; bastou a arte mudar para o numero envelhecer. E, com a
     * moldura comendo a borda, sobrava pouquissimo pixel para o bicho: na barra
     * de tarefas nao dava para ver o que era.
     *
     * Agora nao ha recorte nem moldura: o quadro inteiro do sprite vai para um
     * icone de 32, ampliado por dois. Nada para sair de sincronia com a arte, e o
     * gato ocupa o icone todo.
     */
    static void icone(File sprite, File saida) throws Exception {
        if (!sprite.isFile()) {
            System.err.println("icone: sprite não encontrado, pulando: " + sprite);
            return;
        }
        BufferedImage folha = ImageIO.read(sprite);
        int lado = folha.getHeight();
        BufferedImage quadro = folha.getSubimage(0, 0, lado, lado);

        // Fator dois, e nao um redimensionamento: em pixel art o unico aumento
        // que nao borra e o inteiro, e dois e o que cabe no tamanho de icone que
        // o Windows pede.
        final int FATOR = 2;
        BufferedImage img = new BufferedImage(lado * FATOR, lado * FATOR,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                int argb = quadro.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                for (int dy = 0; dy < FATOR; dy++) {
                    for (int dx = 0; dx < FATOR; dx++) {
                        img.setRGB(x * FATOR + dx, y * FATOR + dy, argb);
                    }
                }
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
