package com.retronova.game.objects.furniture;

import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Furniture extends Entity {

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

    Furniture(int ID, double x, double y) {
        super(ID, x, y, 1);
        setSolid();
    }
}
