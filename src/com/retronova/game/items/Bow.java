package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.Arrow;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Bow extends Item {

    private double angle = 0;
    private int count;
    private int countShot;
    private final BufferedImage arrowSprite;

    Bow(int id) {
        super(id, "Bow", "bow");
        addSpecifications("Arrow add poisson", "player damage", "shot slowed");
        this.arrowSprite = new Arrow(0,0,0,0, null).getSprite();
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Enemy nearest = player.getNearest(player.getRange(), Enemy.class);
        if(nearest != null){
            angle = nearest.getAngle(player);
            count++;
            if(count > (player.getAttackSpeed()*3.25d)/5) {
                count = 0;
                countShot++;
                this.plusIndexSprite();
            }
            if(countShot >= 5) {
                countShot = 0;
                shot(player);
            }
        }else {
            resetIndexSprite();
        }
    }

    private void shot(Player shooter) {
        double x = shooter.getX();
        double y = shooter.getY();
        Arrow arrow = new Arrow(x, y, shooter.getDamage(), angle, shooter);
        Game.getMap().put(arrow);
        Sound.play(Sounds.Bow);
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        int x = (int) player.getX() + player.getWidth()/2 - Game.C.getX();
        int y = (int) player.getY() + player.getHeight()/2 - Game.C.getY();
        double xx = x - getSprite().getWidth()/2d;
        double yy = y - getSprite().getHeight()/2d;
        Rotate.draw(getSprite(), (int)xx, (int)yy, angle + Math.PI/4, null, g);
        renderArrow(player, g);
    }

    private void renderArrow(Player player, Graphics2D g) {
        if(countShot < 1) {
            return;
        }
        double x = player.getX() + Math.cos(angle+Math.PI) * (countShot-3) * Configs.SCALE;
        double y = player.getY() + Math.sin(angle+Math.PI) * (countShot-3) * Configs.SCALE;
        Rotate.draw(arrowSprite, (int)x - Game.C.getX(), (int)y - Game.C.getY(), angle + Math.PI/4, null, g);
    }

}
