package com.retronova.game.map.arena;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Portal;

public class Arena extends GameMap {

    //Para cada tipo de arena, colocar o nome do .png.
    private static final String[] ARENAS = {"easy", "normal", "hard"};

    private final Waves waves;
    private final Room room;

    public Arena(int type, Room room) {
        super(ARENAS[type]);
        this.room = room;
        this.waves = new Waves(this);
        Player player = Game.getPlayer();
        player.setX(getBounds().width/2d);
        player.setY(getBounds().height/2d);
        Sound.stop(Musics.Music2);
        Sound.play(Musics.Music3, true);
    }

    public Waves getWaves() {
        return waves;
    }

    public Room getRoom() {
        return this.room;
    }

    @Override
    public void tick() {
        waves.tick();
    }
}
