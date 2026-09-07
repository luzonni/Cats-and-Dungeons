package com.retronova.game.objects.tiles;

public enum TileIDs {
    // 0x_2xRed_2xGreen_2xBlue
    // 00 -> 0
    // ff -> 255
    Void(0x00000000, false),
    Brick(0xff515453, true),
    PurpleMud(0xff3d253b, false),
    Lava(0xffdf7126, false),
    Ice(0xff00c8ff, false),
    DeathSand(0xff35211d, false),
    DarkBricks(0xff355157, true),
    Ceramics(0xff46353d, false),
    Sand(0xfff7d18a, false),
    // Parede em duas faces: o topo claro recebe a luz, a face escura e o que se
    // ve de frente. E o contraste entre as duas que da volume a parede.
    StoneTop(0xff4a6b70, true),
    StoneFace(0xff243840, true),
    // Piso de laje. As variantes existem para quebrar a repeticao: um tile unico
    // de chao deixa a dungeon visivelmente tilada.
    DungeonFloor(0xff3a4048, false),
    DungeonFloorCracked(0xff343a41, false),
    DungeonFloorMossy(0xff3f4a3c, false),
    DungeonFloorBroken(0xff2f353c, false),
    // Variantes da face da parede. Paredes longas de um tile so ficam obviamente
    // repetidas; estas quebram o padrao e ainda ambientam.
    StoneGrate(0xff1f3038, true),
    StoneRelief(0xff2a3f47, true),
    StoneBanner(0xff33474e, true),
    // Degraus. Atravessavel: marca a descida sem partir o salao em dois.
    StoneSteps(0xff5a7d81, false),
    // Rocha em volta da sala, no lugar do Void. Sem ela a camera mostrava o
    // vazio preto ao chegar na borda do mapa.
    Bedrock(0xff11161c, true);

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
