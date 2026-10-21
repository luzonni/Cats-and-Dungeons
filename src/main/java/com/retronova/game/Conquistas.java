package com.retronova.game;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * As conquistas do jogo: metas de longo prazo que atravessam as corridas.
 *
 * O ouro e os upgrades dão um motivo para jogar a próxima corrida; as conquistas
 * dão um motivo para jogá-la DE OUTRO JEITO — vencer com cada gato, aguentar mais
 * tempo, enfrentar uma horda maior. Cada uma é uma função do {@link Progresso},
 * que são só números guardados entre corridas. Não existe uma lista de "já
 * desbloqueadas" separada para ficar fora de sincronia: se o progresso diz que a
 * meta foi batida, está desbloqueada.
 *
 * Gravado em saves/conquistas.json, separado do {@link Meta} e das corridas:
 * apagar uma corrida não apaga o que foi conquistado.
 */
public final class Conquistas {

    private Conquistas() {
    }

    /** Novas sempre no FIM: a ordem é a da tela. */
    public enum Conquista {
        PRIMEIRO_SANGUE("First Blood", "Defeat your first enemy", 1) {
            @Override
            public long valor(Progresso p) {
                return p.abatesTotais;
            }
        },
        EXTERMINADOR("Exterminator", "Defeat 1,000 enemies in total", 1000) {
            @Override
            public long valor(Progresso p) {
                return p.abatesTotais;
            }
        },
        SOBREVIVENTE("Survivor", "Last 5 minutes in a single run", 5 * 60) {
            @Override
            public long valor(Progresso p) {
                return p.melhorTempo;
            }
        },
        VETERANO("Veteran", "Last 15 minutes in a single run", 15 * 60) {
            @Override
            public long valor(Progresso p) {
                return p.melhorTempo;
            }
        },
        MARATONA("Long Haul", "Reach the 30-minute mark", 30 * 60) {
            @Override
            public long valor(Progresso p) {
                return p.melhorTempo;
            }
        },
        CACADOR_DE_REIS("Rat Slayer", "Defeat the Rat King", 1) {
            @Override
            public long valor(Progresso p) {
                return p.vitorias;
            }
        },
        ORGULHO_FELINO("Feline Pride", "Win with each of the three cats", 3) {
            @Override
            public long valor(Progresso p) {
                return p.gatosVencedores.size();
            }
        },
        EXPERIENTE("Seasoned", "Reach level 20", 20) {
            @Override
            public long valor(Progresso p) {
                return p.melhorNivel;
            }
        },
        CACADOR_DE_TESOUROS("Treasure Hunter", "Open 10 chests in a single run", 10) {
            @Override
            public long valor(Progresso p) {
                return p.melhorBaus;
            }
        },
        CERCADO("Surrounded", "Face 100 enemies at once", 100) {
            @Override
            public long valor(Progresso p) {
                return p.melhorHorda;
            }
        };

        private final String nome;
        private final String descricao;
        private final long meta;

        Conquista(String nome, String descricao, long meta) {
            this.nome = nome;
            this.descricao = descricao;
            this.meta = meta;
        }

        public String nome() {
            return nome;
        }

        public String descricao() {
            return descricao;
        }

        /** O número que o progresso tem de alcançar. */
        public long meta() {
            return meta;
        }

        /** Onde o progresso está agora, para esta conquista. Pode passar da meta. */
        public abstract long valor(Progresso p);

        public boolean alcancada(Progresso p) {
            return valor(p) >= meta;
        }

        /** Quanto falta, de 0 a 1, para a barra de progresso. */
        public double fracao(Progresso p) {
            return Math.max(0, Math.min(1, valor(p) / (double) meta));
        }

        /** O progresso mostrado, limitado à meta: "800 / 1000", nunca "1500 / 1000". */
        public long valorMostrado(Progresso p) {
            return Math.min(meta, valor(p));
        }
    }

    // ------------------------------------------------------------ o progresso

    /** Os números guardados entre corridas. Puro: sem disco, sem estado global. */
    public static final class Progresso {
        long abatesTotais;
        int melhorTempo;
        int melhorNivel;
        int melhorBaus;
        int melhorHorda;
        int vitorias;
        final Set<String> gatosVencedores = new LinkedHashSet<>();

        public long abatesTotais() {
            return abatesTotais;
        }

        public int vitorias() {
            return vitorias;
        }

        /** Soma uma corrida terminada. Os "melhores" só sobem. */
        public void registrarCorrida(int segundos, int abates, int nivel, int baus, int horda) {
            abatesTotais += Math.max(0, abates);
            melhorTempo = Math.max(melhorTempo, segundos);
            melhorNivel = Math.max(melhorNivel, nivel);
            melhorBaus = Math.max(melhorBaus, baus);
            melhorHorda = Math.max(melhorHorda, horda);
        }

        /** Registra uma vitória do gato dado. */
        public void registrarVitoria(String gato) {
            vitorias++;
            if (gato != null && !gato.isEmpty()) {
                gatosVencedores.add(gato);
            }
        }

        /** As conquistas que este progresso alcança e o {@code antes} ainda não alcançava. */
        public List<Conquista> novasDesde(Progresso antes) {
            List<Conquista> novas = new ArrayList<>();
            for (Conquista c : Conquista.values()) {
                if (c.alcancada(this) && !c.alcancada(antes)) {
                    novas.add(c);
                }
            }
            return novas;
        }

        public int totalDesbloqueadas() {
            int n = 0;
            for (Conquista c : Conquista.values()) {
                if (c.alcancada(this)) {
                    n++;
                }
            }
            return n;
        }

        public Progresso copia() {
            Progresso o = new Progresso();
            o.abatesTotais = abatesTotais;
            o.melhorTempo = melhorTempo;
            o.melhorNivel = melhorNivel;
            o.melhorBaus = melhorBaus;
            o.melhorHorda = melhorHorda;
            o.vitorias = vitorias;
            o.gatosVencedores.addAll(gatosVencedores);
            return o;
        }

        @SuppressWarnings("unchecked")
        public String paraJson() {
            JSONObject o = new JSONObject();
            o.put("abatesTotais", abatesTotais);
            o.put("melhorTempo", (long) melhorTempo);
            o.put("melhorNivel", (long) melhorNivel);
            o.put("melhorBaus", (long) melhorBaus);
            o.put("melhorHorda", (long) melhorHorda);
            o.put("vitorias", (long) vitorias);
            JSONArray gatos = new JSONArray();
            gatos.addAll(gatosVencedores);
            o.put("gatosVencedores", gatos);
            return o.toJSONString();
        }

        /** Lê um progresso; campo faltando ou torto vira zero, nunca derruba o jogo. */
        public static Progresso deJson(String texto) throws Exception {
            JSONObject o = (JSONObject) new JSONParser().parse(texto);
            Progresso p = new Progresso();
            p.abatesTotais = Math.max(0, numero(o, "abatesTotais"));
            p.melhorTempo = (int) Math.max(0, numero(o, "melhorTempo"));
            p.melhorNivel = (int) Math.max(0, numero(o, "melhorNivel"));
            p.melhorBaus = (int) Math.max(0, numero(o, "melhorBaus"));
            p.melhorHorda = (int) Math.max(0, numero(o, "melhorHorda"));
            p.vitorias = (int) Math.max(0, numero(o, "vitorias"));
            if (o.get("gatosVencedores") instanceof JSONArray gatos) {
                for (Object g : gatos) {
                    if (g instanceof String nome && !nome.isEmpty()) {
                        p.gatosVencedores.add(nome);
                    }
                }
            }
            return p;
        }

        private static long numero(JSONObject o, String chave) {
            return o.get(chave) instanceof Number n ? n.longValue() : 0;
        }
    }

    // ------------------------------------------------------------ o estado e o disco

    private static final Path ARQUIVO = Paths.get("saves", "conquistas.json");
    private static Progresso atual = new Progresso();
    private static boolean carregado;
    /** Desbloqueadas nesta sessão e ainda não vistas na tela de conquistas. */
    private static final Set<Conquista> aindaNaoVistas = new LinkedHashSet<>();

    private static void carregar() {
        if (carregado) {
            return;
        }
        carregado = true;
        try {
            if (Files.exists(ARQUIVO)) {
                atual = Progresso.deJson(Files.readString(ARQUIVO, StandardCharsets.UTF_8));
            }
        } catch (Exception arquivoRuim) {
            // Arquivo corrompido: comeca do zero em vez de nao abrir o jogo.
            System.err.println("conquistas.json ilegivel, comecando do zero: " + arquivoRuim.getMessage());
            atual = new Progresso();
        }
    }

    private static void gravar() {
        if (Boolean.getBoolean("cd.bench")) {
            return;     // modo de teste: nunca mexe na progressao de verdade do jogador
        }
        try {
            Files.createDirectories(ARQUIVO.getParent());
            Files.writeString(ARQUIVO, atual.paraJson(), StandardCharsets.UTF_8);
        } catch (Exception falha) {
            System.err.println("Nao foi possivel gravar conquistas.json: " + falha.getMessage());
        }
    }

    /** O progresso atual, só para leitura: quem quiser mexer usa os métodos abaixo. */
    public static Progresso progresso() {
        carregar();
        return atual.copia();
    }

    /**
     * Fecha uma corrida: soma os números e grava.
     *
     * @return as conquistas que esta corrida desbloqueou
     */
    public static List<Conquista> registrarCorrida(int segundos, int abates, int nivel, int baus, int horda) {
        carregar();
        Progresso antes = atual.copia();
        atual.registrarCorrida(segundos, abates, nivel, baus, horda);
        return aplicar(antes);
    }

    /**
     * Soma uma vitória sobre o chefe final e grava na hora: o jogador pode fechar
     * o jogo na tela de vitória, e a conquista não pode se perder ali.
     */
    public static List<Conquista> registrarVitoria(String gato) {
        carregar();
        Progresso antes = atual.copia();
        atual.registrarVitoria(gato);
        return aplicar(antes);
    }

    private static List<Conquista> aplicar(Progresso antes) {
        List<Conquista> novas = atual.novasDesde(antes);
        aindaNaoVistas.addAll(novas);
        gravar();
        return novas;
    }

    /** Há conquistas novas que o jogador ainda não viu na tela? */
    public static boolean temNovas() {
        return !aindaNaoVistas.isEmpty();
    }

    public static boolean ehNova(Conquista c) {
        return aindaNaoVistas.contains(c);
    }

    /** O jogador abriu a tela: as marcas de "novo" somem. */
    public static void marcarComoVistas() {
        aindaNaoVistas.clear();
    }
}
