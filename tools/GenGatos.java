import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Desenha os tres gatos jogaveis com a construcao do gato vendedor.
 *
 * POR QUE O VENDEDOR E A REFERENCIA. Ele e a arte mais bem resolvida do jogo, e
 * resolve coisas que os tres jogaveis nao resolviam:
 *
 *  - RABO NO CHAO. Nos jogaveis o rabo saia do flanco esquerdo, na altura do
 *    tronco, apontando para fora — ocupava a lateral inteira e lia como um braco
 *    ou uma mancha. No vendedor ele se enrola no canto de baixo, encostado na
 *    base do sprite, que e onde o rabo de um gato sentado fica.
 *  - BARRIGA CLARA. Focinho, peito e a frente das patas dianteiras em um tom
 *    claro, separados do resto da pelagem. E a marcacao mais comum em gato de
 *    verdade, e no jogo e o que faz o bicho ter rosto: sem ela o Azrael era uma
 *    silhueta preta sem feicao nenhuma contra um cenario escuro.
 *  - CORPO JUNTO. O vendedor nao tem duas pernas separadas na linha de baixo;
 *    tem duas patas dianteiras juntas. Fica compacto, como bicho agachado, em
 *    vez de boneco em pe.
 *  - SEM PATA NA LATERAL. Aquele pixel de pata no flanco saiu. O item passa a
 *    ser desenhado na linha das patas dianteiras, que e onde a mao de fato esta.
 *
 * O QUE NAO VEM DO VENDEDOR e a identidade de cada um. Nenhuma cor e inventada
 * aqui: todas sao AMOSTRADAS da arte antiga de cada gato — pelagem, orelha,
 * olho, nariz, veste. O unico tom novo em cena e o creme da barriga, e ele e o
 * creme do proprio vendedor, ja presente no jogo. Entao o Muffin continua sendo
 * o cinza de cachecol verde, o Azrael o preto de coroa e capa roxa, e o Finn o
 * gato da sorte de coleira e sino.
 *
 * AS TRES ANIMACOES
 *
 *  - PARADO: quatro quadros, descanso e respiro. O respiro e a mesma tecnica do
 *    vendedor — a cabeca AFUNDA um pixel no corpo em vez de o bicho inteiro
 *    subir e descer, e a metade de baixo (patas e rabo) nao se mexe. E o que
 *    diferencia respirar de pular.
 *  - ANDANDO: quatro quadros, contato e passada. So esta folha troca a base
 *    cheia do vendedor por duas patas separadas: uma delas sai do chao a cada
 *    passada, abrindo um entalhe no contorno de baixo, e o corpo sobe um pixel
 *    junto. Entalhe, e nao troca de cor — no Azrael pelagem e contorno sao o
 *    mesmo preto, e no Finn a pata e quase da cor do corpo, entao so a silhueta
 *    le nos tres.
 *  - DORMINDO: quatro quadros. A cabeca afunda dois pixels, os olhos fecham numa
 *    linha e a respiracao fica mais lenta ainda. E o estado que o vendedor ja
 *    tinha e os jogaveis nao: os "Z" subindo sem o bicho mudar de pose era meia
 *    piada.
 *
 * Uso: java tools/GenGatos.java [--preview]
 */
public class GenGatos {

    private static final String DESTINO =
            "src/main/resources/com/retronova/resources/sprites/objects/player/";
    /** Arte antiga, so como fonte de cor. Ver {@link #paleta}. */
    private static final String ANTIGA = "tools/assets/";

    /**
     * O gato em repouso, em simbolos.
     *
     * O ENQUADRAMENTO E O DO VENDEDOR, coluna por coluna. Isto foi um erro na
     * primeira versao: a cabeca veio da arte antiga, que e centrada na coluna 8,
     * e o corpo veio do vendedor, que e centrado na 7 — a cabeca ficava um pixel
     * a direita do tronco. Agora as duas metades usam o mesmo eixo, e cada linha
     * ocupa exatamente as mesmas colunas que a linha correspondente do vendedor:
     * orelhas em 3,4 e 10,11; cabeca de 0 a 14; queixo de 1 a 13; colar de 3 a
     * 11; tronco de 4 a 10; rabo de 11 a 14.
     *
     *   #  contorno       o  pelagem       s  pelagem escura
     *   f  dentro da orelha               F  penugem da bochecha
     *   m  creme (focinho, peito, patas)  M  creme na sombra
     *   E  olho           n  vinco do focinho    N  nariz
     *   A  veste          B  veste na sombra     C  metal (sino, fecho)
     *   t  rabo           T  rabo na sombra
     */
    private static final String[] REPOUSO = {
            "...##.....##....",
            "..#of#####fo#...",
            "..#ofososofo#...",
            "..#ooooooooo#...",
            ".#ooooomooooo#..",
            "#FooEommmoEooF#.",
            ".#moEmmnmmEom#..",
            "#FmmmmnNnmmmmF#.",
            ".##mmmmmmmmm##..",
            "...#AABBBAA#....",
            "....##ACA##.....",
            "....#AAmAA#.....",
            "....#AAmAA#..##.",
            "...#AAmmmAA##Tt#",
            "...#ABmsmBATttT#",
            "...#AsmsmsA####.",
    };

    /**
     * Linha que some quando a cabeca afunda.
     *
     * O respiro nao desloca o sprite inteiro: as linhas de 0 ate esta descem um
     * pixel e esta e engolida, entao o que esta abaixo — patas e rabo — fica
     * exatamente onde estava. E o truque do vendedor, e e o unico jeito de
     * afundar a cabeca sem cortar a ponta das orelhas na linha zero.
     */
    private static final int CORTE = 11;

    /**
     * A linha do chao na CAMINHADA, com as duas patas separadas.
     *
     * Parado e dormindo mantem a base cheia do vendedor — bicho sentado nao tem
     * pe aparecendo. Andando precisa deles: sem duas patas destacadas nao ha o
     * que alternar, e era esse o problema da primeira versao. Ali a passada
     * trocava a COR de dois pixels dentro do corpo, e cor nao le em dois dos tres
     * gatos: no Finn o creme da pata e quase o creme do corpo, e no Azrael
     * pelagem e contorno sao o mesmo preto. O que aparece contra o cenario e a
     * silhueta, entao e a silhueta que tem de mudar.
     */
    private static final String ANDANDO_14 = "...#AmmsmmATttT#";
    private static final String ANDANDO_15 = "...##mm#mm#####.";

    /** Colunas do chao de cada pata, com o contorno delas. */
    private static final int[] PATA_ESQ = {3, 4, 5, 6}, PATA_DIR = {8, 9, 10};

    /** Um gato: o nome do arquivo, a paleta e ate onde a veste cobre. */
    record Gato(String nome, int vesteAte, boolean coroa, boolean barriga, boolean bandana) { }

    private static final Gato[] GATOS = {
            // O cachecol do Muffin desce ate o peito e para; a capa do Azrael vai
            // ate o chao, como a do vendedor; a coleira do Finn e so uma linha.
            new Gato("muffin", 13, false, true, false),
            // O Azrael NAO leva a barriga clara. A graca dele e ser preto
            // inteiro: quem da feicao ao rosto sao o olho dourado e a coroa, e
            // uma mancha creme no meio do peito tirava dele a unica coisa que o
            // separa dos outros dois de longe.
            new Gato("azrael", 15, true, false, false),
            // O Finn usa BANDANA, e nao coleira. Uma linha de vermelho sumia
            // quando a cabeca afundava no respiro, e ele virava um bicho branco
            // sem nada — do lado de um cinza de cachecol e de um preto de capa,
            // parecia o irmao que ficou sem figurino. O pano desce ate o peito e
            // termina em ponta, entao ha vermelho sobrando para a cabeca cobrir.
            new Gato("finn", 11, false, true, true),
    };

    public static void main(String[] a) throws Exception {
        boolean preview = a.length > 0 && a[0].equals("--preview");
        for (Gato gato : GATOS) {
            Map<Character, Integer> cor = paleta(gato);

            BufferedImage[] parado = {
                    quadro(REPOUSO, cor, gato, 0), quadro(REPOUSO, cor, gato, 0),
                    quadro(afundar(REPOUSO, 1), cor, gato, 0), quadro(afundar(REPOUSO, 1), cor, gato, 0),
            };
            // Contato com o corpo BAIXO, passada com o corpo alto — a subida de um
            // pixel esta na arte, e nao num deslize de tela: assim o rabo e a pata
            // apoiada ficam no chao enquanto o resto sobe, que e o que acontece de
            // verdade. Deslizando o sprite inteiro, o gato subia com rabo e tudo.
            String[] emPe = emPe(REPOUSO);
            String[] contato = afundar(emPe, 1);
            BufferedImage[] andando = {
                    // O RABO ACOMPANHA A PASSADA. Sobe num contato, desce no
                    // outro, entao ele completa um ciclo a cada duas passadas —
                    // metade da cadencia das patas, que e como um rabo de verdade
                    // se comporta: ele responde ao corpo, nao a cada pisada.
                    quadro(rabo(contato, true), cor, gato, 0),
                    quadro(rabo(passada(emPe, true), true), cor, gato, 0),
                    quadro(rabo(contato, false), cor, gato, 0),
                    quadro(rabo(passada(emPe, false), false), cor, gato, 0),
            };
            String[] sono = fecharOlhos(REPOUSO);
            BufferedImage[] dormindo = {
                    quadro(afundar(sono, 1), cor, gato, 1), quadro(afundar(sono, 1), cor, gato, 1),
                    quadro(afundar(sono, 2), cor, gato, 1), quadro(afundar(sono, 2), cor, gato, 1),
            };

            conferirComVendedor(gato, parado[0]);
            gravar(gato, "idle", parado, preview);
            gravar(gato, "walking", andando, preview);
            gravar(gato, "sleeping", dormindo, preview);
        }
        if (preview) {
            System.out.println("preview em gatos_preview.png (nada gravado em resources)");
            contatoDeProva();
        }
    }

    /**
     * Confere a silhueta contra a do vendedor, linha por linha.
     *
     * Existe porque ja errei isto uma vez: a cabeca veio da arte antiga, o corpo
     * veio do vendedor, e o gato saiu com a cabeca um pixel a direita do tronco.
     * Olhar o desenho ampliado nao pega esse tipo de erro — um pixel some no meio
     * de um bicho de dezesseis. Comparar as colunas ocupadas pega.
     *
     * Falha alto e nao silenciosamente: se alguem mexer na grade e sair do
     * enquadramento do vendedor, o gerador diz em qual linha.
     */
    static void conferirComVendedor(Gato gato, BufferedImage q) throws Exception {
        BufferedImage v = ImageIO.read(new File(
                "src/main/resources/com/retronova/resources/sprites/objects/npc/seller.png"));
        StringBuilder erros = new StringBuilder();
        for (int y = 0; y < 16; y++) {
            String a = colunas(v, y), b = colunas(q, y);
            if (!a.equals(b)) {
                erros.append(System.lineSeparator()).append("    linha ").append(y)
                        .append(": vendedor ").append(a).append(", gato ").append(b);
            }
        }
        if (erros.length() > 0) {
            System.out.println("  AVISO " + gato.nome()
                    + " fora do enquadramento do vendedor:" + erros);
        }
    }

    /** Primeira e ultima coluna opaca da linha. */
    static String colunas(BufferedImage im, int y) {
        int a = -1, b = -1;
        for (int x = 0; x < 16; x++) {
            if ((im.getRGB(x, y) >>> 24) != 0) {
                if (a < 0) {
                    a = x;
                }
                b = x;
            }
        }
        return a < 0 ? "vazia" : (a + ".." + b);
    }

    // ------------------------------------------------------------------ paleta

    /**
     * Recolhe as cores de cada gato da arte antiga dele.
     *
     * As coordenadas sao pontos do desenho velho em que se sabe o que esta ali:
     * o meio da testa e pelagem, a linha 1 da orelha e o miolo dela, e por ai.
     * Amostrar em vez de escrever hexadecimal aqui e o que garante que o Muffin
     * continue sendo o cinza que ele era, e nao um cinza parecido.
     */
    static Map<Character, Integer> paleta(Gato gato) throws Exception {
        BufferedImage v = ImageIO.read(new File(ANTIGA + "gato_antigo_" + gato.nome() + ".png"));
        Map<Character, Integer> c = new HashMap<>();
        c.put('#', 0xff000000);
        c.put('o', v.getRGB(7, 3));        // testa
        c.put('s', v.getRGB(7, 2));        // marca da testa
        c.put('f', v.getRGB(5, 1));        // dentro da orelha
        c.put('F', v.getRGB(2, 5));        // penugem da bochecha
        c.put('E', v.getRGB(5, 5));        // olho
        c.put('N', v.getRGB(8, 7));        // nariz
        c.put('A', v.getRGB(5, 9));        // veste
        // A sombra da veste e DERIVADA, nao amostrada: naquele pixel o Azrael
        // tem contorno preto e o Finn ja tem pelagem, entao amostrar dava a cor
        // errada em dois dos tres.
        c.put('B', escurecer(v.getRGB(5, 9), 0.7));
        // O vinco do focinho NAO e amostrado: na arte antiga aquele pixel era
        // quase preto, e sobre o creme da barriga ele virava um bico escuro no
        // meio da cara. No vendedor esse tracinho e uma pelagem escurecida, nao
        // um contorno — e o que o mantem sendo dobra de focinho e nao buraco.
        c.put('n', escurecer(c.get('o'), 0.55));
        // Olho fechado: o proprio olho apagado, e nao o vinco do focinho. No
        // Azrael o vinco e preto sobre pelagem preta, e ele dormia sem cara
        // nenhuma; escurecendo o dourado, a palpebra continua sendo o olho.
        c.put('z', escurecer(c.get('E'), 0.5));
        // O rabo sai da coluna 1, e nao da 2: e ali que ele comecava na arte
        // antiga, e no Finn e a diferenca entre rabo vermelho e rabo branco.
        c.put('t', v.getRGB(1, 12));
        c.put('T', escurecer(v.getRGB(1, 12), 0.7));

        // Creme da barriga: o do vendedor. E o unico tom que nao vem da arte
        // antiga, porque ela simplesmente nao tinha barriga clara.
        BufferedImage vendedor = ImageIO.read(new File(
                "src/main/resources/com/retronova/resources/sprites/objects/npc/seller.png"));
        int creme = vendedor.getRGB(7, 8);
        c.put('m', creme);
        c.put('M', vendedor.getRGB(5, 14));

        // O Finn ja e creme: a barriga dele precisa ser mais clara que o corpo,
        // senao o desenho inteiro vira uma mancha so.
        if (c.get('o').equals(creme)) {
            c.put('m', clarear(creme, 14));
            c.put('M', creme);
        }

        // Metal: o sino do Finn e o fecho do Azrael. Quem nao tem fica com a
        // propria veste no lugar, e o pixel some dentro dela.
        c.put('C', metal(v, c.get('A')));
        return c;
    }

    /** Amarelo mais claro presente no sprite: sino ou fecho. Null vira a veste. */
    static int metal(BufferedImage v, int veste) {
        int melhor = veste;
        double maior = -1;
        for (int y = 0; y < v.getHeight(); y++) {
            for (int x = 0; x < 16; x++) {
                int p = v.getRGB(x, y);
                if ((p >>> 24) == 0) {
                    continue;
                }
                int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                if (r < 180 || g < 140 || b > 120) {
                    continue;              // so amarelo forte
                }
                double lum = r + g;
                if (lum > maior) {
                    maior = lum;
                    melhor = p;
                }
            }
        }
        return melhor;
    }

    static int escurecer(int cor, double fator) {
        int r = (int) (((cor >> 16) & 255) * fator);
        int g = (int) (((cor >> 8) & 255) * fator);
        int b = (int) ((cor & 255) * fator);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    static int clarear(int cor, int passo) {
        int r = Math.min(255, ((cor >> 16) & 255) + passo);
        int g = Math.min(255, ((cor >> 8) & 255) + passo);
        int b = Math.min(255, (cor & 255) + passo);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ poses

    /**
     * Afunda a cabeca {@code n} pixels dentro do corpo.
     *
     * As linhas de 0 ate {@link #CORTE} descem, e as de baixo ficam. A linha
     * engolida e a de cima do corte, que e a menos informativa do tronco.
     */
    static String[] afundar(String[] base, int n) {
        String[] o = base.clone();
        for (int i = 0; i < n; i++) {
            String[] passo = o.clone();
            for (int y = CORTE; y > 0; y--) {
                passo[y] = o[y - 1];
            }
            passo[0] = "................";
            o = passo;
        }
        return o;
    }

    /**
     * Levanta uma das patas dianteiras do chao.
     *
     * A pata some da ultima linha e o corpo fica apoiado so na outra: e um
     * ENTALHE no contorno de baixo, visivel contra qualquer fundo e em qualquer
     * paleta. Foi assim que a arte antiga fazia — ela escondia uma perna inteira
     * por quadro — e era por isso que aquela caminhada lia.
     */
    static String[] passada(String[] base, boolean esquerda) {
        char[][] g = grade(base);
        for (int x : esquerda ? PATA_DIR : PATA_ESQ) {
            g[15][x] = '.';
        }
        return desfazer(g);
    }

    /** Linhas que o rabo ocupa na grade, contorno incluido. */
    private static final int PRIMEIRA_LINHA_DO_RABO = 11;

    /**
     * Levanta ou abaixa a ponta do rabo um pixel.
     *
     * O rabo era a unica parte do gato que nao se mexia andando: o balanco esta na
     * cabeca e nas patas, e ele ficava rigido atras, o que le como pedaco colado.
     * Num sprite deste tamanho basta a PONTA subir e descer um pixel entre os
     * quadros para a coisa toda ganhar vida.
     *
     * MOVE O BLOCO INTEIRO, CONTORNO JUNTO. A primeira versao deslocava so os
     * pixels de rabo e deixava o contorno preto onde estava — a ponta subia e a
     * moldura dela ficava para tras, abrindo buracos na silhueta. Num desenho com
     * contorno fechado como este, o contorno e parte da forma: quem move a forma
     * move a borda dela.
     *
     * Um pixel, e nao dois, e so nas colunas do fim: mexer o rabo todo faria ele
     * parecer solto do corpo, e mais de um pixel viraria chicote. E a mesma
     * economia do respiro do quadro parado, que sobe a cabeca um unico pixel.
     *
     * @param subir true levanta a ponta, false abaixa
     */
    static String[] rabo(String[] base, boolean subir) {
        char[][] g = grade(base);
        int altura = g.length;
        int largura = g[0].length;

        // A coluna mais a esquerda que ainda e ponta: duas colunas antes do fim do
        // rabo. Antes disso o rabo encosta no corpo e nao deve se mexer.
        int fim = -1;
        for (int y = PRIMEIRA_LINHA_DO_RABO; y < altura; y++) {
            for (int x = largura - 1; x >= 0; x--) {
                if (g[y][x] == 'T' || g[y][x] == 't') {
                    fim = Math.max(fim, x);
                    break;
                }
            }
        }
        if (fim < 0) {
            return base;
        }
        int comeco = Math.max(0, fim - 1);

        char[][] o = grade(base);
        for (int x = comeco; x < largura; x++) {
            for (int y = PRIMEIRA_LINHA_DO_RABO; y < altura; y++) {
                o[y][x] = '.';
            }
        }
        for (int x = comeco; x < largura; x++) {
            for (int y = PRIMEIRA_LINHA_DO_RABO; y < altura; y++) {
                int destino = subir ? y - 1 : y + 1;
                if (destino < PRIMEIRA_LINHA_DO_RABO || destino >= altura) {
                    continue;
                }
                if (g[y][x] != '.') {
                    o[destino][x] = g[y][x];
                }
            }
        }
        return desfazer(o);
    }

    /** Troca a base cheia do vendedor pela base de duas patas da caminhada. */
    static String[] emPe(String[] base) {
        char[][] g = grade(base);
        g[14] = ANDANDO_14.toCharArray();
        g[15] = ANDANDO_15.toCharArray();
        return desfazer(g);
    }

    /**
     * Fecha os olhos: risco horizontal no lugar do bloco vertical.
     *
     * O olho aberto e um bloco de dois pixels em pe; o fechado tem de ser DEITADO,
     * senao continua lendo como olho aberto e escuro. A linha de cima vira
     * pelagem e a de baixo cresce um pixel para dentro do rosto.
     */
    static String[] fecharOlhos(String[] base) {
        char[][] g = grade(base);
        for (int y = 0; y < 15; y++) {
            for (int x = 1; x < 15; x++) {
                if (g[y][x] != 'E') {
                    continue;
                }
                boolean deCima = g[y + 1][x] == 'E';
                if (deCima) {
                    g[y][x] = 'o';
                } else {
                    g[y][x] = 'z';
                    int paraDentro = x < 8 ? x + 1 : x - 1;
                    g[y][paraDentro] = 'z';
                }
            }
        }
        return desfazer(g);
    }

    static char[][] grade(String[] base) {
        char[][] g = new char[16][];
        for (int y = 0; y < 16; y++) {
            g[y] = base[y].toCharArray();
        }
        return g;
    }

    static String[] desfazer(char[][] g) {
        String[] o = new String[16];
        for (int y = 0; y < 16; y++) {
            o[y] = new String(g[y]);
        }
        return o;
    }

    // ------------------------------------------------------------------ desenho

    /**
     * Pinta um quadro.
     *
     * @param olhos 0 abertos, 1 fechados — dormindo, o olho vira um risco da cor
     *              do vinco, que e como se desenha olho fechado nesta escala.
     */
    static BufferedImage quadro(String[] grade, Map<Character, Integer> cor, Gato gato, int olhos) {
        BufferedImage q = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                char c = grade[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                // Gato sem barriga clara: o creme inteiro vira pelagem.
                if (!gato.barriga()) {
                    if (c == 'm') {
                        c = 'o';
                    } else if (c == 'M') {
                        c = 's';
                    }
                }
                // Onde a veste nao chega, o simbolo dela vira pelagem.
                if (y > gato.vesteAte()) {
                    if (c == 'A') {
                        c = 'o';
                    } else if (c == 'B') {
                        c = 's';
                    } else if (c == 'C' && cor.get('C').equals(cor.get('A'))) {
                        c = 'o';
                    }
                }
                if (c == 'E' && olhos == 1) {
                    c = 'n';
                }
                if (gato.bandana()) {
                    // O no cai sobre o peito: o creme do meio vira pano, e a
                    // ponta desce uma linha abaixo de onde a veste termina.
                    if ((y == 10 || y == 11) && c == 'm') {
                        c = 'A';
                    } else if (y == 12 && x == 7) {
                        c = 'B';
                    }
                }
                Integer p = cor.get(c);
                if (p != null) {
                    q.setRGB(x, y, p);
                }
            }
        }
        if (gato.coroa()) {
            coroa(q, cor);
        }
        return q;
    }

    /**
     * Coroa do Azrael, por cima da cabeca.
     *
     * Redesenhada e nao copiada da arte antiga porque a cabeca afunda: uma coroa
     * colada em coordenada fixa descolaria da testa no quadro de respiro. Aqui
     * ela e assentada na primeira linha de pelagem que existir no quadro.
     */
    static void coroa(BufferedImage q, Map<Character, Integer> cor) {
        int topo = -1;
        for (int y = 0; y < 16 && topo < 0; y++) {
            for (int x = 0; x < 16; x++) {
                if ((q.getRGB(x, y) >>> 24) != 0) {
                    topo = y;
                    break;
                }
            }
        }
        if (topo < 0 || topo + 1 >= 16) {
            return;
        }
        int ouro = cor.get('C');
        // Entre as orelhas, que agora ocupam 3,4 e 10,11: o vao vai de 5 a 9.
        for (int x : new int[]{6, 8}) {
            q.setRGB(x, topo, ouro);
        }
        for (int x = 6; x <= 8; x++) {
            q.setRGB(x, topo + 1, ouro);
        }
    }

    // ------------------------------------------------------------------ saida

    private static final Map<String, BufferedImage> PREVIEW = new LinkedHashMap<>();

    static void gravar(Gato gato, String estado, BufferedImage[] quadros, boolean preview)
            throws Exception {
        BufferedImage folha = new BufferedImage(16 * quadros.length, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = folha.createGraphics();
        for (int i = 0; i < quadros.length; i++) {
            g.drawImage(quadros[i], i * 16, 0, null);
        }
        g.dispose();
        if (preview) {
            PREVIEW.put(gato.nome() + " " + estado, folha);
            return;
        }
        String nome = "player_" + gato.nome() + "_" + estado + ".png";
        ImageIO.write(folha, "png", new File(DESTINO + nome));
        System.out.println("  " + nome);
    }

    /** Folha de contato com tudo ampliado, para conferir antes de gravar. */
    static void contatoDeProva() throws Exception {
        int z = 10, linha = 16 * z + 26;
        BufferedImage o = new BufferedImage(16 * 4 * z + 200, linha * PREVIEW.size() + 20,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = o.createGraphics();
        g.setColor(new Color(0x2a2a30));
        g.fillRect(0, 0, o.getWidth(), o.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setFont(new Font("Consolas", Font.BOLD, 13));
        int y = 16;
        for (Map.Entry<String, BufferedImage> e : PREVIEW.entrySet()) {
            g.setColor(new Color(0xffcc44));
            g.drawString(e.getKey(), 16, y + 12);
            g.drawImage(e.getValue(), 170, y, e.getValue().getWidth() * z, 16 * z, null);
            y += linha;
        }
        g.dispose();
        ImageIO.write(o, "png", new File("gatos_preview.png"));
    }
}
