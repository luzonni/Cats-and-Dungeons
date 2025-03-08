package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MouseSquire extends Entity {

    private int countAnim;
    private int cooldown;

    MouseSquire(int ID, double x, double y) {
        super(ID, x, y, 0.4);
        loadSprites("mousesquire");
        setResistances(AttackTypes.Melee, AttackTypes.Piercing); //resistencias que o personagem tem
        setSolid();
        setAlive();
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
    }

    public void animation() {
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
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

}
