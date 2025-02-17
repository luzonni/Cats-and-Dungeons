package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    private BufferedImage[] sprite;
    private int countAnim;
    private int indexAnim;


    Player(int ID, double x, double y) {
        super(ID, x, y, 0.9);
        this.sprite = getSprite("player");
        getPhysical().addForce(100,Math.toRadians(45));
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

        double randians = Math.atan2(vertical, horizontal);

        if(vertical != 0 || horizontal != 0){
            getPhysical().addForce(5, randians);
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
