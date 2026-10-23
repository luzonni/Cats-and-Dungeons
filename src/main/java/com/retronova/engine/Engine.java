package com.retronova.engine;

import com.retronova.engine.inputs.gamepad.Gamepad;
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


    public static Window window;
    private static BufferStrategy BUFFER;

    public static Random RAND = new Random();

    public Engine() {
        stackActivities = new ArrayList<>();
        Configs.init();
        Configs.load();
        Configs.sobreporParaDiagnostico();
        FontHandler.addFont("game", "septem");
        Sound.load();
        if (Foto.ligado()) {
            Sound.silenciar();      // fotografar nao toca nada nas caixas de quem esta na maquina
        }
        //O Sheeter monta "<path>/<módulo>/<nome>.png" por conta própria. Passar
        //resPath com a barra final gera "//" no meio do caminho: num classpath
        //explodido a URL é normalizada e funciona, mas dentro de um .jar os nomes
        //das entradas são comparados literalmente e o recurso não é encontrado.
        SpriteSheet.load(Engine.resPath.substring(0, Engine.resPath.length() - 1), Configs.GameScale());
        Engine.window = new Window(GameTag);
        // A ABERTURA (ver Abertura) por cima da tela inicial, que já espera embaixo
        // sem tocar a música: a abertura a traz por desvanecimento. Fica de fora na
        // bancada e no modo de foto, que entram direto (os roteiros de foto contam
        // com o menu no primeiro segundo); -Dcd.intro=completa|curta a força.
        String intro = System.getProperty("cd.intro");
        boolean abrir = !Boolean.getBoolean("cd.bench") && (!Foto.ligado() || intro != null);
        Menu menu = new Menu(!abrir);
        heapActivity(menu);
        if (abrir) {
            // COMPLETA A CADA INÍCIO: ter visto a abertura não deve encurtá-la.
            // A versão curta fica disponível só pela flag de diagnóstico.
            boolean completa = !"curta".equalsIgnoreCase(intro);
            heapActivity(new com.retronova.menus.Abertura(menu, completa));
        }
        // BANCADA (-Dcd.bench=true): entra direto numa arena com o Finn, para medir
        // desempenho sem navegar menus. Espaco 0 nao e um espaco de save valido —
        // nada e gravado nem apagado; as corridas do jogador ficam intactas.
        if (Boolean.getBoolean("cd.bench")) {
            // -Dcd.gato=0|1|2 escolhe Muffin, Azrael ou Finn (o padrao e o Finn).
            int gatoDaBancada = Math.max(0, Math.min(2, Integer.getInteger("cd.gato", 2)));
            // -Dcd.dialogo=<id> comeca na antecamara, onde o vendedor esta.
            String dialogo = System.getProperty("cd.dialogo");
            heapActivity(new com.retronova.game.Game(gatoDaBancada, dialogo != null
                    ? new com.retronova.game.map.room.Room("beginning")
                    : new com.retronova.game.map.arena.Arena(), 0));
            // -Dcd.piloto=true anda e da dash sozinho; -Dcd.imortal=true (ver
            // Player.strike) impede o gato de cair. Juntos, medem a horda cheia
            // com a camera varrendo o campo, que e o caso que trava.
            if (Boolean.getBoolean("cd.piloto")) {
                PilotoDeBancada.iniciar();
            }
            // Telas por cima, para conferir o desenho delas sem jogar ate elas.
            // Abrem depois de um segundo de partida: a chegada do gato fecha
            // qualquer pausa aberta antes dela.
            // -Dcd.roleta abre o baú também: a roleta foi desligada (ver Roleta), e
            // roteiros antigos de bancada continuam caindo na tela que a substituiu.
            if (Boolean.getBoolean("cd.bau") || Boolean.getBoolean("cd.roleta")) {
                com.retronova.game.Game.depoisDeUmSegundo =
                        () -> pause(new com.retronova.game.map.arena.AberturaDoBau());
            } else if (Boolean.getBoolean("cd.fim")) {
                // A tela de fim de corrida, com os numeros de 1s de partida.
                com.retronova.game.Game.depoisDeUmSegundo = () -> {
                    com.retronova.game.Estatisticas.encerrar(312, 840, 17, "Finn");
                    heapActivity(new com.retronova.menus.GameOver(2, 0));
                };
            } else if (System.getProperty("cd.dialogo") != null) {
                // -Dcd.dialogo=<id da conversa> abre essa conversa com o vendedor, sem
                // grava-la como vista: para ver cada contexto e cada expressao sem
                // jogar ate eles. -Dcd.dialogo=auto escolhe como o jogo escolheria.
                String id = System.getProperty("cd.dialogo");
                com.retronova.game.Game.depoisDeUmSegundo = () -> {
                    var vendedores = com.retronova.game.Game.getMap()
                            .getEntities(com.retronova.game.objects.entities.NPCs.Seller.class);
                    Object falante = vendedores.isEmpty() ? null : vendedores.get(0);
                    if (id.equals("auto")) {
                        com.retronova.game.dialogos.Dialogos.conversar("vendedor", falante);
                    } else {
                        com.retronova.game.dialogos.Dialogos.mostrarParaTeste(id, falante);
                    }
                };
            } else if (Boolean.getBoolean("cd.nivel")) {
                com.retronova.game.Game.depoisDeUmSegundo =
                        () -> pause(new com.retronova.game.map.arena.SubirDeNivel());
            } else if (System.getProperty("cd.rei") != null) {
                // -Dcd.rei=N chama o Rato Rei ao lado do gato, ja na fase N (1, 2 ou 3);
                // N=4 o chama com 1 de vida, para ver a vitoria sem lutar a luta toda,
                // para ver cada fase da luta sem esperar os trinta minutos.
                int fase = Math.max(1, Math.min(4, Integer.getInteger("cd.rei", 1)));
                com.retronova.game.Game.depoisDeUmSegundo = () -> {
                    var gato = com.retronova.game.Game.getPlayer();
                    var rei = com.retronova.game.objects.entities.Entity.build(
                            com.retronova.game.objects.entities.EntityIDs.RatoRei.ordinal(), 0, 0);
                    rei.setX(gato.getX() + 6 * com.retronova.game.objects.GameObject.SIZE());
                    rei.setY(gato.getY());
                    rei.setLife(fase == 4 ? 1 : rei.getLifeSize() * (1 - (fase - 1) / 3.0) - 1);
                    com.retronova.game.Game.getMap().put(rei);
                };
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
            // -Dcd.buffs=true da um nivel de cada passiva comum e uma de cada melhoria
            // da roleta, para ver a tela de Status (E) com a coluna de buffs cheia.
            if (Boolean.getBoolean("cd.buffs")) {
                Runnable antes = com.retronova.game.Game.depoisDeUmSegundo;
                com.retronova.game.Game.depoisDeUmSegundo = () -> {
                    var gato = com.retronova.game.Game.getPlayer();
                    for (var p : com.retronova.game.items.Passiva.values()) {
                        if (!p.exclusiva()) {
                            gato.getMelhorias().subirPassiva(gato, p);
                        }
                    }
                    for (var m : com.retronova.game.items.Melhoria.values()) {
                        gato.getMelhorias().aplicar(gato, m, com.retronova.game.items.Raridade.COMUM);
                    }
                    if (antes != null) {
                        antes.run();
                    }
                };
            }
            // -Dcd.habilidade=N da ao gato a habilidade exclusiva dele ja no nivel N
            // (1 a 3), para ver o efeito sem esperar a carta sair no level up.
            Integer habilidade = Integer.getInteger("cd.habilidade");
            if (habilidade != null) {
                Runnable antes = com.retronova.game.Game.depoisDeUmSegundo;
                com.retronova.game.Game.depoisDeUmSegundo = () -> {
                    var gato = com.retronova.game.Game.getPlayer();
                    for (var p : com.retronova.game.items.Passiva.values()) {
                        if (p.exclusiva() && p.disponivelPara(gato.getName())) {
                            for (int i = 0; i < Math.max(1, Math.min(3, habilidade)); i++) {
                                gato.getMelhorias().subirPassiva(gato, p);
                            }
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

    /** A tela por cima da activity (pausa, opções da pausa, cartas), ou null. */
    public static Activity sobreposta() {
        return Engine.OverView;
    }

    /** Quantas telas há na pilha. */
    public static int telasNaPilha() {
        return Engine.stackActivities.size();
    }

    public static void CLOSE() {
        Engine.getACTIVITY().dispose();
        Engine.isRunning = false;
    }

    public static int[] getResolution() {
        return window.resolucao();
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
     * A taxa do monitor EM QUE A JANELA ESTÁ, em Hz.
     *
     * Por monitor, e não uma vez só: com dois monitores de taxas diferentes, a
     * janela arrastada de um para o outro precisa passar a mirar a taxa do novo —
     * mirar 165 num monitor de 144 (ou o contrário) garante um quadro repetido ou
     * perdido a cada poucos. {@link #conferirMonitor()} relê uma vez por segundo.
     *
     * DESCONHECIDA VIRA 60. {@code DisplayMode.getRefreshRate()} pode devolver
     * REFRESH_RATE_UNKNOWN (Javadoc de DisplayMode), e 60 Hz é o que todo monitor
     * aceita: mirar menos que o monitor só repete quadros, nunca rasga.
     */
    private static int taxaCacheada;
    private static GraphicsDevice monitorDaTaxa;

    public static int taxaDoMonitor() {
        if (taxaCacheada == 0) {
            conferirMonitor();
        }
        return taxaCacheada;
    }

    /** Relê o monitor da janela e a taxa dele, se algo mudou. Uma vez por segundo. */
    private static void conferirMonitor() {
        try {
            GraphicsConfiguration gc = window != null ? window.getGraphicsConfiguration() : null;
            GraphicsDevice dev = gc != null ? gc.getDevice()
                    : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            int taxa = dev.getDisplayMode().getRefreshRate();
            int nova = taxa > 0 && taxa != DisplayMode.REFRESH_RATE_UNKNOWN ? taxa : 60;
            if (dev != monitorDaTaxa || nova != taxaCacheada) {
                monitorDaTaxa = dev;
                taxaCacheada = nova;
                System.out.println("Monitor refresh: " + nova + " Hz" + (taxa > 0 ? "" : " (unknown, using 60)"));
            }
            // A janela pode ter sido arrastada para outro monitor: a resolução que
            // vale (e o encaixe na tela) depende do tamanho dele.
            if (window != null) {
                window.atualizarMonitor();
            }
        } catch (Exception ignorado) {
            if (taxaCacheada == 0) {
                taxaCacheada = 60;
            }
        }
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

    /**
     * Desenha e mostra um quadro. Devolve se um quadro chegou à tela.
     *
     * A PERDA DE CONTEÚDO É TRATADA COMO O JAVADOC DE BufferStrategy MANDA:
     * redesenha enquanto {@code contentsRestored()}, mostra de novo enquanto
     * {@code contentsLost()} — com a MESMA estratégia. Antes, todo contentsLost()
     * jogava o BufferStrategy fora e criava outro no quadro seguinte; e o
     * contentsLost() acontece justamente no Alt+Tab, ao minimizar e quando outra
     * janela cobre esta. Recriar a cadeia de buffers nesses momentos era a piscada.
     * Só uma IllegalStateException (componente sem tela: janela fechada ou ainda
     * não exibida) descarta a estratégia — aí ela não tem mesmo como continuar.
     */
    /** O quadro inteiro no tamanho da imagem do jogo, no Graphics dado. */
    private static void desenharJogo(Graphics2D g) {
        prepararQuadro(g);
        desenharTelas(g);
        if (Debugging.running) {
            Debugging.render(g);
        }
    }

    /**
     * A imagem do jogo e a ampliação inteira dela, na placa de vídeo.
     *
     * VolatileImage porque ela mora na memória da placa (Javadoc de VolatileImage)
     * e o OpenGL a escala sem passar pela CPU; e por isso também ela pode ser
     * perdida — validate() a refaz quando é preciso.
     */
    private static java.awt.image.VolatileImage imagemDoJogo, imagemAmpliada;

    /**
     * A imagem do jogo escalada para o canvas, centrada, com tarjas pretas.
     *
     * NÍTIDA NA ESCALA INTEIRA, E SÓ SUAVE NO RESTO. Ampliar pixel art direto por
     * um fator quebrado (1280 em 1920 é 1,5x) dá duas escolhas ruins: vizinho mais
     * próximo, com pixels de larguras diferentes que tremem quando a câmera anda,
     * ou suavização, que borra tudo. Aqui a imagem é primeiro ampliada pelo maior
     * inteiro que cabe, sem suavizar, e só o que falta (de 1x a 2x) é suavizado —
     * o "sharp bilinear" dos emuladores: as bordas ficam firmes e o borrão fica
     * restrito a um pixel. Reduzir (janela menor que a imagem) usa a suavização
     * direto, que é o que reduz sem serrilhar.
     */
    private static void desenharEscalado(Graphics2D tela) {
        int w = window.largura(), h = window.altura();
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        if (gc == null || w <= 0 || h <= 0) {
            return;
        }
        imagemDoJogo = valida(imagemDoJogo, gc, w, h);
        do {
            if (imagemDoJogo.validate(gc) == java.awt.image.VolatileImage.IMAGE_INCOMPATIBLE) {
                imagemDoJogo = valida(null, gc, w, h);
            }
            Graphics2D g = imagemDoJogo.createGraphics();
            try {
                desenharJogo(g);
            } finally {
                g.dispose();
            }
        } while (imagemDoJogo.contentsLost());

        Rectangle destino = window.areaNaTela();
        tela.setColor(Color.black);
        tela.fillRect(0, 0, window.getCanvasLargura(), window.getCanvasAltura());
        double escala = window.escalaNaTela();
        int inteiro = (int) Math.floor(escala + 1e-6);
        java.awt.Image origem = imagemDoJogo;
        if (inteiro >= 2 && Math.abs(escala - inteiro) > 1e-6) {
            imagemAmpliada = valida(imagemAmpliada, gc, w * inteiro, h * inteiro);
            Graphics2D a = imagemAmpliada.createGraphics();
            a.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            a.drawImage(imagemDoJogo, 0, 0, w * inteiro, h * inteiro, null);
            a.dispose();
            origem = imagemAmpliada;
        }
        boolean nitida = Math.abs(escala - Math.round(escala)) < 1e-6;
        tela.setRenderingHint(RenderingHints.KEY_INTERPOLATION, nitida
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        tela.drawImage(origem, destino.x, destino.y, destino.width, destino.height, null);
    }

    /** A VolatileImage do tamanho pedido, refeita só se mudou de tamanho ou de placa. */
    private static java.awt.image.VolatileImage valida(java.awt.image.VolatileImage atual,
                                                       GraphicsConfiguration gc, int w, int h) {
        if (atual == null || atual.getWidth() != w || atual.getHeight() != h
                || atual.validate(gc) == java.awt.image.VolatileImage.IMAGE_INCOMPATIBLE) {
            if (atual != null) {
                atual.flush();
            }
            return gc.createCompatibleVolatileImage(w, h, Transparency.OPAQUE);
        }
        return atual;
    }

    private static int quadrosPerdidos, estrategiasCriadas;
    /** Quadros seguidos com a superfície perdida antes de recriar a estratégia (~1 s). */
    private static final int QUADROS_ATE_RECRIAR = 60;

    private static boolean desenharQuadro() {
        if (Engine.BUFFER == null) {
            try {
                Engine.vsync = Configs.VSync() && criarComVSync();
                sondagemDoVsync = 0;
                if (!Engine.vsync) {
                    window.createBufferStrategy(3);
                }
                Engine.BUFFER = window.getBufferStrategy();
                if (Video.LIGADO) {
                    // Cada criação é uma piscada em potencial: contar mostra se a
                    // estratégia está sendo jogada fora sem necessidade.
                    System.out.println("[video] BufferStrategy criado (" + (++estrategiasCriadas) + "a vez)");
                }
            } catch (IllegalStateException aindaNaoExibivel) {
                //A janela ainda não está pronta; tenta de novo no próximo quadro.
                Engine.BUFFER = null;
            }
            return false;
        }
        BufferStrategy buffer = Engine.BUFFER;
        try {
            // Poucas voltas: com a janela minimizada a superfície pode seguir
            // perdida indefinidamente, e o laço do jogo não pode ficar preso aqui.
            for (int tentativa = 0; tentativa < 3; tentativa++) {
                do {
                    Graphics2D g = (Graphics2D) buffer.getDrawGraphics();
                    try {
                        if (window.desenhoDireto()) {
                            desenharJogo(g);
                        } else {
                            desenharEscalado(g);
                        }
                    } finally {
                        g.dispose();
                    }
                } while (buffer.contentsRestored());
                buffer.show();
                if (!buffer.contentsLost()) {
                    quadrosPerdidos = 0;
                    Debugging.quadroMostrado();
                    return true;
                }
            }
            // ÚLTIMO RECURSO, e não reflexo. Com o monitor dormindo ou o driver
            // reiniciado, a superfície pode nunca voltar sozinha — era a janela
            // branca do jogo deixado ocioso. Só depois de um segundo inteiro perdida
            // a estratégia é recriada; um Alt+Tab se resolve bem antes disso.
            if (++quadrosPerdidos > QUADROS_ATE_RECRIAR) {
                quadrosPerdidos = 0;
                invalidateBuffer();
            }
            return false;
        } catch (IllegalStateException semSuperficie) {
            invalidateBuffer();
            return false;
        }
    }

    /** A tela corrente e a que estiver por cima dela. */
    private static void desenharTelas(Graphics2D g) {
        if (!stackActivities.isEmpty()) {
            getACTIVITY().render(g);
        }
        if (OverView != null) {
            OverView.render(g);
        }
    }

    /** O quadro inteiro num Graphics qualquer, para o modo de foto. Ver Foto. */
    static void desenharTudo(Graphics2D g) {
        prepararQuadro(g);
        desenharTelas(g);
    }

    /** O quadro como chega ao canvas, escalado se preciso. Para o modo de foto. */
    static void desenharNaTela(Graphics2D g) {
        if (window.desenhoDireto()) {
            desenharJogo(g);
        } else {
            desenharEscalado(g);
        }
    }

    /** Fundo preto e as dicas de desenho das opcoes. Vale para a tela e para a foto. */
    private static void prepararQuadro(Graphics2D graphics) {
        graphics.setColor(Color.black);
        graphics.fillRect(0, 0, window.largura(), window.altura());
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
        // Antes da janela e da Activity: o aperto do controle chega às teclas
        // virtuais no mesmo tick em que a tela vai lê-las.
        Gamepad.tick();
        Sound.tick();      // desvanecimentos de música (ver Sound.desvanecer)
        Foto.tick();
        window.tick();
        Debugging.tick();
        Debugging.setInfo("Ticks", Engine.HERTZ + " / " + (int) Engine.HZ);
        // Com o teto ao lado dá para ver de relance se o jogo está entregando o
        // que foi pedido nas opções — era o que faltava para notar que ele
        // entregava 125 de 144.
        Debugging.setInfo("FPS", Engine.FRAMES + " / " + descricaoDoAlvo());
        Runtime vm = Runtime.getRuntime();
        long emUso = vm.totalMemory() - vm.freeMemory();
        Debugging.setInfo("RAM do jogo", Debugging.formatBytes(emUso)
                + " de " + Debugging.formatBytes(vm.maxMemory()));
        Debugging.setInfo("Screen Size", "[" + Engine.window.largura() + " / " + Engine.window.altura() + "]");
        Debugging.setInfo("Controle", Gamepad.conectado() ? "conectado" : "nenhum");
    }

    @Override
    public void run() {
        // O relógio de 1 ms do Windows antes de qualquer espera: ver RelogioDoSistema.
        RelogioDoSistema.pedirPrecisao();
        final long nsTick = (long) (Engine.T / Engine.HZ);
        long agora = System.nanoTime();
        // PRAZOS ABSOLUTOS. O próximo tick e o próximo quadro são instantes no
        // relógio, e cada um avança um período exato a partir do anterior — não a
        // partir de "quando deu". É o "Fix Your Timestep!" (Glenn Fiedler) e o
        // "Game Loop" do Game Programming Patterns (Robert Nystrom): a lógica anda
        // em passo fixo, o quadro sai quando chega a hora dele, e o resto do tempo
        // a thread espera.
        long proximoTick = agora;
        long proximoQuadro = agora;
        int Hz = 0;
        int frames = 0;
        long timer = agora;
        window.destacarAoIniciar();
        while (isRunning) {
            try {
                agora = System.nanoTime();

                //O tick tem prioridade sobre o render. Quando o render atrasa, a
                //simulação recupera o atraso executando ticks em sequência, até
                //MAX_CATCHUP por volta.
                int ticksExecutados = 0;
                while (agora >= proximoTick && ticksExecutados < MAX_CATCHUP) {
                    long inicioTick = System.nanoTime();
                    // O F11 E LIDO NO TICK, e nao a cada volta do laco. Fora daqui
                    // ele rodava tambem na espera entre quadros, que gira no lugar
                    // milhoes de vezes por segundo — e cada leitura percorre as 47
                    // teclas comparando texto. Medido: ~22% do tempo da thread do
                    // jogo.
                    if (com.retronova.engine.inputs.Acao.TELA_CHEIA.apertou() && window != null) {
                        Configs.setFullscreen(!Configs.Fullscreen());
                        window.aplicarVideo();
                    }
                    this.tick();
                    if (ACTIVITY_RUNNING && !stackActivities.isEmpty()) {
                        getACTIVITY().tick();
                    }
                    if (OverView != null) {
                        OverView.tick();
                    }
                    Hz++;
                    proximoTick += nsTick;
                    ticksExecutados++;
                    Desempenho.tick(System.nanoTime() - inicioTick, ticksExecutados);
                    agora = System.nanoTime();
                }
                //Atraso grande demais para recuperar: desiste dele em vez de
                //acumular dívida e entrar em espiral.
                if (agora - proximoTick > MAX_CATCHUP * nsTick) {
                    proximoTick = agora;
                }

                // O QUADRO. Com vsync de verdade o show() espera o monitor: o
                // próprio monitor é o limitador, e um segundo limite por relógio só
                // brigaria com ele. Sem vsync (desligado, ou ignorado pelo driver),
                // o quadro sai no prazo dele; sem limite, sai sempre.
                long periodo = periodoDoQuadro();
                boolean noRelogio = !vsync && periodo > 0;
                boolean desenhou = false;
                if (!noRelogio || agora >= proximoQuadro) {
                    alpha = (float) Math.max(0, Math.min(1, 1 - (proximoTick - agora) / (double) nsTick));
                    long inicioRender = System.nanoTime();
                    if (desenharQuadro()) {
                        frames++;
                        sondarVsync();
                        Desempenho.quadro(System.nanoTime() - inicioRender);
                    }
                    desenhou = true;
                    if (noRelogio) {
                        // Avança UM período. Se o quadro saiu mais de um período
                        // atrasado, recomeça a contagem dali: tentar recuperar
                        // mandaria quadros em rajada, que é o que se lê como engasgo.
                        proximoQuadro += periodo;
                        if (agora - proximoQuadro > periodo) {
                            proximoQuadro = agora + periodo;
                        }
                    }
                }

                // Uma vez por segundo: contadores, monitor atual e o vsync que mente.
                if (agora - timer >= 1_000_000_000L) {
                    // VSYNC QUE NAO ESPERA. Alguns drivers aceitam o pedido e o
                    // ignoram em janela: o show() volta na hora e o jogo dispara a
                    // milhares de quadros. Medido, nao suposto — e ai o limitador
                    // assume, no ritmo do monitor.
                    if (vsync && frames > taxaDoMonitor() * 1.4) {
                        vsync = false;
                        System.out.println("VSync: ignored by the driver, pacing to "
                                + taxaDoMonitor() + " Hz instead");
                    }
                    conferirMonitor();
                    Engine.FRAMES = frames;
                    frames = 0;
                    Engine.HERTZ = Hz;
                    Hz = 0;
                    timer += 1_000_000_000L;
                }

                // A ESPERA. Só quando não havia nada a fazer nesta volta; dormir
                // estando atrasado é o que impedia o laço de recuperar.
                if (ticksExecutados == 0 && !desenhou) {
                    long alvo = noRelogio ? Math.min(proximoTick, proximoQuadro) : proximoTick;
                    esperarAte(alvo);
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

    /**
     * Quanto o sleep(1) desta máquina dura DE VERDADE, em ns, medido a cada sono.
     *
     * A ESPERA NÃO CONFIA NO SLEEP. Com o relógio de 1 ms ele dura 1 a 2 ms; sem
     * ele (winmm indisponível, ou o Windows ignorando o pedido de uma janela
     * minimizada), até 19 ms — medido. Dormir quando falta menos que isso para o
     * quadro é perder o quadro, e era esse o FPS inconstante sem VSync. Aqui a
     * thread só dorme quando sobra tempo para o pior sono recente, e gira
     * (onSpinWait) no trecho final: com o relógio bom ela dorme quase o tempo todo;
     * com o ruim, gira mais, mas não perde o prazo.
     */
    private static long sonoMedido = 2_000_000L;

    private static void esperarAte(long alvo) throws InterruptedException {
        long falta = alvo - System.nanoTime();
        if (falta > sonoMedido + 300_000L) {
            long antes = System.nanoTime();
            Thread.sleep(1);
            long durou = System.nanoTime() - antes;
            // Sobe na hora para o pior visto; desce devagar, para um sono rápido
            // isolado não convencer a espera de que o relógio melhorou.
            sonoMedido = durou > sonoMedido ? durou : (sonoMedido * 31 + durou) / 32;
        } else if (falta > 0) {
            Thread.onSpinWait();
        }
    }

    /** O ritmo que o laço persegue agora, por extenso, para o overlay de depuração (F3). */
    private static String descricaoDoAlvo() {
        long p = periodoDoQuadro();
        String alvo = p == 0 ? "sem limite" : String.valueOf(Math.round(Engine.T / p));
        return alvo + (Configs.VSync() ? (vsync ? " (vsync)" : " (vsync ignorado, relógio)") : " (relógio)");
    }

    /**
     * O período do quadro em ns, ou 0 sem limite.
     *
     * VSync ligado: a taxa do monitor (se o driver honrar, o show() já espera; se
     * ignorar, o relógio assume nessa taxa). VSync desligado: o limite escolhido —
     * {@link Configs#LIMITE_MONITOR} é a taxa do monitor, {@link Configs#SEM_LIMITE}
     * é nenhum.
     */
    /**
     * O VSYNC PEDIDO ESTÁ VALENDO? Responde em 20 quadros, não em um segundo.
     *
     * Muitos drivers aceitam o pedido de vsync e o ignoram (em janela, e em
     * algumas máquinas também na janela sem bordas): o show() volta na hora. A
     * detecção antiga só olhava o FPS ao fim de cada segundo — e o primeiro segundo
     * de cada buffer novo (abrir o jogo, F11, ligar o VSync) rodava solto, a 400+
     * quadros, e o seguinte tropeçava na troca para o relógio. Era o "bugado ao
     * abrir". Agora, se os 20 primeiros quadros saem em menos de 70% do período do
     * monitor cada um, o show() não está esperando nada: o relógio assume já.
     * A checagem por segundo continua, para o driver que passa a ignorar depois.
     */
    private static final int QUADROS_DA_SONDAGEM = 20;
    private static int sondagemDoVsync = -1;
    private static long inicioDaSondagem;

    private static void sondarVsync() {
        if (!vsync || sondagemDoVsync < 0) {
            return;
        }
        long agora = System.nanoTime();
        if (sondagemDoVsync == 0) {
            inicioDaSondagem = agora;       // o primeiro quadro só marca o início
        } else if (sondagemDoVsync == QUADROS_DA_SONDAGEM) {
            double media = (agora - inicioDaSondagem) / (double) QUADROS_DA_SONDAGEM;
            double periodo = Engine.T / taxaDoMonitor();
            if (media < periodo * 0.7) {
                vsync = false;
                System.out.printf(java.util.Locale.ROOT,
                        "VSync: ignored by the driver (%.2f ms per frame), pacing to %d Hz instead%n",
                        media / 1e6, taxaDoMonitor());
            }
            sondagemDoVsync = -1;
            return;
        }
        sondagemDoVsync++;
    }

    /** O período alvo do quadro, em ns (0 sem limite), para a linha do gráfico do F3. */
    static long periodoAlvoNs() {
        return periodoDoQuadro();
    }

    private static long periodoDoQuadro() {
        int alvo;
        if (Configs.VSync()) {
            alvo = taxaDoMonitor();
        } else {
            int limite = Configs.MaxFrames();
            if (limite == Configs.SEM_LIMITE) {
                return 0;
            }
            alvo = limite == Configs.LIMITE_MONITOR ? taxaDoMonitor() : limite;
        }
        return (long) (Engine.T / Math.max(1, alvo));
    }

}
