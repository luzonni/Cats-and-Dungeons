package com.retronova.game.items;

import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BloodyAxe extends Item {

    private double angle = 0;
    private final double itemDistance = 20;
    private Enemy enemy;

    BloodyAxe(int id) {
        super(id, "Bloody Axe", "bloody_axe");
        addSpecifications("Life Leech", "Heals a percentage of damage dealt");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        this.enemy = player.getNearest(player.getRange() * 0.3, Enemy.class);

        this.angle += Math.PI / 64;
        if(this.angle > Math.PI / 4){
            this.angle = 0;
            attack(player);
        }
    }

    public void attack(Player player){
        if(enemy == null){
            return;
        }
        double damage = player.getDamage();
        enemy.strike(AttackTypes.Piercing, damage);
        player.setLife(player.getLife() + damage * 0.06);

    }

    @Override
    public void render(Graphics2D g) {
        if(enemy == null){
            return;
        }
        Player player = Game.getPlayer();
        BufferedImage sprite = getSprite();

        int x = (int)player.getX();
        int y = (int)player.getY();
        int side = (Integer.compare((int)enemy.getX(), (int)player.getX())) * player.getWidth();
        double currentAngle = -this.angle;
        if(side > 0){
            sprite = SpriteSheet.flip(sprite, -1 , 1);
            currentAngle -= Math.PI / 2;
        }else {
            currentAngle -= Math.PI / 4;
        }

        Rotate.draw(sprite, x + side, y, currentAngle, null, g);
    }


}