package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Sword extends Item {

    private double rad;
    private int count;
    private Entity nearest;

    Sword(int id) {
        super(id, "Sword", "sword");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        this.nearest = player.getNearest(3);
        if(nearest != null) {
            count++;
            if(count > 7) {
                if(player.getX() > nearest.getX()) {
                    rad -= Math.PI / 12d;
                }else {
                    rad += Math.PI / 12d;
                }
                if(Math.abs(rad) > Math.PI) {
                    rad = 0;
                    this.nearest.strike(AttackTypes.Melee, player.getDamage());
                    //TODO esse efeito eh apenas para testes
                    this.nearest.addEffect("slowness", e -> e.getPhysical().setFriction(0.9), 5);
                }
            }
        }else {
            rad = 0;
        }
    }

    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        double x = player.getX() + player.getWidth()/2d - Game.C.getX();
        double y = player.getY() + player.getHeight()/2d - Game.C.getY();
        if(this.nearest != null)
            if(player.getX() > nearest.getX()) {
                x -= player.getWidth()/2d;
            }else {
                x += player.getWidth()/2d;
            }
        renderSword((int)x, (int)y, g);
        g.setColor(Color.red);
        g.fillRect((int)x, (int)y, Configs.SCALE, Configs.SCALE);
    }

    private void renderSword(int x, int y, Graphics2D g) {
        BufferedImage sprite = getSprite();
        Point pointRotate = new Point(3 * Configs.SCALE, 12 * Configs.SCALE);
        x -= pointRotate.x;
        y -= pointRotate.y;
        Rotate.draw(sprite, x, y, rad - Math.PI/4, pointRotate, g);
    }

}
