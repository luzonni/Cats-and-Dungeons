package com.retronova.game.objects.entities.enemies;

import com.retronova.game.objects.GameObject;

import com.retronova.engine.sound.Sounds;

import com.retronova.game.objects.entities.AttackTypes;

import com.retronova.game.objects.Investida;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MonarkMouse extends Enemy {

    private int countAnim;

    public MonarkMouse(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("monarkmouse");
        setSolid();
        setLife(120);
        setSpeed(1.1);
        setXpWeight(11d);
        // O HEAVY GANHA ARMADURA. E o papel dele: nao ser rapido nem esperto, e sim
        // demorar a cair. Antes isso seria feito com resistencia, e resistencia
        // escolhe de quem ele demora a cair.
        setArmadura(60);
        // ELE NAO TINHA ATAQUE NENHUM. Andava a meia velocidade do zumbi e nunca
        // machucava — um alvo ambulante. E as ondas da sala doze em diante sao
        // feitas dele, entao o fim do jogo era mais VAZIO que o comeco, e nao mais
        // dificil.
        //
        // Vira o "Heavy" do elenco: lento, aguenta, e o golpe mais pesado depois da
        // explosao. Preparo longo e alcance maior que o dos outros, para ele
        // ameacar um espaco em vez de um ponto.
        golpeCorpoACorpo(Investida.pesada(), 1.6, 12, AttackTypes.Melee,
                6, 60, Sounds.MouseSquire);
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
        if(countAnim > 10) {
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
        renderComAviso(sprite, d);
    }

    @Override
    public void dispose() {}
}






