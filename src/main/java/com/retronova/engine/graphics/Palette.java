package com.retronova.engine.graphics;

import java.awt.Color;

/**
 * Paleta única do jogo.
 *
 * Estas cores já eram usadas soltas pelas interfaces in-game (Inter, Store,
 * Inventory) e pelos sprites de UI. Os menus usavam um cinza-azulado próprio,
 * de outro sistema de design; centralizar aqui existe para que isso não
 * volte a acontecer.
 */
public final class Palette {

    /** Contorno e texto sobre fundo claro. */
    public static final Color OUTLINE = new Color(0x09122C);
    /** Sombra / preenchimento de elemento secundário. */
    public static final Color DEEP = new Color(0x872341);
    /** Preenchimento principal. */
    public static final Color MAIN = new Color(0xBE3144);
    /** Realce, foco e destaque. */
    public static final Color LIGHT = new Color(0xE17564);
    /** Texto sobre fundo escuro. */
    public static final Color TEXT = new Color(0xFFF7D1);

    /** Véu escuro sobre a cena, para sobreposições. */
    public static final Color VEIL = new Color(9, 18, 44, 190);

    private Palette() { }
}
