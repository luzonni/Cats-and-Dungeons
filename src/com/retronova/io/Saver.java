package com.retronova.io;

import com.retronova.game.objects.entities.Entity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

public class Saver {

    private JSONObject object;
    private List<Entity> entities;

    public Saver(List<Entity> entities) {
        this.entities = entities;
        this.object = new JSONObject();
        this.object.put("ENTITY", new JSONArray());
    }

    public Saver updateEntities() {
        JSONArray entitiesObject = (JSONArray) this.object.get("ENTITY");
        for(int i = 0; i < entities.size(); i++) {
            JSONArray entity = new JSONArray();
            entity.add(entities.get(i).getID());
            entity.add(entities.get(i).getX());
            entity.add(entities.get(i).getY());
            entitiesObject.add(entity);
        }
        this.object.replace("ENTITY", entitiesObject);
        return this;
    }

    public void save() {
        //Logic to save files
    }

}
