package com.retronova.game.objects.entities.enemies;

import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class KingCursedCatBoss extends Enemy {

    private int countAnim;

    public KingCursedCatBoss(int ID, double x, double y) {
        super(ID,x,y, 80);
        setWidth(2);
        setHeight(2);
        loadSprites("kingcursedcatboss");
        setLife(1100);
        setSpeed(4);
        setSolid();

    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        double angle = player.getAngle(this);
        getPhysical().addForce("Moving",getSpeed(), angle);

    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() - Game.C.getY();
        g.setColor(Color.red);
        g.drawRect(x,y,getWidth(), getHeight());
    }



}
