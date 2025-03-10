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
        boolean solid = mapping.getSolid();
        switch (mapping) {
            case Brick -> {
                return new Bricks(ID, x, y, solid);
            }
            case Stone -> {
                return new Stone(ID, x, y, solid);
            }
            case Ice -> {
                return new Ice(ID, x, y, solid);
            }
            case Lava -> {
                return new Lava(ID, x, y, solid);
            }
            case DeathSand -> {
                return new DeathSand(ID, x, y, solid);
            }
            case DarkBricks -> {
                return new DarkBriks(ID, x, y, solid);
            }
            case Ceramics -> {
                return new Ceramics(ID, x, y, solid);
            }
            default -> {
                return new Void(-1, x, y, solid);
            }
        }
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
