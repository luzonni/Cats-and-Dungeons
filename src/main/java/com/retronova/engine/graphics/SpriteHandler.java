package com.retronova.engine.graphics;

import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.FlipException;
import com.retronova.engine.exceptions.OutOfPixels;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class SpriteHandler {

    private final BufferedImage SHEET;
    private final boolean loss;
    private final int scale;

    public static BufferedImage getSprite(String module, String sprite, int scale) {
        return new SpriteHandler(module, sprite, scale).getSHEET();
    }

    public SpriteHandler(String module, String sprite, int scale) {
        this.scale = scale;
        boolean loss;
        BufferedImage sheet;
        String path = Engine.resPath + module + "/" + sprite + ".png";
        try {
            sheet = ImageIO.read(Objects.requireNonNull(getClass().getResource(path)));
            loss = false;
        }catch (Exception e) {
            sheet = sadImage();
            loss = true;
        }
        if(scale > 1) {
            sheet = setScale(sheet, sheet.getWidth()*scale, sheet.getHeight()*scale);
        }
        this.loss = loss;
        this.SHEET = sheet;
    }

    private static BufferedImage setScale(BufferedImage img, int width, int height) {
        BufferedImage NewImg = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
        NewImg.getGraphics().drawImage(img, 0, 0, width, height, null);
        return NewImg;
    }

    private BufferedImage sadImage() {
        BufferedImage sad = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_RGB);
        Graphics g = sad.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 6 * scale, 6 * scale);
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, 8 * scale, 8 * scale);
        g.fillRect(8 * scale, 8 * scale, 8 * scale, 8 * scale);
        g.dispose();
        return sad;
    }

    public BufferedImage getSubimage(int x, int y, int width, int heigth) {
        if(loss)
            return SHEET;
        x*=scale;
        y*=scale;
        width*=scale;
        heigth*=scale;
        if(x < 0 || x + width > SHEET.getWidth() || y < 0 || y + heigth > SHEET.getHeight()) {
            throw new OutOfPixels("the subimage is out of image pixels");
        }
        return SHEET.getSubimage(x, y, width, heigth);
    }

    public BufferedImage getSpriteWithIndex(int indexX, int indexY) {
        if(loss)
            return SHEET;
        int size = 16 * scale;
        int x = indexX * size;
        int y = indexY * size;
        if(x < 0 || x + size > SHEET.getWidth() || y < 0 || y + size > SHEET.getHeight()) {
            throw new OutOfPixels("the subimage is out of image pixels");
        }
        return SHEET.getSubimage(x, y, size, size);
    }
    public BufferedImage getSpriteWithIndex(int indexX, int indexY, int size) {
        if(loss)
            return SHEET;
        size *= scale;
        int x = indexX * size;
        int y = indexY * size;
        if(x < 0 || x + size > SHEET.getWidth() || y < 0 || y + size > SHEET.getHeight()) {
            System.err.println("Erro getBAGULHO" + size);
            throw new OutOfPixels("the subimage is out of image pixels");
        }
        return SHEET.getSubimage(x, y, size, size);
    }

    public BufferedImage[] getSprites(int prefix) {
        int length = this.getWidth() / 16;
        BufferedImage[] sprites = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            sprites[i] = this.getSpriteWithIndex(i, prefix);
        }
        return sprites;
    }

    public int getWidth() {
        return this.SHEET.getWidth()/scale;
    }

    public int getHeight() {
        return this.SHEET.getHeight() / scale;
    }

    public int getScale() {
        return this.scale;
    }

    public BufferedImage getSHEET() {
        return this.SHEET;
    }

    /**
     * Espelhos ja feitos, por imagem de origem: [h][v] com 0 = normal, 1 = invertido.
     *
     * O flip era chamado no render de cada bicho, a cada quadro, e criava uma
     * imagem nova toda vez. Com uma horda a 165 FPS isso eram milhares de imagens
     * por segundo para o coletor de lixo — e o tranco periodico dele era o "lag".
     * Os quadros das folhas sao sempre os mesmos objetos, entao o espelho de cada
     * um so precisa existir uma vez. Fraco na chave para folhas descartadas sairem.
     */
    private static final java.util.Map<BufferedImage, BufferedImage[][]> ESPELHOS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static BufferedImage flip(BufferedImage image, int horizontal, int vertical) {
        if(horizontal == 0 || vertical == 0)
            throw new FlipException("Valores precisam ser 1 (default) ou -1 (invertido)");
        if (horizontal == 1 && vertical == 1) {
            return image;
        }
        BufferedImage[][] feitos = ESPELHOS.computeIfAbsent(image, k -> new BufferedImage[2][2]);
        int h = horizontal < 0 ? 1 : 0;
        int v = vertical < 0 ? 1 : 0;
        if (feitos[h][v] == null) {
            feitos[h][v] = espelhar(image, horizontal, vertical);
        }
        return feitos[h][v];
    }

    private static BufferedImage espelhar(BufferedImage image, int horizontal, int vertical) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage flippedImage = new BufferedImage(width, height, image.getType());
        Graphics2D g = flippedImage.createGraphics();
        AffineTransform transform = AffineTransform.getScaleInstance(vertical, horizontal);
        int translateX = vertical < 0 ? -width : 0;
        int translateY = horizontal < 0 ? -height : 0;
        transform.translate(translateX, translateY);
        g.drawImage(image, transform, null);
        g.dispose();
        return flippedImage;
    }

}
