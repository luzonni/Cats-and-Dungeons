package com.retronova.game.objects.tiles;

import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.Entity;

public abstract class Tile extends GameObject {

    public static Tile build(int ID, Object... values) {
        TileIDs mapping = TileIDs.values()[ID];
        int x = ((values.length >= 1) ? ((Number)values[0]).intValue() : 0) * GameObject.SIZE();
        int y = ((values.length >= 2) ? ((Number)values[1]).intValue() : 0) * GameObject.SIZE();
        boolean solid = mapping.getSolid();
        switch (mapping) {
            case Brick -> {
                return new Bricks(ID, x, y, solid);
            }
            case PurpleMud -> {
                return new PurpleMud(ID, x, y, solid);
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
            case Sand -> {
                return new Sand(ID, x, y, solid);
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

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Tile.class, sprites));
    }

    public abstract void effect(Entity e);

    @Override
    public void tick() {

    }


}
