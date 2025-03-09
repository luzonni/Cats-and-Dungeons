package com.retronova.engine.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DrawSprite {

    public static BufferedImage draw(BufferedImage sprite, Color color) {
        int width = sprite.getWidth();
        int height = sprite.getHeight();
        int[] rgb = sprite.getRGB(0, 0, width, height, null, 0, width);
        for(int i = 0; i < rgb.length; i++) {
            if(rgb[i] != 0x00000000) {
                int baseColor = rgb[i];
                int R1 = (baseColor >> 16) & 0xFF;
                int G1 = (baseColor >> 8) & 0xFF;
                int B1 = baseColor & 0xFF;
                int A1 = (baseColor >> 24) & 0xFF; // Mantemos a transparência original

                int R2 = (color.getRGB() >> 16) & 0xFF;
                int G2 = (color.getRGB() >> 8) & 0xFF;
                int B2 = color.getRGB() & 0xFF;

                // Somamos as cores, garantindo que não ultrapassem 255 (máximo do canal de cor)
                int R = Math.min(255, R1 + R2);
                int G = Math.min(255, G1 + G2);
                int B = Math.min(255, B1 + B2);

                // Mantemos a transparência da cor base
                int newColor = (A1 << 24) | (R << 16) | (G << 8) | B;
                rgb[i] = newColor;
            }
        }
        BufferedImage newSprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        newSprite.setRGB(0, 0, width, height, rgb, 0, width);
        return newSprite;
    }

}
