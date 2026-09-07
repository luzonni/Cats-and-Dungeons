package com.retronova.game.objects.tiles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import studio.retrozoni.sheeter.SpriteSheet;

public abstract class Tile extends GameObject {

    private static final Font fontDebugTile;
    private static final Color colorDebugTile;

    static {
        fontDebugTile = FontHandler.font(FontHandler.Septem, Configs.GameScale()*6);
        colorDebugTile = new Color(252, 127, 3);
    }

    /**
     * Liga e desliga a colisao deste tile em tempo de execução.
     *
     * Existe para o portão da antecâmara: enquanto a grade está baixada, o vão
     * dele precisa bloquear passagem como se fosse parede, e voltar a ser chão no
     * instante em que a grade sobe. Fazer isso pelo tile, e não empurrando o gato
     * de volta, é o que faz a barreira ser a MESMA coisa que uma parede — o
     * Physical já resolve colisão contra tile sólido, e a entidade não precisa
     * disputar posição com a física a cada tick.
     */
    public void bloquear(boolean bloqueado) {
        setSolid(bloqueado);
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
            case StoneTop -> {
                return new StoneTop(ID, x, y, solid);
            }
            case StoneFace -> {
                return new StoneFace(ID, x, y, solid, "stoneFace");
            }
            case StoneGrate -> {
                return new StoneFace(ID, x, y, solid, "stoneGrate");
            }
            case StoneRelief -> {
                return new StoneFace(ID, x, y, solid, "stoneRelief");
            }
            case StoneBanner -> {
                return new StoneFace(ID, x, y, solid, "stoneBanner");
            }
            case StoneSteps -> {
                return new StoneSteps(ID, x, y, solid);
            }
            case Bedrock -> {
                return new Bedrock(ID, x, y, solid);
            }
            case DungeonFloor -> {
                return new DungeonFloor(ID, x, y, solid, "dungeonFloor");
            }
            case DungeonFloorCracked -> {
                return new DungeonFloor(ID, x, y, solid, "dungeonFloorCracked");
            }
            case DungeonFloorMossy -> {
                return new DungeonFloor(ID, x, y, solid, "dungeonFloorMossy");
            }
            case DungeonFloorBroken -> {
                return new DungeonFloor(ID, x, y, solid, "dungeonFloorBroken");
            }
            default -> {
                return new Void(TileIDs.Void.ordinal(), x, y, solid);
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
        setSheet(new SpriteSheet("sprites/objects/tile", sprites));
    }

    public abstract void effect(Entity e);

    @Override
    public void tick() {

    }

    public void renderBounds(int index, Graphics2D graphics) {
        Graphics2D g = (Graphics2D)graphics.create();
        int padding = Configs.GameScale();
        g.setColor(colorDebugTile);
        g.setStroke(new BasicStroke(Configs.GameScale()/2f));
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
