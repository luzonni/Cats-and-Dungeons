package com.retronova.game.objects.entities;

import com.retronova.exceptions.EntityNotFound;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Physical;

public abstract class Entity extends GameObject {

    private final Physical physical;
    private AttackTypes[] resistanceOf;
    private double life;

    public static Entity build(int ID, double x, double y) {
        IDs entityId = IDs.values()[ID];
        x *= GameObject.SIZE();
        y *= GameObject.SIZE();
        switch (entityId) {
            case Zombie -> {
                return new Zombie(ID, x, y);
            }
            case Skeleton -> {
                return new Skeleton(ID, x, y);
            }
        }
        throw new EntityNotFound("Entity not found");
    }

    public Entity(int   ID, double x, double y, double friction) {
        super(ID);
        setX(x);
        setY(y);
        this.physical = new Physical(this, friction);
        setResistances(null);
        setLife(100d);
    }

    public double getLife() {
        return this.life;
    }

    protected void setLife(double life) {
        this.life = life;
    }

    protected void setResistances(AttackTypes... resistences) {
        this.resistanceOf = resistences;
    }

    public void strike(AttackTypes type, double damage) {
        if(this.resistanceOf != null && type != null)
            for(AttackTypes resistence : this.resistanceOf) {
                if(resistence.equals(type)) {
                    //TODO calculo de dano com a resistência
                    this.life -= damage * resistence.getResistence();
                    return;
                }
            }
        //TODO damo total caso não exista resistência ( dano bruto )
        this.life -= damage;
    }

    public void die() {
        //TODO adicionar particula de morte
        //TODO adicionar som de morte
        Game.getMap().getEntities().remove(this);
    }

    @Override
    public void tick() {
        setDepth();
        getPhysical().moment();
        if(this.life <= 0)
            this.die();
    }


    public Physical getPhysical() {
        return this.physical;
    }

}
