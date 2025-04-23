package com.retronova.game.objects.entities.NPCs;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Store;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Word;

public class Seller extends NPC {

    private final Store store;
    private int countAnim;

    public static final Item[][] stock = {
            {
                    Item.build(ItemIDs.ClawBlades.ordinal()), Item.build(ItemIDs.Kunai.ordinal()), Item.build(ItemIDs.Sword.ordinal()), Item.build(ItemIDs.Feed.ordinal(), 5),
                    Item.build(ItemIDs.Silk.ordinal()), Item.build(ItemIDs.Bomb.ordinal()), Item.build(ItemIDs.Laser.ordinal()),
                    Item.build(ItemIDs.Catnip.ordinal(), 312), Item.build(ItemIDs.Acorn.ordinal(), 23),
                    Item.build(ItemIDs.Watermelon.ordinal(), 12),
                    Item.build(ItemIDs.GasBomb.ordinal()),
                    Item.build(ItemIDs.BloodyAxe.ordinal()),
                    Item.build(ItemIDs.MagneticOrb.ordinal(), 15),
                    Item.build(ItemIDs.BowEletric.ordinal()),
                    Item.build(ItemIDs.SwordFire.ordinal())

            },
            {Item.build(ItemIDs.Silk.ordinal()), Item.build(ItemIDs.Bomb.ordinal()), Item.build(ItemIDs.Sword.ordinal())},
    };

    public static final int[][] prices = {
            {11, 12, 16, 7, 2, 12, 1, 5, 3, 2, 7, 20, 30, 10,8},
            {6, 3, 10}
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
        countAnim++;
        if (player.getDistance(this) <= GameObject.SIZE() * 3) {
            if (Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
                Game.getInter().put("store", this.store, true);
                Game.getInter().open("store");
            }
            if (countAnim >= 10) {
                countAnim = 0;
                getSheet().plusIndex();
            }
        }else {
            getSheet().setIndex(2);
            if (countAnim >= 60) {
                countAnim = 0;
                double x = getX() + Engine.RAND.nextInt(getWidth());
                double y = getY() + Engine.RAND.nextInt(getHeight());
                String z = Engine.RAND.nextBoolean() ? "Z" : "z";
                Particle p = new Word(z, x, y, 1);
                Game.getMap().put(p);
            }
        }
    }
}