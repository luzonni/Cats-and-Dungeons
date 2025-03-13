package com.retronova.game.objects.particles;

import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Sheet;

import java.awt.*;

public abstract class Particle extends GameObject {

    private final double seconds;

    public Particle(double x, double y, double seconds) {
        super(-1);
        setX(x);
        setY(y);
        this.seconds = seconds * 60;
    }

    @Override
    public void loadSprites(String... sprites) {
        this.setSheet(new Sheet<>(Particle.class, sprites));
    }

    protected double getSeconds() {
        return this.seconds;
    }

    @Override
    public abstract void render(Graphics2D g);
}