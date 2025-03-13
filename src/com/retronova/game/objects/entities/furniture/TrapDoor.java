package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.map.Arena;
import com.retronova.game.objects.entities.Player;

public class TrapDoor extends Furniture {
    private int indexSprite;
    private int count;

    public TrapDoor(int ID, double x, double y) {
        super(ID, x, y);
        loadSprites("trapdoor");
    }

    @Override
    public void tick() {
        Player nearest = getNearest(2, Player.class);
        if(nearest != null) {
            count++;
            if(count > 7 && indexSprite < 11) {
                indexSprite++;
            }
        }else {
            count++;
            if(count > 0 && indexSprite > 0) {
                indexSprite--;
            }
        }
        if(indexSprite == 11) {
            if(Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
                Arena arena = new Arena(1);
                arena.put(Game.getPlayer());
                Game.getGame().changeMap(arena);
            }
        }
        getSheet().setIndex(indexSprite);
    }
}