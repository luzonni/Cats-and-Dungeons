package com.retronova.game.objects.tiles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Sheet;
import com.retronova.game.objects.entities.Entity;

public abstract class Tile extends GameObject {

    private static Font fontDebugTile;
    private static Color colorDebugTile;

    static {
        fontDebugTile = FontHandler.font(FontHandler.Septem, Configs.GameScale()*6);
        colorDebugTile = new Color(252, 127, 3);
    }


    public static Tile build(int ID, Object... values) {
        TileIDs mapping = TileIDs.values()[ID];
        int x = ((values.length >= 1) ? ((Number)values[0]).intValue() : 0) * GameObject.SIZE();
        int y = ((values.length >= 2) ? ((Number)values[1]).intValue() : 0) * GameObject.SIZE();
        boolean solid = mapping.getSolid();
        switch (mapping) {
            case Brick -> {
                return new Bricks(ID, x, y, solid);
            }
            case PurpleMud -> {
                return new PurpleMud(ID, x, y, solid);
            }
            case Ice -> {
                return new Ice(ID, x, y, solid);
            }
            case Lava -> {
                return new Lava(ID, x, y, solid);
            }
            case DeathSand -> {
                return new DeathSand(ID, x, y, solid);
            }
            case DarkBricks -> {
                return new DarkBriks(ID, x, y, solid);
            }
            case Ceramics -> {
                return new Ceramics(ID, x, y, solid);
            }
            case Sand -> {
                return new Sand(ID, x, y, solid);
            }
            default -> {
                return new Void(-1, x, y, solid);
            }
        }
    }

    Tile(int ID, int x, int y, boolean solid) {
        super(ID);
        setX(x);
        setY(y);
        if(solid){
            setSolid();
        }
    }

    @Override
    public void loadSprites(String... sprites) {
        setSheet(new Sheet<>(Tile.class, sprites));
    }

    public abstract void effect(Entity e);

    @Override
    public void tick() {

    }


    public void renderBounds(int index, Graphics2D graphics) {
        Graphics2D g = (Graphics2D)graphics.create();
        int padding = Configs.GameScale();
        g.setColor(colorDebugTile);
        g.setStroke(new BasicStroke(Configs.GameScale()/2));
        g.drawRect((int)this.getX() + padding, (int)this.getY() + padding, this.getWidth() - padding*2, this.getHeight() - padding*2);
        String text = String.valueOf(index);
        int wF = FontHandler.getWidth(text, fontDebugTile);
        int hF = FontHandler.getHeight(text, fontDebugTile);
        int x = (int)this.getX() + this.getWidth()/2 - wF/2;
        int y = (int)this.getY() + this.getHeight()/2 + hF/2;
        g.setFont(fontDebugTile);
        g.drawString(text, x, y);
        g.dispose();
    }

}
