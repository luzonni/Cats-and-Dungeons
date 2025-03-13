package com.retronova.engine.graphics;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Galaxy {

    private final BufferedImage space;
    private final List<Star> stars;
    private int count;

    public Galaxy() {
        this.stars = new ArrayList<>();
        int w = Engine.window.getWidth() / Configs.getSCALE();
        int h = Engine.window.getHeight() / Configs.getSCALE();
        this.space = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    public void tick() {
        Graphics g = space.getGraphics();
        g.setColor(new Color(0,0,0,80));
        g.fillRect(0, 0, space.getWidth(), space.getHeight());
        count++;
        if(count > 60) {
            count= 0;
            int x = Engine.RAND.nextInt(space.getWidth());
            int y = Engine.RAND.nextInt(space.getHeight());
            double theta = Engine.RAND.nextDouble() * Math.PI * 2;
            double speed = 1d + Engine.RAND.nextDouble()*3;
            this.stars.add(new Star(new Point(x, y), theta, speed, 1 + Engine.RAND.nextInt(2)));
        }
        for(int i = 0; i < stars.size(); i++) {
            Star star = stars.get(i);
            Point p = star.tick();
            if(star.life <= 0) {
                stars.remove(star);
                i--;
            }
            g.setColor(new Color(255, 255 ,255, star.life));
            g.fillRect(p.x, p.y, star.size, star.size);
        }
    }

    public void render(Graphics2D g) {
        g.drawImage(space, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), Engine.window);
    }

    private static class Star {

        Point position;
        double theta;
        double speed;
        int life;
        int size;

        Star(Point position, double theta, double speed, int size) {
            this.position = position;
            this.theta = theta;
            this.speed = speed;
            this.size = size;
            this.life = 255;
        }
        public Point tick() {
            double x = position.getX() + Math.cos(theta) * speed;
            double y = position.getY() + Math.sin(theta) * speed;
            position.setLocation(x, y);
            life-=5;
            return position;
        }

    }

}
