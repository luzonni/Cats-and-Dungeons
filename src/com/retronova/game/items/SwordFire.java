package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SwordFire extends Item {

    private int side;
    private double rad;
    private int count;

    private final double damage;
    private final Rectangle boundsAttack;


    SwordFire(int id) {
        super(id, "SwordFire", "swordfire");
        setIndexSprite(Engine.RAND.nextInt(25));
        this.damage = 35;
        this.side = 1;
        this.boundsAttack = new Rectangle(GameObject.SIZE()*2, (int)(GameObject.SIZE()*3d));
        addSpecifications("Fire Attack", "Player damage + "+ this.damage, "very fast");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        setBoundsAttack(player);
        Enemy nearest = player.getNearest(3, Enemy.class);
        if(nearest != null) {
            count++;
            if(count > player.getAttackSpeed()*0.1) {
                rad += (Math.PI/12) * side;
                if(Math.abs(rad) > Math.PI) {
                    count = 0;
                    rad = 0;
                    side *= -1;
                    attack(player, nearest);
                }
            }
        }else {
            rad = 0;
        }
    }

    private void setBoundsAttack(Player player) {
        int dist = (this.side == -1) ? this.boundsAttack.width * side : 0;
        double x = (player.getX() + player.getWidth()/2d) + dist;
        double y = (player.getY() + player.getHeight()/2d) - this.boundsAttack.height/2d;
        this.boundsAttack.setLocation((int)x, (int)y);
    }

    private void attack(Player player, Enemy enemy) {
        if(enemy.colliding(this.boundsAttack)) {
            enemy.strike(AttackTypes.Fire, this.damage + player.getDamage());
            double r = enemy.getAngle(player);
            enemy.getPhysical().addForce("knockback", 16, r);
        }
        Sound.play(Sounds.Sword);
    }

    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        double x = player.getX() + player.getWidth()/2d - Game.C.getX();
        double y = player.getY() + player.getHeight()/1.5d - Game.C.getY();
        renderSword((int)x, (int)y, g);
        Rectangle rec = this.boundsAttack;
        g.setColor(Color.red);
        g.drawRect(rec.x - Game.C.getX(), rec.y - Game.C.getY(), rec.width, rec.height);
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
}