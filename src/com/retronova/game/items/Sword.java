package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
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
                rad -= Math.PI/12;
                if(Math.abs(rad) > Math.PI) {
                    rad = 0;
                    this.nearest.strike(AttackTypes.Melee, player.getDamage());
                }
            }
        }else {
            rad = 0;
        }
    }

    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        BufferedImage sprite = getSprite();
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        Point pointRotate = new Point(3 * Configs.SCALE, 12 * Configs.SCALE);
        double x = player.getX() + player.getWidth()/2d;
        double y = player.getY();
        double radnear = 0;
        if(this.nearest != null) {
            radnear = nearest.getAngle(player);
            double dist = nearest.getDistance(player);
            x = player.getX() + player.getWidth()/2d - pointRotate.x + Math.cos(radnear) * (dist - GameObject.SIZE());
            y = player.getY() + Math.sin(radnear) * (dist - GameObject.SIZE());
        }
        x -= Game.C.getX();
        y -= Game.C.getY();
        Rotate.draw(sprite, (int)x, (int)y, radnear - rad, pointRotate, g);
    }
}
