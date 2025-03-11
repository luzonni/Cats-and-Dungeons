package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.tiles.Tile;

import java.util.ArrayList;
import java.util.List;

public class Waves implements Runnable {

    private Thread thread;

    private int wave = 1;
    private double waveMultiplier = 1;
    private int amount;
    private int counter;
    private int lastCounter;
    private boolean paused;
    private final Arena gameMap;
    volatile List<Entity> listAppend;

    public Waves(Arena gameMap) {
        this.gameMap = gameMap;
    }

    public boolean getPause(){
        return paused;
    }
    public void setPause(boolean paused){
        this.paused = paused;
    }

    public int getWave() {
        return wave;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    private void wavingHandling(){
        if(!paused){
            counter++;
        }
        if(counter > 60 * 60){
            counter = 0;
            wave++;
            paused = true;
            System.out.println("Fim");
        }else if(counter > lastCounter + 3.5 * 60){
            lastCounter = counter;

            amount = (int) (4 * waveMultiplier);
            waveMultiplier += 0.09 + wave * 0.2; // testar balanceamento apos adicionar armas

            thread = new Thread(this);
            thread.start();
        }
    }

    private List<Entity> listEntity(EntityIDs[] types, int amount){
        List<Entity> lista = new ArrayList<>();
        for(int i = 0; i < amount;i++){
            lista.add(Entity.build(types[Engine.RAND.nextInt(types.length)].ordinal(), 0, 0));
        }
        return lista;
    }

    private synchronized List<Entity> spawner(List<Entity> entidades) {
        List<Entity> list = new ArrayList<>();
        while (!entidades.isEmpty()){
            int x = Engine.RAND.nextInt(gameMap.getBounds().width / GameObject.SIZE());
            int y = Engine.RAND.nextInt(gameMap.getBounds().height / GameObject.SIZE());
            Tile tile = gameMap.getTile(x ,y );
            if(!tile.isSolid()){
                Entity e = entidades.getFirst();
                e.setX(x * GameObject.SIZE());
                e.setY(y * GameObject.SIZE());
                list.add(e);
                entidades.remove(e);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return list;
    }

    public void tick() {
        wavingHandling();
        if(listAppend != null && !listAppend.isEmpty()) {
            Game.getMap().putAll(listAppend);
            listAppend.clear();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Contador para adicionar: " + amount);
            //TODO criar sistemas para escolher os inimigos que aparecerão em cada wave!
            EntityIDs[] types = {EntityIDs.Skeleton, EntityIDs.Zombie, EntityIDs.Slime, EntityIDs.MouseVampire, EntityIDs.RatExplode};
            listAppend = spawner(listEntity(types, amount));
        }catch (Exception e) {
            System.err.println("Erro em waves...");
        }
    }
}
