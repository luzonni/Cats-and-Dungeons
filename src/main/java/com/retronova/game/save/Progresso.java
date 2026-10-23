package com.retronova.game.save;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A PROGRESSÃO PERMANENTE: moedas e melhorias de cada gato, que nunca se perdem.
 *
 * DOIS SAVES, DOIS DONOS. A corrida em andamento continua em
 * {@code saves/corrida-N.json} (Corrida), que é apagado quando a corrida acaba.
 * O que fica de uma corrida para a outra mora aqui, em {@code saves/progresso.json},
 * e nada que aconteça numa corrida — morrer, abandonar, recomeçar, apagar o
 * espaço — apaga este arquivo. Só a opção "Reset progress" das opções.
 *
 * POR GATO. Cada gato tem as próprias moedas e as próprias melhorias, como foi
 * pedido. (A recomendação de design, de moedas compartilhadas, está em
 * docs/ARQUITETURA.md; mudar é trocar {@link DoGato} por um registro único.)
 *
 * O CRÉDITO DE UMA CORRIDA ACONTECE UMA VEZ SÓ. Cada corrida nasce com um id, e
 * o id creditado fica gravado aqui no MESMO arquivo e na MESMA escrita que as
 * moedas: ou as duas coisas ficam no disco, ou nenhuma. Pedir o crédito de novo
 * (o jogo fechou entre o crédito e o apagar do save da corrida, a morte foi
 * processada duas vezes) devolve zero. E a Corrida, ao ler um save cujo id já
 * foi creditado, o descarta: a corrida acabou, não se retoma.
 *
 * FORMATO (versão {@value #VERSAO}):
 * <pre>
 * { "versao": 2,
 *   "gatos": { "Muffin": { "moedas": 120, "ganhas": 900, "melhorias": { "vida": 2 } } },
 *   "creditadas": ["id-da-corrida", ...] }
 * </pre>
 * Versão 1 é o antigo saves/meta.json (ouro e cinco upgrades, globais); ver
 * {@link #migrarDoMeta}.
 *
 * Na bancada (-Dcd.bench) e no modo foto nada é gravado no save do jogador: o
 * modo foto usa a pasta isolada de sempre, a bancada não grava.
 */
public final class Progresso {

    public static final int VERSAO = 2;

    /** Os gatos que existem, pelo nome de players/*.json. A migração divide o ouro antigo entre eles. */
    public static final List<String> GATOS = List.of("Muffin", "Azrael", "Finn");

    /** Quantos ids de corrida creditada ficam guardados. Bem mais do que as corridas abertas ao mesmo tempo (3). */
    static final int IDS_GUARDADOS = 200;

    private static final String PASTA = com.retronova.engine.Foto.pastaDeSaves();
    private static Path arquivo = Paths.get(PASTA, "progresso.json");
    private static Path metaAntigo = Paths.get(PASTA, "meta.json");

    private static Dados dados;
    private static ArquivoSeguro.Origem origem = ArquivoSeguro.Origem.NOVO;

    private Progresso() {
    }

    // ------------------------------------------------------------------ dados

    /** O que um gato tem guardado. */
    public static final class DoGato {
        int moedas;
        /** Total já ganho, para estatística e para calibrar o balanceamento. */
        long ganhas;
        final Map<String, Integer> melhorias = new LinkedHashMap<>();

        public int moedas() {
            return moedas;
        }

        public long ganhas() {
            return ganhas;
        }

        public int nivel(String melhoria) {
            return melhorias.getOrDefault(melhoria, 0);
        }

        public Map<String, Integer> melhorias() {
            return Collections.unmodifiableMap(melhorias);
        }
    }

    /** O arquivo inteiro, em memória. Puro: dá para testar sem disco. */
    public static final class Dados {
        final Map<String, DoGato> gatos = new LinkedHashMap<>();
        final LinkedHashSet<String> creditadas = new LinkedHashSet<>();

        public DoGato gato(String nome) {
            return gatos.computeIfAbsent(nome == null ? "" : nome, n -> new DoGato());
        }

        public boolean foiCreditada(String id) {
            return id != null && creditadas.contains(id);
        }

        /** Soma as moedas de uma corrida, se ela ainda não foi creditada. Devolve o que entrou. */
        public int creditar(String id, String gato, int moedas) {
            if (id == null || id.isEmpty() || creditadas.contains(id)) {
                return 0;
            }
            creditadas.add(id);
            while (creditadas.size() > IDS_GUARDADOS) {
                creditadas.remove(creditadas.iterator().next());
            }
            DoGato g = gato(gato);
            int m = Math.max(0, moedas);
            g.moedas += m;
            g.ganhas += m;
            return m;
        }

        /** Gasta {@code custo} subindo a melhoria um nível, se der. */
        public boolean comprar(String gato, String melhoria, int custo, int nivelMaximo) {
            DoGato g = gato(gato);
            if (custo < 0 || g.nivel(melhoria) >= nivelMaximo || g.moedas < custo) {
                return false;
            }
            g.moedas -= custo;
            g.melhorias.merge(melhoria, 1, Integer::sum);
            return true;
        }

        /** Zera as melhorias do gato e devolve {@code reembolso} moedas. */
        public void redistribuir(String gato, int reembolso) {
            DoGato g = gato(gato);
            g.moedas += Math.max(0, reembolso);
            g.melhorias.clear();
        }

        @SuppressWarnings("unchecked")
        public String paraJson() {
            JSONObject o = new JSONObject();
            o.put("versao", (long) VERSAO);
            JSONObject gs = new JSONObject();
            for (Map.Entry<String, DoGato> e : gatos.entrySet()) {
                JSONObject g = new JSONObject();
                g.put("moedas", (long) e.getValue().moedas);
                g.put("ganhas", e.getValue().ganhas);
                JSONObject ms = new JSONObject();
                for (Map.Entry<String, Integer> m : e.getValue().melhorias.entrySet()) {
                    ms.put(m.getKey(), (long) m.getValue());
                }
                g.put("melhorias", ms);
                gs.put(e.getKey(), g);
            }
            o.put("gatos", gs);
            JSONArray ids = new JSONArray();
            ids.addAll(creditadas);
            o.put("creditadas", ids);
            return o.toJSONString();
        }

        /** Lê a versão atual. Versão mais nova que esta é recusada (não se rebaixa save). */
        public static Dados deJson(String texto) throws Exception {
            JSONObject o = (JSONObject) new JSONParser().parse(texto);
            long versao = o.get("versao") instanceof Number n ? n.longValue() : -1;
            if (versao != VERSAO) {
                throw new IllegalStateException("versao " + versao + " desconhecida");
            }
            Dados d = new Dados();
            JSONObject gs = (JSONObject) o.get("gatos");
            if (gs != null) {
                for (Object k : gs.keySet()) {
                    JSONObject g = (JSONObject) gs.get(k);
                    DoGato dg = d.gato(String.valueOf(k));
                    dg.moedas = (int) Math.max(0, longo(g.get("moedas")));
                    dg.ganhas = Math.max(0, longo(g.get("ganhas")));
                    JSONObject ms = (JSONObject) g.get("melhorias");
                    if (ms != null) {
                        for (Object mk : ms.keySet()) {
                            int n = (int) Math.max(0, longo(ms.get(mk)));
                            if (n > 0) {
                                dg.melhorias.put(String.valueOf(mk), n);
                            }
                        }
                    }
                }
            }
            if (o.get("creditadas") instanceof JSONArray ids) {
                for (Object id : ids) {
                    d.creditadas.add(String.valueOf(id));
                }
            }
            return d;
        }

        private static long longo(Object v) {
            return v instanceof Number n ? n.longValue() : 0L;
        }
    }

    // ------------------------------------------------------------- migração

    /**
     * Versão 1 → 2: o antigo saves/meta.json, global, vira progresso por gato.
     *
     * NADA SE PERDE: os upgrades antigos valiam para todos os gatos, então cada
     * gato recebe os mesmos níveis (com os nomes novos: VIGOR → vida, FORCA →
     * dano, AGILIDADE → velocidade, SORTE → sorte, IMA → coleta). O ouro guardado
     * é dividido entre os três, arredondando para cima — dividir, e não copiar,
     * porque copiar triplicaria o dinheiro do jogador.
     */
    public static Dados migrarDoMeta(String metaJson) throws Exception {
        JSONObject o = (JSONObject) new JSONParser().parse(metaJson);
        Dados d = new Dados();
        int ouro = o.get("ouro") instanceof Number n ? n.intValue() : 0;
        int porGato = (int) Math.ceil(Math.max(0, ouro) / (double) GATOS.size());
        Map<String, String> nomes = Map.of("VIGOR", "vida", "FORCA", "dano", "AGILIDADE", "velocidade",
                "SORTE", "sorte", "IMA", "coleta");
        for (String gato : GATOS) {
            DoGato g = d.gato(gato);
            g.moedas = porGato;
            for (Map.Entry<String, String> e : nomes.entrySet()) {
                if (o.get(e.getKey()) instanceof Number n && n.intValue() > 0) {
                    g.melhorias.put(e.getValue(), n.intValue());
                }
            }
        }
        return d;
    }

    // ------------------------------------------------------------- disco

    /** Os dados, lidos (ou migrados) na primeira consulta. */
    public static synchronized Dados dados() {
        if (dados == null) {
            carregar();
        }
        return dados;
    }

    /** De onde veio o que está carregado: principal, backup, novo ou perdido. */
    public static synchronized ArquivoSeguro.Origem origem() {
        dados();
        return origem;
    }

    private static void carregar() {
        ArquivoSeguro.Lido<Dados> lido = ArquivoSeguro.ler(arquivo, Dados::deJson);
        dados = lido.valor();
        origem = lido.origem();
        if (dados == null && lido.origem() == ArquivoSeguro.Origem.NOVO && Files.exists(metaAntigo)) {
            try {
                dados = migrarDoMeta(Files.readString(metaAntigo));
                origem = ArquivoSeguro.Origem.PRINCIPAL;
                gravar();
                // O antigo fica ao lado, renomeado: a migração não destrói o original.
                Files.move(metaAntigo, metaAntigo.resolveSibling("meta.json.migrado"),
                        StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[save] meta.json migrado para progresso.json (versao " + VERSAO + ").");
            } catch (Exception falha) {
                System.err.println("[save] nao consegui migrar meta.json: " + falha.getMessage());
            }
        }
        if (dados == null) {
            dados = new Dados();
        }
    }

    private static void gravar() {
        // Bancada: nunca mexe na progressão de verdade. Com o modo foto junto, a
        // pasta já é a isolada (build/foto/saves) e gravar é o que deixa testar o
        // crédito de moedas de ponta a ponta.
        if (Boolean.getBoolean("cd.bench") && !com.retronova.engine.Foto.ligado()) {
            return;
        }
        try {
            ArquivoSeguro.gravar(arquivo, dados.paraJson(), Dados::deJson);
        } catch (Exception falha) {
            System.err.println("[save] nao foi possivel gravar progresso.json: " + falha.getMessage());
        }
    }

    // ------------------------------------------------------------- consulta

    public static synchronized int moedas(String gato) {
        return dados().gato(gato).moedas;
    }

    public static synchronized int nivel(String gato, String melhoria) {
        return dados().gato(gato).nivel(melhoria);
    }

    public static synchronized DoGato doGato(String gato) {
        return dados().gato(gato);
    }

    public static synchronized boolean foiCreditada(String idDaCorrida) {
        return dados().foiCreditada(idDaCorrida);
    }

    // ------------------------------------------------------------- ações

    /**
     * Credita as moedas de uma corrida que acabou. Devolve quantas entraram — zero
     * se este id já tinha sido creditado.
     */
    public static synchronized int creditar(String idDaCorrida, String gato, int moedas) {
        boolean nova = idDaCorrida != null && !idDaCorrida.isEmpty() && !dados().foiCreditada(idDaCorrida);
        int entrou = dados().creditar(idDaCorrida, gato, moedas);
        if (nova) {
            gravar();   // grava mesmo com zero moedas: o id creditado também precisa ficar
        }
        return entrou;
    }

    public static synchronized boolean comprar(String gato, String melhoria, int custo, int nivelMaximo) {
        boolean ok = dados().comprar(gato, melhoria, custo, nivelMaximo);
        if (ok) {
            gravar();
        }
        return ok;
    }

    public static synchronized void redistribuir(String gato, int reembolso) {
        dados().redistribuir(gato, reembolso);
        gravar();
    }

    /**
     * Apaga a progressão de todos os gatos. O arquivo atual vira backup antes, como
     * em toda gravação: um engano ainda tem volta à mão, renomeando o .bak.
     */
    public static synchronized void apagarTudo() {
        Dados limpo = new Dados();
        // Os ids creditados ficam: apagar a progressão não pode deixar uma corrida
        // velha ser creditada de novo.
        limpo.creditadas.addAll(dados().creditadas);
        dados = limpo;
        gravar();
    }

    // ------------------------------------------------------------- testes

    /** Aponta para outra pasta e esquece o que estava carregado. Só para os testes. */
    static synchronized void usarPasta(Path pasta) {
        arquivo = pasta.resolve("progresso.json");
        metaAntigo = pasta.resolve("meta.json");
        dados = null;
        origem = ArquivoSeguro.Origem.NOVO;
    }

    /** Os gatos com algo guardado. */
    public static synchronized List<String> gatosComProgresso() {
        return new ArrayList<>(dados().gatos.keySet());
    }
}
