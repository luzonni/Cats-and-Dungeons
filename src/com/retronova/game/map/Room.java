package com.retronova.game.map;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;

public class Room extends GameMap {

    public Room(String mapName) {
        super(mapName);
        Sound.play(Musics.Music2, true);
    }


    @Override
    public void tick() {

    }
}
