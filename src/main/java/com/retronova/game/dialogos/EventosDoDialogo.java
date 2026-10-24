package com.retronova.game.dialogos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * O que uma conversa pode FAZER: abrir uma loja, tocar um som, mudar uma pose.
 *
 * Os arquivos citam o evento pelo nome; quem decide o que o nome faz é o registro
 * daqui. É o que deixa trocar o efeito sem tocar no sistema de diálogos nem nos
 * dados: "abrir_loja_do_vendedor" abre hoje o espelho do vendedor, preservando
 * o contrato dos dados pelo tratador em {@link #registrar}.
 *
 * Um evento pode ter argumento depois de ":" — "som:Coin" toca o som Coin.
 */
public final class EventosDoDialogo {

    private EventosDoDialogo() {
    }

    /** Quem disparou o evento: a conversa e a entidade que fala (pode ser nula). */
    public record Origem(Conversa conversa, Object falante, String argumento) {
    }

    private static final Map<String, Consumer<Origem>> TODOS = new LinkedHashMap<>();

    static {
        // A LOJA DO VENDEDOR. O falante é o próprio vendedor, e é ele quem sabe
        // qual loja abrir: o diálogo não conhece Store, Inter nem a transição.
        // Para a loja do espelho, registre aqui outro tratador com o mesmo nome.
        registrar("abrir_loja_do_vendedor", o -> {
            if (o.falante() instanceof Lojista lojista) {
                lojista.abrirLoja();
            }
        });
        // Um som de efeito, pelo nome em Sounds: "som:Coin".
        registrar("som", o -> {
            try {
                com.retronova.engine.sound.Sound.play(com.retronova.engine.sound.Sounds.valueOf(o.argumento()));
            } catch (IllegalArgumentException semEsse) {
                System.err.println("[dialogos] som desconhecido: " + o.argumento());
            }
        });
    }

    /** Quem tem uma loja para abrir no fim da conversa. */
    public interface Lojista {
        void abrirLoja();
    }

    /** Liga um nome de evento a um efeito. Registrar de novo o mesmo nome troca o efeito. */
    public static void registrar(String nome, Consumer<Origem> efeito) {
        TODOS.put(nome, efeito);
    }

    public static boolean conhece(String evento) {
        return TODOS.containsKey(nome(evento));
    }

    public static void disparar(String evento, Conversa conversa, Object falante) {
        Consumer<Origem> efeito = TODOS.get(nome(evento));
        if (efeito == null) {
            System.err.println("[dialogos] evento desconhecido: " + evento);
            return;
        }
        int corte = evento.indexOf(':');
        efeito.accept(new Origem(conversa, falante, corte < 0 ? "" : evento.substring(corte + 1)));
    }

    private static String nome(String evento) {
        int corte = evento.indexOf(':');
        return corte < 0 ? evento : evento.substring(0, corte);
    }
}
