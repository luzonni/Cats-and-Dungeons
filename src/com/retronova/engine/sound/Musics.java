package com.retronova.engine.sound;

public enum Musics {

    Geral("geral"),
    Menu("menu_principal"),
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
