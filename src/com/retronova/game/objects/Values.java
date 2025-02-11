package com.retronova.game.objects;

import java.util.HashMap;
import java.util.Map;

public class Values {

    private Map<String, Object> values;

    public Values() {
        values = new HashMap<String, Object>();
    }

    public void addInt(String name, int value) {
        this.values.put(name, value);
    }

    public void setInt(String name, int newValue) {
        this.values.replace(name, newValue);
    }

    public int getInt(String name) {
        return (int)this.values.get(name);
    }

    public void addDouble(String name, double value) {
        this.values.put(name, values);
    }

    public void setDouble(String name, double newValue) {
        this.values.replace(name, newValue);
    }

    public double getDouble(String name) {
        return (double)this.values.get(name);
    }

    public void addBoolean(String name, boolean value) {
        this.values.put(name, value);
    }

    public void setBoolean(String name, boolean newvalue) {
        this.values.replace(name, newvalue);
    }

    public boolean getBoolean(String name) {
        return (boolean)this.values.get(name);
    }

    public String[] getKeys() {
        return values.keySet().toArray(new String[0]);
    }

}
