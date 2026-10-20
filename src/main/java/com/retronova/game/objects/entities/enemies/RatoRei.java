package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.Investida;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * O chefe final da corrida: aparece aos 30 minutos.
 *
 * O relógio da corrida vai até 30:00, e é ele que dá o fim. Até aqui os chefes
 * de cinco em cinco minutos eram gatos; o último é o rato que comanda a horda
 * inteira — muito maior, muito mais duro e com o golpe mais pesado do jogo.
 *
 * Ainda não há arte própria: usa o escudeiro ampliado três vezes, pixel a pixel.
 */
public class RatoRei extends Enemy {

    private static final int AMPLIACAO = 3;

    private int countAnim;
    private final Map<BufferedImage, BufferedImage> ampliados = new IdentityHashMap<>();

    @Override
    public boolean chefe() {
        return true;
    }

    public RatoRei(int ID, double x, double y) {
        super(ID, x, y, 120);
        setWidth(AMPLIACAO);
        setHeight(AMPLIACAO);
        loadSprites("mousesquire");
        setSolid();
        setLife(6000);
        setSpeed(2.6);
        setArmadura(400);
        setXpWeight(300d);
        golpeCorpoACorpo(Investida.pesada(), 2.4, 40, AttackTypes.Melee,
                16, 50, Sounds.MouseSquire);
    }

    @Override
    public void tick() {
        if (!tickGolpe()) {
            Player player = Game.getPlayer();
            getPhysical().addForce("move", getSpeed(), player.getAngle(this));
        }
        if (++countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0];
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(ampliar(getSprite()), 1, orientation);
        renderComAviso(sprite, g);
    }

    private BufferedImage ampliar(BufferedImage original) {
        return ampliados.computeIfAbsent(original, o -> {
            BufferedImage grande = new BufferedImage(o.getWidth() * AMPLIACAO,
                    o.getHeight() * AMPLIACAO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = grande.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(o, 0, 0, grande.getWidth(), grande.getHeight(), null);
            g.dispose();
            return grande;
        });
    }
}
