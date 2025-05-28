package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.io.Resources;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.physical.Repulsion;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.game.objects.tiles.TileIDs;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.tiles.Void;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public abstract class GameMap {

    private int length;
    private Rectangle bounds;

    private List<Entity> entities;

    private List<Particle> particles;
    private Tile[] map;

    private final Repulsion repulsion;

    public GameMap(String mapName) {
        this.entities = new ArrayList<>();
        this.particles = new ArrayList<>();
        loadMap(mapName);
        this.repulsion = new Repulsion(this);
        this.repulsion.start();
    }

    public void addPlayer(Player player) {
        put(player);
        player.setX(getBounds().getWidth()/2);
        player.setY(getBounds().getHeight()/2);
    }

    private void loadMap(String mapName) {
        BufferedImage mapImage = new SpriteHandler("maps", mapName, 1).getSHEET();
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        this.bounds = new Rectangle(width * GameObject.SIZE(), height * GameObject.SIZE());
        int[] rgb = mapImage.getRGB(0, 0, width, height, null, 0, width);
        this.map = convertMap(rgb, width, height);
        this.length = width;
        JSONObject jsonObject = null;
        try {
            jsonObject = Resources.getJsonFile("maps", mapName);
        } catch (IOException ignore) {
            System.err.println("Arquivo: " + mapName);
        }
        if(jsonObject != null && !jsonObject.isEmpty()) {
            loadEntities(width, height, jsonObject);
        }
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

    private void loadEntities(int width, int height, JSONObject jsonObject) {
        List<Entity> list = new ArrayList<>();
        JSONArray arrEntity = (JSONArray) jsonObject.get("entities");
        for(int i = 0; i < arrEntity.size(); i++) {
            JSONObject obj = (JSONObject) arrEntity.get(i);
            String name = (String)obj.get("name");
            EntityIDs[] entityIDs = EntityIDs.values();
            if(obj.containsKey("chanceToAppear")) {
                double chance = ((Number)obj.get("chanceToAppear")).doubleValue() * 100;
                if(Engine.RAND.nextInt(100) > chance) {
                    continue;
                }
            }
            int id = -1;
            for(int j = 0; j < entityIDs.length; j++) {
                if(entityIDs[j].name().equalsIgnoreCase(name)) {
                    id = entityIDs[j].ordinal();
                    break;
                }
            }
            if(id == -1) {
                throw new EntityNotFound("Erro ao colocar a entidade ao mapa.");
            }
            int x = -1;
            int y = -1;
            if(obj.containsKey("position")) {
               String type = ((String)obj.get("position"));
               if(type.equalsIgnoreCase("Random")) {
                   Tile tile;
                   do {
                       x = Engine.RAND.nextInt(width);
                       y = Engine.RAND.nextInt(height);
                       tile = this.map[x + y * this.length];
                   } while (tile.isSolid() || (tile instanceof Void));
               }else if(type.equalsIgnoreCase("Center")) {
                   x = width/2;
                   y = height/2;
               }
            }else {
                x = ((Number)obj.get("x")).intValue();
                y = ((Number)obj.get("y")).intValue();
            }
            if(x == -1 || y == -1) {
                throw new EntityNotFound("Erro ao colocar a entidade ao mapa.");
            }
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

    public Repulsion getRepulsion() {
        return this.repulsion;
    }

    public Tile[] getMap() {
        return this.map;
    }

    public Tile getTile(int x, int y) {
        try {
            return getMap()[x + y * length];
        }catch(IndexOutOfBoundsException ignore) { }
        try {
            x /= GameObject.SIZE();
            y /= GameObject.SIZE();
            return getMap()[x + y * length];
        }catch (IndexOutOfBoundsException ignore) { }
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
        this.repulsion.dispose();
    }


    public static boolean mouseOnRect(Rectangle bounds) {
        int mouseX = Mouse.getX();
        int mouseY = Mouse.getY();

        Point2D.Float screenPoint = new Point2D.Float(mouseX, mouseY);
        Point2D.Float worldPoint = new Point2D.Float();

        try {

            AffineTransform inverse = Camera.getAt().createInverse(); // Inverso da transformação usada no render
            inverse.transform(screenPoint, worldPoint);
        } catch (NoninvertibleTransformException ex) {
            ex.printStackTrace();
        }

        return bounds.contains(worldPoint.x, worldPoint.y);
    }

    public static boolean clickOnRect(Mouse_Button button, Rectangle rec) {
        return mouseOnRect(rec) && Mouse.click(button);
    }

    public static boolean pressingOnRect(Mouse_Button button, Rectangle rec) {
        return mouseOnRect(rec) && Mouse.pressing(button);
    }

}
