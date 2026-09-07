package com.retronova.game.objects.entities.furniture;

/**
 * Braseiro aceso. Único ponto quente do cenário.
 *
 * Cumpre função narrativa antes de decorativa: fogo aceso diz que alguém esteve
 * aqui há pouco, e marca a antecâmara como área segura. Fogo parado não diz
 * isso — por isso a chama anima.
 */
public class Brazier extends Furniture {

    private final Fogo fogo;

    public Brazier(int ID, double x, double y) {
        super(ID, x, y, 1000);
        loadSprites("brazier");
        this.fogo = new Fogo(this, x, y);
    }

    @Override
    public void tick() {
        fogo.tick();
    }
}
