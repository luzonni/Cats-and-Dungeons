package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Degraus de pedra. Atravessável.
 *
 * Marca a descida até o portão sem virar obstáculo — a versão anterior usava
 * {@link Bricks}, que é sólido, e a plataforma acabava partindo o salão em dois.
 * Aqui os degraus são leitura visual, não colisão: guiam o olhar até a saída.
 */
public class StoneSteps extends Tile {

    StoneSteps(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("stoneSteps");
    }

    @Override
    public void effect(Entity e) {
    }
}
