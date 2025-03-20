package com.retronova.game.objects.entities;

import java.beans.ConstructorProperties;

public class Effect {

    private final String name;
    private final Entity entity;
    private final EffectApplicator applicator;
    private final double seconds;
    private int count;

    public Effect(String name, Entity entity, EffectApplicator applicator, double seconds) {
        this.name = name;
        this.entity = entity;
        this.applicator = applicator;
        this.seconds = seconds * 60;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(!(o instanceof Effect)) {
            return false;
        }
        return this.name.equals(((Effect) o).getName());
    }

    public String getName() {
        return this.name;
    }

    public void tick() {
        this.applicator.effect(entity);
        count++;
        if(count >= seconds) {
            entity.removeEffect(this);
        }
    }

}
