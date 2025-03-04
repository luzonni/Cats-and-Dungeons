package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Skeleton extends Entity {

    private static BufferedImage[][] sprite;
    private int countAnim;
    private int indexState;
    private int indexAnim; //


    Skeleton(int ID, double x, double y) {
        super(ID, x, y, 0.5);
        if(sprite == null) {
            sprite = new BufferedImage[][] {loadSprite("mouseskeleton", 0), loadSprite("mouseskeleton", 1)};
        }

        setResistances(AttackTypes.Fire, AttackTypes.Piercing);
        setSolid();
        setAlive();
    }

    @Override
    public BufferedImage getSprite() {
        return sprite[indexState][indexAnim];
    }

    public void tick() {
        moveIA();
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            indexAnim++;
            if(indexAnim >= sprite[indexState].length) {
                indexAnim = 0;
            }
        }
    }

    private void moveIA() {
        Player player = Game.getPlayer();
        double radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce(1, radians);
        this.indexState = getPhysical().isMoving() ? 1 : 0;
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if(orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteSheet.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
    }

    @Override
    public void dispose() {
        sprite = null;
    }

}
