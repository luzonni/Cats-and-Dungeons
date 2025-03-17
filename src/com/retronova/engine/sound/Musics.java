package com.retronova.engine.sound;

public enum Musics {

    Music1("menu"),
    Music2("room"),
    Music3("fight");

    private final String ResourceName;

    Musics(String resourceName) {
        this.ResourceName = resourceName;
    }

    public String resource() {
        return this.ResourceName;
    }

}
