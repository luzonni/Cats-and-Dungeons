package com.retronova.game.objects.physical;

import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;

public class Physically implements Runnable {

    private final GameMap map;
    private Thread thread;
    private boolean running;

    public Physically(GameMap map) {
        this.map = map;
        thread = new Thread(this);
    }

    public void start() {
        this.running = true;
        this.thread.start();
    }

    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void run() {
        while (running) {
            for(int i = 0; i < map.getEntities().size(); i++) {
                Entity e = map.getEntities().get(i);
                e.getPhysical().repulsion();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void dispose() {
        running = false;
    }

}
