package com.retronova.game.objects.furniture;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.map.Arena;

public class Door extends Furniture {

    private boolean opened;

    Door(int ID, double x, double y) {
        super(ID, x, y);
        loadSprites("door");
    }

    public void open() {
        this.opened = true;
    }

    public boolean isOpened() {
        return this.opened;
    }

    @Override
    public void tick() {
        //TODO apenas para passar o cenário, isso não será assim!
        if(Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
            Arena arena = new Arena(1);
            arena.put(Game.getPlayer());
            Game.getGame().changeMap(arena);
        }
    }
}
