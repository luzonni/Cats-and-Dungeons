package com.retronova.game.items;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BloodyAxe extends Item {

    private double angle = 0;
    private final double itemDistance = 20;

    BloodyAxe(int id) {
        super(id, "Bloody Axe", "bloody_axe");
        addSpecifications("Life Leech", "Heals a percentage of damage dealt");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Enemy nearest = player.getNearest(player.getRange(), Enemy.class);

        if (nearest != null) {
            this.angle = nearest.getAngle(player);
            double damage = player.getDamage();
            if (player.getNearest(2, Enemy.class) == nearest && nearest.colliding(new Rectangle((int)getX() - 10, (int)getY() - 10, 20, 20))) {
                nearest.strike(AttackTypes.Melee, damage);
                player.setLife(player.getLife() + damage * (0.10));
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        BufferedImage sprite = getSprite();

        if (sprite != null) {
            double centerX = player.getX() + player.getWidth() / 2.0;
            double centerY = player.getY() + player.getHeight() / 2.0;

            double drawX = centerX + Math.cos(angle) * itemDistance - sprite.getWidth() / 2 - Game.C.getX();
            double drawY = centerY + Math.sin(angle) * itemDistance - sprite.getHeight() / 2 - Game.C.getY();

            Rotate.draw(sprite, (int) drawX, (int) drawY, angle + Math.PI / 4, new Point(sprite.getWidth() / 2, sprite.getHeight() / 2), g);
        }
    }

    public double getX() {
        Player player = Game.getPlayer();
        double centerX = player.getX() + player.getWidth() / 2.0;
        return centerX + Math.cos(angle) * itemDistance;
    }

    public double getY() {
        Player player = Game.getPlayer();
        double centerY = player.getY() + player.getHeight() / 2.0;
        return centerY + Math.sin(angle) * itemDistance;
    }

    public double getAngle() {
        return angle;
    }
}