package com.retronova.game.objects.physical;

import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;

import java.util.ArrayList;
import java.util.List;

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

    public void tick() {
        List<Entity> entities = map.getEntities();
        List<Entity> nears = new ArrayList<>();
        for(int i = 0; i < entities.size(); i++) {
            Entity e1 = entities.get(i);
            if(e1.isSolid()) {
                nears.clear();
                getNears(e1, entities, nears);
                for (int j = 0; j < nears.size(); j++) {
                    repulsion(e1, nears.get(j));
                }
            }
        }
    }

    private void getNears(Entity entity, List<Entity> list, List<Entity> nears) {
        for(int i = 0; i < list.size(); i++) {
            Entity e = list.get(i);
            double total_radius = (e.getWidth() + entity.getWidth()) / 2d;
            double distance = e.getDistance(entity);
            if(total_radius > distance && e != entity && e.isSolid()) {
                nears.add(e);
            }
        }
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
