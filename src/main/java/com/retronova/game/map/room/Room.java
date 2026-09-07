package com.retronova.game.map.room;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.map.GameMap;

public class Room extends GameMap {

    public Room(String mapName) {
        super(mapName);
        // Silencia o combate aqui, e nao no dispose da arena: o construtor do
        // mapa novo roda antes do dispose do antigo, entao quem entra e quem
        // manda na trilha.
        Sound.stop(Musics.Fight);
        Sound.play(Musics.Room, true);
    }

    @Override
    public void tick() {

    }

}
