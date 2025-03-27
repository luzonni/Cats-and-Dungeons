package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.utilities.Skull;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Skeleton extends Enemy {

    private int countAnim;
    private int cooldown;
    private boolean soundPlaying = false;
    private int soundStopDelay = 0;

    public Skeleton(int ID, double x, double y) {
        super(ID, x, y, 0.5);
        loadSprites("mouseskeleton");
        setLife(60);
        setSpeed(1);
        addResistances(AttackTypes.Fire, 0.5);
        addResistances(AttackTypes.Poison, 0.8);
        addResistances(AttackTypes.Piercing, 1);
        setSolid();
    }

    public void tick() {
        moveIA();
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
        Player player = Game.getPlayer();
        cooldown++;
        if(cooldown >= 45 && player.getDistance(this) <= GameObject.SIZE() * 5) {
            cooldown = 0;
            Skull skull = new Skull(getX(), getY(), player.getAngle(this));
            Game.getMap().put(skull);
            Sound.play(Sounds.Skeleton);
        }
    }

    private void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        if(player.getDistance(this) >= GameObject.SIZE() * 3){
            getPhysical().addForce("move", getSpeed(), radians);
        }

    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
    }
}