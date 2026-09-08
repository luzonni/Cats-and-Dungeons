package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Alpha;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A seda salta de inimigo em inimigo, perdendo forca a cada salto.
 *
 * O peso continua alto de proposito: e o atrito que faz a curva ficar redonda
 * quando ela troca de alvo. O que muda e a caixa, que agora e a pequena do
 * projetil, e o teste de acerto, que passa a ser por varredura.
 */
public class Silk extends Projetil {

    private final List<Entity> visited;
    private float fading = 1f;
    private double r;

    public Silk(double centroX, double centroY, double damage, Entity shooter) {
        super(centroX, centroY, 20, shooter);
        setSpeed(7d);
        loadSprites("silk");
        setDamage(damage);
        this.visited = new ArrayList<>();
        this.visited.add(shooter);
    }

    @Override
    public void tick() {
        if (visited.size() >= 5) {
            this.disappear();
            return;
        }
        // O ACERTO VEM PRIMEIRO e nao depende de nada: raspando a parede ou nao,
        // quem a seda cruzou neste tick leva dano. Enquanto isso estava dentro do
        // "se ha alvo e nao bati na parede", um inimigo colado no muro era
        // atravessado sem levar arranhao.
        Enemy alvo = atingido(Enemy.class);
        if (alvo != null && !visited.contains(alvo)) {
            alvo.strike(AttackTypes.Flat, getDamage());
            setDamage(getDamage() - getDamage() * 0.25d);
            visited.add(alvo);
        }
        Enemy nearest = getNearest(Game.getPlayer().getRange(), Enemy.class, visited.getLast());
        if (nearest != null && !visited.contains(nearest) && !bateuNaParede()) {
            r += 0.24d;
            getPhysical().addForce("follow_silk", getSpeed(), nearest.getAngle(this));
            fading = 1;
        } else {
            fading -= 0.015f;
            if (fading <= 0.1) {
                this.disappear();
            }
        }
        marcarPosicao();
    }

    @Override
    public void render(Graphics2D g) {
        renderLine(g);
        BufferedImage sp = Alpha.getImage(getSprite(), fading);
        Rotate.draw(sp, (int) (meioX() - sp.getWidth() / 2d),
                (int) (meioY() - sp.getHeight() / 2d), r, null, g);
    }

    private void renderLine(Graphics2D g) {
        g.setStroke(new BasicStroke(Configs.GameScale()));
        g.setColor(new Color(243, 113, 153));
        int x = (int) meioX();
        int y = (int) meioY();
        for (int i = visited.size() - 1; i >= 0; i--) {
            Entity v = visited.get(i);
            int xx = (int) v.getX() + v.getWidth() / 2;
            int yy = (int) v.getY() + v.getHeight() / 2;
            g.drawLine(xx, yy, x, y);
            x = xx;
            y = yy;
        }
    }
}
