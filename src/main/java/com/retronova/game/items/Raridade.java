package com.retronova.game.items;

import java.awt.Color;

/**
 * O QUANTO UM ITEM É INCOMUM.
 *
 * O jogo tinha elemento — o que a arma É — mas não tinha raridade — o quanto ela
 * vale. São perguntas diferentes e o jogador faz as duas ao ver três cartas: uma
 * espada de gelo e uma espada lendária são as duas "espadas com cor", e sem um
 * segundo eixo elas parecem equivalentes.
 *
 * OS QUATRO DEGRAUS, e o que separa cada um do seguinte:
 *
 *   COMUM — as três armas com que os gatos começam. Não são ruins; são o ponto de
 *   partida contra o qual todo o resto é lido.
 *
 *   RARO — as variantes elementais. O que as torna raras não é força, é ESCOLHA:
 *   cada elemento troca cadência por dano, então pegar uma é decidir como a
 *   corrida vai ser jogada.
 *
 *   ÉPICO — as armas de comportamento próprio: laser, kunai, tridente, foice,
 *   garras. Elas não têm família nem elemento porque cada uma é o seu próprio
 *   assunto — a kunai volta, o tridente atravessa, o laser não tem projétil.
 *
 *   LENDÁRIO — a exceção assumida. É o único degrau em que dano e cadência sobem
 *   juntos, e por isso é o único que não pede escolha nenhuma: quando cai, cai.
 *
 * AS CORES NÃO SÃO INVENTADAS AQUI para o cinza e o dourado: o cinza é o
 * {@code Palette.LIGHT} da interface e o dourado é a cor que o elemento lendário
 * já usa. Azul e roxo são acréscimos, na convenção que praticamente todo jogo do
 * gênero usa há décadas — e convenção herdada é reconhecida sem ser ensinada.
 */
public enum Raridade {

    COMUM("comum", new Color(0x7E849C)),
    RARO("raro", new Color(0x4FA3D9)),
    EPICO("epico", new Color(0xA05FD0)),
    // O SELO DA LENDARIA E O QUE ERA DO ELEMENTO.
    //
    // Sao dois desenhos para a mesma ideia: "lendario.png", feito para a coluna de
    // raridade, e "lendaria.png", feito para a coluna de elemento. Como a lendaria
    // deixou de ter selo de elemento — ela e raridade, nao fogo nem gelo — o
    // segundo desenho ficaria sem uso, e ele e o melhor dos dois.
    LENDARIO("lendaria", new Color(0xFFD850));

    /**
     * As três armas iniciais. Tudo o mais sem elemento é comportamento próprio.
     *
     * A lista é dos IDs e não dos nomes: nome muda quando alguém melhora um texto,
     * e o ordinal do enum é o que os JSON de personagem já gravam.
     */
    private static final java.util.Set<ItemIDs> INICIAIS =
            java.util.EnumSet.of(ItemIDs.Sword, ItemIDs.Bow, ItemIDs.Wand);

    private final String simbolo;
    private final Color cor;

    Raridade(String simbolo, Color cor) {
        this.simbolo = simbolo;
        this.cor = cor;
    }

    /**
     * Quanto esta raridade brilha, de 0 a 1.
     *
     * O BRILHO E UMA ESCADA, e nao um efeito ligado em tudo. Com as tres cartas
     * acesas do mesmo jeito, o brilho nao dizia nada — era enfeite. Numa escada, a
     * propria quantidade de luz vira informacao: a comum nao brilha, e a lendaria e
     * visivelmente a mais acesa da mesa antes de o jogador ler nome nenhum.
     */
    public float brilho() {
        return switch (this) {
            case COMUM -> 0f;
            case RARO -> 0.3f;
            case EPICO -> 0.6f;
            case LENDARIO -> 1f;
        };
    }

    /**
     * O degrau escrito em ingles, para a interface.
     *
     * O enum e em portugues como todo o resto do codigo, mas a tela do jogo e em
     * ingles — "Choose your reward", "Fire Sword". Imprimir COMUM no meio disso
     * seria a unica palavra fora da lingua, e num lugar onde ela e lida sempre.
     */
    public String rotulo() {
        return switch (this) {
            case COMUM -> "Common";
            case RARO -> "Rare";
            case EPICO -> "Epic";
            case LENDARIO -> "Legendary";
        };
    }

    /**
     * Peso desta raridade no sorteio de cartas.
     *
     * RARIDADE E, ANTES DE TUDO, CHANCE DE APARECER — era isso que faltava para a
     * palavra significar alguma coisa. Enquanto a carta era uma arma, "epica" so
     * dizia de que cor era a borda; com melhorias, o grau passa a ser QUANTO a
     * carta rende, e ai a frequencia e o que da peso ao encontro.
     *
     * Os numeros seguem a proporcao do Hades, que e severa de proposito: la o raro
     * sai em dez por cento das vezes e o epico em cinco, ou seja, a grande maioria
     * das ofertas e comum. Um lendario que aparece toda hora nao e lendario — e
     * so o valor normal com outro nome, e o jogador para de comemorar.
     */
    public int peso() {
        return switch (this) {
            case COMUM -> 60;
            case RARO -> 25;
            case EPICO -> 12;
            case LENDARIO -> 3;
        };
    }

    /**
     * Teto da chance de promocao.
     *
     * Sem teto, copias suficientes de Presagio fariam toda carta subir de grau —
     * e uma promocao certa e a mesma coisa que apagar o degrau de baixo. Sessenta
     * por cento ja e enorme e ainda deixa quase metade das cartas no grau sorteado.
     */
    public static final double PROMOCAO_MAXIMA = 0.6;

    /** O degrau imediatamente acima, ou este mesmo se ja for o topo. */
    public Raridade acima() {
        Raridade[] todas = values();
        return ordinal() + 1 < todas.length ? todas[ordinal() + 1] : this;
    }

    /**
     * Sorteia e, com a chance dada, sobe um degrau.
     *
     * A PROMOCAO E DEPOIS DO SORTEIO, e nao um remexer nos pesos. Mexer nos pesos
     * seria mais "exato" e e pior de duas formas: o jogador nao consegue prever o
     * resultado de somar duas cartas, e a conta pode inverter a ordem dos degraus
     * sem ninguem perceber. Promover e uma frase que se entende inteira — "esta
     * carta podia ter vindo um grau acima" — e nunca desarruma a escada.
     *
     * A PROMOCAO NAO ENCADEIA. Uma carta sobe no maximo um degrau, entao um comum
     * nunca vira lendario de uma vez: o presagio aproxima o topo, nao teleporta
     * para ele.
     */
    public static Raridade sortear(double promocao) {
        Raridade tirada = sortear();
        double chance = Math.max(0, Math.min(PROMOCAO_MAXIMA, promocao));
        if (chance > 0 && com.retronova.engine.Engine.RAND.nextDouble() < chance) {
            return tirada.acima();
        }
        return tirada;
    }

    /** Uma raridade ao acaso, respeitando os pesos. */
    public static Raridade sortear() {
        int total = 0;
        for (Raridade r : values()) {
            total += r.peso();
        }
        int tirada = com.retronova.engine.Engine.RAND.nextInt(total);
        for (Raridade r : values()) {
            tirada -= r.peso();
            if (tirada < 0) {
                return r;
            }
        }
        return COMUM;
    }

    /** Nome do arquivo do selo desta raridade. */
    public String simbolo() {
        return this.simbolo;
    }

    public Color cor() {
        return this.cor;
    }

    /**
     * Em que degrau este item cai.
     *
     * A regra é derivada, e não uma tabela: tabela de item para raridade envelhece
     * na primeira arma nova que alguém escrever sem lembrar de atualizá-la. Aqui a
     * pergunta é feita ao próprio item — que elemento ele tem, e se ele é um dos
     * três de partida — e uma arma nova cai sozinha no degrau certo.
     */
    public static Raridade de(Item item) {
        Elemento elemento = item.elemento();
        if (elemento == Elemento.LENDARIA) {
            return LENDARIO;
        }
        if (elemento != Elemento.NENHUM) {
            return RARO;
        }
        ItemIDs[] ids = ItemIDs.values();
        int id = item.getID();
        if (id >= 0 && id < ids.length && INICIAIS.contains(ids[id])) {
            return COMUM;
        }
        return EPICO;
    }
}
