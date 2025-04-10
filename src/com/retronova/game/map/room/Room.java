package com.retronova.game.map.room;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.map.GameMap;

public class Room extends GameMap {

    public Room(String mapName) {
        super(mapName);
        Sound.play(Musics.Room, true);
    }

    @Override
    public void tick() {

    }

}
