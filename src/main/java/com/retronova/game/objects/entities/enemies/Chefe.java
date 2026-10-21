package com.retronova.game.objects.entities.enemies;

/**
 * A base dos chefes: nome, barra de vida na tela e FASES.
 *
 * Cada chefe era uma classe solta que perseguia o gato do começo ao fim, sem
 * mostrar quanta vida restava nem mudar de comportamento. A luta era "bater até
 * cair". Aqui o chefe passa a ter fases, que são fatias iguais da vida: com três
 * fases, a segunda começa em 66% e a terceira em 33%. Quem estende só decide o
 * que muda em cada uma, em {@link #aoMudarDeFase}.
 *
 * A fase é função pura da vida ({@link #faseDe}), e não um contador que sobe:
 * assim não existe estado para ficar fora de sincronia, e dá para testar sem
 * abrir o jogo.
 */
public abstract class Chefe extends Enemy {

    private int fase;

    protected Chefe(int ID, double x, double y, int tamanho) {
        super(ID, x, y, tamanho);
    }

    /** O nome que a barra de vida mostra. */
    public abstract String nome();

    /** Quantas fases a luta tem. Um chefe de uma fase só não muda de comportamento. */
    public int fases() {
        return 1;
    }

    @Override
    public final boolean chefe() {
        return true;
    }

    /** A fase atual, de 0 a {@code fases() - 1}. */
    public int fase() {
        return fase;
    }

    /** Chamado uma vez ao entrar numa fase nova. Aqui o chefe se enfurece, invoca, muda de golpe. */
    protected void aoMudarDeFase(int nova) {
    }

    /**
     * Confere a fase. Cada chefe chama isto no começo do próprio {@code tick}.
     *
     * Só sobe: se a vida voltar (cura, ou {@code escalarVida} logo depois de
     * nascer), a luta não regride para uma fase que já foi dada como acabada.
     */
    protected final void verificarFase() {
        int atual = faseDe(getLife(), getLifeSize(), fases());
        while (fase < atual) {
            fase++;
            aoMudarDeFase(fase);
        }
    }

    /**
     * A fase para uma vida: fatias iguais, da cheia (fase 0) à quase morta (última).
     *
     * @param vida   vida atual
     * @param maxima vida máxima; zero ou negativa conta como fase 0
     * @param fases  quantas fases a luta tem
     */
    public static int faseDe(double vida, double maxima, int fases) {
        if (fases <= 1 || maxima <= 0) {
            return 0;
        }
        double fracao = Math.max(0, Math.min(1, vida / maxima));
        int fase = (int) Math.floor((1 - fracao) * fases);
        return Math.min(fases - 1, fase);
    }
}
