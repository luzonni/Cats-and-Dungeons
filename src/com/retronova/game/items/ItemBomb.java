package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Bomb;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class ItemBomb extends Item {

    private int count;

    ItemBomb(int id) {
        super(id, "Bomb", "bomb");
        addSpecifications("EXPLODE", "you need to run!", "3x player damage", "only one per throw");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        count++;
        if(count > player.getAttackSpeed()*1.5) {
            count = 0;
            Entity nearest = player.getNearest(10);
            if(nearest != null) {
                Bomb bomb = new Bomb(player.getX(), player.getY(), player.getDamage() * 3, player);
                bomb.getPhysical().addForce("throw", 10, nearest.getAngle(player));
                Game.getMap().put(bomb);
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
    }
}
