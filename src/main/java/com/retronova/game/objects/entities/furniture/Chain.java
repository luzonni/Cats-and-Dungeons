package com.retronova.game.objects.entities.furniture;

/**
 * Corrente presa à parede. Sugere que algo foi contido aqui.
 */
public class Chain extends Furniture {

    public Chain(int ID, double x, double y) {
        this(ID, x, y, "chain");
    }

    /**
     * @param sprite qual versao desenhar.
     *
     * A mesma peca serve a salas de temas diferentes, e o unico que muda entre elas
     * e a paleta. Passar o nome pelo mapa evita uma classe nova por tema — seriam
     * seis Pilares identicos com uma linha diferente cada.
     */
    public Chain(int ID, double x, double y, String sprite) {
        super(ID, x, y, 1000, false);
        loadSprites(sprite);
        setGroundObject();
    }

    @Override
    public void tick() {
    }
}
