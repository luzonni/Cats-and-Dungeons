package com.retronova.game.objects.entities.NPCs;

import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import studio.retrozoni.sheeter.SpriteSheet;

public abstract class NPC extends Entity {


    protected NPC(int ID, double x, double y, int weight) {
        super(ID, x, y, weight);
        setSolid();
        setClickable();
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new SpriteSheet("sprites/objects/npc", sprites));
    }

    @Override
    public void strike(AttackTypes type, double damage) {

    }

    @Override
    public void die() {
        this.disappear();
    }

}
