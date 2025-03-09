package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.interfaces.inventory.Inventory;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Player extends Entity {

    public static final Player[] TEMPLATES = new Player[] {
            new Player("cinzento", 10, 5, 0.3, 15, 30, 5, 4),
            new Player("mago", 10, 5, 0.3, 12, 11, 5, 4),
            new Player("sortudo", 200, 7, 0.3, 1, 50, 5, 4)
    };

    public static Player newPlayer(int index) {
        Player p = TEMPLATES[index];
        return new Player(
                p.getName(),
                p.getDamage(),
                p.getSpeed(),
                p.getLuck(),
                p.getAttackSpeed(),
                p.getRange(),
                p.getInventory().getBagSize(),
                p.getInventory().getHotbarSize()
        );
    }

    private final String name;
    private double XP;
    private int level;
    private int countAnim;

    private double luck;

    private final Inventory inventory;

    Player(String name, double damage, double speed, double luck, double attackSpeed, double range, int bagSize, int hotSize) {
        super(0, 0, 0, 0.5);
        this.name = name;
        this.luck = luck;
        this.inventory = new Inventory(bagSize, hotSize);
        loadSprites("player_"+name+"_idle", "player_"+name+"_walking");
        setDamage(damage);
        setSpeed(speed);
        setAttackSpeed(attackSpeed);
        setRange(range);
        setSolid();
        setAlive();
    }

    public String getName() {
        return this.name;
    }

    public Inventory getInventory() {
        return this.inventory;
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
//            this.addEffect(
//                    "nomeDoEfeito",
//                    e -> {
//                            e.strike(AttackTypes.Fire, 1);
//                        },
//            1);
            List<Entity> entities = Game.getMap().getEntities();
            for(int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                if(e != this) {
                    e.disappear();
                }
            }
        }
        updateMovement();
        tickItemHand();
    }

    @Override
    public void die() {
        super.die();
        Game.restart();
    }

    public double getLuck() {
        if(this.modifiers.containsKey(Modifiers.Luck)) {
            return this.luck + this.modifiers.get(Modifiers.Luck);
        }
        return luck;
    }

    private void setLuck(double luck) {
        this.luck = luck;
    }

    public void plusXp(double weight) {
        double currentXp = getXp();
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
        if(KeyBoard.KeyPressing("W") || KeyBoard.KeyPressing("Up")){
            getPhysical().addForce(getSpeed(), -Math.PI/2);
        }
        if(KeyBoard.KeyPressing("S") || KeyBoard.KeyPressing("Down")){
            getPhysical().addForce(getSpeed(), Math.PI/2);
        }
        if(KeyBoard.KeyPressing("A") || KeyBoard.KeyPressing("Left")){
            getPhysical().addForce(getSpeed(), Math.PI);
        }
        if(KeyBoard.KeyPressing("D") || KeyBoard.KeyPressing("Right")){
            getPhysical().addForce(getSpeed(), 0);
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
