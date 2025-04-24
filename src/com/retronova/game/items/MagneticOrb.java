package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

public class MagneticOrb extends Consumable {

    MagneticOrb(int id) {
        super(id, "Magnetic Orb", "ima");
        addSpecifications("Attracts nearby items");
    }

    @Override
    public void consume() {
        Player player = Game.getPlayer();
        player.setRangeOfColect(Game.getMap().getBounds().getWidth());
    }
}
