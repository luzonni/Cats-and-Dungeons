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
import java.util.List;

public class Loading implements Activity, Runnable {

    private final Thread thread;

    private final List<Activity> stack;
    private final Activity next;
    private final ActionBack action;
    private volatile boolean finish;
    private boolean start;

    private final BufferedImage spriteLoad;
    private double rotating;

    public Loading(List<Activity> stack, Activity next, ActionBack action) {
        this.thread = new Thread(this);
        this.stack = stack;
        this.next = next;
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
        if(finish) {
            stack.remove(this);
            stack.add(next);
        }
        rotating += Math.toRadians(2);
    }

    @Override
    public void render(Graphics2D g) {

        int x = Engine.window.getWidth()/2 - spriteLoad.getWidth()/2;
        int y = Engine.window.getHeight()/2 - spriteLoad.getHeight()/2;
        Rotate.draw(spriteLoad, x, y, rotating, null, g);

        int padding = Configs.Margin();
        Font font = FontG.font(FontG.Game,Configs.UiScale() * 12);
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
