package com.retronova.game.items;

import com.retronova.game.objects.entities.AttackTypes;

import java.awt.Color;

/**
 * O elemento de uma arma: o que ela e alem da cor.
 *
 * O pedido foi explicito — "nao e pra ser so ruido": espada de fogo tem de dar
 * dano de fogo. Entao o elemento nao mora no nome nem no PNG, mora aqui, e
 * carrega as tres coisas que ele muda no jogo: o TIPO DE DANO, que passa pelo
 * mapa de resistencias de cada inimigo em Entity.strike; o MULTIPLICADOR, que e o
 * unico jeito de "lendaria" significar alguma coisa sem inventar um elemento que
 * ninguem resiste; e a COR, usada nos efeitos desenhados em tela, para o feitico
 * na tela combinar com a arma na mao.
 *
 * NENHUM e um valor de verdade, e nao ausencia: a arma comum passa por exatamente
 * o mesmo caminho das outras, so que devolvendo o tipo de dano padrao da arma. Sem
 * isso cada classe precisaria de um "if elemento == null" antes de todo golpe.
 *
 * A LENDARIA nao tem tipo proprio de proposito. Um sexto tipo de dano que nenhum
 * bicho resiste seria, na pratica, dano puro com nome bonito; o que a torna
 * lendaria e bater mais forte no tipo que a arma ja tinha.
 */
public enum Elemento {

    //                sufixo      nome         tipo de dano       dano  cadencia  cor
    NENHUM("",          "",          null,              1.00, 1.00, new Color(0xB6CBCF)),
    FOGO("_fire",       "Fire ",     AttackTypes.Fire,  1.18, 1.15, new Color(0xDF864D)),
    GELO("_ice",        "Ice ",      AttackTypes.Ice,   1.10, 1.08, new Color(0x85C4D4)),
    AGUA("_water",      "Water ",    AttackTypes.Water, 0.92, 0.88, new Color(0x79A8D4)),
    TERRA("_earth",     "Earth ",    AttackTypes.Earth, 1.45, 1.40, new Color(0xB07E41)),
    AR("_air",          "Air ",      AttackTypes.Air,   0.78, 0.75, new Color(0xD7FFE4)),
    LENDARIA("_legend", "Legendary ", null,             1.50, 0.95, new Color(0xFFD850));

    private final String sufixo;
    private final String prefixoDoNome;
    private final AttackTypes ataque;
    private final double multiplicador;
    private final double cadencia;
    private final Color cor;

    Elemento(String sufixo, String prefixoDoNome, AttackTypes ataque,
             double multiplicador, double cadencia, Color cor) {
        this.sufixo = sufixo;
        this.prefixoDoNome = prefixoDoNome;
        this.ataque = ataque;
        this.multiplicador = multiplicador;
        this.cadencia = cadencia;
        this.cor = cor;
    }

    /**
     * Quanto TEMPO o golpe leva, em relacao a arma sem elemento.
     *
     * A ideia e a materia da arma: terra e pedra, entao pesa e desce devagar; ar
     * quase nao tem massa e vai num sopro; agua escorre, um pouco mais rapida que
     * o normal; fogo e gelo ficam perto do meio.
     *
     * O DANO ANDA JUNTO, E ISSO NAO E DETALHE. Mexer so na velocidade quebra o
     * jogo: se o ar bate mais rapido e nada cobra por isso, ar deixa de ser um
     * estilo e vira a escolha certa — e todo o resto vira enfeite. E a armadilha
     * que os foruns de design descrevem como "agilidade tambem aumenta dano, so
     * que disfarcado de dano por segundo".
     *
     * Entao os dois numeros se acompanham: quem bate rapido bate fraco, quem bate
     * devagar bate forte, e o dano por segundo fica quase igual entre os
     * elementais — dentro de cinco por cento. O que muda de verdade e o RISCO e a
     * SENSACAO: com terra, cada golpe e um compromisso longo em que voce fica
     * exposto e precisa acertar; com ar, voce cutuca de leve e sai. A lendaria e a
     * excecao proposital: ela e o premio raro, e premio raro pode ser melhor.
     */
    public double cadencia() {
        return this.cadencia;
    }

    /** O sufixo do arquivo: "sword" + "_fire" = sword_fire.png. */
    public String sprite(String base) {
        return base + sufixo;
    }

    public String nome(String base) {
        return prefixoDoNome + base;
    }

    /**
     * O tipo de dano. Cai no padrao da arma quando o elemento nao tem um proprio.
     *
     * @param padrao o que a arma usa sem elemento — corte na espada, perfuracao
     *               no machado e no arco, feitico na varinha.
     */
    public AttackTypes ataque(AttackTypes padrao) {
        return ataque == null ? padrao : ataque;
    }

    public double dano(double base) {
        return base * multiplicador;
    }

    /**
     * O arquivo do simbolo deste elemento, ou null para a arma sem elemento.
     *
     * O nome sai do proprio enum em minuscula, com acento tirado — assim
     * acrescentar um elemento novo nao pede uma tabela a mais para manter.
     */
    public String simbolo() {
        return switch (this) {
            case NENHUM -> null;
            case FOGO -> "fogo";
            case AGUA -> "agua";
            case GELO -> "gelo";
            case TERRA -> "terra";
            case AR -> "ar";
            // LENDARIA NAO TEM SIMBOLO DE ELEMENTO.
            //
            // Ela e uma RARIDADE que mora no enum de elemento por conveniencia de
            // sufixo de arquivo — mas nao e fogo, agua nem terra. Dando simbolo a
            // ela, a carta lendaria mostrava dois selos como se tivesse elemento, e
            // o canto direito passava a mentir: ali so deve haver desenho quando a
            // arma de fato tem um elemento.
            case LENDARIA -> null;
        };
    }

    /**
     * O elemento escrito em ingles, para a interface.
     *
     * O ENUM E EM PORTUGUES, A TELA E EM INGLES, e ate agora os dois se misturavam
     * na mesma linha: as armas montavam a ficha com {@code name().toLowerCase()} e o
     * jogador lia "lendaria damage" logo abaixo de "Legendary Wand". O nome do
     * item ja vinha traduzido porque usava {@link #nome}, mas a descricao nao — e
     * como as duas coisas aparecem juntas, a divergencia ficava lado a lado.
     *
     * Sai do mesmo lugar de onde o nome sai, e nao de uma tabela nova: assim as
     * duas nunca podem discordar.
     */
    public String rotulo() {
        return nome("").trim();
    }

    public Color cor() {
        return cor;
    }

    /**
     * O som deste elemento, ou nulo quando nao ha um.
     *
     * Os arquivos ja existiam e nao eram usados por ninguem. Amarra-los aqui, e nao
     * em cada arma, e o que faz uma corrida de agua SOAR como agua independentemente
     * do que o gato esteja empunhando — que e o mesmo raciocinio da cor e do tipo de
     * dano, que tambem moram neste enum.
     */
    public com.retronova.engine.sound.Sounds som() {
        return switch (this) {
            case NENHUM -> null;
            case FOGO -> com.retronova.engine.sound.Sounds.Fogo;
            case GELO -> com.retronova.engine.sound.Sounds.Gelo;
            case AGUA -> com.retronova.engine.sound.Sounds.Agua;
            case TERRA -> com.retronova.engine.sound.Sounds.Terra;
            case AR -> com.retronova.engine.sound.Sounds.Ar;
            case LENDARIA -> com.retronova.engine.sound.Sounds.Trovao;
        };
    }

    /**
     * O elemento DESTA CORRIDA.
     *
     * A VIRADA E ESTA: o elemento deixou de pertencer à arma e passou a pertencer à
     * partida. Antes, ter gelo significava ter achado uma espada de gelo, e a
     * decisão acontecia na loja, uma arma de cada vez. Agora é uma escolha só, no
     * começo, que vale para tudo o que o gato empunhar até o fim — que é como o
     * Hades faz com as bênçãos de um deus.
     *
     * SILENCIOSO FORA DA PARTIDA. A tela de seleção de personagem constrói gatos e
     * itens para mostrar, e ali não existe corrida nenhuma; {@code Game.getPlayer}
     * lança nesse caso. Devolver NENHUM em vez de propagar deixa a vitrine e os
     * menus funcionarem sem saber que este sistema existe.
     */
    public static Elemento daCorrida() {
        try {
            com.retronova.game.objects.entities.Player gato =
                    com.retronova.game.Game.getPlayer();
            return gato == null ? NENHUM : gato.elemento();
        } catch (RuntimeException foraDaPartida) {
            return NENHUM;
        }
    }
}
