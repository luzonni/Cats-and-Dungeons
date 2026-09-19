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
    Axe;

    /**
     * Itens que existem no codigo mas estao FORA de jogo.
     *
     * Os consumiveis nunca foram balanceados e o ima foi retirado; enquanto isso
     * nao se resolve, eles nao podem ser oferecidos como carta nem vendidos, senao
     * a escolha do jogador vale nada — pior que nao oferecer.
     *
     * MORA AQUI PORQUE O CONJUNTO E UM SO. Ele estava escrito identico em dois
     * lugares, a carta de recompensa e a loja, e conjunto duplicado so fica igual
     * enquanto ninguem mexe: bastaria alguem religar a racao num dos dois para o
     * jogo passar a oferecer na loja o que a carta recusa. Com um dono, religar um
     * item e apagar uma linha.
     */
    public static final java.util.Set<ItemIDs> EM_ESPERA;

    static {
        java.util.EnumSet<ItemIDs> fora = java.util.EnumSet.of(
                Feed, Acorn, Catnip, Watermelon, MagneticOrb, Bomb, GasBomb);
        // AS VARIANTES ELEMENTAIS SAEM DO CATALOGO, e nao do codigo.
        //
        // O elemento virou escolha de CORRIDA: quem pega gelo no comeco empunha a
        // espada de gelo sem precisar acha-la, porque Item.build monta a base ja com
        // o elemento da partida. Com isso, oferecer "SwordIce" na loja passou a ser
        // uma pergunta sem sentido — numa corrida de agua ela seria uma arma que
        // contradiz a propria corrida, e numa de gelo seria a arma que voce ja tem.
        //
        // As ENTRADAS FICAM NO ENUM de proposito. O ordinal delas esta gravado em
        // save de jogador e em JSON de mapa; remove-las deslocaria todos os IDs
        // seguintes e faria uma corrida salva voltar com outra arma na mao. Ficam
        // aqui, construiveis, so nao oferecidas.
        //
        // A ARTE NAO SE PERDE. Cada variante e uma silhueta diferente, desenhada a
        // partir dos pacotes, e e justamente ela que aparece quando a corrida tem
        // aquele elemento — ver Item.build.
        for (ItemIDs id : values()) {
            String nome = id.name();
            for (String base : new String[]{"Sword", "Axe", "Bow", "Wand"}) {
                if (nome.startsWith(base) && !nome.equals(base)
                        && !nome.equals("BowEletric")) {
                    fora.add(id);
                }
            }
        }
        EM_ESPERA = java.util.Collections.unmodifiableSet(fora);
    }

    /** Este item pode ser oferecido ou vendido? */
    public boolean jogavel() {
        return !EM_ESPERA.contains(this);
    }
}
