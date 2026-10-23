package com.retronova.game.dialogos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Qual conversa tocar agora. Pura: recebe tudo e não lê nada do jogo.
 *
 * A REGRA, NA ORDEM:
 *  1. só as do gatilho e do personagem pedidos, que passam nas condições;
 *  2. as de uma vez só ("once") saem depois de vistas;
 *  3. ganha a de maior prioridade — o Hades põe a reação ao que acabou de
 *     acontecer acima da conversa de rotina, e o Left 4 Dead escolhe a regra
 *     mais específica antes da genérica; aqui as duas ideias viram um número;
 *  4. no empate, a MENOS VISTA; depois, a vista há mais tempo; por fim, sorteio.
 *
 * O passo 4 é o que evita repetição sem lista de "já tocou": com três falas
 * genéricas no mesmo nível, cada uma toca uma vez antes de qualquer uma repetir,
 * e a que acabou de tocar nunca é a próxima enquanto houver outra.
 */
public final class EscolhaDeConversa {

    private EscolhaDeConversa() {
    }

    public static Conversa escolher(List<Conversa> todas, String personagem, String gatilho,
                                    ContextoDoDialogo ctx, Random sorteio) {
        List<Conversa> servem = new ArrayList<>();
        for (Conversa c : todas) {
            if (!c.personagem().equals(personagem) || !c.gatilho().equals(gatilho)) {
                continue;
            }
            if (c.repeticao() == Conversa.Repeticao.UMA_VEZ && ctx.memoria().vezes(c.id()) > 0) {
                continue;
            }
            if (!Condicoes.todas(c, ctx)) {
                continue;
            }
            servem.add(c);
        }
        if (servem.isEmpty()) {
            return null;
        }
        Comparator<Conversa> ordem = Comparator
                .comparingInt((Conversa c) -> -c.prioridade())
                .thenComparingInt(c -> ctx.memoria().vezes(c.id()))
                .thenComparingLong(c -> ctx.memoria().vistaEm(c.id()));
        servem.sort(ordem);
        Conversa melhor = servem.get(0);
        List<Conversa> empatadas = new ArrayList<>();
        for (Conversa c : servem) {
            if (ordem.compare(c, melhor) == 0) {
                empatadas.add(c);
            }
        }
        return empatadas.get(sorteio.nextInt(empatadas.size()));
    }
}
