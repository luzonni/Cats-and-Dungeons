import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * OS SIMBOLOS DOS ELEMENTOS, desenhados aqui.
 *
 * POR QUE DESENHADOS, e nao baixados. A busca por icone elemental em dominio
 * publico deu em dois lugares e nenhum serve: o unico conjunto completo — doze
 * elementos, tamanho certo, desenho bom — e CC-BY, e este projeto e CC0 de ponta a
 * ponta, com a razao registrada em docs/TERCEIROS.md; o unico CC0 tem quatro
 * elementos e e vetor, que nao e pixel art. Ficar sem simbolo tambem nao servia:
 * sem ele, tres cartas lado a lado obrigam a ler tres fichas.
 *
 * COMO ESTES FORAM FEITOS, para que sobrevivam ao tamanho. Cada simbolo e uma
 * matriz de texto de nove por nove, escrita a mao aqui embaixo, e nao um desenho
 * traçado por curva. Nessa escala o que decide se um icone funciona nao e o
 * capricho do traco: e a SILHUETA. Se as formas nao se distinguem em preto e
 * branco, nenhuma cor as salva — por isso cada uma tem um contorno proprio: a
 * chama sobe em ponta, a gota e larga embaixo, o floco e simetrico em seis, a
 * pedra e um bloco chanfrado, o ar e uma espiral aberta e a lendaria e a unica com
 * pontas radiais.
 *
 * A SEGUNDA TONALIDADE, e por que so a terra usa.
 *
 * Cinco dos seis simbolos sao massas de UMA cor so, e funcionam assim porque a
 * forma deles ja carrega tudo: chama, gota, floco e espiral tem contorno proprio.
 * A terra nao tem — o referente dela nao e uma forma, e uma MATERIA. Qualquer
 * silhueta compacta pintada de marrom chapado vira uma bola marrom, que foi
 * exatamente o relato. Uma face clara e uma escura resolvem isso do jeito que
 * pixel art resolve ha quarenta anos: nao desenhando mais coisa, e sim dizendo de
 * onde vem a luz. O escuro e a propria cor do elemento a sessenta por cento, e nao
 * um marrom novo — a paleta continua tendo um marrom de terra so.
 *
 * A COR VEM DE Elemento.cor(), o mesmo valor usado no clarao da varinha, nas
 * particulas e na borda da carta. Escolher outra aqui criaria um segundo vermelho
 * de fogo, e dois vermelhos de fogo e o comeco de uma paleta que ninguem controla.
 *
 *     java tools/GenIconesElementais.java
 */
public class GenIconesElementais {

    private static final String SAIDA =
            "src/main/resources/com/retronova/resources/sprites/items/elementos/";

    /** Lado do icone. Nove cabe num canto de carta sem competir com a arte. */
    private static final int LADO = 9;

    /**
     * Um simbolo: nome do arquivo, cor do miolo e a matriz.
     *
     * Na matriz, '#' e a face iluminada, '+' e a face na sombra e '.' e o vazio. O
     * contorno escuro nao e escrito: ele e calculado, cercando tudo o que for corpo
     * — assim o simbolo continua legivel sobre qualquer fundo, claro ou escuro, que
     * e exatamente o caso de uma carta que pode ter arte de qualquer cor atras.
     */
    private record Simbolo(String nome, int cor, String[] forma) {
    }

    private static final Simbolo[] SIMBOLOS = {
            new Simbolo("fogo", 0xDF864D, new String[]{
                    "....#....",
                    "...##....",
                    "..###....",
                    "..####.#.",
                    ".#####.#.",
                    ".########",
                    "#########",
                    ".#######.",
                    "..#####..",
            }),
            // A GOTA REFEITA: bico fino em cima, barriga larga embaixo, e um
            // brilho recortado no ombro esquerdo.
            //
            // A primeira era um triangulo que virava circulo no meio do caminho, sem
            // cintura nenhuma — de longe ela lia como a pedra da terra com outra
            // cor, que e exatamente o que um simbolo nao pode fazer. Uma gota se
            // reconhece pelo ESTREITAMENTO de cima, e era ele que faltava.
            new Simbolo("agua", 0x79A8D4, new String[]{
                    "....#....",
                    "....#....",
                    "...###...",
                    "...###...",
                    "..#####..",
                    ".##.####.",
                    ".##.####.",
                    "..######.",
                    "...####..",
            }),
            new Simbolo("gelo", 0x85C4D4, new String[]{
                    "#...#...#",
                    ".#..#..#.",
                    "..#.#.#..",
                    "...###...",
                    "#########",
                    "...###...",
                    "..#.#.#..",
                    ".#..#..#.",
                    "#...#...#",
            }),
            // MONTANHA, e nao pedra. A pedra foi tentada e nao da certo.
            //
            // A versao anterior era um octogono preenchido, com um comentario que
            // prometia facetas e chanfros que a matriz nao tinha: todo pixel era
            // '#'. Chapada e compacta, ela era literalmente uma bola marrom, que
            // foi o relato.
            //
            // O problema nao se resolvia ajustando o contorno. Passando as
            // candidatas pelo teste que vale nesta escala — desenhar so a silhueta,
            // em preto, e ver se ainda da para dizer o que e — todas as variantes
            // de pedra reprovaram: seixo, cristal, bloco chanfrado e pedra lascada
            // viram a mesma mancha arredondada, porque pedra NAO TEM silhueta. E o
            // unico dos seis elementos cujo referente e uma materia e nao uma forma.
            //
            // Montanha tem. Base reta, topo recortado, dois picos de alturas
            // diferentes: nada mais no jogo se parece com isso, e ela e reconhecida
            // em preto puro, antes de a cor chegar. E diz a coisa certa sobre este
            // elemento — Elemento.java descreve a terra como a materia que pesa e
            // desce devagar, e montanha e a imagem de massa parada que nao se move.
            //
            // A luz vem da esquerda, como no resto da arte do jogo: a encosta
            // esquerda do pico principal e a face clara, e ela ABRE para baixo,
            // acompanhando o cone. O segundo pico fica inteiro na sombra de
            // proposito — dar a ele a propria face clara criava uma listra fina que
            // no tamanho real vira sujeira. Uma face clara e uma escura, e mais nada.
            new Simbolo("terra", 0xB07E41, new String[]{
                    "...#.....",
                    "..##+....",
                    "..##+.+..",
                    ".###+.++.",
                    ".####++++",
                    "#####++++",
                    "######+++",
                    "######+++",
                    "#########",
            }),
            // Tres rajadas paralelas, com a de cima enrolando na ponta. E como o
            // vento se desenha em quadrinho, e sobretudo e uma silhueta ABERTA: as
            // outras cinco sao massas fechadas, entao o vazio ja distingue esta de
            // longe, antes de a cor chegar.
            new Simbolo("ar", 0xD7FFE4, new String[]{
                    ".........",
                    ".######..",
                    "#.....##.",
                    ".......#.",
                    "..######.",
                    ".........",
                    ".#####...",
                    ".........",
                    "..###....",
            }),
            // ------------------------------------------------------ raridades
            //
            // Estes nao sao elementos, sao DEGRAUS — e por isso a familia deles e
            // outra: todos partem do mesmo losango e vao ganhando pontas. Assim a
            // leitura e imediata mesmo sem cor, porque a diferenca entre eles e a
            // QUANTIDADE de coisa, que e literalmente o que raridade significa.
            new Simbolo("comum", 0x7E849C, new String[]{
                    ".........",
                    ".........",
                    "....#....",
                    "...###...",
                    "..#####..",
                    "...###...",
                    "....#....",
                    ".........",
                    ".........",
            }),
            new Simbolo("raro", 0x4FA3D9, new String[]{
                    ".........",
                    "....#....",
                    "...###...",
                    "..#####..",
                    ".#######.",
                    "..#####..",
                    "...###...",
                    "....#....",
                    ".........",
            }),
            new Simbolo("epico", 0xA05FD0, new String[]{
                    "....#....",
                    "..#.#.#..",
                    "...###...",
                    "..#####..",
                    "#########",
                    "..#####..",
                    "...###...",
                    "..#.#.#..",
                    "....#....",
            }),
            new Simbolo("lendario", 0xFFD850, new String[]{
                    "#...#...#",
                    ".#.###.#.",
                    "..#####..",
                    ".#######.",
                    "#########",
                    ".#######.",
                    "..#####..",
                    ".#.###.#.",
                    "#...#...#",
            }),

            new Simbolo("lendaria", 0xFFD850, new String[]{
                    "....#....",
                    "..#.#.#..",
                    "...###...",
                    ".#.###.#.",
                    "#########",
                    ".#.###.#.",
                    "...###...",
                    "..#.#.#..",
                    "....#....",
            }),
    };

    /** Contorno: o mesmo escuro que os itens do jogo usam. */
    private static final int CONTORNO = 0xFF222222;

    public static void main(String[] args) throws Exception {
        File pasta = new File(SAIDA);
        if (!pasta.isDirectory() && !pasta.mkdirs()) {
            System.err.println("nao consegui criar " + pasta.getPath());
            System.exit(1);
        }
        System.out.println("Simbolos elementais");
        for (Simbolo s : SIMBOLOS) {
            ImageIO.write(desenhar(s), "png", new File(SAIDA + s.nome() + ".png"));
            System.out.println("  sprites/items/elementos/" + s.nome() + ".png");
        }
    }

    /**
     * Pinta o corpo e depois cerca com o contorno.
     *
     * O icone sai um pixel maior que a matriz de cada lado, para o contorno caber
     * sem comer o desenho. Sem esse quadro extra, cercar significaria apagar a
     * borda do simbolo — e nesta escala a borda E metade do simbolo.
     */
    private static BufferedImage desenhar(Simbolo s) {
        int lado = LADO + 2;
        BufferedImage im = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        boolean[][] corpo = new boolean[lado][lado];
        int sombra = escurecer(s.cor());
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                char c = s.forma()[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                corpo[y + 1][x + 1] = true;
                im.setRGB(x + 1, y + 1, 0xFF000000 | (c == '+' ? sombra : s.cor()));
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

    /**
     * A face na sombra: a MESMA cor a sessenta por cento.
     *
     * Multiplicar, e nao escolher um segundo marrom a mao. Um valor escrito aqui
     * seria mais uma cor a manter, e ela sairia do lugar assim que alguem mexesse
     * em Elemento.cor() — que e justamente de onde o claro vem. Sessenta por cento
     * abre distancia suficiente para as duas faces se separarem no tamanho real
     * sem a escura encostar no contorno, que e quase preto.
     */
    private static int escurecer(int rgb) {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.60);
        int g = (int) (((rgb >> 8) & 0xFF) * 0.60);
        int b = (int) ((rgb & 0xFF) * 0.60);
        return (r << 16) | (g << 8) | b;
    }

    /** Contorno so nas quatro direcoes: na diagonal ele engorda o desenho. */
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
