package com.retronova.game.objects.particles;

import com.retronova.engine.graphics.Alpha;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class Portal extends Particle {

    private int count;
    private final Player player;
    private int animationTimer;
    private final int animationSpeed = 14;
    private final int yOffset = 10;

    public Portal(double x, double y, double seconds, Player player) {
        super(x, y, seconds);
        loadSprites("firetest");
        this.player = player;
    }

    @Override
    public void tick() {
        setX(player.getX());
        setY(player.getY());
        count++;

        animationTimer++;
        if (animationTimer >= animationSpeed) {
            getSheet().plusIndex();
            animationTimer = 0;
        }

        if (count >= getSeconds()) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        float t = 1f - count / (float) getSeconds();
        int x = (int) getX() - Game.C.getX();
        int y = (int) getY() - Game.C.getY() + yOffset;
        Alpha.draw(getSprite(), x, y, t, g);
    }
}