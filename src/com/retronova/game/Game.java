package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.io.File;
import java.util.List;

public class Game implements Activity {

    public static Camera C;

    private static GameMap gameMap;

    //Teste

    public Game() {
        Game.C = new Camera();
        File playground = new File("maps/playground/");
        gameMap = new GameMap(playground);
        gameMap.getEntities().add(Entity.build(0, 0, 0));
    }

    @Override
    public void tick() {
        //tick logic
        List<Entity> entities = gameMap.getEntities();
        /**
         * entities.sort(Entity.Depth) ->
         * Este metodo organizará a lista de objetos de acordo com sua profundidade do eixo Y.
         */
        entities.sort(Entity.Depth);
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            entity.tick();
            if(entity instanceof Player player) {
                C.setX((int)player.getX() - Engine.window.getWidth()/2);
                C.setY((int)player.getY() - Engine.window.getHeight()/2);
            }
        }
        Tile[] map = gameMap.getMap();
        for(int i = 0; i < map.length; i++) {
            Tile tile = map[i];
            tile.tick();
        }
    }

    public static GameMap getMap() {
        return Game.gameMap;
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
        Game.gameMap = null;
    }

}
