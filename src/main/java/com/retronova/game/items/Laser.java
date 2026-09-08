package com.retronova.game.items;

import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.BasicStroke;
import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.AttackTypes;

import java.awt.*;

public class Laser extends Item {

    private double angle = 0;
    private int count;

    /** Giro a mais somado a direcao do tiro. Vem do graus da pose. */
    private double giroExtra() {
        Poses.Pose p = pose();
        return p == null ? 0 : Math.toRadians(p.graus());
    }

    private int countShot;
    private Entity currentTarget;
    private double laserDistance = 30;

    Laser(int id) {
        super(id, "Laser", "laser");
        addSpecifications("Laser add burn", "player damage", "shot instant");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        // APONTA para o mais proximo mesmo com parede no meio, e so ATIRA quando
        // ha caminho. Amarrar as duas coisas deixava o blaster travado no ultimo
        // angulo, apontado para o nada, enquanto o inimigo passava na frente.
        Entity nearest = alvoAlcancavel(player, player.getRange());
        Entity paraApontar = nearest != null ? nearest
                : alvoParaMirar(player, player.getRange());
        if (paraApontar != null) {
            angle = paraApontar.getAngle(player);
        }

        if (currentTarget != null && !Game.getMap().getEntities(Enemy.class).contains(currentTarget)) {
            currentTarget = null;
        }

        if (nearest != null) {
            currentTarget = nearest;
            count++;

            if (count > (player.getAttackSpeed() * 3.25d) / 5) {
                count = 0;
                countShot++;
                this.plusIndexSprite();
            }

            if (countShot >= 5) {
                countShot = 0;
                shot(player);
            }
        } else {
            resetIndexSprite();
        }
    }

    private void shot(Player shooter) {
        double constantDamage = 10.0;

        if (currentTarget != null) {
            currentTarget.strike(AttackTypes.Laser, constantDamage);
        }
        Sound.play(Sounds.Laser);
    }

    /**
     * O feixe.
     *
     * O laser dava dano e fazia barulho sem nada sair dele — a piada do gato que
     * atira laser não existe se não houver laser. São três linhas sobrepostas:
     * uma larga e translúcida que faz o brilho, uma média e uma fina e branca no
     * meio, que é o que dá a impressão de núcleo quente. É como se desenha feixe
     * em jogo: o branco no centro é a intensidade, o vermelho em volta é o calor.
     */
    private void desenharFeixe(Graphics2D g, Player player) {
        if (currentTarget == null) {
            return;
        }
        // O feixe sai da BOCA do cano, definida no editor. Sem isso ele nascia no
        // meio do desenho, ou seja, no meio da arma.
        java.awt.geom.Point2D.Double a = ancoraDeMira(player, angle, 0);
        java.awt.geom.Point2D.Double b =
                boca(getSprite(), a.x, a.y, angle + giroExtra(), Rotate.PARA_DIREITA);
        int x1 = (int) b.x;
        int y1 = (int) b.y;
        int x2 = (int) (currentTarget.getX() + currentTarget.getWidth() / 2d);
        int y2 = (int) (currentTarget.getY() + currentTarget.getHeight() / 2d);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int e = Configs.GameScale();
        // A carga aparece no feixe: ele engrossa conforme o tiro se aproxima.
        float carga = Math.min(1f, countShot / 4f);
        g2.setStroke(new BasicStroke(Math.max(1f, e * (1.6f + carga * 1.4f))));
        g2.setColor(new Color(255, 60, 60, (int) (60 + carga * 50)));
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(new BasicStroke(Math.max(1f, e * (0.8f + carga * 0.6f))));
        g2.setColor(new Color(255, 120, 120, (int) (120 + carga * 80)));
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(new BasicStroke(Math.max(1f, e * 0.4f)));
        g2.setColor(new Color(255, 240, 240, (int) (180 + carga * 60)));
        g2.drawLine(x1, y1, x2, y2);
        g2.dispose();
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        desenharFeixe(g, player);
        int x = (int) player.getX() + player.getWidth() / 2;
        int y = (int) player.getY() + player.getHeight() / 2;
        // ADIANTADO na direcao da mira e ancorado pelo MIOLO DO DESENHO, como o
        // arco. O sprite novo vem dos pacotes, desenhado na diagonal, entao o
        // facing e DIAGONAL — com o +PI/4 solto de antes a arma ficava torta.
        // Distancia, desvio e giro vem da pose, como no arco: laser tambem e
        // arma apontada, e ate agora os campos do editor nao mexiam nela.
        java.awt.geom.Point2D.Double a = ancoraDeMira(player, angle, 0);
        double xx = a.x;
        double yy = a.y;
        // O blaster e desenhado DEITADO, apontando para a direita — os pacotes de
        // arma de fogo desenham assim, ao contrario dos de RPG, que usam a
        // diagonal. Com DIAGONAL ele saia quarenta e cinco graus fora da mira.
        Rotate.apontar(getSprite(), xx, yy, angle + giroExtra(), Rotate.PARA_DIREITA, g);
    }

    public double getX() {
        Player player = Game.getPlayer();
        double centerX = player.getX() + player.getWidth() / 2.0;
        return centerX + Math.cos(angle) * laserDistance;
    }

    public double getY() {
        Player player = Game.getPlayer();
        double centerY = player.getY() + player.getHeight() / 2.0;
        return centerY + Math.sin(angle) * laserDistance;
    }

    public double getAngle() {
        return angle;
    }
}