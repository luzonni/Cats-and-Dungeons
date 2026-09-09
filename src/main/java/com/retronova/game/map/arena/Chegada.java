package com.retronova.game.map.arena;

import com.retronova.engine.graphics.Alpha;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A CHEGADA NA ARENA: o portal abre, o gato aparece, e só então a briga começa.
 *
 * O QUE ACONTECIA ANTES. O jogador caía no meio da sala já pronto, e a primeira
 * onda nascia no mesmo instante. Não havia um segundo entre estar na sala anterior
 * e estar cercado — o turno começava antes de o jogador ter olhado para ele.
 *
 * A ORDEM AQUI É O PONTO, e é o que foi pedido: primeiro o portal com o gato,
 * DEPOIS os bichos. Os dois juntos anulam um ao outro — o clarão do portal e o
 * branco dos inimigos nascendo competem pela mesma atenção, e o jogador não lê
 * nenhum dos dois. Em sequência, cada um tem a sua vez: "cheguei", e então "eles
 * estão vindo dali".
 *
 * TRÊS TEMPOS, pela mesma razão da queda na morte — o que dá peso a um momento é
 * segurá-lo, não enchê-lo de quadros:
 *
 *   ABRIR (24) — o portal cresce no chão, sozinho. O gato ainda não está lá.
 *   SURGIR (30) — ele aparece de dentro, ganhando cor, como os inimigos ganham.
 *   FECHAR (18) — o portal encolhe e some, e a arena solta as ondas.
 *
 * Enquanto isso o gato não anda e não apanha: {@link Player#morrendo()} já ensinou
 * que estado que suspende o controle precisa também suspender o dano, senão o
 * jogador perde vida numa cena em que não podia reagir.
 */
public class Chegada {

    private static final int ABRIR = 24;
    private static final int SURGIR = 30;
    private static final int FECHAR = 18;
    private static final int TOTAL = ABRIR + SURGIR + FECHAR;

    private final SpriteSheet folha;
    private int t;

    public Chegada() {
        this.folha = new SpriteSheet("sprites/objects/furniture", new String[]{"portalCistern"});
    }

    /** A chegada terminou; a arena pode soltar as ondas. */
    public boolean acabou() {
        return t >= TOTAL;
    }

    /** O gato já está visível, mesmo que o portal ainda não tenha fechado. */
    public boolean gatoVisivel() {
        return t >= ABRIR;
    }

    public void tick() {
        if (acabou()) {
            return;
        }
        // O SOM SAI NO PRIMEIRO TICK, E NAO NO CONSTRUTOR.
        //
        // A arena e construida ANTES de o jogo trocar de mapa, e a troca de mapa
        // corta todos os efeitos — para que nenhum som da sala anterior vaze para a
        // seguinte. O portal do gato nascia nesse intervalo e era cortado pela
        // propria limpeza, meio segundo depois de tocar: era por isso que a chegada
        // era silenciosa. Tocando no primeiro tick, a limpeza ja passou.
        if (t == 0) {
            Sound.play(Sounds.Portal);
        }
        t++;
        // O portal roda os proprios quadros enquanto existe.
        if (t % 6 == 0) {
            folha.plusIndex();
        }
    }

    /**
     * Quanto do gato já apareceu, de 0 a 1.
     *
     * Serve ao mesmo clarão que os inimigos usam ao nascer: um só recurso para
     * "está se formando", em vez de dois jeitos diferentes de dizer a mesma coisa.
     */
    public float formacao() {
        if (t <= ABRIR) {
            return 0f;
        }
        return Math.min(1f, (t - ABRIR) / (float) SURGIR);
    }

    /**
     * Desenha o portal no chão, sob o gato.
     *
     * ELE CRESCE E ENCOLHE em vez de aparecer inteiro: um disco que surge do nada
     * no tamanho final lê como um decalque colado ali, e não como algo abrindo.
     */
    public void render(Graphics2D g, Player jogador) {
        if (acabou()) {
            return;
        }
        float escala;
        if (t < ABRIR) {
            escala = t / (float) ABRIR;
        } else if (t < ABRIR + SURGIR) {
            escala = 1f;
        } else {
            escala = 1f - (t - ABRIR - SURGIR) / (float) FECHAR;
        }
        if (escala <= 0.01f) {
            return;
        }
        // NO CENTRO EXATO DO TILE em que o gato nasce.
        //
        // A primeira versão ancorava no PÉ do desenho, deslocada dois pixels, para
        // o portal parecer estar no chão. Ficou fora de centro: quem olha compara o
        // portal com o tile embaixo dele, e qualquer desvio salta. A caixa do gato
        // ocupa exatamente um tile, então o centro dela É o centro do tile — e é o
        // único ponto que não precisa de ajuste nenhum para bater.
        desenhar(g, folha.getSprite(),
                (int) jogador.getX() + jogador.getWidth() / 2,
                (int) jogador.getY() + jogador.getHeight() / 2, escala);
    }

    /**
     * Desenha um portal centrado num ponto. Compartilhado com o nascimento dos
     * inimigos: o gato e os bichos chegam da mesma maneira, então chegam com o
     * mesmo desenho — duas animações diferentes para o mesmo acontecimento fariam o
     * jogador procurar uma diferença que não existe.
     */
    public static void desenhar(Graphics2D g, BufferedImage arte,
                                int centroX, int centroY, float escala) {
        if (arte == null || escala <= 0.01f) {
            return;
        }
        int lado = (int) (GameObject.SIZE() * 1.4 * escala);
        BufferedImage esmaecido = Alpha.getImage(arte, Math.min(1f, escala + 0.2f));
        g.drawImage(esmaecido, centroX - lado / 2, centroY - lado / 2, lado, lado, null);
    }
}
