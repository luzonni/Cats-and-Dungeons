import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * OS ÍCONES DAS CARTAS DE MELHORIA.
 *
 * POR QUE CADA MELHORIA PRECISA DE UM DESENHO PRÓPRIO. A carta de arma tinha a
 * arte do item no meio, e era ela que o jogador reconhecia antes de ler. A carta
 * de melhoria não tem item nenhum — sem um ícone, as três cartas de uma oferta
 * ficariam visualmente idênticas e a escolha viraria leitura de texto pequeno,
 * três vezes, toda sala. Num momento em que o combate acabou de parar, isso é
 * atrito puro.
 *
 * MESMA RECEITA DOS ELEMENTAIS, e de propósito: matriz de texto de nove por
 * nove, contorno calculado em volta do que for corpo, duas tonalidades quando a
 * forma precisa de volume. Se estes fossem desenhados de outro jeito, a mesma
 * carta teria dois vocabulários visuais — o selo de raridade num canto falando
 * uma língua e o ícone do meio falando outra. Ver GenIconesElementais.
 *
 * AS CORES NÃO SÃO DECORATIVAS. Cada uma é a convenção que o gênero já usa:
 * vermelho para vida, laranja para dano, azul-aço para defesa, verde para
 * velocidade, amarelo para cadência, dourado-claro para sorte. Convenção herdada
 * é reconhecida sem ser ensinada.
 *
 *     java tools/GenIconesMelhorias.java
 */
public class GenIconesMelhorias {

    private static final String SAIDA =
            "src/main/resources/com/retronova/resources/sprites/items/melhorias/";

    private static final int LADO = 9;
    private static final int CONTORNO = 0xFF222222;

    /** '#' é a face clara, '+' é a sombra, '.' é o vazio. */
    private record Icone(String nome, int cor, String[] forma) { }

    private static final Icone[] ICONES = {

            // VIGOR — o coração. É o símbolo de vida mais reconhecido que existe;
            // inventar outro seria pedir que o jogador aprendesse o que ele já sabe.
            new Icone("vigor", 0xD9455F, new String[]{
                    ".##...##.",
                    "####.####",
                    "#########",
                    "#####++++",
                    ".###++++.",
                    "..##+++..",
                    "...##+...",
                    "....#....",
                    ".........",
            }),

            // FURIA — a garra. Podia ser uma espada, mas espada é UMA arma e a
            // melhoria vale para todas; a garra é do gato e não promete família
            // nenhuma.
            new Icone("furia", 0xE07A3C, new String[]{
                    "#..#..#..",
                    "##.##.##.",
                    "##.##.##+",
                    "##.##.##+",
                    "##+##+##+",
                    ".######++",
                    "..#####++",
                    "..#####+.",
                    "...####..",
            }),

            // COURO — o escudo. Face clara à esquerda e sombra à direita, que é o
            // que o faz parecer uma placa curva em vez de um brasão chapado.
            new Icone("couro", 0x6E8BA8, new String[]{
                    "####.####",
                    "#####++++",
                    "#####++++",
                    "#####++++",
                    "#####++++",
                    ".####+++.",
                    ".####+++.",
                    "..###++..",
                    "...###...",
            }),

            // PATAS — a pegada. Quatro dedos e a almofada: velocidade dita pelo
            // rastro de quem passou, e não por uma seta, que serviria a qualquer
            // jogo.
            //
            // Os dedos ficaram GRUDADOS à almofada e maiores do que o primeiro
            // desenho. Separados por dois pixels de vazio, como estavam, no tamanho
            // real eles deixavam de ser dedos e viravam quatro quadradinhos soltos
            // pairando sobre uma mancha.
            new Icone("patas", 0x63C56A, new String[]{
                    "##..#..##",
                    "##.###.##",
                    "##.###.##",
                    ".........",
                    "..#####..",
                    ".#######.",
                    "#########",
                    ".##+++++.",
                    "..##+++..",
            }),

            // FRENESI — o raio. Cadência é a única das seis que fala de TEMPO, e o
            // raio é o desenho que o olho lê como "rápido" sem intermediário.
            new Icone("frenesi", 0xE8C84A, new String[]{
                    "....###..",
                    "...###+..",
                    "..###+...",
                    ".######..",
                    "..+####..",
                    "....##+..",
                    "...##+...",
                    "..##+....",
                    ".##......",
            }),

            // INSTINTO — o dado. O trevo foi tentado primeiro e nao sobrevive a
            // nove pixels: quatro lobulos arredondados viram uma mancha, e as tres
            // variantes desenhadas leram como cogumelo, coracao e taca — a do
            // coracao sendo a pior, porque colidia com o icone de vida.
            //
            // O dado passa pelo mesmo teste que decidiu o icone da terra: a
            // SILHUETA. Ele e o unico QUADRADO entre seis formas organicas, entao
            // se separa das outras antes de qualquer detalhe ser lido — e os cinco
            // pontos continuam visiveis no tamanho real. Sorte desenhada como acaso
            // tambem e mais honesta do que sorte desenhada como amuleto: o atributo
            // mexe em esquiva e no peso do que cai, que sao rolagens.
            //
            // Fica CHAPADO, sem a segunda tonalidade que os outros usam. Os demais
            // precisam dela para ter volume; um dado visto de frente e uma face
            // plana, e sombrea-lo so sujou o desenho quando foi tentado.
            new Icone("instinto", 0xF2D479, new String[]{
                    "#########",
                    "#..###..#",
                    "#..###..#",
                    "#########",
                    "###..####",
                    "###..####",
                    "#########",
                    "#..###..#",
                    "#########",
            }),

            // PRESAGIO — a seta para cima. E o simbolo canonico de "isto melhora",
            // e aqui a carta melhora literalmente as proximas cartas: nenhum
            // desenho figurativo diria isso mais rapido.
            //
            // Tambem e a unica forma PONTIAGUDA do conjunto. As outras seis sao
            // massas arredondadas ou um quadrado; um triangulo sobre haste se separa
            // de todas elas na silhueta, que e o teste que decidiu os outros icones.
            // O olho e a estrela cadente foram tentados e viraram borrao no tamanho
            // real.
            //
            // O lilas e mais claro que o roxo do selo epico de proposito: os dois
            // podem aparecer na mesma carta, e duas roxas iguais lado a lado fariam
            // o jogador procurar uma relacao que nao existe.
            new Icone("presagio", 0xC79BEE, new String[]{
                    "....#....",
                    "...###...",
                    "..#####..",
                    ".#######.",
                    "#########",
                    "..###++..",
                    "..###++..",
                    "..###++..",
                    "..###++..",
            }),
    };

    public static void main(String[] args) throws Exception {
        File pasta = new File(SAIDA);
        if (!pasta.isDirectory() && !pasta.mkdirs()) {
            System.err.println("nao consegui criar " + pasta.getPath());
            System.exit(1);
        }
        System.out.println("Icones de melhoria");
        for (Icone i : ICONES) {
            ImageIO.write(desenhar(i), "png", new File(SAIDA + i.nome() + ".png"));
            System.out.println("  sprites/items/melhorias/" + i.nome() + ".png");
        }
    }

    private static BufferedImage desenhar(Icone i) {
        int lado = LADO + 2;
        BufferedImage im = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        boolean[][] corpo = new boolean[lado][lado];
        int sombra = escurecer(i.cor());
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                char c = i.forma()[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                corpo[y + 1][x + 1] = true;
                im.setRGB(x + 1, y + 1, 0xFF000000 | (c == '+' ? sombra : i.cor()));
            }
        }
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                if (corpo[y][x] || !vizinhoDeCorpo(corpo, x, y)) {
                    continue;
                }
                im.setRGB(x, y, CONTORNO);
            }
        }
        return im;
    }

    /** A sombra e a mesma cor a sessenta por cento. Ver GenIconesElementais. */
    private static int escurecer(int rgb) {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.60);
        int g = (int) (((rgb >> 8) & 0xFF) * 0.60);
        int b = (int) ((rgb & 0xFF) * 0.60);
        return (r << 16) | (g << 8) | b;
    }

    private static boolean vizinhoDeCorpo(boolean[][] corpo, int x, int y) {
        int[][] lados = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] d : lados) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && ny >= 0 && ny < corpo.length && nx < corpo[0].length
                    && corpo[ny][nx]) {
                return true;
            }
        }
        return false;
    }
}
