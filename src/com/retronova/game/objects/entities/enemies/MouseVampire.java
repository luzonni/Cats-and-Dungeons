package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseVampire extends Enemy {

    private int countAnim;
    private int cooldown;
    private boolean soundPlaying = false;
    private int soundStopDelay = 0;

    public MouseVampire(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousevampire");
        setSolid();
        setLife(60);
        setXpWeight(500000.0d);
    }

    public void tick() {
        moveIA();
        animation();
        attackPlayer();
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce("move", 0.80, radians);
    }

    public void animation() {
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    public void attackPlayer() {
        Player player = Game.getPlayer();
        cooldown++;

        // Resetar o soundStopDelay se o jogador estiver em contato, independentemente do cooldown
        if (player.getBounds().intersects(this.getBounds())) {
            soundStopDelay = 30; // Reseta o delay enquanto o jogador estiver em contato
        }

        if (player.getBounds().intersects(this.getBounds()) && cooldown > 45) {
            cooldown = 0;
            player.strike(AttackTypes.Melee, 3);
            player.getPhysical().addForce("knockback_vampire", 0.82d, getPhysical().getAngleForce());
            Sound.play(Sounds.MouseVampire);
            soundPlaying = true;
        } else {
            if (soundPlaying) {
                if (soundStopDelay > 0) {
                    soundStopDelay--;
                } else {
                    Sound.stop(Sounds.MouseVampire);
                    soundPlaying = false;
                }
            }
        }
    }

    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }
}