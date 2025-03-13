package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Sword extends Item {

    private int side;
    private double rad;
    private int count;

    private final BufferedImage sword_attack;
    private final double damage;
    private final Rectangle boundsAttack;


    Sword(int id) {
        super(id, "Sword", "sword");
        this.damage = 35;
        this.side = 1;
        this.boundsAttack = new Rectangle(GameObject.SIZE()*2, (int)(GameObject.SIZE()*3d));
        sword_attack = new SpriteSheet("items", "sword_attack", Configs.GameScale()).getSHEET();
        addSpecifications("Melee Attack", "Player damage + "+this.damage, "very fast");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        this.boundsAttack.setLocation((int)player.getX() - player.getWidth()/2 + this.boundsAttack.width/2 * side, (int)player.getY() - (this.boundsAttack.height - player.getHeight())/2);
        Entity nearest = player.getNearest(3, Enemy.class);
        if(nearest != null) {
            count++;
            if(count > player.getAttackSpeed()*0.1) {
                rad += (Math.PI/12) * side;
                if(Math.abs(rad) > Math.PI) {
                    count = 0;
                    rad = 0;
                    side *= -1;
                    attack(player);
                }
            }
        }else {
            rad = 0;
        }
    }

    private void attack(Player player) {
        List<Enemy> enemies = Game.getMap().getEntities(Enemy.class);
        for(int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if(e.colliding(this.boundsAttack)) {
                e.strike(AttackTypes.Melee, player.getDamage() + this.damage);
                double r = e.getAngle(player);
                e.getPhysical().addForce("knockback", 16, r);
            }
        }
    }

    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        double x = player.getX() + player.getWidth()/2d - Game.C.getX();
        double y = player.getY() + player.getHeight()/1.5d - Game.C.getY();
        renderSword((int)x, (int)y, g);
        drawAttackEffect(g);
//        g.setColor(Color.red);
//        g.drawRect(this.boundsAttack.x - Game.C.getX(), this.boundsAttack.y - Game.C.getY(), this.boundsAttack.width, this.boundsAttack.height);
    }

    private void renderSword(int x, int y, Graphics2D g) {
        BufferedImage sprite = getSprite();
        Point pointRotate = new Point(3 * Configs.GameScale(), 12 * Configs.GameScale());
        x -= pointRotate.x;
        y -= pointRotate.y;
        x+= (int) (Math.cos(rad) * Configs.GameScale() *6 * side);
        y+= (int) (Math.sin(rad) * Configs.GameScale() *4 * side);
        double rotate = (rad - Math.PI/4) + Math.PI/8*side;
        Rotate.draw(sprite, x, y, rotate, pointRotate, g);
    }

    private void drawAttackEffect(Graphics2D g) {
        if(Math.abs(rad) > Math.PI/4 && Math.abs(rad) < Math.PI/2) {
            BufferedImage flipped = SpriteSheet.flip(this.sword_attack,1, side);
            int x = this.boundsAttack.x + (this.boundsAttack.width - this.sword_attack.getWidth())/2 - Game.C.getX();
            int y = this.boundsAttack.y + (this.boundsAttack.height - this.sword_attack.getHeight())/2 - Game.C.getY();
            g.drawImage(flipped, x, y, null);
        }
    }

}
