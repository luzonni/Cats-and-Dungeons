package com.retronova.game.map;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class Waves {
    private int mapWidth;
    private int mapHeight;
    private boolean[][] collisionMap;
    private Random random;

    public Waves(String mapPath) {
        Waves waves = new Waves("res/maps/easy.png");
        try {
            BufferedImage mapImage = ImageIO.read(new File(mapPath));
            this.mapWidth = mapImage.getWidth();
            this.mapHeight = mapImage.getHeight();
            this.collisionMap = new boolean[mapWidth][mapHeight];
            this.random = new Random();

            loadCollisionMap(mapImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCollisionMap(BufferedImage mapImage) {
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int pixel = mapImage.getRGB(x, y);
                collisionMap[x][y] = isWalkable(pixel);
            }
        }
    }

    private boolean isWalkable(int pixel) {
        return ((pixel >> 24) & 0xFF) != 0; // garante de 0 a 255 bit, o valor do pixel.
    }

    public int[] getRandomSpawnLocation() {
        // percorre todas as posições (x, y) do mapa.
        List<int[]> validPositions = new ArrayList<>();

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (collisionMap[x][y]) {
                    validPositions.add(new int[]{x, y});
                }
            }
        }

        return validPositions.isEmpty() ? null : validPositions.get(random.nextInt(validPositions.size())); // posição aleatória escolhida
    }
}
