package com.retronova.game.objects;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Objects;

public abstract class GameObject {

    private final Values values;
    protected int depth;

    public static int SIZE() {
        return 16 * Engine.SCALE;
    }

    public GameObject(int ID) {
        this.values = new Values();
        this.values.addInt("id", ID);
        this.values.addDouble("x", 0);
        this.values.addDouble("y", 0);
        this.values.addInt("width", SIZE());
        this.values.addInt("height", SIZE());
    }

    public BufferedImage[] getSprite(String spriteName) {
        SpriteSheet sheet = new SpriteSheet("objects", spriteName, Engine.SCALE);
        int length = sheet.getWidth() / 16;
        BufferedImage[] sprites = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            sprites[i] = sheet.getSprite(i, 0);
        }
        return sprites;
    }

    public BufferedImage[] getSprite(String spriteName, int prefix) {
        SpriteSheet sheet = new SpriteSheet("objects", spriteName, Engine.SCALE);
        int length = sheet.getWidth() / 16;
        BufferedImage[] sprites = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            sprites[i] = sheet.getSprite(i, prefix);
        }
        return sprites;
    }

    public int getID() {
        return this.values.getInt("id");
    }

    public double getX() {
        return this.values.getDouble("x");
    }

    public double getY() {
        return this.values.getDouble("y");
    }

    public int getWidth() {
        return this.values.getInt("width");
    }

    public int getHeight() {
        return this.values.getInt("height");
    }

    public int getDepth() {
        return this.depth;
    }

    public void setX(double newX) {
        this.values.setDouble("x", newX);
    }

    public void setY(double newY) {
        this.values.setDouble("y", newY);
    }

    public void setDepth() {
        this.depth = (int)getY();
    }

    public static Comparator<GameObject> Depth = Comparator.comparingInt(GameObject::getDepth);

    public void renderSprite(BufferedImage sprite, Graphics2D g) {
        g.drawImage(sprite, (int)getX() - Game.C.getX(), (int)getY() - Game.C.getY(), getWidth(), getHeight(),null);
    }

    public void renderSprite(BufferedImage sprite, int x, int y, Graphics2D g) {
        g.drawImage(sprite, x - Game.C.getX(), y - Game.C.getY(), getWidth(), getHeight(),null);
    }

    public void renderSprite(BufferedImage sprite, int x, int y, int width, int height, Graphics2D g) {
        g.drawImage(sprite, x - Game.C.getX(), y - Game.C.getY(), width, height,null);
    }

    public abstract void tick();

    public abstract void render(Graphics2D g);

    public abstract void dispose();

}
