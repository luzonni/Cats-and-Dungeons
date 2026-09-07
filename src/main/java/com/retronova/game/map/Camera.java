package com.retronova.game.map;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
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
    private float currentZoom;
    private double x, y;

    public Camera(Rectangle bounds, double speed) {
        this.bounds = bounds;
        this.speed = speed;
        this.zoom = Configs.Zoom() / 100f;
        this.currentZoom = this.zoom;
    }

    public static AffineTransform getAt() {
        Camera cam = Game.getCam();
        AffineTransform at = new AffineTransform();
        int screenWidth = Engine.window.getWidth();
        int screenHeight = Engine.window.getHeight();
        int camX = cam.getX() + screenWidth/2;
        int camY = cam.getY() + screenHeight/2;
        double zoom = cam.currentZoom;
        double centerX = camX + screenWidth / 2.0 / zoom;
        double centerY = camY + screenHeight / 2.0 / zoom;
        at.translate(screenWidth / 2.0, screenHeight / 2.0);
        at.scale(zoom, zoom);
        at.translate(-centerX + (screenWidth/2d)/zoom, -centerY + (screenHeight/2d)/zoom);
        return at;
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
        // A aproximacao vem das opcoes, e nao mais de Ctrl+= / Ctrl+-. O atalho
        // era ajuste de desenvolvimento, sem persistencia e sem limite util; com
        // uma opcao de verdade ele so brigaria com ela por quem manda no zoom.
        this.zoom = Configs.Zoom() / 100f;
        float def = (zoom - this.currentZoom) / 8f;
        this.currentZoom += def;
        if(Math.abs(zoom - currentZoom) < 0.001f) {
            this.currentZoom = zoom;
        }
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
