package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Skull extends Utility {

    private final double dir;
    private double r;


    public Skull(double x, double y, double dir) {
        super(-1, x, y, 0.0);
        this.dir = dir;
        loadSprites("skull");
        if(Engine.RAND.nextBoolean()) {
            getSheet().plusIndex();
        }
        setSpeed(4.5);
        setDamage(7.3);
    }

    @Override
    public void tick() {
        if(getPhysical().crashing()) {
            this.disappear();
        }
        getPhysical().addForce("moving",getSpeed(), this.dir);
        Player player = Game.getPlayer();
        if(player.colliding(this)){
            player.strike(AttackTypes.Impact, getDamage());
            this.disappear();
        }
        r += 0.02d;

    }

    @Override
    public void render(Graphics2D g) {
        BufferedImage sprite = getSprite();
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() - Game.C.getY();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.scale(0.5,0.5);
        Rotate.draw(sprite, x, y, r, null, g2);
    }
}
