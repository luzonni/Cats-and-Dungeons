package com.retronova.engine;

/**
 * A matemática de escala da interface, num lugar só e sem estado: dado o tamanho
 * da imagem do jogo e as preferências, quantos pixels da tela vale um pixel de
 * arte do HUD e dos menus.
 *
 * ---------------------------------------------------------------------------
 * O DEFEITO QUE ISTO SUBSTITUI
 *
 * Eram três fórmulas, nenhuma proporcional à resolução:
 *
 *   - HUD size (2 a 6) era o multiplicador CRU: "4" era 4 pixels por pixel de
 *     arte em 720p e em 4K, então o HUD ocupava o dobro da tela em 1280 do que em
 *     2560;
 *   - GUI size (1, 2, 3) SUBTRAÍA passos de um automático: em 720p o "1" era
 *     metade do "3" (2 contra 4), em 4K era 80% dele (8 contra 10);
 *   - Margin era em pixels absolutos: 20 px são muito em 720p e nada em 4K.
 *
 * ---------------------------------------------------------------------------
 * O MODELO
 *
 * Uma RESOLUÇÃO DE REFERÊNCIA, 640x360: é nela que o HUD foi desenhado, em pixels
 * de arte. O fator da tela é quanto ela cabe na imagem do jogo, pelo lado mais
 * apertado (o menor dos dois), para nada sair da tela numa proporção diferente:
 *
 *     fator = min(largura / 640, altura / 360)
 *
 * E a escala de cada camada é SEMPRE MULTIPLICATIVA e calculada dos valores de
 * referência, nunca do valor já escalado (que acumularia arredondamento):
 *
 *     HUD    = arredonda(fator × Interface% × HUD%)
 *     menus  = arredonda(fator × 1,125 × Interface% × Menus%)
 *     margem = arredonda(fator × margem de referência)
 *
 * ESCALA INTEIRA, ARREDONDADA UMA VEZ, NO FIM. Pixel art ampliada por fator
 * quebrado borra (suavizada) ou fica com pixels de larguras diferentes (vizinho
 * mais próximo); por isso a escala é inteira, como no Minecraft e em todo jogo de
 * pixel art que escala a interface. Arredondar só o produto final — e não o fator
 * e depois cada preferência — mantém a proporção o mais perto possível do pedido:
 * as resoluções 16:9 comuns são múltiplas de 640x360 (720p = 2, 1080p = 3, 1440p
 * = 4, 4K = 6), e entre elas 100% ocupa EXATAMENTE a mesma fração da tela. Fora
 * delas (1366x768, 1600x900) o erro é o de arredondar para o pixel inteiro.
 *
 * O DPI NÃO ENTRA, de propósito, e é a mesma decisão da correção de resolução:
 * Main fixa sun.java2d.uiScale=1 (JEP 263), a resolução é em pixels da tela, e a
 * interface acompanha a resolução — numa tela de DPI alto, a mesma fração.
 *
 * Os menus usam 1,125 unidade por unidade do HUD: o tamanho que o jogador
 * escolheu como o certo (o "75%" da primeira versão, que usava 1,5) virou o 100%.
 * Com 1,5 os menus ficavam grandes demais no padrão (5 em 1080p); com 1,125 são 2
 * em 720p e 3 em 1080p.
 */
public final class EscalaDaInterface {

    private EscalaDaInterface() {
    }

    // ----------------------------------------------------------- ajuste fino

    /** A resolução de referência do HUD, em pixels de arte. */
    public static final int REF_LARGURA = 640, REF_ALTURA = 360;
    /** Unidades dos menus por unidade do HUD (os menus são compostos em 568x320). */
    public static final double MENUS_POR_REFERENCIA = 1.125;
    /** A menor tela que um menu precisa, em pixels de arte: nenhum menu passa dela. */
    public static final int MENU_MINIMO_LARGURA = 320, MENU_MINIMO_ALTURA = 180;
    /** Faixa e passo das preferências de tamanho, em por cento. */
    public static final int PCT_MINIMO = 50, PCT_MAXIMO = 200, PASSO = 5, PCT_PADRAO = 100;
    /** Margem das bordas em pixels de referência: padrão e máximo. */
    public static final int MARGEM_PADRAO = 7, MARGEM_MAXIMA = 16;
    /**
     * O MENOR TAMANHO LEGÍVEL. As fontes da casa são pixel art de 5 a 8 pixels de
     * altura: em escala 1 elas têm de 5 a 8 pixels de tela, ilegíveis num monitor
     * de 720p para cima. A partir de fator 2 (1280x720), nenhuma camada desce de 2.
     */
    public static final int ESCALA_LEGIVEL = 2;

    // ----------------------------------------------------------- fórmulas

    /** Quanto a resolução de referência cabe na imagem do jogo, pelo lado mais apertado. */
    public static double fator(int largura, int altura) {
        if (largura <= 0 || altura <= 0) {
            return 3;   // ainda sem janela: o de 1080p
        }
        return Math.min(largura / (double) REF_LARGURA, altura / (double) REF_ALTURA);
    }

    /** Pixels de tela por pixel de arte do HUD, antes do teto do layout (ver LayoutDoHud). */
    public static int hud(int largura, int altura, int interfacePct, int hudPct) {
        double f = fator(largura, altura);
        int u = (int) Math.round(f * interfacePct / 100.0 * hudPct / 100.0);
        return Math.max(legivel(f), u);
    }

    /** Pixels de tela por pixel de arte dos menus, já no teto do que cabe na tela. */
    public static int menus(int largura, int altura, int interfacePct, int menusPct) {
        double f = fator(largura, altura);
        int u = (int) Math.round(f * MENUS_POR_REFERENCIA * interfacePct / 100.0 * menusPct / 100.0);
        int teto = tetoDosMenus(largura, altura);
        return Math.max(Math.min(legivel(f), teto), Math.min(teto, u));
    }

    /** A maior escala de menu que cabe na tela (o menor menu tem 320x180 pixels de arte). */
    public static int tetoDosMenus(int largura, int altura) {
        if (largura <= 0 || altura <= 0) {
            return 5;
        }
        return Math.max(1, Math.min(largura / MENU_MINIMO_LARGURA, altura / MENU_MINIMO_ALTURA));
    }

    /** A margem das bordas em pixels de tela. */
    public static int margem(int largura, int altura, int margemDeReferencia) {
        return (int) Math.round(fator(largura, altura) * Math.max(0, margemDeReferencia));
    }

    private static int legivel(double fator) {
        return fator >= ESCALA_LEGIVEL ? ESCALA_LEGIVEL : 1;
    }

    /**
     * O maior por cento (em passos de {@link #PASSO}) cuja escala não passa do teto.
     * É o máximo que o slider deixa escolher: valor acima disso não teria efeito,
     * ou faria peças se sobreporem.
     */
    public static int maiorPctQueCabe(java.util.function.IntUnaryOperator escalaDoPct, int teto) {
        for (int pct = PCT_MAXIMO; pct > PCT_MINIMO; pct -= PASSO) {
            if (escalaDoPct.applyAsInt(pct) <= teto) {
                return pct;
            }
        }
        return PCT_MINIMO;
    }

    /** Leva um valor gravado à faixa e ao passo das preferências. */
    public static int pctValido(int pct) {
        int p = Math.max(PCT_MINIMO, Math.min(PCT_MAXIMO, pct));
        return Math.round(p / (float) PASSO) * PASSO;
    }
}
