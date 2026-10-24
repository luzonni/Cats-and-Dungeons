package com.retronova.game.dialogos;

import java.util.List;

/**
 * Uma fala: quem fala, com que cara, o quê, e o que acontece quando ela começa.
 *
 * O TEXTO É UMA CHAVE, não a frase. A frase mora em {@code dialogos/textos.json},
 * num lugar só, para que uma tradução futura troque um arquivo e não caçe frases
 * espalhadas pelas conversas.
 *
 * @param personagem id do personagem (ver {@code dialogos/personagens.json})
 * @param emocao     a cara do retrato nesta fala
 * @param texto      chave da frase em {@code textos.json}
 * @param eventos    eventos disparados quando a fala aparece (ver EventosDoDialogo)
 */
public record Fala(String personagem, Emocao emocao, String texto, List<String> eventos) {

    public Fala {
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
        emocao = emocao == null ? Emocao.NEUTRO : emocao;
    }
}
