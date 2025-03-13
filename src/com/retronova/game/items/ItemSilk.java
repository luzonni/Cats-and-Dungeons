package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Entity;
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
        Enemy nearest = player.getNearest(player.getRange(), Enemy.class);
        if(nearest != null)
            count++;
        if(count > player.getAttackSpeed()*3) {
            count = 0;
            Silk silk = new Silk(player.getX(), player.getY(), player.getDamage(), player);
            Game.getMap().put(silk);
        }
    }

    @Override
    public void render(Graphics2D g) {

    }

}
