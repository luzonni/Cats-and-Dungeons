package com.retronova.game.objects.physical;

import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Afasta corpos solidos que se sobrepoem, na proporcao do peso de cada um.
 *
 * RODA NO TICK DO JOGO, e nao mais numa thread propria. A thread mexia nas
 * posicoes enquanto o laco principal lia e desenhava as mesmas entidades, e
 * nunca terminava: o dispose so baixava uma flag, mas ela dormia num wait que
 * ninguem mais acordava — cada troca de mapa deixava uma thread presa para tras.
 * No tick ela roda depois de todo mundo andar, que e o momento certo de
 * desfazer as sobreposicoes, e a conta de uma horda cheia cabe folgada no quadro.
 */
public class Repulsion {

    private final GameMap map;

    public Repulsion(GameMap map) {
        this.map = map;
    }

    /**
     * Quem esta em cada celula, pelo indice na lista de entidades.
     *
     * A busca de vizinhos era contra a lista INTEIRA, para cada corpo solido: com
     * 170 bichos, uns 29 mil testes de distancia por tick, crescendo com o
     * quadrado da horda — e a horda e justamente o que cresce. Com a grade, cada
     * corpo so olha as nove celulas em volta da sua.
     *
     * A celula tem o lado do MAIOR corpo solido do tick: dois corpos so se tocam
     * se os centros estao a menos de (l1 + l2) / 2, que nunca passa desse lado, e
     * entao o vizinho de verdade sempre esta numa das nove.
     */
    private final Map<Long, List<Integer>> grade = new HashMap<>();
    private final List<Integer> candidatos = new ArrayList<>();
    private final List<Entity> nears = new ArrayList<>();

    public void tick() {
        List<Entity> entities = map.getEntities();
        double celula = 0;
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (e.isSolid()) {
                celula = Math.max(celula, e.getWidth());
            }
        }
        if (celula <= 0) {
            return;
        }
        grade.clear();
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (e.isSolid()) {
                grade.computeIfAbsent(chave(celulaDe(centroX(e), celula), celulaDe(centroY(e), celula)),
                        k -> new ArrayList<>(4)).add(i);
            }
        }
        for(int i = 0; i < entities.size(); i++) {
            Entity e1 = entities.get(i);
            if(e1.isSolid()) {
                nears.clear();
                getNears(e1, entities, celula);
                for (int j = 0; j < nears.size(); j++) {
                    repulsion(e1, nears.get(j));
                }
            }
        }
    }

    /**
     * Os vizinhos que se sobrepoem a {@code entity}, NA ORDEM DA LISTA — a mesma
     * ordem de antes da grade, para os empurroes saírem na mesma sequencia.
     *
     * A grade e montada no comeco do tick, e os empurroes mexem nas posicoes na
     * hora; um par que um empurrao anterior acabou de aproximar pode ficar para o
     * tick seguinte, dezesseis milesimos depois. A distancia em si e medida agora,
     * na posicao atual, como sempre foi.
     */
    private void getNears(Entity entity, List<Entity> list, double celula) {
        candidatos.clear();
        int gx = celulaDe(centroX(entity), celula), gy = celulaDe(centroY(entity), celula);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                List<Integer> naCelula = grade.get(chave(gx + dx, gy + dy));
                if (naCelula != null) {
                    candidatos.addAll(naCelula);
                }
            }
        }
        candidatos.sort(null);
        for (int k = 0; k < candidatos.size(); k++) {
            Entity e = list.get(candidatos.get(k));
            double total_radius = (e.getWidth() + entity.getWidth()) / 2d;
            double distance = e.getDistance(entity);
            if(total_radius > distance && e != entity && e.isSolid()) {
                nears.add(e);
            }
        }
    }

    private static double centroX(Entity e) {
        return e.getX() + e.getWidth() / 2d;
    }

    private static double centroY(Entity e) {
        return e.getY() + e.getHeight() / 2d;
    }

    static int celulaDe(double coordenada, double celula) {
        return (int) Math.floor(coordenada / celula);
    }

    static long chave(int gx, int gy) {
        return ((long) gx << 32) ^ (gy & 0xFFFFFFFFL);
    }

    void repulsion(Entity e1, Entity e2) {
        double total_radius = (e1.getWidth() + e2.getWidth()) / 2d;
        double radians = Math.atan2((e2.getY() - e1.getY()), (e2.getX() - e1.getX()));
        double distance = e1.getDistance(e2);
        if(total_radius > distance) {
            double inside = total_radius - distance;
            double rx = Math.cos(radians) * inside;
            double ry = Math.sin(radians) * inside;
            double totalWeight = e1.getPhysical().getWeight() + e2.getPhysical().getWeight();
            double proportion1 = e2.getPhysical().getWeight() / totalWeight;
            double proportion2 = e1.getPhysical().getWeight() / totalWeight;
            e1.getPhysical().moveSystem(-rx * proportion1, -ry * proportion1);
            e2.getPhysical().moveSystem(rx * proportion2, ry * proportion2);
        }
    }

}
