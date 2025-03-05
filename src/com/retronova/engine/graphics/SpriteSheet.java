package com.retronova.engine.graphics;

import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.FlipException;
import com.retronova.engine.exceptions.OutOfPixels;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class SpriteSheet {

    private final BufferedImage SHEET;
    private final boolean loss;
    private final int scale;

    public SpriteSheet(String module, String sprite, int scale) {
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
        return this.SHEET.getHeight()/scale;
    }

    public int getScale() {
        return this.scale;
    }

    public BufferedImage getSHEET() {
        return this.SHEET;
    }

    public static BufferedImage flip(BufferedImage image, int horizontal, int vertical) {
        if(horizontal == 0 || vertical == 0)
            throw new FlipException("Valores precisam ser 1 (default) ou -1 (invertido)");
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
