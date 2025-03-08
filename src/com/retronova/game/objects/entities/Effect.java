package com.retronova.game.objects.entities;

public class Effect {

    private final Entity entity;
    private final EffectApplicator applicator;
    private final int seconds;
    private int count;

    public Effect(Entity entity, EffectApplicator applicator, int seconds) {
        this.entity = entity;
        this.applicator = applicator;
        this.seconds = seconds * 60;
    }

    public void tick() {
        this.applicator.effect(entity);
        count++;
        if(count >= seconds) {
            entity.removeEffect(this);
        }
    }

}
