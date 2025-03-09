package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

public class Feed extends Passive {


    Feed(int id) {
        super(id, "Feed", "feed");
    }

    @Override
    public void apply() {
        Player player = Game.getPlayer();
        player.addModifier(Modifiers.Life, 10);
        player.addModifier(Modifiers.Luck, -0.1);
    }
}
