package com.retronova.engine.inputs.gamepad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da limpeza da leitura crua da XInput. */
class EstadoDoControleTest {

    @Test
    @DisplayName("analógico parado ou na zona morta lê zero")
    void zonaMorta() {
        double[] centro = EstadoDoControle.analogico((short) 0, (short) 0);
        assertEquals(0, centro[0]);
        assertEquals(0, centro[1]);
        double[] folga = EstadoDoControle.analogico((short) 5000, (short) -5000);
        assertEquals(0, folga[0], "drift de analógico gasto não pode andar sozinho");
        assertEquals(0, folga[1]);
    }

    @Test
    @DisplayName("o curso começa em zero na borda da zona e termina em 1")
    void reescala() {
        short borda = (short) 7900;
        assertEquals(0, EstadoDoControle.analogico(borda, (short) 0)[0], 0.01);
        assertEquals(1, EstadoDoControle.analogico(Short.MAX_VALUE, (short) 0)[0], 1e-9);
        assertEquals(-1, EstadoDoControle.analogico(Short.MIN_VALUE, (short) 0)[0], 1e-9);
    }

    @Test
    @DisplayName("Y da XInput é para cima; o do jogo é para baixo")
    void eixoYInvertido() {
        assertEquals(-1, EstadoDoControle.analogico((short) 0, Short.MAX_VALUE)[1], 1e-9);
    }

    @Test
    @DisplayName("zona morta radial preserva a direção da diagonal")
    void diagonal() {
        double[] v = EstadoDoControle.analogico((short) 20000, (short) 20000);
        assertEquals(Math.abs(v[0]), Math.abs(v[1]), 1e-9);
    }

    @Test
    @DisplayName("gatilho encostado não conta; apertado conta")
    void gatilhos() {
        assertEquals(0, EstadoDoControle.gatilho((byte) 20));
        assertTrue(EstadoDoControle.gatilho((byte) 200) > 0);
        assertEquals(1, EstadoDoControle.gatilho((byte) 0xFF), 1e-9);
    }

    @Test
    @DisplayName("bits de botão mapeiam para o enum")
    void botoes() {
        EstadoDoControle e = EstadoDoControle.deXInput((short) 0x9000, (byte) 0, (byte) 255,
                (short) 0, (short) 0, (short) 0, (short) 0);
        assertTrue(e.pressionado(Gamepad.Botao.A));
        assertTrue(e.pressionado(Gamepad.Botao.Y), "0x8000 é o bit de sinal do short: precisa de & 0xFFFF");
        assertTrue(e.pressionado(Gamepad.Botao.RT));
        assertFalse(e.pressionado(Gamepad.Botao.B));
        assertFalse(e.pressionado(Gamepad.Botao.LT));
    }
}
