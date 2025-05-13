package com.retronova.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;

public class Debugging {

    public static boolean running = true;

    public static boolean showEntityHitBox = false;
    public static boolean showParticleHitBox = false;
    public static boolean showTileBox = false;

    private static Point position;
    private static Font font;
    private static Color background;

    private static String[] infos;

    public static void init() {
        font = FontG.font(FontG.Septem, Configs.UiScale()*8);
        position = new Point(Configs.Margin(), Configs.Margin());
        background = new Color(255, 255, 255, 100);
        infos = new String[10];
        for(int i = 0; i < infos.length; i++) {
            infos[i] = "INFO";
        }
        infos[0] = Engine.GameTag + " - " + Engine.VERSION;
    }

    public static void toggleRunning() {
        running = !running;
    }

    public static void render(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        infos[1] = "Ticks: " + String.valueOf(Engine.HERTZ);
        infos[2] = "FPS: " + String.valueOf(Engine.FRAMES);
        infos[3] = "Screen Size: [" + Engine.window.getWidth() + " / " + Engine.window.getHeight() + "]";
        String mouseP = "X: " + Mouse.getX() + " Y: " + Mouse.getY();
        infos[4] = "Mouse on screen position: [" + mouseP + " ]";
        String mouseMapP = "X: " + (Mouse.getX() + Game.getCam().getX()) + " Y: " + (Mouse.getY() + Game.getCam().getY());
        infos[5] = "Mouse on map position: [" + mouseMapP + "]";
        infos[6] = "OpenGL: " + System.getProperty("sun.java2d.opengl");
        int entitiesSize = Game.getMap().getEntities().size();
        infos[7] = "Amount of entities: " + entitiesSize;
        int particlesSize = Game.getMap().getParticles().size();
        infos[8] = "Amount of particles: " + particlesSize;

        for(int i = 0; i < infos.length; i++) {
            String info = infos[i];
            int x = position.x;
            int y = position.y + (int)(FontG.getHeight(info, font)*1.5)*i;
            renderInfo(info, x, y, g);
        }
        g.dispose();
    }

    private static void renderInfo(String text, int x, int y, Graphics2D g) {
        int padding = Configs.UiScale();
        g.setColor(background);
        int wF = FontG.getWidth(text, font);
        int hF = FontG.getHeight(text, font);
        g.fillRect(x, y, wF + padding*2, hF + padding*2);
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawString(text, x + padding*2, y + hF + padding);
        g.setColor(Color.WHITE);
        g.drawString(text, x + padding, y + hF);
    }

}
