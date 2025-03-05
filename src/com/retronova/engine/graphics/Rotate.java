package com.retronova.engine.graphics;

import com.retronova.engine.Engine;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Rotate {

    public static void draw(BufferedImage sprite, int x, int y, double angle, Point pointImageRotate, Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform at = new AffineTransform();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        at.translate(x, y);
        if(pointImageRotate == null)
            pointImageRotate = new Point(sprite.getWidth()/2, sprite.getHeight()/2);
        at.rotate(angle, pointImageRotate.x, pointImageRotate.y);
        g2.drawImage(sprite, at, Engine.window);
    }

}
