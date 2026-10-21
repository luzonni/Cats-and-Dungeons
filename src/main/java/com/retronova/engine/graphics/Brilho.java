package com.retronova.engine.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Um disco colorido semitransparente — aura de elite, brilho de tiro inimigo —
 * desenhado a partir de uma imagem pronta, e não como forma.
 *
 * {@code fillOval} com suavização é rasterizado pelo Marlin NA CPU a cada chamada,
 * e a máscara resultante ainda é enviada à placa de vídeo. Com a horda cheia isso
 * acontecia para cada elite e para cada caveira no ar, todo quadro: medido, ~12%
 * do tempo da thread do jogo, e é a parte que mais pesa numa máquina mais fraca.
 *
 * Aqui o disco é pintado UMA vez, por cor (com a opacidade já embutida, em
 * degraus), numa imagem pequena; desenhar vira copiar essa imagem esticada, o
 * que o pipeline OpenGL faz na placa. A borda continua suave porque a imagem
 * foi pintada suavizada e é ampliada com filtro.
 */
public final class Brilho {

    /** Lado da imagem do disco. Ampliada até o raio pedido; é luz, não precisa de nitidez. */
    private static final int LADO = 64;
    /** Degraus de opacidade guardados: o suficiente para um pulso não parecer escada. */
    private static final int DEGRAUS = 16;

    private static final Map<Integer, BufferedImage> DISCOS = new ConcurrentHashMap<>();

    private Brilho() {
    }

    /**
     * Desenha um disco de raio {@code raio} centrado em (cx, cy) na cor dada,
     * incluindo a opacidade dela.
     */
    public static void disco(Graphics2D g, double cx, double cy, double raio, Color cor) {
        if (raio <= 0 || cor.getAlpha() <= 0) {
            return;
        }
        BufferedImage img = DISCOS.computeIfAbsent(chave(cor), Brilho::pintar);
        Object antes = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int lado = (int) Math.round(raio * 2);
        g.drawImage(img, (int) Math.round(cx - raio), (int) Math.round(cy - raio), lado, lado, null);
        if (antes != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, antes);
        }
    }

    /** A cor com a opacidade arredondada para um dos degraus: o que vira chave do cache. */
    static int chave(Color cor) {
        int degrau = Math.round(cor.getAlpha() / 255f * DEGRAUS);
        int alfa = Math.max(1, Math.min(255, Math.round(degrau * 255f / DEGRAUS)));
        return (alfa << 24) | (cor.getRGB() & 0xFFFFFF);
    }

    private static BufferedImage pintar(int argb) {
        BufferedImage img = Acelerada.de(new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB));
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(argb, true));
        g.fillOval(0, 0, LADO, LADO);
        g.dispose();
        return img;
    }
}
