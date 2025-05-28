package com.retronova.game.items;

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

    private int countdown;

    ClawBlades(int id) {
        super(id, "Claw Blades", "claw_blades");
        this.boundsAttack = new Rectangle((int)(GameObject.SIZE()*1.5d),GameObject.SIZE()*2);
        this.side = 1;
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        setBoundsAttack(player);
        Enemy nearest = player.getNearest(2, Enemy.class);
        if(nearest != null) {
            countdown++;
            if(countdown > player.getAttackSpeed()*0.1) {
                rad += (Math.PI/12) * side;
                if(Math.abs(rad) > Math.PI) {
                    countdown = 0;
                    rad = 0;
                    side *= -1;
                    attack(player, nearest);
                }
            }
        }else {
            rad = 0;
        }
    }

    private void setBoundsAttack(Player player) {
        int dist = (this.side == -1) ? this.boundsAttack.width * side : 0;
        double x = (player.getX() + player.getWidth()/2d) + dist;
        double y = (player.getY() + player.getHeight()/2d) - this.boundsAttack.height/2d;
        this.boundsAttack.setLocation((int)x, (int)y);
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
        Rectangle rec = this.boundsAttack;
        Player player = Game.getPlayer();
        double x = player.getX();
        double y = player.getY() + player.getHeight()/1.5d;
        renderBlades((int)x, (int)y, g);
        g.setColor(Color.red);
        g.drawRect(rec.x, rec.y, rec.width, rec.height);
        g.drawLine(rec.x + rec.width/2, rec.y, rec.x + rec.width/2, rec.y + rec.height);
    }

    private void renderBlades(int x, int y, Graphics2D g) {
        BufferedImage sprite = getSprite();
        Point pointRotate = new Point(3 * Configs.GameScale(), 12 * Configs.GameScale());
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
