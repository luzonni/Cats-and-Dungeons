package com.retronova.game.objects.furniture;

import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.game.objects.GameObject;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Furniture extends GameObject {

    public static Furniture build(int id, double x, double y) {
        FurnitureIDs furnitureID = FurnitureIDs.values()[id];
        x *= GameObject.SIZE();
        y *= GameObject.SIZE();
        switch(furnitureID) {
            case Door -> {
                return new Door(id, x, y);
            }
        }
        throw new EntityNotFound("Furniture entity not found");
    }

    public Furniture(int ID, double x, double y) {
        super(ID);
        this.setX(x);
        this.setY(y);
    }
}
