package com.retronova.game.map;

import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.furniture.Furniture;
import com.retronova.game.objects.furniture.FurnitureIDs;

public class Room extends GameMap {

    public Room(String mapName) {
        super(mapName);
        double x = 15;
        double y = 12;
        Furniture door = Furniture.build(FurnitureIDs.Door.ordinal(), x, y);
        put(door);
    }

    @Override
    public void tick() {

    }
}
