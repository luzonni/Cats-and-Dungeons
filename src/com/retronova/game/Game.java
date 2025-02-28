package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.exceptions.NotInActivity;
import com.retronova.game.hud.HUD;
import com.retronova.game.map.Camera;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.interfaces.Pause;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.inputs.keyboard.KeyBoard;

import java.awt.*;
import java.util.List;

public class Game implements Activity {

    private Player player;
    public static Camera C;
    private final int difficulty;
    private final int indexPlayer;
    private final GameMap map;

    //Este é apenas o menu do pause.
    private Activity inerface;
    //Aqui estara toda a parte de interface do jogo, como barras de vida, slots dos items, etc...
    private HUD hud;

    //Teste
    public Game(int indexPlayer, int difficulty, GameMap map) {
        this.difficulty = difficulty;
        this.indexPlayer = indexPlayer;
        this.map = map;
        Player player = Player.newPlayer(indexPlayer);
        this.player = player;
        this.map.addPlayer(player);

        Game.C = new Camera(this.map.getBounds(), 0.25d);
        Game.C.setFollowed(player);
        this.hud = new HUD(player);
    }

    @Override
    public void tick() {
        if(KeyBoard.KeyPressed("ESCAPE")) {
            Engine.pause(new Pause());
        }
        if(KeyBoard.KeyPressed("E")) {
            Engine.pause(player.getInventory());
        }
        map.getWaves().tick();
        //tick logic
        List<Entity> entities = map.getEntities();
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
        Tile[] map = this.map.getMap();
        for(int i = 0; i < map.length; i++) {
            Tile tile = map[i];
            tile.tick();
        }

        //Atualização da camera, sempre no final!
        C.tick();
        hud.tick();
    }

    public static void reiniciarJogo() {
        Game game = getGame();
        Engine.setActivity(new Game(game.indexPlayer, game.difficulty, new GameMap()));
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

    public static HUD getUI() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.hud;
        }
        throw new NotInActivity("Não é possível retornar a UI pois a activity atual não é um jogo");
    }

    @Override
    public void render(Graphics2D g) {
        //Render logic
        renderMap(g);
        renderEntities(g);
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
        }
    }

    @Override
    public void dispose() {
        //limpar memoria
        System.out.println("Dispose Game");
    }
}
