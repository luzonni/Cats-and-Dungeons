package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Piso de laje da dungeon, em pedra fria.
 *
 * Substitui o piso roxo-vinho anterior, que era a cor do tema da interface e não
 * de um chão de masmorra.
 *
 * A mesma classe serve às três variantes — inteira, rachada e com limo — porque
 * a única diferença entre elas é o sprite. Elas existem para quebrar a
 * repetição: um tile único de chão deixa a dungeon visivelmente tilada.
 */
public class DungeonFloor extends Tile {

    DungeonFloor(int ID, int x, int y, boolean solid, String sprite) {
        super(ID, x, y, solid);
        loadSprites(sprite);
    }

    @Override
    public void effect(Entity e) {
    }
}
