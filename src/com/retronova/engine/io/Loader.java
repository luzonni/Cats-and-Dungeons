package com.retronova.engine.io;

import com.retronova.game.objects.entities.Entity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class Loader {

    private JSONObject object;

    public Loader(JSONObject object) {
        this.object = object;
    }

    public List<Entity> loadEntities() {
        List<Entity> entities = new ArrayList<>();
        JSONArray arr = (JSONArray) object.get("ENTITY");
        for(int i = 0; i < arr.size(); i++) {
            JSONArray entity = (JSONArray) arr.get(i);
            int ID = ((Number)entity.get(0)).intValue();
            double X = ((Number)entity.get(1)).doubleValue();
            double Y = ((Number)entity.get(2)).doubleValue();
            entities.add(Entity.build(ID, X, Y));
        }
        return entities;
    }

}
