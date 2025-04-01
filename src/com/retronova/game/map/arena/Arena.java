package com.retronova.game.map.arena;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;

public class Arena extends GameMap {

    private final Waves waves;
    private boolean ended;

    public Arena(String name, int difficult, int level) {
        super(name);
        this.ended = false;
        this.waves = new Waves(this, level, difficult);
        Sound.stop(Musics.Room);
        Sound.play(Musics.Fight, true);
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
