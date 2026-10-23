package com.retronova.game.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImpactoDaMorteTest {
    @Test
    void impulsoTemAmplitudeTrocaDeDirecaoETermina() {
        assertEquals(ImpactoDaMorte.AMPLITUDE, ImpactoDaMorte.xEm(0));
        assertTrue(ImpactoDaMorte.xEm(3) < -3);
        assertTrue(ImpactoDaMorte.xEm(6) > 3);
        assertNotEquals(ImpactoDaMorte.yEm(2), ImpactoDaMorte.yEm(3));
        assertEquals(0, ImpactoDaMorte.xEm(ImpactoDaMorte.DURACAO));
        assertEquals(0, ImpactoDaMorte.yEm(100));
    }

    @Test
    void relogioNaoAvancaNoDesenhoEReiniciaNaProximaMorte() {
        var impacto = new ImpactoDaMorte();
        assertEquals(0, impacto.x(0));
        impacto.iniciar();
        double x = impacto.x(0.5f);
        for (int i = 0; i < 240; i++) assertEquals(x, impacto.x(0.5f));
        for (int i = 0; i < ImpactoDaMorte.DURACAO; i++) impacto.tick();
        assertEquals(0, impacto.x(0));
        assertEquals(0, impacto.y(0));
        impacto.iniciar();
        assertEquals(ImpactoDaMorte.AMPLITUDE, impacto.x(0));
    }
}
