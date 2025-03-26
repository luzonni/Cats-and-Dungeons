package com.retronova.game.map.arena;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.furniture.TrapDoor;
import com.retronova.game.objects.particles.Portal;

public class Arena extends GameMap {

    //Para cada tipo de arena, colocar o nome do .png.


    private final Waves waves;
    private boolean ended;

    public Arena(String name, int level) {
        super(name);
        this.ended = false;
        this.waves = new Waves(this, level, Game.getGame().getDifficult());
        Sound.stop(Musics.Music2);
        Sound.play(Musics.Music3, true);
    }

    public Waves getWaves() {
        return waves;
    }

    @Override
    public void tick() {
        if(waves.ended() && !ended) {
            int x = (getBounds().width / GameObject.SIZE()) / 2;
            int y = (getBounds().height / GameObject.SIZE()) / 2;
            Entity trapdoor = Entity.build(EntityIDs.TrapDoor.ordinal(), x, y, "room_shopping");
            put(trapdoor);
            Game.getGame().plusLevel();
            ended = true;
        }else {
            waves.tick();
        }
    }
}
