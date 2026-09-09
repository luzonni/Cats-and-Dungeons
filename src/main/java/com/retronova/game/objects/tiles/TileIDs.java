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
    Bedrock(0xff11161c, true),

    /**
     * A CISTERNA INUNDADA, tema da primeira arena.
     *
     * SEMPRE NO FIM, pela mesma razao dos itens: o ordinal deste enum e o numero
     * gravado nos JSON de mapa, e inserir no meio trocaria o cenario de toda sala
     * ja escrita por outro tile.
     *
     * Os tiles saem de tools/GenCisterna.java, e as cores abaixo tem de bater com
     * as de la — e o mapa que liga a cor do PNG ao tile que aparece na tela.
     */
    /** Agua parada da cisterna. Rasa: molha o passo, nao barra. */
    CisternWater(0xff2f6b3d, false),
    /** Grade redonda na parede: por onde a agua entrou. */
    CisternGrate(0xff24312b, true),
    /** Laje seca da plataforma central. */
    CisternFloor(0xff8f8b7a, false),
    /** Laje rachada. Existe para quebrar a repeticao do piso. */
    CisternFloorCracked(0xff84806f, false),
    /** Laje gasta, a mais escura das tres. */
    CisternFloorWorn(0xff7a7666, false),
    /** Canaleta reta, no sentido norte-sul. */
    CisternChannelV(0xff565349, false),
    /** Canaleta reta, no sentido leste-oeste. */
    CisternChannelH(0xff525046, false),
    /** Cruzamento das duas canaletas. */
    CisternChannelCross(0xff4e4c43, false),
    /** O ralo no piso: para onde as canaletas correm. */
    CisternSink(0xff4a4840, false),
    /** Topo da parede da cisterna: a face que recebe a luz. */
    CisternStoneTop(0xff8a8f92, true),
    /** Face da parede da cisterna, virada para dentro. */
    CisternStoneFace(0xff5c6165, true),
    /** Caveira caida no piso. */
    CisternSkull(0xff6b6f5e, false),
    /** Placa de pressao emperrada. */
    CisternPlate(0xff676b5a, false),
    /** Frasco deixado para tras. */
    CisternFlask(0xff5f6352, false),
    /** Frasco grande, do mesmo despojo. */
    CisternFlaskBig(0xff5b5f4e, false);

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
