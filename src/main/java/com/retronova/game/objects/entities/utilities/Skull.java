package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.Scaling;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A cacapa que o esqueleto atira. Mesmo defeito das armas do jogador, virado
 * para o outro lado: com caixa de um tile inteiro ela acertava o gato antes de
 * parecer perto, e morria na parede do corredor logo ao sair.
 */
public class Skull extends Projetil {

    /** Lado do DESENHO em tiles — continua maior que a caixa de colisao. */
    private static final double DESENHO = 0.75;

    private final BufferedImage sprite;
    private final double dir;
    private double r;

    private static final int LIMITE = 60 * 5;

    public Skull(double centroX, double centroY, double dir) {
        this(centroX, centroY, dir, null);
    }

    public Skull(double centroX, double centroY, double dir, Entity dono) {
        super(centroX, centroY, 0, dono);
        this.dir = dir;
        loadSprites("skull");
        if (Engine.RAND.nextBoolean()) {
            getSheet().plusIndex();
        }
        int lado = (int) (DESENHO * GameObject.SIZE());
        this.sprite = Scaling.s(getSprite(), lado, lado);
        setSpeed(1.5);
        setDamage(7.3);
        getPhysical().addForce("moving", getSpeed(), this.dir);
        Sound.play(Sounds.Woosh);
    }

    @Override
    public void tick() {
        Player alvo = avancar(Player.class, LIMITE);
        if (alvo != null) {
            alvo.strike(AttackTypes.Impact, getDamage());
            Sound.play(Sounds.Skeleton);
            disappear();
            return;
        }
        if (acabou()) {
            disappear();
            return;
        }
        r += 0.2d;
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.draw(sprite, (int) (meioX() - sprite.getWidth() / 2d),
                (int) (meioY() - sprite.getHeight() / 2d), r, null, g);
    }
}
