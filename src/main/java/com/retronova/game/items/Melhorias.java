package com.retronova.game.items;

import com.retronova.engine.Engine;
import com.retronova.game.objects.entities.Modifiers;
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
     * Sorteia melhorias para o caça-níquel do baú (ver Roleta).
     *
     * DUAS ROLAGENS SEPARADAS, como no Hades: primeiro o grau, depois qual
     * melhoria. Elas são independentes de propósito — se o grau dependesse da
     * melhoria, existiriam melhorias "boas" e "ruins" por natureza, e o jogador
     * aprenderia a ignorar metade da lista. Assim qualquer uma pode sair lendária,
     * e o que o jogador avalia é sempre o par.
     *
     * AS MELHORIAS NÃO REPETEM DENTRO DA MESMA OFERTA, quando se pede mais de uma.
     *
     * @param promocao chance de cada carta subir um degrau, de Modifiers.Fortune
     */
    public List<Carta> oferecer(int quantas, double promocao) {
        List<Melhoria> possiveis = new ArrayList<>(List.of(Melhoria.values()));
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
        for (Map.Entry<Modifiers, Integer> e : pontos.entrySet()) {
            double soma = somasDePontos.getOrDefault(e.getKey(), 0d);
            saida.add(new int[]{BASE_PONTOS + e.getKey().ordinal(), e.getValue(),
                    (int) Math.round(soma * 1000)});
        }
        for (Map.Entry<ArmaExtra, Integer> e : armas.entrySet()) {
            saida.add(new int[]{BASE_ARMAS + e.getKey().ordinal(), e.getValue(), 0});
        }
        for (Map.Entry<Passiva, Integer> e : passivas.entrySet()) {
            saida.add(new int[]{BASE_PASSIVAS + e.getKey().ordinal(), e.getValue(), 0});
        }
        for (Evolucao e : evolucoes) {
            saida.add(new int[]{BASE_EVOLUCOES + e.ordinal(), 1, 0});
        }
        return saida.toArray(new int[0][]);
    }

    // ------------------------------------------------------ pontos de nivel

    /**
     * Pontos de atributo, ganhos ao subir de nivel. Sem desgaste: cada ponto vale
     * o mesmo, porque o custo ja cresce na curva de XP.
     *
     * Moram aqui para viajar no mesmo save das melhorias: gravados com o ordinal
     * do modificador somado a BASE_PONTOS, que nenhuma melhoria alcanca. Saves
     * antigos nao tem essas linhas e continuam abrindo.
     */
    private static final int BASE_PONTOS = 1000;
    private final Map<Modifiers, Integer> pontos = new EnumMap<>(Modifiers.class);
    private final Map<Modifiers, Double> somasDePontos = new EnumMap<>(Modifiers.class);

    public void aplicarPonto(Player gato, Modifiers atributo, double valor) {
        gato.addModifier(atributo, valor);
        pontos.merge(atributo, 1, Integer::sum);
        somasDePontos.merge(atributo, valor, Double::sum);
    }

    public int pontos(Modifiers atributo) {
        return pontos.getOrDefault(atributo, 0);
    }

    // ------------------------------------------------------- armas extras

    /** Armas extras e o nivel de cada uma. Gravadas com BASE_ARMAS + ordinal. */
    private static final int BASE_ARMAS = 2000;
    private final Map<ArmaExtra, Integer> armas = new EnumMap<>(ArmaExtra.class);

    public int nivel(ArmaExtra arma) {
        return armas.getOrDefault(arma, 0);
    }

    public void subirArma(ArmaExtra arma) {
        armas.merge(arma, 1, (a, b) -> Math.min(ArmaExtra.NIVEL_MAXIMO, a + b));
    }

    /** Armas que ainda podem sair na roleta: as que nao chegaram ao maximo. */
    public List<ArmaExtra> armasDisponiveis() {
        List<ArmaExtra> lista = new ArrayList<>();
        for (ArmaExtra a : ArmaExtra.values()) {
            if (nivel(a) < ArmaExtra.NIVEL_MAXIMO) {
                lista.add(a);
            }
        }
        return lista;
    }

    // ------------------------------------------------------------ passivas

    /** Passivas e o nivel de cada uma. Gravadas com BASE_PASSIVAS + ordinal. */
    private static final int BASE_PASSIVAS = 3000;
    private final Map<Passiva, Integer> passivas = new EnumMap<>(Passiva.class);

    public int nivel(Passiva p) {
        return passivas.getOrDefault(p, 0);
    }

    /**
     * Sobe a passiva. A ARMADURA e a unica que vira modificador direto (o
     * desconto de dano ja existe no Entity); as outras sao lidas por Bonus.
     */
    public void subirPassiva(Player gato, Passiva p) {
        if (nivel(p) >= p.nivelMaximo()) {
            return;
        }
        passivas.merge(p, 1, Integer::sum);
        if (p == Passiva.ARMADURA) {
            gato.addModifier(Modifiers.Resistance, p.valorPorNivel());
        }
    }

    public Map<Passiva, Integer> passivas() {
        return Collections.unmodifiableMap(passivas);
    }

    public Map<ArmaExtra, Integer> armas() {
        return Collections.unmodifiableMap(armas);
    }

    // ----------------------------------------------------------- evolucoes

    /** Armas que ja evoluiram. Gravadas com BASE_EVOLUCOES + ordinal. */
    private static final int BASE_EVOLUCOES = 4000;
    private final java.util.Set<Evolucao> evolucoes = java.util.EnumSet.noneOf(Evolucao.class);

    public boolean evoluiu(ArmaExtra arma) {
        Evolucao e = Evolucao.de(arma);
        return e != null && evolucoes.contains(e);
    }

    public void evoluir(Evolucao e) {
        evolucoes.add(e);
    }

    /** Uma evolucao pronta para sair do proximo bau: arma no maximo + passiva parceira. */
    public Evolucao evolucaoDisponivel() {
        for (Evolucao e : Evolucao.values()) {
            if (!evolucoes.contains(e) && nivel(e.arma()) >= ArmaExtra.NIVEL_MAXIMO
                    && nivel(e.passiva()) > 0) {
                return e;
            }
        }
        return null;
    }

    public java.util.Set<Evolucao> evolucoes() {
        return Collections.unmodifiableSet(evolucoes);
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
            if (ordinal >= BASE_EVOLUCOES) {
                Evolucao[] todasEvol = Evolucao.values();
                if (ordinal - BASE_EVOLUCOES < todasEvol.length) {
                    evolucoes.add(todasEvol[ordinal - BASE_EVOLUCOES]);
                }
                continue;
            }
            if (ordinal >= BASE_PASSIVAS) {
                Passiva[] todasPass = Passiva.values();
                int p = ordinal - BASE_PASSIVAS;
                if (p < todasPass.length) {
                    for (int i = 0; i < linha[1]; i++) {
                        subirPassiva(gato, todasPass[p]);
                    }
                }
                continue;
            }
            if (ordinal >= BASE_ARMAS) {
                ArmaExtra[] todasArmas = ArmaExtra.values();
                int a = ordinal - BASE_ARMAS;
                if (a < todasArmas.length && linha[1] > 0) {
                    armas.put(todasArmas[a], Math.min(ArmaExtra.NIVEL_MAXIMO, linha[1]));
                }
                continue;
            }
            if (ordinal >= BASE_PONTOS) {
                Modifiers[] mods = Modifiers.values();
                int m = ordinal - BASE_PONTOS;
                if (m < mods.length && linha[1] > 0) {
                    double soma = linha[2] / 1000d;
                    gato.addModifier(mods[m], soma);
                    pontos.put(mods[m], linha[1]);
                    somasDePontos.put(mods[m], soma);
                }
                continue;
            }
            if (ordinal < 0 || ordinal >= todas.length) {
                continue;
            }
            repor(gato, todas[ordinal], linha[1], linha[2] / 1000d);
        }
    }
}
