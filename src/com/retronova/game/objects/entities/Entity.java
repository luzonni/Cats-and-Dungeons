package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.NPCs.Seller;
import com.retronova.game.objects.entities.enemies.*;
import com.retronova.game.objects.entities.furniture.*;
import com.retronova.game.objects.entities.utilities.Drop;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Volatile;
import com.retronova.game.objects.physical.Physical;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Entity extends GameObject {

    private final Physical physical;

    private final Map<AttackTypes, Double> resistances;
    private final Map<Modifiers, Double> modifiers;
    private final List<Effect> effects;

    private double[] life; //este valor é um array de dois valores, o primeiro é a vida original, e o outro a vida atual
    private double range;
    private double speed;
    private double damage;
    private double attackSpeed;

    private boolean clickable;

    public static Entity build(int ID, Object... values) {
        EntityIDs entityId = EntityIDs.values()[ID];
        int length = values.length;
        int ix = (length >= 1) ? ((Number)values[0]).intValue() : 0;
        int iy = (length >= 2) ? ((Number)values[1]).intValue() : 0;
        int x = ix * GameObject.SIZE();
        int y = iy * GameObject.SIZE();
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
                if(values.length != 3) {
                    break;
                }
                JSONArray stock = null;
                try {
                    stock = (JSONArray) ((JSONObject) values[2]).get("stock");
                }catch (ClassCastException ignore) {}
                if(stock == null && ((String)values[2]).equalsIgnoreCase("FULL")) {
                    stock = new JSONArray();
                }
                return new Seller(ID, x, y, stock);
            }
            case Door -> {
                return new Door(ID, x, y);
            }
            case TrapDoor -> {
                String place = (length >= 3) ? (String)values[2] : "None";
                return new TrapDoor(ID, x, y, place);
            }
            case Crate -> {
                JSONArray loot = ((JSONArray)((JSONObject) values[2]).get("loot"));
                System.out.println(loot.toJSONString());
                ItemIDs[] ids = ItemIDs.values();
                int[] lootIds = new int[loot.size()];
                for(int i = 0; i < lootIds.length; i++) {
                    String name = (String) loot.get(i);
                    for(ItemIDs value : ids) {
                        if(name.equalsIgnoreCase(value.name())) {
                            lootIds[i] = value.ordinal();
                        }
                    }
                }
                return new Crate(ID, x, y, lootIds);
            }
            case Plate -> {
                String content = "Eu não tenho conteúdo ;(";
                if(values.length >= 3) {
                    content = (String) values[2];
                }
                return new Plate(ID, x, y, content);
            }
            case GumMachine -> {
                String content = "Eu não tenho conteúdo ;(";
                if(values.length >= 3) {
                    content = (String) values[2];
                }
                return new GumMachine(ID, x, y, content);
            }
            case CryingCat -> {
                return new CryingCat(ID, x, y);
            }
        }
        throw new EntityNotFound("Entity not found");
    }

    protected Entity(int ID, double x, double y, int weight) {
        super(ID);
        setX(x);
        setY(y);
        this.physical = new Physical(this, weight);
        this.resistances = new HashMap<>();
        this.modifiers = new HashMap<>();
        this.effects = new ArrayList<>();
    }

    protected void setClickable() {
        this.clickable = true;
    }

    public boolean clickable() {
        return this.clickable;
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
            double finalDamage = damage * (1 - r);
            double currentLife = getLife();
            setLife(currentLife - finalDamage); // Dano total
            return;
        }
        double currentLife = getLife();
        setLife(currentLife - damage);
    }

    public void strike(AttackTypes type, double damage, Particle particle) {
        strike(type, damage);
        Game.getMap().put(particle);
    }

    public void addModifier(Modifiers modifier, double value) {
        if(!this.modifiers.containsKey(modifier))
            this.modifiers.put(modifier, value);
        else if(modifier.getHeapable()){
            double currentValue = this.modifiers.get(modifier);
            this.modifiers.replace(modifier, currentValue + value);
        }else {
            this.modifiers.replace(modifier, value);
        }
    }

    public boolean hasModifier(Modifiers modifiers) {
        return this.modifiers.containsKey(modifiers);
    }

    public double valueModifier(Modifiers modifiers) {
        return this.modifiers.get(modifiers);
    }

    public void addEffect(String name, EffectApplicator applicator, double seconds, int repetitions) {
        Effect effect = new Effect(name, this, applicator, seconds, repetitions);
        if(!this.effects.contains(effect)) {
            this.effects.add(effect);
        }
    }

    public void addEffect(String name, EffectApplicator applicator, double seconds) {
        this.addEffect(name, applicator, seconds, (int)(seconds*60));
    }

    void removeEffect(Effect effect) {
        this.effects.remove(effect);
    }

    public void preTick() {
        setDepth();
        getPhysical().moment();
    }

    public void postTick() {
        for(int i = 0; i < effects.size(); i++) {
            effects.get(i).tick();
        }
        if (getLife() <= 0) {
            die();
        }
        if(clickable() && GameMap.mouseOnRect(this.getBounds())) {
            Engine.window.pointing();
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

    public void setLife(double life) {
        if(this.life == null) {
            this.life = new double[] {life, life};
            return;
        }
        if(life <= getLifeSize()) {
            this.life[1] = life;
        }
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
        if(Game.getCam().getFollowed() == this) {
            Game.getCam().setFollowed(Game.getPlayer());
        }
        Game.getMap().remove(this);
    }

    public void dropLoot(Item loot) {
        Entity drop = new Drop(getX(), getY(), loot);
        Game.getMap().put(drop);
        drop.getPhysical().addForce("drop", 3 + Engine.RAND.nextInt(3), Engine.RAND.nextDouble(Math.PI*2));
    }

    public Physical getPhysical() {
        return this.physical;
    }

    // LOCAL PARA EFEITOS PADRÕES (sempre começando com EFFECT);

    /**
     *
     * @param seconds quantos segundos ela durará
     * @param repetitions são dentro do tempo total, quantas vezes ele irá acontecer
     * @apiNote O dano de poison retira SEMPRE 10% da vida da entidade!
     */
    public void EFFECT_POISON(double seconds, int repetitions) {
        this.addEffect("Poison", (e) -> {
            Sound.play(Sounds.Poison);
            double x = e.getX() + Engine.RAND.nextInt(e.getWidth());
            double y = e.getY() + Engine.RAND.nextInt(e.getHeight());
            Particle particle = new Volatile("poison", x, y);
            double damage = e.getLifeSize() * 0.1;
            e.strike(AttackTypes.Poison, damage, particle);
        }, seconds, repetitions);
    }

    /**
     *
     * @param seconds quantos segundos ela durará
     * @param repetitions são dentro do tempo total, quantas vezes ele irá acontecer
     * @apiNote O dano de poison retira SEMPRE 15% da vida da entidade!
     */
    public void EFFECT_FIRE(double seconds, int repetitions) {
        this.addEffect("Fire", (e) -> {
            //TODO TROCAR PARA SOM DE FOGO!
            Sound.play(Sounds.Poison);
            double x = e.getX() + Engine.RAND.nextInt(e.getWidth());
            double y = e.getY() + Engine.RAND.nextInt(e.getHeight());
            Particle particle = new Volatile("fire", x, y);
            double damage = e.getLifeSize() * 0.15;
            e.strike(AttackTypes.Fire, damage, particle);
        }, seconds, repetitions);
    }

    /**
     *
     * @param seconds quantos segundos ela durará
     * @param repetitions são dentro do tempo total, quantas vezes ele irá acontecer
     * @apiNote A regeneração regenerará 5% da vida da entidade a cada repetição
     */
    public void EFFECT_REGENERATION(double seconds, int repetitions) {
        this.addEffect("Regeneration", (e) -> {
            //TODO TROCAR PARA SOM DE REGENERAÇÃO!
            Sound.play(Sounds.Poison);
            double x = e.getX() + Engine.RAND.nextInt(e.getWidth());
            double y = e.getY() + Engine.RAND.nextInt(e.getHeight());
            Particle particle = new Volatile("heart", x, y);
            double reg = e.getLifeSize() * 0.05;
            e.setLife(e.getLife() + reg);
            Game.getMap().put(particle);
        }, seconds, repetitions);
    }

    public void EFFECT_STUNNED(int seconds) {
        this.addEffect("stunned", (e) -> {
            e.getPhysical().setDrag(0.6);
            e.getPhysical().addForce("stunned", e.getSpeed()*2, Engine.RAND.nextDouble()*Math.PI*2);
            System.out.println("Stunned");
        }, seconds);
    }
}
