package com.retronova.game.objects.physical;

public class Vector {

    private final double angle;
    private final double vecX;
    private final double vecY;
    private double force;

    Vector(double force, double angle) {
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
        if(o == null | !(o instanceof Vector)) {
            return false;
        }
        return this.angle == ((Vector) o).angle;
    }

}
