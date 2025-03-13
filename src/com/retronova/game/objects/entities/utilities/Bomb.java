package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Smoke;
import com.retronova.game.objects.particles.Spark;

import java.awt.*;
import java.util.List;

public class Bomb extends Utility {

    private final double damage;
    private final Entity shooter;
    private int count;
    private int countEx;

    private final Point[] positions_spark = {
            new Point(12, 5),
            new Point(12, 4),
            new Point(12, 3),
            new Point(11, 2),
            new Point(10, 2),
            new Point(9, 2),
            new Point(8, 3),
            new Point(8, 4),
            new Point(8, 4)
    };

    public Bomb(double x, double y, double damage, Entity shooter) {
        super(-1, x, y, 0.2);
        this.damage = damage;
        this.shooter = shooter;
        setSolid();
        loadSprites("bomb");
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
        if(countEx < positions_spark.length) {
            int x = (int)getX() + positions_spark[countEx].x * Configs.GameScale();
            int y = (int)getY() + positions_spark[countEx].y * Configs.GameScale();
            Particle p = new Spark(x, y, 0.3d);
            Game.getMap().put(p);
        }
    }

    private void explosion() {
        List<Entity> entities = Game.getMap().getEntities();
        double range = GameObject.SIZE() * 4;
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(e.getDistance(this) < range && e != shooter) {
                e.strike(AttackTypes.Melee, this.damage);
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
