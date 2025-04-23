package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;

public class BloodyAxe extends Item {

    BloodyAxe(int id) {
        super(id, "Bloody Axe", "sprite");
    }


    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Enemy enemy = player.getNearest(2, Enemy.class);
        if(enemy != null){
            double damage = player.getDamage();
            enemy.strike(AttackTypes.Melee, damage);
            player.setLife(player.getLife() + damage * (0.10));
        }
    }

    @Override
    public void render(Graphics2D g) {

    }
}
