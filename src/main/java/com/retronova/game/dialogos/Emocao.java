package com.retronova.game.dialogos;

/**
 * As emoções que uma fala pode pedir, e a coluna do retrato que mostra cada uma.
 *
 * UMA LISTA SÓ, E CURTA. As referências convergem num núcleo pequeno: o Stardew
 * Valley tem neutro, feliz, triste, bravo e um retrato "próprio" por personagem;
 * o Celeste e o Hades variam os nomes, mas cada personagem tem um punhado de
 * caras, não dezenas. Poucas emoções, bem desenhadas, leem melhor a 32 pixels do
 * que muitas que se confundem.
 *
 * MALICIOSO entrou além das seis do pedido: o vendedor provoca o gato depois de
 * uma morte rápida, e nenhuma das seis diz "zombando". É o equivalente ao retrato
 * próprio do Stardew, só que com nome.
 *
 * A ORDEM É A DAS COLUNAS DO RETRATO (ver Retrato e tools/GenRetratos.java):
 * emoção nova entra no FIM, senão as folhas já desenhadas trocam de cara.
 */
public enum Emocao {
    NEUTRO("neutral"),
    FELIZ("happy"),
    TRISTE("sad"),
    BRAVO("angry"),
    CONFUSO("confused"),
    SURPRESO("surprised"),
    MALICIOSO("smug");

    /** O nome usado nos arquivos de dados. */
    private final String chave;

    Emocao(String chave) {
        this.chave = chave;
    }

    public String chave() {
        return chave;
    }

    /** A emoção pelo nome do arquivo de dados; desconhecida ou vazia vira {@link #NEUTRO}. */
    public static Emocao de(String chave) {
        if (chave == null) {
            return NEUTRO;
        }
        for (Emocao e : values()) {
            if (e.chave.equalsIgnoreCase(chave) || e.name().equalsIgnoreCase(chave)) {
                return e;
            }
        }
        return NEUTRO;
    }
}
