package com.retronova.game.dialogos;

/**
 * O que o jogo sabe AGORA, para as condições perguntarem.
 *
 * É uma fotografia, montada uma vez por escolha: a lógica de escolher conversa
 * fica pura (só lê este registro e a memória) e dá para testar sem jogo aberto.
 *
 * @param gato          nome do gato em jogo ("Muffin", "Azrael", "Finn"), ou vazio
 * @param moedas        moedas do gato
 * @param ouro          ouro guardado entre corridas (Meta)
 * @param vitorias      vitórias de todas as corridas (Conquistas)
 * @param memoria       conversas vistas e a última corrida (MemoriaDosDialogos)
 */
public record ContextoDoDialogo(String gato, int moedas, int ouro, int vitorias,
                                MemoriaDosDialogos.Dados memoria) {

    public ContextoDoDialogo {
        gato = gato == null ? "" : gato;
        memoria = memoria == null ? new MemoriaDosDialogos.Dados() : memoria;
    }
}
