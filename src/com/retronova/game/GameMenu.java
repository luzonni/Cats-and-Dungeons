package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.graphics.FontG;
import com.retronova.inputs.keyboard.KeyBoard;

import java.awt.*;

public class GameMenu implements Activity {

    @Override
    public void tick() {
        if(KeyBoard.KeyPressed("ESCAPE")) {
            Engine.pause(null);
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(new Color(0,0,0,120));
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
        int middleX = Engine.window.getWidth()/2;
        int middleY = Engine.window.getHeight()/2;
        Font font = FontG.font(Engine.SCALE * 16);
        String value = "Pause";
        g.setFont(font);
        int wf = FontG.getWidth(value, font);
        int hf = FontG.getHeight(value, font);
        g.setColor(Color.white);
        g.drawString(value, middleX - wf/2, middleY - hf/2);
    }

    @Override
    public void dispose() {

    }
}
