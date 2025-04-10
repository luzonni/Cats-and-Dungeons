package com.retronova.engine;

import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.graphics.FontG;
import com.retronova.menus.Loading;
import com.retronova.menus.Menu;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Engine implements Runnable {

    private static String GameTag = "Cats & Dungeons";


    private static Thread thread;
    private static boolean isRunning;
    public static final double HZ = 60;
    public static final double T = 1_000_000_000.0;
    public static int FRAMES;
    public static int HERTZ;

    private static Activity OverView;
    private static List<Activity> stackActivities;
    private static boolean ACTIVITY_RUNNING;

    public static final String resPath = "/com/retronova/res/";

    public static final int[][] resolutions = {{1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};

    public static Window window;
    private static BufferStrategy BUFFER;

    public static Random RAND = new Random();

    public Engine() {
        stackActivities = new ArrayList<>();
        Configs.init();
        Configs.load();
        FontG.addFont("game", "septem");
        Sound.load();
        Engine.window = new Window(GameTag);
        heapActivity(new Menu());
        start();
    }

    //Sempre usar essa função para mudar de Activity! Nunca usar a variável direto.
    public static void heapActivity(Activity activity) {
        ACTIVITY_RUNNING = true;
        Engine.stackActivities.add(activity);
    }

    public static void heapActivity(Activity activity, ActionBack action) {
        ACTIVITY_RUNNING = true;
        Engine.stackActivities.add(new Loading(stackActivities, activity, action));
    }

    public static void backActivity() {
        Activity ac = Engine.getACTIVITY();
        if(ac != null) {
            ac.dispose();
        }
        Engine.stackActivities.removeLast();
    }

    public static void backActivity(int amount) {
        Engine.pause(null);
        for(int i = 0; i < amount; i++) {
            Activity ac = Engine.getACTIVITY();
            if(ac != null) {
                ac.dispose();
            }
            Engine.stackActivities.removeLast();
        }
    }

    /**
     * Chame essa função com uma UI para pausar a activity atual e sobrepor outra activity
     * Pausar a activity atual não impede a renderização, apenas os ticks...
     * Caso insira null como parametro, a activity é despausada.
     *
     * @param ui é a activity que será exibida por cima da activity atual.
     */
    public static void pause(Activity ui) { //quando essa função é chamada, o jogo é pausado.
        if(ui != null) {
            ACTIVITY_RUNNING = false;
            Engine.OverView = ui;
        }else {
            ACTIVITY_RUNNING = true;
            Engine.OverView = null;
        }
    }

    public static Activity getACTIVITY() {
        return Engine.stackActivities.getLast();
    }

    public static void CLOSE() {
        Engine.getACTIVITY().dispose();
        Engine.isRunning = false;
    }

    public static int[] getResolution() {
        return Engine.resolutions[Configs.getIndexResolution()];
    }

    private Graphics2D getGraphics() {
        if(Engine.BUFFER == null) {
            window.createBufferStrategy(3);
            Engine.BUFFER = window.getBufferStrategy();
            return null;
        }
        Graphics2D graphics = (Graphics2D) BUFFER.getDrawGraphics();
        graphics.setColor(Color.black);
        graphics.fillRect(0, 0, window.getWidth(), window.getHeight());
        if(Configs.isNeatGraphics()) {
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        }else {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            graphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        }
        return graphics;
    }

    private synchronized void start() {
        thread = new Thread(this, "Engine");
        isRunning = true;
        thread.start();
    }

    private synchronized void stop() {
        Sound.dispose();
        window.getFrame().dispose();
    }

    private void render(Graphics2D graphics) {
        graphics.dispose();
        BUFFER.show();
    }

    @Override
    public void run() {
        System.out.println("Inicializando Thread MAX FRAMES = " + Configs.MaxFrames());
        long lastTimeHZ = System.nanoTime();
        double amountOfHz = Engine.HZ;
        double ns_HZ = Engine.T / amountOfHz;
        double delta_HZ = 0;
        long lastTimeFPS = System.nanoTime();
        double amountOfFPS = Configs.MaxFrames();
        double ns_FPS = Engine.T / amountOfFPS;
        double delta_FPS = 0;
        int Hz = 0;
        int frames = 0;
        double timer = System.currentTimeMillis();
        window.requestFocus();
        while (isRunning) {
            try {
                long nowHZ = System.nanoTime();
                delta_HZ += (nowHZ - lastTimeHZ) / ns_HZ;
                lastTimeHZ = nowHZ;
                if (delta_HZ >= 1) {
                    if (KeyBoard.KeyPressed("F11") && window != null) {
                        Configs.setFullscreen(!Configs.Fullscreen());
                        window.resetWindow();
                    }
                    if (ACTIVITY_RUNNING && !stackActivities.isEmpty()) {
                        getACTIVITY().tick();
                    }
                    if (OverView != null) {
                        OverView.tick();
                    }
                    Hz++;
                    delta_HZ--;
                }
                long nowFPS = System.nanoTime();
                delta_FPS += (nowFPS - lastTimeFPS) / ns_FPS;
                lastTimeFPS = nowFPS;
                if (delta_FPS >= 1) {
                    Graphics2D g = getGraphics();
                    if(g == null)
                        continue;
                    if (!stackActivities.isEmpty()) {
                        getACTIVITY().render(g);
                    }
                    if (OverView != null) {
                        OverView.render(g);
                    }
                    render(g);
                    frames++;
                    delta_FPS--;
                }
                //Show fps
                if (System.currentTimeMillis() - timer >= 1000) {
                    Engine.window.getFrame().setTitle(Engine.GameTag + " - Hz: " + Hz + " / Frames: " + frames);
                    Engine.FRAMES = frames;
                    frames = 0;
                    Engine.HERTZ = Hz;
                    Hz = 0;
                    timer += 1000;
                }
                Thread.sleep(1);
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.err.println("==============================================================");
                e.printStackTrace();
                System.exit(1);
            }
        }
        stop();
        System.out.println("Exit");
    }

}



