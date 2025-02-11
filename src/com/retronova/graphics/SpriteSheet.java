package com.retronova.graphics;

import com.retronova.engine.Engine;
import com.retronova.exceptions.OutOfPixels;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
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

    public BufferedImage subImage(int x, int y, int width, int heigth) {
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

    public BufferedImage getSprite(int indexX, int indexY) {
        if(loss)
            return SHEET;
        int x = indexX * 16 * scale;
        int y = indexY * 16 * scale;
        int size = 16 * scale;
        if(x < 0 || x + size > SHEET.getWidth() || y < 0 || y + size > SHEET.getHeight()) {
            throw new OutOfPixels("the subimage is out of image pixels");
        }
        return SHEET.getSubimage(x, y, size, size);
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

}
