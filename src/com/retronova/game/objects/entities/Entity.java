package com.retronova.game.objects.entities;

import com.retronova.exceptions.EntityNotFound;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Physical;

public abstract class Entity extends GameObject {

    private final Physical physical;
    private AttackTypes[] resistanceOf;
    private double[] life; //este valor é um array de dois valores, o primeiro é a vida original, e o outro a vida atual

    public static Entity build(int ID, double x, double y) {
        EntityID entityId = EntityID.values()[ID];
        x *= GameObject.SIZE();
        y *= GameObject.SIZE();
        switch (entityId) {
            case Zombie -> {
                return new Zombie(ID, x, y);
            }
            case Skeleton -> {
                return new Skeleton(ID, x, y);
            }
            case Slime -> {
                return new Slime(ID, x, y);
            }
        }
        throw new EntityNotFound("Entity not found");
    }

    public Entity(int ID, double x, double y, double friction) {
        super(ID);
        setX(x);
        setY(y);
        this.physical = new Physical(this, friction);
        setResistances(null);
        setLife(100d);
    }

    public double getLife() {
        return this.life[1];
    }

    public double getLifeSize() {
        return this.life[0];
    }

    protected void setLife(double life) {
        if(this.life == null) {
            this.life = new double[] {life, life};
            return;
        }
        this.life[1] = life;
    }

    protected void setResistances(AttackTypes... resistences) {
        this.resistanceOf = resistences;
    }

    public void strike(AttackTypes type, double damage) {
        if (this.resistanceOf != null && type != null) {
            for (AttackTypes resistance : this.resistanceOf) {
                if (resistance.equals(type)) {
                    setLife(getLife() - damage * (1 - resistance.getResistance()));
                    return;
                }
            }
        }
        setLife(getLife() - damage); // Dano total
        if (getLife() <= 0) {
            die();
        }
    }


    public void die() {
        //TODO adicionar particula de morte
        //TODO adicionar som de morte
        Game.getMap().getEntities().remove(this);
    }

    public Physical getPhysical() {
        return this.physical;
    }

}
