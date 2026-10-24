package com.retronova.game.espelho;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tabela do espelho e a conta base + espelho + corrida.
 *
 * Os números são para o balanceamento mexer à vontade; o que se guarda aqui são
 * as regras que não podem quebrar: cada id único (ele é gravado no save), custos
 * crescentes, ícone que existe, e a decomposição que a tela de Status mostra.
 */
class EspelhoTest {

    @Test
    @DisplayName("A tabela do arquivo carrega, com ids únicos, ícones existentes e custos crescentes")
    void tabela() {
        List<MelhoriaDoEspelho> t = Espelho.tabela();
        assertTrue(t.size() >= 10, "tabela vazia ou incompleta: " + t.size());
        Set<String> ids = new HashSet<>();
        for (MelhoriaDoEspelho m : t) {
            assertTrue(ids.add(m.id()), "id repetido: " + m.id());
            assertTrue(m.nivelMaximo() >= 1, m.id());
            assertTrue(m.texto().contains("{v}"), m.id() + " sem {v} no texto");
            assertTrue(com.retronova.game.items.Icone.pelaChave(m.icone()) != com.retronova.game.items.Icone.DESCONHECIDO,
                    m.id() + " com icone desconhecido " + m.icone());
            for (int i = 1; i < m.nivelMaximo(); i++) {
                assertTrue(m.custo(i) > m.custo(i - 1), m.id() + ": custo nao cresce no nivel " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("As melhorias migradas do meta.json antigo existem na tabela, com pelo menos 5 níveis")
    void idsDaMigracao() {
        for (String id : List.of("vida", "dano", "velocidade", "sorte", "coleta")) {
            MelhoriaDoEspelho m = Espelho.porId(id);
            assertNotNull(m, id);
            assertTrue(m.nivelMaximo() >= 5, id + ": um save antigo pode ter nivel 5");
        }
    }

    @Test
    @DisplayName("Custo, gasto acumulado, valor e texto de uma melhoria")
    void contasDaMelhoria() {
        MelhoriaDoEspelho m = new MelhoriaDoEspelho("x", "X", "", "VIGOR", Espelho.Efeito.VIDA,
                6, 1, 0, "+{v} max health", List.of(40, 80, 140));
        assertEquals(40, m.custo(0));
        assertEquals(-1, m.custo(3), "no maximo nao ha proximo nivel");
        assertEquals(120, m.gasto(2));
        assertEquals(18, m.valor(3), 1e-9);
        assertEquals(18, m.valor(9), 1e-9, "nivel acima do maximo nao soma mais");
        assertEquals("+12 max health", m.efeitoNoNivel(2));
        MelhoriaDoEspelho pct = new MelhoriaDoEspelho("y", "Y", "", "IMA", Espelho.Efeito.COLETA,
                0.10, 100, 0, "+{v}% pickup range", List.of(30, 60));
        assertEquals("+20% pickup range", pct.efeitoNoNivel(2));
    }

    @Test
    @DisplayName("Base + espelho + corrida: a corrida é o que os modificadores somam além do espelho")
    void decompor() {
        // Muffin: vida base 80, espelho +12, cartas +20 -> total 112, modificador 32.
        Espelho.Camadas c = Espelho.decompor(80, 32, 12);
        assertEquals(80, c.base(), 1e-9);
        assertEquals(12, c.espelho(), 1e-9);
        assertEquals(20, c.corrida(), 1e-9);
        assertEquals(112, c.total(), 1e-9);
        // Sem cartas, a corrida é zero; sem espelho, tudo é da corrida.
        assertEquals(0, Espelho.decompor(80, 12, 12).corrida(), 1e-9);
        assertEquals(15, Espelho.decompor(10, 15, 0).corrida(), 1e-9);
    }

    @Test
    @DisplayName("O balanceamento inicial cabe na estimativa: tudo de um gato entre 7 e 12 mil moedas")
    void custoTotal() {
        int total = 0;
        for (MelhoriaDoEspelho m : Espelho.tabela()) {
            total += m.gasto(m.nivelMaximo());
        }
        assertTrue(total >= 7000 && total <= 12000, "custo total " + total
                + " fora da faixa; atualize a estimativa em docs/ARQUITETURA.md se for de proposito");
    }
}
