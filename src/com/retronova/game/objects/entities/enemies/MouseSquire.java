package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseSquire extends Enemy {

    private int countAnim;
    private int cooldown;
    private boolean soundPlaying = false;
    private int soundStopDelay = 0;

    public MouseSquire(int ID, double x, double y) {
        super(ID, x, y, 0.4);
        loadSprites("mousesquire");
        setSolid();
        setLife(60);
        setXpWeight(500000.0d);
    }

    public void tick() {
        moveIA();
        animation();
        setDamage();
    }

    public void setDamage() {
        Player player = Game.getPlayer();
        cooldown++;

        // Resetar o soundStopDelay se o jogador estiver em contato, independentemente do cooldown
        if (player.getBounds().intersects(this.getBounds())) {
            soundStopDelay = 30; // Reseta o delay enquanto o jogador estiver em contato
        }

        if (player.getBounds().intersects(this.getBounds()) && cooldown > 40) {
            cooldown = 0;
            player.strike(AttackTypes.Melee, 6);
            player.getPhysical().addForce("knockback", 3.0d, getPhysical().getAngleForce());
            Sound.play(Sounds.MouseSquire);
            soundPlaying = true;
        } else {
            if (soundPlaying) {
                if (soundStopDelay > 0) {
                    soundStopDelay--;
                } else {
                    Sound.stop(Sounds.MouseSquire);
                    soundPlaying = false;
                }
            }
        }
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        if (this.getDistance(player) < GameObject.SIZE() * 6) {
            double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
            getPhysical().addForce("move", 0.75d, radians);
        }
    }

    public void animation() {
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    @Override
    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0];
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }
}