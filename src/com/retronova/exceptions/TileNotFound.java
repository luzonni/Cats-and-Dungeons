package com.retronova.exceptions;

public class TileNotFound extends RuntimeException {
    public TileNotFound(String message) {
        super(message);
    }
}
