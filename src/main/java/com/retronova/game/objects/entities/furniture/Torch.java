package com.retronova.game.objects.entities.furniture;

/**
 * Tocha de parede. Luz e sinal de presença recente.
 *
 * Atravessável e no chão: fica encostada na parede, e travar passagem por causa
 * de um enfeite de um tile só atrapalharia a circulação da sala.
 */
public class Torch extends Furniture {

    private final Fogo fogo;

    public Torch(int ID, double x, double y) {
        super(ID, x, y, 1000, false);
        loadSprites("torch");
        setGroundObject();
        this.fogo = new Fogo(this, x, y);
    }

    @Override
    public void tick() {
        fogo.tick();
    }
}
