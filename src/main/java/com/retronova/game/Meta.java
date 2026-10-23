package com.retronova.game;

import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.save.Progresso;

import java.awt.Color;

/**
 * Os upgrades permanentes antigos, agora gravados por gato no save permanente.
 *
 * ERA O SAVE DE PROGRESSÃO: ouro e cinco upgrades globais em saves/meta.json,
 * sem versão, sem backup e escrito por cima. A progressão passou para
 * save/Progresso (por gato, versionado, com escrita atômica, backup e
 * recuperação), e o meta.json antigo é migrado para lá na primeira leitura.
 * Esta classe ficou como a mesma porta de antes (a loja do fim de corrida e o
 * Bonus a usam) até o espelho do vendedor substituí-la.
 */
public final class Meta {

    private Meta() {
    }

    /** Os upgrades permanentes da loja. O id é o nome gravado no progresso (ver Progresso.migrarDoMeta). */
    public enum Permanente {
        VIGOR("Vitality", "+6 max health", new Color(0xE0475F), "vida"),
        FORCA("Might", "+1 damage", new Color(0xF5933D), "dano"),
        AGILIDADE("Swiftness", "+0.08 move speed", new Color(0x4FC3E8), "velocidade"),
        SORTE("Fortune", "+1 luck", new Color(0x6BD66B), "sorte"),
        IMA("Magnet", "+10% pickup range", new Color(0x5FB0FF), "coleta");

        public static final int NIVEL_MAXIMO = 5;

        private final String nome;
        private final String efeito;
        private final Color cor;
        private final String id;

        Permanente(String nome, String efeito, Color cor, String id) {
            this.nome = nome;
            this.efeito = efeito;
            this.cor = cor;
            this.id = id;
        }

        public String nome() {
            return nome;
        }

        public String efeito() {
            return efeito;
        }

        public Color cor() {
            return cor;
        }

        public String id() {
            return id;
        }
    }

    /** Moedas guardadas do gato. */
    public static int ouro(String gato) {
        return Progresso.moedas(gato);
    }

    public static int nivel(String gato, Permanente p) {
        return Progresso.nivel(gato, p.id());
    }

    /** Preco do proximo nivel: 40, 80, 120... */
    public static int preco(String gato, Permanente p) {
        return 40 * (nivel(gato, p) + 1);
    }

    public static boolean comprar(String gato, Permanente p) {
        return Progresso.comprar(gato, p.id(), preco(gato, p), Permanente.NIVEL_MAXIMO);
    }

    /** Aplica os upgrades permanentes num gato recem-criado. */
    public static void aplicar(Player gato) {
        String nome = gato.getName();
        int vigor = nivel(nome, Permanente.VIGOR);
        if (vigor > 0) {
            gato.addModifier(Modifiers.Life, 6.0 * vigor);
            gato.setLife(gato.getLifeSize());
        }
        if (nivel(nome, Permanente.FORCA) > 0) {
            gato.addModifier(Modifiers.Damage, nivel(nome, Permanente.FORCA));
        }
        if (nivel(nome, Permanente.AGILIDADE) > 0) {
            gato.addModifier(Modifiers.Speed, 0.08 * nivel(nome, Permanente.AGILIDADE));
        }
        if (nivel(nome, Permanente.SORTE) > 0) {
            gato.addModifier(Modifiers.Luck, nivel(nome, Permanente.SORTE));
        }
    }
}
