package com.retronova.game.items;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class DangerousWand extends Item {
    private final Rectangle boundsAttack;
    private double angle;
    private final double orbitRadius = GameObject.SIZE() * 2;
    private double targetAngle = 0;
    private final Point spriteRotatePoint;

    DangerousWand(int id) {
        super(id, "Dangerous Wand", "dangerouswand");
        addSpecifications("Raw magical energy", "player damage", "shot instant");
        this.boundsAttack = new Rectangle(GameObject.SIZE(), GameObject.SIZE());
        this.spriteRotatePoint = new Point(getSprite().getWidth() / 2, getSprite().getHeight() / 2);
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        double x = player.getX() + (player.getWidth() / 2) + Math.cos(this.angle) * orbitRadius - this.boundsAttack.width / 2;
        double y = player.getY() + (player.getHeight() / 2) + Math.sin(this.angle) * orbitRadius - this.boundsAttack.height / 2;
        this.boundsAttack.setLocation((int) x, (int) y);
        this.angle += Math.PI / 16;

        Enemy nearest = player.getNearest(player.getRange(), Enemy.class);
        if (nearest != null) {
            double wandCenterX = this.boundsAttack.x + this.boundsAttack.width / 2;
            double wandCenterY = this.boundsAttack.y + this.boundsAttack.height / 2;
            double enemyCenterX = nearest.getX() + nearest.getWidth() / 2;
            double enemyCenterY = nearest.getY() + nearest.getHeight() / 2;
            this.targetAngle = Math.atan2(enemyCenterY - wandCenterY, enemyCenterX - wandCenterX);
        }

        this.attack(player);
    }

    private void attack(Player player) {
        List<Enemy> entities = Game.getMap().getEntities(Enemy.class);
        for (Enemy e : entities) {
            if (e.colliding(this.boundsAttack)) {
                e.strike(AttackTypes.Melee, player.getDamage() * 0.5);
                break;
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        BufferedImage sprite = getSprite();

        if (sprite != null) {
            int drawX = this.boundsAttack.x - Game.C.getX();
            int drawY = this.boundsAttack.y - Game.C.getY();
            Rotate.draw(sprite, drawX, drawY, targetAngle + Math.PI / 2, spriteRotatePoint, g);
        }

        g.setColor(Color.RED);
        g.drawRect(this.boundsAttack.x - Game.C.getX(), this.boundsAttack.y - Game.C.getY(), this.boundsAttack.width, this.boundsAttack.height);
    }
}