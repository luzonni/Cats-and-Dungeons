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

    public static boolean Vignette() {
        return (boolean) VALUES.get("vignette");
    }

    public static void setVignette(boolean vignette) {
        VALUES.replace("vignette", vignette);
    }

    public static boolean Fullscreen() {
        return (boolean) VALUES.get("fullscreen");
    }

    public static void setFullscreen(boolean fullscreen) {
        VALUES.replace("fullscreen", fullscreen);
    }

    public static int GameScale() {
        return ((Number)VALUES.get("SCALE")).intValue();
    }

    public static void setGameScale(int scale) {
        VALUES.replace("SCALE", scale);
    }


    public static int UiScale() {
        return ((Number)VALUES.get("UISCALE")).intValue();
    }

    public static void setUiScale(int uiScale) {
        VALUES.replace("UISCALE", uiScale);
        update();
    }

    public static int HudScale() {
        return ((Number)VALUES.get("HUDSCALE")).intValue();
    }

    public static void setHudScale(int HUDSCALE) {
        VALUES.replace("HUDSCALE", HUDSCALE);
    }

    public static int Margin() {
        return ((Number)VALUES.get("MARGIN")).intValue();
    }

    public static void setMargin(int MARGIN) {
        VALUES.replace("MARGIN", MARGIN);
        update();
    }

    public static boolean isNeatGraphics() {
        return (boolean) VALUES.get("NeatGraphics");
    }

    public static void setNeatGraphics(boolean NeatGraphics) {
        VALUES.replace("NeatGraphics", NeatGraphics);
    }

    public static int Volum() {
        return ((Number)VALUES.get("VOLUM")).intValue();
    }

    public static void setVolum(int VOLUM) {
        VALUES.replace("VOLUM", VOLUM);
        update();
    }

    public static int Music() {
        return ((Number)VALUES.get("MUSIC")).intValue();
    }

    public static void setMusic(int MUSIC) {
        VALUES.replace("MUSIC", MUSIC);
        update();
    }

    public static int MaxFrames() {
        return ((Number)VALUES.get("MaxFrames")).intValue();
    }

    public static void setMaxFrames(int MaxFrames) {
        VALUES.replace("MaxFrames", MaxFrames);
        update();
    }

}
