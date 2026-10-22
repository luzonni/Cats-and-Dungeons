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
 * O PADRÃO de cada camada sai do fator, e o jogador escolhe PASSOS INTEIROS a
 * partir dele, só entre os que cabem na tela:
 *
 *     HUD padrão    = arredonda(fator)          (720p = 2, 1080p = 3, 4K = 6)
 *     menus padrão  = arredonda(fator × 1,125)  (720p = 2, 1080p = 3)
 *     escala        = padrão + passo, entre o mínimo legível e o teto da tela
 *     margem        = arredonda(fator × margem de referência)
 *
 * ESCALA INTEIRA. Pixel art ampliada por fator quebrado borra (suavizada) ou fica
 * com pixels de larguras diferentes (vizinho mais próximo); por isso a escala é
 * inteira, como no Minecraft (GUI Scale 1, 2, 3, 4 e Auto). As resoluções 16:9
 * comuns são múltiplas de 640x360, e entre elas o padrão ocupa EXATAMENTE a mesma
 * fração da tela.
 *
 * PASSOS, E NÃO POR CENTO. A primeira versão pedia 50% a 200% e multiplicava três
 * camadas (Interface × Menus × HUD). Com escala inteira, em 1080p os menus têm uns
 * quatro tamanhos de verdade, e a barra oferecia trinta e uma posições: a maioria
 * não mudava nada, o "200%" aparecia cheio com a escala presa no teto, e acima de
 * 100% não fazia sentido, já que o padrão já vem da resolução. Agora cada passo é
 * um tamanho que se vê, e só existem os que cabem: em 720p os menus vão do padrão
 * ao Larger; em 4K, o HUD vai de Smaller a Largest.
 *
 * O DPI NÃO ENTRA, de propósito, e é a mesma decisão da correção de resolução:
 * Main fixa sun.java2d.uiScale=1 (JEP 263), a resolução é em pixels da tela, e a
 * interface acompanha a resolução — numa tela de DPI alto, a mesma fração.
 *
 * Os menus usam 1,125 unidade por unidade do HUD: o tamanho que o jogador
 * escolheu nos testes (2 em 720p e 3 em 1080p). Com 1,5 os menus ficavam grandes
 * demais no padrão (5 em 1080p).
 */
public final class EscalaDaInterface {

    private EscalaDaInterface() {
    }

    // ----------------------------------------------------------- ajuste fino

    /** A resolução de referência do HUD, em pixels de arte. */
    public static final int REF_LARGURA = 640, REF_ALTURA = 360;
    /** Unidades dos menus por unidade do HUD (os menus são compostos em 568x320). */
    public static final double MENUS_POR_REFERENCIA = 1.125;
    /**
     * A menor tela que um menu precisa, em pixels de arte: nenhum menu passa dela.
     * 384x224 e não 320x180: o painel de Settings, com título, abas, Resume e
     * rodapé, ocupa uns 222 de altura, e com 180 a escala 5 numa janela de 1080p
     * punha o Resume em cima do rodapé.
     */
    public static final int MENU_MINIMO_LARGURA = 384, MENU_MINIMO_ALTURA = 224;
    /** Passos de tamanho a partir do padrão: dois menores e três maiores, no máximo. */
    public static final int TAMANHO_MINIMO = -2, TAMANHO_MAXIMO = 3, TAMANHO_PADRAO = 0;
    /** O nome de cada passo na tela, de TAMANHO_MINIMO a TAMANHO_MAXIMO. */
    private static final String[] NOMES = {"Smaller", "Small", "Default", "Large", "Larger", "Largest"};
    /** Margem das bordas em pixels de referência: padrão e máximo. */
    public static final int MARGEM_PADRAO = 7, MARGEM_MAXIMA = 16;
    /**
     * O MENOR TAMANHO LEGÍVEL. As fontes da casa são pixel art de 5 a 8 pixels de
     * altura: em escala 1 elas têm de 5 a 8 pixels de tela, ilegíveis num monitor
     * de 720p para cima. A partir de fator 2 (1280x720), nenhuma camada desce de 2.
     */
    public static final int ESCALA_LEGIVEL = 2;

    // ----------------------------------------------------------- fórmulas

    /**
     * As escalas possíveis de uma camada nesta tela: o padrão, o piso (legível) e o
     * teto (o que cabe). O passo escolhido vira escala por aqui, e a barra de
     * Settings mostra de {@link #menor()} a {@link #maior()}.
     */
    public record Faixa(int padrao, int piso, int teto) {

        /** O menor passo que ainda muda alguma coisa nesta tela. */
        public int menor() {
            return Math.min(maior(), Math.max(TAMANHO_MINIMO, piso - padrao));
        }

        /** O maior passo que ainda cabe nesta tela. */
        public int maior() {
            return Math.max(TAMANHO_MINIMO, Math.min(TAMANHO_MAXIMO, teto - padrao));
        }

        /** O passo gravado, trazido para o que esta tela comporta. */
        public int efetivo(int tamanho) {
            return Math.max(menor(), Math.min(maior(), tamanho));
        }

        /** Pixels de tela por pixel de arte para este passo. */
        public int escala(int tamanho) {
            return Math.max(piso, Math.min(teto, padrao + efetivo(tamanho)));
        }
    }

    /** Quanto a resolução de referência cabe na imagem do jogo, pelo lado mais apertado. */
    public static double fator(int largura, int altura) {
        if (largura <= 0 || altura <= 0) {
            return 3;   // ainda sem janela: o de 1080p
        }
        return Math.min(largura / (double) REF_LARGURA, altura / (double) REF_ALTURA);
    }

    /** As escalas do HUD nesta tela; o teto vem do layout (ver LayoutDoHud.escalaMaxima). */
    public static Faixa faixaDoHud(int largura, int altura, int tetoDoLayout) {
        double f = fator(largura, altura);
        return faixa((int) Math.round(f), legivel(f), Math.max(1, tetoDoLayout));
    }

    /** As escalas dos menus nesta tela; o teto é o menor menu caber inteiro. */
    public static Faixa faixaDosMenus(int largura, int altura) {
        double f = fator(largura, altura);
        return faixa((int) Math.round(f * MENUS_POR_REFERENCIA), legivel(f), tetoDosMenus(largura, altura));
    }

    private static Faixa faixa(int padrao, int legivel, int teto) {
        int piso = Math.min(legivel, teto);
        return new Faixa(Math.max(piso, Math.min(teto, padrao)), piso, teto);
    }

    /** A maior escala de menu que cabe na tela (ver MENU_MINIMO_LARGURA). */
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

    /** Leva um passo gravado à faixa dos passos. */
    public static int tamanhoValido(int tamanho) {
        return Math.max(TAMANHO_MINIMO, Math.min(TAMANHO_MAXIMO, tamanho));
    }

    /** O nome do passo na tela: Small, Default, Large... */
    public static String nome(int tamanho) {
        return NOMES[tamanhoValido(tamanho) - TAMANHO_MINIMO];
    }
}
