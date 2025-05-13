package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.interfaces.GumInterface;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Volatile;

public class GumMachine extends Furniture {

    private final GumInterface gumInterface;

    private Item[] teste = {//TODO esses itens precisam ser passados pelo parametro do construtor command.
            Item.build(ItemIDs.Bow.ordinal()),
            Item.build(ItemIDs.Acorn.ordinal(), 2),
            Item.build(ItemIDs.Feed.ordinal(), 5),
            Item.build(ItemIDs.Sword.ordinal()),
            Item.build(ItemIDs.Sword.ordinal()),
            Item.build(ItemIDs.Bomb.ordinal()),
            Item.build(ItemIDs.Catnip.ordinal(), 1),
            Item.build(ItemIDs.Catnip.ordinal(), 6),
            Item.build(ItemIDs.Laser.ordinal()),
            Item.build(ItemIDs.Wand.ordinal()),
            Item.build(ItemIDs.Watermelon.ordinal(), 2),
            Item.build(ItemIDs.Watermelon.ordinal(), 6),
            Item.build(ItemIDs.Silk.ordinal()),
            Item.build(ItemIDs.Silk.ordinal()),
    };
    private int price;

    public GumMachine(int ID, double x, double y, String command) {
        super(ID, x, y, 500);
        loadSprites("gum_machine");
        setClickable();
        this.price = 98;
        this.gumInterface = new GumInterface(this, teste);
    }

    public int getPrice() {
        return this.price;
    }

    @Override
    public void disappear() {
        this.gumInterface.dispose();
        Game.getMap().remove(this);
        for(int i = 0; i < 10; i++) {
            Particle dust = new Volatile("dust", getX()+getWidth()/2d, getY()+getHeight()/2d);
            Game.getMap().put(dust);
        }
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        if (player.getDistance(this) <= GameObject.SIZE() * 3) {
            if (GameMap.clickOnRect(Mouse_Button.LEFT, this.getBounds())) {
                Game.getInter().put("gum", this.gumInterface, true);
                Game.getInter().open("gum");
            }
        }
    }
}
