package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseSquire extends Enemy {

    private int countAnim;
    private int cooldown;
    private boolean soundPlaying = false;
    private int soundStopDelay = 0;

    public MouseSquire(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousesquire");
        setSolid();
        setLife(70);
        setSpeed(3);
        addResistances(AttackTypes.Fire, 0.6);
        addResistances(AttackTypes.Poison, 1);
        addResistances(AttackTypes.Piercing, 0.7);
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
            player.addEffect("knockback", (e) -> {
                e.getPhysical().setRoughness(0.2);
                e.getPhysical().addForce("knockback", 8.0d, getPhysical().getAngleForce());
            }, 0.08);

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
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce("move", getSpeed(), radians);

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