package com.retronova.game.objects.entities.enemies;

import com.retronova.game.objects.Investida;

import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.objects.entities.AttackTypes;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Zombie extends Enemy {

    private int countAnim;

    public Zombie(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousezombie");
        setLife(40);
        setXpWeight(5d);
        setSpeed(1.2);
        setWidth(0.5);
        setHeight(0.8);
        addResistances(AttackTypes.Fire, 0.5);
        setSolid();
        // O MAIS LENTO GANHA O GOLPE MAIS LENTO. Ele anda a cerca de um terco da
        // velocidade do gato; se o bote dele fosse rapido, a unica leitura possivel
        // seria "nao chegue perto", e nao ha decisao nisso. Com preparo pesado, dar
        // a volta nele passa a ser uma jogada.
        golpeCorpoACorpo(Investida.pesada(), 1.1, 4, AttackTypes.Melee,
                1.5, 40, Sounds.Zombie);
    }

    @Override
    public void tick() {
        // O GOLPE PRENDE O BICHO. Enquanto ele esta no preparo ou no corte, nao
        // anda — e o que torna o aviso visivel e o que permite sair de perto.
        if (!tickGolpe()) {
            moveIA();
        }
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    /**
     * Vai ao PONTO DE CERCO, e nao ao centro do gato: cada zumbi chega pelo
     * proprio flanco. Colado no gato, o ponto e o proprio gato.
     *
     * EM LINHA RETA. Ja houve rota por A* aqui, para as arenas com parede no
     * caminho; no campo aberto a reta chega ao mesmo lugar, e buscar rota para cada
     * zumbi a cada terco de segundo era o maior custo da logica com a horda cheia.
     */
    private void moveIA() {
        getPhysical().addForce("move", getSpeed(), anguloDeCerco());
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderComAviso(sprite, g);
    }
}
