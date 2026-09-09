package com.retronova.game.objects.entities.furniture;

/**
 * Entulho de pedra. Set dressing barato, espalhado para quebrar a repetição do
 * piso e sugerir ruína.
 *
 * Atravessável de propósito: são cacos no chão, não uma barreira. Sólido, cada
 * peça virava uma parede invisível e a sala virava labirinto.
 */
public class Rubble extends Furniture {

    public Rubble(int ID, double x, double y) {
        this(ID, x, y, "rubble");
    }

    /**
     * @param sprite qual versao desenhar.
     *
     * A mesma peca serve a salas de temas diferentes, e o unico que muda entre elas
     * e a paleta. Passar o nome pelo mapa evita uma classe nova por tema — seriam
     * seis Pilares identicos com uma linha diferente cada.
     */
    public Rubble(int ID, double x, double y, String sprite) {
        super(ID, x, y, 1000, false);
        loadSprites(sprite);
        setGroundObject();
    }

    @Override
    public void tick() {
    }
}
