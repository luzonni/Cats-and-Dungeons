package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.KunaiThrown;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Kunai extends Item {

    private double angle;
    private final double damage = 2;
    private int countAttack;

    public Kunai(int id) {
        super(id, "Kunai", "kunai");
        addSpecifications("Throwable", "Player damage + " + this.damage, "medium speed");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Enemy target = player.getNearest(player.getRange(), Enemy.class);
        if(target != null) {
            this.angle = target.getAngle(player);
            countAttack++;
            if(countAttack >= 60) {
                countAttack = 0;
                double currentDamage = player.getDamage() + this.damage;
                KunaiThrown kunai = new KunaiThrown(player.getX(), player.getY(), currentDamage, this.angle);
                Game.getMap().put(kunai);
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        double middleX = player.getX() + player.getWidth()/2d;
        double middleY = player.getY() + player.getHeight()/2d;
        BufferedImage sprite = getSprite();
        Point spriteRotatePosition = new Point((int)(2.5*Configs.GameScale()), (int)(13.5*Configs.GameScale()));
        double drawX = (middleX - spriteRotatePosition.x) + Math.cos(this.angle) * player.getWidth()/2d;
        double drawY = (middleY - spriteRotatePosition.y) + Math.sin(this.angle) * player.getHeight()/2d;
        Rotate.draw(sprite, (int)drawX - Game.C.getX(), (int)drawY - Game.C.getY(), this.angle + Math.PI/4, spriteRotatePosition, g);
    }
}