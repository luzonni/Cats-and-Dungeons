package com.retronova.engine.inputs.gamepad;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controle no padrão Xbox.
 *
 * NÃO HÁ TELA QUE CONHEÇA O CONTROLE. Ele aperta as mesmas teclas que o jogo já
 * lê por nome ({@link KeyBoard#pad}) e mexe no mesmo cursor que o mouse
 * ({@link Mouse#moverVirtual}). Assim as quase cinquenta leituras de teclado e as
 * telas feitas só para mouse — loja, inventário, vendedor — funcionam sem mudar.
 * O que é próprio do controle, e não tem tecla equivalente, é pouco: o movimento
 * analógico ({@link #movimento()}), LB/RB ({@link #pressionou}) e a vibração.
 *
 * O CURSOR VIRTUAL só assume quando o analógico direito é usado. A partir daí o
 * A clica em vez de confirmar. Ele sai de cena quando o mouse de verdade se mexe
 * ou quando o jogador volta a navegar pelo D-pad.
 */
public final class Gamepad {

    public enum Botao {
        CIMA(0x0001), BAIXO(0x0002), ESQUERDA(0x0004), DIREITA(0x0008),
        START(0x0010), VIEW(0x0020), L3(0x0040), R3(0x0080),
        LB(0x0100), RB(0x0200), A(0x1000), B(0x2000), X(0x4000), Y(0x8000),
        LT(0), RT(0);

        final int mascara;

        Botao(int mascara) {
            this.mascara = mascara;
        }
    }

    /**
     * Botão -> tecla que ele aperta. A e B ficam de fora: o A depende do cursor,
     * e o B de estar em jogo ou numa tela (ver {@link #emJogo()}).
     */
    private static final Map<Botao, String> TECLAS = new LinkedHashMap<>();

    static {
        TECLAS.put(Botao.START, "Escape");
        TECLAS.put(Botao.X, "F");
        TECLAS.put(Botao.Y, "E");
        TECLAS.put(Botao.VIEW, "Q");
        TECLAS.put(Botao.LT, "Space");
        TECLAS.put(Botao.RT, "Space");
        TECLAS.put(Botao.CIMA, "Up");
        TECLAS.put(Botao.BAIXO, "Down");
        TECLAS.put(Botao.ESQUERDA, "Left");
        TECLAS.put(Botao.DIREITA, "Right");
    }

    private static final Set<String> DIRECOES = Set.of("Up", "Down", "Left", "Right");

    /** Deflexão do analógico que conta como apertar uma direção nos menus. */
    static final double LIMIAR_DIRECAO = 0.5;
    /** Auto-repeat de direção segurada, em ticks: 400 ms e depois a cada ~120 ms. */
    static final int REPETE_APOS = 24, REPETE_CADA = 7;
    /** Fração da altura da janela que o cursor anda por tick com o analógico no fim. */
    static final double VELOCIDADE_CURSOR = 0.018;
    /**
     * Porta vazia só é consultada uma vez por segundo. XInputGetState numa porta
     * sem controle é sabidamente lento, e consultar as quatro a cada tick faria o
     * jogo engasgar em quem nem tem controle.
     */
    private static final int INTERVALO_BUSCA = 60;

    private static int porta = -1;
    private static int esperaBusca;
    private static XInput.Estado leitura;

    private static EstadoDoControle anterior = EstadoDoControle.SOLTO;
    private static Map<String, Boolean> teclasAnteriores = new HashMap<>();
    private static final Map<String, Integer> seguradas = new HashMap<>();
    private static final Set<Botao> pendentes = EnumSet.noneOf(Botao.class);

    private static boolean cursorAtivo;
    private static double cursorX, cursorY;

    /** O jogo rodou no último tick. Ver emJogo. */
    private static boolean emJogo;
    /** Tecla que o B está apertando, escolhida no instante do aperto. */
    private static String teclaDoB = "Escape";

    private static int vibracaoRestante;
    private static boolean ganchoDeSaida;

    private Gamepad() {
    }

    /** Chamado uma vez por tick pelo Engine, antes da Activity. */
    public static void tick() {
        if (XInput.API == null) {
            return;
        }
        EstadoDoControle estado = ler();
        if (vibracaoRestante > 0 && --vibracaoRestante == 0) {
            motores(0, 0);
        }
        aplicar(estado, Engine.window.getWidth(), Engine.window.getHeight());
    }

    private static EstadoDoControle ler() {
        if (leitura == null) {
            leitura = new XInput.Estado();
        }
        if (porta < 0) {
            if (--esperaBusca > 0) {
                return EstadoDoControle.SOLTO;
            }
            esperaBusca = INTERVALO_BUSCA;
            for (int i = 0; i < 4 && porta < 0; i++) {
                if (XInput.API.XInputGetState(i, leitura) == XInput.ERROR_SUCCESS) {
                    porta = i;
                }
            }
            if (porta < 0) {
                return EstadoDoControle.SOLTO;
            }
        }
        if (XInput.API.XInputGetState(porta, leitura) != XInput.ERROR_SUCCESS) {
            porta = -1;     // desconectou: volta a procurar, devagar
            esperaBusca = INTERVALO_BUSCA;
            return EstadoDoControle.SOLTO;
        }
        return EstadoDoControle.deXInput(leitura.controle);
    }

    /**
     * Traduz uma leitura em teclas e cursor.
     *
     * Separado da leitura para ser testável sem a DLL. Largura e altura zero
     * desligam o cursor, que é o que os testes usam.
     */
    static void aplicar(EstadoDoControle estado, int largura, int altura) {
        if (Mouse.consumirMovimentoReal()) {
            cursorAtivo = false;
        }
        moverCursor(estado, largura, altura);

        Map<String, Boolean> teclas = new HashMap<>();
        for (Map.Entry<Botao, String> e : TECLAS.entrySet()) {
            teclas.merge(e.getValue(), estado.pressionado(e.getKey()), Boolean::logicalOr);
        }

        // O B INTERAGE EM JOGO E VOLTA NAS TELAS. A escolha e feita no aperto e
        // fica presa ate soltar: apertar B no vendedor abre a loja, e no tick
        // seguinte o jogo ja esta pausado — se o B virasse Esc ali, a loja
        // fecharia no mesmo instante em que abriu.
        boolean jogando = emJogo;
        emJogo = false;
        if (estado.pressionado(Botao.B) && !anterior.pressionado(Botao.B)) {
            teclaDoB = jogando ? "Interact" : "Escape";
        }
        teclas.merge("Interact", false, Boolean::logicalOr);
        teclas.merge(teclaDoB, estado.pressionado(Botao.B), Boolean::logicalOr);
        teclas.merge("Left", estado.lx() < -LIMIAR_DIRECAO, Boolean::logicalOr);
        teclas.merge("Right", estado.lx() > LIMIAR_DIRECAO, Boolean::logicalOr);
        teclas.merge("Up", estado.ly() < -LIMIAR_DIRECAO, Boolean::logicalOr);
        teclas.merge("Down", estado.ly() > LIMIAR_DIRECAO, Boolean::logicalOr);

        // Voltar a navegar pelas direções devolve o A ao papel de confirmar.
        if (cursorAtivo) {
            for (String direcao : DIRECOES) {
                if (teclas.get(direcao) && !teclasAnteriores.getOrDefault(direcao, false)) {
                    recolherCursor();
                    break;
                }
            }
        }

        if (cursorAtivo) {
            clicar(Mouse_Button.LEFT, estado.pressionado(Botao.A), anterior.pressionado(Botao.A));
            clicar(Mouse_Button.RIGHT, estado.pressionado(Botao.R3), anterior.pressionado(Botao.R3));
            teclas.put("Enter", false);
        } else {
            teclas.put("Enter", estado.pressionado(Botao.A));
        }

        for (Map.Entry<String, Boolean> e : teclas.entrySet()) {
            String nome = e.getKey();
            boolean agora = e.getValue();
            boolean antes = teclasAnteriores.getOrDefault(nome, false);
            if (agora && !antes) {
                KeyBoard.pad(nome, true);
                seguradas.put(nome, 0);
            } else if (!agora && antes) {
                KeyBoard.pad(nome, false);
                seguradas.remove(nome);
            } else if (agora && DIRECOES.contains(nome)) {
                int t = seguradas.merge(nome, 1, Integer::sum);
                if (t >= REPETE_APOS && (t - REPETE_APOS) % REPETE_CADA == 0) {
                    KeyBoard.pad(nome, true);   // reaciona se o menu já gastou o aperto
                }
            }
        }
        teclasAnteriores = teclas;

        for (Botao b : new Botao[] {Botao.LB, Botao.RB}) {
            if (estado.pressionado(b) && !anterior.pressionado(b)) {
                pendentes.add(b);
            } else if (!estado.pressionado(b)) {
                pendentes.remove(b);
            }
        }
        anterior = estado;
    }

    private static void moverCursor(EstadoDoControle estado, int largura, int altura) {
        double raio = Math.hypot(estado.rx(), estado.ry());
        if (raio == 0 || largura <= 0 || altura <= 0) {
            return;
        }
        if (!cursorAtivo) {
            cursorAtivo = true;
            // Nasce onde o mouse estava, se ele estava na janela; senão, onde o
            // cursor virtual foi deixado da última vez.
            if (Mouse.getX() >= 0 && Mouse.getY() >= 0) {
                cursorX = Mouse.getX();
                cursorY = Mouse.getY();
            }
        }
        // Curva quadrática: o começo do curso é lento e preciso para acertar um
        // slot, o fim é rápido para atravessar a tela.
        double passo = raio * VELOCIDADE_CURSOR * altura;
        cursorX = Math.max(0, Math.min(largura - 1, cursorX + estado.rx() * passo));
        cursorY = Math.max(0, Math.min(altura - 1, cursorY + estado.ry() * passo));
        Mouse.moverVirtual((int) cursorX, (int) cursorY);
    }

    /**
     * Some com o cursor e o tira de cima da tela.
     *
     * Deixado onde estava, ele continuaria "sobre" algum botão, e os menus dão o
     * foco a quem está sob o mouse — o D-pad ficaria brigando com um cursor que
     * nem aparece mais.
     */
    private static void recolherCursor() {
        cursorAtivo = false;
        if (anterior.pressionado(Botao.A)) {
            Mouse.botaoVirtual(Mouse_Button.LEFT, false);
        }
        if (anterior.pressionado(Botao.R3)) {
            Mouse.botaoVirtual(Mouse_Button.RIGHT, false);
        }
        Mouse.moverVirtual(-1, -1);
    }

    private static void clicar(Mouse_Button botao, boolean agora, boolean antes) {
        if (agora != antes) {
            Mouse.botaoVirtual(botao, agora);
        }
    }

    // ------------------------------------------------------------- consulta

    /**
     * Chamado pelo jogo a cada tick em que a partida está rolando, sem pausa nem
     * interface aberta. Enquanto isso, o B interage em vez de voltar.
     */
    public static void emJogo() {
        emJogo = true;
    }

    public static boolean conectado() {
        return porta >= 0;
    }

    public static boolean cursorAtivo() {
        return cursorAtivo;
    }

    /**
     * Direção do analógico esquerdo, ou nulo com ele solto.
     *
     * O tamanho do vetor vai até 1, mas o jogo usa só o ângulo: andar mais devagar
     * com o analógico pela metade mudaria o equilíbrio de velocidade dos gatos.
     */
    public static double[] movimento() {
        if (anterior.lx() == 0 && anterior.ly() == 0) {
            return null;
        }
        return new double[] {anterior.lx(), anterior.ly()};
    }

    /** LB/RB, consumível como KeyBoard.KeyPressed. */
    public static boolean pressionou(Botao botao) {
        return pendentes.remove(botao);
    }

    // -------------------------------------------------------------- vibração

    /**
     * Vibra por alguns ticks.
     *
     * Motor esquerdo (grave) na força pedida e o direito (agudo) mais fraco: o
     * golpe tem que ser sentido como pancada, não como zumbido.
     */
    public static void vibrar(double forca, int ticks) {
        if (XInput.API == null || porta < 0 || !Configs.Rumble()) {
            return;
        }
        double f = Math.max(0, Math.min(1, forca));
        motores(f, f * 0.6);
        vibracaoRestante = ticks;
        if (!ganchoDeSaida) {
            // Um controle mandado vibrar continua vibrando se o jogo fechar antes
            // do tick que o desligaria.
            ganchoDeSaida = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> motores(0, 0), "Gamepad-desliga"));
        }
    }

    private static void motores(double esquerdo, double direito) {
        if (XInput.API == null || porta < 0) {
            return;
        }
        XInput.Vibracao v = new XInput.Vibracao();
        v.wLeftMotorSpeed = (short) Math.round(esquerdo * 65535);
        v.wRightMotorSpeed = (short) Math.round(direito * 65535);
        XInput.API.XInputSetState(porta, v);
    }

    /** Volta ao estado inicial, soltando o que estiver apertado. Para testes. */
    static void reiniciar() {
        for (Map.Entry<String, Boolean> e : teclasAnteriores.entrySet()) {
            if (e.getValue()) {
                KeyBoard.pad(e.getKey(), false);
            }
        }
        teclasAnteriores = new HashMap<>();
        seguradas.clear();
        pendentes.clear();
        anterior = EstadoDoControle.SOLTO;
        cursorAtivo = false;
        cursorX = cursorY = 0;
        emJogo = false;
        teclaDoB = "Escape";
    }
}
