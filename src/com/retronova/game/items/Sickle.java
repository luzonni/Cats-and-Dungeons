package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;

public class Sickle extends Item {

    private double rotationAngle = 0;
    private final double rotationSpeed = 0.1;
    private final double damage = 30.0;
    private final double radius = 10 * Configs.GameScale();
    private final Rectangle boundsAttack = new Rectangle(GameObject.SIZE() * 2, GameObject.SIZE() * 2);
    private boolean attacking = false;
    private boolean canDamage = true;

    Sickle(int id) {
        super(id, "Sickle", "sickle");
        addSpecifications("Whirling attack", "Player damage + " + damage, "fast attack speed");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        rotationAngle += rotationSpeed;
        if (rotationAngle > 2 * Math.PI) {
            rotationAngle -= 2 * Math.PI;
        }

        updateBoundsAttack(player);
        Enemy nearest = player.getNearest(radius * 1.5, Enemy.class);

        if (nearest != null) {
            attacking = true;
            if (nearest instanceof Enemy enemy && enemy.colliding(boundsAttack) && canDamage) {
                attack(player, enemy);
                canDamage = false;
            }
        } else {
            attacking = false;
        }

        if (rotationAngle < rotationSpeed * 2) {
            canDamage = true;
        }
    }

    private void updateBoundsAttack(Player player) {
        double playerCenterX = player.getX() + player.getWidth() / 2.0;
        double playerCenterY = player.getY() + player.getHeight() / 2.0;

        double offsetX = Math.cos(rotationAngle) * radius;
        double offsetY = Math.sin(rotationAngle) * radius;

        int x = (int)(playerCenterX + offsetX - boundsAttack.width / 2.0);
        int y = (int)(playerCenterY + offsetY - boundsAttack.height / 2.0);
        boundsAttack.setLocation(x, y);
    }

    private void attack(Player player, Entity target) {
        target.strike(AttackTypes.Melee, damage + player.getDamage());
        // Sound.play(Sounds.Slash);
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        double playerCenterX = player.getX() + player.getWidth() / 2.0;
        double playerCenterY = player.getY() + player.getHeight() / 2.0;

        double offsetX = Math.cos(rotationAngle) * radius;
        double offsetY = Math.sin(rotationAngle) * radius;

        int drawX = (int)(playerCenterX + offsetX - getSprite().getWidth() / 2d);
        int drawY = (int)(playerCenterY + offsetY - getSprite().getHeight() / 2d);

        Rotate.draw(getSprite(), drawX, drawY, rotationAngle + Math.PI / 2, null, g);

        if (attacking) {
            g.setColor(Color.GREEN);
            g.drawRect(boundsAttack.x, boundsAttack.y, boundsAttack.width, boundsAttack.height);
        }
    }

    public double getX() {
        Player player = Game.getPlayer();
        return player.getX() + player.getWidth() / 2 + Math.cos(rotationAngle) * radius;
    }

    public double getY() {
        Player player = Game.getPlayer();
        return player.getY() + player.getHeight() / 2 + Math.sin(rotationAngle) * radius;
    }

    public double getAngle() {
        return rotationAngle;
    }
}