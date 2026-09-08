package com.retronova.game.objects.entities;

public enum AttackTypes {

    Flat,
    Fire,
    Piercing,
    Melee,
    Poison,
    Explosion,
    Sorcery,
    Laser,
    Impact,
    Throw,
    /**
     * Os elementos das variantes de arma. Ver items/Elemento.java.
     *
     * Entram aqui e nao num enum separado porque quem consulta e o mapa de
     * resistencias de Entity.strike, que e indexado por AttackTypes: um bicho de
     * gelo resiste a Ice do mesmo jeito que um esqueleto resiste a Piercing, e
     * nao ha razao para o sistema saber que uns sao "elementais" e outros nao.
     * Tipo ausente do mapa leva dano cheio, entao acrescentar aqui nao quebra
     * inimigo nenhum que ja exista.
     */
    Ice,
    Water,
    Earth,
    Air,

}
