package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Smoke;
import com.retronova.game.objects.particles.Spark;

import java.util.List;


public class ThrownGasBom extends Utility {
    private final double damage;
    private int count;
    private int countEx;

    public ThrownGasBom(double x, double y, double damage) {
        super(x, y, 0.7);
        this.damage = damage;
        loadSprites("gasbomb");
    }

    @Override
    public void tick() {
        count++;
        if(count > 12) {
            count = 0;
            countEx++;
            getSheet().plusIndex();
        }
        if(countEx == 10) {
            explosion();
        }
    }

    private void explosion() {
        List<Enemy> entities = Game.getMap().getEntities(Enemy.class);
        double range = GameObject.SIZE() * 4;
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(e.getDistance(this) < range) {
                e.EFFECT_STUNNED(10, 100);
            }
        }
        for(int i = 0; i < 120; i++) {
            double angle = Engine.RAND.nextDouble(Math.toRadians(360));
            double fator = 0.8d;
            double raio = range * Math.pow(Engine.RAND.nextDouble(), fator);
            double x = getX() + Math.cos(angle) * raio;
            double y = getY() + Math.sin(angle) * raio;
            Particle smoke = new Smoke(x, y, 1.2, Engine.RAND.nextDouble(Math.PI*2));
            Game.getMap().put(smoke);
        }
        this.disappear();
    }

}
