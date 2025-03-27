package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.Scaling;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Skull extends Utility {

    private final BufferedImage sprite;
    private final double dir;
    private double r;


    public Skull(double x, double y, double dir) {
        super(-1, x, y, 0.0);
        this.dir = dir;
        loadSprites("skull");
        if(Engine.RAND.nextBoolean()) {
            getSheet().plusIndex();
        }
        setWidth(0.75);
        setHeight(0.75);
        this.sprite = Scaling.s(getSprite(), getWidth(), getHeight());
        setSpeed(1.5);
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
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() - Game.C.getY();
        Rotate.draw(sprite, x, y, r, null, g);
    }
}
