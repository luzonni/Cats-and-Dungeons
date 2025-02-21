package com.retronova.game.objects.entities;

public enum AttackTypes {
    Fire(0.5),      // 50% de resistência de fogo né
    Piercing(0.2),  // 20% de resistência, perfuração de flexas ou outros
    Melee(0.9);     // Sem resistência a ataques de perto

    private final double resistance;

    AttackTypes(double resistance) {
        this.resistance = resistance;
    }

    public double getResistance() {
        return resistance;
    }
}
