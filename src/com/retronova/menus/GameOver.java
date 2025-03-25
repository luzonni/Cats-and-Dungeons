package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class GameOver implements Activity {

    private final Player player;

    public GameOver(Player player) {
        this.player = player;
    }

    @Override
    public void tick() {
        System.out.println("Game Over!");
    }

    @Override
    public void render(Graphics2D g) {

    }

    @Override
    public void dispose() {

    }
}
