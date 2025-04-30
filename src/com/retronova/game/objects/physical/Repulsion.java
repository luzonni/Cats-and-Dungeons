package com.retronova.game.objects.physical;

import com.retronova.engine.Configs;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;

import java.util.ArrayList;
import java.util.List;

public class Repulsion implements Runnable {

    private final GameMap map;
    private final Thread thread;
    private boolean running;

    public Repulsion(GameMap map) {
        this.map = map;
        thread = new Thread(this, "Repulsion-Thread");
    }

    public void start() {
        this.running = true;
        this.thread.start();
    }

    public boolean isRunning() {
        return this.running;
    }

    private void cycle() {
        List<Entity> entities = new ArrayList<>(List.copyOf(map.getEntities()));
        for(int i = 0; i < entities.size(); i++) {
            Entity e1 = entities.get(i);
            if(e1.isSolid()) {
                List<Entity> nears = getNears(e1, entities);
                for (int j = 0; j < nears.size(); j++) {
                    Entity e2 = nears.get(j);
                    repulsion(e1, e2);
                }
            }
        }
    }

    private List<Entity> getNears(Entity entity, List<Entity> list) {
        List<Entity> nears = new ArrayList<>();
        for(int i = 0; i < list.size(); i++) {
            Entity e = list.get(i);
            double total_radius = (e.getWidth() + entity.getWidth()) / 2d;
            double distance = e.getDistance(entity);
            if(total_radius > distance && e != entity && e.isSolid()) {
                nears.add(e);
            }
        }
        return nears;
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

    @Override
    public void run() {
        while (running) {
            try {
                synchronized (this) {
                    this.wait();
                }
                cycle();
            }catch (Exception e) {
                String err = "Bem... esse erro não vai afetar em NADA o jogo, mas é um erro ;( " +
                        "\nO que fazer? Acho que chorar, porque resolver isso é desnecessário e chato, então chore, faz bem!";
                System.err.println(err);
            }
        }
    }

    public void dispose() {
        running = false;
    }

}
