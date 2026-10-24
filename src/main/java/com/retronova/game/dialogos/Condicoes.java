package com.retronova.game.dialogos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * As condições que uma conversa pode exigir, pelo nome usado nos dados.
 *
 * SÓ O QUE O JOGO JÁ SABE VERIFICAR. Cada entrada lê um fato que existe hoje:
 * a última corrida (gravada pela MemoriaDosDialogos no fim de cada uma), o gato
 * em jogo, as moedas, o ouro do Meta, as vitórias das Conquistas e as conversas
 * já vistas. Condição nova é uma linha aqui e um campo no ContextoDoDialogo; os
 * arquivos de diálogo passam a poder usá-la sem mais nada.
 *
 * CONDIÇÃO DESCONHECIDA REPROVA a conversa, e não a aprova. Um nome digitado
 * errado no JSON aprovado faria a fala tocar fora de contexto (o vendedor
 * comemorando uma vitória que não houve); reprovado, ela só deixa de tocar, e o
 * aviso no console diz por quê.
 */
public final class Condicoes {

    private Condicoes() {
    }

    /** Uma condição: o fato do contexto comparado com o valor escrito no arquivo. */
    @FunctionalInterface
    public interface Condicao {
        boolean vale(ContextoDoDialogo ctx, Conversa conversa, Object valor);
    }

    private static final Map<String, Condicao> TODAS = new LinkedHashMap<>();

    static {
        // Primeira vez que o gato fala com o dono da conversa.
        TODAS.put("primeiro_encontro", (c, conv, v) ->
                (c.memoria().conversasCom(conv.personagem()) == 0) == bool(v));
        // Resultado da corrida anterior: "nenhuma", "morte" ou "vitoria".
        TODAS.put("ultima_corrida", (c, conv, v) ->
                c.memoria().ultimoResultado().equalsIgnoreCase(String.valueOf(v)));
        // Duração da corrida anterior, em segundos. Sem corrida anterior, não vale.
        TODAS.put("ultima_corrida_ate_segundos", (c, conv, v) ->
                c.memoria().houveCorrida() && c.memoria().ultimaDuracao() <= num(v));
        TODAS.put("ultima_corrida_desde_segundos", (c, conv, v) ->
                c.memoria().houveCorrida() && c.memoria().ultimaDuracao() >= num(v));
        TODAS.put("ultima_corrida_nivel_minimo", (c, conv, v) ->
                c.memoria().houveCorrida() && c.memoria().ultimoNivel() >= num(v));
        // Gato em jogo, pelo nome.
        TODAS.put("gato", (c, conv, v) -> c.gato().equalsIgnoreCase(String.valueOf(v)));
        TODAS.put("moedas_minimo", (c, conv, v) -> c.moedas() >= num(v));
        TODAS.put("moedas_maximo", (c, conv, v) -> c.moedas() <= num(v));
        TODAS.put("ouro_minimo", (c, conv, v) -> c.ouro() >= num(v));
        TODAS.put("vitorias_minimo", (c, conv, v) -> c.vitorias() >= num(v));
        TODAS.put("corridas_minimo", (c, conv, v) -> c.memoria().corridas() >= num(v));
        TODAS.put("corridas_maximo", (c, conv, v) -> c.memoria().corridas() <= num(v));
        // Quantas vezes o gato já conversou com este personagem.
        TODAS.put("conversas_minimo", (c, conv, v) ->
                c.memoria().conversasCom(conv.personagem()) >= num(v));
        // Esta conversa foi vista no máximo N vezes.
        TODAS.put("vista_no_maximo", (c, conv, v) -> c.memoria().vezes(conv.id()) <= num(v));
    }

    /** Os nomes aceitos nos arquivos. Para a documentação e para o teste. */
    public static Set<String> nomes() {
        return TODAS.keySet();
    }

    /** A conversa passa em todas as condições dela? */
    public static boolean todas(Conversa conversa, ContextoDoDialogo ctx) {
        for (Map.Entry<String, Object> e : conversa.condicoes().entrySet()) {
            Condicao c = TODAS.get(e.getKey());
            if (c == null) {
                avisarUmaVez(conversa.id() + ": condicao desconhecida '" + e.getKey() + "'");
                return false;
            }
            if (!c.vale(ctx, conversa, e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static final java.util.Set<String> AVISADOS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void avisarUmaVez(String msg) {
        if (AVISADOS.add(msg)) {
            System.err.println("[dialogos] " + msg);
        }
    }

    private static boolean bool(Object v) {
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
    }

    private static double num(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
