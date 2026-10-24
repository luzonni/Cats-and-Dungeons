package com.retronova.game.espelho;

import com.retronova.engine.Engine;
import com.retronova.game.map.arena.SubirDeNivel;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.save.Progresso;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * O ESPELHO DO VENDEDOR: melhorias permanentes compradas com moedas, por gato.
 *
 * É o Espelho da Noite do Hades no vendedor da antecâmara: entre uma corrida e
 * outra, o gato troca as moedas que trouxe por níveis de melhorias que valem em
 * todas as corridas seguintes. A tabela (nomes, níveis, custos, valores) está
 * toda em progressao/espelho.json; aqui só mora o que cada EFEITO faz.
 *
 * AS TRÊS CAMADAS DE UM ATRIBUTO: a base do gato (players/*.json) não muda; o
 * espelho soma por cima no início de cada corrida ({@link #aplicar}); as cartas
 * da corrida somam por cima dos dois. A tela de Status mostra as três
 * ({@link #decompor}).
 *
 * COMPRAR NA ANTECÂMARA VALE NA HORA. O gato da antecâmara já é o da corrida
 * que vai começar, então a compra soma a diferença nele ({@link #aplicarDiferenca})
 * em vez de esperar a próxima corrida.
 */
public final class Espelho {

    private Espelho() {
    }

    /** O que cada melhoria faz no jogo. Novo efeito: um nome aqui e um caso em {@link #aplicarDiferenca}. */
    public enum Efeito {
        /** Vida máxima (Modifiers.Life). Começa a corrida cheia. */
        VIDA(Modifiers.Life),
        DANO(Modifiers.Damage),
        ALCANCE(Modifiers.Range),
        /** Fração do dano recebido cortada (Modifiers.Resistance, teto de 80% no Player). */
        RESISTENCIA(Modifiers.Resistance),
        VELOCIDADE(Modifiers.Speed),
        SORTE(Modifiers.Luck),
        /** Multiplicador do ímã de XP, lido por Bonus.ima. */
        COLETA(null),
        /** Chance de crítico, lida por Bonus.critico. */
        CRITICO(null),
        /** Rerolls a mais por corrida na escolha de cartas (SubirDeNivel). */
        REROLLS(null),
        /** Cartas a mais na mão da escolha de cartas (SubirDeNivel). */
        CARTAS(null),
        /** Multiplicador das moedas da corrida (Corrida.ganancia). */
        MOEDAS(null);

        /** O modificador que ele soma no gato; null nos que são lidos na hora. */
        final Modifiers modificador;

        Efeito(Modifiers modificador) {
            this.modificador = modificador;
        }

        static Efeito de(String nome) {
            return valueOf(nome.toUpperCase(java.util.Locale.ROOT));
        }
    }

    // ---------------------------------------------------------------- tabela

    private static List<MelhoriaDoEspelho> tabela;

    /** A tabela do arquivo, lida uma vez. */
    public static synchronized List<MelhoriaDoEspelho> tabela() {
        if (tabela == null) {
            try (InputStream in = Espelho.class.getResourceAsStream(Engine.resPath + "progressao/espelho.json")) {
                tabela = deJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception falha) {
                throw new IllegalStateException("Tabela do espelho invalida", falha);
            }
        }
        return tabela;
    }

    /** Lê a lista "melhorias" do arquivo. Puro, para os testes. */
    static List<MelhoriaDoEspelho> deJson(String texto) throws Exception {
        JSONObject raiz = (JSONObject) new JSONParser().parse(texto);
        List<MelhoriaDoEspelho> saida = new ArrayList<>();
        for (Object o : (JSONArray) raiz.get("melhorias")) {
            JSONObject m = (JSONObject) o;
            List<Integer> custos = new ArrayList<>();
            for (Object c : (JSONArray) m.get("custos")) {
                custos.add(((Number) c).intValue());
            }
            saida.add(new MelhoriaDoEspelho(
                    (String) m.get("id"), (String) m.get("nome"), (String) m.get("descricao"),
                    (String) m.get("icone"), Efeito.de((String) m.get("efeito")),
                    num(m.get("porNivel"), 0), num(m.get("exibicao"), 1), (int) num(m.get("casas"), 0),
                    (String) m.get("texto"), custos));
        }
        if (saida.stream().map(MelhoriaDoEspelho::id).distinct().count() != saida.size()) {
            throw new IllegalArgumentException("IDs duplicados no espelho");
        }
        return List.copyOf(saida);
    }

    private static double num(Object v, double padrao) {
        return v instanceof Number n ? n.doubleValue() : padrao;
    }

    public static MelhoriaDoEspelho porId(String id) {
        for (MelhoriaDoEspelho m : tabela()) {
            if (m.id().equals(id)) {
                return m;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- leitura

    public static int nivel(String gato, MelhoriaDoEspelho m) {
        return Math.min(Progresso.nivel(gato, m.id()), m.nivelMaximo());
    }

    /** O total que o espelho soma ao gato num efeito, em unidades do jogo. */
    public static double valor(String gato, Efeito efeito) {
        double soma = 0;
        for (MelhoriaDoEspelho m : tabela()) {
            if (m.efeito() == efeito) {
                soma += m.valor(nivel(gato, m));
            }
        }
        return soma;
    }

    public static double valor(Player gato, Efeito efeito) {
        return gato == null ? 0 : valor(gato.getName(), efeito);
    }

    /** Cartas a mais na escolha de cartas. */
    public static int cartasExtras(Player gato) {
        return (int) Math.round(valor(gato, Efeito.CARTAS));
    }

    /** O multiplicador de moedas do gato: 1 sem a melhoria. */
    public static double ganancia(String gato) {
        return 1 + valor(gato, Efeito.MOEDAS);
    }

    /** O que o gato já gastou no espelho: o reembolso de redistribuir. */
    public static int gasto(String gato) {
        int soma = 0;
        for (MelhoriaDoEspelho m : tabela()) {
            soma += m.gasto(nivel(gato, m));
        }
        return soma;
    }

    // ---------------------------------------------------------------- ações

    /** Aplica o espelho num gato que começa (ou retoma) uma corrida. */
    public static void aplicar(Player gato) {
        SubirDeNivel.ajustarTrocasExtras((int) Math.round(valor(gato, Efeito.REROLLS)));
        for (MelhoriaDoEspelho m : tabela()) {
            int n = nivel(gato.getName(), m);
            if (n > 0) {
                aplicarDiferenca(gato, m, n);
            }
        }
    }

    /** Resultado de uma tentativa de compra, para a tela dar o retorno certo. */
    public enum Compra {
        FEITA, SEM_MOEDAS, NO_MAXIMO, ERRO_AO_SALVAR
    }

    /** Compra o próximo nível para o gato e já soma no gato vivo. */
    public static Compra comprar(Player gato, MelhoriaDoEspelho m) {
        String nome = gato.getName();
        int n = nivel(nome, m);
        if (n >= m.nivelMaximo()) {
            return Compra.NO_MAXIMO;
        }
        try {
            if (!Progresso.comprar(nome, m.id(), m.custo(n), m.nivelMaximo())) {
                return Compra.SEM_MOEDAS;
            }
        } catch (IllegalStateException falha) {
            System.err.println("[espelho] compra preservada sem alteracoes: " + falha);
            return Compra.ERRO_AO_SALVAR;
        }
        aplicarDiferenca(gato, m, 1);
        return Compra.FEITA;
    }

    /**
     * Devolve todas as moedas gastas pelo gato e zera as melhorias dele, tirando
     * também do gato vivo o que o espelho tinha somado. Grátis, como o "Refund"
     * do Vampire Survivors: o Hades cobra uma chave, mas aqui não há uma segunda
     * moeda, e cobrar moedas por redistribuir só puniria quem experimenta.
     */
    public static int redistribuir(Player gato) {
        String nome = gato.getName();
        int reembolso = gasto(nome);
        List<Integer> niveis = tabela().stream().map(m -> nivel(nome, m)).toList();
        Progresso.redistribuir(nome, reembolso);
        int i = 0;
        for (MelhoriaDoEspelho m : tabela()) {
            int n = niveis.get(i++);
            if (n > 0) {
                aplicarDiferenca(gato, m, -n);
            }
        }
        SubirDeNivel.ajustarTrocasExtras(0);   // lido do progresso, que agora está zerado
        return reembolso;
    }

    /** Soma (ou tira, com delta negativo) níveis de uma melhoria num gato vivo. */
    static void aplicarDiferenca(Player gato, MelhoriaDoEspelho m, int delta) {
        double v = m.porNivel() * delta;
        if (m.efeito().modificador != null) {
            gato.addModifier(m.efeito().modificador, v);
            if (m.efeito() == Efeito.VIDA) {
                // A vida nova vem cheia; a tirada não pode deixar a atual acima do máximo.
                gato.setLife(Math.max(1, Math.min(gato.getLifeSize(), gato.getLife() + v)));
            }
        } else if (m.efeito() == Efeito.REROLLS) {
            SubirDeNivel.ajustarTrocasExtras((int) Math.round(valor(gato, Efeito.REROLLS)));
        }
        // COLETA, CRITICO, CARTAS e MOEDAS são lidos na hora de usar: nada a somar.
    }

    // ------------------------------------------------------- tela de Status

    /** Um atributo em três camadas: a base do gato, o que o espelho soma e o que a corrida somou. */
    public record Camadas(double base, double espelho, double corrida) {
        public double total() {
            return base + espelho + corrida;
        }
    }

    /**
     * Separa um atributo nas três camadas.
     *
     * A base vem do arquivo do gato; o espelho, da tabela; a corrida é o resto — o
     * que os modificadores somam além do espelho (cartas, passivas, a habilidade
     * de início do gato). Pura: recebe os números, não lê o jogo.
     *
     * @param base       valor do arquivo do gato
     * @param modificador soma dos modificadores no gato agora
     * @param doEspelho  o que o espelho somou
     */
    public static Camadas decompor(double base, double modificador, double doEspelho) {
        return new Camadas(base, doEspelho, modificador - doEspelho);
    }
}
