package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseSquire extends Entity {
    private final BufferedImage[][] sprite;
    private int countAnim; //tempo de cada mudança
    private int indexState; //estado atual da animação
    private int indexAnim;
    private int cooldown;

    MouseSquire(int ID, double x, double y) {
        super(ID, x, y, 0.4);
        sprite = new BufferedImage[][] {loadSprite("mousesquire", 0), loadSprite("mousesquire", 1)};
        setResistances(AttackTypes.Melee, AttackTypes.Piercing); //resistencias que o personagem tem
        setSolid();
        setAlive();
    }

    @Override
    public BufferedImage getSprite() {
        return sprite [indexState][indexAnim];
    }

    public void tick() {
        moveIA();
        animation();
        setDamage();
    }

    public void setDamage() {
        Player player = Game.getPlayer(); // tem que pegar a instância do player pelo Game.getPlayer
        cooldown++;
        if(player.getBounds().intersects(this.getBounds()) && cooldown > 40) { // getBounds acho que é a posição ao redor do jogador
            cooldown = 0;
            player.strike(AttackTypes.Melee, 6);
            player.getPhysical().addForce(3.0d, getPhysical().getAngleForce());
        }
    }

    public void moveIA() {
        Player player = Game.getPlayer();
        if(this.getDistance(player) < GameObject.SIZE() * 6) {
            double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
            getPhysical().addForce(0.75d, radians);
        }
        this.indexState = getPhysical().isMoving() ? 1 : 0;
    }

    public void animation() {
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            indexAnim++;
            if(indexAnim >= sprite[indexState].length) {
                indexAnim = 0;
            }
        }
    }

    @Override
    public void render(Graphics2D d) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, d);
    }

    @Override
    public void dispose() {};

}
