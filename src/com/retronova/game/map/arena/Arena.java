package com.retronova.game.map.arena;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.util.List;

public class Arena extends GameMap {

    private static final String[] maps = {"easy", "normal", "hard"};
    private final Waves waves;
    private boolean ended;

    public Arena(int difficult) {
        super(maps[difficult]);
        this.ended = false;
        this.waves = new Waves(this, Game.getGame().getLevel(), difficult);
        Sound.stop(Musics.Room);
        Sound.play(Musics.Fight, true);
    }

    public Waves getWaves() {
        return waves;
    }

    @Override
    public void tick() {
        if(waves.ended() && !ended && enemiesEmpty()) {
            int x = (getBounds().width / GameObject.SIZE()) / 2;
            int y = (getBounds().height / GameObject.SIZE()) / 2;
            Entity trapdoor = Entity.build(EntityIDs.TrapDoor.ordinal(), x, y, "LOAD ROOM shopping");
            put(trapdoor);
            ended = true;
        }else {
            waves.tick();
        }
    }

    boolean enemiesEmpty() {
        List<Enemy> entities = Game.getMap().getEntities(Enemy.class);
        return entities.isEmpty();
    }
}
