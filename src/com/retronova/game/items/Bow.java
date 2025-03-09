package com.retronova.game.items;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Arrow;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Bow extends Item {

    private double angle = 0;
    private int count;
    private int lastCount;

    Bow(int id) {
        super(id, "Bow", "bow");
        addSpecifications("Arrow add poisson", "player damage");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Entity nearest = player.getNearest(player.getRange());
        if(nearest != null){
            angle = nearest.getAngle(player);
            count++;
            if(count > lastCount + player.getAttackSpeed()/5) {
                lastCount = count;
                this.plusIndexSprite();
            }
            if(count >= player.getAttackSpeed()) {
                count = 0;
                lastCount = 0;
                double x = player.getX() + player.getWidth() * Math.cos(angle);
                double y = player.getY() + player.getHeight() * Math.sin(angle);
                Arrow arrow = new Arrow(x, y, player.getDamage(), angle, player);
                Game.getMap().getEntities().add(arrow);
                Sound.play(Sounds.Bow);
            }
        }else {
            resetIndexSprite();
        }
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        int x = (int) player.getX() + player.getWidth()/2 - Game.C.getX();
        int y = (int) player.getY() + player.getHeight()/2 - Game.C.getY();
        double xx = x - getSprite().getWidth()/2d;
        double yy = y - getSprite().getHeight()/2d;
        Rotate.draw(getSprite(), (int)xx, (int)yy, angle + Math.PI/4, null, g);
    }
}
