package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.exceptions.PlayerInstanceException;
import com.retronova.game.Game;
import com.retronova.game.interfaces.inventory.Inventory;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.map.Camera;
import com.retronova.graphics.SpriteSheet;
import com.retronova.inputs.keyboard.KeyBoard;
import com.retronova.inputs.mouse.Mouse;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    public static final Player[] TEMPLATES = new Player[] {
            new Player("cinzento", 10, 5),
            new Player("mago", 15, 4),
            new Player("sortudo", 15, 6)
    };

    public static Player newPlayer(int index) {
        return new Player(TEMPLATES[index].getName(), TEMPLATES[index].damege, TEMPLATES[index].speed);
    }

    private String name;
    private BufferedImage[][] sprite;
    private int countAnim;
    private int indexAnim;
    private int stateAnim;

    private double damege;
    private final double speed;

    private Inventory inventory;

    public Player() {
        super(0, 0, 0, 0);
        throw new PlayerInstanceException("Não é permitido instancia o player dessa maneira!");
    }

    private Player(String name, double damage, double speed) {
        super(0, 0, 0, 0.5);
        this.name = name;
        this.damege = damage;
        this.speed = speed;
        this.sprite = new BufferedImage[][] {
                getSprite("player/"+name, 0),
                getSprite("player/"+name, 1)
        };
        //TODO faze cada player ter o tamanho da mochila diferente!
        this.inventory = new Inventory(4, 3);
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
        double angle = Math.atan2((getY() - Game.C.getY()) - Mouse.getY(), (getX() - Game.C.getX()) - Mouse.getX());
        g.rotate(angle, getX() - Game.C.getX(), getY() - Game.C.getY());
        g.drawImage(item.getSprite(), (int)(getX() - Game.C.getX()), (int)(getY() - Game.C.getY()), null);
        g.rotate(-angle, getX() - Game.C.getX(), getY() - Game.C.getY());
    }

    @Override
    public void dispose() {
        this.sprite = null;
    }

}
