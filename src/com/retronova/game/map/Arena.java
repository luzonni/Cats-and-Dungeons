package com.retronova.game.map;

public class Arena extends GameMap {

    //Para cada tipo de arena, colocar o nome do .png.
    private static final String[] ARENAS = {"easy", "normal", "hard"};

    private final Waves waves;

    public Arena(int type) {
        super(ARENAS[type]);
        this.waves = new Waves(this);
    }

    public Waves getWaves() {
        return waves;
    }
    
    @Override
    public void tick() {
        waves.tick();
    }
}
