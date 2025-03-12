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

    public static BufferedImage getImage(BufferedImage sprite, float alpha) {
        BufferedImage newImage = new BufferedImage(sprite.getWidth(), sprite.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) newImage.getGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(sprite, 0, 0, Engine.window);
        g2d.dispose();
        return newImage;
    }

}
