package com.retronova.game.objects.physical;

import com.retronova.engine.Configs;

public class Vector {

    private final String id;
    private double vecX;
    private double vecY;
    private double angle;
    private double force;

    Vector(String id, double force, double angle) {
        this.id = id;
        this.vecX = Math.cos(angle);
        this.vecY = Math.sin(angle);
        this.angle = angle;
        this.force = force;
    }

    double getAngle() {
        return this.angle;
    }

    double getVecX() {
        return this.vecX * force;
    }

    void setAngle(double angle) {
        this.angle = angle;
        this.vecX = Math.cos(angle);
        this.vecY = Math.sin(angle);
    }

    double getVecY() {
        return this.vecY * force;
    }

    double getForce() {
        return this.force;
    }

    void setForce(double force) {
        this.force = force;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Vector vec) {
            return this.id.equals(vec.id);
        }
        return false;
    }

}
