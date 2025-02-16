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
