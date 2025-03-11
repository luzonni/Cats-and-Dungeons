package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.ParticleIDs;

import java.awt.image.BufferedImage;
import java.util.List;

public class Bomb extends Entity{

    private final double damage;
    private final Entity shooter;
    private int count;
    private int countEx;


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
            double fator = 0.8d; // Quanto menor, mais pontos próximos ao centro
            double raio = range * Math.pow(Engine.RAND.nextDouble(), fator);
            double x = getX() + Math.cos(angle) * raio;
            double y = getY() + Math.sin(angle) * raio;
            Particle smoke = Particle.build(ParticleIDs.Smoke, x, y, 1.2, Engine.RAND.nextDouble(Math.PI*2));
            Game.getMap().put(smoke);
        }
        this.disappear();
    }

}
