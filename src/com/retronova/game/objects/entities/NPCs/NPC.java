package com.retronova.game.objects.entities.NPCs;

import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;

public abstract class NPC extends Entity {


    protected NPC(int ID, double x, double y, double friction) {
        super(ID, x, y, friction);
        setSolid();
    }

    @Override
    public void strike(AttackTypes type, double damage) {

    }

    @Override
    public void die() {
        this.disappear();
    }

}
