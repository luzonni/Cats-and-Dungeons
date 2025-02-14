package com.retronova.game.objects.entities;

import com.retronova.exceptions.EntityNotFound;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Physical;

public abstract class Entity extends GameObject {

    private Physical physical;

    public static Entity build(int ID, double x, double y) {
        IDs entityId = IDs.values()[ID];
        x *= GameObject.SIZE();
        y *= GameObject.SIZE();
        switch (entityId) {
            case Player -> {
                return new Player(ID, x, y);
            }
        }
        throw new EntityNotFound("Entity not found");
    }

    public Entity(int ID, double x, double y) {
        super(ID);
        setX(x);
        setY(y);
        this.physical = new Physical(this);
    }

    @Override
    public void tick() {
        setDepth();
    }


    public Physical getPhysical() {
        return this.physical;
    }
}
