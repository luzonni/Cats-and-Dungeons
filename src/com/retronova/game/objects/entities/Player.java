package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.interfaces.inventory.Inventory;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    public static final Player[] TEMPLATES = new Player[] {
            new Player("cinzento", 10, 5, 0.3, 15, 30, 7.2, 5, 4),
            new Player("mago", 10, 5, 0.3, 12, 11, 5, 5, 4),
            new Player("sortudo", 10, 5, 0.3, 12, 11, 5, 5, 4)
    };

    public static Player newPlayer(int index) {
        Player p = TEMPLATES[index];
        return new Player(
                p.getName(),
                p.getDamage(),
                p.getSpeed(),
                p.getLuck(),
                p.getAttackSpeed(),
                p.getRangeDamage(),
                p.getRange(),
                p.getInventory().getBagSize(),
                p.getInventory().getHotbarSize()
        );
    }

    private final String name;
    private BufferedImage[][] sprite;
    private int countAnim;
    private int indexAnim;
    private int stateAnim;

    private double speed;
    private double damage;
    private double luck;
    private int attackSpeed;
    private double rangeDamage;
    private double range;

    private final Inventory inventory;

    Player(String name, double damage, double speed, double luck, int attackSpeed, double rangeDamage, double range, int bagSize, int hotSize) {
        super(0, 0, 0, 0.5);
        this.name = name;
        this.damage = damage;
        this.speed = speed;
        this.luck = luck;
        this.attackSpeed = attackSpeed;
        this.rangeDamage = rangeDamage;
        this.range = range;
        this.inventory = new Inventory(bagSize, hotSize);
        this.sprite = new BufferedImage[][] {
                loadSprite("player/"+name, 0),
                loadSprite("player/"+name, 1)
        };
        setSolid();
        setAlive();
    }

    @Override
    public BufferedImage getSprite() {
        return sprite[stateAnim][indexAnim];
    }

    public String getName() {
        return this.name;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public void tick() {
        updateMovement();
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            indexAnim++;
            if (indexAnim >= sprite[stateAnim].length) {
                indexAnim = 0;
            }
        }
        tickItemHand();
    }
    public double getRangeDamage() {
        return rangeDamage;
    }

    public void setRangeDamage(double rangeDamage) {
        this.rangeDamage = rangeDamage;
    }

    public int getAttackSpeed() {
        return attackSpeed;
    }

    public void setAttackSpeed(int attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public double getLuck() {
        return luck;
    }

    public void setLuck(double luck) {
        this.luck = luck;
    }

    public double getSpeed() {
        return speed;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    private void tickItemHand() {
        Item item = getInventory().getItemHand();
        if(item != null)
            item.tick();
    }

    private void updateMovement(){
        if(!(Engine.getACTIVITY() instanceof Game))
            return;
        stateAnim = getPhysical().isMoving() ? 1 : 0;
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
            getPhysical().addForce(this.speed, radians);
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

    @Override
    public void dispose() {
        this.sprite = null;
    }

}
