package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Sword extends Item {

    private int side;
    private double rad;
    private int count;

    private final BufferedImage sword_attack;
    private final double damage;
    private final Rectangle boundsAttack;


    Sword(int id) {
        super(id, "Sword", "sword");
        setIndexSprite(Engine.RAND.nextInt(25));
        this.damage = 35;
        this.side = 1;
        this.boundsAttack = new Rectangle(GameObject.SIZE()*2, (int)(GameObject.SIZE()*3d));
        sword_attack = new SpriteHandler("sprites/items", "sword_attack", Configs.GameScale()).getSHEET();
        addSpecifications("Melee Attack", "Player damage + "+ this.damage, "very fast");
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
            enemy.strike(AttackTypes.Melee, this.damage + player.getDamage());
            double r = enemy.getAngle(player);
            enemy.getPhysical().addForce("knockback", 3, r);
        }
        Sound.play(Sounds.Sword);
    }

    public void render(Graphics2D g) {
        Player player = Game.getPlayer();
        double x = player.getX() + player.getWidth()/2d;
        double y = player.getY() + player.getHeight()/1.5d;
        renderSword((int)x, (int)y, g);
        drawAttackEffect(g);
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
            BufferedImage flipped = SpriteHandler.flip(this.sword_attack,1, side);
            int x = this.boundsAttack.x + (this.boundsAttack.width - this.sword_attack.getWidth())/2;
            int y = this.boundsAttack.y + (this.boundsAttack.height - this.sword_attack.getHeight())/2;
            g.drawImage(flipped, x, y, null);
        }
    }

}
