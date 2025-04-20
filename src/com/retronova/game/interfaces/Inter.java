package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inter implements Activity {


    private Rectangle[] tabs;
    private final Map<String, Activity> inter;
    private final List<String> temp;
    private String index;

    public Inter() {
        this.inter = new HashMap<>();
        this.temp = new ArrayList<>();
        this.index = "";
    }

    public void open() {
        if(this.inter.isEmpty()) {
            return;
        }
        if(index.isBlank()) {
            this.index = keys()[0];
        }
        Engine.pause(this);
    }

    public void open(String index) {
        this.index = index;
        Engine.pause(this);
    }

    public void close() {
        Engine.pause(null);
        closeTemps();
        for(String key : keys()) {
            this.inter.get(key).dispose();
        }
    }

    private void closeTemps() {
        for(int i = 0; i < this.temp.size(); i++) {
            String tempKey = this.temp.get(i);
            this.remove(tempKey);
            this.temp.remove(this.temp.get(i));
            this.index = "";
        }
    }

    private String[] keys() {
        return inter.keySet().toArray(new String[0]);
    }

    public void put(String interName, Activity inter, boolean temp) {
        if(!this.inter.containsKey(interName)) {
            this.inter.put(interName, inter);
            this.tabs = tabs();
            if(temp && !this.temp.contains(interName)) {
                this.temp.add(interName);
            }
        }
    }

    public void remove(String interName) {
        if(index.equals(interName)) {
            this.index = keys()[0];
        }
        this.inter.get(interName).dispose();
        this.inter.remove(interName);
        this.tabs = tabs();
        refreshPositions();
    }

    private void refreshPositions() {
        String[] keys = keys();
        Rectangle rec = this.tabs[0];
        int width = rec.width * keys.length;
        int x = Engine.window.getWidth()/2 - width/2;
        int y = Configs.Margin();
        for(int i = 0; i < keys.length; i++) {
            tabs[i].setLocation(x + (rec.width+Configs.HudScale()*2)*i, y);
        }
    }

    private Rectangle[] tabs() {
        String[] keys = keys();
        int scale = Configs.HudScale();
        Rectangle rec = new Rectangle(48*scale, 12*scale);
        Rectangle[] tabs = new Rectangle[keys.length];
        for(int i = 0; i < keys.length; i++) {
            tabs[i] = new Rectangle(rec.width, rec.height);
        }
        return tabs;
    }
    
    @Override
    public void tick() {
        if(KeyBoard.KeyPressed("E") || KeyBoard.KeyPressed("Escape") ) {
            close();
        }
        refreshPositions();
        for(int i = 0; i < tabs.length; i++) {
            Rectangle rec = tabs[i];
            if(Mouse.clickOn(Mouse_Button.LEFT, rec)) {
                this.index = keys()[i];
            }
        }
        if(inter.containsKey(index))
            inter.get(index).tick();
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(new Color(0,0,0, 80));
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
        if(inter.containsKey(index))
            inter.get(index).render(g);
        renderTabs(g);
    }

    private void renderTabs(Graphics2D g) {
        Font font = FontG.font(FontG.Game,Configs.HudScale()*6);
        g.setFont(font);
        g.setStroke(new BasicStroke(Configs.HudScale()*2));
        for(int i = 0; i < tabs.length; i++) {
            Rectangle rec = tabs[i];
            String name = keys()[i];
            Color border = index.equals(name) ? new Color(0xe17564) : new Color(0x09122c);
            g.setColor(border);
            g.drawRect(rec.x, rec.y, rec.width, rec.height);
            g.setColor(new Color(0x872341));
            g.fillRect(rec.x, rec.y, rec.width, rec.height);
            int wf = FontG.getWidth(name, font);
            int hf = FontG.getHeight(name, font);
            g.setColor(new Color(0x09122c));
            g.drawString(name, rec.x + rec.width/2 - wf/2 + Configs.HudScale(), rec.y + rec.height/2 + hf/2 + Configs.HudScale());
            Color c = index.equals(name) ? Color.white : new Color(0xe17564);
            g.setColor(c);
            g.drawString(name, rec.x + rec.width/2 - wf/2, rec.y + rec.height/2 + hf/2);
        }
    }

    @Override
    public void dispose() {
        this.inter.clear();
    }

}
