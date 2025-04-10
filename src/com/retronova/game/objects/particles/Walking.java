package com.retronova.game.objects.particles;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;

import java.awt.*;

public class Walking extends Particle {

    private int count;
    private int indexAnim;
    private int animationSpeed = 10;
    private int lifeTime = 30;

    private static int walkingParticleTimer = 0;
    private static final int walkingParticleInterval = 10;

    public Walking(double x, double y) {
        super(x, y, 0.5);
        loadSprites("walking");
        setX(getX() - getWidth() / 2d);
        setY(getY() - getHeight() / 2d);
    }

    @Override
    public void tick() {
        count++;
        if (count >= animationSpeed) {
            count = 0;
            indexAnim++;
            if (indexAnim >= getSheet().size()) {
            } else {
                getSheet().setIndex(indexAnim);
            }
        }
        lifeTime--;
        if (lifeTime <= 0) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    public static void createWalkingParticle(double x, double y) {
        walkingParticleTimer++;
        if (walkingParticleTimer >= walkingParticleInterval) {
            Walking walkingParticle = new Walking(x, y);
            Game.getMap().put(walkingParticle);
            walkingParticleTimer = 0;
        }
    }

    public static void resetTimer() {
        walkingParticleTimer = 0;
    }
}