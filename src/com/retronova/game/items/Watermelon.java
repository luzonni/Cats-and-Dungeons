package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

public class Watermelon extends Consumable {

    Watermelon(int id) {
        super(id, "Watermelon", "watermelon");
        addSpecifications("set 5% of dodge", "Uniq Passive");
    }

    @Override
    public void consume() {
        Player player = Game.getPlayer();
        player.addModifier(Modifiers.Dodge, 0.05);
        player.addPassive(this);
    }
}
