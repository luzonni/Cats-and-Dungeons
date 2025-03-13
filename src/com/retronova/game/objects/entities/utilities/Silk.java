package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Alpha;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Silk extends Utility {

    private final List<Entity> visited;
    private float fading = 1f;
    private double r;

    public Silk(double x, double y, double damage, Entity shooter) {
        super(-1, x, y, 0.25d);
        setSpeed(7d);
        loadSprites("silk");
        setDamage(damage);
        this.visited = new ArrayList<>();
        this.visited.add(shooter);
    }

    @Override
    public void tick() {
        if(visited.size() >= 5){
            this.disappear();
            return;
        }
        Enemy nearest = getNearest(Game.getPlayer().getRange(), Enemy.class, visited.getLast());
        if(nearest != null && !visited.contains(nearest) && !getPhysical().crashing()) {
            r+=0.24d;
            double rf = nearest.getAngle(this);
            getPhysical().addForce("follow_silk", getSpeed(), rf);
            fading = 1;
            if(colliding(nearest)) {
                nearest.strike(AttackTypes.Flat, getDamage());
                setDamage(getDamage() - getDamage()*0.25d);
                visited.add(nearest);
            }
        }else {
            fading -=0.015f;
            if(fading <= 0.1) {
                this.disappear();
                return;
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() - Game.C.getY();
        renderLine(g);
        BufferedImage sp = Alpha.getImage(getSprite(), fading);
        Rotate.draw(sp, x, y, r, null, g);
    }

    private void renderLine(Graphics2D g) {
        g.setStroke(new BasicStroke(Configs.GameScale()));
        g.setColor(new Color(243, 113, 153));
        int x = (int)getX() + getWidth()/2 - Game.C.getX();
        int y = (int)getY() + getHeight()/2 - Game.C.getY();
        for(int i = visited.size()-1; i >= 0; i--) {
            Entity v = visited.get(i);
            int xx = (int) v.getX() + v.getWidth()/2 - Game.C.getX();
            int yy = (int) v.getY() + v.getHeight()/2 - Game.C.getY();
            g.drawLine(xx, yy, x, y);
            x = xx;
            y = yy;
        }
    }

}
