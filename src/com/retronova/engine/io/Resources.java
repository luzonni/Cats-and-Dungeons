package com.retronova.engine.io;

import com.retronova.engine.Engine;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.URL;
import java.util.Objects;

public class Resources {

    public static JSONObject getJsonFile(String module, String name) throws IOException {
        String path = Engine.resPath + module + "/" + name + ".json";
        try {
            URL resource = Resources.class.getResource(path);
            if(resource == null)
                throw new IOException("Erro ao ler o arquivo: " + name);
            InputStream stream = resource.openStream();
            Reader isr = new InputStreamReader(stream);
            JSONParser parse = new JSONParser();
            parse.reset();
            return (JSONObject) parse.parse(isr);
        } catch (ParseException e) {
            System.err.println("Erro getJson(): " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
