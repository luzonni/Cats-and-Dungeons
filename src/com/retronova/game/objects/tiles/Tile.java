package com.retronova.game.objects.tiles;

import com.retronova.exceptions.TileNotFound;
import com.retronova.game.map.Mapping;
import com.retronova.game.objects.GameObject;

public abstract class Tile extends GameObject {

    public static Tile build(int ID, int x, int y) {
        Mapping mapping = Mapping.values()[ID];
        x *= GameObject.SIZE();
        y *= GameObject.SIZE();
        switch (mapping) {
            case Brick -> {
                return new Bricks(ID, x, y);
            }
            case Stone -> {
                return new Stone(ID, x, y);
            }
        }
        throw new TileNotFound("Tile not found");
    }

    Tile(int ID, int x, int y) {
        super(ID);
        setX(x);
        setY(y);
    }
}
