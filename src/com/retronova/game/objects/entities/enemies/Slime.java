package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.util.Random;

public class Slime extends Enemy {

    private int countAnim;
    private Random random;
    private int jumpCoolDown;
    private boolean isJumping;
    private int attackCooldown = 0;

    public Slime(int ID, double x, double y) {
        super(ID, x, y, 0.2);
        loadSprites("slime");
        jumpCoolDown = 0;
        isJumping = false;
        random = new Random();
        setSolid();
        setSpeed(6);
        setLife(10);
        setXpWeight(800000.6d);
    }

    public void tick() {
        moveIA();
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }

        Player player = Game.getPlayer();
        //double distance = Math.sqrt(Math.pow((player.getX() - getX()), 2) + Math.pow(player.getY() - getY(), 2));

        // Lógica de colisão e som
        if (player.getBounds().intersects(this.getBounds()) && attackCooldown <= 0) {
            try {
                Sound.play(Sounds.Slime);
                attackCooldown = 15; // Reduzido para 15 para teste
                System.out.println("Slime sound played!"); // Debug
            } catch (Exception e) {
                System.err.println("Error playing Slime sound: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

    private void moveIA() {
        double radians;
        if (jumpCoolDown > 0) {
            jumpCoolDown--;
            return;
        }

        Player player = Game.getPlayer();
        //double distance = Math.sqrt(Math.pow((player.getX() - getX()), 2) + Math.pow(player.getY() - getY(), 2));

        //if (distance < GameObject.SIZE() * 1.0) {
        //    return;
        //}

        //if (distance < GameObject.SIZE() * 6) {
        radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce("jump", getSpeed(), radians);
        //} else {
        //    radians = random.nextDouble() * (2 * Math.PI);
        //    getPhysical().addForce("follow", getSpeed(), radians);
        //}

        jumpCoolDown = 30 + random.nextInt(20);
    }
}