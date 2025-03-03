package com.retronova.engine.sound;

public enum Musics {

    Music1("menusom");

    private final String ResourceName;

    Musics(String resourceName) {
        this.ResourceName = resourceName;
    }

    public String resource() {
        return this.ResourceName;
    }

}
