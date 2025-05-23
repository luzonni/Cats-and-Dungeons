package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.Scaling;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Skull extends Utility {

    private final BufferedImage sprite;
    private final double dir;
    private double r;


    public Skull(double x, double y, double dir) {
        super(x, y, 0);
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
        Sound.play(Sounds.Woosh);
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
            Sound.play(Sounds.Skeleton);
            this.disappear();
        }
        r += 0.2d;
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.draw(sprite, (int)getX(), (int)getY(), r, null, g);
    }
}