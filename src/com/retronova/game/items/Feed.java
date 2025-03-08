package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

public class Feed extends Passive{


    Feed(int id) {
        super(id, "Feed", "feed");
    }

    @Override
    public void apply() {
        Player player = Game.getPlayer();
        double currentLifeSize = player.getLifeSize();

    }
}
