package com.retronova.game.objects.tiles;

import com.retronova.game.objects.entities.Entity;

/**
 * Face frontal da parede de pedra. Sólida.
 *
 * É o lado que se vê de frente, não o que recebe luz: por isso é o tile escuro
 * do par. Vem sempre abaixo de {@link StoneTop}.
 *
 * A mesma classe atende as variantes da face — lisa, com vãos, com relevo e com
 * flâmula — porque entre elas só muda o sprite. Elas existem para quebrar a
 * repetição de paredes longas e, no caso do relevo e da flâmula, para o cenário
 * contar alguma coisa: quem construiu isto, e quem passou por aqui depois.
 */
public class StoneFace extends Tile {

    StoneFace(int ID, int x, int y, boolean solid, String sprite) {
        super(ID, x, y, solid);
        loadSprites(sprite);
    }

    @Override
    public void effect(Entity e) {
    }
}
