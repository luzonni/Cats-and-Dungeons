package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Kunai extends Item {

    private final BufferedImage kunaiSprite;
    private final double damage = 25;
    private double xOffset, yOffset;
    private double x, y, dx, dy;
    private boolean thrown = false;
    private final int speed = 12;
    private Enemy target;

    public Kunai(int id) {
        super(id, "Kunai", "kunai");
        kunaiSprite = new SpriteSheet("sprites/items", "kunai", Configs.GameScale()).getSHEET();
        addSpecifications("Throwable", "Player damage + "+this.damage, "medium speed");
        this.xOffset = 10;
        this.yOffset = 0;
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();

        if (!thrown) {
            this.x = player.getX() + xOffset;
            this.y = player.getY() + yOffset;

            target = player.getNearest(5, Enemy.class);
            if (target != null) {
                thrown = true;
                double angle = Math.atan2(target.getY() - y, target.getX() - x);
                dx = Math.cos(angle) * speed;
                dy = Math.sin(angle) * speed;
                // Sound.play(Sounds.Throw); ME LEMBRA DE POR O SOM AQUI POR FAVOR, TEM QUE ACHAR
            }
        } else {
            x += dx;
            y += dy;

            if (target != null && target.colliding(new Rectangle((int)x, (int)y, Configs.GameScale()*2, Configs.GameScale()*2))) {
                target.strike(AttackTypes.Throw, this.damage + player.getDamage());
                target.getPhysical().addForce("knockback", 12, target.getAngle(player));
                resetKunai();
            }
        }
    }

    private void resetKunai() {
        thrown = false;
        target = null;
        this.xOffset = 10;
        this.yOffset = 0;
    }

    @Override
    public void render(Graphics2D g) {
        int drawX = (int)x - Game.C.getX();
        int drawY = (int)y - Game.C.getY();
        g.drawImage(kunaiSprite, drawX, drawY, null);
    }
}