package com.retronova.game.objects;

/**
 * A ESTRUTURA DE UM GOLPE, compartilhada por quem bate — arma ou bicho.
 *
 * Nasceu dentro de Item, para as armas do gato. Saiu de lá quando os inimigos
 * precisaram da mesma coisa, e a alternativa era escrever uma segunda máquina de
 * estados igual do outro lado. Duas seriam um problema de verdade e não de
 * arrumação: o que faz um combate ser legível é o jogador reconhecer a MESMA
 * gramática em tudo o que bate. Se o golpe do gato tem preparo, corte, extensão e
 * recuperação, e o do rato tem outra coisa qualquer, não há o que aprender — cada
 * inimigo vira um caso à parte.
 *
 * Também é o que dá ao inimigo a coisa que faltava: um instante entre "vai bater"
 * e "bateu". Ver Enemy.golpeCorpoACorpo.
 */
/**
 * Um golpe em quatro fases, contadas em ticks.
 *
 * É a estrutura que a referência de animação descreve, e o que separa um
 * golpe que "bate" de um que só troca de sprite:
 *
 *   PREPARO       a arma recua. Quanto mais longo, mais pesada ela parece —
 *                 e é a única coisa que avisa o jogador que o golpe vem.
 *   CORTE         o deslocamento rápido. É aqui que o dano sai.
 *   EXTENSÃO      a arma segue ALÉM do alvo. Sem isso o golpe parece
 *                 cortado no quadro do impacto, que é o defeito mais comum.
 *   RECUPERAÇÃO   volta à pose de porte.
 *
 * Os tempos vêm da mesma referência, convertidos de milissegundos para os
 * 60 ticks por segundo do jogo: leve fecha em 400 ms, lança em 550, e arma
 * pesada em 800 — com o preparo e a extensão levando quase tudo.
 */
public final class Investida {

    private final int preparo, corte, extensao, recuperacao;
    private int t = -1;

    private Investida(int preparo, int corte, int extensao, int recuperacao) {
        this.preparo = preparo;
        this.corte = corte;
        this.extensao = extensao;
        this.recuperacao = recuperacao;
    }

    /**
     * Rapida: 170 ms. Garras e laminas curtas.
     *
     * O preparo cabe em tres ticks e a recuperacao em dois — quase nada. E o
     * que separa uma arma de assassino de uma espada: a espada ganha peso
     * SEGURANDO os quadros, e estas ganham ameaca por nao segurar nenhum. A
     * proporcao entre as quatro fases e a mesma da leve, so comprimida.
     */
    /**
     * A mesma investida, esticada ou encurtada pelo elemento.
     *
     * Multiplica os quatro tempos de uma vez para que a PROPORCAO entre eles
     * — preparo, corte, extensao, recuperacao — nao mude: e essa proporcao que
     * da a leitura do golpe. Encurtar so o preparo deixaria o ataque rapido e
     * ilegivel; esticar so a recuperacao pareceria travamento, e nao peso.
     *
     * Minimo de um quadro em cada etapa: etapa de zero quadro nao existe na
     * tela, e o golpe passaria a pular pedaco.
     */
    public Investida vezes(double fator) {
        return new Investida(
                Math.max(1, (int) Math.round(preparo * fator)),
                Math.max(1, (int) Math.round(corte * fator)),
                Math.max(1, (int) Math.round(extensao * fator)),
                Math.max(1, (int) Math.round(recuperacao * fator)));
    }

    /** O ciclo inteiro do golpe, em quadros. */
    public int duracao() {
        return preparo + corte + extensao + recuperacao;
    }

    public static Investida rapida() {
        return new Investida(3, 2, 3, 2);
    }

    /** Leve: 400 ms. Espada, faca, foice. */
    public static Investida leve() {
        return new Investida(6, 3, 6, 3);
    }

    /** Média: 550 ms. Lança e haste. */
    public static Investida media() {
        return new Investida(12, 3, 9, 9);
    }

    /** Pesada: 800 ms, quase metade só de preparo e extensão. */
    public static Investida pesada() {
        return new Investida(15, 3, 18, 12);
    }

    private int total() {
        return preparo + corte + extensao + recuperacao;
    }

    public void comecar() {
        if (t < 0) {
            t = 0;
        }
    }

    public void tick() {
        if (t >= 0 && ++t >= total()) {
            t = -1;
        }
    }

    public boolean ativa() {
        return t >= 0;
    }

    /** O quadro exato do impacto: um só, para o dano não sair repetido. */
    public boolean acertaAgora() {
        return t == preparo + corte;
    }

    /**
     * Onde a arma está, de -1 (recuo máximo) a +1 (extensão máxima).
     *
     * O recuo é lento e a ida é rápida, que é o que dá o peso: a mesma
     * distância percorrida em três ticks depois de quinze de preparo.
     */
    public double avanco() {
        if (t < 0) {
            return 0;
        }
        if (t < preparo) {
            // Recua devagar, desacelerando no fim do preparo.
            double f = t / (double) preparo;
            return -Math.sin(f * Math.PI / 2);
        }
        if (t < preparo + corte) {
            double f = (t - preparo) / (double) corte;
            return -1 + 2 * f;
        }
        if (t < preparo + corte + extensao) {
            double f = (t - preparo - corte) / (double) extensao;
            return 1 - 0.15 * f;         // segue adiante, quase parado
        }
        double f = (t - preparo - corte - extensao) / (double) Math.max(1, recuperacao);
        return 0.85 * (1 - f);
    }
}
