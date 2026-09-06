package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.utilities.ThrownFurball;

import java.awt.*;

public class Furball extends Item {
    int cooldown;

    Furball(int id) {
        super(id, "Furball", "furball");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        cooldown++;
        if(cooldown > player.getAttackSpeed()){
            cooldown = 0;
            for(double angle = 0; angle < Math.PI * 2; angle += (Math.PI * 2) / 6){
                ThrownFurball ball = new ThrownFurball(player.getX(), player.getY(), angle);
                Game.getMap().put(ball);

            }
        }
    }

    @Override
    public void render(Graphics2D g) {

    }
}
