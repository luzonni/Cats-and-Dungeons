package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    private BufferedImage[] sprite;
    private int countAnim;
    private int indexAnim;

    private double damege;
    private final double speed;

    public Player(double x, double y, String nome, double friction, double damege, double speed) {
        super(0, x, y, friction);
        this.damege = damege;
        this.speed = speed * Engine.SCALE;
        this.sprite = getSprite("player", nome);
    }

    @Override
    public BufferedImage getSprite() {
        return sprite[0];
    }

    @Override
    public void tick() {
        super.tick();
        updateMovement();
        if(countAnim > 2) {
            countAnim = 0;
            indexAnim++;
            if(indexAnim >= sprite.length) {
                indexAnim = 0;
            }
        }
    }

    private void updateMovement(){
        int vertical = 0;
        int horizontal = 0;


        if(KeyBoard.KeyPressing("W") || KeyBoard.KeyPressing("Up")){
            vertical = -1;
        }
        if(KeyBoard.KeyPressing("S") || KeyBoard.KeyPressing("Down")){
            vertical = 1;
        }
        if(KeyBoard.KeyPressing("A") || KeyBoard.KeyPressing("Left")){
            horizontal = -1;
        }
        if(KeyBoard.KeyPressing("D") || KeyBoard.KeyPressing("Right")){
            horizontal = 1;
        }

        double radians = Math.atan2(vertical, horizontal);

        if(vertical != 0 || horizontal != 0){
            getPhysical().addForce(this.speed, radians);
        }


    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(sprite[indexAnim], g);
    }

    @Override
    public void dispose() {
        this.sprite = null;
    }
}
