package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.exceptions.NotInActivity;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.util.List;

public class Game implements Activity {

    public static final Player[] PLAYERS = new Player[] {
            new Player(0, 0, "cinzento", 0.8, 10, 5),
            new Player(0, 0, "mago", 0.8, 15, 10),
            new Player(0,0, "sortudo", 0.4, 15, 20),
    };

    private Player player;
    public static Camera C;
    private GameMap gameMap;

    private GameUI ui;

    //Teste
    public Game(Player player, GameMap map) {
        this.player = player;
        this.gameMap = map;
        this.gameMap.getEntities().add(player);
        ui = new GameUI();
        Game.C = new Camera(gameMap.getBounds(), 0.25d);
        Game.C.setFollowed(player);
        player.setX(map.getBounds().getWidth()/2);
        player.setY(map.getBounds().getHeight()/2);
    }

    @Override
    public void tick() {
        if(KeyBoard.KeyPressed("ESCAPE")) {
            Engine.pause(ui);
        }
        //tick logic
        List<Entity> entities = gameMap.getEntities();
        /**
         * entities.sort(Entity.Depth) ->
         * Este metodo organizará a lista de objetos de acordo com sua profundidade do eixo Y.
         */
        entities.sort(Entity.Depth);
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            entity.setDepth();
            entity.getPhysical().moment();
            entity.tick();
        }
        Tile[] map = gameMap.getMap();
        for(int i = 0; i < map.length; i++) {
            Tile tile = map[i];
            tile.tick();
        }

        //Atualização da camera, sempre no final!
        C.tick();
    }

    public static GameMap getMap() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.gameMap;
        }
        throw new NotInActivity("Não é possível retornar o mapa pois a activity atual não é o jogo!");
    }

    public static Player getPlayer() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.player;
        }
        throw new NotInActivity("Não é possível retornar o player pois a activity atual não é o jogo!");
    }

    public static Activity getUI() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.ui;
        }
        throw new NotInActivity("Não é possível retornar a UI pois a activity atual não é um jogo");
    }

    @Override
    public void render(Graphics2D g) {
        //Render logic
        renderMap(g);
        renderEntities(g);
    }

    private void renderMap(Graphics2D g) {
        Tile[] map = gameMap.getMap();
        for(int i = 0; i < map.length; i++) {
            map[i].render(g);
        }
    }

    private void renderEntities(Graphics2D g) {
        List<Entity> entities = gameMap.getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            entity.render(g);
        }
    }

    @Override
    public void dispose() {
        //limpar memoria
        System.out.println("Dispose Game");
    }
}
