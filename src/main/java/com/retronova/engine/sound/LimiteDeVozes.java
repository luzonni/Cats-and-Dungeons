package com.retronova.engine.sound;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Quantas vozes de um mesmo efeito podem tocar juntas.
 *
 * O mixer do TinySound percorre, PARA CADA AMOSTRA DE ÁUDIO (44 mil por
 * segundo), a lista inteira de sons tocando. Numa horda grande cada acerto,
 * morte, caveira e moeda abria uma voz nova, sem teto, e a thread do mixer
 * chegou a gastar mais CPU que o jogo inteiro — medido no Flight Recorder,
 * concentrado no laço sobre os efeitos. Numa máquina mais fraca isso disputa
 * núcleo com o desenho e vira queda de FPS.
 *
 * Duas regras, por efeito:
 *  - o mesmo som não começa de novo antes de {@link #INTERVALO_MINIMO} ms: dois
 *    iguais no mesmo instante não soam como dois, só mais alto;
 *  - no máximo {@link #VOZES_POR_SOM} vozes do mesmo som ao mesmo tempo. A
 *    quinta espada no mesmo segundo não acrescenta nada que se ouça.
 *
 * Puro, sem áudio: recebe o relógio e a duração, para dar para testar.
 */
final class LimiteDeVozes {

    static final long INTERVALO_MINIMO = 40;
    static final int VOZES_POR_SOM = 4;

    /** Quando cada voz em curso termina, por efeito, da mais antiga à mais nova. */
    private final Map<Sounds, Deque<Long>> terminos = new EnumMap<>(Sounds.class);
    private final Map<Sounds, Long> ultimoInicio = new EnumMap<>(Sounds.class);

    /**
     * Pode tocar agora? Se sim, a voz já fica registrada.
     *
     * @param agora   relógio em milissegundos
     * @param duracao duração do efeito em milissegundos
     */
    synchronized boolean tocar(Sounds som, long agora, long duracao) {
        Long ultimo = ultimoInicio.get(som);
        if (ultimo != null && agora - ultimo < INTERVALO_MINIMO) {
            return false;
        }
        Deque<Long> fila = terminos.computeIfAbsent(som, s -> new ArrayDeque<>());
        while (!fila.isEmpty() && fila.peekFirst() <= agora) {
            fila.pollFirst();
        }
        if (fila.size() >= VOZES_POR_SOM) {
            return false;
        }
        fila.addLast(agora + Math.max(1, duracao));
        ultimoInicio.put(som, agora);
        return true;
    }
}
