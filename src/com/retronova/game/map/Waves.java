package com.retronova.game.map;

import com.retronova.game.objects.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Waves {

    private int waveNumber = 1;
    private int enemyCount;
    private int enemiesSpawned;
    private boolean waveActive;
    private final GameMap gameMap; // referência ao GameMap
    private final Random random;
    private Timer waveTimer;
    private long lastSpawnTime;
    private long spawnInterval = 2000; // tempo entre spawns em MS (2s)
    private int currentEnemyCount = 3; // inicia com 3 inimigos por wave

    public Waves(GameMap gameMap) {
        this.gameMap = gameMap; // armazena a referência ao GameMap
        this.random = new Random();
        this.lastSpawnTime = System.currentTimeMillis();
        startNextWave();
    }

    public void updateWave() {
        if (System.currentTimeMillis() - lastSpawnTime >= spawnInterval) {
            spawnEnemies(currentEnemyCount);
            lastSpawnTime = System.currentTimeMillis();
        }
    }

    public void startNextWave() {
        waveActive = true;
        enemyCount = 5 + waveNumber * 2; // a cada wave, spawna +2 inimigos, alteravel também.
        enemiesSpawned = 0;

        System.out.println("🚀 Iniciando Wave " + waveNumber + " com " + enemyCount + " inimigos!");

        waveTimer = new Timer();
        scheduleSpawning();
    }

    private void scheduleSpawning() {
        int totalWaveTime = 20 * 1000; // 20 segundos
        int initialSpawnRate = totalWaveTime / enemyCount; // tempo entre spawns, ajustavel também, tudo aqui.

        waveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (enemiesSpawned >= enemyCount) {
                    endWave();
                    return;
                }
                spawnEnemy();
                enemiesSpawned++;
                // aumenta a frequência no final da onda, e vai aumentando, tem que ver se vai ficar dms (últimos 5 segundos)
                if (enemiesSpawned >= enemyCount - 3) {
                    System.out.println("⚠️ Acelerando spawn!");
                    waveTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (enemiesSpawned < enemyCount) {
                                spawnEnemy();
                                enemiesSpawned++;
                            }
                        }
                    }, 1000);
                }
            }
        }, 0, initialSpawnRate);
    }

    private void spawnEnemy() {
        int enemyType = random.nextInt(3) + 1; // 1 = Zombie, 2 = Skeleton, 3 = Slime
        int[] spawnPos = getRandomSpawnLocation();

        if (spawnPos != null) {
            Entity enemy = Entity.build(enemyType, spawnPos[0], spawnPos[1]);
            gameMap.getEntities().add(enemy); // adiciona ao GameMap
            System.out.println("🧟 Spawned: " + enemy.getClass().getSimpleName() + " at " + spawnPos[0] + "," + spawnPos[1]);
        }
    }

    public void spawnEnemies(int count) {
        for (int i = 0; i < count; i++) {
            int[] spawnPos = getRandomSpawnLocation();
            if (spawnPos != null) {
                int enemyType = random.nextInt(3) + 1; // 1 = Zombie, 2 = Skeleton, 3 = Slime
                Entity enemy = Entity.build(enemyType, spawnPos[0], spawnPos[1]);
                gameMap.getEntities().add(enemy); // agora ta usando a instância correta!!!!!!!
                System.out.println("Spawned: " + enemy.getClass().getSimpleName() + " at " + spawnPos[0] + "," + spawnPos[1]);
            } else {
                System.out.println("No valid spawn position found!");
            }
        }
    }

    private int[] getRandomSpawnLocation() {
        int x = random.nextInt(20); // tamanho do mapa, ajustavel, só mexer.
        int y = random.nextInt(20);
        return new int[]{x, y};
    }

    private void endWave() {
        waveActive = false;
        waveTimer.cancel();
        System.out.println("🎉 Wave " + waveNumber + " concluída!");

        waveNumber++;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startNextWave();
            }
        }, 5000); // delay de 5 segundos para a proxima onda
    }

    public List<Entity> getEntities() {
        return gameMap.getEntities(); // return dos inimigos do gamemap
    }
}
