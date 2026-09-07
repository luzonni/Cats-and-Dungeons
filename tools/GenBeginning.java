import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Gera a antecâmara da dungeon — a sala inicial.
 *
 *     java tools/GenBeginning.java
 *
 * Utilitário de partida, fora do build. O mapa é conteúdo e deve ser editado à
 * mão num editor de imagem depois; rodar de novo sobrescreve ajustes manuais.
 *
 * O mapa é um PNG em que a COR DE CADA PIXEL identifica um tile, conforme
 * game/objects/tiles/TileIDs.java. As cores precisam bater exatamente.
 *
 * DECISÕES DE DESENHO, conforme documentation/padroes/ARTE-CENARIO.md:
 *
 *  - Paredes horizontais têm DUAS faces: a linha de cima é StoneTop (clara, é a
 *    superfície que recebe luz) e a de baixo é StoneFace (escura, é o lado que se
 *    vê). O contraste entre as duas é o que dá volume. Uma faixa de tile só fica
 *    chapada.
 *  - A face da parede alterna com variantes — vãos, relevo, flâmula. Uma parede
 *    longa de um tile só denuncia a grade na hora.
 *  - O vendedor fica NO CAMINHO entre a entrada e a descida, não num nicho
 *    lateral que o jogador pode nunca visitar.
 *  - A descida é um vão RECORTADO NA PAREDE SUL, com degraus que afunilam até
 *    ele. Os degraus são atravessáveis: a versão anterior usava tijolo, que é
 *    sólido, e a plataforma acabava partindo o salão em dois.
 *  - Tudo que não é sala vira ROCHA, nunca Void. O Void desenha transparente e a
 *    câmera mostrava um vazio preto ao chegar na borda do mapa. Por isso o mapa é
 *    bem maior que o salão: a margem de rocha precisa cobrir a tela inteira nos
 *    cantos, senão o preto reaparece.
 */
public class GenBeginning {

    // Cores de TileIDs.
    static final int ROCHA      = 0xff11161c;
    static final int PISO       = 0xff3a4048;   // laje de dungeon
    static final int PISO_RACHADO = 0xff343a41;
    static final int PISO_LIMO  = 0xff3f4a3c;
    static final int PISO_QUEBRADO = 0xff2f353c;
    static final int STONE_TOP  = 0xff4a6b70;   // face superior da parede
    static final int STONE_FACE = 0xff243840;   // face frontal da parede
    static final int STONE_GRATE  = 0xff1f3038; // face com vãos
    static final int STONE_RELIEF = 0xff2a3f47; // face com relevo
    static final int STONE_BANNER = 0xff33474e; // face com flâmula
    static final int DEGRAU     = 0xff5a7d81;

    /*
     * O salão era 40x38 e sobrava chão vazio por toda parte: a mobília ficava
     * perdida e a travessia virava caminhada. Uma antecâmara é o último ponto
     * seguro antes da descida, não uma praça — ela precisa caber quase inteira na
     * tela para o jogador ler de uma vez o que tem ali.
     *
     * A tela cresceu de novo, mas só de rocha em volta: a câmera só para de
     * mostrar o lado de fora do mundo se o mapa for maior que a janela.
     */
    static final int W = 40, H = 34;

    // Salão, centrado na tela. As paredes ocupam duas linhas nas bordas.
    static final int X1 = 7,  X2 = 32;
    static final int Y1 = 7,  Y2 = 29;
    // Alcova de entrada, ao norte.
    static final int ALC_X1 = 18, ALC_X2 = 22, ALC_Y1 = 3;
    // Eixo do salão. Entrada, caminho, degraus e portão dividem o mesmo centro.
    static final int EIXO = 20;

    static int[][] mapa = new int[W][H];

    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/resources/com/retronova/resources/maps");
        if (!dir.isDirectory()) {
            System.err.println("Rode a partir da raiz do repositório.");
            System.exit(1);
        }
        preencher(ROCHA);
        salao();
        alcova();
        descida();
        desgaste();
        ornamentos();
        gravar(new File(dir, "beginning.png"));
        System.out.println("ok: beginning.png (" + W + "x" + H + ")");
    }

    static void preencher(int cor) {
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                mapa[x][y] = cor;
            }
        }
    }

    static void por(int x, int y, int cor) {
        if (x >= 0 && y >= 0 && x < W && y < H) {
            mapa[x][y] = cor;
        }
    }

    /** Piso do salão e paredes com face dupla nas bordas horizontais. */
    static void salao() {
        for (int x = X1; x <= X2; x++) {
            for (int y = Y1; y <= Y2; y++) {
                mapa[x][y] = PISO;
            }
        }
        paredeHorizontal(X1, X2, Y1);          // parede norte
        paredeHorizontal(X1, X2, Y2 - 1);      // parede sul
        for (int y = Y1; y <= Y2; y++) {       // paredes laterais, duas colunas
            por(X1, y, STONE_TOP);
            por(X1 + 1, y, STONE_FACE);
            por(X2, y, STONE_TOP);
            por(X2 - 1, y, STONE_FACE);
        }
    }

    /** Duas linhas: topo claro em cima, face escura embaixo. */
    static void paredeHorizontal(int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) {
            por(x, y, STONE_TOP);
            por(x, y + 1, STONE_FACE);
        }
    }

    /** Corredor curto ao norte, por onde se chega. */
    static void alcova() {
        for (int x = ALC_X1; x <= ALC_X2; x++) {
            for (int y = ALC_Y1; y <= Y1 + 1; y++) {
                mapa[x][y] = PISO;
            }
        }
        for (int y = ALC_Y1; y <= Y1; y++) {
            por(ALC_X1, y, STONE_TOP);
            por(ALC_X2, y, STONE_TOP);
        }
        paredeHorizontal(ALC_X1, ALC_X2, ALC_Y1);
        // Abre a passagem para o salão.
        for (int x = ALC_X1 + 1; x < ALC_X2; x++) {
            por(x, Y1, PISO);
            por(x, Y1 + 1, PISO);
        }
    }

    /**
     * Vão recortado na parede sul, e degraus afunilando até ele.
     *
     * O afunilamento é o que faz a saída ler como destino: as linhas do chão
     * convergem para um ponto só, e o olho segue. Antes havia uma plataforma
     * retangular no meio da sala, que apenas atrapalhava a travessia.
     *
     * O recorte tem a forma do sprite do portão: três tiles na SOLEIRA, que é a
     * linha do topo da parede, e um só na PASSAGEM, a linha da face. É o que faz
     * o gato entrar por um vão emoldurado de pedra dos dois lados, em vez de
     * pisar sobre um desenho de porta. Atrás não precisa de tampa: a rocha que
     * cerca a sala já é sólida.
     */
    static void descida() {
        for (int x = EIXO - 1; x <= EIXO + 1; x++) {
            por(x, Y2 - 1, PISO);
        }
        por(EIXO, Y2, PISO);

        int[] larguras = {4, 3, 2};
        for (int i = 0; i < larguras.length; i++) {
            for (int x = EIXO - larguras[i]; x <= EIXO + larguras[i]; x++) {
                por(x, Y2 - 7 + i, DEGRAU);
            }
        }
    }

    /**
     * Desgaste do piso: trilha, manchas de ruína e umidade nas paredes.
     *
     * Substitui duas coisas que estavam erradas. A primeira era a trilha: uma
     * faixa retangular de laje rachada, de borda reta, saindo da alcova — dava a
     * impressão de dois pisos diferentes emendados, um na entrada e outro no
     * resto. A segunda era o desgaste, sorteado tile a tile de forma
     * independente: isso espalha confete uniforme, e ruína de verdade é irregular
     * — há cantos intactos e cantos onde tudo cedeu.
     *
     * Agora são três pesos somados, e o tipo de laje sai do peso final:
     *
     *  - TRILHA, que decai com a distância ao eixo por onde todo mundo anda, e
     *    com ruído na borda, para ela desfiar em vez de terminar numa reta.
     *  - MANCHAS, focos de estrago espalhados pelo salão, cada um com raio
     *    próprio.
     *  - UMIDADE, que só existe encostada nas paredes: musgo cresce onde escorre
     *    água, não onde passa gente.
     *
     * A semente é fixa para o mapa sair igual toda vez que o gerador rodar.
     */
    static void desgaste() {
        java.util.Random r = new java.util.Random(20260907L);

        int[][] focos = new int[16][];
        for (int i = 0; i < focos.length; i++) {
            focos[i] = new int[]{
                X1 + r.nextInt(X2 - X1 + 1),
                Y1 + r.nextInt(Y2 - Y1 + 1),
                2 + r.nextInt(4)
            };
        }

        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (mapa[x][y] != PISO) {
                    continue;
                }
                double trilha = trilha(x, y, r);
                double mancha = 0;
                for (int[] f : focos) {
                    double d = Math.hypot(x - f[0], y - f[1]);
                    if (d < f[2]) {
                        mancha = Math.max(mancha, 1 - d / f[2]);
                    }
                }
                double peso = Math.max(trilha, mancha);
                double umidade = umidade(x, y);

                if (umidade > 0 && r.nextDouble() < umidade) {
                    mapa[x][y] = PISO_LIMO;
                } else if (r.nextDouble() < peso) {
                    // Quanto mais castigado o ponto, maior a chance de a laje ter
                    // cedido de vez em vez de apenas trincar. A proporcao e baixa
                    // de proposito: laje lascada e um acento, e repetida demais
                    // deixa de ser estrago e vira textura.
                    mapa[x][y] = (r.nextDouble() < peso * 0.22) ? PISO_QUEBRADO : PISO_RACHADO;
                }
            }
        }
    }

    /**
     * Peso da trilha no ponto: 1 no eixo, zero a partir de cinco tiles.
     *
     * O ruído somado à distância é o que desfia a borda. Sem ele o decaimento é
     * perfeito e a trilha volta a parecer pintada.
     */
    static double trilha(int x, int y, java.util.Random r) {
        if (y < ALC_Y1 || y > Y2 - 8) {
            return 0;
        }
        double distancia = Math.abs(x - EIXO) + r.nextDouble() * 2.2;
        return Math.max(0, 1 - distancia / 5.0);
    }

    /** Musgo só junto de parede, onde escorre água e ninguém pisa. */
    static double umidade(int x, int y) {
        boolean encostado = solido(x - 1, y) || solido(x + 1, y)
                || solido(x, y - 1) || solido(x, y + 1);
        if (!encostado) {
            return 0;
        }
        // Longe da trilha o musgo pega melhor: no meio do caminho o pé o arranca.
        return Math.abs(x - EIXO) < 4 ? 0.06 : 0.22;
    }

    static boolean solido(int x, int y) {
        if (x < 0 || y < 0 || x >= W || y >= H) {
            return true;
        }
        int c = mapa[x][y];
        return c == STONE_TOP || c == STONE_FACE || c == ROCHA
                || c == STONE_GRATE || c == STONE_RELIEF || c == STONE_BANNER;
    }

    /**
     * Variantes na face das paredes.
     *
     * Relevo perto da entrada, onde o jogador chega e olha primeiro: é a marca de
     * quem construiu isto. Flâmula acesa nas laterais, o sinal de que alguém
     * esteve aqui e o único calor da sala. Vãos gradeados na parede sul, junto da
     * descida, sugerindo o que espera do outro lado.
     */
    static void ornamentos() {
        int faceNorte = Y1 + 1, faceSul = Y2;
        por(EIXO - 4, faceNorte, STONE_RELIEF);
        por(EIXO + 4, faceNorte, STONE_RELIEF);
        por(EIXO - 11, faceNorte, STONE_BANNER);
        por(EIXO + 11, faceNorte, STONE_BANNER);
        for (int dx : new int[]{-11, -7, 7, 11}) {
            por(EIXO + dx, faceSul, STONE_GRATE);
        }
        por(EIXO - 3, faceSul, STONE_BANNER);
        por(EIXO + 3, faceSul, STONE_BANNER);
    }

    static void gravar(File saida) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                img.setRGB(x, y, mapa[x][y]);
            }
        }
        ImageIO.write(img, "png", saida);
    }
}
