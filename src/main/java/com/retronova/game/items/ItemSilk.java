package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.Silk;

import java.awt.*;

public class ItemSilk extends Item {

    private int count;

    ItemSilk(int id) {
        super(id, "Silk", "silk");
        addSpecifications("Throws", "player damage", "very slowed");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Enemy nearest = alvoVisivel(player, player.getRange());
        if(nearest != null)
            count++;
        if(count > player.getAttackSpeed()*3) {
            count = 0;
            Silk silk = new Silk(player.getX() + player.getWidth() / 2d,
                    player.getY() + player.getHeight() / 2d, player.getDamage(), player);
            Game.getMap().put(silk);
        }
    }


}
