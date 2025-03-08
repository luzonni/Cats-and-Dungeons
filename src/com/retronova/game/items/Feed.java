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
        player.setLifeSize(currentLifeSize + 5);
        player.setDamage(player.getDamage() + 5);
        player.setRangeDamage(player.getRangeDamage() + 5);
        player.setLuck(player.getLuck() - 5.0d);

    }
}
