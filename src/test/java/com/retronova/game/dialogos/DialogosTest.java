package com.retronova.game.dialogos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os arquivos de diálogo e a escolha da conversa.
 *
 * Erro de dado não dá erro de compilação: um texto faltando vira "[chave]" na
 * tela, uma condição digitada errado cala a conversa. Por isso os arquivos reais
 * são conferidos aqui, e a escolha é testada nos contextos que o vendedor usa.
 */
class DialogosTest {

    private static List<Conversa> doVendedor() {
        return CatalogoDeDialogos.get().conversas();
    }

    private static ContextoDoDialogo ctx(MemoriaDosDialogos.Dados m) {
        return new ContextoDoDialogo("Muffin", 100, 0, 0, m);
    }

    private static Conversa escolher(MemoriaDosDialogos.Dados m) {
        return EscolhaDeConversa.escolher(doVendedor(), "vendedor", "interagir", ctx(m), new Random(7));
    }

    @Test
    @DisplayName("Os arquivos de diálogo carregam sem texto, personagem, condição ou evento faltando")
    void arquivosValidos() {
        CatalogoDeDialogos c = CatalogoDeDialogos.get();
        assertTrue(c.problemas().isEmpty(), "problemas nos dialogos: " + c.problemas());
        assertNotNull(c.personagem("vendedor"));
        assertTrue(c.conversas().size() >= 10);
    }

    @Test
    @DisplayName("Primeira vez: a apresentação, e só uma vez")
    void apresentacao() {
        MemoriaDosDialogos.Dados m = new MemoriaDosDialogos.Dados();
        Conversa primeira = escolher(m);
        assertEquals("vendedor.apresentacao", primeira.id());
        m.marcar(primeira);
        assertNotEquals("vendedor.apresentacao", escolher(m).id());
    }

    @Test
    @DisplayName("Depois de uma morte rápida, o vendedor provoca")
    void morteRapida() {
        MemoriaDosDialogos.Dados m = new MemoriaDosDialogos.Dados();
        m.marcar(doVendedor().get(0));
        m.fimDeCorrida("morte", 45, 3, 10, "Muffin");
        assertTrue(escolher(m).id().startsWith("vendedor.morte_rapida"));
    }

    @Test
    @DisplayName("Depois de uma vitória, a primeira vez é especial e depois varia")
    void vitoria() {
        MemoriaDosDialogos.Dados m = new MemoriaDosDialogos.Dados();
        m.marcar(doVendedor().get(0));
        m.fimDeCorrida("vitoria", 1800, 40, 5000, "Finn");
        ContextoDoDialogo venceu = new ContextoDoDialogo("Finn", 100, 0, 1, m);
        Conversa c = EscolhaDeConversa.escolher(doVendedor(), "vendedor", "interagir", venceu, new Random(1));
        assertEquals("vendedor.primeira_vitoria", c.id());
        m.marcar(c);
        c = EscolhaDeConversa.escolher(doVendedor(), "vendedor", "interagir", venceu, new Random(1));
        assertTrue(c.id().startsWith("vendedor.vitoria."));
    }

    @Test
    @DisplayName("As genéricas giram: nenhuma repete antes de todas tocarem, e nunca duas vezes seguidas")
    void semRepeticao() {
        MemoriaDosDialogos.Dados m = new MemoriaDosDialogos.Dados();
        m.marcar(doVendedor().get(0));   // passou da apresentação
        ContextoDoDialogo semGato = new ContextoDoDialogo("", 100, 0, 0, m);
        Set<String> vistas = new HashSet<>();
        String anterior = null;
        for (int i = 0; i < 4; i++) {
            Conversa c = EscolhaDeConversa.escolher(doVendedor(), "vendedor", "interagir", semGato, new Random(i));
            assertTrue(c.id().startsWith("vendedor.generica"), c.id());
            assertTrue(vistas.add(c.id()), "repetiu antes de girar: " + c.id());
            m.marcar(c);
            anterior = c.id();
        }
        Conversa quinta = EscolhaDeConversa.escolher(doVendedor(), "vendedor", "interagir", semGato, new Random(99));
        assertNotEquals(anterior, quinta.id());
    }

    @Test
    @DisplayName("Condição desconhecida reprova a conversa em vez de aprovar")
    void condicaoDesconhecida() {
        Conversa c = new Conversa("teste", "vendedor", "interagir", 999,
                Map.of("condicao_que_nao_existe", true), Conversa.Repeticao.SEMPRE,
                List.of(new Fala("vendedor", Emocao.FELIZ, "x", List.of())), List.of());
        assertNull(EscolhaDeConversa.escolher(List.of(c), "vendedor", "interagir",
                ctx(new MemoriaDosDialogos.Dados()), new Random(0)));
    }

    @Test
    @DisplayName("Emoção desconhecida vira neutra")
    void emocaoPadrao() {
        assertEquals(Emocao.NEUTRO, Emocao.de("furioso_demais"));
        assertEquals(Emocao.MALICIOSO, Emocao.de("smug"));
    }

    @Test
    @DisplayName("A memória volta igual do JSON")
    void memoriaEmJson() throws Exception {
        MemoriaDosDialogos.Dados m = new MemoriaDosDialogos.Dados();
        m.marcar(doVendedor().get(0));
        m.marcar(doVendedor().get(3));
        m.fimDeCorrida("vitoria", 1800, 41, 3000, "Azrael");
        MemoriaDosDialogos.Dados de = MemoriaDosDialogos.Dados.deJson(m.paraJson());
        assertEquals(1, de.vezes(doVendedor().get(0).id()));
        assertEquals(2, de.conversasCom("vendedor"));
        assertEquals(m.vistaEm(doVendedor().get(3).id()), de.vistaEm(doVendedor().get(3).id()));
        assertEquals("vitoria", de.ultimoResultado());
        assertEquals(1800, de.ultimaDuracao());
        assertEquals(1, de.corridas());
    }
}
