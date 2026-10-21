package com.retronova.game;

import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

/**
 * Progressao ENTRE corridas: ouro guardado e upgrades permanentes.
 *
 * Antes, morrer apagava tudo — nivel, armas, cartas — e a corrida seguinte
 * comecava identica. Isso funciona para uma partida, nao para a decima: falta o
 * "so mais uma" do genero, que vem de cada derrota deixar alguma coisa. Agora
 * cada corrida rende OURO (pelo tempo e pelos abates), e o ouro compra niveis de
 * upgrades que valem para todas as corridas seguintes. Pequenos de proposito:
 * o gato fica um pouco mais forte, e o jogador sente que aprendeu E cresceu.
 *
 * Gravado em saves/meta.json, separado dos espacos de corrida: apagar uma corrida
 * nao apaga o que foi conquistado.
 */
public final class Meta {

    private Meta() {
    }

    /** Os upgrades permanentes da loja. Novos sempre no FIM: o arquivo grava pelo nome. */
    public enum Permanente {
        VIGOR("Vitality", "+6 max health", new Color(0xE0475F)),
        FORCA("Might", "+1 damage", new Color(0xF5933D)),
        AGILIDADE("Swiftness", "+0.08 move speed", new Color(0x4FC3E8)),
        SORTE("Fortune", "+1 luck", new Color(0x6BD66B)),
        IMA("Magnet", "+10% pickup range", new Color(0x5FB0FF));

        public static final int NIVEL_MAXIMO = 5;

        private final String nome;
        private final String efeito;
        private final Color cor;

        Permanente(String nome, String efeito, Color cor) {
            this.nome = nome;
            this.efeito = efeito;
            this.cor = cor;
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
    }

    private static final Path ARQUIVO = Paths.get(com.retronova.engine.Foto.pastaDeSaves(), "meta.json");
    private static int ouro;
    private static final Map<Permanente, Integer> niveis = new EnumMap<>(Permanente.class);
    private static boolean carregado;

    private static void carregar() {
        if (carregado) {
            return;
        }
        carregado = true;
        try {
            if (!Files.exists(ARQUIVO)) {
                return;
            }
            JSONObject o = (JSONObject) new JSONParser().parse(Files.readString(ARQUIVO, StandardCharsets.UTF_8));
            Object g = o.get("ouro");
            ouro = g instanceof Number n ? n.intValue() : 0;
            for (Permanente p : Permanente.values()) {
                Object v = o.get(p.name());
                if (v instanceof Number n) {
                    niveis.put(p, Math.max(0, Math.min(Permanente.NIVEL_MAXIMO, n.intValue())));
                }
            }
        } catch (Exception arquivoRuim) {
            // Arquivo corrompido: comeca do zero em vez de nao abrir o jogo.
            System.err.println("meta.json ilegivel, comecando do zero: " + arquivoRuim.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void gravar() {
        try {
            JSONObject o = new JSONObject();
            o.put("ouro", (long) ouro);
            for (Permanente p : Permanente.values()) {
                o.put(p.name(), (long) nivel(p));
            }
            Files.createDirectories(ARQUIVO.getParent());
            Files.writeString(ARQUIVO, o.toJSONString(), StandardCharsets.UTF_8);
        } catch (Exception falha) {
            System.err.println("Nao foi possivel gravar meta.json: " + falha.getMessage());
        }
    }

    public static int ouro() {
        carregar();
        return ouro;
    }

    public static int nivel(Permanente p) {
        carregar();
        return niveis.getOrDefault(p, 0);
    }

    /** Preco do proximo nivel: 40, 80, 120... */
    public static int preco(Permanente p) {
        return 40 * (nivel(p) + 1);
    }

    public static boolean comprar(Permanente p) {
        carregar();
        if (nivel(p) >= Permanente.NIVEL_MAXIMO || ouro < preco(p)) {
            return false;
        }
        ouro -= preco(p);
        niveis.merge(p, 1, Integer::sum);
        gravar();
        return true;
    }

    /**
     * O bonus de vencer o chefe final: o mesmo que uns trinta minutos de corrida
     * rendem so pelo tempo (180 de ouro), um pouco acima. Vencer tem de pagar mais
     * que sobreviver ate o fim e morrer — senao o chefe nao e um objetivo.
     */
    public static final int BONUS_DE_VITORIA = 200;

    /** O que uma corrida rende: um pouco pelo tempo, um pouco pelos abates. */
    public static int ouroDaCorrida(int segundos, int abates) {
        return segundos / 6 + abates / 8;
    }

    public static void depositar(int quanto) {
        if (Boolean.getBoolean("cd.bench")) {
            return;     // modo de teste: nunca mexe na progressao de verdade do jogador
        }
        carregar();
        ouro += Math.max(0, quanto);
        gravar();
    }

    /** Aplica os upgrades permanentes num gato recem-criado. */
    public static void aplicar(Player gato) {
        carregar();
        int vigor = nivel(Permanente.VIGOR);
        if (vigor > 0) {
            gato.addModifier(Modifiers.Life, 6.0 * vigor);
            gato.setLife(gato.getLifeSize());
        }
        if (nivel(Permanente.FORCA) > 0) {
            gato.addModifier(Modifiers.Damage, nivel(Permanente.FORCA));
        }
        if (nivel(Permanente.AGILIDADE) > 0) {
            gato.addModifier(Modifiers.Speed, 0.08 * nivel(Permanente.AGILIDADE));
        }
        if (nivel(Permanente.SORTE) > 0) {
            gato.addModifier(Modifiers.Luck, nivel(Permanente.SORTE));
        }
    }
}
