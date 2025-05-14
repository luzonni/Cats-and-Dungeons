package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;

public class Camera {

    private GameObject followed;
    private final Rectangle bounds;
    private final double speed;
    private float zoom;
    private double x, y;

    public Camera(Rectangle bounds, double speed) {
        this.bounds = bounds;
        this.speed = speed;
        this.zoom = 1f;
    }

    public static AffineTransform getAt() {
        Camera cam = Game.getCam();
        AffineTransform at = new AffineTransform();
        int screenWidth = Engine.window.getWidth();
        int screenHeight = Engine.window.getHeight();
        int camX = cam.getX() + screenWidth/2;
        int camY = cam.getY() + screenHeight/2;
        double zoom = cam.getZoom();
        double centerX = camX + screenWidth / 2.0 / zoom;
        double centerY = camY + screenHeight / 2.0 / zoom;
        at.translate(screenWidth / 2.0, screenHeight / 2.0);
        at.scale(zoom, zoom);
        at.translate(-centerX + (screenWidth/2d)/zoom, -centerY + (screenHeight/2d)/zoom);
        return at;
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
        if(KeyBoard.KeyPressed("P") && this.zoom < 3f) {
            this.zoom = Math.round((this.zoom + 0.2) * 10f) / 10f;
        }else if(KeyBoard.KeyPressed("O") && this.zoom > 1f) {
            this.zoom = Math.round((this.zoom - 0.2) * 10f) / 10f;
        }
        System.out.println(zoom);
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
