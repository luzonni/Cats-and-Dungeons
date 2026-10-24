package com.retronova.game.dialogos;

import com.retronova.engine.Engine;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê os arquivos de diálogo: personagens, textos e conversas.
 *
 * TUDO EM {@code resources/.../dialogos/}:
 *  - {@code personagens.json}: quem fala, com nome, retrato, voz e o arquivo das
 *    conversas de cada um;
 *  - {@code textos.json}: todas as frases, por chave — o lugar central para uma
 *    tradução futura;
 *  - um arquivo de conversas por personagem (o vendedor em {@code vendedor.json}).
 *
 * POR QUE UM ÍNDICE, e não "ler todo arquivo da pasta": dentro do jar não há como
 * listar uma pasta de recursos de forma confiável. O personagem diz qual é o
 * arquivo dele, e personagem novo é uma entrada a mais no índice.
 *
 * Chaves começadas por "_" são notas para quem escreve e são ignoradas, porque o
 * JSON não tem comentário.
 *
 * Lido uma vez e guardado; {@link #recarregar} existe para os testes.
 */
public final class CatalogoDeDialogos {

    private static final String PASTA = Engine.resPath + "dialogos/";

    private final Map<String, Personagem> personagens = new LinkedHashMap<>();
    private final Map<String, String> textos = new LinkedHashMap<>();
    private final List<Conversa> conversas = new ArrayList<>();
    private final List<String> problemas = new ArrayList<>();

    private static CatalogoDeDialogos atual;

    private CatalogoDeDialogos() {
    }

    public static synchronized CatalogoDeDialogos get() {
        if (atual == null) {
            atual = carregar();
        }
        return atual;
    }

    public static synchronized void recarregar() {
        atual = null;
    }

    public Personagem personagem(String id) {
        return personagens.get(id);
    }

    public Map<String, Personagem> personagens() {
        return Collections.unmodifiableMap(personagens);
    }

    /** A frase de uma chave; a própria chave entre colchetes se faltar, para aparecer no teste de jogo. */
    public String texto(String chave) {
        String t = textos.get(chave);
        return t != null ? t : "[" + chave + "]";
    }

    public boolean temTexto(String chave) {
        return textos.containsKey(chave);
    }

    public List<Conversa> conversas() {
        return Collections.unmodifiableList(conversas);
    }

    /** O que estava errado nos arquivos: texto faltando, personagem desconhecido. Vazio = tudo certo. */
    public List<String> problemas() {
        return Collections.unmodifiableList(problemas);
    }

    // ----------------------------------------------------------------- leitura

    private static CatalogoDeDialogos carregar() {
        CatalogoDeDialogos c = new CatalogoDeDialogos();
        try {
            JSONObject textos = ler("textos.json");
            for (Object k : textos.keySet()) {
                String chave = String.valueOf(k);
                if (!chave.startsWith("_")) {
                    c.textos.put(chave, String.valueOf(textos.get(k)));
                }
            }
            JSONObject pessoas = ler("personagens.json");
            for (Object k : pessoas.keySet()) {
                String id = String.valueOf(k);
                if (id.startsWith("_")) {
                    continue;
                }
                JSONObject p = (JSONObject) pessoas.get(k);
                List<String> voz = new ArrayList<>();
                if (p.get("voz") instanceof JSONArray a) {
                    for (Object v : a) {
                        voz.add(String.valueOf(v));
                    }
                }
                c.personagens.put(id, new Personagem(id, texto(p, "nome", id), texto(p, "retrato", ""),
                        voz, texto(p, "moldura", null)));
                String arquivo = texto(p, "conversas", null);
                if (arquivo != null) {
                    c.lerConversas(id, ler(arquivo));
                }
            }
        } catch (Exception falha) {
            c.problemas.add("falha ao ler os dialogos: " + falha);
        }
        c.validar();
        for (String p : c.problemas) {
            System.err.println("[dialogos] " + p);
        }
        return c;
    }

    private void lerConversas(String dono, JSONObject arquivo) {
        Object lista = arquivo.get("conversas");
        if (!(lista instanceof JSONArray a)) {
            problemas.add(dono + ": arquivo sem a lista 'conversas'");
            return;
        }
        for (Object o : a) {
            JSONObject j = (JSONObject) o;
            String id = texto(j, "id", null);
            if (id == null) {
                problemas.add(dono + ": conversa sem id");
                continue;
            }
            String personagem = texto(j, "personagem", dono);
            List<Fala> falas = new ArrayList<>();
            if (j.get("falas") instanceof JSONArray fs) {
                for (Object f : fs) {
                    JSONObject fj = (JSONObject) f;
                    falas.add(new Fala(texto(fj, "personagem", personagem),
                            Emocao.de(texto(fj, "emocao", null)), texto(fj, "texto", ""),
                            lista(fj.get("eventos"))));
                }
            }
            Map<String, Object> condicoes = new LinkedHashMap<>();
            if (j.get("condicoes") instanceof JSONObject cj) {
                for (Object k : cj.keySet()) {
                    String nome = String.valueOf(k);
                    if (!nome.startsWith("_")) {
                        condicoes.put(nome, cj.get(k));
                    }
                }
            }
            int prioridade = j.get("prioridade") instanceof Number n ? n.intValue() : 0;
            conversas.add(new Conversa(id, personagem, texto(j, "gatilho", "interagir"), prioridade,
                    condicoes, Conversa.Repeticao.de(texto(j, "repeticao", null)), falas,
                    lista(j.get("aoTerminar"))));
        }
    }

    private void validar() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Conversa c : conversas) {
            if (!ids.add(c.id())) {
                problemas.add("conversa repetida: " + c.id());
            }
            if (c.falas().isEmpty()) {
                problemas.add(c.id() + ": sem falas");
            }
            for (String cond : c.condicoes().keySet()) {
                if (!Condicoes.nomes().contains(cond)) {
                    problemas.add(c.id() + ": condicao desconhecida '" + cond + "'");
                }
            }
            for (Fala f : c.falas()) {
                if (!personagens.containsKey(f.personagem())) {
                    problemas.add(c.id() + ": personagem desconhecido '" + f.personagem() + "'");
                }
                if (!textos.containsKey(f.texto())) {
                    problemas.add(c.id() + ": texto faltando '" + f.texto() + "'");
                }
                for (String ev : f.eventos()) {
                    if (!EventosDoDialogo.conhece(ev)) {
                        problemas.add(c.id() + ": evento desconhecido '" + ev + "'");
                    }
                }
            }
            for (String ev : c.aoTerminar()) {
                if (!EventosDoDialogo.conhece(ev)) {
                    problemas.add(c.id() + ": evento desconhecido '" + ev + "'");
                }
            }
        }
    }

    private static JSONObject ler(String arquivo) throws Exception {
        try (InputStream in = CatalogoDeDialogos.class.getResourceAsStream(PASTA + arquivo)) {
            if (in == null) {
                throw new IllegalStateException("nao achei " + PASTA + arquivo);
            }
            return (JSONObject) new JSONParser().parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static String texto(JSONObject o, String chave, String padrao) {
        Object v = o.get(chave);
        return v == null ? padrao : String.valueOf(v);
    }

    private static List<String> lista(Object o) {
        List<String> saida = new ArrayList<>();
        if (o instanceof JSONArray a) {
            for (Object v : a) {
                saida.add(String.valueOf(v));
            }
        }
        return saida;
    }
}
