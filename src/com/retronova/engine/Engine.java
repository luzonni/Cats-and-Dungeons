package com.retronova.engine;

import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Player;
import com.retronova.graphics.FontG;
import com.retronova.menus.Menu;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.util.Random;

public class Engine implements Runnable {

    private static String GameTag = "Roguelike";

    private static Thread thread;
    private static boolean isRunning;
    public static final double HZ = 60;
    public static final double T = 1_000_000_000.0;
    public static int FRAMES;
    public static int HERTZ;

    private static Activity ACTIVITY;
    private static boolean ACTIVITY_RUNNING;

    public static final String resPath = "/com/retronova/res/";

    public static final int SCALE = 4;
    public static final int UISCALE = 3;
    public static final int[][] resolutions = {{1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};
    public static double MaxFrames = 60;
    public static int index_res = 0;

    public static Window window;
    static BufferStrategy BUFFER;

    public static Random RAND = new Random();

    public Engine() {
        FontG.addFont("game");
        Engine.window = new Window(GameTag);
        setActivity(new Menu());
        start();
    }

    //Sempre usar essa função para mudar de Activity! Nunca usar a variável direto.
    public static boolean setActivity(Activity activity) {
        ACTIVITY_RUNNING = true;
        try {
            ACTIVITY = activity;
            return true;
        }catch (Exception e) {
            return false;
        }
    }

    public static void pause() { //quando essa função é chamada, o jogo é pausado.
        ACTIVITY_RUNNING = !ACTIVITY_RUNNING;
    }

    public static Activity getACTIVITY() {
        return Engine.ACTIVITY;
    }

    public static void CLOSE() {
        Engine.ACTIVITY.dispose();
        Engine.isRunning = false;
    }

    public static int[] getResolution() {
        return Engine.resolutions[index_res];
    }

    private Graphics2D getGraphics() {
        Graphics2D graphics = (Graphics2D) BUFFER.getDrawGraphics();
        graphics.setColor(Color.black);
        graphics.fillRect(0, 0, window.getWidth(), window.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        return graphics;
    }

    private synchronized void start() {
        thread = new Thread(this, "Engine");
        isRunning = true;
        thread.start();
    }

    private synchronized void stop() {
        window.getFrame().dispose();
    }

    private void render(Graphics2D graphics) {
        graphics.dispose();
        BUFFER.show();
    }

    @Override
    public void run() {
        //System values
        long lastTimeHZ = System.nanoTime();
        double amountOfHz = Engine.HZ;
        double ns_HZ = Engine.T / amountOfHz;
        double delta_HZ = 0;

        long lastTimeFPS = System.nanoTime();
        double amountOfFPS = Engine.MaxFrames;
        double ns_FPS = Engine.T / amountOfFPS;
        double delta_FPS = 0;

        //To Show
        int Hz = 0;
        int frames = 0;
        double timer = System.currentTimeMillis();
        window.requestFocus();
        while(isRunning) {
            try {
                long nowHZ = System.nanoTime();
                delta_HZ += (nowHZ - lastTimeHZ) / ns_HZ;
                lastTimeHZ = nowHZ;
                if(delta_HZ >= 1) {
                    if(ACTIVITY_RUNNING && ACTIVITY != null) {
                        ACTIVITY.tick();
                    }
                    Hz++;
                    delta_HZ--;
                }

                long nowFPS = System.nanoTime();
                delta_FPS += (nowFPS - lastTimeFPS) / ns_FPS;
                lastTimeFPS = nowFPS;
                if(delta_FPS >= 1) {
                    Graphics2D g = getGraphics();
                    if(ACTIVITY_RUNNING && ACTIVITY != null) {
                        ACTIVITY.render(g);
                    }
                    render(g);
                    frames++;
                    delta_FPS--;
                }

                //Show fps
                if(System.currentTimeMillis() - timer >= 1000){
                    Engine.window.getFrame().setTitle(Engine.GameTag+" - Hz: " + Hz + " / Frames: " + frames);
                    Engine.FRAMES = frames;
                    frames = 0;
                    Engine.HERTZ = Hz;
                    Hz = 0;
                    timer += 1000;
                }
                Thread.sleep(1); //Otimização de CPU ( limita a renderização ilimitada )
            }catch(Exception e) {
                System.out.println("ERROR!");
                e.printStackTrace();
                System.exit(1);
            }
        }
        stop();
    }
}
