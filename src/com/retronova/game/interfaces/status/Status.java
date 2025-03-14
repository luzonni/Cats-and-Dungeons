package com.retronova.game.interfaces.status;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Status implements Activity {

    private final Player player;

    private BufferedImage status;

    public Status(Player player) {
        this.player = player;
        this.status = new SpriteSheet("ui","status", Configs.HudScale()).getSHEET();
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
