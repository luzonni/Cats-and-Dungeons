package com.retronova.game.objects.entities.NPCs;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Store;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Seller extends NPC {

    private final Store store;
    private int frame = 0;
    private int animationSpeed = 10; // Ajuste este valor para controlar a velocidade

    public static final Item[][] stock = {
            {
                    Item.build(ItemIDs.Bow.ordinal()), Item.build(ItemIDs.Sword.ordinal()), Item.build(ItemIDs.Feed.ordinal(), 5),
                    Item.build(ItemIDs.Silk.ordinal()), Item.build(ItemIDs.Bomb.ordinal()), Item.build(ItemIDs.Laser.ordinal()),
                    Item.build(ItemIDs.Catnip.ordinal(), 312)
            },
            {Item.build(ItemIDs.Silk.ordinal()), Item.build(ItemIDs.Bomb.ordinal()), Item.build(ItemIDs.Sword.ordinal())},
    };
    public static final int[][] prices = {
            {12, 16, 7, 2, 12, 1, 5},
            {6, 3, 10}
    };

    public Seller(int ID, double x, double y) {
        super(ID, x, y, 0.5);
        int index = 0;
        this.store = new Store(stock[index], prices[index]);
        loadSprites("seller2");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        if (player.getDistance(this) <= GameObject.SIZE() * 3) {
            Game.getInter().put("store", this.store);
            if (Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
                Game.getInter().open("store");
            }
        } else {
            Game.getInter().remove("store");
        }

        // Lógica de animação
        frame++;
        if (frame >= animationSpeed) {
            frame = 0;
            getSheet().nextFrame();
        }
    }
}