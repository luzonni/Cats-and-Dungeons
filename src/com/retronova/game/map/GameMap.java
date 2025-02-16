package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.exceptions.MapFileException;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.tiles.IDs;
import com.retronova.game.objects.tiles.Tile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GameMap {

    private int width;
    private Rectangle bounds;
    private final Tile[] map;
    private final List<Entity> entities;

    public GameMap(File map) {
        this.map = loadMap(map);
        this.entities = loadEntities(map);
    }

    private Tile[] loadMap(File map) {
        BufferedImage mapImage;
        try {
            String path = map.getAbsolutePath() + "/mapImage.png";
            System.out.println(path);
            mapImage = ImageIO.read(new File(path));
        } catch (IOException e) {
            throw new MapFileException(e.getMessage());
        }
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        this.bounds = new Rectangle(width * GameObject.SIZE(), height * GameObject.SIZE());
        int[] rgb = mapImage.getRGB(0, 0, width, height, null, 0, width);
        this.width = width;
        return convertMap(rgb, width, height);
    }

    // O(N³)
    private Tile[] convertMap(int[] rgb, int width, int height) {
        Tile[] map = new Tile[width*height];
        IDs[] values = IDs.values();
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int index = x + y * width;
                for(int i = 0; i < values.length; i++) {
                    if(Color.decode(values[i].getColor()).getRGB() == rgb[index]) {
                        map[index] = Tile.build(values[i].ordinal(), x, y);
                    }
                }
            }
        }
        return map;
    }

    private List<Entity> loadEntities(File map) {
        List<Entity> entities = new ArrayList<>();
        JSONObject mapEntities;
        try {
            String path = map.getAbsolutePath() + "/entities.json";
            InputStream stream = new FileInputStream(path);
            Reader isr = new InputStreamReader(stream);
            JSONParser parse = new JSONParser();
            parse.reset();
            mapEntities = (JSONObject) parse.parse(isr);
        }catch (IOException | ParseException e) {
            throw new MapFileException("Entity file cannot read");
        }
        JSONArray arr = (JSONArray) mapEntities.get("Entities");
        for(int i = 0; i < arr.size(); i++) {
            JSONArray entity = (JSONArray) arr.get(i);
            int id = ((Number)entity.get(0)).intValue();
            int x = ((Number)entity.get(1)).intValue();
            int y = ((Number)entity.get(2)).intValue();
            entities.add(Entity.build(id, x, y));
        }
        return entities;
    }

    public Tile[] getMap() {
        return this.map;
    }

    public Tile getTile(int x, int y) {
        return getMap()[x + y * width];
    }

    public List<Entity> getEntities() {
        return this.entities;
    }

    public Rectangle getBounds() {
        return this.bounds;
    }

    public static void main(String[] args) {
        File file = new File(args[0]);
        File imgFile = new File(file.getAbsolutePath() + "/mapImage.png");
        BufferedImage mapImage;
        try {
            mapImage = ImageIO.read(imgFile);
        } catch (IOException e) {
            System.err.println("Erro no diretorio do mapa");
            return;
        }
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        JSONObject object = new JSONObject();
        JSONArray entitiesArr = new JSONArray();
        JSONArray player = new JSONArray();
        player.add(0);
        player.add(width/2);
        player.add(height/2);
        entitiesArr.add(player);

        for(int i = 0; i < 25; i++) {
            JSONArray zombie = new JSONArray();
            zombie.add(1);
            zombie.add(1 + Engine.RAND.nextInt(width-2));
            zombie.add(1 + Engine.RAND.nextInt(height-2));
            entitiesArr.add(zombie);
        }

        object.put("Entities", entitiesArr);
        try {
            String l = args[0] + "/" + "entities.json";
            FileWriter writer = new FileWriter(l);
            writer.write(object.toJSONString());
            writer.close();
        } catch (IOException e) {
            throw new MapFileException("Erro ao salvar as entidades do: " + args[0]);
        }
    }

}
