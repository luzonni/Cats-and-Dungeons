package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Rocha macica em volta da sala. Solida.
 *
 * Nao existe para ser vista de perto, e sim para nao existir o que havia antes:
 * fora das paredes o mapa era {@link Void}, que desenha transparente, e a camera
 * mostrava um vazio preto sempre que chegava perto da borda. Preenchendo a
 * margem com pedra, a dungeon parece escavada dentro da montanha e o jogador
 * nunca ve o lado de fora do mundo.
 */
public class Bedrock extends Tile {

    Bedrock(int ID, int x, int y, boolean solid) {
        super(ID, x, y, solid);
        loadSprites("bedrock");
    }

    @Override
    public void effect(Entity e) {
    }
}
