package com.retronova.engine;

import com.retronova.game.Game;

import java.awt.*;
import java.awt.image.BufferStrategy;

public class Engine implements Runnable {

    private static String GameTag = "Roguelike";

    private Thread thread;
    private boolean isRunning;
    public static final double HZ = 60;
    public static final double T = 1_000_000_000.0;
    public static int FRAMES;
    public static int HERTZ;

    public Activity ACTIVITY;
    public boolean ACTIVITY_RUNNING;

    public static final String resPath = "/com/retronova/res/";

    public static final int SCALE = 3;
    public static final int[][] resolutions = {{1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};
    public static double MaxFrames = 60;
    public static int index_res = 0;

    public static Window window;
    static BufferStrategy BUFFER;

    public Engine() {
        Engine.window = new Window(GameTag);
        this.ACTIVITY = new Game();
        this.ACTIVITY_RUNNING = true;
        start();
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

    public synchronized void start() {
        this.thread = new Thread(this, "Engine");
        this.isRunning = true;
        thread.start();
    }

    public synchronized void stop() {
        thread.interrupt();
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
