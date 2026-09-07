package com.retronova.game.objects.entities.furniture;

import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import studio.retrozoni.sheeter.SpriteSheet;

public abstract class Furniture extends Entity {

    Furniture(int ID, double x, double y, int weight) {
        this(ID, x, y, weight, true);
    }

    /**
     * @param solido false para peças que o jogador atravessa — entulho no chão,
     *               vãos de porta. Bloquear tudo faz a sala virar labirinto.
     */
    Furniture(int ID, double x, double y, int weight, boolean solido) {
        super(ID, x, y, weight);
        setSolid(solido);
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new SpriteSheet("sprites/objects/furniture", sprites));
    }

    /**
     * Avança um quadro da folha de sprites.
     *
     * Existe porque {@link Fogo} anima a chama e não é um GameObject: o
     * {@code getSheet()} é protegido e só se alcança de dentro da hierarquia.
     */
    void proximoQuadro() {
        getSheet().plusIndex();
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        //TODO em furniture, caso queiram criar um sistema para destruir-las, pode ser implementada aqui...
    }

    @Override
    public void die() {
        this.disappear();
    }
}
