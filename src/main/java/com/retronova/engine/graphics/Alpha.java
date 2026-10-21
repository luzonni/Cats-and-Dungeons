package com.retronova.engine.graphics;

import com.retronova.engine.Engine;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Alpha {

    public static void draw(BufferedImage sprite, int x, int y, float alpha, Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(sprite, x, y, Engine.window);
        g2d.dispose();
    }

    /**
     * O sprite esmaecido, GUARDADO por imagem e por degrau de opacidade.
     *
     * Criava uma imagem nova a cada chamada, e as chamadas são por quadro: o
     * portal de cada bicho que nasce (ver Chegada), a seda, o gato caindo. Imagem
     * nova não está na placa de vídeo, então cada uma ainda era esticada pela CPU
     * e enviada à GPU, de novo e de novo. Com a horda nascendo em grupos logo
     * fora da tela, um dash em direção a ela trazia dezenas de portais para a
     * tela de uma vez — era a travada. Guardada, a imagem é a mesma a cada quadro,
     * fica na placa, e esticar vira trabalho da GPU.
     *
     * Em {@link #DEGRAUS} degraus: o suficiente para um esmaecimento não parecer
     * escada, e um teto para o cache. Quem recebe a imagem não pode alterá-la —
     * nenhum chamador altera.
     */
    public static BufferedImage getImage(BufferedImage sprite, float alpha) {
        int degrau = degrau(alpha);
        if (degrau >= DEGRAUS) {
            return sprite;
        }
        BufferedImage[] doSprite = ESMAECIDOS.computeIfAbsent(sprite, s -> new BufferedImage[DEGRAUS]);
        BufferedImage pronto = doSprite[degrau];
        if (pronto == null) {
            pronto = esmaecer(sprite, degrau / (float) DEGRAUS);
            doSprite[degrau] = pronto;
        }
        return pronto;
    }

    private static final int DEGRAUS = 16;

    private static final java.util.Map<BufferedImage, BufferedImage[]> ESMAECIDOS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** A opacidade em degraus inteiros, de 0 a DEGRAUS. */
    static int degrau(float alpha) {
        return Math.max(0, Math.min(DEGRAUS, Math.round(alpha * DEGRAUS)));
    }

    private static BufferedImage esmaecer(BufferedImage sprite, float alpha) {
        BufferedImage newImage = new BufferedImage(sprite.getWidth(), sprite.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) newImage.getGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(sprite, 0, 0, Engine.window);
        g2d.dispose();
        return newImage;
    }

}
