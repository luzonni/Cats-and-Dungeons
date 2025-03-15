package com.retronova.engine.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;

public abstract class FontG {

    public static final int Game = 0;
    public static final int Septem = 1;

    private static Font[] font;

    public static void addFont(String... fonts) {
        font = new Font[fonts.length];
        for(int i = 0; i < fonts.length; i++) {
            try {
                InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream("com/retronova/res/source/" + fonts[i] + ".ttf");
                if(stream != null)
                    font[i] = Font.createFont(Font.TRUETYPE_FONT, stream);
            } catch (FontFormatException | IOException e) {
                System.out.println("ERROR!");
                e.printStackTrace();
                System.exit(1);
            }
        }

    }

    public static Font font(int type, float size) {
        if(type > font.length-1)
            throw new RuntimeException("Fonte não existe");
        return font[type].deriveFont(size);
    }

    public static int getWidth(String text, Font font) {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
        return (int)(font.getStringBounds(text, frc).getWidth());
    }

    public static int getHeight(String text, Font font) {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
        return (int)(font.getStringBounds(text, frc).getHeight() - 0.29*font.getSize());
    }

    public static int getSize(int type) {
        if(type > font.length-1)
            throw new RuntimeException("Fonte não existe");
        return font[type].getSize();
    }

}
