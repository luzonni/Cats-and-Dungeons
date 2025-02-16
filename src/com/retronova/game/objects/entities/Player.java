package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    private BufferedImage[] sprite;
    private int countAnim;
    private int indexAnim;
    private int speed;

    Player(int ID, double x, double y) {
        super(ID, x, y);
        this.sprite = getSprite("player");
        this.speed = 2 * Engine.SCALE;
    }

    @Override
    public void tick() {
        super.tick();
        //Essa logica de movimentação serve apenas de exemplo
        if(KeyBoard.KeyPressing("A") || KeyBoard.KeyPressing("Left")) {
            this.setX(this.getX()-this.speed);
            countAnim++;
        }
        if(KeyBoard.KeyPressing("D") || KeyBoard.KeyPressing("Right")) {
            this.setX(this.getX()+this.speed);
            countAnim++;
        }
        if(KeyBoard.KeyPressing("W") || KeyBoard.KeyPressing("Up")) {
            this.setY(this.getY()-this.speed);
            countAnim++;
        }
        if(KeyBoard.KeyPressing("S") || KeyBoard.KeyPressing("Down")) {
            this.setY(this.getY()+this.speed);
            countAnim++;
        }
        if(countAnim > 2) {
            countAnim = 0;
            indexAnim++;
            if(indexAnim >= sprite.length) {
                indexAnim = 0;
            }
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
