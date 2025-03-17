package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.items.ItemLaser;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Laser<T extends Entity> extends Utility {

    private final double damage;
    private double angle;
    private final Class<T> type;
    private int duration = 20;
    private final BufferedImage laserSprite;
    private Entity currentTarget;
    private double range = 200;
    private ItemLaser itemLaser;

    public Laser(double x, double y, double damage, double angle, Class<T> type, ItemLaser itemLaser) {
        super(-1, x, y, 0);
        this.damage = damage;
        this.angle = angle;
        this.type = type;
        loadSprites("teste");
        this.laserSprite = getSprite();
        this.itemLaser = itemLaser;
    }

    @Override
    public void tick() {
        if (duration <= 0) {
            this.disappear();
            return;
        }
        duration--;

        if (currentTarget == null || !Game.getMap().getEntities(type).contains(currentTarget)) {
            currentTarget = Game.getPlayer().getNearest(1000, type);
        }

        if (currentTarget != null && getDistance(currentTarget) <= range) {
            currentTarget.strike(AttackTypes.Laser, damage);
            currentTarget.getPhysical().addForce("knockback", 3.0, this.angle);
            currentTarget.addEffect("burn", (Entity e) -> {
                e.strike(AttackTypes.Laser, 0.2d);
            }, 3);
        }

        if (itemLaser != null) {
            setX(itemLaser.getX());
            setY(itemLaser.getY());
            this.angle = itemLaser.getAngle();
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (currentTarget != null) {
            double distance = getDistance(currentTarget);
            int laserLength = (int) distance;

            int laserWidth = laserSprite.getWidth();
            int laserHeight = laserSprite.getHeight();

            for (int i = 0; i < laserLength; i += laserWidth) {
                int x = (int) (getX() + Math.cos(angle) * i) - Game.C.getX();
                int y = (int) (getY() + Math.sin(angle) * i) - Game.C.getY();
                Rotate.draw(laserSprite, x, y, angle, null, g);
            }
        }
    }
}