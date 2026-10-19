package com.retronova.game.map.arena;

import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.enemies.Quebravel;
import com.retronova.game.objects.tiles.TileIDs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Arena da cisterna gerada na hora: um CAMPO ABERTO, grande, para andar livre.
 *
 * A PRIMEIRA VERSAO GERAVA CAVERNAS, e as paredes atrapalhavam tudo: o gato se
 * enroscava fugindo, os ratos se enroscavam perseguindo, e a horda chegava em
 * fila pelos corredores em vez de cercar. Num jogo de horda o espaco e a arma
 * do jogador — Vampire Survivors e um campo sem fim por isso.
 *
 * Agora e um campo de 160x160 tiles so com a borda de parede. O que varia e o
 * CHAO: manchas de pedra gasta e rachada, pocas de agua (atravessaveis) e
 * enfeites — o bastante para o lugar nao parecer um ladrilho infinito. Os
 * unicos objetos solidos sao braseiros esparsos, que servem de marco e de luz.
 * Os objetos quebraveis nao sao postos aqui de uma vez: a Arena os mantem
 * aparecendo em volta do gato enquanto ele anda (ver Arena.manterQuebraveis).
 */
public final class Gerador {

    private Gerador() {
    }

    public static final int LARGURA = 160;
    public static final int ALTURA = 160;
    private static final int BORDA = 2;
    /** Distancia entre braseiros, em tiles (com sorteio em volta). */
    private static final int GRADE_DE_BRASEIROS = 20;
    private static final int RAIO_DO_NASCIMENTO = 6;

    public static GameMap.Planta cisterna(long semente) {
        long inicio = System.nanoTime();
        Random r = new Random(semente);
        boolean[][] parede = new boolean[LARGURA][ALTURA];
        for (int x = 0; x < LARGURA; x++) {
            for (int y = 0; y < ALTURA; y++) {
                parede[x][y] = x < BORDA || y < BORDA || x >= LARGURA - BORDA || y >= ALTURA - BORDA;
            }
        }
        int cx = LARGURA / 2, cy = ALTURA / 2;
        // Sem saida: a corrida e sem fim (ver Waves). O "ponto limpo" e so o nascimento.
        Point saida = new Point(cx, cy);
        double[][] manchas = ruido(r, 14);
        double[][] pocas = ruido(r, 10);

        BufferedImage img = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < LARGURA; x++) {
            for (int y = 0; y < ALTURA; y++) {
                img.setRGB(x, y, tile(parede, x, y, manchas[x][y], pocas[x][y], cx, cy, saida, r).getColor());
            }
        }
        GameMap.Planta planta = new GameMap.Planta(img, entidades(parede, saida, cx, cy, r), primeirosQuebraveis(parede, r, cx, cy));
        if (com.retronova.engine.Desempenho.LIGADO) {
            System.out.printf("[perf] arena gerada em %.1f ms%n", (System.nanoTime() - inicio) / 1e6);
        }
        return planta;
    }

    /**
     * Ruido suave: valores sorteados numa grade grossa e interpolados entre si.
     * Da manchas organicas (e nao sal-e-pimenta) com uma conta so.
     */
    private static double[][] ruido(Random r, int celula) {
        int gw = LARGURA / celula + 2, gh = ALTURA / celula + 2;
        double[][] grade = new double[gw][gh];
        for (int i = 0; i < gw; i++) {
            for (int j = 0; j < gh; j++) {
                grade[i][j] = r.nextDouble();
            }
        }
        double[][] v = new double[LARGURA][ALTURA];
        for (int x = 0; x < LARGURA; x++) {
            for (int y = 0; y < ALTURA; y++) {
                double fx = x / (double) celula, fy = y / (double) celula;
                int i = (int) fx, j = (int) fy;
                double tx = suave(fx - i), ty = suave(fy - j);
                double a = grade[i][j] + (grade[i + 1][j] - grade[i][j]) * tx;
                double b = grade[i][j + 1] + (grade[i + 1][j + 1] - grade[i][j + 1]) * tx;
                v[x][y] = a + (b - a) * ty;
            }
        }
        return v;
    }

    private static double suave(double t) {
        return t * t * (3 - 2 * t);
    }

    private static TileIDs tile(boolean[][] p, int x, int y, double mancha, double poca,
                                int cx, int cy, Point saida, Random r) {
        if (p[x][y]) {
            boolean face = y + 1 < ALTURA && !p[x][y + 1];
            return face ? TileIDs.CisternStoneFace : TileIDs.CisternStoneTop;
        }
        boolean limpo = Math.hypot(x - cx, y - cy) <= 3 || Math.abs(x - saida.x) <= 1 && Math.abs(y - saida.y) <= 2;
        if (limpo) {
            return TileIDs.CisternFloor;
        }
        // Pocas: onde o ruido de agua passa do limite. Atravessaveis, so mudam o chao.
        if (poca > 0.74) {
            return TileIDs.CisternWater;
        }
        double d = r.nextDouble();
        if (d < 0.025) {
            TileIDs[] enfeites = {TileIDs.CisternSkull, TileIDs.CisternPlate, TileIDs.CisternFlask,
                    TileIDs.CisternFlaskBig, TileIDs.CisternSink};
            return enfeites[r.nextInt(enfeites.length)];
        }
        // Manchas de pedra gasta e rachada, em vez de ladrilho repetido.
        if (mancha > 0.68) return d < 0.6 ? TileIDs.CisternFloorCracked : TileIDs.CisternFloorWorn;
        if (mancha > 0.5) return d < 0.5 ? TileIDs.CisternFloorWorn : TileIDs.CisternFloor;
        return d < 0.88 ? TileIDs.CisternFloor : TileIDs.CisternFloorWorn;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject entidades(boolean[][] p, Point saida, int cx, int cy, Random r) {
        JSONObject json = new JSONObject();
        JSONObject nascimento = new JSONObject();
        nascimento.put("x", (long) cx);
        nascimento.put("y", (long) cy);
        json.put("spawn", nascimento);
        JSONArray lista = new JSONArray();
        // Braseiros numa grade folgada com sorteio: marcos e luz pelo campo todo.
        for (int gx = GRADE_DE_BRASEIROS / 2; gx < LARGURA - BORDA; gx += GRADE_DE_BRASEIROS) {
            for (int gy = GRADE_DE_BRASEIROS / 2; gy < ALTURA - BORDA; gy += GRADE_DE_BRASEIROS) {
                int x = gx + r.nextInt(9) - 4, y = gy + r.nextInt(9) - 4;
                if (x <= BORDA || y <= BORDA || x >= LARGURA - BORDA - 1 || y >= ALTURA - BORDA - 1
                        || p[x][y] || Math.hypot(x - cx, y - cy) < RAIO_DO_NASCIMENTO + 4) {
                    continue;
                }
                lista.add(entidade("Brazier", x, y));
            }
        }
        json.put("entities", lista);
        return json;
    }

    /** Alguns quebraveis ja em volta do nascimento; o resto a Arena vai repondo. */
    private static List<Entity> primeirosQuebraveis(boolean[][] p, Random r, int cx, int cy) {
        List<Entity> extras = new ArrayList<>();
        int s = GameObject.SIZE();
        for (int i = 0; i < 6; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = RAIO_DO_NASCIMENTO + 2 + r.nextDouble() * 6;
            int x = (int) (cx + Math.cos(ang) * dist), y = (int) (cy + Math.sin(ang) * dist);
            if (p[x][y] || p[x][y + 1]) {
                continue;
            }
            extras.add(new Quebravel(x * (double) s, y * (double) s, r.nextDouble() < 0.35));
        }
        return extras;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject entidade(String nome, int x, int y, String... valores) {
        JSONObject o = new JSONObject();
        o.put("name", nome);
        o.put("x", (long) x);
        o.put("y", (long) y);
        JSONArray v = new JSONArray();
        for (String s : valores) {
            v.add(s);
        }
        o.put("values", v);
        return o;
    }
}
