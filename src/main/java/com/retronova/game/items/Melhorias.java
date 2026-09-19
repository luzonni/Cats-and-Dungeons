package com.retronova.game.items;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * O que o gato já escolheu nesta corrida, e o que pode ser oferecido a seguir.
 *
 * MORA NUM LUGAR SÓ porque três coisas precisam da mesma contagem: aplicar a
 * melhoria com o desgaste certo, escrever na carta quanto ela vai render de
 * verdade, e gravar a corrida para poder retomá-la. Espalhado, bastaria um
 * desses três esquecer de contar para o jogador retomar um save e receber de
 * novo o valor cheio de uma melhoria que ele já tinha oito vezes.
 */
public class Melhorias {

    private final Map<Melhoria, Integer> tomadas = new EnumMap<>(Melhoria.class);

    /**
     * Quanto cada melhoria somou ao todo, de verdade.
     *
     * GUARDADO, E NAO RECALCULADO. Refazer a conta a partir da contagem exigiria
     * saber o GRAU de cada copia — a mesma melhoria tomada tres vezes pode ter
     * vindo comum, epica e comum, e as tres valem numeros diferentes. Guardar a
     * soma tambem desamarra os saves da curva de desgaste: ajustar balanceamento
     * deixa de mudar retroativamente o atributo de toda corrida ja gravada.
     */
    private final Map<Melhoria, Double> somas = new EnumMap<>(Melhoria.class);

    /** Quantas cópias desta melhoria o gato já tem. */
    public int quantas(Melhoria melhoria) {
        return tomadas.getOrDefault(melhoria, 0);
    }

    public Map<Melhoria, Integer> todas() {
        return Collections.unmodifiableMap(tomadas);
    }

    /**
     * Entrega a melhoria ao gato, já descontado o desgaste desta cópia.
     *
     * O DESGASTE É CALCULADO ANTES DE CONTAR, e a ordem importa: a primeira cópia
     * tem de valer inteira. Contando primeiro, ela já entraria como segunda e o
     * jogador nunca receberia o valor que a carta prometeu.
     */
    public void aplicar(Player gato, Melhoria melhoria, Raridade raridade) {
        double valor = melhoria.valor(raridade) * Melhoria.desgaste(quantas(melhoria));
        gato.addModifier(melhoria.atributo(), valor);
        tomadas.merge(melhoria, 1, Integer::sum);
        somas.merge(melhoria, valor, Double::sum);
    }

    /** Reaplica uma corrida gravada, sem recalcular nada. Ver {@link #comoNumeros}. */
    public void repor(Player gato, Melhoria melhoria, int quantidade, double somaGravada) {
        if (quantidade <= 0) {
            return;
        }
        gato.addModifier(melhoria.atributo(), somaGravada);
        tomadas.put(melhoria, quantidade);
        somas.put(melhoria, somaGravada);
    }

    /**
     * Uma oferta: a melhoria e o grau em que ela vem.
     *
     * @param jaTinha quantas cópias havia ANTES desta, para a carta poder dizer o
     *                valor real em vez do valor de tabela
     */
    public record Carta(Melhoria melhoria, Raridade raridade, int jaTinha) {

        public String efeito() {
            return melhoria.efeito(raridade, jaTinha);
        }

        /** A linha de classificação: o grau e o assunto. */
        public String classificacao() {
            return raridade.rotulo() + " - " + melhoria.assunto();
        }
    }

    /**
     * Sorteia a oferta de fim de turno.
     *
     * DUAS ROLAGENS SEPARADAS, como no Hades: primeiro o grau, depois qual
     * melhoria. Elas são independentes de propósito — se o grau dependesse da
     * melhoria, existiriam melhorias "boas" e "ruins" por natureza, e o jogador
     * aprenderia a ignorar metade da lista. Assim qualquer uma pode sair lendária,
     * e o que o jogador avalia é sempre o par.
     *
     * AS MELHORIAS NÃO REPETEM DENTRO DA MESMA OFERTA. Duas cartas da mesma coisa
     * desperdiçariam uma das três opções — e três viram uma escolha só.
     */
    /**
     * A partir de que sala o Presagio para de ser oferecido.
     *
     * Ele melhora as cartas FUTURAS, entao quanto mais tarde vem, menos vale — e na
     * ultima sala nao vale nada. Uma opcao objetivamente morta e o pior que um
     * sistema de cartas pode ter: quem conhece o jogo simplesmente nunca a escolhe,
     * e quem nao conhece e punido por isso. Some das ultimas salas em vez de virar
     * uma pegadinha para novato.
     *
     * A corrida tem vinte ondas; quinze deixa um quarto do caminho sem ele.
     */
    private static final int ULTIMA_SALA_COM_PRESAGIO = 15;

    /**
     * @param nivel      em que sala a oferta esta sendo feita
     * @param promocao   chance de cada carta subir um degrau, de Modifiers.Fortune
     */
    public List<Carta> oferecer(int quantas, int nivel, double promocao) {
        List<Melhoria> possiveis = new ArrayList<>(List.of(Melhoria.values()));
        if (nivel >= ULTIMA_SALA_COM_PRESAGIO) {
            possiveis.remove(Melhoria.PRESAGIO);
        }
        Collections.shuffle(possiveis, Engine.RAND);
        List<Carta> oferta = new ArrayList<>();
        for (int i = 0; i < quantas && i < possiveis.size(); i++) {
            Melhoria m = possiveis.get(i);
            oferta.add(new Carta(m, Raridade.sortear(promocao), quantas(m)));
        }
        return oferta;
    }

    // ------------------------------------------------------------ gravação

    /**
     * A corrida gravada, em pares {ordinal da melhoria, quantidade, soma aplicada}.
     *
     * A SOMA VAI JUNTO, e nao so a quantidade — ver o campo {@code somas}.
     *
     * A soma é multiplicada por mil para caber em inteiro sem perder a casa
     * decimal que a resistência usa.
     */
    public int[][] comoNumeros() {
        List<int[]> saida = new ArrayList<>();
        for (Map.Entry<Melhoria, Integer> e : tomadas.entrySet()) {
            double soma = somas.getOrDefault(e.getKey(), 0d);
            saida.add(new int[]{e.getKey().ordinal(), e.getValue(),
                    (int) Math.round(soma * 1000)});
        }
        return saida.toArray(new int[0][]);
    }

    /**
     * Devolve o que estava gravado.
     *
     * Um ordinal fora da faixa é ignorado em silêncio: ele só aparece se alguém
     * tiver removido uma melhoria do enum entre a gravação e agora, e nesse caso a
     * corrida continuar sem ela é melhor do que não abrir.
     */
    public void deNumeros(Player gato, int[][] gravado) {
        if (gravado == null) {
            return;
        }
        Melhoria[] todas = Melhoria.values();
        for (int[] linha : gravado) {
            if (linha == null || linha.length < 3) {
                continue;
            }
            int ordinal = linha[0];
            if (ordinal < 0 || ordinal >= todas.length) {
                continue;
            }
            repor(gato, todas[ordinal], linha[1], linha[2] / 1000d);
        }
    }
}
