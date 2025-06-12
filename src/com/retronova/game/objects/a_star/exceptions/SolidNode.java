package com.retronova.game.objects.a_star.exceptions;

public class SolidNode extends RuntimeException {
    public SolidNode(String message) {
        super(message);
    }
}
