package com.retronova.game.map;

import com.retronova.engine.io.Resources;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.physical.Physically;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.game.objects.tiles.TileIDs;
import com.retronova.engine.graphics.SpriteSheet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public abstract class GameMap {

    private final String name;
    private int length;
    private Rectangle bounds;

    private List<Entity> entities;

    private List<Particle> particles;
    private Tile[] map;
    private final Physically physically;

    public GameMap(String mapName) {
        this.name = mapName;
        this.entities = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.map = loadMap(mapName);
        this.physically = new Physically(this);
        this.physically.start();
    }

    public void restart() {
        this.entities = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.map = loadMap(name);
    }

    public void addPlayer(Player player) {
        put(player);
        player.setX(getBounds().getWidth()/2);
        player.setY(getBounds().getHeight()/2);
    }

    private Tile[] loadMap(String mapName) {
        BufferedImage mapImage = new SpriteSheet("maps", mapName, 1).getSHEET();
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        this.bounds = new Rectangle(width * GameObject.SIZE(), height * GameObject.SIZE());
        int[] rgb = mapImage.getRGB(0, 0, width, height, null, 0, width);
        this.length = width;
        JSONObject jsonObject = null;
        try {
            jsonObject = Resources.getJsonFile("maps", mapName);
        } catch (IOException ignore) {
            System.err.println("Arquivo: " + mapName);
        }
        if(jsonObject != null && !jsonObject.isEmpty()) {
            List<Entity> list = new ArrayList<>();
            JSONArray arrEntity = (JSONArray) jsonObject.get("entities");
            for(int i = 0; i < arrEntity.size(); i++) {
                JSONObject obj = (JSONObject) arrEntity.get(i);
                int id = ((Number)obj.get("id")).intValue();
                int x = ((Number)obj.get("x")).intValue();
                int y = ((Number)obj.get("y")).intValue();
                JSONArray objValues = (JSONArray)obj.get("values");
                Object[] values = new Object[objValues.size() + 2];
                values[0] = x;
                values[1] = y;
                for(int j = 2; j < values.length; j++) {
                    values[j] = objValues.get(j-2);
                }
                Entity e = Entity.build(id, values);
                list.add(e);
            }
            putAll(list);
        }
        return convertMap(rgb, width, height);
    }

    private Tile[] convertMap(int[] rgb, int width, int height) {
        Tile[] map = new Tile[width * height];
        TileIDs[] values = TileIDs.values();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + y * width;
                for (TileIDs value : values) {
                    if (value.getColor() == rgb[index]) {
                        map[index] = Tile.build(value.ordinal(), x, y);
                    }
                }
            }
        }
        return map;
    }

    public Tile[] getMap() {
        return this.map;
    }

    public Tile getTile(int x, int y) {
        try {
            return getMap()[x + y * length];
        }catch(IndexOutOfBoundsException ignore) {

        }
        try {
            x /= GameObject.SIZE();
            y /= GameObject.SIZE();
            return getMap()[x + y * length];
        }catch (IndexOutOfBoundsException ignore) {

        }
        return Tile.build(TileIDs.Void.ordinal());
    }

    public void depth() {
        entities.sort(Entity.Depth);
        particles.sort(Particle.Depth);
    }

    public List<Entity> getEntities() {
        return List.copyOf(this.entities);
    }

    public <T extends Entity> List<T> getEntities(Class<T> type) {
        List<T> list = new ArrayList<>();
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(type.isInstance(e)) {
                list.add((T) e);
            }
        }
        return list;
    }

    public List<Particle> getParticles() {
        if (particles == null) {
            this.particles = new ArrayList<>(); // evita erro de ponteiro
        }
        return List.copyOf(this.particles);
    }

    public void put(Entity e) {
        entities.add(e);
    }

    public void putAll(List<Entity> e) {
        this.entities.addAll(e);
    }

    public boolean put(Particle p) {
        return this.particles.add(p);
    }

    public void remove(Entity e) {
        this.entities.remove(e);
    }

    public void remove(Particle p) {
        this.particles.remove(p);
    }

    public abstract void tick();

    public Rectangle getBounds() {
        return this.bounds;
    }

    public void dispose() {
        this.physically.dispose();
    }

}
