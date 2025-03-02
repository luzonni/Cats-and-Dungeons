package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.util.List;

public class Silk extends Item {

    Silk(int id) {
        super(id, "Silk", "silk");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        List<Entity> entities = Game.getMap().getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(e.equals(player))
                continue;
            if(player.getDistance(e) <= GameObject.SIZE()*3) {
                //Melhorar interação de dano.
                e.strike(AttackTypes.Melee, 10);
            }
        }
    }
}
