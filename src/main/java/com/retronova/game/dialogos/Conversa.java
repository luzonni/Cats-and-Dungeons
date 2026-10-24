package com.retronova.game.dialogos;

import java.util.List;
import java.util.Map;

/**
 * Uma conversa: uma sequência de falas e as regras de quando ela pode tocar.
 *
 * É O MODELO DO HADES E DO LEFT 4 DEAD, em ponto pequeno. Cada conversa diz em
 * que situação serve (gatilho e condições) e quanto vale (prioridade); o jogo
 * junta as que servem agora e toca a mais importante, desempatando pela menos
 * vista. Escrever uma fala nova é acrescentar uma conversa com as condições
 * certas — nada no código precisa saber que ela existe.
 *
 * @param id          identificador único; é o que a memória grava
 * @param personagem  quem é o dono da conversa (o falante principal)
 * @param gatilho     o que a dispara: "interagir", "fim_de_corrida", "fala_solta"...
 * @param prioridade  maior toca primeiro; as genéricas ficam em 0
 * @param condicoes   nome da condição → valor esperado (ver Condicoes)
 * @param repeticao   se pode tocar de novo depois de vista
 * @param falas       as falas, em ordem
 * @param aoTerminar  eventos disparados quando a conversa acaba ou é pulada
 */
public record Conversa(String id, String personagem, String gatilho, int prioridade,
                       Map<String, Object> condicoes, Repeticao repeticao,
                       List<Fala> falas, List<String> aoTerminar) {

    /** Quantas vezes uma conversa pode tocar. */
    public enum Repeticao {
        /** Uma vez na vida do save: apresentação, reação a um marco. */
        UMA_VEZ,
        /** Quantas vezes for escolhida; o desempate evita repetir a mesma em seguida. */
        SEMPRE;

        public static Repeticao de(String texto) {
            return "once".equalsIgnoreCase(texto) || "uma_vez".equalsIgnoreCase(texto) ? UMA_VEZ : SEMPRE;
        }
    }

    public Conversa {
        condicoes = condicoes == null ? Map.of() : Map.copyOf(condicoes);
        falas = falas == null ? List.of() : List.copyOf(falas);
        aoTerminar = aoTerminar == null ? List.of() : List.copyOf(aoTerminar);
        repeticao = repeticao == null ? Repeticao.SEMPRE : repeticao;
    }
}
