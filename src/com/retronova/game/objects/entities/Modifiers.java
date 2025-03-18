package com.retronova.game.objects.entities;

public enum Modifiers {
    //Modificadores são valores que alteram os atributos das entidades.
    Life(true),
    Range(true),
    Damage(true),
    Speed(false),
    AttackSpeed(false),
    Luck(true),
    Dodge(false),
    Dash(false);

    private boolean heapable;
    Modifiers(boolean heapable) {
        this.heapable = heapable;
    }
    public boolean getHeapable() {
        return this.heapable;
    }

}
