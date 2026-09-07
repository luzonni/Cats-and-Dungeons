import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Portao da dungeon animado e a alavanca que o abre.
 *
 * POR QUE NAO ESTA EM GenKenney. Aquele gerador precisa da pasta do pacote do
 * Kenney, que nao e versionada; este so precisa do que ja esta no repositorio.
 * A moldura do portao e reaproveitada intacta do PNG atual, e a paleta e LIDA
 * dos pixels dele — nenhuma cor e reescrita aqui, entao o portao animado nao tem
 * como sair de tom em relacao ao resto do cenario, nem o dia em que as rampas do
 * GenKenney mudarem.
 *
 * O QUE MUDA NO PORTAO. Ele era um quadro so, com a grade ja recolhida na verga.
 * Agora sao oito: do fechado, com as barras cravadas na soleira, ate exatamente
 * aquele mesmo quadro recolhido, que continua sendo o ultimo. So o miolo de
 * 32x32 e redesenhado; verga, jambas e cornija sao copiadas do original.
 *
 * A ALAVANCA e desenhada do zero porque nao existe nenhuma no projeto, em tres
 * quadros — em pe, no meio do curso e puxada. O ferro sai da mesma rampa de
 * pedra do portao e o cabo sai da madeira da porta de entrada, entao ela ja
 * nasce sendo a mesma oficina que fez o resto.
 *
 * Uso: java tools/GenPortao.java
 */
public class GenPortao {

    private static final String DESTINO =
            "src/main/resources/com/retronova/resources/sprites/objects/furniture/";
    private static final String ORIGINAIS = "tools/assets/";

    /** Quadros da subida. Oito da cerca de meio segundo a 15 ticks por quadro. */
    private static final int QUADROS = 8;

    /** Canto superior esquerdo do miolo dentro da moldura de 48x48. */
    private static final int MIOLO_X = 8, MIOLO_Y = 16, MIOLO = 32;

    /**
     * Onde as barras terminam, em linhas do miolo.
     *
     * ABERTO e o valor que a arte atual ja tinha: a grade recolhida, com as
     * pontas aparecendo logo abaixo da viga. FECHADO e a soleira. O ultimo quadro
     * da animacao e, pixel a pixel, o portao que ja estava no jogo.
     */
    private static final int ABERTO = 8, FECHADO = 31;

    /** Colunas das barras, em pares de dois pixels. Igual a arte original. */
    private static final int[] BARRAS = {2, 8, 14, 20, 26};

    // Paleta lida do proprio PNG. Ver amostrar().
    static int P0, P1, P2, P3, P4, P5, P6, P7, P8;
    static int SOLEIRA, SOLEIRA_LUZ;
    static int MADEIRA, MADEIRA_LUZ;

    public static void main(String[] a) throws Exception {
        BufferedImage portao = original("gate.png");
        BufferedImage entrada = ImageIO.read(new File(DESTINO + "entrance.png"));
        amostrar(portao, entrada);

        BufferedImage folha = new BufferedImage(48 * QUADROS, 48, BufferedImage.TYPE_INT_ARGB);
        for (int t = 0; t < QUADROS; t++) {
            BufferedImage q = copiar(portao);
            // Do fechado para o aberto: o ultimo quadro repete o original.
            double fracao = t / (double) (QUADROS - 1);
            int base = (int) Math.round(FECHADO + (ABERTO - FECHADO) * fracao);
            grade(q, base);
            folha.createGraphics().drawImage(q, t * 48, 0, null);
        }
        ImageIO.write(folha, "png", new File(DESTINO + "gate.png"));
        System.out.println("  gate.png  " + folha.getWidth() + "x48 (" + QUADROS + " quadros)");

        BufferedImage alavanca = alavanca();
        ImageIO.write(alavanca, "png", new File(DESTINO + "lever.png"));
        System.out.println("  lever.png " + alavanca.getWidth() + "x16 (3 quadros)");
    }

    /**
     * Le a arte limpa, preservando uma copia na primeira vez.
     *
     * Sem isso a segunda execucao leria a folha de oito quadros como se fosse a
     * moldura de um quadro so, e o portao viraria uma tira de lixo.
     */
    static BufferedImage original(String nome) throws Exception {
        File guardado = new File(ORIGINAIS + "portao_original_" + nome);
        if (!guardado.exists()) {
            ImageIO.write(ImageIO.read(new File(DESTINO + nome)), "png", guardado);
        }
        return ImageIO.read(guardado);
    }

    /**
     * Recolhe a paleta dos pixels em que ela ficou registrada.
     *
     * Cada coordenada abaixo e um lugar do desenho em que se sabe qual degrau da
     * rampa foi usado — a cornija guarda os claros, o fundo do vao guarda os
     * escuros, as barras guardam os medios. E o jeito de nao ter uma segunda
     * copia da paleta neste arquivo, que sairia de sincronia sem ninguem notar.
     */
    static void amostrar(BufferedImage p, BufferedImage e) {
        P8 = p.getRGB(0, 0);           // cornija, fiada de cima
        P7 = p.getRGB(0, 1);           // cornija, fiada seguinte
        P1 = p.getRGB(0, 14);          // sombra da cornija
        P0 = p.getRGB(0, 15);          // sombra dura
        P3 = p.getRGB(10, 12);         // ombreira sobre o vao
        P5 = p.getRGB(9, MIOLO_Y + 1); // viga da grade
        P6 = p.getRGB(10, MIOLO_Y + 4);// barra
        P4 = p.getRGB(10, MIOLO_Y + 7);// ponta da barra
        P2 = p.getRGB(12, MIOLO_Y + 4);// fundo do vao, faixa de cima
        SOLEIRA_LUZ = p.getRGB(12, MIOLO_Y + 28);
        SOLEIRA = p.getRGB(12, MIOLO_Y + 29);
        MADEIRA = e.getRGB(10, 20);
        MADEIRA_LUZ = e.getRGB(14, 20);
    }

    /**
     * Redesenha o miolo com a grade parando na linha {@code base}.
     *
     * O fundo e refeito antes, porque a grade que estava ali tem de sumir: pintar
     * a nova por cima deixaria a antiga aparecendo por baixo em todos os quadros
     * menos o ultimo.
     */
    static void grade(BufferedImage o, int base) {
        // Fundo do vao: escurece para baixo, nas mesmas tres faixas de sempre.
        for (int y = 0; y < MIOLO; y++) {
            int cor = y < 6 ? P2 : (y < 14 ? P1 : P0);
            faixaM(o, 0, y, MIOLO, 1, cor);
        }
        // Soleira: a laje que o gato pisa ao atravessar.
        faixaM(o, 0, 28, MIOLO, 4, SOLEIRA);
        faixaM(o, 0, 28, MIOLO, 1, SOLEIRA_LUZ);

        // Viga: o alojamento em que a grade se recolhe. Nao se move.
        faixaM(o, 0, 0, MIOLO, 3, P5);
        faixaM(o, 0, 0, MIOLO, 1, P7);

        // Travessas: sobem junto com a grade e somem dentro da viga quando ela
        // termina de subir. Sao elas que fazem a grade ler como uma peca so em
        // vez de barras soltas lado a lado.
        for (int t : new int[]{11, 22}) {
            int y = base - t;
            if (y >= 4) {
                faixaM(o, 0, y, MIOLO, 2, P5);
                faixaM(o, 0, y, MIOLO, 1, P7);
            }
        }

        // Barras, com a ponta em dois tons — e a ponta que diz onde a grade
        // termina, e sem ela a barra parecia cortada.
        for (int x : BARRAS) {
            faixaM(o, x, 3, 2, Math.max(0, base - 4), P6);
            faixaM(o, x, base - 1, 2, 1, P4);
            faixaM(o, x, base, 1, 1, P3);
        }

        // Sombra interna: o miolo e recuado dentro da moldura. Vai por ultimo,
        // como no gerador da moldura, senao a grade passaria por cima dela.
        for (int y = 0; y < MIOLO; y++) {
            pontoM(o, 0, y, P0);
            pontoM(o, MIOLO - 1, y, P0);
        }
        faixaM(o, 0, 0, MIOLO, 1, P0);
    }

    /**
     * Alavanca de chao, 16x16, em tres quadros.
     *
     * DE PAREDE PARA CHAO. A primeira versao era uma chapa pregada na alvenaria,
     * e ficava errada por duas razoes ao mesmo tempo. A parede desta sala so tem
     * duas fileiras — o topo e a face —, entao "pregada na parede" so podia
     * significar a fileira de baixo, longe da altura em que o gato anda. E, vista
     * de frente numa sala desenhada de cima, a chapa apontava para o jogador em
     * vez de para dentro do mundo.
     *
     * No chao os dois problemas somem: a base assenta na laje como qualquer movel
     * da antecamara, o cabo sobe na vertical, e a peca fica ao lado do gato em vez
     * de abaixo dele.
     *
     * Tres quadros: em pe, no meio do curso e puxada. O punho de madeira e a unica
     * coisa que nao e ferro — e o que separa "peca presa no chao" de "coisa que se
     * pega com a mao".
     */
    static BufferedImage alavanca() {
        int[][] pontas = {{7, 1}, {9, 4}, {11, 8}};
        int eixoX = 7, eixoY = 11;
        BufferedImage o = new BufferedImage(16 * pontas.length, 16, BufferedImage.TYPE_INT_ARGB);
        for (int t = 0; t < pontas.length; t++) {
            BufferedImage q = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            // Cabo primeiro, para a base ficar por cima dele: e o encaixe do cabo
            // dentro do bloco que faz a peca parecer uma coisa so.
            linha(q, eixoX + 1, eixoY, pontas[t][0] + 1, pontas[t][1] + 1, P0);
            linha(q, eixoX, eixoY, pontas[t][0], pontas[t][1], P8);
            faixa(q, pontas[t][0], pontas[t][1], 2, 2, MADEIRA);
            ponto(q, pontas[t][0], pontas[t][1], MADEIRA_LUZ);

            // Base: duas faces, como toda pedra desta sala — o topo pega luz e a
            // frente cai na sombra.
            faixa(q, 4, 11, 8, 2, P5);
            faixa(q, 4, 11, 8, 1, P7);
            faixa(q, 4, 13, 8, 3, P3);
            faixa(q, 4, 15, 8, 1, P0);
            // Cantos chanfrados, para o bloco nao ler como caixa.
            ponto(q, 4, 11, P3);
            ponto(q, 11, 11, P3);
            // Boca por onde o cabo sai, e os dois parafusos do berco.
            faixa(q, eixoX - 1, 11, 3, 2, P0);
            ponto(q, 5, 14, P5);
            ponto(q, 10, 14, P5);
            o.createGraphics().drawImage(q, t * 16, 0, null);
        }
        return o;
    }

    static void linha(BufferedImage o, int x0, int y0, int x1, int y1, int cor) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int erro = dx - dy;
        while (true) {
            ponto(o, x0, y0, cor);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int e2 = 2 * erro;
            if (e2 > -dy) {
                erro -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                erro += dx;
                y0 += sy;
            }
        }
    }

    /** Retangulo em coordenadas do MIOLO do portao, ja somando o recuo dele. */
    static void faixaM(BufferedImage o, int x, int y, int w, int h, int cor) {
        faixa(o, x + MIOLO_X, y + MIOLO_Y, w, h, cor);
    }

    static void pontoM(BufferedImage o, int x, int y, int cor) {
        ponto(o, x + MIOLO_X, y + MIOLO_Y, cor);
    }

    static void faixa(BufferedImage o, int x, int y, int w, int h, int cor) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                ponto(o, x + i, y + j, cor);
            }
        }
    }

    static void ponto(BufferedImage o, int x, int y, int cor) {
        if (x < 0 || y < 0 || x >= o.getWidth() || y >= o.getHeight()) {
            return;
        }
        o.setRGB(x, y, cor);
    }

    static BufferedImage copiar(BufferedImage s) {
        BufferedImage o = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
        o.createGraphics().drawImage(s, 0, 0, null);
        return o;
    }
}
