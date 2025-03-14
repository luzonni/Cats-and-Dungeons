package com.retronova.game.objects.entities.NPCs;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.interfaces.store.Store;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

public class Seller extends NPC {

    private Store store;
    private Item[][] stock = {
            {Item.build(ItemIDs.Sword), Item.build(ItemIDs.Silk), Item.build(ItemIDs.Bow)},
            {Item.build(ItemIDs.Silk), Item.build(ItemIDs.Bomb), Item.build(ItemIDs.Sword)},
    };

    private int[][] prices = {
            {12, 16, 7}
    };


    public Seller(int ID, double x, double y) {
        super(ID, x, y, 0.5);
        int index = 0;
        this.store = new Store(stock[index], prices[index]);
        loadSprites("seller");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        if(player.getDistance(this) <= GameObject.SIZE()*3) {
            Game.getInter().put("store", this.store);
            if(Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
                Game.getInter().open("store");
            }
        }else {
            Game.getInter().remove("store");
        }
    }
}
