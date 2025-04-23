package com.retronova.game.objects.entities.utilities;

import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;

public abstract class Utility extends Entity {

    protected Utility(double x, double y, double friction) {
        super(-1, x, y, friction);
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        //TODO em utilitários, bater não fará nada...
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Utility.class, sprites));
    }

    @Override
    public void die() {
        this.disappear();
    }

}
