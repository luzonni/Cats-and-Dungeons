package com.retronova.game.interfaces.status;

import com.retronova.engine.Activity;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class Status implements Activity {

    private final Player player;

    public Status(Player player) {
        this.player = player;
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics2D g) {

    }

    @Override
    public void dispose() {

    }
}
