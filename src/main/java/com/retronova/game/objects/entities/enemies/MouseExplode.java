package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Smoke;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseExplode extends Enemy {

    private int countAnim;

    public MouseExplode(int ID, double x, double y) {
        super(ID,x,y,25);
        loadSprites("ratexplode");
        setDamage(33);
        setSolid();
        setSpeed(1.3d);
        setLife(200);
        setXpWeight(12.4d);
    }

    public void tick() {
        if (pavio >= 0) {
            // ACESO, ELE PARA. Um bicho que persegue enquanto o pavio queima nao da
            // para evitar — a unica leitura seria correr, e correr de um bicho mais
            // lento que voce nao e decisao nenhuma.
            if (--pavio <= 0) {
                explodir(Game.getPlayer());
            }
        } else {
            moveIA();
        }
        animar();
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        // ELE VOLTOU A ANDAR. A linha que o movia estava COMENTADA, entao ele
        // nascia com duzentos de vida e trinta e tres de dano e ficava plantado ate
        // alguem encostar. Como as ondas da sala doze em diante sao feitas dele,
        // metade do jogo era um campo de estatuas.
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce("move", getSpeed(), radians);
        if (this.getDistance(player) < GameObject.SIZE() * ALCANCE) {
            acender();
        }
    }

    /**
     * Quadros entre acender o pavio e explodir.
     *
     * A EXPLOSAO PRECISA DE MAIS AVISO QUE QUALQUER OUTRO GOLPE, porque e a que
     * mais machuca: trinta e tres de dano contra oitenta de vida. Sem pavio, ela
     * era um pedagio por chegar perto; com pavio, vira uma corrida — da para
     * recuar, matar antes, ou aceitar a troca. Cinquenta quadros sao quase um
     * segundo, longos de proposito.
     */
    private static final int PAVIO = 50;

    /** A que distancia o pavio acende, em tiles. */
    private static final double ALCANCE = 1.8;

    private int pavio = -1;

    private void acender() {
        if (pavio < 0) {
            pavio = PAVIO;
            Sound.play(Sounds.Crack);
        }
    }

    /** De 0 a 1 conforme o pavio queima. E o clarao que avisa. */
    private float carga() {
        return pavio < 0 ? 0f : 1f - pavio / (float) PAVIO;
    }

    private void explodir(Player player) {
        // SO ACERTA QUEM AINDA ESTIVER PERTO. O pavio nao vale nada se o dano sair
        // de qualquer jeito: e esta conferencia que transforma o aviso em chance.
        if (player.getDistance(this) < GameObject.SIZE() * ALCANCE * 1.6) {
            player.strike(AttackTypes.Explosion, getDamage());
        }
        double range = GameObject.SIZE()*1.5d;
        for(int i = 0; i < 60; i++) {
            double angle = Engine.RAND.nextDouble(Math.toRadians(360));
            double x = getX() + Math.cos(angle) * Engine.RAND.nextDouble(range);
            double y = getY() + Math.sin(angle) * Engine.RAND.nextDouble(range);
            Particle smoke = new Smoke(x, y, 1.2, Engine.RAND.nextDouble(Math.PI*2));
            Game.getMap().put(smoke);
        }
        Sound.play(Sounds.MouseExplode);
        disappear();
    }

    private void animar() {
        countAnim ++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }


    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        float carga = carga();
        renderSprite(carga > 0.01f
                ? com.retronova.game.objects.entities.Expressao.clarao(sprite, carga)
                : sprite, d);
    }

}
