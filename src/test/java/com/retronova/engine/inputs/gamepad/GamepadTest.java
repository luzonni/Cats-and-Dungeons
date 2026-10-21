package com.retronova.engine.inputs.gamepad;

import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do mapeamento do controle.
 *
 * Tudo aqui roda sem controle e sem Windows: a leitura da XInput é separada da
 * tradução ({@code Gamepad.aplicar}), e é a tradução que tem regra para quebrar.
 * Ela é o que garante que o controle aperte as teclas do jogo do mesmo jeito que
 * o teclado — um aperto por toque, solto ao soltar, sem atropelar o teclado.
 */
class GamepadTest {

    private static EstadoDoControle botoes(Gamepad.Botao... apertados) {
        int mascara = 0;
        double lt = 0, rt = 0;
        for (Gamepad.Botao b : apertados) {
            if (b == Gamepad.Botao.LT) lt = 1;
            else if (b == Gamepad.Botao.RT) rt = 1;
            else mascara |= b.mascara;
        }
        return new EstadoDoControle(mascara, lt, rt, 0, 0, 0, 0);
    }

    private static EstadoDoControle analogicoEsquerdo(double x, double y) {
        return new EstadoDoControle(0, 0, 0, x, y, 0, 0);
    }

    private static void aplicar(EstadoDoControle e) {
        Gamepad.aplicar(e);
    }

    @BeforeEach
    @AfterEach
    void limpar() {
        Gamepad.reiniciar();
        Entrada.mouse();
        for (KeyBoard.Key k : KeyBoard.Keys) {
            KeyBoard.KeyPressed(k.getName());
        }
    }

    @Test
    @DisplayName("A confirma: aciona Enter uma vez por toque")
    void aConfirma() {
        aplicar(botoes(Gamepad.Botao.A));
        assertTrue(KeyBoard.KeyPressed("Enter"));
        aplicar(botoes(Gamepad.Botao.A));   // segurando
        assertFalse(KeyBoard.KeyPressed("Enter"), "segurar não pode virar outro aperto");
        aplicar(EstadoDoControle.SOLTO);
        aplicar(botoes(Gamepad.Botao.A));
        assertTrue(KeyBoard.KeyPressed("Enter"), "soltar e apertar de novo é outro aperto");
    }

    @Test
    @DisplayName("soltar o botão solta a tecla")
    void soltarSolta() {
        aplicar(botoes(Gamepad.Botao.RT));
        assertTrue(KeyBoard.KeyPressing("Space"));
        aplicar(EstadoDoControle.SOLTO);
        assertFalse(KeyBoard.KeyPressing("Space"));
    }

    @Test
    @DisplayName("B e Start apertam o mesmo Escape sem se atropelar")
    void doisBotoesUmaTecla() {
        aplicar(botoes(Gamepad.Botao.B, Gamepad.Botao.START));
        aplicar(botoes(Gamepad.Botao.START));
        assertTrue(KeyBoard.KeyPressing("Escape"), "largar o B não solta o Start que segue apertado");
        aplicar(EstadoDoControle.SOLTO);
        assertFalse(KeyBoard.KeyPressing("Escape"));
    }

    @Test
    @DisplayName("em jogo o B interage, e não pausa")
    void bInterageEmJogo() {
        Gamepad.emJogo();
        aplicar(botoes(Gamepad.Botao.B));
        assertTrue(KeyBoard.KeyPressed("Interact"));
        assertFalse(KeyBoard.KeyPressing("Escape"), "B em jogo abria a pausa");
    }

    @Test
    @DisplayName("o B segurado não vira Esc quando a loja abre por cima do jogo")
    void bPresoAoAperto() {
        Gamepad.emJogo();
        aplicar(botoes(Gamepad.Botao.B));          // abre a loja: o jogo pausa
        assertTrue(KeyBoard.KeyPressed("Interact"));
        aplicar(botoes(Gamepad.Botao.B));          // ainda segurando, já sem jogo
        assertFalse(KeyBoard.KeyPressing("Escape"), "a loja fecharia no tick seguinte");
        aplicar(EstadoDoControle.SOLTO);
        aplicar(botoes(Gamepad.Botao.B));          // novo aperto, na loja
        assertTrue(KeyBoard.KeyPressed("Escape"), "nas telas o B volta");
    }

    @Test
    @DisplayName("largar o controle não solta a tecla segurada no teclado")
    void tecladoEControleIndependentes() {
        KeyBoard teclado = new KeyBoard();
        java.awt.Container fonte = new java.awt.Container();   // leve: roda headless
        java.awt.event.KeyEvent w = new java.awt.event.KeyEvent(fonte,
                java.awt.event.KeyEvent.KEY_PRESSED, 0, 0, java.awt.event.KeyEvent.VK_UP, '\0');
        teclado.keyPressed(w);
        aplicar(botoes(Gamepad.Botao.CIMA));
        aplicar(EstadoDoControle.SOLTO);
        assertTrue(KeyBoard.KeyPressing("Up"), "o teclado continua segurando");
        teclado.keyReleased(w);
        assertFalse(KeyBoard.KeyPressing("Up"));
    }

    @Test
    @DisplayName("o analógico empurrado vira direção nos menus, e a zona do meio não")
    void analogicoComoDirecao() {
        aplicar(analogicoEsquerdo(0.3, 0));
        assertFalse(KeyBoard.KeyPressing("Right"), "deflexão pequena só anda, não navega");
        aplicar(analogicoEsquerdo(0.9, 0));
        assertTrue(KeyBoard.KeyPressing("Right"));
        aplicar(analogicoEsquerdo(0, -0.9));
        assertTrue(KeyBoard.KeyPressing("Up"), "Y negativo é para cima, na convenção da tela");
        assertFalse(KeyBoard.KeyPressing("Right"));
    }

    @Test
    @DisplayName("direção segurada repete como o auto-repeat do teclado")
    void autoRepeat() {
        aplicar(botoes(Gamepad.Botao.BAIXO));
        assertTrue(KeyBoard.KeyPressed("Down"));
        int apertos = 0;
        int ticks = Gamepad.REPETE_APOS + 2 * Gamepad.REPETE_CADA;
        for (int t = 1; t <= ticks; t++) {
            aplicar(botoes(Gamepad.Botao.BAIXO));
            if (KeyBoard.KeyPressed("Down")) {
                apertos++;
                assertTrue(t >= Gamepad.REPETE_APOS, "repetiu cedo demais, no tick " + t);
            }
        }
        assertEquals(3, apertos);
    }

    @Test
    @DisplayName("movimento só existe fora da zona morta")
    void movimento() {
        aplicar(EstadoDoControle.SOLTO);
        assertNull(Gamepad.movimento());
        aplicar(analogicoEsquerdo(0.4, -0.2));
        assertArrayEquals(new double[] {0.4, -0.2}, Gamepad.movimento(), 1e-9);
    }

    @Test
    @DisplayName("apertar o controle esconde o mouse das telas; mexer o mouse o traz de volta")
    void modoControle() {
        Entrada.mouse();
        assertTrue(Mouse.getX() >= 0, "no modo mouse a posição é a de verdade");
        aplicar(botoes(Gamepad.Botao.BAIXO));
        assertTrue(Entrada.usandoControle());
        assertFalse(Mouse.on(new java.awt.Rectangle(0, 0, 10_000, 10_000)),
                "um mouse parado em cima de uma carta roubava o foco do D-pad");
        Entrada.mouse();
        assertFalse(Entrada.usandoControle());
        assertTrue(Mouse.on(new java.awt.Rectangle(-1, -1, 10_000, 10_000)));
    }

    @Test
    @DisplayName("segurar o controle parado não toma o jogo de quem mexeu o mouse")
    void seguradoNaoRetoma() {
        aplicar(botoes(Gamepad.Botao.A));
        Entrada.mouse();
        aplicar(botoes(Gamepad.Botao.A));   // o mesmo botão, ainda apertado
        assertFalse(Entrada.usandoControle());
    }

    @Test
    @DisplayName("o analógico direito é olhar, e não aperta tecla nenhuma")
    void analogicoDireitoOlha() {
        aplicar(new EstadoDoControle(0, 0, 0, 0, 0, 0.8, -0.5));
        assertArrayEquals(new double[] {0.8, -0.5}, Gamepad.olhar(), 1e-9);
        for (KeyBoard.Key k : KeyBoard.Keys) {
            assertFalse(KeyBoard.KeyPressing(k.getName()), "o olhar apertou " + k.getName());
        }
        aplicar(EstadoDoControle.SOLTO);
        assertArrayEquals(new double[] {0, 0}, Gamepad.olhar(), 1e-9);
    }

    @Test
    @DisplayName("LB/RB valem um toque cada, e o toque some se ninguém o usar")
    void bumpers() {
        aplicar(botoes(Gamepad.Botao.RB));
        assertTrue(Gamepad.pressionou(Gamepad.Botao.RB));
        assertFalse(Gamepad.pressionou(Gamepad.Botao.RB));
        assertFalse(Gamepad.pressionou(Gamepad.Botao.LB));
        aplicar(botoes(Gamepad.Botao.LB));
        aplicar(EstadoDoControle.SOLTO);
        assertFalse(Gamepad.pressionou(Gamepad.Botao.LB),
                "toque solto sem consumo não pode sobrar para a próxima tela");
    }
}
