package com.retronova.engine.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Scaling {

    public static BufferedImage s(BufferedImage img, double s) {
        int newWidth = (int)(img.getWidth()*s);
        int newHeight = (int)(img.getHeight()*s);

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Aplicando interpolação de alta qualidade para suavizar a imagem
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Desenhando a imagem redimensionada
        g2d.drawImage(img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaledImage;
    }

    public static BufferedImage s(BufferedImage img, int newWidth, int newHeight) {
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Aplicando interpolação de alta qualidade para suavizar a imagem
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Desenhando a imagem redimensionada
        g2d.drawImage(img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaledImage;
    }

}
