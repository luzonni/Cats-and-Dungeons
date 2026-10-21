package com.retronova.game.objects.entities;

import com.retronova.game.items.Passiva;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Os numeros e as regras das habilidades do Muffin e do Azrael, sem abrir o jogo. */
class HabilidadesDosGatosTest {

    @Test
    void cadaHabilidadeSoSaiParaOProprioGato() {
        assertTrue(Passiva.RETALIACAO.disponivelPara("Muffin"));
        assertFalse(Passiva.RETALIACAO.disponivelPara("Finn"));
        assertFalse(Passiva.RETALIACAO.disponivelPara("Azrael"));

        assertTrue(Passiva.EXPLOSAO_ARCANA.disponivelPara("Azrael"));
        assertFalse(Passiva.EXPLOSAO_ARCANA.disponivelPara("Muffin"));
        assertFalse(Passiva.EXPLOSAO_ARCANA.disponivelPara("Finn"));

        assertTrue(Passiva.RETALIACAO.exclusiva());
        assertTrue(Passiva.EXPLOSAO_ARCANA.exclusiva());
    }

    @Test
    void asHabilidadesVaoAteONivelTres() {
        assertEquals(3, Passiva.RETALIACAO.nivelMaximo());
        assertEquals(3, Passiva.EXPLOSAO_ARCANA.nivelMaximo());
        assertEquals(3, Passiva.VOLEIO.nivelMaximo());
    }

    @Test
    void aCartaDizAlgoDiferenteEmCadaNivel() {
        for (Passiva p : new Passiva[]{Passiva.RETALIACAO, Passiva.EXPLOSAO_ARCANA}) {
            String n1 = p.efeitoNoNivel(1), n2 = p.efeitoNoNivel(2), n3 = p.efeitoNoNivel(3);
            assertFalse(n1.isBlank(), p + " nivel 1 sem texto");
            assertNotEquals(n1, n2, p.name());
            assertNotEquals(n2, n3, p.name());
            assertEquals(n3, p.efeitoNoNivel(9), "acima do teto fica no ultimo texto");
            assertEquals(n1, p.efeitoNoNivel(0), "abaixo de 1 fica no primeiro");
        }
        // As passivas comuns continuam no formato de sempre.
        assertEquals(Passiva.AREA.efeitoDeUmNivel(), Passiva.AREA.efeitoNoNivel(2));
    }

    @Test
    void aOndaDoMuffinCresceENaoViraImunidade() {
        for (int n = 1; n < 3; n++) {
            assertTrue(Bonus.raioDaRetaliacao(n + 1) > Bonus.raioDaRetaliacao(n), "raio cresce");
            assertTrue(Bonus.danoDaRetaliacao(n + 1, 10) > Bonus.danoDaRetaliacao(n, 10), "dano cresce");
            assertTrue(Bonus.recargaDaRetaliacao(n + 1) < Bonus.recargaDaRetaliacao(n), "recarga cai");
            assertTrue(Bonus.empurraoDaRetaliacao(n + 1) > Bonus.empurraoDaRetaliacao(n), "empurrao cresce");
        }
        // A recarga nunca chega perto da invulnerabilidade de 24 ticks: a onda nao
        // pode sair a cada golpe de um cerco.
        assertTrue(Bonus.recargaDaRetaliacao(3) >= 24 * 4);
    }

    @Test
    void oDanoDaOndaAcompanhaODanoDoGato() {
        assertTrue(Bonus.danoDaRetaliacao(1, 30) > Bonus.danoDaRetaliacao(1, 10));
        assertEquals(Bonus.danoDaRetaliacao(1, 0), Bonus.danoDaRetaliacao(1, -5), 1e-9,
                "dano negativo do gato nao tira dano da onda");
    }

    @Test
    void aExplosaoDoAzraelNuncaPassaDoGolpe() {
        for (int n = 1; n <= 3; n++) {
            double f = Bonus.fracaoDaExplosao(n);
            assertTrue(f > 0 && f < 1, "fracao entre 0 e 1 no nivel " + n);
        }
        for (int n = 1; n < 3; n++) {
            assertTrue(Bonus.fracaoDaExplosao(n + 1) > Bonus.fracaoDaExplosao(n));
            assertTrue(Bonus.raioDaExplosao(n + 1) > Bonus.raioDaExplosao(n));
        }
    }

    @Test
    void nivelForaDoIntervaloFicaNasPontas() {
        assertEquals(Bonus.raioDaRetaliacao(1), Bonus.raioDaRetaliacao(0), 1e-9);
        assertEquals(Bonus.raioDaRetaliacao(3), Bonus.raioDaRetaliacao(7), 1e-9);
        assertEquals(Bonus.fracaoDaExplosao(1), Bonus.fracaoDaExplosao(-2), 1e-9);
        assertEquals(Bonus.fracaoDaExplosao(3), Bonus.fracaoDaExplosao(4), 1e-9);
    }

    @Test
    void aFichaDoGatoAnunciaAHabilidade() {
        assertTrue(Bonus.habilidade("Muffin").contains("Retaliate"));
        assertTrue(Bonus.habilidade("Azrael").contains("Arcane Burst"));
        assertTrue(Bonus.habilidade("Finn").contains("Volley"));
    }
}
