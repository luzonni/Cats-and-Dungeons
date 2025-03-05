package com.retronova.game.items;

import com.retronova.engine.Engine;
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
                Arrow arrow = new Arrow(x, y, player.getRangeDamage(), angle);
                Game.getMap().getEntities().add(arrow);
            }
        }else {
            resetIndexSprite();
        }
    }

    @Override
    public void render(Graphics2D g) {
        //TODO tem um bug no sprite do arco, acredito que esse bug visual so aconteça no linux....
        Player player = Game.getPlayer();
        int x = (int) player.getX() + player.getWidth()/2 - Game.C.getX();
        int y = (int) player.getY() + player.getHeight()/2 - Game.C.getY();
        double xx = x - getSprite().getWidth()/2d;
        double yy = y - getSprite().getHeight()/2d;
        AffineTransform at = new AffineTransform();
        at.translate(xx, yy);
        at.rotate(angle + Math.PI/4, getSprite().getWidth()/2d, getSprite().getHeight()/2d);
        g.drawImage(getSprite(), at, null);
    }
}
