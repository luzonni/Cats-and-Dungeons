package com.retronova.game.objects.entities;

public class Effect {

    private final String name;
    private final Entity entity;
    private final EffectApplicator applicator;
    private final int seconds;
    private int count;

    public Effect(String name, Entity entity, EffectApplicator applicator, int seconds) {
        this.name = name;
        this.entity = entity;
        this.applicator = applicator;
        this.seconds = seconds * 60;
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
