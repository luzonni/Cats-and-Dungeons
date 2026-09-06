package com.retronova.game.objects.entities;

public class Effect {

    private final String name;
    private final Entity entity;
    private final EffectApplicator applicator;
    private final double seconds;
    private final double repetition;
    private double ticks, count;

    public Effect(String name, Entity entity, EffectApplicator applicator, double seconds, int repetitions) {
        this.name = name;
        this.entity = entity;
        this.applicator = applicator;
        this.seconds = seconds * 60;
        this.repetition = this.seconds / repetitions;
        this.count = this.repetition;
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
        count++;
        if(count > repetition) {
            count = 0;
            this.applicator.effect(entity);
        }
        ticks++;
        if(ticks >= seconds) {
            entity.removeEffect(this);
        }
    }

}
