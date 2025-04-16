package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.engine.io.Resources;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Inventory;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.particles.Volatile;
import com.retronova.game.objects.particles.Word;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {

    public static final Player[] TEMPLATES = new Player[] {
            build("Muffin"),
            build("Azrael"),
            build("Finn")
    };

    private static Player build(String name) {
        try {
            JSONObject json = Resources.getJsonFile("players", name);
            JSONObject values = (JSONObject) json.get("values");
            JSONArray inventory = (JSONArray) json.get("inventory");
            double life = ((Number)values.get("life")).doubleValue();
            double damage = ((Number)values.get("damage")).doubleValue();
            double speed = ((Number)values.get("speed")).doubleValue();
            double luck = ((Number)values.get("luck")).doubleValue();
            double attackSpeed = ((Number)values.get("attackSpeed")).doubleValue();
            double range = ((Number)values.get("range")).doubleValue();
            int bagSize = ((Number)values.get("bagSize")).intValue();
            int hotBarSize = ((Number)values.get("hotBarSize")).intValue();
            Player player = new Player(name, life, damage, speed, luck, attackSpeed, range, bagSize, hotBarSize);
            for(int i = 0; i < inventory.size(); i++) {
                JSONArray itemValues = (JSONArray) inventory.get(i);
                for(ItemIDs itemID : ItemIDs.values()) {
                    if(itemID.name().equalsIgnoreCase((String)itemValues.get(0))) {
                        int id = itemID.ordinal();
                        int amount = 1;
                        if(itemValues.size() > 1)
                            amount = ((Number)itemValues.get(1)).intValue();
                        Item item = Item.build(id, amount);
                        player.getInventory().give(item);
                        break;
                    }
                }
            }
            return player;
        }catch (IOException ignore) {
            throw new EntityNotFound("Player não encontrado");
        }
    }

    public static Player newPlayer(int index) {
        return build(TEMPLATES[index].getName());
    }

    private final String name;
    private double XP;
    private int money;
    private int level;
    private int countAnim;

    private final double luck;

    private int countDash;
    private boolean dash;
    private int countWalking;

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
        loadSprites("player_"+name.toLowerCase()+"_idle", "player_"+name.toLowerCase()+"_walking");
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
    }

    @Override
    public void strike(AttackTypes type, double damage) {
        if (hasModifier(Modifiers.Dodge)) {
            double percent = valueModifier(Modifiers.Dodge) + getLuck() * 0.10d;
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
        if(this.hasModifier(Modifiers.Luck)) {
            double l = this.luck + this.valueModifier(Modifiers.Luck);
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

    private void updateMovement() {
        if (!(Engine.getACTIVITY() instanceof Game))
            return;
        getSheet().setType(getPhysical().isMoving() ? 1 : 0);
        int vertical = 0;
        int horizontal = 0;
        boolean isMoving = false;

        if (KeyBoard.KeyPressing("W") || KeyBoard.KeyPressing("Up")) {
            vertical = -1;
            isMoving = true;
        }
        if (KeyBoard.KeyPressing("S") || KeyBoard.KeyPressing("Down")) {
            vertical = 1;
            isMoving = true;
        }
        if (KeyBoard.KeyPressing("A") || KeyBoard.KeyPressing("Left")) {
            horizontal = -1;
            isMoving = true;
        }
        if (KeyBoard.KeyPressing("D") || KeyBoard.KeyPressing("Right")) {
            horizontal = 1;
            isMoving = true;
        }

        countDash++;
        if (KeyBoard.KeyPressed("SPACE")) {
            if (hasModifier(Modifiers.Dash) && countDash > 45) {
                dash = true;
                countDash = 0;
            }
        }
        if (isMoving) {
            double radians = Math.atan2(vertical, horizontal);
            if (dash) {
                addEffect("dash", (e) -> {
                    getPhysical().addForce("dash", getSpeed() * valueModifier(Modifiers.Dash), radians);
                    e.getPhysical().setFriction(0.1d);
                }, 0.01d);
                dash = false;
            }
            getPhysical().addForce("move", getSpeed(), radians);
            countWalking++;
            if(countWalking > 10) {
                countWalking = 0;
                Volatile walkingP = new Volatile("walking", getX() + getWidth()/2d, getY() + getHeight());
                Game.getMap().put(walkingP);
                Sound.play(Sounds.Walking);
            }
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
                "Atk Speed: " + getAttackSpeed(),
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