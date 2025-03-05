package com.retronova.engine.exceptions;

public class InventoryOutsOfBounds extends RuntimeException {
    public InventoryOutsOfBounds(String message) {
        super(message);
    }
}
