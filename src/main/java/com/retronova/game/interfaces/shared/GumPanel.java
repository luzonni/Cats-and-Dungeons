package com.retronova.game.interfaces.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GumPanel {

    private int padding;
    private final Rectangle bounds;
    private double speed;
    private boolean opened;
    private final List<Silly> sillies;

    private int step;
    private int count;

    public GumPanel(Rectangle bounds, BufferedImage[] sprites) {
        this.padding = Configs.HudScale()*4;
        this.bounds = bounds;
        this.sillies = new ArrayList<>();
        for(int i = 0; i < sprites.length; i++) {
            this.sillies.add(new Silly(this.bounds, sprites[i]));
        }
        this.step = this.sillies.size()*5;
        this.speed = 1.6d;
    }

    public void openAward() {
        this.opened = true;
    }

    public boolean opened() {
        return this.opened;
    }

    public void tick() {
        for (Silly silly : sillies) {
            silly.tick();
        }
        if(opened && !this.sillies.isEmpty()) {
            this.count++;
            if(this.count > step) {
                this.speed += 1.6d;
                this.step-=5;
                this.count = 0;
                this.sillies.removeFirst();
            }
        }
    }

    public boolean animFinish() {
        return sillies.isEmpty();
    }

    public void render(Graphics2D g) {
        g.setColor(new Color(0x09122c));
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        for (Silly silly : sillies) {
            silly.render(g);
        }
    }

    private class Silly {

        private final BufferedImage sprite;
        private final Point position;

        private double dir;
        private double rotate;

        private Silly(Rectangle bounds, BufferedImage sprite) {
            this.sprite = sprite;
            int x = padding + bounds.x + Engine.RAND.nextInt(bounds.width - padding*2);
            int y = padding + bounds.y + Engine.RAND.nextInt(bounds.height - padding*2);
            this.position = new Point(x, y);
            this.dir = Engine.RAND.nextDouble() * Math.PI*2d;
        }

        public void tick() {
            double dirX = Math.cos(dir)*speed;
            double dirY = Math.sin(dir)*speed;
            if(this.position.x + dirX < bounds.x + padding || this.position.x + dirX > bounds.x + bounds.width - padding) {
                dirX*=-1;
            }
            if(this.position.y + dirY < bounds.y + padding || this.position.y + dirY > bounds.y + bounds.height - padding) {
                dirY*=-1;
            }
            this.dir = Math.atan2(dirY, dirX);
            double nextX = this.position.x + dirX;
            double nextY = this.position.y + dirY;
            this.position.setLocation(nextX, nextY);
            this.rotate += 0.2d;
        }

        public void render(Graphics2D g) {
            int width = Configs.HudScale() * 16;
            int height = Configs.HudScale() * 16;
            int x = position.x - width/2;
            int y = position.y - height/2;
            Rotate.draw(this.sprite, new Rectangle(x, y, width, height), rotate, null, g);
        }

    }

}
