package com.retronova.engine.sound;

public enum Musics {

    Menu("menu"),
    Room("room"),
    Fight("fight"),
    GameOver("game_over");

    private final String ResourceName;

    Musics(String resourceName) {
        this.ResourceName = resourceName;
    }

    public String resource() {
        return this.ResourceName;
    }

}
