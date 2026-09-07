package com.retronova.game.items;

import java.awt.*;

public class Wand extends Item {

    Wand(int id) {
        // O arquivo se chama magicstick.png. Pedindo "wand" o SpriteHandler nao
        // achava nada e devolvia a imagem de erro — a varinha aparecia como um
        // xadrez magenta na hotbar, no inventario e na loja, nao so na ficha de
        // personagem. Nenhum sprite chamado wand jamais existiu no projeto.
        super(id, "Wand", "magicstick");
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {

    }
}
