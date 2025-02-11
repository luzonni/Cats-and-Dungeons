package com.retronova.game.objects.tiles;

import com.retronova.exceptions.TileNotFound;
import com.retronova.game.objects.GameObject;

public abstract class Tile extends GameObject {

    public static Tile build(int ID, int x, int y) {
        Tile tile;
        throw new TileNotFound("Tile not found");
    }

    public Tile(int ID, int x, int y) {
        super(ID);
        setX(x);
        setY(y);
    }
}
