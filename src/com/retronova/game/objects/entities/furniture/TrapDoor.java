package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.map.Arena;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.furniture.Furniture;

public class TrapDoor extends Furniture {

    private int indexSprite;
    private int count;

    public TrapDoor(int ID, double x, double y) {
        super(ID, x, y);
        loadSprites("firetest");
    }

    @Override
    public void tick() {
        count++;
        if (count > 10) {
            count = 0;
            indexSprite++;
            if (indexSprite > 2) {
                indexSprite = 0;
            }
        }

        if (indexSprite == 2) {
            if (Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
                Arena arena = new Arena(1);
                arena.put(Game.getPlayer());
                Game.getGame().changeMap(arena);
            }
        }
        getSheet().setIndex(indexSprite);
    }
}