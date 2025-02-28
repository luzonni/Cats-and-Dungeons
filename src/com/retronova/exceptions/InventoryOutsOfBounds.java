package com.retronova.exceptions;

public class InventoryOutsOfBounds extends RuntimeException {
    public InventoryOutsOfBounds(String message) {
        super(message);
    }
}
