package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawSprite;
import com.retronova.game.Game;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.utilities.Xp;
import com.retronova.game.objects.particles.DamageMobs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public abstract class Enemy extends Entity {

    private final Map<AttackTypes, Double> resistances;

    private double xpWeight;

    private boolean tookDamage;
    private int countTookDamage;

    protected Enemy(int ID, double x, double y, double friction) {
        super(ID, x, y, friction);
        this.resistances = new HashMap<>();
        addResistances(AttackTypes.Flat, 0);
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Enemy.class, sprites));
    }

    @Override
    public void tickEntityEffects() {
        super.tickEntityEffects();
        if(tookDamage) {
            countTookDamage++;
            if(countTookDamage > 50) {
                countTookDamage = 0;
                tookDamage = false;
            }
        }
    }

    protected void addResistances(AttackTypes attack, double resistance) {
        this.resistances.put(attack, resistance);
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        Game.getMap().put(new DamageMobs(getX(), getY(), 0.6, Engine.RAND.nextDouble()*Math.PI*2));
        if(resistances.containsKey(type)) {
            double r = resistances.get(type);
            setLife(getLife() - (damage * (1 - r))); // Dano total
            return;
        }
        setLife(getLife() - damage);
        this.tookDamage = true;
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
        e.setWeight(getXpWeight() * (1d + Engine.RAND.nextDouble(luck)));
        Game.getMap().put(e);
        e.getPhysical().addForce("move", 7, Math.PI*2);
    }

    public void renderLife(Graphics2D g) {
        if(getLife() == getLifeSize())
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
