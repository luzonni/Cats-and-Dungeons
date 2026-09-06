package com.retronova.engine;

import java.awt.*;

public interface Activity {

    void tick();

    void render(Graphics2D g);

    void dispose();

}
