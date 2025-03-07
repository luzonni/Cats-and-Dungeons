package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;

import java.awt.image.BufferedImage;

public class Xp extends Entity {

    private static BufferedImage[] sprites;
    private int count;
    private int indexSprite;
    private double weight;
    private double speed;

    Xp(int ID, double x, double y) {
        super(ID, x, y, 0.5d);
        if(sprites == null)
            sprites = loadSprite("xp");
    }

    @Override
    public BufferedImage getSprite() {
        return sprites[indexSprite];
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return this.weight;
    }

    @Override
    public void tick() {
        count++;
        if(count > 15) {
            count = 0;
            indexSprite++;
            if(indexSprite > sprites.length-1) {
                indexSprite = 0;
            }
        }
        Player player = Game.getPlayer();
        if(player.getDistance(this) < GameObject.SIZE()/3d) {
            player.plusXp(getWeight());
            disappear();
        }
        if(player.getDistance(this) < GameObject.SIZE()*5) {
            if(speed < 15d) {
                speed += 0.15d;
            }
            getPhysical().addForce(speed, player.getAngle(this));
        }
    }

    @Override
    public void dispose() {
        sprites = null;
    }
}
