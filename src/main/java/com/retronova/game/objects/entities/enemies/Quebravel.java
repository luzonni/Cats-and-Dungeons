package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.utilities.Bau;
import com.retronova.game.objects.entities.utilities.Cura;
import com.retronova.game.objects.entities.utilities.Xp;
import com.retronova.game.objects.particles.Cadaver;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Caixote ou barril espalhado pela arena, que quebra com qualquer golpe.
 *
 * POR QUE E UM Enemy. As armas — espada, arco, novelo, aura, todas — procuram
 * alvos do tipo Enemy. Sendo um, ele apanha de tudo sem que nenhuma arma precise
 * saber que ele existe. O que o separa de um bicho e alvoAutomatico(): a mira das
 * armas o ignora (o gato nao gasta flecha em caixote com rato do lado) e ele nao
 * conta para a onda terminar.
 *
 * O PREMIO e o motivo de desviar a rota: um coracao que cura ou um orbe grande
 * de XP. Pouco, mas imediato — e e isso que faz o mapa valer a pena ser andado.
 */
public class Quebravel extends Enemy {

    /** Chance de o premio ser cura; o resto e XP grande. */
    private static final double CHANCE_DE_CURA = 0.4;
    /** Quanto vale o orbe grande — umas cinco mortes de zumbi. */
    private static final double XP_GRANDE = 25;

    public Quebravel(double x, double y, boolean barril) {
        super(-1, x, y, 1000);
        setSheet(new SpriteSheet("sprites/objects/furniture",
                new String[]{barril ? "barrelCistern" : "crate"}));
        getSheet().setIndex(Engine.RAND.nextInt(getSheet().size()));
        setLife(16);
        setSolid();
        nascerPronto();
    }

    @Override
    public boolean alvoAutomatico() {
        return false;
    }

    @Override
    public void tick() {
        // Parado: so o piscar do golpe anda, e isso e o postTick do Enemy.
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    @Override
    public void die() {
        disappear();
        Sound.play(Sounds.Crack);
        BufferedImage pedaco = getSprite();
        Game.getMap().put(new Cadaver(pedaco,
                getX() + (getWidth() - pedaco.getWidth()) / 2.0,
                getY() + getHeight() - pedaco.getHeight()));
        double cx = getX() + getWidth() / 4.0;
        double cy = getY() + getHeight() / 4.0;
        if (Engine.RAND.nextDouble() < CHANCE_DE_CURA) {
            Game.getMap().put(new Cura(cx, cy));
        } else {
            Xp orbe = new Xp(cx, cy).grande();
            orbe.setWeight(XP_GRANDE);
            Game.getMap().put(orbe);
        }
        // Um bau de vez em quando, metade da chance de um bicho.
        if (Engine.RAND.nextDouble() < Game.getPlayer().chanceDeBau() * 0.5) {
            Game.getMap().put(new Bau(getX(), getY()));
        }
    }
}
