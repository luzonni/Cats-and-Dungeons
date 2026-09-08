import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Editor de pose das armas: poe a arma na mao do gato ARRASTANDO, e o jogo reflete
 * na hora.
 *
 * POR QUE ELE EXISTE. Ajustar onde uma arma fica na mao por conta e screenshot e
 * lento e erra — cada arma tem uma forma, e o ponto onde ela e segurada nao sai de
 * calculo nenhum, sai de olhar. Aqui a arma e arrastada com o mouse ate parecer
 * certo, e o resultado vira dado em
 * {@code src/main/resources/com/retronova/resources/poses.properties}. O jogo
 * releva esse arquivo de meio em meio segundo, entao da para deixar o jogo aberto
 * do lado e ver a mudanca sem recompilar nada.
 *
 * A MATEMATICA E A MESMA DO JOGO, de proposito, copiada de Item.naMao e de
 * Player.getMao: o gato e desenhado no mesmo lugar, a mao no mesmo pixel, e a arma
 * girada em volta do mesmo punho. Se as duas contas divergirem, o editor vira
 * mentira — entao ela e curta e esta anotada dos dois lados.
 *
 * COMO USAR
 *   arrastar com o botao esquerdo ....... move a arma
 *   roda do mouse ....................... gira (Shift para ir de 5 em 5)
 *   botao direito em cima da arma ....... define o PUNHO ali (onde a mao segura)
 *   Ctrl+Z .............................. desfaz
 *   o arquivo e salvo sozinho a cada mexida
 *
 * Uso: java tools/EditorDePose.java
 */
public class EditorDePose {

    private static final String RES = "src/main/resources/com/retronova/resources/";
    private static final String ITENS = RES + "sprites/items/";
    private static final String GATOS = RES + "sprites/objects/player/";
    private static final Path ARQUIVO = Path.of(RES + "poses.properties");

    /** O quadro de arte de todo item e de todo gato. */
    private static final int QUADRO = 16;

    /**
     * Itens em espera: nao entram na lista.
     *
     * Sao os consumiveis e as bombas, tirados da loja enquanto o trabalho e nas
     * armas. Ver EM_ESPERA em Seller.java — as duas listas dizem a mesma coisa e
     * precisam andar juntas; aqui e para o editor nao mostrar o que o jogo nao
     * mostra mais.
     */
    private static final java.util.Set<String> EM_ESPERA = java.util.Set.of(
            "feed", "acorn", "catnip", "watermelon", "ima", "bomb", "gasbomb");

    /** Onde fica a mao, no quadro do gato. Copiado de Player.MAO_X / MAO_Y. */
    private static final int MAO_X = 11, MAO_Y = 13;

    static final class Pose {
        int dx, dy, graus, punhoX, punhoY;
        /** Vira o desenho na horizontal. Nao e giro: giro deitaria a arma. */
        boolean espelhar;
        /** Quanto a arma gira e avanca no auge do golpe. */
        int golpeGraus = 80, golpePx = 4;
        /** O pixel do desenho de onde o tiro sai. -1 = miolo do desenho. */
        int bocaX = -1, bocaY = -1;

        Pose(int dx, int dy, int graus, int punhoX, int punhoY, boolean espelhar) {
            this.dx = dx;
            this.dy = dy;
            this.graus = graus;
            this.punhoX = punhoX;
            this.punhoY = punhoY;
            this.espelhar = espelhar;
        }

        Pose copia() {
            Pose c = new Pose(dx, dy, graus, punhoX, punhoY, espelhar);
            c.golpeGraus = golpeGraus;
            c.golpePx = golpePx;
            c.bocaX = bocaX;
            c.bocaY = bocaY;
            return c;
        }

        @Override
        public String toString() {
            return dx + ", " + dy + ", " + graus + ", " + punhoX + ", " + punhoY
                    + ", " + (espelhar ? 1 : 0) + ", " + golpeGraus + ", " + golpePx
                    + ", " + bocaX + ", " + bocaY;
        }
    }

    private final Map<String, Pose> poses = new LinkedHashMap<>();
    private final List<String> armas = new ArrayList<>();
    private final Map<String, BufferedImage> sprites = new LinkedHashMap<>();
    private final Map<String, BufferedImage> gatos = new LinkedHashMap<>();
    private final Map<String, BufferedImage> flechas = new LinkedHashMap<>();
    private final List<Runnable> desfazer = new ArrayList<>();

    private String arma;
    private String gato = "muffin";
    private boolean espelhado;
    private int escala = 14;

    private Tela tela;
    private JLabel numeros;
    private JSpinner giro;
    private JCheckBox espelhoDaArte;
    private JSpinner golpeGiro, golpeAvanco;

    /**
     * A animacao do golpe.
     *
     * {@code peso} escolhe qual Investida imitar — leve, media ou pesada, os
     * mesmos tempos de Item.Investida. {@code t} e o tick dentro dela; com a
     * animacao parada o t vem da barra, para se deter no quadro que se quer
     * julgar. E no quadro parado que se ve se a pose esta boa: passando rapido,
     * qualquer coisa parece aceitavel.
     */
    private int[] peso = {15, 3, 18, 12};
    private int t = -1;
    private boolean rodando;
    private JSlider linhaDoTempo;
    private JSlider mira;
    private JLabel modo;
    /** Evita que atualizar() reescreva o campo e dispare o proprio listener. */
    private boolean travarGiro;

    public static void main(String[] args) throws Exception {
        if (!new File(ITENS).isDirectory()) {
            System.err.println("Rode a partir da raiz do repositorio.");
            System.exit(1);
        }
        EditorDePose e = new EditorDePose();
        e.carregar();
        SwingUtilities.invokeLater(e::montarJanela);
    }

    // ------------------------------------------------------------- carga

    private void carregar() throws IOException {
        File[] arquivos = new File(ITENS).listFiles((d, n) -> n.endsWith(".png"));
        if (arquivos != null) {
            java.util.Arrays.sort(arquivos);
            for (File f : arquivos) {
                BufferedImage im = ImageIO.read(f);
                if (im.getWidth() != QUADRO || im.getHeight() != QUADRO) {
                    continue;              // folhas de varios quadros ficam de fora
                }
                String nome = f.getName().substring(0, f.getName().length() - 4);
                if (EM_ESPERA.contains(nome)) {
                    continue;
                }
                armas.add(nome);
                sprites.put(nome, im);
            }
        }
        File pastaFlechas = new File(RES + "sprites/objects/utility");
        File[] fs = pastaFlechas.listFiles((d, n) -> n.startsWith("arrow") && n.endsWith(".png"));
        if (fs != null) {
            for (File f : fs) {
                flechas.put(f.getName().substring(0, f.getName().length() - 4), ImageIO.read(f));
            }
        }
        for (String g : new String[]{"muffin", "azrael", "finn"}) {
            File f = new File(GATOS + "player_" + g + "_idle.png");
            if (f.isFile()) {
                BufferedImage folha = ImageIO.read(f);
                gatos.put(g, folha.getSubimage(0, 0, QUADRO, Math.min(QUADRO, folha.getHeight())));
            }
        }
        if (Files.isRegularFile(ARQUIVO)) {
            Properties p = new Properties();
            try (var in = Files.newBufferedReader(ARQUIVO, StandardCharsets.UTF_8)) {
                p.load(in);
            }
            for (String chave : p.stringPropertyNames()) {
                String[] c = p.getProperty(chave).split(",");
                if (c.length == 5 || c.length == 6 || c.length == 8 || c.length == 10) {
                    Pose pose = new Pose(n(c[0]), n(c[1]), n(c[2]), n(c[3]), n(c[4]),
                            c.length >= 6 && n(c[5]) != 0);
                    if (c.length >= 8) {
                        pose.golpeGraus = n(c[6]);
                        pose.golpePx = n(c[7]);
                    }
                    if (c.length == 10) {
                        pose.bocaX = n(c[8]);
                        pose.bocaY = n(c[9]);
                    }
                    poses.put(chave.trim(), pose);
                }
            }
        }
        arma = armas.isEmpty() ? null : armas.get(0);
    }

    private static int n(String s) {
        return Integer.parseInt(s.trim());
    }

    /**
     * A pose de uma arma que ainda nao foi ajustada.
     *
     * O chute inicial e o mesmo que o jogo usa sem arquivo: punho no pixel opaco
     * mais proximo do canto de baixo a direita, que e onde estes pacotes desenham
     * o cabo. Comecar do padrao e melhor que comecar do zero — quase sempre so
     * falta um empurrao de um ou dois pixels.
     */
    private Pose poseDe(String nome) {
        return poses.computeIfAbsent(nome, k -> {
            BufferedImage im = sprites.get(k);
            int px = QUADRO - 1, py = QUADRO - 1, melhor = Integer.MAX_VALUE;
            for (int y = 0; y < im.getHeight(); y++) {
                for (int x = 0; x < im.getWidth(); x++) {
                    if ((im.getRGB(x, y) >>> 24) <= 16) {
                        continue;
                    }
                    int custo = (QUADRO - 1 - x) + (QUADRO - 1 - y);
                    if (custo < melhor) {
                        melhor = custo;
                        px = x;
                        py = y;
                    }
                }
            }
            int[] porte = porteDe(k);
            return new Pose(porte[1], porte[2], porte[0] + 45, px, py, false);
        });
    }

    /**
     * Como esta arma ATACA. Cada familia tem um movimento proprio no jogo, e o
     * editor mostrava o do machado para todas — o que fazia a previa mentir para
     * quase todo mundo.
     *
     *   machado  investida: recua, avanca e volta, girando junto (Item.Investida)
     *   espada   varredura: o gume passa de um lado ao outro, alternando o lado
     *   mira     nao ha golpe; a arma aponta para o alvo (arco, laser, tridente)
     *   varinha  fica parada e sai um raio dela
     *   miudo    nao tem ataque proprio
     */
    private static String modoDe(String nome) {
        if (nome.startsWith("bow") || nome.equals("laser")) {
            return "mira";
        }
        if (nome.startsWith("sword")) {
            return "espada";
        }
        if (nome.equals("trident")) {
            return "arremesso";
        }
        if (nome.startsWith("axe") || nome.equals("bloody_axe")) {
            return "machado";
        }
        if (nome.startsWith("wand") || nome.equals("magicstick")) {
            return "varinha";
        }
        return "miudo";
    }

    /**
     * Quanto o desenho desta arma ja esta girado em repouso. Ver Rotate.
     *
     * NAO E IGUAL PARA TODAS. Os pacotes de icone de RPG desenham na diagonal, mas
     * os de arma de fogo desenham DEITADO, apontando para a direita — e o laser
     * vem de um desses. Tratando os dois como diagonais, a busca pela ponta olhava
     * quarenta e cinco graus fora e parava na culatra da pistola; era isso que
     * fazia o feixe sair por baixo da arma em vez de pela boca.
     */
    private static double facingDe(String nome) {
        return nome.equals("laser") || nome.equals("kunai")
                ? 0 : 3 * Math.PI / 4;
    }

    /** O centro dos pixels opacos. Mesma conta de Rotate.centroDoDesenho. */
    private static Point centroDoDesenho(BufferedImage im) {
        int x0 = 99, y0 = 99, x1 = -1, y1 = -1;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                if ((im.getRGB(x, y) >>> 24) > 16) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        return x1 < 0 ? new Point(8, 8) : new Point((x0 + x1) / 2, (y0 + y1) / 2);
    }

    /**
     * O porte padrao da arma: {graus, dx, dy}. Copiado de Item.Porte.
     *
     * Precisa bater com o jogo, senao o editor abre mostrando uma pose e o jogo
     * mostra outra — e ai o ajuste comeca de um lugar que ninguem pediu. Os 45
     * graus somados em cima vem de Item.grausDaArte: a arte destes pacotes ja vem
     * inclinada, e o Porte foi calculado para arma em pe.
     */
    private static int[] porteDe(String nome) {
        if (nome.startsWith("axe") || nome.equals("bloody_axe") || nome.equals("trident")) {
            return new int[]{62, 2, 1};       // DUAS_MAOS
        }
        if (nome.startsWith("wand") || nome.equals("magicstick")) {
            return new int[]{8, 3, -1};       // CAJADO
        }
        if (nome.startsWith("sword") || nome.equals("sickle")
                || nome.equals("kunai") || nome.equals("claw_blades")) {
            return new int[]{25, 2, 0};       // UMA_MAO
        }
        return new int[]{0, 1, 0};            // MIUDO
    }

    // ------------------------------------------------------------- janela

    private void montarJanela() {
        JFrame f = new JFrame("Editor de pose — Cats & Dungeons");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JList<String> lista = new JList<>(armas.toArray(new String[0]));
        lista.setSelectedValue(arma, true);
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && lista.getSelectedValue() != null) {
                arma = lista.getSelectedValue();
                atualizar();
            }
        });
        // A LISTA NAO PODE RECEBER FOCO. Uma JList com foco engole as teclas: as
        // setas trocam de item em vez de mover a arma, e qualquer letra faz busca
        // — apertar F pulava para o primeiro item com F em vez de espelhar.
        lista.setFocusable(false);
        JScrollPane rolagem = new JScrollPane(lista);
        rolagem.setPreferredSize(new Dimension(200, 0));

        tela = new Tela();

        JComboBox<String> quemGato = new JComboBox<>(gatos.keySet().toArray(new String[0]));
        quemGato.addActionListener(e -> {
            gato = (String) quemGato.getSelectedItem();
            atualizar();
        });
        JCheckBox virado = new JCheckBox("olhando para a esquerda");
        virado.setToolTipText("Vira o gato. Arco e laser não acompanham: eles seguem a mira.");
        virado.addActionListener(e -> {
            espelhado = virado.isSelected();
            atualizar();
        });
        JSlider zoom = new JSlider(6, 26, escala);
        zoom.addChangeListener((ChangeEvent e) -> {
            escala = zoom.getValue();
            atualizar();
        });
        // O GIRO PRECISA DE UM CAMPO VISIVEL. Roda do mouse e comodo para quem ja
        // sabe, mas nao se descobre olhando — e girar e a coisa que mais se mexe
        // aqui. O campo mostra o numero, aceita digitar e tem as setinhas.
        giro = new JSpinner(new SpinnerNumberModel(0, -360, 360, 1));
        ((JSpinner.DefaultEditor) giro.getEditor()).getTextField().setColumns(4);
        giro.addChangeListener(e -> {
            if (arma == null || travarGiro) {
                return;
            }
            Pose p = poseDe(arma);
            int novo = (Integer) giro.getValue();
            if (novo != p.graus) {
                registrarDesfazer();
                p.graus = novo;
                salvar();
                atualizar();
            }
        });

        espelhoDaArte = new JCheckBox("espelhar arte (F)");
        espelhoDaArte.addActionListener(e -> {
            if (arma == null || travarGiro) {
                return;
            }
            registrarDesfazer();
            poseDe(arma).espelhar = espelhoDaArte.isSelected();
            salvar();
            atualizar();
        });

        golpeGiro = new JSpinner(new SpinnerNumberModel(80, -360, 360, 5));
        golpeAvanco = new JSpinner(new SpinnerNumberModel(4, -32, 32, 1));
        ((JSpinner.DefaultEditor) golpeGiro.getEditor()).getTextField().setColumns(4);
        ((JSpinner.DefaultEditor) golpeAvanco.getEditor()).getTextField().setColumns(3);
        golpeGiro.addChangeListener(e -> mudarGolpe());
        golpeAvanco.addChangeListener(e -> mudarGolpe());

        JButton padrao = new JButton("Voltar ao padrão");
        padrao.addActionListener(e -> {
            registrarDesfazer();
            poses.remove(arma);
            salvar();
            atualizar();
        });

        numeros = new JLabel(" ");
        numeros.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("gato:"));
        topo.add(quemGato);
        topo.add(virado);
        topo.add(new JLabel("graus:"));
        topo.add(giro);
        topo.add(espelhoDaArte);
        topo.add(new JLabel("zoom:"));
        topo.add(zoom);
        topo.add(padrao);

        JComboBox<String> qualPeso = new JComboBox<>(new String[]{"leve", "media", "pesada"});
        qualPeso.setSelectedIndex(2);
        qualPeso.addActionListener(e -> {
            int i = qualPeso.getSelectedIndex();
            peso = i == 0 ? new int[]{6, 3, 6, 3}
                    : i == 1 ? new int[]{12, 3, 9, 9} : new int[]{15, 3, 18, 12};
            linhaDoTempo.setMaximum(total() - 1);
            atualizar();
        });
        JToggleButton tocar = new JToggleButton("atacar");
        linhaDoTempo = new JSlider(0, total() - 1, 0);
        linhaDoTempo.setPreferredSize(new Dimension(240, 20));
        linhaDoTempo.addChangeListener(e -> {
            if (!rodando) {
                t = linhaDoTempo.getValue();
                atualizar();
            }
        });
        Timer relogio = new Timer(1000 / 60, e -> {
            if (rodando) {
                t = (t + 1) % total();
                linhaDoTempo.setValue(t);
                atualizar();
            }
        });
        tocar.addActionListener(e -> {
            rodando = tocar.isSelected();
            if (rodando) {
                t = 0;
                relogio.start();
            } else {
                relogio.stop();
                t = -1;
                atualizar();
            }
        });

        mira = new JSlider(-180, 180, 0);
        mira.setPreferredSize(new Dimension(180, 20));
        mira.addChangeListener(e -> atualizar());

        modo = new JLabel();

        JPanel golpe = new JPanel(new FlowLayout(FlowLayout.LEFT));
        golpe.add(modo);
        golpe.add(new JLabel("golpe:"));
        golpe.add(tocar);
        golpe.add(qualPeso);
        golpe.add(linhaDoTempo);
        golpe.add(new JLabel("giro:"));
        golpe.add(golpeGiro);
        golpe.add(new JLabel("avanco:"));
        golpe.add(golpeAvanco);
        golpe.add(new JLabel("direção do alvo (só prévia):"));
        golpe.add(mira);

        JPanel barras = new JPanel(new GridLayout(2, 1));
        barras.add(topo);
        barras.add(golpe);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(numeros, BorderLayout.WEST);
        rodape.add(new JLabel("  arraste ou setas = mover · roda / Q E / campo \"graus\" = girar"
                + " (Shift ×5) · F = espelhar · ponto amarelo = de onde sai o tiro (automático) · "
                + "botão direito = punho · Ctrl+Z = desfazer  "),
                BorderLayout.EAST);

        f.setLayout(new BorderLayout());
        f.add(rolagem, BorderLayout.WEST);
        f.add(tela, BorderLayout.CENTER);
        f.add(barras, BorderLayout.NORTH);
        f.add(rodape, BorderLayout.SOUTH);
        f.setSize(1040, 720);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        atualizar();
    }

    private void atualizar() {
        if (arma != null) {
            Pose p = poseDe(arma);
            numeros.setText(String.format(" %-16s  dx=%3d  dy=%3d  graus=%4d  punho=(%2d,%2d)",
                    arma, p.dx, p.dy, p.graus, p.punhoX, p.punhoY));
            if (modo != null) {
                String m = modoDe(arma);
                // Arma de mira nao acompanha o lado do gato: ela e posta na
                // direcao do alvo. A caixinha "olhando para a esquerda" so vira o
                // gato, e sem dizer isso a previa parece travada.
                modo.setText(m.equals("mira")
                        ? "  [mira — segue a mira, não o lado do gato]  "
                        : "  [" + m + "]  ");
                boolean deMira = m.equals("mira");
                mira.setEnabled(deMira || m.equals("varinha") || m.equals("arremesso"));
                boolean temGolpe = m.equals("machado") || m.equals("espada");
                golpeGiro.setEnabled(temGolpe);
                golpeAvanco.setEnabled(temGolpe);
                linhaDoTempo.setEnabled(true);
            }
            if (giro != null) {
                travarGiro = true;
                giro.setValue(p.graus);
                espelhoDaArte.setSelected(p.espelhar);
                golpeGiro.setValue(p.golpeGraus);
                golpeAvanco.setValue(p.golpePx);
                travarGiro = false;
            }
        }
        if (tela != null) {
            tela.repaint();
        }
    }

    private void mudarGolpe() {
        if (arma == null || travarGiro) {
            return;
        }
        registrarDesfazer();
        Pose p = poseDe(arma);
        p.golpeGraus = (Integer) golpeGiro.getValue();
        p.golpePx = (Integer) golpeAvanco.getValue();
        salvar();
        atualizar();
    }

    private int total() {
        if (arma == null) {
            return peso[0] + peso[1] + peso[2] + peso[3];
        }
        String m = modoDe(arma);
        if (m.equals("espada")) {
            return 2 * (LEVE[0] + LEVE[1] + LEVE[2] + LEVE[3]);   // ida e volta
        }
        if (m.equals("mira")) {
            return ENCAIXE + VOO;
        }
        if (m.equals("arremesso")) {
            return SEGURA + IDA * 2;
        }
        return peso[0] + peso[1] + peso[2] + peso[3];
    }

    /**
     * O tiro do arco, em ticks: encaixar a flecha e depois solta-la.
     *
     * No jogo o countShot vai de um a cinco enquanto a corda estica, e no cinco
     * a flecha sai. Aqui o encaixe ocupa vinte e cinco quadros, cinco por passo,
     * e o voo mais vinte — o bastante para ver a flecha deixar o arco, que era
     * justamente o que nao dava para conferir.
     */
    private static final int ENCAIXE = 25, VOO = 20;

    /**
     * O arremesso do tridente: um tempo na mao, a ida e a volta.
     *
     * No jogo ele viaja seis tiles, o que aqui sairia da tela; a previa usa um
     * alcance curto so para mostrar o GESTO — sai da mao, vai reto e volta reto
     * pela mesma linha, que e o que ThrownTrident faz.
     */
    private static final int SEGURA = 10, IDA = 20;
    private static final double ALCANCE_DA_PREVIA = 13;

    /** Os tempos da Investida leve, que e a que a espada usa. */
    private static final int[] LEVE = {6, 3, 6, 3};

    /**
     * De que lado a espada vem no quadro atual: +1 e depois -1.
     *
     * A espada passou a usar a mesma Investida do machado, so que na versao leve,
     * alternando o lado a cada golpe — forehand e backhand. Antes ela dava uma
     * volta de 180 graus em velocidade constante, o que nao le como golpe.
     */
    private int ladoDoGolpe() {
        if (arma == null || !modoDe(arma).equals("espada") || t < 0) {
            return 1;
        }
        return t < LEVE[0] + LEVE[1] + LEVE[2] + LEVE[3] ? 1 : -1;
    }

    /**
     * A curva do golpe, de -1 a +1. Copiada de Item.Investida.avanco().
     *
     * Ela e o que da o peso da pancada: recua devagar durante o preparo, cruza a
     * distancia inteira em tres ticks, segue adiante quase parada e volta. Se o
     * editor usasse uma curva mais simples, a previa mentiria justamente no que
     * importa — os quadros do impacto.
     */
    private double avanco() {
        if (t < 0) {
            return 0;
        }
        boolean espada = arma != null && modoDe(arma).equals("espada");
        int[] usado = espada ? LEVE : peso;
        int ciclo = usado[0] + usado[1] + usado[2] + usado[3];
        int t = espada ? this.t % ciclo : this.t;
        int preparo = usado[0], corte = usado[1], extensao = usado[2], recuperacao = usado[3];
        if (t < preparo) {
            return -Math.sin((t / (double) preparo) * Math.PI / 2);
        }
        if (t < preparo + corte) {
            return -1 + 2 * ((t - preparo) / (double) corte);
        }
        if (t < preparo + corte + extensao) {
            return 1 - 0.15 * ((t - preparo - corte) / (double) extensao);
        }
        return 0.85 * (1 - (t - preparo - corte - extensao)
                / (double) Math.max(1, recuperacao));
    }

    private void registrarDesfazer() {
        Pose atual = poses.containsKey(arma) ? poses.get(arma).copia() : null;
        String qual = arma;
        desfazer.add(() -> {
            if (atual == null) {
                poses.remove(qual);
            } else {
                poses.put(qual, atual);
            }
        });
        while (desfazer.size() > 100) {
            desfazer.remove(0);
        }
    }

    private void salvar() {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(ARQUIVO, StandardCharsets.UTF_8))) {
            w.println("# Onde cada arma fica na mao do gato.");
            w.println("# Escrito por tools/EditorDePose.java e lido por game/items/Poses.java.");
            w.println("# nome = dx, dy, graus, punhoX, punhoY, espelhar, golpeGraus, golpePx, bocaX, bocaY");
            for (Map.Entry<String, Pose> e : poses.entrySet()) {
                w.println(e.getKey() + " = " + e.getValue());
            }
        } catch (IOException naoDeu) {
            System.err.println("Nao consegui salvar: " + naoDeu);
        }
    }

    // ------------------------------------------------------------- desenho

    private final class Tela extends JPanel {

        private Point arrastando;
        private boolean arrastandoBoca;
        /** O mouse esta em cima da bolinha. So para acender o realce. */
        private boolean pertoDaBoca;
        private int dxInicial, dyInicial;

        Tela() {
            setBackground(new Color(0x2A2634));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (arma == null) {
                        return;
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        definirPunho(e.getPoint());
                        return;
                    }
                    registrarDesfazer();
                    Pose p = poseDe(arma);
                    arrastandoBoca = false;
                    arrastando = e.getPoint();
                    dxInicial = p.dx;
                    dyInicial = p.dy;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (arrastando == null || arma == null) {
                        return;
                    }
                    Pose p = poseDe(arma);
                    double telaX = e.getX() - arrastando.x, telaY = e.getY() - arrastando.y;
                    if (modoDe(arma).equals("mira")) {
                        // EM MODO DE MIRA os campos nao sao "para a direita" e
                        // "para baixo", e sim "a frente" e "para o lado" — a arma
                        // e posta em relacao ao alvo. Arrastando sem projetar, o
                        // valor gravado so batia com o gesto quando a mira estava
                        // apontada para a direita.
                        double ang = Math.toRadians(mira.getValue());
                        double c = Math.cos(ang), sn = Math.sin(ang);
                        p.dx = dxInicial + (int) Math.round((telaX * c + telaY * sn) / escala);
                        p.dy = dyInicial + (int) Math.round((-telaX * sn + telaY * c) / escala);
                    } else {
                        int lado = espelhado ? -1 : 1;
                        p.dx = dxInicial + (int) Math.round(telaX / escala) * lado;
                        p.dy = dyInicial + (int) Math.round(telaY / escala);
                    }
                    atualizar();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (arrastando != null) {
                        arrastando = null;
                        salvar();
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (arma == null) {
                        return;
                    }
                    registrarDesfazer();
                    Pose p = poseDe(arma);
                    p.graus += e.getWheelRotation() * (e.isShiftDown() ? 5 : 1);
                    salvar();
                    atualizar();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
            addMouseWheelListener(m);

            InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            // Teclado para o ajuste fino: arrastar acerta o lugar, mas o ultimo
            // pixel e o ultimo grau saem melhor de tecla que de mouse.
            tecla(im, KeyEvent.VK_LEFT, 0, () -> mover(-1, 0));
            tecla(im, KeyEvent.VK_RIGHT, 0, () -> mover(1, 0));
            tecla(im, KeyEvent.VK_UP, 0, () -> mover(0, -1));
            tecla(im, KeyEvent.VK_DOWN, 0, () -> mover(0, 1));
            tecla(im, KeyEvent.VK_Q, 0, () -> girar(-1));
            tecla(im, KeyEvent.VK_E, 0, () -> girar(1));
            tecla(im, KeyEvent.VK_F, 0, () -> poseDe(arma).espelhar = !poseDe(arma).espelhar);
            // Em forma de TEXTO. Com KeyStroke.getKeyStroke(VK_Q, SHIFT_DOWN_MASK)
            // o atalho nao disparava; a forma "shift Q" e a que a Swing resolve
            // igual a que ela mesma monta ao receber a tecla.
            tecla(im, "shift Q", () -> girar(-5));
            tecla(im, "shift E", () -> girar(5));
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "desfazer");
            getActionMap().put("desfazer", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!desfazer.isEmpty()) {
                        desfazer.remove(desfazer.size() - 1).run();
                        salvar();
                        atualizar();
                    }
                }
            });
        }

        private void tecla(InputMap im, String atalho, Runnable acao) {
            ligar(im, KeyStroke.getKeyStroke(atalho), "acao_" + atalho, acao);
        }

        private void tecla(InputMap im, int codigo, int mod, Runnable acao) {
            ligar(im, KeyStroke.getKeyStroke(codigo, mod), "acao" + codigo + "_" + mod, acao);
        }

        private void ligar(InputMap im, KeyStroke atalho, String nome, Runnable acao) {
            im.put(atalho, nome);
            getActionMap().put(nome, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (arma != null) {
                        registrarDesfazer();
                        acao.run();
                        salvar();
                        atualizar();
                    }
                }
            });
        }

        private void mover(int dx, int dy) {
            Pose p = poseDe(arma);
            p.dx += dx;
            p.dy += dy;
        }

        private void girar(int graus) {
            poseDe(arma).graus += graus;
        }

        /** Onde o quadro do gato comeca, em pixels de tela. */
        private Point cantoDoGato() {
            return new Point(getWidth() / 2 - QUADRO * escala / 2,
                    getHeight() / 2 - QUADRO * escala / 2);
        }

        /** A mao, em pixels de tela. Mesma conta de Player.getMao. */
        private Point mao() {
            Point c = cantoDoGato();
            int coluna = espelhado ? QUADRO - 1 - MAO_X : MAO_X;
            return new Point(c.x + coluna * escala + escala / 2,
                    c.y + MAO_Y * escala + escala / 2);
        }

        /**
         * Onde o tiro sai, marcado no proprio desenho.
         *
         * Guardado como PIXEL DO SPRITE e nao como deslocamento na tela: assim a
         * boca gira junto com a arma. Guardada como "tantos pixels a frente do
         * gato", ela ficaria certa mirando para um lado e errada em todos os
         * outros — e era isso que nao dava para configurar antes.
         */
        private void definirBoca(Point clique) {
            Pose p = poseDe(arma);
            Point pixel = pixelDoDesenho(clique, p);
            if (pixel == null) {
                return;
            }
            p.bocaX = (espelhado ^ p.espelhar) ? QUADRO - 1 - pixel.x : pixel.x;
            p.bocaY = pixel.y;
            atualizar();
        }

        /**
         * De um clique na tela para o pixel do desenho — DESFAZENDO a rotacao.
         *
         * A arma e desenhada girada em volta de um pivo, entao dividir o clique
         * pela escala sem antes girar de volta da um pixel que nao tem relacao com
         * o que esta debaixo do cursor. Na pratica o resultado caia fora do quadro
         * de dezesseis quase sempre, o metodo desistia, e o efeito era arrastar a
         * bolinha e ela nao sair do lugar.
         *
         * A conta e a inversa exata da transformada do desenho: tira a origem do
         * quadro, tira o pivo, gira por MENOS o angulo, devolve o pivo, e so
         * entao divide pela escala.
         */
        private Point pixelDoDesenho(Point clique, Pose p) {
            boolean deMira = m().equals("mira");
            Point onde = deMira ? cantoNaMira(p) : ondeAArmaEsta(p);
            double pivoX, pivoY, giro;
            if (deMira) {
                Point miolo = centroDoDesenho(sprites.get(arma));
                pivoX = miolo.x * escala + escala / 2.0;
                pivoY = miolo.y * escala + escala / 2.0;
                giro = Math.toRadians(mira.getValue() + p.graus) + facingDe(arma);
            } else {
                int lado = espelhado ? -1 : 1;
                int coluna = (espelhado ^ p.espelhar) ? QUADRO - 1 - p.punhoX : p.punhoX;
                pivoX = coluna * escala + escala / 2.0;
                pivoY = p.punhoY * escala + escala / 2.0;
                giro = Math.toRadians(p.graus * lado);
            }
            double lx = clique.x - onde.x - pivoX;
            double ly = clique.y - onde.y - pivoY;
            double c = Math.cos(-giro), sn = Math.sin(-giro);
            double rx = lx * c - ly * sn + pivoX;
            double ry = lx * sn + ly * c + pivoY;
            int x = (int) Math.floor(rx / escala);
            int y = (int) Math.floor(ry / escala);
            return (x < 0 || y < 0 || x >= QUADRO || y >= QUADRO) ? null : new Point(x, y);
        }

        private String m() {
            return modoDe(arma);
        }

        /** O ponto onde uma arma apontada e ancorada — igual ao do jogo. */
        private java.awt.geom.Point2D.Double ancoraDeMira(Pose p) {
            double ang = Math.toRadians(mira.getValue());
            Point centro = centroDoGato();
            double dist = p.dx * escala;
            double lateral = p.dy * escala;
            return new java.awt.geom.Point2D.Double(
                    centro.x + Math.cos(ang) * dist + Math.cos(ang + Math.PI / 2) * lateral,
                    centro.y + Math.sin(ang) * dist + Math.sin(ang + Math.PI / 2) * lateral);
        }

        /** O canto do quadro da arma quando ela esta em modo de mira. */
        private Point cantoNaMira(Pose p) {
            java.awt.geom.Point2D.Double a = ancoraDeMira(p);
            double ax = a.x;
            double ay = a.y;
            Point miolo = centroDoDesenho(sprites.get(arma));
            return new Point((int) (ax - (miolo.x * escala + escala / 2.0)),
                    (int) (ay - (miolo.y * escala + escala / 2.0)));
        }

        private void definirPunho(Point clique) {
            Pose p = poseDe(arma);
            Point pixel = pixelDoDesenho(clique, p);
            if (pixel == null) {
                return;
            }
            int x = pixel.x, y = pixel.y;
            registrarDesfazer();
            // O clique vem da tela, ja espelhado; o arquivo guarda sempre a coluna
            // do desenho NAO espelhado, senao a pose mudaria de sentido conforme o
            // lado em que ela tivesse sido ajustada.
            p.punhoX = (espelhado ^ p.espelhar) ? QUADRO - 1 - x : x;
            p.punhoY = y;
            salvar();
            atualizar();
        }

        /** O canto de cima a esquerda do quadro da arma, em pixels de tela. */
        private Point ondeAArmaEsta(Pose p) {
            Point mao = mao();
            int lado = espelhado ? -1 : 1;
            int coluna = (espelhado ^ p.espelhar) ? QUADRO - 1 - p.punhoX : p.punhoX;
            int punhoX = coluna * escala + escala / 2;
            int punhoY = p.punhoY * escala + escala / 2;
            // O avanco do golpe entra aqui, no mesmo lugar em que Item.naMao o
            // soma: no dx, e no sentido para onde o gato olha.
            // No arremesso a arma fica PARADA na mao ate ser jogada: aplicar a
            // curva de golpe ali fazia o tridente dar uma machadada antes de sair,
            // que foi o "se comporta como uma espada" reportado.
            String mm = modoDe(arma);
            double extra = (mm.equals("espada") || mm.equals("machado"))
                    ? avanco() * ladoDoGolpe() * p.golpePx : 0;
            return new Point((int) (mao.x - punhoX + (p.dx + extra) * escala * lado),
                    mao.y - punhoY + p.dy * escala);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            Point c = cantoDoGato();

            g.setColor(new Color(0x39344A));
            for (int i = 0; i <= QUADRO; i++) {
                g.drawLine(c.x + i * escala, c.y, c.x + i * escala, c.y + QUADRO * escala);
                g.drawLine(c.x, c.y + i * escala, c.x + QUADRO * escala, c.y + i * escala);
            }

            BufferedImage bicho = gatos.get(gato);
            if (bicho != null) {
                BufferedImage b = espelhado ? espelhar(bicho) : bicho;
                g.drawImage(b, c.x, c.y, QUADRO * escala, QUADRO * escala, null);
            }
            if (arma == null) {
                return;
            }
            Pose p = poseDe(arma);
            BufferedImage sp = sprites.get(arma);
            String m = modoDe(arma);
            if (m.equals("arremesso") && t >= SEGURA) {
                desenharArremesso(g, sp);
                marcarMao(g);
                return;
            }
            if (m.equals("mira")) {
                desenharMirando(g, sp);
                marcarBoca(g);
                marcarMao(g);
                return;
            }
            boolean virar = espelhado ^ p.espelhar;
            if (virar) {
                sp = espelhar(sp);
            }
            int lado = espelhado ? -1 : 1;
            int coluna = virar ? QUADRO - 1 - p.punhoX : p.punhoX;
            double pivoX = coluna * escala + escala / 2.0;
            double pivoY = p.punhoY * escala + escala / 2.0;

            Point onde = ondeAArmaEsta(p);
            // Varinha e tridente ficam PARADOS na mao: um atira, o outro e jogado.
            // Aplicar a curva de golpe neles fazia a varinha dar machadadas no
            // preview, coisa que ela nunca faz no jogo.
            boolean golpeia = m.equals("espada") || m.equals("machado");
            double golpe = golpeia ? avanco() * ladoDoGolpe() * p.golpeGraus : 0;
            double giroRad = Math.toRadians((p.graus + golpe) * lado);

            // Mesma transformada de Rotate.draw: translada e gira em volta do punho.
            Graphics2D g2 = (Graphics2D) g.create();
            AffineTransform at = new AffineTransform();
            at.translate(onde.x, onde.y);
            at.rotate(giroRad, pivoX, pivoY);
            at.scale(escala, escala);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(sp, at, null);
            g2.dispose();

            if (m.equals("varinha")) {
                desenharRaio(g);
            }

            marcarBoca(g);
            marcarMao(g);
        }

        /**
         * A ponta da arma, em amarelo — de onde o tiro sai.
         *
         * So aparece nas armas que atiram, e nao ha o que configurar: e o pixel
         * mais adiantado do desenho na direcao da mira, achado na hora. O ponto
         * esta aqui para CONFERIR, nao para escolher.
         */
        private void marcarBoca(Graphics2D g) {
            if (!atira()) {
                return;
            }
            java.awt.geom.Point2D.Double b = ondeSaiOTiro();
            g.setColor(new Color(0xFFD84F));
            g.fillOval((int) b.x - 3, (int) b.y - 3, 7, 7);
            g.setColor(new Color(0x1A1420));
            g.drawOval((int) b.x - 3, (int) b.y - 3, 7, 7);
        }

        /**
         * O clique pegou na bolinha?
         *
         * A margem acompanha o ZOOM. Estava fixa em sete pixels de tela: no zoom
         * baixo isso e menos de um pixel de arte, entao errar por pouco pegava a
         * arma em vez da bolinha — e o que se via era a bolinha "arrastando o
         * sprite junto". Errar tem de ser dificil, e a bolinha se acende quando o
         * mouse esta no alcance, para nao ser adivinhacao.
         */
        private boolean naBoca(Point onde) {
            java.awt.geom.Point2D.Double b = ondeSaiOTiro();
            return onde.distance(b.x, b.y) <= Math.max(10, escala);
        }

        private boolean atira() {
            String m = m();
            return m.equals("mira") || m.equals("varinha");
        }

        /**
         * De onde o tiro sai, na tela.
         *
         * Sem nada marcado e a PONTA da arma, achada sozinha. Com um ponto
         * marcado — arrastado na bolinha amarela — e ele. Achar sozinho cobre
         * quase todos os casos; arrastar cobre o resto, e e o mesmo gesto que
         * move a arma, entao nao ha conceito novo para aprender.
         */
        private java.awt.geom.Point2D.Double ondeSaiOTiro() {
            Pose p = poseDe(arma);
            double ang = Math.toRadians(mira.getValue());
            int lado = espelhado ? -1 : 1;
            // A varinha e segurada, entao a ponta dela gira com a INCLINACAO DA
            // POSE — nao com a mira. Com giro zero, o ponto caia no pixel mais a
            // direita do desenho sem girar, que num cajado inclinado e o pe dele.
            boolean deMira = m().equals("mira");
            double giro = deMira
                    ? ang + Math.toRadians(p.graus) + facingDe(arma)
                    : Math.toRadians(p.graus * lado);
            Point onde = deMira ? cantoNaMira(p) : ondeAArmaEsta(p);
            Point miolo = centroDoDesenho(sprites.get(arma));
            double ax = onde.x + miolo.x * escala + escala / 2.0;
            double ay = onde.y + miolo.y * escala + escala / 2.0;
            if (p.bocaX >= 0) {
                return bocaNoMundo(p, ax, ay, giro);
            }
            if (deMira) {
                // Arma apontada: a frente do desenho e o oposto do facing.
                return pontaNoMundo(ax, ay, giro, -facingDe(arma), null);
            }
            // Arma segurada: a ponta e o pixel mais longe do PUNHO.
            return pontaNoMundo(ax, ay, giro, 0, new Point(p.punhoX, p.punhoY));
        }

        /**
         * A ponta da arma no mundo. Mesma conta de Item.boca e Wand.pontaDaVarinha.
         *
         * A busca e feita NO ESPACO DO DESENHO. A versao anterior girava o pixel e
         * projetava no eixo ja girado, o que se cancela em algebra e devolvia
         * sempre o pixel mais a direita do arquivo — no cajado, o pe dele.
         *
         * @param direcao para onde e "a frente" dentro do arquivo. Arma segurada
         *                usa -1, que quer dizer "o mais longe do punho".
         */
        private java.awt.geom.Point2D.Double pontaNoMundo(double ax, double ay, double giro,
                                                          double direcao, Point de) {
            BufferedImage sp = sprites.get(arma);
            Point miolo = centroDoDesenho(sp);
            int melhorX = miolo.x, melhorY = miolo.y;
            double melhor = -Double.MAX_VALUE;
            for (int y = 0; y < sp.getHeight(); y++) {
                for (int x = 0; x < sp.getWidth(); x++) {
                    if ((sp.getRGB(x, y) >>> 24) <= 16) {
                        continue;
                    }
                    double avanco = de != null
                            ? (x - de.x) * (x - de.x) + (y - de.y) * (y - de.y)
                            : x * Math.cos(direcao) + y * Math.sin(direcao);
                    if (avanco > melhor) {
                        melhor = avanco;
                        melhorX = x;
                        melhorY = y;
                    }
                }
            }
            if (de == null) {
                // A boca e o MEIO DA FACE DA FRENTE. Mesma conta de
                // Item.pontaDoDesenho: pegar um pixel qualquer da coluna mais
                // avancada punha o ponto na quina de baixo do cano.
                double somaX = 0, somaY = 0;
                int n = 0;
                for (int y = 0; y < sp.getHeight(); y++) {
                    for (int x = 0; x < sp.getWidth(); x++) {
                        if ((sp.getRGB(x, y) >>> 24) <= 16) {
                            continue;
                        }
                        if (x * Math.cos(direcao) + y * Math.sin(direcao) >= melhor - 1) {
                            somaX += x;
                            somaY += y;
                            n++;
                        }
                    }
                }
                if (n > 0) {
                    melhorX = (int) Math.round(somaX / n);
                    melhorY = (int) Math.round(somaY / n);
                }
            }
            double bx = (melhorX - miolo.x) * escala, by = (melhorY - miolo.y) * escala;
            double c = Math.cos(giro), sn = Math.sin(giro);
            return new java.awt.geom.Point2D.Double(ax + bx * c - by * sn, ay + bx * sn + by * c);
        }

        private void marcarMao(Graphics2D g) {
            g.setColor(new Color(0xFF4FD1));
            g.fillRect(mao().x - 2, mao().y - 2, 5, 5);
            g.setColor(new Color(0x4FE3FF));
            g.drawOval(mao().x - 6, mao().y - 6, 13, 13);
        }

        /** O centro do gato, de onde o arco e o laser sao medidos. */
        private Point centroDoGato() {
            Point c = cantoDoGato();
            return new Point(c.x + QUADRO * escala / 2, c.y + QUADRO * escala / 2);
        }

        /**
         * Arco, laser e tridente NAO tem pose de mao: eles apontam para o alvo.
         *
         * Copiado de Bow.render — a arma e posta a sete pixels do centro do gato,
         * na direcao da mira, ancorada pelo MIOLO DO DESENHO, e girada por
         * mira + Rotate.DIAGONAL. Mexer em dx, dy ou graus nao muda nada aqui, e
         * por isso esses campos aparecem desligados para estas armas.
         */
        private void desenharMirando(Graphics2D g, BufferedImage sp) {
            Pose p = poseDe(arma);
            double ang = Math.toRadians(mira.getValue());
            Point centro = centroDoGato();
            java.awt.geom.Point2D.Double a = ancoraDeMira(p);
            double ax = a.x;
            double ay = a.y;
            double giro = ang + Math.toRadians(p.graus);

            g.setColor(new Color(0x2E7A4A));
            g.drawLine(centro.x, centro.y,
                    (int) (centro.x + Math.cos(ang) * 20 * escala),
                    (int) (centro.y + Math.sin(ang) * 20 * escala));

            porOMiolo(g, sp, ax, ay, giro + facingDe(arma));

            // O tiro sai da PONTA — a mesma conta de ondeSaiOTiro, para o ponto
            // amarelo e o feixe nunca discordarem.
            java.awt.geom.Point2D.Double bc = ondeSaiOTiro();
            // ---- a flecha, SO no arco. O laser tambem aponta, mas o que sai
            // dele e um feixe: desenhar flecha ali dizia que o laser era um arco.
            if (!arma.startsWith("bow")) {
                if (t >= 0) {
                    desenharFeixe(g, bc.x, bc.y, ang);
                }
                return;
            }
            BufferedImage flecha = flechaDe(arma);
            if (flecha == null || t < 0) {
                return;
            }
            if (t < ENCAIXE) {
                // Encaixada: recua na corda enquanto a corda estica. Mesma conta
                // de Bow.renderArrow — recuo de (5 - countShot) pixels de arte.
                int countShot = Math.min(5, 1 + t / 5);
                double recuo = (5 - countShot) * escala;
                double fx = ax - Math.cos(ang) * recuo;
                double fy = ay - Math.sin(ang) * recuo;
                porOMiolo(g, flecha, fx, fy, ang + Math.PI / 2);
            } else {
                // Solta: sai da BOCA e viaja a sete pixels por tick, que e a
                // velocidade da Arrow no jogo.
                double andou = (t - ENCAIXE) * 7.0 * escala;
                double fx = bc.x + Math.cos(ang) * andou;
                double fy = bc.y + Math.sin(ang) * andou;
                porOMiolo(g, flecha, fx, fy, ang + Math.PI / 2);
            }
        }

        /** A boca no mundo. Mesma conta de Item.boca. */
        private java.awt.geom.Point2D.Double bocaNoMundo(Pose p, double ax, double ay, double giro) {
            if (p.bocaX < 0) {
                return new java.awt.geom.Point2D.Double(ax, ay);
            }
            Point miolo = centroDoDesenho(sprites.get(arma));
            double bx = p.bocaX * escala + escala / 2.0 - (miolo.x * escala + escala / 2.0);
            double by = p.bocaY * escala + escala / 2.0 - (miolo.y * escala + escala / 2.0);
            double c = Math.cos(giro), sn = Math.sin(giro);
            return new java.awt.geom.Point2D.Double(ax + bx * c - by * sn, ay + bx * sn + by * c);
        }

        /** Desenha o sprite com o MIOLO do desenho em (ax, ay). Ver Rotate.apontar. */
        private void porOMiolo(Graphics2D g, BufferedImage sp, double ax, double ay, double giro) {
            Point miolo = centroDoDesenho(sp);
            double px = miolo.x * escala + escala / 2.0;
            double py = miolo.y * escala + escala / 2.0;
            Graphics2D g2 = (Graphics2D) g.create();
            AffineTransform at = new AffineTransform();
            at.translate(ax - px, ay - py);
            at.rotate(giro, px, py);
            at.scale(escala, escala);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(sp, at, null);
            g2.dispose();
        }

        /**
         * O tridente no ar: sai do meio do gato, vai reto e volta pela mesma reta.
         *
         * Copiado de ThrownTrident: a volta e o mesmo caminho de tras para a
         * frente, e nao uma curva atras do dono — foi essa a correcao que fez ele
         * parecer o martelo do Thor em vez de um bumerangue.
         */
        private void desenharArremesso(Graphics2D g, BufferedImage sp) {
            double ang = Math.toRadians(mira.getValue());
            Point centro = centroDoGato();
            int passo = t - SEGURA;
            double f = passo < IDA ? passo / (double) IDA
                    : 1 - (passo - IDA) / (double) IDA;
            double d = f * ALCANCE_DA_PREVIA * escala;

            g.setColor(new Color(0x2E7A4A));
            g.drawLine(centro.x, centro.y,
                    (int) (centro.x + Math.cos(ang) * ALCANCE_DA_PREVIA * escala),
                    (int) (centro.y + Math.sin(ang) * ALCANCE_DA_PREVIA * escala));

            // DIAGONAL, e nao PARA_CIMA. O preview ficou com a orientacao da epoca
            // em que o tridente era desenhado a mao, em pe; o do pacote vem na
            // diagonal, e ThrownTrident ja usa DIAGONAL. Era isso que fazia a arma
            // sair torta em relacao a linha do arremesso.
            porOMiolo(g, sp, centro.x + Math.cos(ang) * d, centro.y + Math.sin(ang) * d,
                    ang + 3 * Math.PI / 4);
        }

        /** A flecha do elemento deste arco, ou a comum. */
        private BufferedImage flechaDe(String bow) {
            int corte = bow.indexOf('_');
            String nome = corte < 0 ? "arrow" : "arrow" + bow.substring(corte);
            BufferedImage f = flechas.get(nome);
            return f != null ? f : flechas.get("arrow");
        }

        /** O feixe do laser, saindo do cano. Ver Laser.desenharFeixe. */
        private void desenharFeixe(Graphics2D g, double ax, double ay, double ang) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            double fim = 18 * escala;
            int x2 = (int) (ax + Math.cos(ang) * fim);
            int y2 = (int) (ay + Math.sin(ang) * fim);
            g2.setStroke(new BasicStroke(Math.max(1f, escala * 1.6f)));
            g2.setColor(new Color(255, 60, 60, 90));
            g2.drawLine((int) ax, (int) ay, x2, y2);
            g2.setStroke(new BasicStroke(Math.max(1f, escala * 0.5f)));
            g2.setColor(new Color(255, 240, 240, 200));
            g2.drawLine((int) ax, (int) ay, x2, y2);
            g2.dispose();
        }

        /**
         * A varinha atirando: clarao na ponta e a bola voando. Ver Wand.
         *
         * Era um raio ate o alvo, do tempo em que a varinha acertava sem nada
         * sair dela. Agora e o que o jogo faz de verdade — o que viaja e a bola,
         * a tres pixels e meio por tick, e a ponta so pisca.
         */
        private void desenharRaio(Graphics2D g) {
            double ang = Math.toRadians(mira.getValue());
            java.awt.geom.Point2D.Double ponta = ondeSaiOTiro();

            // A LINHA DE TIRO APARECE SEMPRE, mesmo com a animacao parada. Antes
            // so existia enquanto o golpe rodava, entao quem abria a varinha via
            // uma arma parada e nenhuma pista de para onde o feitico ia.
            g.setColor(new Color(0x2E7A4A));
            g.drawLine((int) ponta.x, (int) ponta.y,
                    (int) (ponta.x + Math.cos(ang) * 20 * escala),
                    (int) (ponta.y + Math.sin(ang) * 20 * escala));
            if (t < 0) {
                return;
            }

            int ciclo = 30;
            int passo = t % ciclo;
            if (passo < 5) {
                float f = 1 - passo / 5f;
                int raio = (int) (escala * (0.4f + f * 0.7f));
                g.setColor(new Color(255, 240, 180, (int) (200 * f)));
                g.fillOval((int) ponta.x - raio, (int) ponta.y - raio, raio * 2, raio * 2);
            }
            BufferedImage bola = flechas.get(bolaDe(arma));
            if (bola != null) {
                double andou = passo * 3.5 * escala;
                porOMiolo(g, bola, ponta.x + Math.cos(ang) * andou,
                        ponta.y + Math.sin(ang) * andou, 0);
            }
        }

        /** A bola do elemento desta varinha. */
        private String bolaDe(String wand) {
            int corte = wand.indexOf('_');
            String nome = corte < 0 ? "bolt" : "bolt" + wand.substring(corte);
            return flechas.containsKey(nome) ? nome : "bolt";
        }

        private BufferedImage espelhar(BufferedImage im) {
            BufferedImage o = new BufferedImage(im.getWidth(), im.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = o.createGraphics();
            g.drawImage(im, im.getWidth(), 0, -im.getWidth(), im.getHeight(), null);
            g.dispose();
            return o;
        }
    }
}
