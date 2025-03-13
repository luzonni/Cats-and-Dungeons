package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.Bomb;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.util.List;

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
            if(canThrow()){
                return;
            }
            Enemy nearest = player.getNearest(10, Enemy.class);
            if(nearest != null) {
                Bomb bomb = new Bomb(player.getX(), player.getY(), player.getDamage() * 3, player);
                bomb.getPhysical().addForce("throw", 10, nearest.getAngle(player));
                Game.getMap().put(bomb);
            }
        }
    }

    public boolean canThrow() {
        List<Entity> entities = Game.getMap().getEntities();
        for(int i = 0; i < entities.size(); i++) {
            if(entities.get(i) instanceof Bomb) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Graphics2D g) {
    }
}
