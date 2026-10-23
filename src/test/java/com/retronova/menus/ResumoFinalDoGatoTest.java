package com.retronova.menus;

import com.retronova.engine.graphics.Palette;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResumoFinalDoGatoTest {
    @Test
    void fichaSobreviveAAlteracoesNasListasDaPartida() {
        var dados = new ArrayList<TelaDeStatus.Linha>();
        dados.add(new TelaDeStatus.Linha("Damage", "42", Palette.TEXT, null, "Final damage"));
        var ficha = new ResumoFinalDoGato(dados, dados, dados);
        dados.clear();
        assertEquals("42", ficha.atributos().getFirst().valor());
        assertEquals(1, ficha.armas().size());
        assertEquals(1, ficha.melhorias().size());
        assertThrows(UnsupportedOperationException.class, () -> ficha.atributos().clear());
    }

    @Test
    void faltaDeFichaNaoInventaValores() {
        var ficha = ResumoFinalDoGato.vazio();
        assertEquals(List.of(), ficha.atributos());
        assertTrue(ficha.armas().isEmpty() && ficha.melhorias().isEmpty());
    }
}
