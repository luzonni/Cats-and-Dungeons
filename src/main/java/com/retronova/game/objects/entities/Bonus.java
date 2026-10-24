package com.retronova.game.objects.entities;

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

    /** A chance de critico que vem do gato (a habilidade do Finn). A tela de Status a mostra como base. */
    public static double criticoInato(Player gato) {
        return e(gato, "Finn") ? 0.10 : 0;
    }

    /** A reducao de dano que vem do gato (a habilidade do Muffin). A tela de Status a mostra como base. */
    public static double resistenciaInata(Player gato) {
        return e(gato, "Muffin") ? 0.10 : 0;
    }

    /** Chance de critico, de 0 a 1. A sorte tambem empurra um pouco. */
    public static double critico(Player gato) {
        if (gato == null) {
            return 0;
        }
        return nivel(gato, Passiva.CRITICO) * Passiva.CRITICO.valorPorNivel()
                + criticoInato(gato) + gato.chanceDeSorte() * 0.05
                + com.retronova.game.espelho.Espelho.valor(gato, com.retronova.game.espelho.Espelho.Efeito.CRITICO);
    }

    /** Quantas flechas o arco solta por disparo: 1 mais o nivel do Voleio (max. 4). */
    public static int flechas(Player gato) {
        return 1 + nivel(gato, Passiva.VOLEIO);
    }

    /**
     * Fracao do dano que CADA flecha leva quando sao n de uma vez.
     *
     * O TIRO INTEIRO RENDE RAIZ DE N, e nao n. Com n flechas de dano cheio, o
     * voleio dobrava, triplicava e quadruplicava o dano do Finn — nenhuma outra
     * carta chega perto disso, e ela passaria a ser a unica escolha. Dividindo
     * por raiz de n, 2 flechas valem 1,41x, 3 valem 1,73x e 4 valem 2x: cada
     * nivel ainda e um ganho claro, so que cada vez menor. E o leque ainda espalha
     * as flechas, entao contra um alvo so, de longe, nem todas acertam.
     */
    public static double danoPorFlecha(int flechas) {
        return 1 / Math.sqrt(Math.max(1, flechas));
    }

    // ------------------------------------------------------ Retaliacao (Muffin)

    /**
     * Os numeros da onda do Muffin, por nivel (1 a 3).
     *
     * A ONDA E MAIS EMPURRAO QUE DANO. O que mata o Muffin e o cerco, nao a falta
     * de dano — a espada dele ja resolve um bicho por vez. Entao o que cresce mais
     * por nivel e o RAIO e a recarga; o dano sobe menos, e sempre puxa do dano do
     * gato, para a onda continuar valendo no minuto vinte.
     *
     * A RECARGA E O FREIO. Sem ela, com a invulnerabilidade de 0,4 s depois de
     * apanhar, um cerco de dez bichos daria duas ondas e meia por segundo — o
     * Muffin ficaria imune a cerco. Com 3 s / 2,5 s / 2 s ele continua precisando
     * se mexer.
     */
    public static double raioDaRetaliacao(int nivel) {
        return switch (Math.max(1, Math.min(3, nivel))) {
            case 1 -> 1.6;
            case 2 -> 2.0;
            default -> 2.4;
        };
    }

    /** Dano da onda: um fixo mais o dano do gato, multiplicado pelo nivel. */
    public static double danoDaRetaliacao(int nivel, double danoDoGato) {
        double mult = switch (Math.max(1, Math.min(3, nivel))) {
            case 1 -> 1.0;
            case 2 -> 1.5;
            default -> 2.0;
        };
        return (12 + Math.max(0, danoDoGato)) * mult;
    }

    /** Ticks entre duas ondas. */
    public static int recargaDaRetaliacao(int nivel) {
        return switch (Math.max(1, Math.min(3, nivel))) {
            case 1 -> 180;
            case 2 -> 150;
            default -> 120;
        };
    }

    /** Forca do empurrao, na mesma unidade dos outros knockbacks. */
    public static double empurraoDaRetaliacao(int nivel) {
        return 5 + Math.max(1, Math.min(3, nivel));
    }

    // ------------------------------------------------- Explosao arcana (Azrael)

    /**
     * Fracao do golpe que a explosao causa EM VOLTA do alvo, por nivel.
     *
     * Abaixo de um de proposito: o alvo direto ja levou o golpe inteiro, e a
     * explosao so existe para o resto do grupo. Contra um bicho sozinho ela nao
     * soma nada — o Azrael nao vira o melhor matador de chefe por causa dela.
     */
    public static double fracaoDaExplosao(int nivel) {
        return switch (Math.max(1, Math.min(3, nivel))) {
            case 1 -> 0.35;
            case 2 -> 0.50;
            default -> 0.65;
        };
    }

    /** Raio da explosao, em tiles, ANTES do multiplicador de area do gato. */
    public static double raioDaExplosao(int nivel) {
        return switch (Math.max(1, Math.min(3, nivel))) {
            case 1 -> 1.0;
            case 2 -> 1.3;
            default -> 1.6;
        };
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
                + com.retronova.game.espelho.Espelho.valor(gato, com.retronova.game.espelho.Espelho.Efeito.COLETA);
    }

    /** A habilidade do gato, em uma linha, para a selecao e a tela final. */
    public static String habilidade(String nomeDoGato) {
        return switch (nomeDoGato) {
            case "Finn" -> "+10% critical chance, Volley skill";
            case "Muffin" -> "-10% damage taken, Retaliate skill";
            case "Azrael" -> "+20% weapon area, Arcane Burst skill";
            default -> "";
        };
    }

    /**
     * Aplica no inicio da corrida o que vale desde o primeiro segundo: a
     * habilidade do Muffin (que e um modificador) e os upgrades permanentes.
     */
    public static void aplicarNoInicio(Player gato) {
        if (e(gato, "Muffin")) {
            gato.addModifier(Modifiers.Resistance, resistenciaInata(gato));
        }
        com.retronova.game.espelho.Espelho.aplicar(gato);
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
