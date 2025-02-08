package com.retronova.engine;

import java.awt.*;

public interface Activity {

    void tick();

    void render(Graphics g);

    void dispose();

}
