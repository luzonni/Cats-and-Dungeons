package com.retronova.game.items;

import com.retronova.engine.Debugging;
import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ClawBlades extends Item {

    private double rad;
    private final Rectangle boundsAttack;
    private int side;

    private final Investida investida = Investida.rapida();

    ClawBlades(int id) {
        super(id, "Claw Blades", "claw_blades");
        this.boundsAttack = new Rectangle((int)(GameObject.SIZE()*1.5d),GameObject.SIZE()*2);
        this.side = 1;
    }

    @Override
    protected Porte porte() {
        return Porte.UMA_MAO;
    }

    /**
     * GARRA E ARMA DE ASSASSINO: golpe curto e repetido.
     *
     * Ela tinha a mesma varredura de 180 graus em velocidade constante que a
     * espada tinha — e ali era ainda pior, porque o que se espera de uma garra e
     * uma sequencia rapida, nao um giro. Agora usa a Investida RAPIDA, de 170 ms
     * contra os 400 da espada, alternando a mao a cada golpe.
     */
    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Enemy nearest = alvoVisivel(player, 2);
        setBoundsAttack(player, nearest);
        if (nearest != null) {
            investida.comecar();
        }
        boolean estava = investida.ativa();
        investida.tick();
        this.atacando = investida.ativa();
        if (investida.acertaAgora() && nearest != null) {
            attack(player, nearest);
        }
        if (estava && !investida.ativa()) {
            side *= -1;
        }
    }

    /**
     * A caixa de ataque vai para o lado do ALVO.
     *
     * Antes ela alternava entre o centro e a esquerda conforme o sentido do
     * giro, sem olhar onde o inimigo estava: dava para ver a garra golpeando
     * para a direita e o bicho da esquerda levando dano. Era o mesmo defeito que
     * a referência chama de hitbox que não bate com o que se vê.
     */
    private void setBoundsAttack(Player player, Enemy alvo) {
        double meioX = player.getX() + player.getWidth() / 2d;
        double meioY = player.getY() + player.getHeight() / 2d;
        int lado = 0;
        if (alvo != null) {
            lado = alvo.getX() + alvo.getWidth() / 2d < meioX ? -1 : 1;
        }
        double x = meioX + (lado < 0 ? -this.boundsAttack.width : 0);
        double y = meioY - this.boundsAttack.height / 2d;
        this.boundsAttack.setLocation((int) x, (int) y);
    }

    private void attack(Player player, Enemy enemy) {
        if(enemy.colliding(this.boundsAttack)) {
            enemy.strike(AttackTypes.Piercing, player.getDamage());
            double addLife = player.getLifeSize() * 0.05d;
            player.setLife(player.getLife() + addLife);
        }
    }

    @Override
    public void render(Graphics2D g) {
        // Parado, a pose vem do porte, igual para todas as armas. So o golpe
        // e desenhado por aqui.
        if (!atacando) {
            naMao(g, getSprite());
            return;
        }
        Rectangle rec = this.boundsAttack;
        // Mesmo caminho da espada e do machado: a pose manda no lugar e no arco.
        naMaoGolpeando(g, getSprite(), investida.avanco() * side);
        // A caixa de ataque E O EIXO dela so aparecem com a depuracao ligada.
        // A linha vermelha ficou de fora na primeira passada e continuava
        // desenhada sempre — parecia parte da arma.
        if (Debugging.showEntityHitBox) {
            g.setColor(Color.red);
            g.drawRect(rec.x, rec.y, rec.width, rec.height);
            g.drawLine(rec.x + rec.width / 2, rec.y, rec.x + rec.width / 2, rec.y + rec.height);
        }
    }

    private void renderBlades(int x, int y, Graphics2D g) {
        BufferedImage sprite = getSprite();
        Point pointRotate = empunhadura(sprite);
        if(side == -1) {
            pointRotate.setLocation( 13 * Configs.GameScale(), 12 * Configs.GameScale());
            sprite = SpriteHandler.flip(sprite, 1, -1);
        }
        y -= pointRotate.y;
        x+= (int) (Math.cos(rad) * Configs.GameScale() * 8 * side);
        y+= (int) (Math.sin(rad) * Configs.GameScale() * 4 * side);
        double rotate = (rad);
        Rotate.draw(sprite, x, y, rotate, pointRotate, g);
    }
}
