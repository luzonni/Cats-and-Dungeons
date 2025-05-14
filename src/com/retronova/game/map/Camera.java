package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.game.objects.GameObject;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Camera {

    private final AffineTransform at;
    private GameObject followed;
    private final Rectangle bounds;
    private final double speed;
    private double zoom;
    private double x, y;

    public Camera(Rectangle bounds, double speed) {
        this.bounds = bounds;
        this.speed = speed;
        this.zoom = 1d;
        this.at = new AffineTransform();
    }

    public AffineTransform getAt() {
        return this.at;
    }

    public double getZoom() {
        return this.zoom;
    }

    public int getX() {
        return (int) this.x;
    }

    public int getY() {
        return (int)this.y;
    }

    public Rectangle getBounds() {
        return this.bounds;
    }

    public void setX(int newX) {
        this.x = newX;
    }

    public void setY(int newY) {
        this.y = newY;
    }

    public void tick() {
        if(followed != null)
            follow();
        if(KeyBoard.KeyPressed("P")) {
            this.zoom+=0.25d;
        }else if(KeyBoard.KeyPressed("O")) {
            this.zoom-=0.25d;
        }
        at.setToScale(getZoom(), getZoom());
        at.setToTranslation(-getX(), -getY());
    }

    public void setFollowed(GameObject followed) {
        this.followed = followed;
    }

    public GameObject getFollowed() {
        return this.followed;
    }

    private void follow() {
        int width = Engine.window.getWidth();
        int height = Engine.window.getHeight();
        double targetX = followed.getBounds().getCenterX() - width/2d;
        double targetY = followed.getBounds().getCenterY() - height/2d;
        int xx = (int)(getX() + (targetX - getX()) * speed);
        int yy = (int) (getY() + (targetY - getY()) * speed);
        setX(xx);
        setY(yy);
        if(width  > bounds.width ) {
            setX((bounds.width-width)/2);
        }else {
            if(xx < bounds.x)
                setX(bounds.x);
            if(xx + width > bounds.width)
                setX(bounds.width - width);
        }
        if (height > bounds.height) {
            setY((bounds.height-height)/2);
        }else {
            if(yy < bounds.y)
                setY(bounds.y);
            if(yy + height > bounds.height)
                setY(bounds.height - height);
        }
    }

}
