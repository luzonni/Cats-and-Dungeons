package com.retronova.game.interfaces.status;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Status implements Activity {

    private final Player player;
    private final BufferedImage status;

    private final Point pointStatus;
    private final Point pointPlayer;

    public Status(Player player) {
        this.player = player;
        this.status = new SpriteSheet("ui","status", Configs.HudScale()).getSHEET();
        this.pointPlayer = new Point();
        this.pointStatus = new Point();
        refreshPositions();
    }

    private void refreshPositions() {
        int x = Engine.window.getWidth()/2 - status.getWidth()/2;
        int y = Engine.window.getHeight()/2 - status.getHeight()/2;
        this.pointStatus.setLocation(x, y);
        this.pointPlayer.setLocation(x + 6*Configs.HudScale(), y + 9*Configs.HudScale());
    }

    @Override
    public void tick() {
        refreshPositions();
    }

    @Override
    public void render(Graphics2D g) {
        g.drawImage(status, pointStatus.x, pointStatus.y, null);
        g.drawImage(player.getSprite(0), pointPlayer.x, pointPlayer.y, 16 * Configs.HudScale(), 16 * Configs.HudScale(), Engine.window);
    }

    @Override
    public void dispose() {

    }
}
