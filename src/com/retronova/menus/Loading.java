package com.retronova.menus;

import com.retronova.engine.ActionBack;
import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.graphics.FontG;

import java.awt.*;

public class Loading implements Activity {

    private final Activity activity;
    private final ActionBack action;
    private boolean finish;

    public Loading(Activity activity, ActionBack action) {
        this.activity = activity;
        this.action = action;
    }

    @Override
    public void tick() {
        synchronized (this) {
            new Thread(() -> {
                action.action();
                finish = true;
            }).start();
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
}
