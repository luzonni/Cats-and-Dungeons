package com.retronova.engine;

import kuusisto.tinysound.Sound;
import kuusisto.tinysound.TinySound;
import java.io.File;

public class Sounds {

    public static Sound zombieSound;
    public static kuusisto.tinysound.Music menuMusica;
    private static final double ZOMBIE_SOUND_VOLUME = 0.0;
    private static final double MENU_MUSIC_VOLUME = 0.0;

    public static void init() {
        TinySound.init();
        zombieSound = TinySound.loadSound(new File("res/com/retronova/res/sounds/zombie_sound.wav"));
        menuMusica = TinySound.loadMusic(new File("res/com/retronova/res/sounds/menusom.wav"));

    }


    public static void playMenuMusic() {
        if (menuMusica != null && !menuMusica.playing()) {
            menuMusica.setVolume(MENU_MUSIC_VOLUME);
            menuMusica.play(true);
        } else {
            System.out.println("menuMusic está vazio/nulo");
        }
    }

    public static void resumeMenuMusic() {
        if (menuMusica != null) {
            menuMusica.setVolume(MENU_MUSIC_VOLUME); // Volta pro volume original
        }
    }


    public static void pauseMenuMusic() {
        if (menuMusica != null) {
            menuMusica.setVolume(0.01); // Muta o som, mas a música continua rodando
        }
    }

    public static void playZombieSound() {
        if (zombieSound != null) {
            zombieSound.play(ZOMBIE_SOUND_VOLUME);
        }
    }

    public static void playZombieSoundWithVolume(double volume) {
        if (zombieSound != null) {
            zombieSound.play(volume);
        } else {
            System.out.println("zombieSound está vazio/nulo");
        }
    }

    public static void stopZombieSound() {
        if (zombieSound != null) {
            zombieSound.stop();
        }
    }

    public static void shutdown() {
        System.out.println("Sounds.shutdown()");
        TinySound.shutdown();
    }
}