package com.retronova.game.objects.furniture;

import com.retronova.game.objects.GameObject;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Furniture extends GameObject {

    public Furniture(int ID, double x, double y) {
        super(ID);
        this.setX(x);
        this.setY(y);
    }
}
