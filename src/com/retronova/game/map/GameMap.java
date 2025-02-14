package com.retronova.game.map;

import com.retronova.exceptions.MapFileException;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.tiles.IDs;
import com.retronova.game.objects.tiles.Tile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameMap {

    private int width;
    private final Tile[] map;
    private final List<Entity> entities;

    public GameMap(File map) {
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
        int[] rgb = mapImage.getRGB(0, 0, width, height, null, 0, width);
        this.width = width;
        this.map = createMap(rgb, width, height);
        this.entities = new ArrayList<>();
    }

    // O(N³)
    private Tile[] createMap(int[] rgb, int width, int height) {
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

    public Tile[] getMap() {
        return this.map;
    }

    public Tile getTile(int x, int y) {
        return getMap()[x + y * width];
    }

    public List<Entity> getEntities() {
        return this.entities;
    }
}
