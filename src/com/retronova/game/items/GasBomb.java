package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.Bomb;
import com.retronova.game.objects.entities.utilities.ThrownGasBom;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class GasBomb extends Item {

    private int count;
    private final double offsetX = 20;
    private final double offsetY = 10;

    GasBomb(int id) {
        super(id, "GasBomb", "gasbomb");
        addSpecifications("Explode", "cria uma fumaça de gás", "somente uma por vez");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        count++;
        if (count > player.getAttackSpeed() * 1.5) {
            count = 0;
            if (canThrow()) {
                return;
            }
            Enemy nearest = player.getNearest(10, Enemy.class);
            if (nearest != null) {
                ThrownGasBom bomb = new ThrownGasBom(player.getX(), player.getY(), player.getDamage() * 3);
                bomb.getPhysical().addForce("throw", 10, nearest.getAngle(player));
                Game.focus(bomb);
                Game.getMap().put(bomb);
            }
        }
    }

    public boolean canThrow() {
        List<Entity> entities = Game.getMap().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof ThrownGasBom) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        BufferedImage sprite = getSprite();

        if (sprite != null) {
            int drawX = (int) (player.getX() + player.getWidth() / 2 + offsetX - Game.C.getX() - sprite.getWidth() / 2);
            int drawY = (int) (player.getY() + player.getHeight() / 2 + offsetY - Game.C.getY() - sprite.getHeight() / 2);
            g.drawImage(sprite, drawX, drawY, null);
        }
    }
}