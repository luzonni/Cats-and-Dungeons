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

    // RITMO DE HORDA, a la Vampire Survivors.
    //
    // Antes cada fatia do JSON nascia de uma vez, e a primeira onda inteira eram
    // cinco zumbis em sessenta segundos: o jogador limpava a tela e ficava
    // esperando. Agora cada fatia vira um fio continuo de pequenos grupos, com mais
    // bichos e mais frageis — morrer rapido e em quantidade e o que da o ritmo.
    // O JSON continua dizendo a PROPORCAO entre ondas; estes numeros dizem o ritmo.

    /** Multiplica a quantidade do JSON. */
    private static final double DENSIDADE = 3.0;
    /** Em quantos grupos cada fatia do JSON e dividida. */
    private static final int GRUPOS_POR_FATIA = 12;
    /** Vida dos bichos de horda; chefe (fatia de um so) fica intacto. */
    private static final double VIDA_HORDA = 0.5;
    /** Anel em volta do gato onde a horda nasce, em tiles: fora da tela, perto. */
    private static final int ANEL_MIN = 8, ANEL_MAX = 13;

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

    /** Em que sala esta onda acontece. Decide a dureza dos bichos. */
    private final int nivel;

    private int counter;
    private int lastCounter;

    private volatile int step;
    /** Quantos grupos ja nasceram nesta onda. step = grupo / GRUPOS_POR_FATIA. */
    private volatile int grupo;
    private volatile List<Entity> listAppend;
    private boolean end;

    public Waves(Arena gameMap, int waveLevel, int difficult) {
        this.gameMap = gameMap;
        this.nivel = waveLevel;
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
            lastCounter += Math.max(1, M / (period * GRUPOS_POR_FATIA));
            new Thread(this, "Waves-Thread").start();
        }
    }

    private synchronized List<Entity> listEntity(){
        List<Entity> lista = new ArrayList<>();
        int fatia = Math.min(step, amount.length - 1);
        boolean chefe = amount[fatia] <= 1;
        int quantos;
        if (chefe) {
            // Fatia de um so e chefe: nasce uma vez, no primeiro grupo, inteiro.
            quantos = grupo % GRUPOS_POR_FATIA == 0 ? amount[fatia] : 0;
        } else {
            quantos = (int) Math.ceil(amount[fatia] * DENSIDADE / GRUPOS_POR_FATIA);
        }
        for(int i = 0; i < quantos; i++){
            int id = types[Engine.RAND.nextInt(types.length)].ordinal();
            Entity e = Entity.build(id, 0, 0);
            // A DUREZA VEM DA SALA, e e aplicada AQUI porque este e o unico lugar
            // que cria inimigo de onda. Espalhar a conta pelas dez classes daria
            // dez lugares para alguem esquecer, e o bicho esquecido nasceria em
            // nivel de fabrica na sala vinte sem nada acusando.
            if (e instanceof com.retronova.game.objects.entities.enemies.Enemy bicho) {
                bicho.escalarPara(this.nivel);
                if (!chefe) {
                    bicho.escalarVida(VIDA_HORDA);
                }
            }
            lista.add(e);
        }
        return lista;
    }

    private synchronized List<Entity> spawner(List<Entity> entidades) {
        List<Entity> list = new ArrayList<>();
        int largura = gameMap.getBounds().width / GameObject.SIZE();
        int altura = gameMap.getBounds().height / GameObject.SIZE();
        int tentativas = 0;
        while (!entidades.isEmpty()){
            int x, y;
            // Primeiro tenta o anel em volta do gato, para a horda chegar de fora
            // da tela e logo; se a arena nao tiver chao ali, cai no sorteio antigo.
            if (tentativas++ < 200 && Game.getPlayer() != null) {
                double ang = Engine.RAND.nextDouble() * Math.PI * 2;
                double dist = ANEL_MIN + Engine.RAND.nextDouble() * (ANEL_MAX - ANEL_MIN);
                x = (int) (Game.getPlayer().getX() / GameObject.SIZE() + Math.cos(ang) * dist);
                y = (int) (Game.getPlayer().getY() / GameObject.SIZE() + Math.sin(ang) * dist);
                if (x < 0 || y < 0 || x >= largura || y >= altura) {
                    continue;
                }
            } else {
                x = Engine.RAND.nextInt(largura);
                y = Engine.RAND.nextInt(altura);
            }
            Tile tile = gameMap.getTile(x ,y);
            if(tile != null && !tile.isSolid() && !(tile instanceof Void)){
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
        // Grupo vazio (o resto de uma fatia de chefe) tambem conta como grupo:
        // senao a contagem empacava e a onda nunca passava para a fatia seguinte.
        if(listAppend != null) {
            Game.getMap().putAll(listAppend);
            listAppend = null;
            synchronized (this) {
                grupo++;
                step = Math.min(grupo / GRUPOS_POR_FATIA, amount.length - 1);
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
