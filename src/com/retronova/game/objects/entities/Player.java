package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Inventory;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.particles.Word;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {

    public static final Player[] TEMPLATES = new Player[] {
            new Player("cinzento", 100,10, 5, 1, 15, 8, 5, 3),
            new Player("mago", 80,10, 5, 0.3, 12, 11, 5, 4),
            new Player("sortudo", 1000,200, 7, 0.3, 1, 50, 5, 4)
    };

    public static Player newPlayer(int index) {
        Player p = TEMPLATES[index];
        Player player = new Player(
                p.getName(),
                p.getLife(),
                p.getDamage(),
                p.getSpeed(),
                p.getLuck(),
                p.getAttackSpeed(),
                p.getRange(),
                p.getInventory().getBagSize(),
                p.getInventory().getHotbarSize()
        );
        System.out.println("Add player inventory in interfaces");
        return player;
    }

    private final String name;
    private double XP;
    private int money;
    private int level;
    private int countAnim;

    private final double luck; // 0.0 ~ 1.0

    private int countDash;
    private boolean dash;

    private final List<Consumable> passives;

    private final Inventory inventory;

    Player(String name, double life, double damage, double speed, double luck, double attackSpeed, double range, int bagSize, int hotSize) {
        super(0, 0, 0, 0.5);
        this.name = name;
        this.luck = luck;
        this.inventory = new Inventory(bagSize, hotSize);
        this.passives = new ArrayList<>();
        setWidth(0.85);
        setHeight(0.9);
        loadSprites("player_"+name+"_idle", "player_"+name+"_walking");
        setLife(life);
        setDamage(damage);
        setSpeed(speed);
        setAttackSpeed(attackSpeed);
        setRange(range);
        setSolid();
        setMoney(100);
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Player.class, sprites));
    }

    public String getName() {
        return this.name;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public BufferedImage getSprite(int index) {
        getSheet().setIndex(index);
        return getSheet().getSprite();
    }

    @Override
    public void tick() {
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
        if(!(Engine.getACTIVITY() instanceof Game))
            return;
        updateMovement();
        tickItemHand();
        if(KeyBoard.KeyPressed("F")) {
            //TODO teste de efeitos
            this.EFFECT_FIRE(10, 10);
            //this.EFFECT_REGENERATION(10, 100);
        }
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        if (modifiers.containsKey(Modifiers.Dodge)) {
            double percent = modifiers.get(Modifiers.Dodge) + getLuck() * 0.10d;
            double a = Engine.RAND.nextDouble(1d);
            if (a <= percent) {
                Game.getMap().put(new Word("Dodge", getX() + getWidth() / 2d, getY() + getHeight() / 2d, 1));
                return;
            }
        }
        Sound.play(Sounds.DamageCat);
        super.strike(type, damage);
    }

    @Override
    public void die() {
        disappear();
    }

    public double getLuck() {
        if(this.modifiers.containsKey(Modifiers.Luck)) {
            double l = this.luck + this.modifiers.get(Modifiers.Luck);
            if(l < 0)
                l = 0;
            return l;
        }
        return luck;
    }

    public List<Consumable> getPassives() {
        return List.copyOf(this.passives);
    }

    public void addPassive(Consumable passive) {
        if(this.passives.contains(passive)) {
            int i = this.passives.indexOf(passive);
            int stack = this.passives.get(i).getStack();
            this.passives.get(i).setStack(stack + 1);
        }else {
            this.passives.add((Consumable) Item.build(passive.getID(), 1));
        }
    }

    public void plusXp(double weight) {
        this.XP += weight;
        while(getXp() >= getXpLength()) { //caso ganhe um xp com peso muito alto, o nivel vai aumentar até o chegar no nivel considerado
            this.XP -= getXpLength();
            this.level++;
        }
    }

    public double getXp() {
        return this.XP;
    }

    public double getXpLength() {
        if(getLevel() == 0)
            return 75;
        return getLevel() * 23.74 + 100 * getLevel()*1.33;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public boolean takeLevel(double levles) {
        if(this.level <= levles) {
            this.level -= levles;
            return true;
        }
        return false;
    }

    public int getLevel() {
        return this.level;
    }

    private void tickItemHand() {
        Item item = getInventory().getItemHand();
        if(item != null)
            item.tick();
    }

    private void updateMovement(){
        if(!(Engine.getACTIVITY() instanceof Game))
            return;
        getSheet().setType(getPhysical().isMoving() ? 1 : 0);
        int vertical = 0;
        int horizontal = 0;
        if(KeyBoard.KeyPressing("W") || KeyBoard.KeyPressing("Up")){
            vertical = -1;
        }
        if(KeyBoard.KeyPressing("S") || KeyBoard.KeyPressing("Down")){
            vertical = 1;
        }
        if(KeyBoard.KeyPressing("A") || KeyBoard.KeyPressing("Left")){
            horizontal = -1;
        }
        if(KeyBoard.KeyPressing("D") || KeyBoard.KeyPressing("Right")){
            horizontal = 1;
        }
        countDash++;
        if(KeyBoard.KeyPressed("SPACE")) {
            if(modifiers.containsKey(Modifiers.Dash) && countDash > 45) {
                dash = true;
                countDash = 0;
            }
        }
        double radians = Math.atan2(vertical, horizontal);
        if(vertical != 0 || horizontal != 0){
            if(dash) {
                addEffect("dash", (e) -> {
                    getPhysical().addForce("dash", getSpeed() * modifiers.get(Modifiers.Dash), radians);
                    e.getPhysical().setFriction(0.1d);
                }, 0.01d);
                dash = false;
            }
            getPhysical().addForce("move", getSpeed(), radians);
        }
    }

    @Override
    public void render(Graphics2D g) {
        BufferedImage sprite = getSprite();
        if(getPhysical().getOrientation()[0] == -1)
            sprite = SpriteSheet.flip(sprite, 1, -1);
        renderSprite(sprite, g);
        drawItem(g);
    }

    private void drawItem(Graphics2D g) {
        Item item = getInventory().getItemHand();
        if(item == null)
            return;
        item.render(g);
    }

    public String[] getInfo() {
        return new String[] {
                "HP: " + getLife(),
                "Attack: " + getDamage(),
                "Speed: " + getSpeedDescription(),
                "Luck: " + getLuck(),
                "Attack Speed: " + getAttackSpeed(),
                "Range: " + getRange(),
                "Bag Size: " + getInventory().getBagSize(),
                "Hotbar Size: " + getInventory().getHotbarSize()

        };
    }

    private String getSpeedDescription() {
        double speed = getSpeed();
        if (speed < 0.3) {
            return "Slow";
        } else if (speed < 0.5) {
            return "Normal";
        } else {
            return "Fast";
        }
    }
}