package com.retronova.game.objects.tiles;

public enum TileIDs {
    // 0x_2xRed_2xGreen_2xBlue
    // 00 -> 0
    // ff -> 255
    Void(0x00000000, false),
    Brick(0xff000000, true),
    Stone(0xffffffff, false),
    Lava(0xffff0000, false),
    Ice(0xff00c8ff, false),
    DeathSand(0xff35211d, false),
    DarkBricks(0xff355157, true),
    Ceramics(0xff46353d, false);

    private final int color;
    private final boolean solid;
    TileIDs(int color, boolean isSolid) {
        this.color = color;
        this.solid = isSolid;
    }

    public int getColor() {
        return this.color;
    }

    public boolean getSolid() {
        return this.solid;
    }

}
