package com.retronova.game.items;

import java.awt.Graphics2D;

/**
 * Os selinhos de raridade e de elemento, num lugar só.
 *
 * Eles nasceram na carta de recompensa e agora precisam aparecer também no
 * inventário e na loja — que são telas escritas por outra parte do código e que
 * não têm nada a ver com a arena. Sem isto, o carregamento das imagens e a regra
 * de "qual selo vai onde" existiriam em três lugares, e o terceiro já entraria
 * ligeiramente diferente dos outros dois.
 *
 * A REGRA É SEMPRE A MESMA, e é o que este arquivo garante: raridade sempre,
 * elemento só quando houver. Um item de terra mostra dois selos; a espada inicial
 * mostra um. É essa diferença de quantidade que deixa o inventário legível de
 * relance, antes de qualquer texto ser lido.
 */
public final class Selos {

    private Selos() {
    }

    /**
     * Desenha os selos de um item no CANTO SUPERIOR ESQUERDO de uma área.
     *
     * Esquerda, e não direita: no inventário o canto direito já é do número de
     * pilha dos consumíveis, e dois carimbos disputando a mesma quina fariam um
     * cobrir o outro na primeira poção empilhada.
     *
     * OS SELOS SÃO OS ÍCONES DO REGISTRO (Icone), a gema do grau e o símbolo do
     * elemento, em fator inteiro: eram PNGs de 9x9 esticados para o lado pedido, e
     * esticar por fator quebrado borrava o pixel.
     *
     * @param lado tamanho do selo em pixels; quem chama decide, porque um slot de
     *             inventário e uma carta de recompensa têm escalas diferentes
     */
    public static void desenhar(Graphics2D g, Item item, int x, int y, int lado) {
        if (item == null) {
            return;
        }
        int escala = Icone.escalaPara(lado);
        int passo = Icone.lado(escala) + Math.max(1, lado / 8);
        Icone.de(item.raridade()).desenhar(g, x, y, escala);
        Icone elemento = Icone.de(item.elemento());
        if (elemento != null) {
            elemento.desenhar(g, x + passo, y, escala);
        }
    }
}
