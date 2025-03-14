package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.NPCs.Seller;
import com.retronova.game.objects.entities.enemies.*;
import com.retronova.game.objects.entities.furniture.Door;
import com.retronova.game.objects.entities.furniture.TrapDoor;
import com.retronova.game.objects.entities.utilities.Drop;
import com.retronova.game.objects.particles.DamageMobs;
import com.retronova.game.objects.physical.Physical;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Entity extends GameObject {

    private final Physical physical;

    private final Map<AttackTypes, Double> resistances;
    protected final Map<Modifiers, Double> modifiers;
    private final List<Effect> effects;

    private double[] life; //este valor é um array de dois valores, o primeiro é a vida original, e o outro a vida atual
    private double range;
    private double speed;
    private double damage;
    private double attackSpeed;

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
                return new MouseExplode(ID, x , y);
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
            case Seller -> {
                return new Seller(ID, x, y);
            }
            case Door -> {
                return new Door(ID, x, y);
            }
            case TrapDoor -> {
                return new TrapDoor(ID, x, y);
            }
        }
        throw new EntityNotFound("Entity not found");
    }

    protected Entity(int ID, double x, double y, double friction) {
        super(ID);
        setX(x);
        setY(y);
        this.physical = new Physical(this, friction);
        this.resistances = new HashMap<>();
        this.modifiers = new HashMap<>();
        this.effects = new ArrayList<>();
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Entity.class, sprites));
    }

    protected void addResistances(AttackTypes attack, double resistance) {
        this.resistances.put(attack, resistance);
    }


    public void strike(AttackTypes type, double damage) {
        if(resistances.containsKey(type)) {
            double r = resistances.get(type);
            setLife(getLife() - (damage * (1 - r))); // Dano total
            return;
        }
        setLife(getLife() - damage);
    }

    public void addModifier(Modifiers modifier, double value) {
        if(!this.modifiers.containsKey(modifier))
            this.modifiers.put(modifier, value);
    }

    public void addEffect(String name, EffectApplicator applicator, int seconds) {
        Effect effect = new Effect(name, this, applicator, seconds);
        if(!this.effects.contains(effect)) {
            this.effects.add(new Effect(name, this, applicator, seconds));
        }
    }

    void removeEffect(Effect effect) {
        this.effects.remove(effect);
    }

    public void tickEntityEffects() {
        setDepth();
        getPhysical().moment();
        for(int i = 0; i < effects.size(); i++) {
            effects.get(i).tick();
        }
        if (getLife() <= 0) {
            die();
        }
    }

    public double getLife() {
        if(this.life == null) {
            return 1;
        }
        return this.life[1];
    }

    public double getLifeSize() {
        if(this.life == null) {
            return 1;
        }
        if(this.modifiers.containsKey(Modifiers.Life)) {
            return this.life[0] + this.modifiers.get(Modifiers.Life);
        }
        return this.life[0];
    }

    protected void setLife(double life) {
        if(this.life == null) {
            this.life = new double[] {life, life};
            return;
        }
        this.life[1] = life;
    }

    public double getDamage() {
        if(modifiers.containsKey(Modifiers.Damage)) {
            return this.damage + modifiers.get(Modifiers.Damage);
        }
        return this.damage;
    }

    protected void setDamage(double damage) {
        this.damage = damage;
    }

    public double getSpeed() {
        if(modifiers.containsKey(Modifiers.Speed)) {
            return this.speed + modifiers.get(Modifiers.Speed);
        }
        return this.speed;
    }

    protected void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getRange() {
        if(modifiers.containsKey(Modifiers.Range)) {
            return this.range + modifiers.get(Modifiers.Range);
        }
        return this.range;
    }

    protected void setRange(double range) {
        this.range = range;
    }

    public double getAttackSpeed() {
        if(modifiers.containsKey(Modifiers.AttackSpeed)) {
            return this.attackSpeed + modifiers.get(Modifiers.AttackSpeed);
        }
        return this.attackSpeed;
    }

    protected void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public Entity getNearest(double range){
        List<Entity> entities = Game.getMap().getEntities();
        Entity nearest = null;
        double maxDist = GameObject.SIZE() * range;
        for(int i = 0; i < entities.size(); i ++){
            Entity e = entities.get(i);
            if(e == this){
                continue;
            }
            double currDist = e.getDistance(this);
            if(currDist < maxDist){
                nearest = e;
                maxDist = currDist;
            }
        }
        return nearest;
    }

    public <T extends Entity> T getNearest(double range, Class<T> type){
        List<Entity> entities = Game.getMap().getEntities();
        T nearest = null;
        double maxDist = GameObject.SIZE() * range;
        for(int i = 0; i < entities.size(); i ++){
            Entity e = entities.get(i);
            if(e == this || !type.isInstance(e)){
                continue;
            }
            double currDist = e.getDistance(this);
            if(currDist < maxDist){
                nearest = (T) e;
                maxDist = currDist;
            }
        }
        return nearest;
    }

    public <T extends Entity> T getNearest(double range, Class<T> type, Entity ignore){
        List<Entity> entities = Game.getMap().getEntities();
        T nearest = null;
        double maxDist = GameObject.SIZE() * range;
        for(int i = 0; i < entities.size(); i ++){
            Entity e = entities.get(i);
            if(e == this || e == ignore || !type.isInstance(e)){
                continue;
            }
            double currDist = e.getDistance(this);
            if(currDist < maxDist){
                nearest = (T) e;
                maxDist = currDist;
            }
        }
        return nearest;
    }

    public boolean colliding(Entity entity) {
        return this.getBounds().intersects(entity.getBounds());
    }

    public boolean colliding(Rectangle rec) {
        return this.getBounds().intersects(rec);
    }

    /**
     * Quando chamado, adiciona partículas no local da morte, aciona um som padrão de morte e remove a entidade do mapa.
     */
    public abstract void die();

    /**
     * Esta função serve apenas para retirar uma entidade do mara, sem nenhum tipo de efeito.
     */
    public void disappear() {
        Game.getMap().remove(this);
    }

    public void dropLoot(Item loot) {
        Entity drop = new Drop(getX(), getY(), loot);
        Game.getMap().put(drop);
        drop.getPhysical().addForce("drop", Engine.RAND.nextInt(10), Engine.RAND.nextDouble(Math.PI*2));
    }

    public Physical getPhysical() {
        return this.physical;
    }
}
