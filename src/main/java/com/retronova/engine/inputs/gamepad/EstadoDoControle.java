package com.retronova.engine.inputs.gamepad;

/**
 * Uma leitura do controle, já limpa.
 *
 * Analógicos em [-1, 1] com a zona morta aplicada, e o eixo Y JÁ NA CONVENÇÃO DA
 * TELA: positivo é para baixo, como no resto do jogo. A XInput entrega o Y para
 * cima, e inverter aqui evita que cada consumidor tenha que lembrar disso.
 *
 * Java puro, sem JNA: é o que deixa o mapeamento ser testado sem controle e sem
 * Windows.
 */
public record EstadoDoControle(int botoes, double lt, double rt,
                               double lx, double ly, double rx, double ry) {

    /**
     * Zona morta do analógico esquerdo recomendada pela Microsoft, 7849 de 32767.
     * Vale para os dois: a do direito (8689) é quase igual e não faz diferença no
     * cursor.
     */
    static final double ZONA_MORTA = 7849 / 32767d;
    /** Limiar dos gatilhos recomendado pela Microsoft: 30 de 255. */
    static final double LIMIAR_GATILHO = 30 / 255d;

    public static final EstadoDoControle SOLTO = new EstadoDoControle(0, 0, 0, 0, 0, 0, 0);

    static EstadoDoControle deXInput(XInput.Controle c) {
        return deXInput(c.wButtons, c.bLeftTrigger, c.bRightTrigger,
                c.sThumbLX, c.sThumbLY, c.sThumbRX, c.sThumbRY);
    }

    static EstadoDoControle deXInput(short botoes, byte lt, byte rt,
                                     short lx, short ly, short rx, short ry) {
        double[] esquerdo = analogico(lx, ly);
        double[] direito = analogico(rx, ry);
        return new EstadoDoControle(botoes & 0xFFFF, gatilho(lt), gatilho(rt),
                esquerdo[0], esquerdo[1], direito[0], direito[1]);
    }

    /**
     * Zona morta RADIAL, e não por eixo.
     *
     * Por eixo, empurrar o analógico quase na diagonal cai na zona de um dos eixos
     * e o gato anda reto: as diagonais ficam "grudentas". Medindo o raio, só o
     * centro é morto. O que sobra é reescalado para ainda começar em zero na borda
     * da zona, senão o movimento daria um salto ao sair dela.
     */
    static double[] analogico(short x, short y) {
        double nx = Math.max(-1, x / 32767d);
        double ny = -Math.max(-1, y / 32767d);
        double raio = Math.hypot(nx, ny);
        if (raio <= ZONA_MORTA) {
            return new double[] {0, 0};
        }
        double util = (Math.min(raio, 1) - ZONA_MORTA) / (1 - ZONA_MORTA);
        return new double[] {nx / raio * util, ny / raio * util};
    }

    static double gatilho(byte valor) {
        double v = (valor & 0xFF) / 255d;
        return v < LIMIAR_GATILHO ? 0 : v;
    }

    public boolean pressionado(Gamepad.Botao botao) {
        return switch (botao) {
            case LT -> lt > 0;
            case RT -> rt > 0;
            default -> (botoes & botao.mascara) != 0;
        };
    }
}
