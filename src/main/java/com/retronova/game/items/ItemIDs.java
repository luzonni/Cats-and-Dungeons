package com.retronova.game.items;

public enum ItemIDs {

    Silk,
    Sword,
    Bow,
    Bomb,
    Feed,
    Acorn,
    Catnip,
    Laser,
    Watermelon,
    Wand,
    Kunai,
    ClawBlades,
    GasBomb,
    BloodyAxe,
    MagneticOrb,
    BowEletric,
    SwordFire,
    DangerousWand,
    Trident,
    Sickle,
    Furball,

    /**
     * As variantes elementais. SEMPRE NO FIM.
     *
     * O ordinal deste enum e o identificador gravado nos JSON de mapa e de
     * personagem: inserir no meio trocaria o inventario inicial de todo mundo e
     * o estoque de todo mapa por outro item. Acrescentar no fim e a unica forma
     * segura de crescer esta lista.
     *
     * SwordFire nao aparece aqui porque ja existia la em cima, com arte propria
     * feita a mao — e a espada de fogo da familia.
     */
    SwordIce,
    SwordWater,
    SwordEarth,
    SwordAir,
    SwordLegend,

    AxeFire,
    AxeIce,
    AxeWater,
    AxeEarth,
    AxeAir,
    AxeLegend,

    BowFire,
    BowIce,
    BowWater,
    BowEarth,
    BowAir,
    BowLegend,

    WandFire,
    WandIce,
    WandWater,
    WandEarth,
    WandAir,
    WandLegend,

    /**
     * O machado COMUM.
     *
     * Entrou depois porque o BloodyAxe fazia os dois papeis: era o machado base e
     * o machado especial ao mesmo tempo. Separados, o de sangue pode ser vermelho
     * e roubar vida, e o comum pode ser o que ele sempre deveria ter sido — um
     * machado.
     */
    Axe,

}
