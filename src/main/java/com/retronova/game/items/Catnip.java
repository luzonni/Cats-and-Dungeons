package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

public class Catnip extends Consumable {

    Catnip(int id) {
        super(id, "Catnip", "catnip");
        addSpecifications("add +3 of speed", "less 3 of range");
    }

    @Override
    public void consume() {
        Player player = Game.getPlayer();
        player.addModifier(Modifiers.Speed, +3);
        player.addModifier(Modifiers.Range, -3);
        player.addPassive(this);
    }
}
