package com.retronova.game.objects.entities;

public enum AttackTypes {
    //TODO Colocar os tipos de ataque que o jogo permite, isso funcionara para calculo de dano de acordo com o tipo de ataque! Os valores atuais são apenas de exemplo!
    Fire(0.4), Piercing(0.6), Melee(0.8);

    final double resistence;

    AttackTypes(double resistence) {
        this.resistence = resistence;
    }

    public double getResistence() {
        return resistence;
    }
}
