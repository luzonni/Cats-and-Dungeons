package com.retronova.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

public class Debugging {

    public static boolean running = true;

    private static SystemInfo systemInfo;

    public static boolean showEntityHitBox = false;
    public static boolean showParticleHitBox = false;
    public static boolean showTileBox = false;
    public static boolean showMouseSets = false;

    private static Point position;
    private static Font font;
    private static Color background;

    private static String[] infos;

    public static void init() {
        font = FontG.font(FontG.Septem, Configs.UiScale()*8);
        position = new Point(Configs.Margin(), Configs.Margin());
        background = new Color(100, 100, 100, 180);
        systemInfo = new SystemInfo();
        infos = new String[13];
        for(int i = 0; i < infos.length; i++) {
            infos[i] = "INFO";
        }
        infos[0] = Engine.GameTag + " - " + Engine.VERSION;
        infos[10] = "Processador: " + systemInfo.getHardware().getProcessor().getProcessorIdentifier().getName();
        infos[9] = "Arquitetura: " + System.getProperty("os.arch");
        long memoria = systemInfo.getHardware().getMemory().getTotal();
        infos[12] = "GraphicsCard" + systemInfo.getHardware().getGraphicsCards().get(0).getName();
        infos[11] = "Memoria: " + formatarBytes(memoria);
    }

    public static void tick() {
        if(KeyBoard.KeyPressed("F3")) {
            Debugging.toggleRunning();
        }
        if(KeyBoard.KeyPressed("F4")) {
            Debugging.showEntityHitBox = !Debugging.showEntityHitBox;
        }
        if(KeyBoard.KeyPressed("F5")) {
            Debugging.showTileBox = !Debugging.showTileBox;
        }
        if(KeyBoard.KeyPressed("F6")) {
            Debugging.showParticleHitBox = !Debugging.showParticleHitBox;
        }
        if(KeyBoard.KeyPressed("F7")) {
            Debugging.showMouseSets = !Debugging.showMouseSets;
        }


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
    }

    private static String formatarBytes(long bytes) {
        final long GB = 1024 * 1024 * 1024;
        final long MB = 1024 * 1024;

        if (bytes >= GB)
            return String.format("%.2f GB", bytes / (double) GB);
        else
            return String.format("%.2f MB", bytes / (double) MB);
    }

    public static void toggleRunning() {
        running = !running;
    }

    public static void render(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

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

    public static void mouse(Graphics2D g) {
        int size = Configs.GameScale()*2;
        int size2 = Configs.GameScale()*6;
        int x = Game.getCam().getX() + Mouse.getX();
        int y = Game.getCam().getY() + Mouse.getY();
        g.setStroke(new BasicStroke(Configs.GameScale()));
        g.setColor(Color.MAGENTA);
        g.fillOval(x - size/2, y - size/2, size, size);
        g.setColor(Color.WHITE);
        g.drawOval(x - size2/2, y - size2/2, size2, size2);
    }

}
