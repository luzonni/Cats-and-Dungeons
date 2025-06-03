package com.retronova.game.objects.entities.furniture;

import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import studio.retrozoni.sheeter.SpriteSheet;

public abstract class Furniture extends Entity {

    Furniture(int ID, double x, double y, int weight) {
        super(ID, x, y, weight);
        setSolid();
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new SpriteSheet("sprites/objects/furniture", sprites));
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        //TODO em furniture, caso queiram criar um sistema para destruir-las, pode ser implementada aqui...
    }

    @Override
    public void die() {
        this.disappear();
    }
}
