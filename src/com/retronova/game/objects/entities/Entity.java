package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.exceptions.EntityNotFound;
import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Physical;

import java.awt.*;

public abstract class Entity extends GameObject {

    private final Physical physical;
    private AttackTypes[] resistanceOf;
    private double[] life; //este valor é um array de dois valores, o primeiro é a vida original, e o outro a vida atual

    public static Entity build(int ID, double x, double y) {
        EntityIDs entityId = EntityIDs.values()[ID];
        x *= GameObject.SIZE();
        y *= GameObject.SIZE();
        switch (entityId) {
            case Zombie -> {
                return new Zombie(ID, x, y);
            }
            case Skeleton -> {
                return new Skeleton(ID, x, y);
            }
            case Slime -> {
                return new Slime(ID, x, y);
            }
            case RatExplode -> {
                return new RatExplode(ID, x , y);
            }
            case MouseSquire -> {
                return new MouseSquire(ID, x, y);
            }
            case MouseVampire -> {
                return new MouseVampire(ID, x, y);
            }
            case CatToyBoss -> {
                return new CatToyBoss(ID,x,y);
            }
            case KingCursedCatBoss -> {
                return new KingCursedCatBoss(ID, x,y);
            }
            case MonarkMouse -> {
                return new MonarkMouse(ID, x, y);
            }
        }
        throw new EntityNotFound("Entity not found");
    }

    Entity(int ID, double x, double y, double friction) {
        super(ID);
        setX(x);
        setY(y);
        this.physical = new Physical(this, friction);
        setResistances(null);
        setLife(100d);
    }

    public double getLife() {
        return this.life[1];
    }

    public double getLifeSize() {
        return this.life[0];
    }

    protected void setLife(double life) {
        if(this.life == null) {
            this.life = new double[] {life, life};
            return;
        }
        this.life[1] = life;
    }

    protected void setResistances(AttackTypes... resistences) {
        this.resistanceOf = resistences;
    }

    public void strike(AttackTypes type, double damage) {
        if (this.resistanceOf != null && type != null) {
            for (AttackTypes resistance : this.resistanceOf) {
                if (resistance.equals(type)) {
                    setLife(getLife() - damage * (1 - resistance.getResistance()));
                    return;
                }
            }
        }
        setLife(getLife() - damage); // Dano total
        if (getLife() <= 0) {
            die();
        }
    }

    public void die() {
        //TODO adicionar particula de morte
        //TODO adicionar som de morte
        Game.getMap().getEntities().remove(this);
    }

    public void dropLoot(Item loot) {
        Entity drop = new Drop(getX(), getY(), loot);
        Game.getMap().getEntities().add(drop);
        drop.getPhysical().addForce(Engine.RAND.nextInt(10), Engine.RAND.nextDouble(Math.PI*2));
    }

    public Physical getPhysical() {
        return this.physical;
    }

    public void renderLife(Graphics2D g) {
        if(getLife() == getLifeSize() || this instanceof Player)
            return;
        int x = (int)getX() - Game.C.getX();
        int y = (int)getY() + getHeight() - Game.C.getY() + Configs.SCALE*2;
        int w = getWidth();
        int h = Configs.SCALE * 3;

        g.setColor(new Color(135, 35, 65));
        g.fillRect(x, y, w, h);
        double lifeSize = w * (getLife() / getLifeSize());
        g.setColor(new Color(190, 49, 68));
        g.fillRect(x, y, (int) lifeSize, h);

        g.setStroke(new BasicStroke(Configs.SCALE));
        g.setColor(new Color(9, 18, 44));
        g.drawRect(x, y, w, h);
    }

}
