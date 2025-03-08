package com.retronova.game.objects.tiles;

import com.retronova.engine.exceptions.TileNotFound;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;

import java.awt.*;

public abstract class Tile extends GameObject {

    public static Tile build(int ID, int x, int y) {
        TileIDs mapping = TileIDs.values()[ID];
        x *= GameObject.SIZE();
        y *= GameObject.SIZE();
        switch (mapping) {
            case Brick -> {
                return new Bricks(ID, x, y);
            }
            case Stone -> {
                return new Stone(ID, x, y);
            }
            case Ice -> {
                return new Ice(ID, x, y);
            }
            case Lava -> {
                return new Lava(ID, x, y);
            }
            case DeathSand -> {
                return new DeathSand(ID, x, y);
            }
        }
        throw new TileNotFound("Tile not found");
    }

    Tile(int ID, int x, int y, boolean solid) {
        super(ID);
        setX(x);
        setY(y);
        if(solid){
            setSolid();
        }
    }

    public abstract void effect(Entity e);

    @Override
    public void tick() {

    }


}
