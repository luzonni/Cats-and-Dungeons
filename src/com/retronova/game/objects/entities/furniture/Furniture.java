package com.retronova.game.objects.entities.furniture;

import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;

public abstract class Furniture extends Entity {

    Furniture(int ID, double x, double y) {
        super(ID, x, y, 1);
        setSolid();
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Furniture.class, sprites));
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
