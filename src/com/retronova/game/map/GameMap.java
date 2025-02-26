package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.exceptions.MapFileException;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.tiles.IDs;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.graphics.SpriteSheet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameMap {
    private final Random random = new Random();
    private int width;
    private Rectangle bounds;
    private final Tile[] map;
    private final List<Entity> entities;

    public GameMap() {
        this.map = loadMap();
        this.entities = new ArrayList<>();
        spawnEnemies(10); // spawna inimigos ao carregar o mapa
    }


    private Tile[] loadMap() {
        BufferedImage mapImage = new SpriteSheet("maps", "easy", 1).getSHEET();
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        this.bounds = new Rectangle(width * GameObject.SIZE(), height * GameObject.SIZE());
        int[] rgb = mapImage.getRGB(0, 0, width, height, null, 0, width);
        this.width = width;
        return convertMap(rgb, width, height);
    }

    private Tile[] convertMap(int[] rgb, int width, int height) {
        Tile[] map = new Tile[width * height];
        IDs[] values = IDs.values();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + y * width;
                for (IDs value : values) {
                    if (Color.decode(value.getColor()).getRGB() == rgb[index]) {
                        map[index] = Tile.build(value.ordinal(), x, y);
                    }
                }
            }
        }
        return map;
    }

    public void spawnEnemies(int count) { // pegar os inimigos pelo ID
        for (int i = 0; i < count; i++) {
            int[] spawnPos = getRandomValidPosition();
            if (spawnPos != null) {
                int enemyType = random.nextInt(3) + 1; // 1 = Zombie, 2 = Skeleton, 3 = Slime, pelo ID, SE COLOCAR 0 BUGA QUE È O ID DO PLAYER
                Entity enemy = Entity.build(enemyType, spawnPos[0], spawnPos[1]);
                entities.add(enemy);
                System.out.println("Spawned: " + enemy.getClass().getSimpleName() + " at " + spawnPos[0] + "," + spawnPos[1]);
            } else {
                System.out.println("No valid spawn position found!");
            }
        }
    }

    private int[] getRandomValidPosition() {
        List<int[]> validPositions = new ArrayList<>();
        for (int y = 0; y < bounds.height / GameObject.SIZE(); y++) {
            for (int x = 0; x < bounds.width / GameObject.SIZE(); x++) {
                if (isTileWalkable(x, y)) {
                    validPositions.add(new int[]{x, y});
                }
            }
        }

        System.out.println("Valid positions: " + validPositions.size());
        if (validPositions.isEmpty()) {
            System.out.println("No walkable tiles found!");
            return null;
        }
        return validPositions.get(random.nextInt(validPositions.size()));
    }

    private boolean isTileWalkable(int x, int y) {
        Tile tile = getTile(x, y);
        return tile != null && !tile.isSolid(); // isSolid() verifica se o tile bloqueia o bonecoy
    }

    public Tile[] getMap() {
        return this.map;
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= map.length / width) {
            return null; // Evita erro
        }
        return getMap()[x + y * width];
    }

    public List<Entity> getEntities() {
        return this.entities;
    }

    public Rectangle getBounds() {
        return this.bounds;
    }
}
