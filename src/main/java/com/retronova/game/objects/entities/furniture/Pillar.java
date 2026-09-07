package com.retronova.game.objects.entities.furniture;

/**
 * Pilar de pedra. Elemento estrutural da dungeon, puramente cenográfico.
 *
 * É uma entidade e não um tile porque entidades entram na ordenação por
 * profundidade: o gato passa atrás do pilar quando está acima dele e na frente
 * quando está abaixo. Tiles são desenhados todos antes das entidades e não
 * conseguem esse efeito.
 *
 * O sprite tem 32x32 e é ancorado pela base, então o pilar sobe um tile acima
 * da célula que ocupa — é daí que vem a sensação de altura.
 */
public class Pillar extends Furniture {

    public Pillar(int ID, double x, double y) {
        // Peso alto: o sistema de física ignora deslocamento acima de 500, então
        // nada empurra o pilar.
        super(ID, x, y, 1000);
        loadSprites("pillar");
    }

    @Override
    public void tick() {
    }
}
