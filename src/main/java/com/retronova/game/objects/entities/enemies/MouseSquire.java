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

public class MouseSquire extends Enemy {

    private int countAnim;
    private int cooldown;
    private boolean soundPlaying = false;
    private int soundStopDelay = 0;

    public MouseSquire(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousesquire");
        setSolid();
        setLife(70);
        setSpeed(3);
        // Ver Skeleton para o porque. Aqui havia ainda Poison 1.0 — IMUNIDADE
        // total —, e o arco anuncia "adiciona veneno": o efeito impresso na arma
        // valia exatamente zero contra este bicho, sem nada dizer ao jogador.
        addResistances(AttackTypes.Fire, 0.6);
        setXpWeight(9d);
        // O "Near" DE REFERENCIA: dano alto, empurrao forte, preparo medio. E dele
        // que o jogador aprende a ler o clarao, porque ele aparece cedo e bate o
        // bastante para doer.
        golpeCorpoACorpo(Investida.media(), 1.2, 8, AttackTypes.Melee,
                8, 45, Sounds.MouseSquire);
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

    @Override
    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0];
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderComAviso(sprite, d);
    }
}