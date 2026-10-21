package com.retronova.engine.inputs.gamepad;

import com.retronova.engine.Configs;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controle no padrão Xbox.
 *
 * NÃO HÁ TELA QUE CONHEÇA O CONTROLE. Ele aperta as mesmas teclas que o jogo já
 * lê por nome ({@link KeyBoard#pad}), e as quase cinquenta leituras de teclado
 * funcionam sem mudar. As telas de grade — inventário, loja — andam por foco com
 * as setas, então servem ao teclado e ao controle do mesmo jeito. O que é próprio
 * do controle, e não tem tecla equivalente, é pouco: o movimento analógico
 * ({@link #movimento()}), o olhar do analógico direito ({@link #olhar()}), LB/RB
 * ({@link #pressionou}) e a vibração.
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
     * Botão -> tecla que ele aperta. O B fica de fora: depende de estar em jogo
     * ou numa tela (ver {@link #emJogo()}).
     */
    private static final Map<Botao, String> TECLAS = new LinkedHashMap<>();

    static {
        TECLAS.put(Botao.A, "Enter");
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
        aplicar(estado);
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
     * Traduz uma leitura em teclas.
     *
     * Separado da leitura para ser testável sem a DLL.
     */
    static void aplicar(EstadoDoControle estado) {
        // Qualquer coisa no controle passa o jogo para o modo controle, e o mouse
        // some das telas. So conta MUDANCA: um analogico segurado parado nao pode
        // ficar tomando o jogo de volta de quem acabou de mexer o mouse.
        if (!estado.equals(anterior) && !estado.equals(EstadoDoControle.SOLTO)) {
            Entrada.controle();
        }
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

    /**
     * Para onde o analógico direito aponta, de -1 a 1 em cada eixo, já sem a zona
     * morta. Solto, devolve zero. A câmera usa isto para espiar. Ver Camera.
     */
    public static double[] olhar() {
        return new double[] {anterior.rx(), anterior.ry()};
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
        emJogo = false;
        teclaDoB = "Escape";
    }
}
