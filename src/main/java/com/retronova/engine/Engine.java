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
    /**
     * Fracao, de 0 a 1, do caminho ate o proximo tick no instante do quadro.
     *
     * A logica anda a 60 Hz fixos; o quadro pode sair a 144 ou 165. Sem isso, um
     * monitor rapido mostra o mesmo estado repetido entre dois ticks e a animacao
     * anda aos trancos. Quem anima pode somar alpha() ao seu contador no render.
     */
    private static float alpha;

    public static float alpha() {
        return alpha;
    }

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
        // BANCADA (-Dcd.bench=true): entra direto numa arena com o Finn, para medir
        // desempenho sem navegar menus. Espaco 0 nao e um espaco de save valido —
        // nada e gravado nem apagado; as corridas do jogador ficam intactas.
        if (Boolean.getBoolean("cd.bench")) {
            heapActivity(new com.retronova.game.Game(2,
                    new com.retronova.game.map.arena.Arena(0, 4), 0));
            // Telas por cima, para conferir o desenho delas sem jogar ate elas.
            // Abrem depois de um segundo de partida: a chegada do gato fecha
            // qualquer pausa aberta antes dela.
            if (Boolean.getBoolean("cd.roleta")) {
                com.retronova.game.Game.depoisDeUmSegundo =
                        () -> pause(new com.retronova.game.map.arena.Roleta());
            } else if (Boolean.getBoolean("cd.nivel")) {
                com.retronova.game.Game.depoisDeUmSegundo =
                        () -> pause(new com.retronova.game.map.arena.SubirDeNivel());
            }
            // -Dcd.armas=RAIO,TREMOR,... da essas armas extras no nivel 3, para ver
            // o efeito delas sem depender do sorteio do caca-niquel.
            String armas = System.getProperty("cd.armas");
            if (armas != null) {
                Runnable antes = com.retronova.game.Game.depoisDeUmSegundo;
                com.retronova.game.Game.depoisDeUmSegundo = () -> {
                    for (String nome : armas.split(",")) {
                        for (int i = 0; i < 3; i++) {
                            com.retronova.game.Game.getPlayer().getMelhorias().subirArma(
                                    com.retronova.game.items.ArmaExtra.valueOf(nome.trim()));
                        }
                    }
                    if (antes != null) {
                        antes.run();
                    }
                };
            }
        }
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

    /** Ha alguma tela por cima do jogo (pausa, cartas, roleta)? */
    public static boolean pausado() {
        return Engine.OverView != null;
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

    /** O buffer atual espera o monitor a cada show()? */
    private static boolean vsync;

    /**
     * Quantos quadros por segundo mirar. Com a opcao VSync, a taxa REAL do monitor
     * — mirar 165 num monitor de 144 (ou o contrario) e garantir um quadro
     * repetido ou perdido a cada poucos. Sem ela, o limite escolhido nas opcoes.
     */
    private static int alvoDeQuadros() {
        return Configs.VSync() ? taxaDoMonitor() : Configs.MaxFrames();
    }

    private static int taxaCacheada;

    static int taxaDoMonitor() {
        if (taxaCacheada == 0) {
            int taxa = DisplayMode.REFRESH_RATE_UNKNOWN;
            try {
                GraphicsConfiguration gc = window != null ? window.getGraphicsConfiguration() : null;
                GraphicsDevice dev = gc != null ? gc.getDevice()
                        : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                taxa = dev.getDisplayMode().getRefreshRate();
            } catch (Exception ignorado) {
            }
            taxaCacheada = taxa > 0 ? taxa : Math.max(60, Configs.MaxFrames());
            System.out.println("Monitor refresh: " + taxaCacheada + " Hz");
        }
        return taxaCacheada;
    }

    /**
     * Cria o buffer com VSYNC, para cada quadro sair no instante em que o monitor
     * atualiza.
     *
     * MEDIDO ANTES: 165 quadros por segundo cravados, desenho de 0,7 ms — e ainda
     * assim a sensacao era de jogo travado. O problema nao era quantos quadros, era
     * QUANDO: sem vsync o intervalo entre quadros variava de 6 a 9 ms, e num
     * monitor de 165 Hz isso vira quadro repetido aqui, quadro pulado ali. O olho
     * le essa irregularidade como engasgo, mesmo com o contador marcando 165.
     *
     * A API mora num pacote interno (sun.java2d.pipe.hw) e so funciona no pipeline
     * OpenGL. Por isso reflexao, e por isso qualquer falha cai no buffer antigo em
     * silencio: sem vsync o jogo continua jogavel, so menos liso.
     */
    private static boolean criarComVSync() {
        try {
            Class<?> ext = Class.forName("sun.java2d.pipe.hw.ExtendedBufferCapabilities");
            Class<?> tipo = Class.forName("sun.java2d.pipe.hw.ExtendedBufferCapabilities$VSyncType");
            Object ligado = null;
            for (Object c : tipo.getEnumConstants()) {
                if (c.toString().equals("VSYNC_ON")) {
                    ligado = c;
                }
            }
            BufferCapabilities base = new BufferCapabilities(
                    new ImageCapabilities(true), new ImageCapabilities(true),
                    BufferCapabilities.FlipContents.UNDEFINED);
            BufferCapabilities caps = (BufferCapabilities) ext
                    .getConstructor(BufferCapabilities.class, tipo).newInstance(base, ligado);
            window.createBufferStrategy(2, caps);
            System.out.println("VSync: on");
            return true;
        } catch (Throwable semVSync) {
            System.out.println("VSync: unavailable (" + semVSync.getClass().getSimpleName() + ")");
            return false;
        }
    }

    private Graphics2D getGraphics() {
        if(Engine.BUFFER == null) {
            try {
                Engine.vsync = Configs.VSync() && criarComVSync();
                if (!Engine.vsync) {
                    window.createBufferStrategy(3);
                }
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
            // BILINEAR, e nao BICUBIC.
            //
            // Esta linha era a opcao inteira travando. O jogo desenha cada sprite
            // JA COM ESCALA — sao centenas de blits ampliados por quadro — e o
            // bicubico e reamostragem de quatro por quatro pixels feita em
            // software para cada um deles. Medido nesta maquina, com quatrocentos
            // sprites por quadro:
            //
            //     NEAREST (desligado) ....  5 ms/quadro   200 fps
            //     BILINEAR ............... 14 ms/quadro    71 fps
            //     BICUBIC ................ 52 ms/quadro    19 fps
            //
            // Dezenove quadros por segundo e o que o jogador descreve como
            // interface travada. O bilinear suaviza igual para arte deste tamanho,
            // e as demais dicas de qualidade nao custam nada em cima dele — na
            // medida, 14,10 contra 14,22 ms. Quem cobrava o preco era so o
            // bicubico.
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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
        Debugging.setInfo("Ticks", Engine.HERTZ + " / " + (int) Engine.HZ);
        // Com o teto ao lado dá para ver de relance se o jogo está entregando o
        // que foi pedido nas opções — era o que faltava para notar que ele
        // entregava 125 de 144.
        Debugging.setInfo("FPS", Engine.FRAMES + " / " + Configs.MaxFrames());
        Runtime vm = Runtime.getRuntime();
        long emUso = vm.totalMemory() - vm.freeMemory();
        Debugging.setInfo("RAM do jogo", Debugging.formatBytes(emUso)
                + " de " + Debugging.formatBytes(vm.maxMemory()));
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
                    long inicioTick = System.nanoTime();
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
                    Desempenho.tick(System.nanoTime() - inicioTick, ticksExecutados);
                }
                //Atraso grande demais para recuperar: desiste dele em vez de
                //acumular dívida e entrar em espiral.
                if (delta_HZ > MAX_CATCHUP) {
                    delta_HZ = 0;
                }

                long nowFPS = System.nanoTime();
                ns_FPS = Engine.T / Math.max(1, alvoDeQuadros());
                delta_FPS += (nowFPS - lastTimeFPS) / ns_FPS;
                lastTimeFPS = nowFPS;
                boolean desenhou = false;
                // Com vsync o show() ja espera o monitor: o proprio monitor e o
                // limitador, e um segundo limite por relogio so brigaria com ele
                // (pulando uma atualizacao de vez em quando — o engasgo de volta).
                if (delta_FPS >= 1 || vsync) {
                    alpha = (float) Math.max(0, Math.min(1, delta_HZ));
                    long inicioRender = System.nanoTime();
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
                        Desempenho.quadro(System.nanoTime() - inicioRender);
                    }
                    //DESCONTA um quadro, não zera. Zerando, a fração que sobrava
                    //de cada volta era jogada fora e o laço nunca alcançava o
                    //limite: com o teto em 144 o jogo entregava 125. Só se o
                    //atraso passar de dois quadros é que ele é abandonado — aí
                    //sim recuperar seria pior do que perder.
                    delta_FPS -= 1;
                    if (delta_FPS > 2 || vsync) {
                        delta_FPS = 0;
                    }
                }

                //Show fps
                if (System.currentTimeMillis() - timer >= 1000) {
                    // VSYNC QUE NAO ESPERA. Alguns drivers aceitam o pedido e o
                    // ignoram em janela: o show() volta na hora e o jogo dispara a
                    // milhares de quadros. Medido, nao suposto — e ai o limitador
                    // de precisao assume, no ritmo do monitor.
                    if (vsync && frames > taxaDoMonitor() * 1.4) {
                        vsync = false;
                        System.out.println("VSync: ignored by the driver, pacing to "
                                + taxaDoMonitor() + " Hz instead");
                    }
                    Engine.FRAMES = frames;
                    frames = 0;
                    Engine.HERTZ = Hz;
                    Hz = 0;
                    timer += 1000;
                }
                //Só dorme quando não havia nada a fazer; dormir estando atrasado
                //é exatamente o que impedia o laço de recuperar.
                //
                //ESPERA DE PRECISAO. O sleep(1) do Windows dorme de um a dois
                //milissegundos, ao acaso — e era isso que fazia o intervalo entre
                //quadros oscilar entre 6 e 9 ms com o contador cravado em 165. O
                //olho le essa oscilacao como engasgo. Agora so dorme quando falta
                //bastante para o proximo quadro ou tick; no ultimo trecho gira no
                //lugar, e o quadro sai na hora certa.
                if (ticksExecutados == 0 && !desenhou) {
                    double faltaQuadro = (1 - delta_FPS) * ns_FPS;
                    double faltaTick = (1 - delta_HZ) * ns_HZ;
                    if (Math.min(faltaQuadro, faltaTick) > 2_000_000) {
                        Thread.sleep(1);
                    } else {
                        Thread.onSpinWait();
                    }
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



