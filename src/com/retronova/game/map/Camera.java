package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.game.objects.GameObject;

import java.awt.*;

public class Camera {

    private GameObject followed;
    private final Rectangle bounds;
    private final double speed;

    private double x, y;

    public Camera(Rectangle bounds, double speed) {
        this.bounds = bounds;
        this.speed = speed;
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
        //caso a nova posição seja menor ou maior que o mapa, a posição é atualizada para o limite.
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
