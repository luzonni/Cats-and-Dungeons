package com.retronova.game.dialogos;

import java.util.List;

/**
 * Quem fala: o nome na aba, o retrato, a voz e a moldura.
 *
 * @param id      chave usada nas falas
 * @param nome    texto da aba (tela em inglês)
 * @param retrato nome do PNG em {@code sprites/retratos/}; vazio = sem retrato
 * @param voz     os blips da fala, em ordem de sorteio; vazia = muda
 * @param moldura nome da moldura 9-slice em {@code ui/}; troca o tema da caixa
 */
public record Personagem(String id, String nome, String retrato, List<String> voz, String moldura) {

    /** Moldura padrão, na paleta da interface. */
    public static final String MOLDURA_PADRAO = "dialogo";

    public Personagem {
        voz = voz == null ? List.of() : List.copyOf(voz);
        moldura = moldura == null || moldura.isBlank() ? MOLDURA_PADRAO : moldura;
    }
}
