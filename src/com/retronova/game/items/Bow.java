package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Arrow;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.util.List;

public class Bow extends Item{
    private double angle = 0;

    Bow(int id) {
        super(id, "Bow", "bow");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        Entity nearest = player.getNearest();
        if(nearest != null){
            angle = nearest.getAngle(player);
        }
        Arrow arrow = new Arrow(player.getX(), player.getY(), 10, angle);
        Game.getMap().getEntities().add(arrow);
    }

    @Override
    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        int x = (int) player.getX() - Game.C.getX() + player.getWidth() / 2;
        int y = (int) player.getY() - Game.C.getY() + player.getHeight() / 2;

        g.rotate(angle, x, y);
        g.drawImage(getSprite(), x + player.getWidth() / 2, y - getSprite().getHeight() / 2, null);
        g.rotate(-angle, x, y);


    }
}
