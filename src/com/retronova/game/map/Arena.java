package com.retronova.game.map;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

public class Arena extends GameMap {

    //Para cada tipo de arena, colocar o nome do .png.
    private static final String[] ARENAS = {"easy", "normal", "hard"};

    private final Waves waves;

    public Arena(int type) {
        super(ARENAS[type]);
        this.waves = new Waves(this);
        Player player = Game.getPlayer();
        player.setX(getBounds().width/2d);
        player.setY(getBounds().height/2d);
    }

    public Waves getWaves() {
        return waves;
    }
    
    @Override
    public void tick() {
        waves.tick();
    }
}
