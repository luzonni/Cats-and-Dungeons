package com.retronova.engine.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FontHandler {

    public static final int Game = 0;
    public static final int Septem = 1;

    private static Font[] font;

    /**
     * AS FONTES DERIVADAS, GUARDADAS POR TIPO E TAMANHO. As telas pedem a mesma
     * fonte a cada quadro (o bloco da seleção de gato pedia seis, até seis vezes
     * por quadro), e deriveFont monta um Font novo a cada pedido: lixo por quadro
     * e coleta de lixo a cada dois segundos. Os tamanhos são poucos (múltiplos da
     * escala), então o mapa fica pequeno.
     */
    private static final Map<Long, Font> derivadas = new ConcurrentHashMap<>();

    /**
     * Um contexto de medida só. Ele é imutável, e criar um (com uma transformação
     * nova) a cada getWidth era alocação pura em todo texto medido por quadro.
     */
    private static final FontRenderContext MEDIDA = new FontRenderContext(new AffineTransform(), false, false);

    public static void addFont(String... fonts) {
        font = new Font[fonts.length];
        derivadas.clear();
        for(int i = 0; i < fonts.length; i++) {
            try {
                InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream("com/retronova/resources/fonts/" + fonts[i] + ".ttf");
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
        long chave = ((long) type << 32) | (Float.floatToIntBits(size) & 0xFFFFFFFFL);
        Font f = derivadas.get(chave);
        if (f == null) {
            f = font[type].deriveFont(size);
            derivadas.put(chave, f);
        }
        return f;
    }

    public static int getWidth(String text, Font font) {
        return (int)(font.getStringBounds(text, MEDIDA).getWidth());
    }

    public static int getHeight(String text, Font font) {
        return (int)(font.getStringBounds(text, MEDIDA).getHeight() - 0.29*font.getSize());
    }

    public static int getSize(int type) {
        if(type > font.length-1)
            throw new RuntimeException("Fonte não existe");
        return font[type].getSize();
    }

}
