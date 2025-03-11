package com.retronova.game.objects.particles;

import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.game.objects.GameObject;

import java.awt.*;

public abstract class Particle extends GameObject {

    public static Particle build(ParticleIDs id, double... values) {
        if(id.getArgs() != values.length) {
            throw new IllegalArgumentException("Particle: " + id.name() + " needs " + id.getArgs() + " arguments to work.");
        }
        int i = id.ordinal();
        double x = values[0];
        double y = values[1];
        double secs = values[2];
        switch (id) {
            case Smoke -> {
                double dir = values[3];
                return new Smoke(i, x, y, secs, dir);
            }
            case Spark -> {
                return new Spark(i, x, y, secs);
            }
        }
        throw new EntityNotFound("Particle not found.");
    }

    private final double seconds;

    public Particle(int ID, double x, double y, double seconds) {
        super(ID);
        setX(x);
        setY(y);
        this.seconds = seconds * 60;
    }

    protected double getSeconds() {
        return this.seconds;
    }

    @Override
    public abstract void render(Graphics2D g);

}
