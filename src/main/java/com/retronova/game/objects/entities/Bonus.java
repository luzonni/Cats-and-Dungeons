package com.retronova.game.objects.entities;

import com.retronova.game.Meta;
import com.retronova.game.items.Passiva;

/**
 * Os multiplicadores que as passivas, o gato escolhido e a progressao entre
 * corridas somam juntos. Um lugar so para perguntar "quanto de area este gato
 * tem?" — as armas nao precisam saber de onde cada pedaco veio.
 *
 * OS TRES GATOS DEIXAM DE SER SO NUMEROS. Antes Muffin, Finn e Azrael mudavam
 * apenas vida, dano e velocidade; agora cada um tem uma habilidade propria que
 * puxa a build para um lado:
 *   Finn   (Ranger)   — +10% de chance de critico: o arqueiro que acerta o ponto fraco.
 *   Muffin (Vanguard) — 10% menos dano recebido: o que aguenta o cerco.
 *   Azrael (Sorcerer) — +20% de area: o que limpa a horda de longe.
 */
public final class Bonus {

    private Bonus() {
    }

    private static boolean e(Player gato, String nome) {
        return gato != null && nome.equals(gato.getName());
    }

    private static int nivel(Player gato, Passiva p) {
        return gato == null ? 0 : gato.getMelhorias().nivel(p);
    }

    /** Multiplicador de raio de todas as armas de area. */
    public static double area(Player gato) {
        return 1 + nivel(gato, Passiva.AREA) * Passiva.AREA.valorPorNivel()
                + (e(gato, "Azrael") ? 0.20 : 0);
    }

    /** Multiplicador do intervalo entre disparos das armas extras (menor e mais rapido). */
    public static double pressa(Player gato) {
        return Math.max(0.4, 1 - nivel(gato, Passiva.PRESSA) * Passiva.PRESSA.valorPorNivel());
    }

    /** Chance de critico, de 0 a 1. A sorte tambem empurra um pouco. */
    public static double critico(Player gato) {
        if (gato == null) {
            return 0;
        }
        return nivel(gato, Passiva.CRITICO) * Passiva.CRITICO.valorPorNivel()
                + (e(gato, "Finn") ? 0.10 : 0) + gato.chanceDeSorte() * 0.05;
    }

    /** Vida recuperada por segundo. */
    public static double regeneracao(Player gato) {
        return nivel(gato, Passiva.REGENERACAO) * Passiva.REGENERACAO.valorPorNivel();
    }

    /** Multiplicador do XP recebido. */
    public static double crescimento(Player gato) {
        return 1 + nivel(gato, Passiva.CRESCIMENTO) * Passiva.CRESCIMENTO.valorPorNivel();
    }

    /** Multiplicador do alcance do ima de XP. */
    public static double ima(Player gato) {
        return 1 + nivel(gato, Passiva.IMA) * Passiva.IMA.valorPorNivel()
                + Meta.nivel(Meta.Permanente.IMA) * 0.10;
    }

    /** A habilidade do gato, em uma linha, para a selecao e a tela final. */
    public static String habilidade(String nomeDoGato) {
        return switch (nomeDoGato) {
            case "Finn" -> "+10% critical chance";
            case "Muffin" -> "-10% damage taken";
            case "Azrael" -> "+20% weapon area";
            default -> "";
        };
    }

    /**
     * Aplica no inicio da corrida o que vale desde o primeiro segundo: a
     * habilidade do Muffin (que e um modificador) e os upgrades permanentes.
     */
    public static void aplicarNoInicio(Player gato) {
        if (e(gato, "Muffin")) {
            gato.addModifier(Modifiers.Resistance, 0.10);
        }
        Meta.aplicar(gato);
    }

    private static double sobra;

    /** Regeneracao, chamada uma vez por tick pelo Game. */
    public static void tick(Player gato) {
        double porTick = regeneracao(gato) / 60.0;
        if (porTick <= 0 || gato.getLife() >= gato.getLifeSize()) {
            return;
        }
        sobra += porTick;
        if (sobra >= 0.1) {
            gato.setLife(Math.min(gato.getLifeSize(), gato.getLife() + sobra));
            sobra = 0;
        }
    }
}
