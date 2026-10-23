package com.retronova.menus;

import com.retronova.game.objects.entities.Player;
import java.util.List;

/** Fotografia dos valores reais ANTES de descartar a partida; mesma fonte do Status. */
public record ResumoFinalDoGato(List<TelaDeStatus.Linha> atributos,
                               List<TelaDeStatus.Linha> armas,
                               List<TelaDeStatus.Linha> melhorias) {
    public ResumoFinalDoGato {
        atributos = List.copyOf(atributos);
        armas = List.copyOf(armas);
        melhorias = List.copyOf(melhorias);
    }

    public static ResumoFinalDoGato capturar(Player gato) {
        return new ResumoFinalDoGato(TelaDeStatus.atributos(gato),
                TelaDeStatus.armas(gato), TelaDeStatus.buffs(gato));
    }

    static ResumoFinalDoGato vazio() {
        return new ResumoFinalDoGato(List.of(), List.of(), List.of());
    }
}
