package com.retronova.game.map.room;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Player;

public class Room extends GameMap {

    public Room(String mapName) {
        super(mapName);
        Sound.play(Musics.Music2, true);
    }

    @Override
    public void tick() {

    }

}
