package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;

public class Coin extends Entity {

    private int count;

    Coin(int ID, double x, double y) {
        super(ID, x, y, 0.2);
        loadSprites("coin");
        setSolid();
        getPhysical().addForce("coinSpawn", 7, Engine.RAND.nextDouble() * Math.PI*2);
    }

    @Override
    public void tick() {
        count++;
        if(count > 7) {
            count = 0;
            getSheet().plusIndex();
        }
        Player player = Game.getPlayer();
        if(player.colliding(this)) {
            this.disappear();
        }
    }
}
