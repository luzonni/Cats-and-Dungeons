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

    /**
     * Cria a janela UMA VEZ. Resolução e tela cheia mudam depois por
     * {@link #aplicarVideo()}, na mesma janela.
     */
    private void initFrame() {
        this.frame = new JFrame(this.name);
        // PRETO DESDE O PRIMEIRO INSTANTE. Até o primeiro quadro, o sistema pinta a
        // janela com o fundo dela, que por padrão é claro: era o clarão antes de a
        // abertura começar. Preto, a abertura sai do preto sem emenda.
        setBackground(Color.BLACK);
        frame.getContentPane().setBackground(Color.BLACK);
        frame.setBackground(Color.BLACK);
        frame.add(this);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            setCursor(DEFAULT_CURSOR);
            frame.setIconImages(iconesDaJanela());
        } catch (Exception ignore) { }
        createOpenGl();
        if (Foto.ligado() && !Foto.janelaReal()) {
            prepararParaFoto();
            return;
        }
        if (Foto.janelaReal()) {
            // Teste na tela de verdade, mas sem roubar o teclado de quem usa a máquina.
            frame.setFocusableWindowState(false);
            frame.setAutoRequestFocus(false);
        }
        boolean telaCheia = Configs.Fullscreen();
        frame.setUndecorated(telaCheia);
        dimensionar(telaCheia, true);
        frame.setVisible(true);
        refreshSize();      // o cache precisa valer já no primeiro quadro
        showAccelerators();
        Video.registrar("abertura", this);
    }

    /**
     * A janela do modo de foto: existe (o BufferStrategy precisa dela), mas fica
     * longe da área visível, nunca pega o foco e não aparece na barra de tarefas —
     * rodar um roteiro de fotos não pode atrapalhar quem está usando a máquina.
     * Sempre em janela, no tamanho pedido, sem encolher para caber no monitor.
     */
    private void prepararParaFoto() {
        Dimension pedido = Foto.tamanho();
        setPreferredSize(pedido != null ? pedido
                : new Dimension(1280, 720));
        frame.setType(java.awt.Window.Type.UTILITY);
        frame.setFocusableWindowState(false);
        frame.setAutoRequestFocus(false);
        frame.pack();
        frame.setLocation(-30000, -30000);
        frame.setVisible(true);
        refreshSize();
    }

    /**
     * Aplica resolução e tela cheia de uma vez, na janela que já existe.
     *
     * NADA DE RECRIAR A JANELA. A versão anterior destruía o JFrame e criava
     * outro a cada resolução nova e a cada F11: a tela apagava, o sistema
     * redesenhava a área de trabalho por baixo e a janela nova surgia — a
     * piscada. Agora:
     *
     *   - resolução nova, mesmo modo: a janela só muda de tamanho;
     *   - tela cheia ligada ou desligada: a borda é a única coisa que o Swing não
     *     deixa trocar com a janela na tela ({@code setUndecorated} exige a janela
     *     não exibível), então ela é escondida, trocada e mostrada de novo — o
     *     mesmo JFrame, numa passada só, já no tamanho final.
     *
     * NA EDT, e a thread do jogo espera: o Swing só aceita mexer em janela pela
     * thread dele, e o quadro seguinte precisa sair já na janela nova. A thread do
     * jogo não pode segurar lock nenhum da janela nessa espera.
     */
    public void aplicarVideo() {
        if (Foto.ligado() && !Foto.janelaReal()) {
            return;     // o modo de foto tem tamanho próprio e nunca vai para a tela
        }
        if (SwingUtilities.isEventDispatchThread()) {
            aplicarVideoNaEdt();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(this::aplicarVideoNaEdt);
        } catch (InterruptedException interrompida) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException falha) {
            throw new IllegalStateException("Falha ao aplicar o vídeo", falha.getCause());
        }
    }

    private void aplicarVideoNaEdt() {
        boolean telaCheia = Configs.Fullscreen();
        if (frame.isUndecorated() != telaCheia) {
            // O BufferStrategy pertence ao canvas exibível; esconder a janela o mata.
            Engine.invalidateBuffer();
            frame.dispose();
            frame.setUndecorated(telaCheia);
            dimensionar(telaCheia, true);
            frame.setVisible(true);
            requestFocus();
        } else {
            dimensionar(telaCheia, false);
        }
        refreshSize();
        Video.registrar("aplicar", this);
    }

    /**
     * O tamanho da janela para o modo atual.
     *
     * TELA CHEIA SEM BORDAS, do tamanho do monitor EM QUE A JANELA ESTÁ — e não do
     * monitor principal, nem pela troca do modo de vídeo. Antes havia três
     * caminhos: no Windows 11, o tamanho da tela; no Linux, a tela cheia
     * exclusiva; e no Windows 10 nenhum dos dois, com a janela sem tamanho
     * nenhum. A tela cheia exclusiva troca o modo do monitor, que apaga a tela a
     * cada troca e a cada Alt+Tab (tutorial "Full-Screen Exclusive Mode" da
     * Oracle); a janela sem bordas não troca nada.
     *
     * EM JANELA, a resolução pedida, encolhida para caber na área útil do mesmo
     * monitor (sem a barra de tarefas). {@code getMaximumWindowBounds()} só vê o
     * monitor principal, e por isso a área sai de {@code getScreenInsets} do
     * monitor da janela.
     */
    private void dimensionar(boolean telaCheia, boolean centralizar) {
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle monitor = gc.getBounds();
        atualizarMonitor();
        if (telaCheia) {
            setPreferredSize(monitor.getSize());
            frame.setBounds(monitor);
            frame.validate();
            return;
        }
        Insets barras = toolkit.getScreenInsets(gc);
        Rectangle util = new Rectangle(monitor.x + barras.left, monitor.y + barras.top,
                monitor.width - barras.left - barras.right, monitor.height - barras.top - barras.bottom);
        int[] res = resolucao();
        setPreferredSize(new Dimension(res[0], res[1]));
        frame.pack();
        Insets bordas = frame.getInsets();
        int maxLargura = util.width - bordas.left - bordas.right;
        int maxAltura = util.height - bordas.top - bordas.bottom;
        // NÃO CABE NA ÁREA ÚTIL (1920x1080 num monitor 1920x1080 com barra de
        // título e de tarefas): a janela encolhe NA PROPORÇÃO da imagem, e a
        // imagem é reduzida para ela. Encolher só o lado que sobrava distorcia a
        // proporção — e fazia "1920" virar 1904x993.
        if (res[0] > maxLargura || res[1] > maxAltura) {
            double k = Math.min(maxLargura / (double) res[0], maxAltura / (double) res[1]);
            setPreferredSize(new Dimension(Math.max(1, (int) (res[0] * k)), Math.max(1, (int) (res[1] * k))));
            frame.pack();
        }
        // Centraliza na abertura e sempre que a janela nova não couber onde está.
        Rectangle atual = frame.getBounds();
        if (centralizar || !util.contains(atual)) {
            frame.setLocation(util.x + Math.max(0, (util.width - atual.width) / 2),
                    util.y + Math.max(0, (util.height - atual.height) / 2));
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


    // ------------------------------------------------------------ tamanhos
    //
    // DOIS TAMANHOS, E O JOGO SÓ CONHECE UM. A RESOLUÇÃO é o tamanho da imagem do
    // jogo — é ela que largura() e altura() devolvem, e é nela que tudo é
    // desenhado e posicionado. O CANVAS é o pedaço da tela que a janela ocupa. Os
    // dois coincidem no caso comum (janela que cabe), e aí o desenho vai direto;
    // quando não coincidem (tela cheia em resolução menor que o monitor, janela
    // maior que a área útil, janela redimensionada), a imagem é escalada para
    // caber, com tarjas pretas (ver Engine.desenharQuadro).
    //
    // Antes o jogo desenhava no tamanho do canvas e chamava isso de resolução: em
    // tela cheia a opção era ignorada, e em janela ela valia só quando cabia.
    //
    // PIXELS FÍSICOS. Main fixa sun.java2d.uiScale=1 (JEP 263 permite), então a
    // escala de DPI do sistema não estica nada: 1280x720 são 1280x720 pixels da
    // tela, a 100% ou a 150%. A janela fica menor num monitor de DPI alto, como em
    // todo jogo que escolhe a resolução em pixels.

    /** Canvas em cache: lido uma vez por tick, nunca por quadro (ver refreshSize). */
    private volatile int cachedWidth;
    private volatile int cachedHeight;
    /** A resolução em vigor: a escolhida, ou a do monitor se ela não couber. */
    private volatile int resW, resH;
    /** O monitor da janela, relido uma vez por segundo e a cada mudança de vídeo. */
    private volatile int monitorW, monitorH;
    /** Onde e em que escala a imagem do jogo vai parar no canvas. */
    private volatile double escalaNaTela = 1;
    private volatile int origemX, origemY, larguraNaTela, alturaNaTela;

    /**
     * Lê o tamanho do canvas e recalcula a resolução e o encaixe.
     *
     * {@code super.getWidth()} só lê um campo, sem o tree lock do AWT. A versão
     * anterior lia {@code frame.getComponent(0)}, que sincroniza no lock global, e
     * disputar esse lock com a EDT a cada leitura travava a janela.
     */
    private void refreshSize() {
        int w = super.getWidth();
        int h = super.getHeight();
        if (w > 0 && h > 0) {
            this.cachedWidth = w;
            this.cachedHeight = h;
        }
        atualizarResolucao();
    }

    /** Relê o monitor em que a janela está. Fora do caminho quente. */
    void atualizarMonitor() {
        JFrame f = this.frame;
        if (f == null) {
            return;
        }
        Rectangle m = f.getGraphicsConfiguration().getBounds();
        monitorW = m.width;
        monitorH = m.height;
        atualizarResolucao();
    }

    private void atualizarResolucao() {
        int w, h;
        if (Foto.ligado() && !Foto.janelaReal()) {
            w = cachedWidth;        // a foto sai no tamanho pedido para ela
            h = cachedHeight;
        } else {
            w = Configs.resolucaoLargura();
            h = Configs.resolucaoAltura();
            // A ESCOLHIDA NÃO CABE NO MONITOR (trocou de monitor, ou é a primeira
            // execução): vale a do monitor, que é sempre segura. A escolha gravada
            // não é apagada — de volta ao monitor grande, ela volta a valer.
            if (monitorW > 0 && (w <= 0 || h <= 0 || w > monitorW || h > monitorH)) {
                w = monitorW;
                h = monitorH;
            }
            if (w <= 0 || h <= 0) {
                w = cachedWidth;
                h = cachedHeight;
            }
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        resW = w;
        resH = h;
        encaixar();
    }

    /** O maior retângulo com a proporção da imagem que cabe no canvas, centrado. */
    private void encaixar() {
        int cw = cachedWidth, ch = cachedHeight;
        if (cw <= 0 || ch <= 0 || resW <= 0 || resH <= 0) {
            return;
        }
        double escala = Math.min(cw / (double) resW, ch / (double) resH);
        int dw = (int) Math.round(resW * escala), dh = (int) Math.round(resH * escala);
        larguraNaTela = dw;
        alturaNaTela = dh;
        origemX = (cw - dw) / 2;
        origemY = (ch - dh) / 2;
        escalaNaTela = escala;
    }

    /** Largura da imagem do jogo, em pixels. Todo o jogo se posiciona por ela. */
    public int largura() {
        if (resW == 0) {
            refreshSize();
        }
        return resW;
    }

    /** Altura da imagem do jogo, em pixels. */
    public int altura() {
        if (resH == 0) {
            refreshSize();
        }
        return resH;
    }

    /** A resolução em vigor, {largura, altura}. */
    public int[] resolucao() {
        return new int[]{largura(), altura()};
    }

    /** A imagem do jogo tem exatamente o tamanho do canvas: o desenho vai direto. */
    boolean desenhoDireto() {
        return resW == cachedWidth && resH == cachedHeight;
    }

    double escalaNaTela() {
        return escalaNaTela;
    }

    Rectangle areaNaTela() {
        return new Rectangle(origemX, origemY, larguraNaTela, alturaNaTela);
    }

    /** Converte um ponto do canvas (o mouse) para a imagem do jogo. */
    public int paraJogoX(int xNoCanvas) {
        return (int) Math.floor((xNoCanvas - origemX) / escalaNaTela);
    }

    public int paraJogoY(int yNoCanvas) {
        return (int) Math.floor((yNoCanvas - origemY) / escalaNaTela);
    }

    /** Largura real do canvas na tela, em pixels. */
    int getCanvasLargura() {
        return cachedWidth;
    }

    /** Altura real do canvas na tela, em pixels. */
    int getCanvasAltura() {
        return cachedHeight;
    }

    /** Resoluções comuns oferecidas nas opções, filtradas pelo monitor. */
    private static final int[][] COMUNS = {
            {1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1080},
            {2560, 1440}, {3440, 1440}, {3840, 2160}
    };

    /**
     * As resoluções que este monitor mostra: as comuns que cabem nele, mais a dele
     * mesmo (que pode ser uma que a lista não tem, como 1680x1050 ou 3440x1440).
     * Oferecer 4K num monitor 1080p era oferecer uma opção que não podia valer.
     */
    public List<Dimension> resolucoesDoMonitor() {
        if (monitorW == 0) {
            atualizarMonitor();
        }
        List<Dimension> lista = new ArrayList<>();
        for (int[] r : COMUNS) {
            if (r[0] <= monitorW && r[1] <= monitorH) {
                lista.add(new Dimension(r[0], r[1]));
            }
        }
        Dimension nativa = new Dimension(monitorW, monitorH);
        if (monitorW > 0 && !lista.contains(nativa)) {
            lista.add(nativa);
        }
        lista.sort((a, b) -> a.width != b.width ? Integer.compare(a.width, b.width)
                : Integer.compare(a.height, b.height));
        return lista;
    }

    /** O tamanho do monitor da janela, em pixels. */
    public Dimension tamanhoDoMonitor() {
        if (monitorW == 0) {
            atualizarMonitor();
        }
        return new Dimension(monitorW, monitorH);
    }

    public Dimension getScreenSize() {
        return toolkit.getScreenSize();
    }

}
