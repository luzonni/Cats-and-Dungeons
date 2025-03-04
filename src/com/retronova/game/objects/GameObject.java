package com.retronova.game.objects;

import com.retronova.engine.Configs;
import com.retronova.game.Game;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;

public abstract class GameObject {

    private final Values values;
    protected int depth;
    private Rectangle bounds;
    private boolean solid = false;

    public static int SIZE() {
        return 16 * Configs.SCALE;
    }

    public GameObject(int ID) {
        this.values = new Values();
        this.values.addInt("id", ID);
        this.values.addDouble("x", 0d);
        this.values.addDouble("y", 0d);
        this.values.addInt("width", SIZE());
        this.values.addInt("height", SIZE());
        this.bounds = new Rectangle(getWidth(), getHeight());
    }

    public BufferedImage[] getSprite(String spriteName) {
        SpriteSheet sheet = new SpriteSheet("objects", spriteName, Configs.SCALE);
        int length = sheet.getWidth() / 16;
        BufferedImage[] sprites = new BufferedImage[length];
        for(int i = 0; i < length; i++) {
            sprites[i] = sheet.getSpriteWithIndex(i, 0);
        }
        return sprites;
    }

    public BufferedImage[] getSprite(String spriteName, int prefix) {
        SpriteSheet sheet = new SpriteSheet("objects", spriteName, Configs.SCALE);
        return sheet.getSprites(prefix);
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

    public Rectangle getBounds() {
        return this.bounds;
    }

    public int getDepth() {
        return this.depth;
    }

    public abstract BufferedImage getSprite();

    public void setX(double newX) {
        this.values.setDouble("x", newX);
        this.bounds.setLocation((int)newX, this.bounds.y);
    }

    public void setY(double newY) {
        this.values.setDouble("y", newY);
        this.bounds.setLocation(this.bounds.x, (int)newY);
    }

    public void setDepth() {
        this.depth = (int)getY();
    }

    protected void setSolid() {
        this.solid = true;
    }

    public boolean isSolid() {
        return this.solid;
    }

    public double getDistance(GameObject object) {
        return Math.sqrt(Math.pow((object.getX() - this.getX()), 2) + Math.pow(object.getY() - this.getY(), 2));
    }

    public double getAngle(GameObject obj) {
        return Math.atan2(getY() - obj.getY(), getX() - obj.getX());
    }

    public double getAngle(Point point) {
        return Math.atan2(point.getY() - getY(), point.getX() - getX());
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
