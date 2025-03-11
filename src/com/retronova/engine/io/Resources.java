package com.retronova.engine.io;

import java.io.File;
import java.net.URL;

public class Resources {

    public static File getFileFromResources(String fileName) {
        URL resource = Resources.class.getResource(fileName);
        if (resource == null) {
            System.err.println("Arquivo não encontrado: " + fileName);
            return null;
        }
        return new File(resource.getFile());
    }
}
