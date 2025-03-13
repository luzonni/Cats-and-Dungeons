package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.ParticleIDs;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseExplode extends Enemy {

    private int countAnim;


    //private final int raioExplosao;
    private final double danoExplosao;

    // TODO: Entender pq a sprite ta nervosa virando de um lado pro outro sem motivo
    public MouseExplode(int ID, double x, double y) {
        super(ID,x,y,0.5);
        this.danoExplosao = 5;
        loadSprites("ratexplode");
        setSolid();
        setAlive();
        setSpeed(1.3d);
        setLife(200);
        setXpWeight(12.4d);
    }

    public void tick() {
        moveIA();
        animar();
    }

    public void moveIA() {
        Player player = Game.getPlayer();

        if(this.getDistance(player) < GameObject.SIZE()) {
            explodir(player);
        }else {
            double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
            getPhysical().addForce("move", getSpeed(), radians);
        }

    }

    private void explodir(Player player) {
        player.strike(AttackTypes.Explosion, danoExplosao);
        double range = GameObject.SIZE()*1.5d;
        for(int i = 0; i < 60; i++) {
            double angle = Engine.RAND.nextDouble(Math.toRadians(360));
            double x = getX() + Math.cos(angle) * Engine.RAND.nextDouble(range);
            double y = getY() + Math.sin(angle) * Engine.RAND.nextDouble(range);
            Particle smoke = Particle.build(ParticleIDs.Smoke, x, y, 1.2, Engine.RAND.nextDouble(Math.PI*2));
            Game.getMap().put(smoke);
        }
        Sound.play(Sounds.MouseExplode);
        disappear();
    }

    private void animar() {
        countAnim ++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }


    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }

}
