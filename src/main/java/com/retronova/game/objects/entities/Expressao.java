package com.retronova.game.objects.entities;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * As caras do gato: a mesma arte, com o rosto mexido.
 *
 * POR QUE ASSIM, E NAO COM ARTE NOVA.
 *
 * O pedido foi mudar so os detalhes do rosto, sem trocar o desenho. Entao nada
 * aqui e desenhado a mao: cada expressao e obtida MEXENDO NOS PIXELS QUE JA
 * EXISTEM na folha do personagem. Isso tem tres consequencias boas — vale para os
 * tres gatos sem ninguem redesenhar nada, vale para todo quadro de toda animacao,
 * e continua valendo se a arte do gato mudar amanha.
 *
 * ONDE FICA O ROSTO. Os tres gatos compartilham a mesma construcao de cabeca,
 * medida na folha: cada olho e um bloco de um pixel de largura por DOIS de altura,
 * nas colunas 4 e 10, linhas 5 e 6. O focinho fica entre as colunas 6 e 8, na
 * linha 7. Nao ha adivinhacao nisso; foi lido dos arquivos.
 *
 * O QUE CADA CARA FAZ, e de onde vem a escolha.
 *
 * A pratica corrente de retorno de golpe em jogo 2D — o que os desenvolvedores
 * chamam de "juice" — junta tres coisas no instante do impacto: um CLARAO curto
 * na silhueta, uma DEFORMACAO de um ou dois quadros, e alguma pausa. Nada disso
 * exige arte nova, e e por isso que funciona em jogo de pixel pequeno: o que
 * comunica nao e o detalhe, e a mudanca brusca.
 *
 * Aqui a parte de rosto entra como o terceiro sinal, o mais barato de ler:
 *
 *   GOLPEANDO — o olho se estreita. Some a linha de cima e fica so a de baixo,
 *   que le como olhar baixado: concentracao, nao dor. O focinho nao muda, porque
 *   atacar nao e sofrer.
 *
 *   MACHUCADO — o olho FECHA. A coluna toda sai e no lugar dela entra um traco
 *   horizontal de tres pixels, e o focinho se abre um pixel para baixo.
 *
 * A DIFERENCA ENTRE AS DUAS E DE ORIENTACAO, e nao de tamanho, e isso foi
 * aprendido errando. A primeira versao apenas tirava uma das duas linhas do olho
 * em cada caso — um pixel a mais ou a menos — e na tela nao se via careta nenhuma.
 * Olho fechado em pixel art nao e olho menor: e olho DEITADO. Trocar uma linha
 * vertical por uma horizontal le de longe; encurtar a vertical, nao.
 */
public final class Expressao {

    private Expressao() {
    }

    /** Colunas dos olhos, medidas na folha dos tres gatos. */
    private static final int OLHO_ESQ = 4;
    private static final int OLHO_DIR = 10;

    /** Linhas do olho: a de cima e a de baixo. */
    private static final int OLHO_TOPO = 5;
    private static final int OLHO_BASE = 6;

    /** O focinho, que so a cara de dor mexe. */
    private static final int FOCINHO_Y = 7;
    private static final int FOCINHO_X = 7;

    public enum Cara {
        NORMAL,
        /** Olho semicerrado. A meia altura de fechar, e o foco de quem ataca. */
        GOLPEANDO,
        /** Olho fechado, boca aberta. */
        MACHUCADO
    }

    /**
     * Prontas e guardadas por sprite.
     *
     * O rosto e refeito pixel a pixel, e isso nao pode acontecer sessenta vezes
     * por segundo para cada quadro. Como a chave e a propria imagem de origem, o
     * mapa fraco solta tudo sozinho quando a folha e descartada.
     */
    private static final Map<BufferedImage, Map<Cara, BufferedImage>> PRONTAS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * A reacao inteira: o corpo clareado E o rosto por cima dele.
     *
     * A ORDEM E O CONSERTO. Antes o rosto era montado primeiro e o clarao passava
     * por cima de tudo; com forca total o corpo inteiro vira branco e leva a careta
     * junto — de fora, o que se via era so o gato piscando branco, exatamente como
     * foi relatado. A expressao existia e estava embaixo da tinta.
     *
     * Agora o clarao pinta so o CORPO, e os pixels do rosto sao carimbados depois,
     * com as cores lidas do desenho ORIGINAL. Os dois sinais passam a somar em vez
     * de brigar: o branco diz que aconteceu agora, e o olho fechado, escuro contra
     * esse branco, diz o que aconteceu. E o momento em que a careta fica mais
     * legivel, e nao menos.
     */
    public static BufferedImage reacao(BufferedImage sprite, Cara cara, float forca) {
        if (sprite == null) {
            return null;
        }
        BufferedImage corpo = clarao(sprite, forca);
        if (cara == Cara.NORMAL) {
            return corpo;
        }
        if (corpo == sprite) {
            return de(sprite, cara);          // sem clarao: o caminho barato
        }
        return montar(corpo, cara, sprite);
    }

    public static BufferedImage de(BufferedImage sprite, Cara cara) {
        if (sprite == null || cara == Cara.NORMAL) {
            return sprite;
        }
        Map<Cara, BufferedImage> doSprite =
                PRONTAS.computeIfAbsent(sprite, k -> new java.util.EnumMap<>(Cara.class));
        BufferedImage pronta = doSprite.get(cara);
        if (pronta == null) {
            pronta = montar(sprite, cara, sprite);
            doSprite.put(cara, pronta);
        }
        return pronta;
    }

    /**
     * @param sprite o que vai para a tela (ja clareado, se houver clarao)
     * @param cores  de onde sair as cores do rosto — sempre o desenho original
     */
    /**
     * @param sprite o que vai para a tela (ja clareado, se houver clarao)
     * @param cores  de onde sair as cores do rosto — sempre o desenho original
     */
    private static BufferedImage montar(BufferedImage sprite, Cara cara, BufferedImage cores) {
        BufferedImage o = new BufferedImage(sprite.getWidth(), sprite.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        o.getGraphics().drawImage(sprite, 0, 0, null);

        // A ESCALA VEM DO PROPRIO SPRITE, e este era o defeito que segurou tudo.
        //
        // As coordenadas do rosto foram lidas do arquivo PNG, que tem dezesseis por
        // dezesseis. Mas o que chega aqui NAO e o arquivo: o carregador ja devolve
        // a imagem ampliada pela escala do jogo, quatro vezes maior. Usando as
        // coordenadas do arquivo direto, cada "pixel do olho" caia num ponto de um
        // por um no canto de cima do desenho — longe do rosto, invisivel em
        // qualquer tamanho de tela. A careta estava sendo calculada certa e
        // aplicada no lugar errado, e por isso so o clarao aparecia: aquele varre a
        // imagem inteira e nao depende de coordenada nenhuma.
        //
        // O mesmo raciocinio ja estava em Player.getMao, que divide a largura do
        // sprite pela escala para achar a coluna da pata. Aqui faltava.
        int px = Math.max(1, sprite.getHeight() / QUADRO);
        int lado = QUADRO * px;
        int quadros = Math.max(1, sprite.getWidth() / lado);
        for (int q = 0; q < quadros; q++) {
            int base = q * lado;
            if (cara == Cara.MACHUCADO) {
                apertarOlho(o, cores, base, OLHO_ESQ, px);
                apertarOlho(o, cores, base, OLHO_DIR, px);
                abrirFocinho(o, cores, base, px);
            } else if (cara == Cara.GOLPEANDO) {
                apertarOlho(o, cores, base, OLHO_ESQ, px);
                apertarOlho(o, cores, base, OLHO_DIR, px);
            }
        }
        return o;
    }

    /** Lado do quadro no arquivo, em pixels de arte. */
    private static final int QUADRO = 16;

    /** Pinta um pixel DE ARTE — que na tela e um quadrado de px por px. */
    private static void pintar(BufferedImage im, int base, int ax, int ay, int px, int cor) {
        int x0 = base + ax * px;
        int y0 = ay * px;
        if (x0 < 0 || y0 < 0 || x0 + px > im.getWidth() || y0 + px > im.getHeight()) {
            return;
        }
        for (int y = 0; y < px; y++) {
            for (int x = 0; x < px; x++) {
                im.setRGB(x0 + x, y0 + y, cor);
            }
        }
    }

    /** Le a cor do meio de um pixel de arte. */
    private static int ler(BufferedImage im, int base, int ax, int ay, int px) {
        int x = base + ax * px + px / 2;
        int y = ay * px + px / 2;
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) {
            return 0;
        }
        return im.getRGB(x, y);
    }

    /**
     * FECHA O OLHO TIRANDO O PIXEL DE CIMA. Nada mais que isso.
     *
     * O olho aberto tem dois pixels, um sobre o outro. Fechado e o de baixo
     * sozinho — a palpebra desceu e cobriu a metade de cima. E so.
     *
     * Duas tentativas anteriores erraram por acrescentar coisa. A primeira trocava
     * o olho por um traco de tres pixels de largura, mais largo que o olho aberto,
     * e aquilo lia como o olho se esticando para os lados em vez de fechar. A
     * segunda descia o pixel restante para uma terceira linha, abaixo do olho, o
     * que inventa uma altura que o desenho nao tem. Em pixel art desse tamanho a
     * unica coisa que precisa acontecer e um pixel deixar de existir.
     *
     * Copiar a cor do vizinho, e nao pintar uma cor escolhida, e o que mantem a
     * expressao valida para os tres gatos: o pelo do Muffin e cinza, o do Azrael e
     * quase preto e o do Finn e creme, e nenhum desses valores aparece aqui.
     */
    private static void apertarOlho(BufferedImage im, BufferedImage cores,
                                    int base, int ax, int px) {
        // O SEGUNDO PIXEL, PARA DENTRO. Medido no sprite de DORMINDO, que e o
        // unico lugar em que a arte oficial ja desenha o gato de olho fechado:
        // la cada olho tem DOIS pixels de largura, simetricos em torno do focinho
        // — esquerdo nas colunas 4 e 5, direito nas 9 e 10 — enquanto o olho
        // aberto tem so a coluna de fora. Fechar e portanto tirar o de cima E
        // acrescentar um para o lado do meio da cara. Seguir o desenho existente
        // vale mais que qualquer regra que eu invente: e assim que este gato ja
        // fecha os olhos quando dorme.
        int paraDentro = ax < QUADRO / 2 ? ax + 1 : ax - 1;
        // O de cima vira pele — a pele DO QUE VAI PARA A TELA, para acompanhar o
        // clarao — e o de baixo e repintado com a cor original do olho. Sem esta
        // segunda parte o pixel que sobra herda o branco do clarao e a careta some
        // justamente no quadro do impacto, que e quando ela mais precisa aparecer.
        pintar(im, base, ax, OLHO_TOPO, px, ler(im, base, ax, OLHO_TOPO - 1, px));
        int corDoOlho = ler(cores, base, ax, OLHO_BASE, px);
        pintar(im, base, ax, OLHO_BASE, px, corDoOlho);
        pintar(im, base, paraDentro, OLHO_BASE, px, corDoOlho);
    }

    /** Puxa o focinho um pixel para baixo: a boca aberta da careta de dor. */
    private static void abrirFocinho(BufferedImage im, BufferedImage cores, int base, int px) {
        int cor = ler(cores, base, FOCINHO_X, FOCINHO_Y, px);
        pintar(im, base, FOCINHO_X, FOCINHO_Y, px, cor);
        pintar(im, base, FOCINHO_X, FOCINHO_Y + 1, px, cor);
    }

    /**
     * A silhueta inteira puxada para o branco.
     *
     * E o sinal mais forte e mais barato de impacto em jogo 2D, e o unico que se
     * enxerga mesmo com o bicho no canto da tela e a camera longe: por um instante
     * o corpo todo muda, nao um pixel do rosto. A careta diz O QUE aconteceu; o
     * clarao diz QUANDO. Sem ele, a expressao passava batida — era essa a queixa
     * de nao parecer sincronizado com o golpe.
     *
     * @param forca de 0 (sem efeito) a 1 (silhueta branca)
     */
    public static BufferedImage clarao(BufferedImage sprite, float forca) {
        if (sprite == null || forca <= 0.01f) {
            return sprite;
        }
        int passo = Math.round(Math.min(1f, forca) * NIVEIS_DE_CLARAO);
        if (passo <= 0) {
            return sprite;
        }
        Map<Integer, BufferedImage> doSprite =
                CLAROES.computeIfAbsent(sprite, k -> new java.util.HashMap<>());
        BufferedImage pronto = doSprite.get(passo);
        if (pronto != null) {
            return pronto;
        }
        float f = passo / (float) NIVEIS_DE_CLARAO;
        BufferedImage o = new BufferedImage(sprite.getWidth(), sprite.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                int p = sprite.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) {
                    continue;
                }
                int r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
                r += (int) ((255 - r) * f);
                g += (int) ((255 - g) * f);
                b += (int) ((255 - b) * f);
                o.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        doSprite.put(passo, o);
        return o;
    }

    /** Degraus de clarao guardados. Mais que isso e memoria para um efeito de 6 quadros. */
    private static final int NIVEIS_DE_CLARAO = 4;

    private static final Map<BufferedImage, Map<Integer, BufferedImage>> CLAROES =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Puxa o focinho um pixel para baixo: a boca aberta da careta de dor. */
    private static void abrirFocinho(BufferedImage im, BufferedImage cores, int x) {
        if (x < 0 || x >= im.getWidth() || FOCINHO_Y + 1 >= im.getHeight()) {
            return;
        }
        im.setRGB(x, FOCINHO_Y, cores.getRGB(x, FOCINHO_Y));
        im.setRGB(x, FOCINHO_Y + 1, cores.getRGB(x, FOCINHO_Y));
    }
}
