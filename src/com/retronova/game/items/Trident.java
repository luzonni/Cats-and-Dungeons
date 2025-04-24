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

public class Trident extends Item {

    private double angle = 0;
    private boolean attacking = false;
    private boolean damageApplied = false;
    private final double damage = 45.0;
    private final double attackSpeed = 0.03;
    private final double attackDistance = 10 * Configs.GameScale();
    private double attackProgress = 0;
    private final Rectangle boundsAttack = new Rectangle(GameObject.SIZE() * 2, GameObject.SIZE() * 2);
    private final int horizontalOffset = -30;

    Trident(int id) {
        super(id, "Trident", "trident");
        addSpecifications("Stab forward", "player damage + " + damage, "medium attack speed");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Entity nearest = player.getNearest(player.getRange(), Enemy.class);

        if (nearest != null) {
            angle = nearest.getAngle(player);

            updateBoundsAttack(player, 0);

            boolean isColliding = nearest instanceof Enemy enemy && enemy.colliding(boundsAttack);

            if (!attacking && isColliding) {
                attacking = true;
                attackProgress = 0;
                damageApplied = false;
            }

            if (attacking) {
                attackProgress += attackSpeed;

                double currentOffset = Math.sin(attackProgress * Math.PI) * attackDistance;
                updateBoundsAttack(player, currentOffset);

                if (attackProgress >= 1) {
                    attacking = false;
                    attackProgress = 0;
                } else if (attackProgress > 0.5 && !damageApplied && isColliding) {
                    attack(player, (Enemy) nearest);
                    damageApplied = true;
                }
            }

        } else {
            attacking = false;
            attackProgress = 0;
        }
    }


    private void updateBoundsAttack(Player player, double offset) {
        double playerCenterX = player.getX() + player.getWidth() / 2.0;
        double playerCenterY = player.getY() + player.getHeight() / 2.0;

        double offsetX = Math.cos(angle) * offset + horizontalOffset;
        double offsetY = Math.sin(angle) * offset;

        int x = (int)(playerCenterX + offsetX - boundsAttack.width / 2.0);
        int y = (int)(playerCenterY + offsetY - boundsAttack.height / 2.0);
        boundsAttack.setLocation(x, y);
    }


    private void attack(Player player, Entity target) {
        target.strike(AttackTypes.Melee, damage + player.getDamage());
        Sound.play(Sounds.Sword);
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        double offset = attacking ? Math.sin(attackProgress * Math.PI) * attackDistance : 0;

        double middleX = player.getX() + player.getWidth() / 2.0;
        double middleY = player.getY() + player.getHeight() / 1.5d;

        double attackX = middleX + Math.cos(angle) * offset + horizontalOffset;
        double attackY = middleY + Math.sin(angle) * offset;

        int drawX = (int)(attackX - getSprite().getWidth() / 2 - Game.C.getX());
        int drawY = (int)(attackY - getSprite().getHeight() / 2 - Game.C.getY());

        Rotate.draw(getSprite(), drawX, drawY, angle, null, g);

        g.setColor(Color.RED);
        g.drawRect(boundsAttack.x - Game.C.getX(), boundsAttack.y - Game.C.getY(),
                boundsAttack.width, boundsAttack.height);
    }


    public double getX() {
        Player player = Game.getPlayer();
        double offset = Math.sin(attackProgress * Math.PI) * attackDistance;
        return player.getX() + player.getWidth() / 2 + Math.cos(angle) * offset;
    }

    public double getY() {
        Player player = Game.getPlayer();
        double offset = Math.sin(attackProgress * Math.PI) * attackDistance;
        return player.getY() + player.getHeight() / 2 + Math.sin(angle) * offset;
    }

    public double getAngle() {
        return angle;
    }
}