package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Sword extends Item {

    private BufferedImage sword_attack;
    private int side;
    private double rad;
    private int count;
    private Entity nearest;

    private double damage;
    private Rectangle boundsAttack;


    Sword(int id) {
        super(id, "Sword", "sword");
        this.damage = 35;
        this.side = 1;
        this.boundsAttack = new Rectangle(GameObject.SIZE()*2, (int)(GameObject.SIZE()*3d));
        sword_attack = new SpriteSheet("items", "sword_attack", Configs.SCALE).getSHEET();
        addSpecifications("Melee Attack", "Player damage + "+this.damage, "level one", "vary fast");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        this.boundsAttack.setLocation((int)player.getX() - player.getWidth()/2 + this.boundsAttack.width/2 * side, (int)player.getY() - (this.boundsAttack.height - player.getHeight())/2);
        this.nearest = player.getNearest(3);
        if(nearest != null) {
            count++;
            if(count > player.getAttackSpeed()*0.1) {
                rad += (Math.PI/12) * side;
                if(Math.abs(rad) > Math.PI) {
                    count = 0;
                    rad = 0;
                    side *= -1;
                    List<Entity> entities = Game.getMap().getEntities();
                    for(int i = 0; i < entities.size(); i++) {
                        Entity e = entities.get(i);
                        if(e.colliding(this.boundsAttack) && e != player && e.isAlive()) {
                            e.strike(AttackTypes.Melee, player.getDamage() + this.damage);
                            double r = e.getAngle(player);
                            e.getPhysical().addForce(12, r);
                        }
                    }
                }
            }
        }else {
            rad = 0;
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
        Point pointRotate = new Point(3 * Configs.SCALE, 12 * Configs.SCALE);
        x -= pointRotate.x;
        y -= pointRotate.y;
        x+= (int) (Math.cos(rad) * Configs.SCALE*6 * side);
        y+= (int) (Math.sin(rad) * Configs.SCALE*4 * side);
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
