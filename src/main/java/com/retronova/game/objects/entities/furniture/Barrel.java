package com.retronova.game.objects.entities.furniture;

/**
 * Barril de madeira. Sugere provisões — alguém se abasteceu aqui.
 */
public class Barrel extends Furniture {

    public Barrel(int ID, double x, double y) {
        this(ID, x, y, "barrel");
    }

    /**
     * @param sprite qual versao desenhar.
     *
     * A mesma peca serve a salas de temas diferentes, e o unico que muda entre elas
     * e a paleta. Passar o nome pelo mapa evita uma classe nova por tema — seriam
     * seis Pilares identicos com uma linha diferente cada.
     */
    public Barrel(int ID, double x, double y, String sprite) {
        super(ID, x, y, 1000, true);
        loadSprites(sprite);
    }

    @Override
    public void tick() {
    }
}
