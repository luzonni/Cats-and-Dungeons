package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawSprite;
import com.retronova.game.Game;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.utilities.Xp;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Volatile;
import com.retronova.game.objects.particles.Word;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class Enemy extends Entity {


    private double xpWeight;

    private boolean tookDamage;
    private int countTookDamage;

    protected Enemy(int ID, double x, double y, int weight) {
        super(ID, x, y, weight);
        addResistances(AttackTypes.Flat, 0);
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Enemy.class, sprites));
    }

    @Override
    public void postTick() {
        super.postTick();
        if(tookDamage) {
            countTookDamage++;
            if(countTookDamage > 50) {
                countTookDamage = 0;
                tookDamage = false;
            }
        }
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        this.strike(type, damage, new Volatile("damagemobs", 0, 0));
    }

    @Override
    public void strike(AttackTypes type, double damage, Particle particle) {
        Player player = Game.getPlayer();
        damage *= Engine.RAND.nextDouble() + player.getLuck();
        super.strike(type, damage);
        this.tookDamage = true;
        double x = getX() + Engine.RAND.nextDouble(getWidth());
        double y = getY() + Engine.RAND.nextDouble(getHeight());
        particle.setX(x);
        particle.setY(y);
        if(damage >= 1d)
            Game.getMap().put(new Word(String.valueOf((int)damage), x, y, 1));
        Game.getMap().put(particle);
    }

    public BufferedImage getSprite() {
        BufferedImage currentSprite = super.getSprite();
        if(tookDamage) {
            currentSprite = DrawSprite.draw(currentSprite, new Color(122, 19, 17));
        }
        return currentSprite;
    }

    protected void setXpWeight(double weight) {
        this.xpWeight = weight;
    }

    protected double getXpWeight() {
        return this.xpWeight;
    }

    @Override
    public void die() {
        this.disappear();
        dropXp();
    }

    private void dropXp() {
        double luck = Game.getPlayer().getLuck();
        Xp e = new Xp(getX(), getY());
        e.setWeight(getXpWeight() * Engine.RAND.nextDouble() * luck);
        Game.getMap().put(e);
        e.getPhysical().addForce("move", 7, Math.PI*2);
    }

    public void renderLife(Graphics2D g) {
        if(getLife() == getLifeSize() || getLife() < 0)
            return;
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() + getHeight() - Game.C.getY() + Configs.GameScale() *2;
        int w = getWidth();
        int h = Configs.GameScale() * 3;

        g.setColor(new Color(135, 35, 65));
        g.fillRect(x, y, w, h);
        double lifeSize = w * (getLife() / getLifeSize());
        g.setColor(new Color(190, 49, 68));
        g.fillRect(x, y, (int) lifeSize, h);

        g.setStroke(new BasicStroke(Configs.GameScale()));
        g.setColor(new Color(9, 18, 44));
        g.drawRect(x, y, w, h);
    }

}
