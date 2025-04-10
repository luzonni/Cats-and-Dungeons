package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.NotInActivity;
import com.retronova.engine.exceptions.NotInMap;
import com.retronova.engine.graphics.Galaxy;
import com.retronova.game.hud.HUD;
import com.retronova.game.interfaces.Inter;
import com.retronova.game.interfaces.status.Status;
import com.retronova.game.map.*;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.arena.Waves;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.Bomb;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.physical.Physically;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.menus.GameOver;
import com.retronova.menus.Pause;

import java.awt.*;
import java.util.List;

public class Game implements Activity {

    private final Galaxy galaxy;

    private final int indexPlayer;

    private long seconds;
    private int count;

    private final Player player;
    public static Camera C;

    private GameMap map;
    private int level;
    private int difficult;

    private final HUD hud;
    private final Inter inter;

    //Teste
    public Game(int indexPlayer, GameMap map) {
        this.indexPlayer = indexPlayer;
        this.inter = new Inter();
        Player player = Player.newPlayer(indexPlayer);
        this.inter.put("inventory", player.getInventory());
        this.inter.put("status", new Status(player));
        this.player = player;
        this.changeMap(map);
        this.hud = new HUD(player);
        this.galaxy = new Galaxy();
    }

    public int getLevel() {
        return this.level;
    }

    public void plusLevel() {
        this.level++;
    }

    public int getDifficult() {
        return this.difficult;
    }

    public void setDifficult(int difficult) {
        this.difficult = difficult;
    }

    public long getSeconds() {
        return this.seconds;
    }

    private void gameOver() {
        Engine.backActivity();
        Engine.heapActivity(new GameOver(player));
    }

    public void changeMap(GameMap newMap) {
        if(newMap == null) {
            return;
        }
        if(this.map != null) {
            this.map.remove(player);
            this.map.dispose();
        }
        this.map = newMap;
        this.map.addPlayer(player);
        Game.C = new Camera(this.map.getBounds(), 0.25d);
        Game.C.setFollowed(player);
    }

    @Override
    public void tick() {
        count++;
        if(count > 60) {
            count = 0;
            seconds++;
            System.out.println("Seconds: " + seconds);
        }
        hud.tick();
        galaxy.tick();
        if(KeyBoard.KeyPressed("ESCAPE")) {
            Engine.pause(new Pause(this));
        }
        if(KeyBoard.KeyPressed("E")) {
            inter.open();
        }
        map.tick();
        map.depth();
        List<Entity> entities = map.getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            entity.tick();
            entity.tickEntityEffects();
            Tile tile = map.getTile((int) entity.getX() + entity.getWidth() / 2, (int) entity.getY() + entity.getHeight());
            tile.effect(entity);
        }
        List<Particle> particles = map.getParticles();
        for(int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            p.tick();
            p.setDepth();
        }
        Tile[] map = this.map.getMap();
        for(int i = 0; i < map.length; i++) {
            Tile tile = map[i];
            tile.tick();
        }
        C.tick();
        if(!this.map.getEntities().contains(player)) {
            gameOver();
        }
    }

    @Override
    public void render(Graphics2D g) {
        galaxy.render(g);
        //Render logic
        renderMap(g);
        renderEntities(g);
        renderParticles(g);
        hud.render(g);
    }

    private void renderMap(Graphics2D g) {
        Tile[] map = this.map.getMap();
        for(int i = 0; i < map.length; i++) {
            map[i].render(g);
        }
    }

    private void renderEntities(Graphics2D g) {
        List<Entity> entities = map.getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            entity.render(g);
            if(entity instanceof Enemy enemy) {
                enemy.renderLife(g);
            }
        }
    }

    private void renderParticles(Graphics2D g) {
        List<Particle> particles = map.getParticles();
        for(int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            p.render(g);
        }
    }

    @Override
    public void dispose() {
        System.out.println("Dispose Game");
        map.dispose();
        inter.dispose();
    }

    public static void focus(GameObject obj) {
        Game.C.setFollowed(obj);
    }

    public static void restart() {
        Game.getInter().dispose();
        GameMap map = getMap();
        Game game = getGame();
        map.restart();
        Engine.backActivity();
        Engine.heapActivity(new Game(game.indexPlayer, map));
    }

    public static Game getGame() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game;
        }
        throw new NotInActivity("Não é possível retornar o mapa pois a activity atual não é o jogo!");
    }

    public static GameMap getMap() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.map;
        }
        throw new NotInActivity("Não é possível retornar o mapa pois a activity atual não é o jogo!");
    }

    public static Player getPlayer() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.player;
        }
        throw new NotInActivity("Não é possível retornar o player pois a activity atual não é o jogo!");
    }

    public static Waves getWave(){
        if(getMap() instanceof Arena arena) {
            return arena.getWaves();
        }
        throw new NotInMap("O mapa atual não é uma Arena!");
    }

    public static Arena getArena() {
        if(getMap() instanceof Arena arena) {
            return arena;
        }
        throw new NotInMap("O mapa atual não é uma room!");
    }

    public static Room getRoom() {
        if(getMap() instanceof Room room) {
            return room;
        }
        throw new NotInMap("O mapa atual não é uma room!");
    }

    public static Inter getInter() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.inter;
        }
        throw new NotInActivity("Não é possível retornar a UI pois a activity atual não é um jogo");
    }

}
