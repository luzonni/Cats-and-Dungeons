package com.retronova.engine;

import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.sound.Sound;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class Window extends Canvas {

    @Serial
    private static final long serialVersionUID = 36752349087L;

    public static final BufferedImage DEFAULT_CURSOR;
    public static final BufferedImage POINTER_CURSOR;
    /** Imagem transparente: o ponteiro escondido do modo controle. */
    private static final BufferedImage SEM_CURSOR = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    static {
        DEFAULT_CURSOR = new SpriteHandler("ui","cursor", 3).getSHEET();
        POINTER_CURSOR = new SpriteHandler("ui", "cursor_hover", 3).getSHEET();
    }

    private final String name;
    private boolean pointing;
    /** Ultimo estado de foco visto, para so agir na troca. */
    private boolean focada = true;
    private JFrame frame;
    private final Toolkit toolkit;

    public Window(String name) {
        this.name = name;
        this.toolkit = Toolkit.getDefaultToolkit();
        initFrame();
        Mouse m = new Mouse();
        KeyBoard k = new KeyBoard();
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
        addKeyListener(k);
    }

    public void initFrame(){
        this.frame = new JFrame(this.name);
        frame.add(this);
        frame.setUndecorated(Configs.Fullscreen() && !Foto.ligado());
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        if(Configs.Fullscreen() && !Foto.ligado()) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows 11")) {
                setPreferredSize(toolkit.getScreenSize());
                frame.setMinimumSize(toolkit.getScreenSize());
            } else if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                if (gd.isFullScreenSupported()) {
                    gd.setFullScreenWindow(frame);
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        }else {
            setPreferredSize(new Dimension(Engine.getResolution()[0], Engine.getResolution()[1]));
            //Sem mínimo: definir o mínimo do frame com a medida do canvas impedia
            //qualquer correção posterior e ainda misturava as duas medidas.
            frame.setMinimumSize(null);
        }

        try {
            setCursor(DEFAULT_CURSOR);
            frame.setIconImages(iconesDaJanela());
        }catch(Exception ignore) { }
        createOpenGl();
        if (Foto.ligado()) {
            prepararParaFoto();
            return;
        }
        frame.pack();
        if (!Configs.Fullscreen()) {
            caberNaAreaUtil();
        }
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        refreshSize();      // o cache precisa valer já no primeiro quadro
        showAccelerators();
    }

    /**
     * A janela do modo de foto: existe (o BufferStrategy precisa dela), mas fica
     * longe da área visível, nunca pega o foco e não aparece na barra de tarefas —
     * rodar um roteiro de fotos não pode atrapalhar quem está usando a máquina.
     * Sempre em janela, no tamanho pedido, sem encolher para caber no monitor.
     */
    private void prepararParaFoto() {
        Dimension pedido = Foto.tamanho();
        if (pedido != null) {
            setPreferredSize(pedido);
        }
        if (frame.isUndecorated()) {
            frame.setUndecorated(false);
        }
        frame.setType(java.awt.Window.Type.UTILITY);
        frame.setFocusableWindowState(false);
        frame.setAutoRequestFocus(false);
        frame.pack();
        frame.setLocation(-30000, -30000);
        frame.setVisible(true);
        refreshSize();
    }

    /**
     * Encolhe o canvas até o frame inteiro caber na área útil do monitor.
     *
     * {@code getMaximumWindowBounds()} já desconta a barra de tarefas, e a
     * decoração do frame (título e bordas) só é conhecida depois do {@code pack()}.
     * Sem esse passo, pedir 2560x1440 numa tela de 2560x1440 gerava uma janela
     * mais alta que a área disponível, que ia parar atrás da barra de tarefas —
     * e só maximizar e restaurar consertava.
     */
    private void caberNaAreaUtil() {
        Rectangle util = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Insets bordas = frame.getInsets();
        int maxLargura = util.width - bordas.left - bordas.right;
        int maxAltura = util.height - bordas.top - bordas.bottom;

        Dimension pedido = getPreferredSize();
        int largura = Math.min(pedido.width, Math.max(1, maxLargura));
        int altura = Math.min(pedido.height, Math.max(1, maxAltura));
        if (largura != pedido.width || altura != pedido.height) {
            setPreferredSize(new Dimension(largura, altura));
            frame.pack();
        }
    }

    /**
     * Ícone em vários tamanhos, para o sistema escolher o mais próximo em vez de
     * reamostrar um único. Todos saem de múltiplos inteiros do PNG de 16px, com
     * nearest-neighbor: reduzir uma imagem grande com suavização é o que deixava
     * o gato borrado e irreconhecível na barra de tarefas.
     */
    private static List<Image> iconesDaJanela() {
        BufferedImage base = new SpriteHandler("ui", "icon", 1).getSHEET();
        List<Image> imagens = new ArrayList<>();
        for (int escala : new int[] {1, 2, 3, 4}) {
            imagens.add(escalarInteiro(base, escala));
        }
        return imagens;
    }

    private static BufferedImage escalarInteiro(BufferedImage origem, int escala) {
        if (escala == 1) {
            return origem;
        }
        int largura = origem.getWidth() * escala;
        int altura = origem.getHeight() * escala;
        BufferedImage destino = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(origem, 0, 0, largura, altura, null);
        g.dispose();
        return destino;
    }

    private void createOpenGl() {
        try {
            System.setProperty("sun.java2d.opengl", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void showAccelerators() {
        boolean oglEnabled = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getImageCapabilities()
                .isAccelerated();
        System.out.println("OpenGL: " + System.getProperty("sun.java2d.opengl")); // "true" se OpenGL estiver ativado
        System.out.println("DirectX: " + System.getProperty("sun.java2d.d3d"));   // "true" se Direct3D estiver ativado
        System.out.println("OpenGL Pipeline enabled: " + oglEnabled);
    }

    public void pointing() {
        this.pointing = true;
    }

    void tick() {
        refreshSize();      // uma leitura do AWT por tick, em vez de dezenas por quadro
        conferirFoco();
        if(Entrada.usandoControle()) {
            // No modo controle o ponteiro some: parado no meio da tela, ele
            // parecia apontar para algo que nao responde mais a ele.
            pointing = false;
            Engine.window.setCursor(SEM_CURSOR);
        }else if(pointing) {
            pointing = false;
            Engine.window.setCursor(Window.POINTER_CURSOR, new Point(6*3, 3));
        }else {
            Engine.window.setCursor(Window.DEFAULT_CURSOR);
        }
    }

    /**
     * Cala o som quando a janela sai de foco, e devolve quando volta.
     *
     * E consulta por tick, e nao WindowFocusListener, de proposito: o listener so
     * dispara em TROCA de foco, e o caso que mais incomodava era a janela que
     * nasce atras de tudo — o processo sobe, a musica comeca e ninguem nunca
     * clicou nela, entao nenhuma troca aconteceria para o listener ouvir.
     * Consultando, o primeiro tick ja pega o estado real.
     */
    private void conferirFoco() {
        if(frame == null) {
            return;
        }
        boolean agora = frame.isFocused();
        if(agora == focada) {
            return;
        }
        focada = agora;
        if(agora) {
            Sound.retomar();
        }else {
            Sound.silenciar();
        }
    }

    private void closeFrame() {
        frame.setVisible(false);
        frame.dispose();
        frame = null;
    }

    /**
     * Recria a janela (F11, resolução nova).
     *
     * NA EDT. Quem pede é a thread do jogo, no meio do tick, e o Swing só aceita
     * criar e destruir janela pela thread dele: fora dela, o JFrame novo nascia
     * disputando o estado do AWT com os eventos que a EDT ainda processava do
     * antigo. A thread do jogo ESPERA a troca terminar (invokeAndWait), para o
     * quadro seguinte já sair no canvas novo — e não pode estar segurando lock
     * nenhum da janela nessa espera, que é o que a EDT precisa para trabalhar.
     */
    public void resetWindow() {
        if (SwingUtilities.isEventDispatchThread()) {
            recriar();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(this::recriar);
        } catch (InterruptedException interrompida) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException falha) {
            throw new IllegalStateException("Falha ao recriar a janela", falha.getCause());
        }
    }

    private void recriar() {
        //O BufferStrategy pertence a este canvas dentro do frame antigo e não
        //sobrevive ao dispose. Sem invalidá-lo, o jogo passa a desenhar num
        //buffer morto e a janela nova fica em branco.
        Engine.invalidateBuffer();
        closeFrame();
        initFrame();
        requestFocus();
    }

    /**
     * Cursores nativos já criados, um por imagem, e o que está na janela agora.
     *
     * O tick escolhe o cursor 60 vezes por segundo, e cada escolha fazia
     * {@code createCustomCursor} — o Windows monta um cursor NATIVO novo a partir
     * da imagem — e depois trocava o da janela, passando pelo lock da AWT, na
     * thread do jogo, mesmo quando nada tinha mudado. No Flight Recorder era o
     * maior custo nativo da thread do jogo. Agora cada cursor nasce uma vez e a
     * janela só é tocada quando o cursor desejado MUDA (ou quando ela é recriada,
     * como no F11, e o frame novo ainda não tem cursor nenhum).
     */
    private final java.util.Map<BufferedImage, Cursor> cursores = new java.util.IdentityHashMap<>();
    private Cursor cursorAtual;
    private JFrame frameDoCursor;

    private void usarCursor(BufferedImage imagem, Point ponta) {
        Cursor c = cursores.computeIfAbsent(imagem, i -> toolkit.createCustomCursor(i, ponta, "cursor"));
        if (c != cursorAtual || frame != frameDoCursor) {
            frame.setCursor(c);
            cursorAtual = c;
            frameDoCursor = frame;
        }
    }

    public synchronized void setCursor(BufferedImage cursor) {
        usarCursor(cursor, new Point(0, 0));
    }

    public synchronized void setCursor(BufferedImage cursor, Point pointClick) {
        usarCursor(cursor, pointClick);
    }

    public synchronized void resetCursor() {
        usarCursor(DEFAULT_CURSOR, new Point(0, 0));
    }

    //Getter's and Setter's

    public JFrame getFrame() {
        return this.frame;
    }

    /**
     * Tamanho em cache.
     *
     * Ler direto do AWT aqui era caro e perigoso: {@code frame.getComponent(0)}
     * sincroniza no tree lock global do AWT, e estes getters são chamados dezenas
     * de vezes por quadro pela thread do jogo — que também é quem executa
     * {@link #resetWindow()}. Disputar esse lock com a EDT travava a janela.
     * O valor é atualizado uma vez por tick e a cada recriação da janela.
     */
    private volatile int cachedWidth;
    private volatile int cachedHeight;

    /** Lê o tamanho real do AWT. Só deve ser chamado fora do caminho quente. */
    private void refreshSize() {
        JFrame f = this.frame;
        if (f == null || f.getComponentCount() == 0) {
            return;
        }
        Component c = f.getComponent(0);
        int w = c.getWidth();
        int h = c.getHeight();
        if (w > 0 && h > 0) {
            this.cachedWidth = w;
            this.cachedHeight = h;
        }
    }

    public int getWidth() {
        if (cachedWidth == 0) {
            refreshSize();
        }
        return cachedWidth;
    }

    public int getHeight() {
        if (cachedHeight == 0) {
            refreshSize();
        }
        return cachedHeight;
    }

    public Dimension getScreenSize() {
        return toolkit.getScreenSize();
    }

}
