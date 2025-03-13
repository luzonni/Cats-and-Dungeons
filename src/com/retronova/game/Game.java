package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.NotInActivity;
import com.retronova.engine.exceptions.NotInMap;
import com.retronova.engine.graphics.Galaxy;
import com.retronova.game.hud.HUD;
import com.retronova.game.interfaces.Pause;
import com.retronova.game.map.*;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.physical.Physically;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.engine.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.util.List;

public class Game implements Activity {

    private final Galaxy galaxy;

    private final int difficulty;
    private final int indexPlayer;

    private final Player player;
    public static Camera C;

    private GameMap map;
    private Physically physically;

    private final HUD hud;

    //Teste
    public Game(int indexPlayer, int difficulty, GameMap map) {
        this.difficulty = difficulty;
        this.indexPlayer = indexPlayer;
        Player player = Player.newPlayer(indexPlayer);
        this.player = player;
        this.changeMap(map);
        this.map.addPlayer(player);
        this.hud = new HUD(player);
        galaxy = new Galaxy();
    }

    public void changeMap(GameMap newMap) {
        if (this.physically != null) {
            this.physically.dispose();
        }

        if (newMap == null) {
            this.map = new Room("beginning");
            this.map.addPlayer(player);
        } else {
            this.map = newMap;
        }

        Game.C = new Camera(this.map.getBounds(), 0.25d);
        Game.C.setFollowed(player);
        this.physically = new Physically(map);
    }

    @Override
    public void tick() {
        hud.tick();
        galaxy.tick();
        if(KeyBoard.KeyPressed("ESCAPE")) {
            Engine.pause(new Pause(this));
        }
        if(KeyBoard.KeyPressed("E")) {
            Engine.pause(player.getInventory());
        }
        if(!physically.isRunning()) {
            physically.start();
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
        //limpar memoria
        System.out.println("Dispose Game");
        physically.dispose();
    }

    public static void restart() {
        Game game = getGame();
        GameMap map = getMap();
        map.restart();
        Engine.setActivity(new Game(game.indexPlayer, game.difficulty, map));
        //TODO tirar saídas de console após finalização da lógica.
        System.out.println("Jogo reiniciado com personagem " + game.indexPlayer + " e dificuldade " + game.difficulty);
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

    public static Room getRoom() {
        if(getMap() instanceof Room room) {
            return room;
        }
        throw new NotInMap("O mapa atual não é uma room!");
    }

    public static HUD getHUD() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.hud;
        }
        throw new NotInActivity("Não é possível retornar a UI pois a activity atual não é um jogo");
    }

}
