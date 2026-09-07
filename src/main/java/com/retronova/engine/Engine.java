package com.retronova.engine;

import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.menus.Loading;
import com.retronova.menus.Menu;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Engine implements Runnable {

    public static String GameTag = "Cats & Dungeons";
    public static String VERSION = "ALPHA 0.1.3";


    private static Thread thread;
    private static boolean isRunning;
    public static final double HZ = 60;
    public static final double T = 1_000_000_000.0;
    /** Teto de ticks encadeados numa volta do laço, para recuperar atraso sem travar. */
    private static final int MAX_CATCHUP = 5;
    public static int FRAMES;
    public static int HERTZ;

    private static Activity OverView;
    private static List<Activity> stackActivities;
    private static boolean ACTIVITY_RUNNING;

    public static final String resPath = "/com/retronova/resources/";

    public static final int[][] resolutions = {{1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};

    public static Window window;
    private static BufferStrategy BUFFER;

    public static Random RAND = new Random();

    public Engine() {
        stackActivities = new ArrayList<>();
        Configs.init();
        Configs.load();
        FontHandler.addFont("game", "septem");
        Sound.load();
        //O Sheeter monta "<path>/<módulo>/<nome>.png" por conta própria. Passar
        //resPath com a barra final gera "//" no meio do caminho: num classpath
        //explodido a URL é normalizada e funciona, mas dentro de um .jar os nomes
        //das entradas são comparados literalmente e o recurso não é encontrado.
        SpriteSheet.load(Engine.resPath.substring(0, Engine.resPath.length() - 1), Configs.GameScale());
        Engine.window = new Window(GameTag);
        heapActivity(new Menu());
        start();
        Debugging.init();
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

    /**
     * Descarta o BufferStrategy atual.
     *
     * Precisa ser chamado sempre que a janela for recriada: o strategy pertence
     * ao canvas dentro do JFrame antigo e morre junto com ele. Continuar usando
     * o strategy morto fazia o jogo desenhar no vazio — a tela branca depois de
     * um F11 ou de aplicar uma resolução nova.
     */
    static void invalidateBuffer() {
        Engine.BUFFER = null;
    }

    private Graphics2D getGraphics() {
        if(Engine.BUFFER == null) {
            try {
                window.createBufferStrategy(3);
                Engine.BUFFER = window.getBufferStrategy();
            } catch (IllegalStateException aindaNaoExibivel) {
                //A janela ainda não está pronta; tenta de novo no próximo quadro.
                Engine.BUFFER = null;
            }
            return null;
        }
        Graphics2D graphics;
        try {
            graphics = (Graphics2D) BUFFER.getDrawGraphics();
        } catch (IllegalStateException superficiePerdida) {
            //Buffer inválido (monitor dormiu, driver resetou, janela recriada).
            //Descarta para reconstruir na volta seguinte em vez de desenhar no vazio.
            Engine.BUFFER = null;
            return null;
        }
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

    private void tick() {
        window.tick();
        Debugging.tick();
        Debugging.setInfo("Ticks", String.valueOf(Engine.HERTZ));
        Debugging.setInfo("Frames", String.valueOf(Engine.FRAMES));
        Debugging.setInfo("Screen Size", "[" + Engine.window.getWidth() + " / " + Engine.window.getHeight() + "]");
    }

    private void render(Graphics2D graphics) {
        //A versao NAO e carimbada aqui. Ela nao muda durante a partida, entao
        //ficava ocupando a tela o jogo inteiro sem informar nada; o lugar dela e
        //a tela inicial, como faz a maioria dos jogos, e o overlay de depuracao
        //(F3) para quem precisa dela com o jogo rodando.
        if(Debugging.running)
            Debugging.render(graphics);
        graphics.dispose();

        //A API de BufferStrategy exige verificar a perda de conteúdo: com o
        //pipeline OpenGL ligado, o back buffer é invalidado quando o monitor
        //dorme, a sessão é bloqueada ou o driver reinicia. Sem esta checagem o
        //jogo seguia "desenhando" numa superfície morta e a janela ficava
        //branca — foi o que aconteceu com o jogo deixado ocioso.
        try {
            BUFFER.show();
            if (BUFFER.contentsLost()) {
                invalidateBuffer();
            }
        } catch (IllegalStateException superficiePerdida) {
            invalidateBuffer();
        }
    }

    @Override
    public void run() {
        long lastTimeHZ = System.nanoTime();
        double amountOfHz = Engine.HZ;
        double ns_HZ = Engine.T / amountOfHz;
        double delta_HZ = 0;
        long lastTimeFPS = System.nanoTime();
        //Relido a cada volta: antes era lido uma única vez, então trocar o
        //limite de quadros nas opções não surtia efeito até reiniciar o jogo.
        double ns_FPS = Engine.T / Math.max(1, Configs.MaxFrames());
        double delta_FPS = 0;
        int Hz = 0;
        int frames = 0;
        double timer = System.currentTimeMillis();
        window.requestFocus();
        while (isRunning) {
            try {
                if (KeyBoard.KeyPressed("F11") && window != null) {
                    Configs.setFullscreen(!Configs.Fullscreen());
                    window.resetWindow();
                }

                long nowHZ = System.nanoTime();
                delta_HZ += (nowHZ - lastTimeHZ) / ns_HZ;
                lastTimeHZ = nowHZ;

                //O tick tem prioridade sobre o render. Antes rodava no máximo um
                //tick por volta do laço; quando o render atrasava, a simulação
                //ficava para trás e o jogo inteiro andava em câmera lenta.
                //Agora recupera o atraso executando ticks em sequência.
                int ticksExecutados = 0;
                while (delta_HZ >= 1 && ticksExecutados < MAX_CATCHUP) {
                    this.tick();
                    if (ACTIVITY_RUNNING && !stackActivities.isEmpty()) {
                        getACTIVITY().tick();
                    }
                    if (OverView != null) {
                        OverView.tick();
                    }
                    Hz++;
                    delta_HZ--;
                    ticksExecutados++;
                }
                //Atraso grande demais para recuperar: desiste dele em vez de
                //acumular dívida e entrar em espiral.
                if (delta_HZ > MAX_CATCHUP) {
                    delta_HZ = 0;
                }

                long nowFPS = System.nanoTime();
                ns_FPS = Engine.T / Math.max(1, Configs.MaxFrames());
                delta_FPS += (nowFPS - lastTimeFPS) / ns_FPS;
                lastTimeFPS = nowFPS;
                boolean desenhou = false;
                if (delta_FPS >= 1) {
                    Graphics2D g = getGraphics();
                    if (g != null) {
                        if (!stackActivities.isEmpty()) {
                            getACTIVITY().render(g);
                        }
                        if (OverView != null) {
                            OverView.render(g);
                        }
                        render(g);
                        frames++;
                        desenhou = true;
                    }
                    //Quadro perdido é quadro perdido: zera em vez de decrementar,
                    //senão tentaria desenhar o atraso e afundaria mais ainda.
                    delta_FPS = 0;
                }

                //Show fps
                if (System.currentTimeMillis() - timer >= 1000) {
                    Engine.FRAMES = frames;
                    frames = 0;
                    Engine.HERTZ = Hz;
                    Hz = 0;
                    timer += 1000;
                }
                //Só dorme quando não havia nada a fazer; dormir estando atrasado
                //é exatamente o que impedia o laço de recuperar.
                if (ticksExecutados == 0 && !desenhou) {
                    Thread.sleep(1);
                }
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



