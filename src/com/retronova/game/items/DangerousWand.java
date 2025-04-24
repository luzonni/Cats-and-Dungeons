package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.util.List;

public class DangerousWand extends Item{
    private final Rectangle boundsAttack;
    private double angle;

    DangerousWand(int id) {
        super(id, "Dangerous Wand", "sprite");
        this.boundsAttack = new Rectangle(GameObject.SIZE(),GameObject.SIZE());

    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        double x = player.getX() + (player.getWidth() / 2) + Math.cos(this.angle) * (GameObject.SIZE() * 2) - this.boundsAttack.width / 2;
        double y = player.getY() + (player.getHeight() / 2) + Math.sin(this.angle) * (GameObject.SIZE() * 2) - this.boundsAttack.height / 2;
        this.boundsAttack.setLocation((int) x, (int) y);
        this.angle += Math.PI / 16;
        this.attack(player);
    }
    private void attack(Player player){
        List<Enemy> entities = Game.getMap().getEntities(Enemy.class);
        for (int i = 0; i < entities.size(); i++) {
            Enemy e = entities.get(i);
            if(e.colliding(this.boundsAttack)){
                e.strike(AttackTypes.Melee, player.getDamage() * 0.5);
                break;
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.red);
        g.drawRect(this.boundsAttack.x - Game.C.getX(), this.boundsAttack.y - Game.C.getY(), this.boundsAttack.width, this.boundsAttack.height);
    }
}
