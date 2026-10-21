package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.utilities.Skull;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Skeleton extends Enemy {

    /**
     * Ticks entre duas caveiras. ERA 45, e o esqueleto e o unico bicho que bate
     * de longe: com meia duzia deles na tela, eram oito caveiras por segundo
     * vindas de onde o gato nao alcanca, e a partida acabava no minuto tres
     * contra um gato que ainda nem tinha build. Com 110 ele continua sendo o
     * bicho que obriga a se mexer, sem ser o que decide a partida sozinho.
     */
    private static final int RECARGA = 110;
    /** Alcance do tiro, em tiles. Mais curto que a tela: ele tem de aparecer antes de atirar. */
    private static final int ALCANCE = 7;

    private int countAnim;
    private int cooldown;

    public Skeleton(int ID, double x, double y) {
        super(ID, x, y, 25);
        // Cada um comeca num ponto diferente da recarga: um grupo que nasce junto
        // nao dispara em rajada sincronizada.
        this.cooldown = Engine.RAND.nextInt(RECARGA);
        loadSprites("mouseskeleton");
        setLife(60);
        setXpWeight(6d);
        setSpeed(2.0);
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
        if(cooldown >= RECARGA && player.getDistance(this) <= GameObject.SIZE() * ALCANCE) {
            cooldown = 0;
            Skull skull = new Skull(getX() + getWidth() / 2d, getY() + getHeight() / 2d,
                    player.getAngle(this), this);
            Game.getMap().put(skull);
        }
    }

    private void moveIA() {
        Player player = Game.getPlayer();
        double radians = anguloDeCerco();   // fecha o cerco em vez de vir em fila
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