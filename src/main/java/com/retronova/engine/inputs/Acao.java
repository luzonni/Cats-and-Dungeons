package com.retronova.engine.inputs;

import com.retronova.engine.inputs.gamepad.Comando;
import com.retronova.engine.inputs.gamepad.Gamepad;
import com.retronova.engine.inputs.keyboard.KeyBoard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Cada ação do jogador, com o nome que a tela mostra e as teclas que ela lê.
 *
 * ---------------------------------------------------------------------------
 * A FONTE ÚNICA DOS CONTROLES
 *
 * O jogo lê teclas VIRTUAIS por nome ({@link KeyBoard#KeyPressed}): "E", "Escape",
 * "Interact". Quem decide qual tecla física é cada nome é {@link KeyBoard#Keys}; quem
 * decide qual botão do controle aperta cada nome é o mapa do {@link Gamepad}. Uma
 * ação aqui diz só QUAIS NOMES ela lê, e o resto é derivado desses dois lugares:
 *
 *   - a tecla do teclado sai do código em KeyBoard.Keys ({@link #codigosDeTecla});
 *   - o botão do controle sai do mapa do Gamepad ({@link #comandos}), a não ser
 *     quando a ação lê o controle direto (LB/RB, analógico direito), e aí ela
 *     declara a peça em {@link #padDireto}.
 *
 * As ações de jogo leem por aqui ({@link #apertou}), então mudar a tecla de uma
 * delas muda o jogo e a tela de Controles juntos. As telas de menu ainda leem as
 * setas e o Enter pelo nome; o AcaoTest varre o código e falha se alguém ler uma
 * tecla que não está em nenhuma ação daqui — a tela não fica para trás.
 *
 * Não há remapeamento: o jogo nunca teve. Se vier, ele muda KeyBoard.Keys e o
 * mapa do Gamepad, e esta tela já mostra o resultado.
 *
 * ---------------------------------------------------------------------------
 * AÇÃO NOVA: uma linha aqui, no grupo dela, com o rótulo em inglês (texto de
 * tela). Ela aparece sozinha nas duas visões da tela de Controles. A ordem das
 * constantes é a ordem da lista; nada é gravado por ordinal.
 */
public enum Acao {

    // ------------------------------------------------------------ na corrida
    MOVER(Grupo.MOVIMENTO, "Move", "Walk. The left stick walks at any angle.",
            Liga.teclas("W", "A", "S", "D", "Up", "Down", "Left", "Right")),
    OLHAR(Grupo.MOVIMENTO, "Look ahead", "Push the camera toward where you aim.",
            Liga.soPad(Comando.ANALOGICO_DIR)),
    DASH(Grupo.MOVIMENTO, "Dash", "A short burst of speed, once you have the dash.",
            Liga.teclas("Space")),

    INTERAGIR(Grupo.CORRIDA, "Interact", "Talk, open chests, pull levers. With the mouse, click the object.",
            Liga.teclas("Interact").mouse(Mouse.ESQUERDO)),
    STATUS(Grupo.CORRIDA, "Status", "Your cat's attributes and build. Opens over the paused game.",
            Liga.teclas("E")),
    USAR_ITEM(Grupo.CORRIDA, "Use item", "Use the item selected in the hotbar.",
            Liga.teclas("F")),
    LARGAR_ITEM(Grupo.CORRIDA, "Drop item", "Drop the item selected in the hotbar.",
            Liga.teclas("Q")),
    TROCAR_ITEM(Grupo.CORRIDA, "Switch item", "Select the next or previous hotbar slot.",
            Liga.soPad(Comando.LB, Comando.RB).mouse(Mouse.RODA)),
    PAUSAR(Grupo.CORRIDA, "Pause", "Pause the run and open the settings.",
            Liga.teclas("Escape")),

    // ------------------------------------------------------------ nas telas
    NAVEGAR(Grupo.MENUS, "Navigate", "Move between options, cards and slots.",
            Liga.teclas("Up", "Down", "Left", "Right")),
    SELECIONAR(Grupo.MENUS, "Select", "Confirm the option under focus.",
            Liga.teclas("Enter").mouse(Mouse.ESQUERDO)),
    VOLTAR(Grupo.MENUS, "Back", "Close the screen and go back.",
            Liga.teclas("Escape")),
    ABAS(Grupo.MENUS, "Switch tabs", "Previous and next tab.",
            Liga.teclas("Q", "E").pad(Comando.LB, Comando.RB)),
    ESCOLHER_CARTA(Grupo.MENUS, "Pick a card", "Pick a level-up card by its number.",
            Liga.teclas("1", "2", "3").semControle()),
    TROCAR_CARTAS(Grupo.MENUS, "Reroll cards", "Draw a new hand when leveling up, while rerolls last.",
            Liga.teclas("Q")),
    PULAR_NIVEL(Grupo.MENUS, "Skip level-up", "Take no card and heal 15% instead, while skips last.",
            Liga.teclas("E")),

    // ------------------------------------------------------------ sistema
    TELA_CHEIA(Grupo.SISTEMA, "Fullscreen", "Toggle fullscreen anywhere in the game.",
            Liga.teclas("F11")),
    DEPURAR(Grupo.SISTEMA, "Debug overlays", "F3 frame graph, F4 hitboxes, F5 tiles, F6 particles, F7 HUD boxes.",
            Liga.teclas("F3", "F4", "F5", "F6", "F7"));

    /** Os grupos, na ordem da tela; o rótulo é texto de tela. */
    public enum Grupo {
        MOVIMENTO("Movement", true), CORRIDA("In a run", true), MENUS("Menus", false), SISTEMA("System", false);

        public final String rotulo;
        /** As ações do grupo valem com a partida rolando (o B interage em vez de voltar). */
        public final boolean emJogo;

        Grupo(String rotulo, boolean emJogo) {
            this.rotulo = rotulo;
            this.emJogo = emJogo;
        }
    }

    /** O que o mouse faz numa ação. */
    public enum Mouse { ESQUERDO, DIREITO, RODA }

    public final Grupo grupo;
    /** O nome da ação na tela. */
    public final String rotulo;
    /** Uma frase para o balão de ajuda. */
    public final String descricao;
    private final String[] teclas;
    private final List<Mouse> mouse;
    /** As peças que a ação lê direto do controle; nulo quando elas saem das teclas. */
    private final Set<Comando> padDireto;
    private final boolean semControle;

    Acao(Grupo grupo, String rotulo, String descricao, Liga liga) {
        this.grupo = grupo;
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.teclas = liga.teclas;
        this.mouse = List.copyOf(liga.mouse);
        this.padDireto = liga.padDireto;
        this.semControle = liga.semControle;
    }

    /** O que uma ação lê, montado na declaração da constante. */
    private static final class Liga {
        private String[] teclas = {};
        private final List<Mouse> mouse = new ArrayList<>();
        private Set<Comando> padDireto;
        private boolean semControle;

        static Liga teclas(String... nomes) {
            Liga l = new Liga();
            l.teclas = nomes;
            return l;
        }

        /** A ação lê estas peças do controle direto, e não por tecla. */
        static Liga soPad(Comando... pecas) {
            return new Liga().pad(pecas[0], java.util.Arrays.copyOfRange(pecas, 1, pecas.length));
        }

        Liga pad(Comando primeira, Comando... resto) {
            padDireto = EnumSet.of(primeira, resto);
            return this;
        }

        Liga mouse(Mouse... m) {
            Collections.addAll(mouse, m);
            return this;
        }

        /** A ação não existe no controle, mesmo que a tecla dela seja apertada por algum botão. */
        Liga semControle() {
            semControle = true;
            return this;
        }
    }

    // ------------------------------------------------------------- leitura

    /** A ação foi acionada neste tick, por qualquer tecla dela. Consome como KeyPressed. */
    public boolean apertou() {
        for (String t : teclas) {
            if (KeyBoard.KeyPressed(t)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------- para mostrar

    /** Os nomes virtuais que a ação lê. */
    public List<String> teclas() {
        return List.of(teclas);
    }

    public List<Mouse> mouse() {
        return mouse;
    }

    /** As teclas físicas (KeyEvent.VK_*) da ação, na ordem declarada; as que só o controle aperta ficam de fora. */
    public List<Integer> codigosDeTecla() {
        List<Integer> saida = new ArrayList<>();
        for (String nome : teclas) {
            for (KeyBoard.Key k : KeyBoard.Keys) {
                if (k.getName().equalsIgnoreCase(nome) && k.getKey() >= 0 && !saida.contains(k.getKey())) {
                    saida.add(k.getKey());
                }
            }
        }
        return saida;
    }

    /** As peças do controle que acionam a ação: as diretas, ou as que apertam as teclas dela. */
    public Set<Comando> comandos() {
        if (semControle) {
            return EnumSet.noneOf(Comando.class);
        }
        if (padDireto != null) {
            return EnumSet.copyOf(padDireto);
        }
        Set<Comando> saida = EnumSet.noneOf(Comando.class);
        for (String t : teclas) {
            saida.addAll(Gamepad.comandosDaTecla(t, grupo.emJogo));
        }
        return saida;
    }

    /** A ação tem como ser feita no teclado ou no mouse. */
    public boolean noTeclado() {
        return !codigosDeTecla().isEmpty() || !mouse.isEmpty();
    }

    /** As ações que uma peça do controle aciona, na ordem da lista. */
    public static List<Acao> doComando(Comando c) {
        List<Acao> saida = new ArrayList<>();
        for (Acao a : values()) {
            if (a.comandos().contains(c)) {
                saida.add(a);
            }
        }
        return saida;
    }
}
