package com.retronova.game.dialogos;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O que o jogador já ouviu, e a corrida anterior: o que as conversas consultam.
 *
 * ARQUIVO PRÓPRIO, saves/dialogos.json, como o Meta e as Conquistas. O save dos
 * espaços (Corrida) é só da corrida em andamento e é apagado na morte; o que o
 * vendedor lembra tem de sobreviver a ela. E a futura progressão permanente vai
 * ter um save novo: com tudo de diálogo num arquivo só, com versão, migrar é ler
 * este JSON e copiar os campos de {@link Dados} — ver docs/ARQUITETURA.md,
 * "Diálogos", seção "Migração".
 *
 * A bancada (-Dcd.bench) não grava, igual ao Meta: a memória vale só enquanto o
 * processo vive, e o save do jogador fica intocado.
 */
public final class MemoriaDosDialogos {

    /** Versão do formato. Sobe quando um campo muda de sentido. */
    public static final int VERSAO = 1;

    private static final Path ARQUIVO = Paths.get(com.retronova.engine.Foto.pastaDeSaves(), "dialogos.json");
    private static Dados dados;

    private MemoriaDosDialogos() {
    }

    /** Os fatos lembrados. Puro: dá para montar e testar sem disco. */
    public static final class Dados {
        final Map<String, Integer> vistas = new LinkedHashMap<>();
        final Map<String, Long> quando = new LinkedHashMap<>();
        final Map<String, Integer> conversas = new LinkedHashMap<>();
        long relogio;
        int corridas;
        String ultimoResultado = "nenhuma";
        int ultimaDuracao, ultimoNivel, ultimosAbates;
        String ultimoGato = "";

        public int vezes(String conversa) {
            return vistas.getOrDefault(conversa, 0);
        }

        /** Momento (em conversas tocadas) em que esta foi vista pela última vez; -1 se nunca. */
        public long vistaEm(String conversa) {
            return quando.getOrDefault(conversa, -1L);
        }

        public int conversasCom(String personagem) {
            return conversas.getOrDefault(personagem, 0);
        }

        public int corridas() {
            return corridas;
        }

        public boolean houveCorrida() {
            return corridas > 0;
        }

        public String ultimoResultado() {
            return ultimoResultado;
        }

        public int ultimaDuracao() {
            return ultimaDuracao;
        }

        public int ultimoNivel() {
            return ultimoNivel;
        }

        /** Registra que a conversa tocou agora. */
        public void marcar(Conversa c) {
            relogio++;
            vistas.merge(c.id(), 1, Integer::sum);
            quando.put(c.id(), relogio);
            conversas.merge(c.personagem(), 1, Integer::sum);
        }

        /** Registra o fim de uma corrida: "morte" ou "vitoria". */
        public void fimDeCorrida(String resultado, int segundos, int nivel, int abates, String gato) {
            corridas++;
            ultimoResultado = resultado;
            ultimaDuracao = Math.max(0, segundos);
            ultimoNivel = Math.max(0, nivel);
            ultimosAbates = Math.max(0, abates);
            ultimoGato = gato == null ? "" : gato;
        }

        @SuppressWarnings("unchecked")
        String paraJson() {
            JSONObject o = new JSONObject();
            o.put("versao", (long) VERSAO);
            o.put("relogio", relogio);
            o.put("vistas", new JSONObject(vistas));
            o.put("quando", new JSONObject(quando));
            o.put("conversas", new JSONObject(conversas));
            JSONObject corrida = new JSONObject();
            corrida.put("total", (long) corridas);
            corrida.put("resultado", ultimoResultado);
            corrida.put("segundos", (long) ultimaDuracao);
            corrida.put("nivel", (long) ultimoNivel);
            corrida.put("abates", (long) ultimosAbates);
            corrida.put("gato", ultimoGato);
            o.put("ultimaCorrida", corrida);
            return o.toJSONString();
        }

        static Dados deJson(String texto) throws Exception {
            JSONObject o = (JSONObject) new JSONParser().parse(texto);
            Dados d = new Dados();
            d.relogio = longo(o.get("relogio"));
            lerInteiros((JSONObject) o.get("vistas"), d.vistas);
            lerInteiros((JSONObject) o.get("conversas"), d.conversas);
            JSONObject q = (JSONObject) o.get("quando");
            if (q != null) {
                for (Object k : q.keySet()) {
                    d.quando.put(String.valueOf(k), longo(q.get(k)));
                }
            }
            JSONObject c = (JSONObject) o.get("ultimaCorrida");
            if (c != null) {
                d.corridas = (int) longo(c.get("total"));
                d.ultimoResultado = c.get("resultado") == null ? "nenhuma" : String.valueOf(c.get("resultado"));
                d.ultimaDuracao = (int) longo(c.get("segundos"));
                d.ultimoNivel = (int) longo(c.get("nivel"));
                d.ultimosAbates = (int) longo(c.get("abates"));
                d.ultimoGato = c.get("gato") == null ? "" : String.valueOf(c.get("gato"));
            }
            return d;
        }

        private static void lerInteiros(JSONObject o, Map<String, Integer> destino) {
            if (o == null) {
                return;
            }
            for (Object k : o.keySet()) {
                destino.put(String.valueOf(k), (int) longo(o.get(k)));
            }
        }

        private static long longo(Object v) {
            return v instanceof Number n ? n.longValue() : 0L;
        }
    }

    /** A memória, lida do disco na primeira consulta. */
    public static synchronized Dados dados() {
        if (dados == null) {
            dados = new Dados();
            try {
                if (Files.exists(ARQUIVO)) {
                    dados = Dados.deJson(Files.readString(ARQUIVO, StandardCharsets.UTF_8));
                }
            } catch (Exception ruim) {
                System.err.println("dialogos.json ilegivel, comecando do zero: " + ruim.getMessage());
                dados = new Dados();
            }
        }
        return dados;
    }

    /** Uma conversa começou a tocar. Grava em seguida. */
    public static synchronized void marcar(Conversa c) {
        dados().marcar(c);
        gravar();
    }

    /** A corrida acabou. Chamado pelo Game, na morte e no encerramento depois da vitória. */
    public static synchronized void fimDeCorrida(boolean venceu, int segundos, int nivel, int abates, String gato) {
        dados().fimDeCorrida(venceu ? "vitoria" : "morte", segundos, nivel, abates, gato);
        gravar();
    }

    private static void gravar() {
        if (Boolean.getBoolean("cd.bench")) {
            return;
        }
        try {
            Files.createDirectories(ARQUIVO.getParent());
            Path temporario = ARQUIVO.resolveSibling("dialogos.json.tmp");
            Files.writeString(temporario, dados.paraJson(), StandardCharsets.UTF_8);
            Files.move(temporario, ARQUIVO, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception falha) {
            System.err.println("Nao foi possivel gravar dialogos.json: " + falha.getMessage());
        }
    }
}
