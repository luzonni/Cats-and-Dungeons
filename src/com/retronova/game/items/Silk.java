package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.inputs.mouse.Mouse;
import com.retronova.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.util.List;

public class Silk extends Item {

    private Rectangle bounds;
    private double r;

    Silk(int id) {
        super(id, "Silk", "silk");
        this.bounds = new Rectangle(GameObject.SIZE(), GameObject.SIZE());
    }

    @Override
    public void tick() {
        r += 0.2d;
        Player player = Game.getPlayer();
        double angle = player.getAngle(new Point(Mouse.getX() + Game.C.getX(), Mouse.getY() + Game.C.getY()));
        if(Mouse.pressing(Mouse_Button.LEFT)) {
            double x = player.getX() + Math.cos(angle) * GameObject.SIZE();
            double y = player.getY() + Math.sin(angle) * GameObject.SIZE();
            this.bounds.setLocation((int)x, (int)y);
        }
        List<Entity> entities = Game.getMap().getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(e.equals(player))
                continue;
            if(e.getBounds().intersects(this.bounds)) {
                e.strike(AttackTypes.Melee, 10);
            }
        }
    }

    public void render(Graphics2D g) {
        int x = bounds.x - Game.C.getX();
        int y = bounds.y - Game.C.getY();
        int w = bounds.width;
        int h = bounds.height;
        g.rotate(r, x + w/2, y + h/2);
        g.drawImage(getSprite(), x, y, null);
        g.rotate(-r, x + w/2, y + h/2);
    }
}
