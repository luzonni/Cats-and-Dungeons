package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

public class Coin extends Utility {

    private int count;

    public Coin(double x, double y) {
        super(x, y, 5);
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
