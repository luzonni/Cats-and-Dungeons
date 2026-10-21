package com.retronova.game.objects.entities;

/**
 * Os tipos de entidade. O ordinal e o ID que {@link Entity#build} recebe.
 *
 * Nada fora da execucao guarda esse numero: os mapas citam a entidade pelo NOME
 * ({@code "name": "Gate"}) e o save da corrida grava os chefes vivos tambem pelo
 * nome. Por isso a lista pode perder entradas sem quebrar mapa nem save — foi
 * assim que sairam a porta, o alcapao, o caixote com loot, a corrente, o Rato
 * Monarca e o Gato Chorao, que nenhum mapa nem a horda usavam mais.
 */
public enum EntityIDs {

    Player,
    Zombie,
    Skeleton,
    Slime,
    RatExplode,
    MouseSquire,
    MouseVampire,
    CatToyBoss,
    KingCursedCatBoss,
    Seller,
    Plate,
    // Em espera junto com o dinheiro: nenhum mapa a coloca. Ver Seller.
    GumMachine,
    Pillar,
    Brazier,
    Rubble,
    Gate,
    Lever,
    Barrel,
    Bones,
    Torch,
    // Mobília decorativa da antecâmara, toda atendida por Decor.
    Shelf,
    Table,
    Rug,
    Chest,
    Bed,
    Anvil,
    Entrance,
    // Chefe final da corrida, aos 30 minutos.
    RatoRei,

}
