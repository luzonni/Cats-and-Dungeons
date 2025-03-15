package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Inventory;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.game.objects.Sheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Player extends Entity {

    public static final Player[] TEMPLATES = new Player[] {
            new Player("cinzento", 100,10, 5, 0.3, 15, 8, 5, 3),
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

    private double luck; // 0.0 ~ 1.0

    private final List<Consumable> passives;

    private final Inventory inventory;

    Player(String name, double life, double damage, double speed, double luck, double attackSpeed, double range, int bagSize, int hotSize) {
        super(0, 0, 0, 0.5);
        this.name = name;
        this.luck = luck;
        this.inventory = new Inventory(bagSize, hotSize);
        this.passives = new ArrayList<>();
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
        if(KeyBoard.KeyPressed("F")) {//Only teste
            //Teste de aplicação de efeito na entidade player
            this.addEffect(
                    "teste",
                    e -> {
                            e.setLife(e.getLifeSize());
                        },
            1);
        }
        updateMovement();
        tickItemHand();
    }

    @Override
    public void die() {
        Game.restart();
    }

    public double getLuck() {
        if(this.modifiers.containsKey(Modifiers.Luck)) {
            return this.luck + this.modifiers.get(Modifiers.Luck);
        }
        return luck;
    }

    public List<Consumable> getPassives() {
        return List.copyOf(this.passives);
    }

    public void addPassive(Consumable passive) {
        this.passives.add(passive);
    }

    private void setLuck(double luck) {
        this.luck = luck;
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
        double radians = Math.atan2(vertical, horizontal);
        if(vertical != 0 || horizontal != 0){
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
}
