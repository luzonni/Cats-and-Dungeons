package com.retronova.game.objects;

import com.retronova.engine.Configs;
import com.retronova.game.Game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;

public abstract class GameObject {

    protected Sheet<? extends GameObject> sheet;

    private int depth;
    private final int ID;
    private final Rectangle bounds;
    private boolean solid = false;

    public static int SIZE() {
        return 16 * Configs.GameScale();
    }

    public GameObject(int ID) {
        this.ID = ID;
        this.bounds = new Rectangle(0, 0, SIZE(), SIZE());
    }

    public abstract void loadSprites(String... sprites);

    protected <T extends GameObject> void setSheet(Sheet<T> sheet) {
        this.sheet = sheet;
    }

    public int getID() {
        return this.ID;
    }

    public double getX() {
        return this.bounds.getX();
    }

    public double getY() {
        return this.bounds.getY();
    }

    public int getWidth() {
        return this.bounds.width;
    }

    public int getHeight() {
        return this.bounds.height;
    }

    public Rectangle getBounds() {
        return this.bounds;
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
        this.bounds.setLocation((int)newX, this.bounds.y);
    }

    public void setY(double newY) {
        this.bounds.setLocation(this.bounds.x, (int)newY);
    }

    public void setWidth(double scale) {
        this.bounds.setSize((int)(SIZE()*scale), this.bounds.height);
    }

    public void setHeight(double scale) {
        this.bounds.setSize(this.bounds.width, (int)(SIZE()*scale));
    }

    public void setDepth() {
        this.depth = (int)getY() + getHeight();
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
