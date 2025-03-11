package com.retronova.menus;

import com.retronova.engine.ActionBack;
import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;

import java.awt.*;

public class Loading implements Activity, Runnable {

    private final Thread thread;

    private final Activity activity;
    private final ActionBack action;
    private volatile boolean finish;
    private boolean start;

    public Loading(Activity activity, ActionBack action) {
        this.thread = new Thread(this);
        this.activity = activity;
        this.action = action;
        finish = false;
        this.start = false;
    }

    @Override
    public synchronized void tick() {
        if(!start) {
            start = true;
            this.thread.start();
        }
        if(finish)
            Engine.setActivity(activity);
    }

    @Override
    public void render(Graphics2D g) {
        Font font = FontG.font(Configs.UISCALE * 12);
        String value = "Loading...";
        g.setColor(Color.white);
        g.setFont(font);
        int wF = FontG.getWidth(value, font);
        g.drawString(value, Engine.window.getWidth() - wF, Engine.window.getHeight());
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
