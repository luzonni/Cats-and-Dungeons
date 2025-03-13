package com.retronova.menus;

import com.retronova.engine.ActionBack;
import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Loading implements Activity, Runnable {

    private final Thread thread;

    private final Activity activity;
    private final ActionBack action;
    private volatile boolean finish;
    private boolean start;

    private BufferedImage spriteLoad;
    private double rotating;

    public Loading(Activity activity, ActionBack action) {
        this.thread = new Thread(this);
        this.activity = activity;
        this.action = action;
        finish = false;
        this.start = false;
        this.spriteLoad = new SpriteSheet("icons", "loading", Configs.UiScale() *2).getSHEET();
    }

    @Override
    public synchronized void tick() {
        if(!start) {
            start = true;
            this.thread.start();
        }
        if(finish)
            Engine.setActivity(activity);
        rotating += Math.toRadians(2);
    }

    @Override
    public void render(Graphics2D g) {

        int x = Engine.window.getWidth()/2 - spriteLoad.getWidth()/2;
        int y = Engine.window.getHeight()/2 - spriteLoad.getHeight()/2;
        Rotate.draw(spriteLoad, x, y, rotating, null, g);

        int padding = Configs.Margin();
        Font font = FontG.font(Configs.UiScale() * 12);
        String value = "Loading...";
        g.setColor(Color.white);
        g.setFont(font);
        int wF = FontG.getWidth(value, font);
        g.drawString(value, Engine.window.getWidth() - wF - padding, Engine.window.getHeight() - padding);
    }

    @Override
    public void dispose() {

    }

    @Override
    public void run() {
        action.action();
        finish = true;
    }
}
