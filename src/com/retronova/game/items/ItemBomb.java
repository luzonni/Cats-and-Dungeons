package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Bomb;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class ItemBomb extends Item{
    private int count;

    ItemBomb(int id) {
        super(id, "Bomb", "bomb");
        addSpecifications("EXPLODE", "you need to run!");
    }

    @Override
    public void tick() {
        count++;
        if(count > 60*2) {
            count = 0;
            Player player = Game.getPlayer();
            Entity nearest = player.getNearest(3);
            if(nearest != null) {
                Bomb bomb = new Bomb(player.getX(), player.getY(), player.getDamage(), player);
                bomb.getPhysical().addForce(20, nearest.getAngle(player));
                Game.getMap().getEntities().add(bomb);
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        int x = (int) player.getX() - Game.C.getX();
        int y = (int) player.getY() - Game.C.getY();
        g.drawImage(getSprite(), x, y, null);
    }
}
