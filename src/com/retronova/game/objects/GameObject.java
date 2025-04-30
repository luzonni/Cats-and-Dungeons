package com.retronova.game.objects;

import com.retronova.engine.Configs;
import com.retronova.game.Game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;

public abstract class GameObject {

    protected Sheet<? extends GameObject> sheet;

    private int depth;
    private boolean groundObject;
    private final int ID;
    private double x, y, width, height;
    private boolean solid = false;

    public static int SIZE() {
        return 16 * Configs.GameScale();
    }

    public GameObject(int ID) {
        this.ID = ID;
        this.width = SIZE();
        this.height = SIZE();
    }

    public abstract void loadSprites(String... sprites);

    protected <T extends GameObject> void setSheet(Sheet<T> sheet) {
        this.sheet = sheet;
    }

    public int getID() {
        return this.ID;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return (int)this.width;
    }

    public int getHeight() {
        return (int)this.height;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }

    public int getDepth() {
        return this.depth;
    }

    protected Sheet getSheet() {
        return this.sheet;
    }

    public BufferedImage getSprite() {
        return this.sheet.getSprite();
    }

    public void setX(double newX) {
        this.x = newX;
    }

    public void setY(double newY) {
        this.y = newY;
    }

    public void setWidth(double scale) {
        this.width = SIZE()*scale;
    }

    public void setHeight(double scale) {
        this.height = SIZE()*scale;
    }

    public void setDepth() {
        int d = (int)getY() + getHeight();
        this.depth = groundObject ? 0 : d;
    }

    protected void setGroundObject() {
        this.groundObject = true;
    }

    protected void setSolid() {
        this.solid = true;
    }

    public boolean isSolid() {
        return this.solid;
    }

    public double getDistance(GameObject object) {
        double x1 = object.getX() + object.getWidth()/2d;
        double y1 = object.getY() + object.getHeight()/2d;
        double x2 = this.getX() + this.getWidth()/2d;
        double y2 = this.getY() + this.getHeight()/2d;
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public double getAngle(GameObject obj) {
        return Math.atan2(getY() - obj.getY(), getX() - obj.getX());
    }

    public double getAngle(Point point) {
        return Math.atan2(point.getY() - getY(), point.getX() - getX());
    }

    public static Comparator<GameObject> Depth = Comparator.comparingInt(GameObject::getDepth);

    public void renderSprite(BufferedImage sprite, Graphics2D g) {
        int x = ((int)getX() + (getWidth() - sprite.getWidth())/2) - Game.C.getX();
        int y = ((int)getY() - (sprite.getHeight()) + getHeight()) - Game.C.getY();
        g.drawImage(sprite, x, y,null);
    }

    public abstract void tick();

    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    public void dispose() {
        sheet.dispose();
    }

}
