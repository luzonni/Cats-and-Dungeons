package com.retronova.game.objects.physical;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Pause;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.furniture.Furniture;
import com.retronova.menus.Menu;

import java.util.ArrayList;
import java.util.List;

public class Physically implements Runnable {

    private final List<Entity> checked;
    private final GameMap map;
    private final Thread thread;
    private boolean running;

    public Physically(GameMap map) {
        this.map = map;
        checked = new ArrayList<>();
        thread = new Thread(this);
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
            if(e.colliding(entity) && e != entity && e.isSolid()) {
                nears.add(e);
            }
        }
        return nears;
    }

    void repulsion(Entity e1, Entity e2) {
        double e1_radius = e1.getWidth() / 2d;
        double e2_radius = e2.getWidth() / 2d;
        if(e1.getBounds().intersects(e2.getBounds())) {
            double radians = Math.atan2((e2.getY() - e1.getY()), (e2.getX() - e1.getX()));
            double distance = e1.getDistance(e2);
            if((e1_radius + e2_radius) > distance) {
                double inside = (e1_radius + e2_radius) - distance;
                double rx = Math.cos(radians) * inside / 2d;
                double ry = Math.sin(radians) * inside / 2d;
                e1.getPhysical().moveSystem(rx * -1, ry * -1);
                e2.getPhysical().moveSystem(rx, ry);
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                cycle();
                Thread.sleep(5);
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
