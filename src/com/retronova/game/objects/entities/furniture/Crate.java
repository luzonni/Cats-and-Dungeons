package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;

public class Crate extends Furniture {

    private final int[] loot;

    public Crate(int ID, double x, double y, int[] loot) {
        super(ID, x, y, 60);
        loadSprites("crate");
        this.getSheet().setIndex(Engine.RAND.nextInt(getSheet().size()));
        this.loot = loot;
        setClickable();
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        if(player.getDistance(this) < GameObject.SIZE()*3 && GameMap.clickOnRect(Mouse_Button.LEFT, this.getBounds())) {
            for(int id : loot) {
                dropLoot(Item.build(id));
            }
            disappear();
        }
    }
}
