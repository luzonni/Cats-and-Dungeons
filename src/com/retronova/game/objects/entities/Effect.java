package com.retronova.game.objects.entities;

public class Effect {

    private final Entity entity;
    private final EffectApplicator applicator;
    private final int secounds;
    private int count;

    public Effect(Entity entity, EffectApplicator applicator, int secounds) {
        this.entity = entity;
        this.applicator = applicator;
        this.secounds = secounds * 60;
    }

    public void tick() {
        this.applicator.effect(entity);
        count++;
        if(count >= secounds) {
            entity.removeEffect(this);
        }
    }

}
