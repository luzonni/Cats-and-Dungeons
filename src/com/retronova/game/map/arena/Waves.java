package com.retronova.game.map.arena;

import com.retronova.engine.Engine;
import com.retronova.engine.io.Resources;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.game.objects.tiles.Void;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Waves implements Runnable {

    private static final int M;

    private static final JSONObject[] controller;

    static {
        M = 60 * 60;
        controller = new JSONObject[3];
        try {
            controller[0] = Resources.getJsonFile("maps/waves", "easy");
            controller[1] = Resources.getJsonFile("maps/waves", "normal");
            controller[2] = Resources.getJsonFile("maps/waves", "hard");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Arena gameMap;

    private final int period;
    private final int[] amount;
    private final EntityIDs[] types;

    private int counter;
    private int lastCounter;

    private volatile int step;
    private volatile List<Entity> listAppend;
    private boolean end;

    public Waves(Arena gameMap, int waveLevel, int difficult) {
        this.gameMap = gameMap;
        JSONArray listWaves = (JSONArray) controller[difficult].get("waves");
        JSONObject wave = (JSONObject) listWaves.get(waveLevel);
        this.period = ((Number)wave.get("period")).intValue();
        this.amount = new int[this.period];
        JSONArray listAmount = (JSONArray) wave.get("amount");
        for(int i = 0; i < amount.length; i++) {
            int currentAmount = ((Number)listAmount.get(i)).intValue();
            this.amount[i] = currentAmount;
        }
        JSONArray listTypes = (JSONArray) wave.get("enemies");
        this.types = new EntityIDs[listTypes.size()];
        EntityIDs[] types = EntityIDs.values();
        for(int i = 0; i < listTypes.size(); i++) {
            String type = String.valueOf(listTypes.get(i));
            for(int j = 0; j < types.length; j++) {
                if(type.equalsIgnoreCase(types[j].name())) {
                    this.types[i] = types[j];
                }
            }
        }
    }

    public int getSeconds() {
        return (int)((this.counter/60d));
    }

    public int currentSpawn() {
        return this.step;
    }

    public int amountSpawns() {
        return this.amount.length;
    }

    public boolean ended() {
        return this.end;
    }

    private void periodic(){
        if(end){
            return;
        }
        counter++;
        if(this.gameMap.enemiesEmpty()) {
            counter+=5;
        }
        if(counter > M){
            counter = 0;
            end = true;
            return;
        }
        if(counter > lastCounter){
            lastCounter += (M/period);
            new Thread(this, "Waves-Thread").start();
        }
    }

    private synchronized List<Entity> listEntity(){
        List<Entity> lista = new ArrayList<>();
        for(int i = 0; i < amount[step]; i++){
            int id = types[Engine.RAND.nextInt(types.length)].ordinal();
            Entity e = Entity.build(id, 0, 0);
            lista.add(e);
        }
        return lista;
    }

    private synchronized List<Entity> spawner(List<Entity> entidades) {
        List<Entity> list = new ArrayList<>();
        while (!entidades.isEmpty()){
            int x = Engine.RAND.nextInt(gameMap.getBounds().width / GameObject.SIZE());
            int y = Engine.RAND.nextInt(gameMap.getBounds().height / GameObject.SIZE());
            Tile tile = gameMap.getTile(x ,y);
            if(!tile.isSolid() && !(tile instanceof Void)){
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
        periodic();
        if(listAppend != null && !listAppend.isEmpty()) {
            Game.getMap().putAll(listAppend);
            listAppend.clear();
            synchronized (this) {
                step++;
            }
        }
    }

    @Override
    public void run() {
        try {
            List<Entity> enemies = listEntity();
            listAppend = spawner(enemies);
        }catch (Exception e) {
            System.err.println("Erro em waves...");
            e.printStackTrace();
        }
    }
}
