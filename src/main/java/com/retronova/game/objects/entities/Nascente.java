package com.retronova.game.objects.entities;

/**
 * Quem CHEGA por um portal, em vez de simplesmente existir.
 *
 * Existe para que o laco de desenho do jogo possa perguntar "voce esta chegando?"
 * sem saber com quem esta falando. Hoje respondem o inimigo e o vendedor, que nao
 * tem parentesco nenhum entre si — um e bicho, o outro e NPC — e mesmo assim
 * chegam da mesma maneira e devem chegar com o mesmo desenho.
 *
 * A alternativa seria repetir o desenho do portal em cada classe que nasce, e essa
 * lista so cresce: o proximo NPC que aparecer no meio da corrida vai querer o
 * mesmo tratamento.
 */
public interface Nascente {

    /** Ainda esta se formando. Enquanto for verdade, nao age e nao pode ser tocado. */
    boolean nascendo();

    /** O tamanho do portal agora, de 0 a 1: abre, segura e fecha. */
    float aberturaDoPortal();

    /**
     * Ja da para ver o corpo, ou so o portal?
     *
     * A ORDEM PEDIDA E: portal, depois o vulto branco, depois o bicho inteiro. As
     * duas primeiras coisas aconteciam juntas — o portal abria com o inimigo ja
     * dentro dele — e por isso a chegada tinha dois tempos em vez de tres. O portal
     * precisa existir sozinho por um instante; e ele que diz ONDE olhar, e o corpo
     * que aparece depois responde o QUE vem.
     */
    boolean corpoVisivel();
}
