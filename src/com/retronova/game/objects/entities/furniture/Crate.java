package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;

public class Crate extends Furniture {

    private final int[] loot;

    public Crate(int ID, double x, double y, int[] loot) {
        super(ID, x, y, 10);
        loadSprites("crate");
        this.getSheet().setIndex(Engine.RAND.nextInt(getSheet().size()));
        this.loot = loot;
        setClickable();
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        if(player.getDistance(this) < GameObject.SIZE()*3 && Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
            for(int id : loot) {
                dropLoot(Item.build(id));
            }
            disappear();
        }
    }
}
