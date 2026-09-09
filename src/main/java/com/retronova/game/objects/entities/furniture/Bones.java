package com.retronova.game.objects.entities.furniture;

/**
 * Ossada no chão. Conta, sem uma linha de texto, que alguém não voltou.
 */
public class Bones extends Furniture {

    public Bones(int ID, double x, double y) {
        this(ID, x, y, "bones");
    }

    /**
     * @param sprite qual versao desenhar.
     *
     * A mesma peca serve a salas de temas diferentes, e o unico que muda entre elas
     * e a paleta. Passar o nome pelo mapa evita uma classe nova por tema — seriam
     * seis Pilares identicos com uma linha diferente cada.
     */
    public Bones(int ID, double x, double y, String sprite) {
        super(ID, x, y, 1000, false);
        loadSprites(sprite);
        setGroundObject();
    }

    @Override
    public void tick() {
    }
}
