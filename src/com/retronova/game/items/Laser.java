package com.retronova.game.items;

import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.AttackTypes;

import java.awt.*;

public class Laser extends Item {

    private double angle = 0;
    private int count;
    private int countShot;
    private Entity currentTarget;
    private double itemOffsetX = 30;
    private double itemOffsetY = 25;
    private double laserDistance = 30;

    Laser(int id) {
        super(id, "Laser", "laser");
        addSpecifications("Laser add burn", "player damage", "shot instant");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Entity nearest = player.getNearest(player.getRange(), Enemy.class);

        if (currentTarget != null && !Game.getMap().getEntities(Enemy.class).contains(currentTarget)) {
            currentTarget = null;
        }

        if (nearest != null) {
            currentTarget = nearest;
            angle = nearest.getAngle(player);
            count++;

            if (count > (player.getAttackSpeed() * 3.25d) / 5) {
                count = 0;
                countShot++;
                this.plusIndexSprite();
            }

            if (countShot >= 5) {
                countShot = 0;
                shot(player);
            }
        } else {
            resetIndexSprite();
        }
    }

    private void shot(Player shooter) {
        double constantDamage = 10.0;

        if (currentTarget != null) {
            currentTarget.strike(AttackTypes.Laser, constantDamage);
        }
        Sound.play(Sounds.Laser);
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        int x = (int) player.getX() + player.getWidth() / 2 - Game.C.getX();
        int y = (int) player.getY() + player.getHeight() / 2 - Game.C.getY();
        double xx = x - getSprite().getWidth() / 2d + itemOffsetX;
        double yy = y - getSprite().getHeight() / 2d + itemOffsetY;
        Rotate.draw(getSprite(), (int) xx, (int) yy, angle + Math.PI / 4, null, g);
    }

    public double getX() {
        Player player = Game.getPlayer();
        double centerX = player.getX() + player.getWidth() / 2.0;
        return centerX + Math.cos(angle) * laserDistance;
    }

    public double getY() {
        Player player = Game.getPlayer();
        double centerY = player.getY() + player.getHeight() / 2.0;
        return centerY + Math.sin(angle) * laserDistance;
    }

    public double getAngle() {
        return angle;
    }
}