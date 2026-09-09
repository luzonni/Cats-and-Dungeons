package com.retronova.engine.graphics;

import com.retronova.engine.Engine;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class Rotate {

    /**
     * ANCORA E MIRA — por que isto existe.
     *
     * Girar sprite por aqui deu errado tres vezes seguidas com o arco, sempre
     * pela mesma dupla de motivos, e nenhum deles se resolve empurrando o
     * desenho alguns pixels para o lado:
     *
     * 1. PIVO. {@code draw} com pivo nulo gira em volta do centro do QUADRO. Mas
     *    o desenho quase nunca esta no centro do quadro: as armas sao assentadas
     *    no fundo dos 16x16, e a flecha, por exemplo, ocupa so de y7 a y15 — o
     *    miolo dela e y11, 3,5 pixels abaixo do miolo do arquivo. Girar em volta
     *    do centro do quadro faz a peca ORBITAR um ponto que nao e ela, num raio
     *    de 3,5 pixels de arte (14 na tela). Como essa orbita acompanha o angulo
     *    da mira, o erro MUDA DE DIRECAO conforme o gato vira — e e por isso que
     *    nenhum deslocamento fixo jamais cancelou.
     *
     * 2. FACING. Todo sprite ja nasce apontando para algum lado, e o codigo
     *    somava {@code PI/4} a esmo, herdado da arte diagonal antiga. A flecha
     *    nova aponta para CIMA e o arco aponta para a DIREITA: com o mesmo
     *    +PI/4 nos dois, um deles sai 45 graus torto de qualquer jeito.
     *
     * {@link #apontar} resolve os dois de uma vez: recebe o ponto onde o miolo
     * do DESENHO deve cair, a direcao da mira e para onde o sprite aponta em
     * repouso, e deriva a rotacao em vez de adivinhar. E o mesmo conceito de
     * pivot de sprite da Unity e de {@code offset} do Godot — ferramenta de
     * verdade guarda a ancora junto da arte justamente para nao cair nisto.
     */
    private static final Map<BufferedImage, Point> MIOLOS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Sprites deitados na horizontal, apontando para a direita (+X). */
    public static final double PARA_DIREITA = 0;

    /** Sprites em pe, apontando para cima. */
    public static final double PARA_CIMA = Math.PI / 2;

    /**
     * Sprites deitados na diagonal, apontando para cima e para a direita.
     *
     * E como vem desenhada a arte dos pacotes de icone de RPG — Shade, Bennyboi
     * e praticamente todo pacote de item 16x16: a diagonal e o que da mais
     * comprimento util dentro de um quadro quadrado. Vale para espadas, machados,
     * arcos e cajados do jogo.
     *
     * MEDIDO, NAO SUPOSTO. Estes pacotes desenham a arma com a PONTA em cima a
     * ESQUERDA e o cabo embaixo a direita — o contrario do que parece a primeira
     * vista. Conferindo pixel a pixel no sword.png: a ponta esta em (1,1) e o
     * punho em (13,14), ou seja, o desenho aponta para -135 graus na tela. O
     * facing e o oposto disso: +3PI/4. Com PI/4, que era o palpite, o arco saia
     * noventa graus fora da mira.
     */
    public static final double DIAGONAL = 3 * Math.PI / 4;

    /**
     * O centro dos pixels opacos, e nao o centro do arquivo.
     *
     * O resultado fica em cache: o sprite ja vem escalado, e varrer 64x64 tres
     * vezes por quadro so para redescobrir a mesma constante seria desperdicio.
     */
    public static Point centroDoDesenho(BufferedImage sprite) {
        Point cache = MIOLOS.get(sprite);
        if (cache != null) {
            return cache;
        }
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = -1, y1 = -1;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) > 16) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        Point miolo = x1 < 0
                ? new Point(sprite.getWidth() / 2, sprite.getHeight() / 2)
                : new Point((x0 + x1) / 2, (y0 + y1) / 2);
        MIOLOS.put(sprite, miolo);
        return miolo;
    }

    /**
     * Desenha o sprite com o miolo do DESENHO em (xAlvo, yAlvo), apontado para
     * {@code mira}.
     *
     * @param facing para onde o sprite aponta parado — {@link #PARA_CIMA} ou
     *               {@link #PARA_DIREITA}. E somado a mira, entao a rotacao sai
     *               da arte, e nao de um numero magico no meio do render.
     */
    public static void apontar(BufferedImage sprite, double xAlvo, double yAlvo,
                               double mira, double facing, Graphics2D g) {
        // MIRANDO PARA A ESQUERDA, ESPELHA — nao gira meia volta.
        //
        // Girar 180 graus poe a arma DE CABECA PARA BAIXO: a pistola fica com o
        // punho para cima, o arco com a curva invertida. E o que se via no laser
        // apontado para tras. Toda arma de mira em jogo 2D resolve isso do mesmo
        // jeito: espelha na horizontal do desenho em vez de rodar.
        //
        // Espelhar troca o lado para onde o desenho aponta, entao o facing muda de
        // sinal junto — sem isso a arma fica certa de pe e errada de direcao.
        if (Math.cos(mira) < 0) {
            sprite = espelharNaVertical(sprite);
            facing = -facing;
        }
        Point miolo = centroDoDesenho(sprite);
        draw(sprite, (int) (xAlvo - miolo.x), (int) (yAlvo - miolo.y),
                mira + facing, miolo, g);
    }

    /** Espelhados ja prontos, para nao refazer a imagem a cada quadro. */
    private static final Map<BufferedImage, BufferedImage> ESPELHADOS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * O mesmo espelho que o apontar usa, para quem precisa acompanhar o desenho.
     *
     * Existe publico porque o calculo da boca da arma tem de espelhar junto: se um
     * espelha e o outro nao, o tiro sai pela coronha quando a mira vai para a
     * esquerda.
     */
    public static BufferedImage espelhadoNaVertical(BufferedImage sprite) {
        return espelharNaVertical(sprite);
    }

    private static BufferedImage espelharNaVertical(BufferedImage sprite) {
        BufferedImage pronto = ESPELHADOS.get(sprite);
        if (pronto != null) {
            return pronto;
        }
        BufferedImage o = new BufferedImage(sprite.getWidth(), sprite.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(sprite, 0, sprite.getHeight(), sprite.getWidth(), -sprite.getHeight(), null);
        g.dispose();
        ESPELHADOS.put(sprite, o);
        return o;
    }

    public static void draw(BufferedImage sprite, int x, int y, double angle, Point pointImageRotate, Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform at = new AffineTransform();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        at.translate(x, y);
        if(pointImageRotate == null)
            pointImageRotate = new Point(sprite.getWidth()/2, sprite.getHeight()/2);
        at.rotate(angle, pointImageRotate.x, pointImageRotate.y);
        g2.drawImage(sprite, at, Engine.window);
        g2.dispose();
    }

    public static void draw(BufferedImage sprite, Rectangle bounds, double angle, Point pointImageRotate, Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scaleX = (double) sprite.getWidth() / bounds.getWidth();
        double scaleY = (double) sprite.getHeight() / bounds.getHeight();
        AffineTransform at = new AffineTransform();
        at.translate(bounds.x, bounds.y);
        if (pointImageRotate == null)
            pointImageRotate = new Point(sprite.getWidth() / 2, sprite.getHeight() / 2);
        double centerX = pointImageRotate.x / scaleX;
        double centerY = pointImageRotate.y / scaleY;
        at.rotate(angle, centerX, centerY);
        at.scale(1 / scaleX, 1 / scaleY);
        g2.drawImage(sprite, at, Engine.window);
        g2.dispose();
    }

}
