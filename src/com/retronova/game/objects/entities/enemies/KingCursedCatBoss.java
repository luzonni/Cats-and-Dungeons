package com.retronova.game.objects.entities.enemies;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class KingCursedCatBoss extends Enemy {

    public KingCursedCatBoss(int ID, double x, double y) {
        super(ID, x, y, 80);
        setWidth(2);
        setHeight(2);
        loadSprites("kingcursedcatboss");
        setLife(1300);
        setSpeed(4);
        setSolid();
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        double angle = player.getAngle(this);
        getPhysical().addForce("Moving",getSpeed(), angle);
        if(player.colliding(this)){
            player.strike(AttackTypes.Melee, 30);
            //TODO Sound.play
            player.getPhysical().addForce("knockback", 20, getPhysical().getAngleForce());
        }

    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        int x = (int)getX();
        int y = (int)getY();
        g.setColor(Color.red);
        g.drawRect(x,y,getWidth(), getHeight());
    }



}
