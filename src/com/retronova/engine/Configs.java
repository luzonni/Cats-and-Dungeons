package com.retronova.engine;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Configs {

    private static Map<String, Object> VALUES;

    public static void init() {
        VALUES = new HashMap<>();
        VALUES.put("vignette", true);
        VALUES.put("fullscreen", false);
        VALUES.put("SCALE", 4);
        VALUES.put("UISCALE", 3);
        VALUES.put("HUDSCALE", 4);
        VALUES.put("MARGIN", 20);
        VALUES.put("NeatGraphics", false);
        VALUES.put("VOLUM", 20);
        VALUES.put("MUSIC", 20);
        VALUES.put("MaxFrames", 60);
    }

    public static void load() {
        String path = "config.json";
        File file = new File(path);
        if(!file.exists()) {
            update();
            return;
        }
        JSONObject object = null;
        try {
            InputStream istream = new FileInputStream(file);
            Reader isr = new InputStreamReader(istream);
            JSONParser parse = new JSONParser();
            parse.reset();
            object = (JSONObject) parse.parse(isr);
        }catch (Exception e) {}
        try {
            String[] keys = VALUES.keySet().toArray(new String[0]);
            for(int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if(object != null && object.containsKey(key)) {
                    VALUES.replace(key, object.get(key));
                }
            }
        } catch (Exception e) {
            file.delete();
        }
    }

    public static void update() {
        String path = "config.json";
        JSONObject object = new JSONObject();
        String[] keys = VALUES.keySet().toArray(new String[0]);
        for(int i = 0; i < keys.length; i++) {
            String key = keys[i];
            object.put(key, VALUES.get(key));
        }
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(object.toJSONString());
            writer.close();
        } catch (IOException ignore) { }
    }

    public static boolean isVignette() {
        return (boolean) VALUES.get("vignette");
    }

    public static boolean isFullscreen() {
        return (boolean) VALUES.get("fullscreen");
    }

    public static void setFullscreen(boolean fullscreen) {
        VALUES.replace("fullscreen", fullscreen);
    }

    public static int getSCALE() {
        return ((Number)VALUES.get("SCALE")).intValue();
    }

    public static int getUISCALE() {
        return ((Number)VALUES.get("UISCALE")).intValue();
    }

    public static int getHUDSCALE() {
        return ((Number)VALUES.get("HUDSCALE")).intValue();
    }

    public static int getMARGIN() {
        return ((Number)VALUES.get("MARGIN")).intValue();
    }

    public static boolean isNeatGraphics() {
        return (boolean) VALUES.get("NeatGraphics");
    }

    public static int getVOLUM() {
        return ((Number)VALUES.get("VOLUM")).intValue();
    }

    public static int getMUSIC() {
        return ((Number)VALUES.get("MUSIC")).intValue();
    }

    public static int getMaxFrames() {
        return ((Number)VALUES.get("MaxFrames")).intValue();
    }

}
