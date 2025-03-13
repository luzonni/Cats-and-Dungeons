package com.retronova.game.objects.particles;

import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public abstract class Particle extends GameObject {

    private final double seconds;

    public Particle(int ID, double x, double y, double seconds) {
        super(ID);
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