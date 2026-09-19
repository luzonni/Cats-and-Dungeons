package com.retronova.game.objects.entities.enemies;

import com.retronova.game.objects.Investida;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseVampire extends Enemy {

    private int countAnim;
    private int cooldown;
    private boolean soundPlaying = false;
    private int soundStopDelay = 0;

    public MouseVampire(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousevampire");
        setSolid();
        setLife(60);
        setXpWeight(7d);
        setSpeed(1.6);
        // RAPIDO E FRACO: preparo curto, dano baixo, sem empurrao. Ele pressiona,
        // nao pune — e o contraste com o escudeiro e o que faz o jogador precisar
        // OLHAR qual dos dois esta carregando.
        golpeCorpoACorpo(Investida.rapida(), 1.0, 4, AttackTypes.Melee,
                0, 30, Sounds.MouseVampire);
    }

    public void tick() {
        if (!tickGolpe()) {
            moveIA();
        }
        animation();
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce("move", getSpeed(), radians);
    }

    public void animation() {
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }



    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }
}