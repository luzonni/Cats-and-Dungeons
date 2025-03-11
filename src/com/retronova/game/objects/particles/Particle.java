package com.retronova.game.objects.particles;

import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.game.objects.GameObject;

import java.awt.*;

public abstract class Particle extends GameObject {

    public static Particle build(ParticleIDs id, double... values) {
        if(values.length < 2) {
            throw new RuntimeException("Valores incompativeis");
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
