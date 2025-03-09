package com.retronova.game.objects.physical;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Pause;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;
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
        Entity currentEntity = getEntity();
        if(currentEntity == null) {
            checked.clear();
            return;
        }
        List<Entity> nears = getNears(currentEntity);
        for(int i = 0; i < nears.size(); i++) {
            repulsion(currentEntity, nears.get(i));
        }
    }

    private List<Entity> getNears(Entity entity) {
        List<Entity> entities = map.getEntities();
        List<Entity> nears = new ArrayList<>();
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(e.colliding(entity) && e != entity && e.isSolid()) {
                nears.add(e);
            }
        }
        return nears;
    }

    private Entity getEntity() {
        List<Entity> entities = map.getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if(!checked.contains(entity)) {
                checked.add(entity);
                return entity;
            }
        }
        return null;
    }

    void repulsion(Entity e1, Entity e2) {
        double e1_radius = e1.getWidth() / 2d;
        double e2_radius = e2.getWidth() / 2d;
        if(e1.getBounds().intersects(e2.getBounds())) {
            double radians = Math.atan2((e2.getY() - e1.getY()), (e2.getX() - e1.getX()));
            double distance = e1.getDistance(e2);
            if((e1_radius + e2_radius) > distance) {
                double inside = (e1_radius + e2_radius) - distance;
                double rx = Math.cos(radians) * inside/2d;
                double ry = Math.sin(radians) * inside/2d;
                e1.getPhysical().moveSystem(rx*-1, ry*-1);
                e2.getPhysical().moveSystem(rx, ry);
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                long startTime = System.nanoTime();

                cycle();

                long estimatedTime = System.nanoTime() - startTime;
                System.out.printf("Draw runtime: %.4f ms%n", estimatedTime / 1000000d);

                Thread.sleep(1);
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
