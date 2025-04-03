package com.retronova.game.objects.entities;

public enum Modifiers {
    //Modificadores são valores que alteram os atributos das entidades.
    // True = valores que são somados conforme forem adicionados as entidades.
    // False = Um valor unico, não amontoavel, que pode ser apenas subistituido.
    Life(true),
    Range(true),
    Damage(true),
    Speed(true),
    AttackSpeed(false),
    Luck(true),
    Dodge(false),
    Dash(false);


    private final boolean heapable;

    Modifiers(boolean heapable) {
        this.heapable = heapable;
    }
    public boolean getHeapable() {
        return this.heapable;
    }

}
