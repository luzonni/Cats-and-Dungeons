package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.tiles.Tile;

import java.util.ArrayList;
import java.util.List;

public class Waves {

    private int wave = 20;
    private double waveMultiplier = 1;
    private int counter;
    private int lastCounter;
    private boolean paused;
    private final GameMap gameMap;

    public Waves(GameMap gameMap) {
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
            //TODO criar sistemas para excolher os inimigos que aparecerão em cada wave!
            EntityIDs[] types = {EntityIDs.Zombie, EntityIDs.Skeleton, EntityIDs.Slime, EntityIDs.RatExplode};
            int amount = (int) (4 * waveMultiplier);
            System.out.println("Contador para adicionar: " + amount);
            waveMultiplier += 0.09 + wave * 0.2; // testar balenceamento apos adicionar armas
            new Thread(() -> {
                spawner(listEntity(types, amount));
            }).start();
        }
    }

    private List<Entity> listEntity(EntityIDs[] types, int amount){
        List<Entity> lista = new ArrayList<>();
        for(int i = 0; i < amount;i++){
            lista.add(Entity.build(types[Engine.RAND.nextInt(types.length)].ordinal(), 0, 0));
        }
        return lista;
    }

    private void spawner(List<Entity> entidades) {
        while (!entidades.isEmpty()){
            int x = Engine.RAND.nextInt(gameMap.getBounds().width / GameObject.SIZE());
            int y = Engine.RAND.nextInt(gameMap.getBounds().height / GameObject.SIZE());
            Tile tile = gameMap.getTile(x ,y );
            if(!tile.isSolid()){
                Entity e = entidades.getFirst();
                e.setX(x * GameObject.SIZE());
                e.setY(y * GameObject.SIZE());
                gameMap.getEntities().add(e);
                entidades.remove(e);
            }
        }
    }

    public void tick() {
        wavingHandling();
    }
}
