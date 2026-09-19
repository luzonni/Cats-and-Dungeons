package com.retronova.game.objects.entities.enemies;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.utilities.Skull;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Skeleton extends Enemy {

    private int countAnim;
    private int cooldown;

    public Skeleton(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mouseskeleton");
        setLife(60);
        setSpeed(3);
        // AS RESISTENCIAS DE TIPO DE ARMA SAIRAM. Ele resistia a Piercing em 0,9
        // e a Poison em 0,8 — e Piercing e o dano do ARCO. Enquanto todo gato podia
        // carregar qualquer arma, isso era textura: quem batia mal de flecha trocava
        // de arma. Com a classe travada, virou imposto cobrado de um gato so: o Finn
        // levava treze segundos para matar um esqueleto que o Muffin resolve em meio
        // segundo. Resistencia so e interessante quando ha resposta, e nao ha.
        //
        // A resistencia a fogo fica: essa e ELEMENTAL, e elemento vai ser escolha de
        // corrida. Ali ela volta a ter resposta — e vai ganhar o espelho dela, a
        // fraqueza, para a escolha premiar e nao so punir.
        addResistances(AttackTypes.Fire, 0.5);
        setSolid();
    }

    public void tick() {
        moveIA();
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
        Player player = Game.getPlayer();
        cooldown++;
        if(cooldown >= 45 && player.getDistance(this) <= GameObject.SIZE() * 8) {
            cooldown = 0;
            Skull skull = new Skull(getX() + getWidth() / 2d, getY() + getHeight() / 2d,
                    player.getAngle(this), this);
            Game.getMap().put(skull);
        }
    }

    private void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        if(player.getDistance(this) >= GameObject.SIZE() * 3){
            getPhysical().addForce("move", getSpeed(), radians);
        }

    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
    }
}