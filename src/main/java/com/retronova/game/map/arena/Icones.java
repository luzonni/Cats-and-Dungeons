package com.retronova.game.map.arena;

import com.retronova.game.items.Icone;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * O que sobrou dos ícones do caça-níquel: o tamanho na grade da Roleta e o
 * rastro de movimento do rolo.
 *
 * Os desenhos de 9x9 que moravam aqui saíram: os ícones agora são os PNG de 16x16
 * do registro {@link Icone}, na paleta do jogo (ver tools/GenIcones.java). A
 * Roleta está fora de uso (ver AberturaDoBau) e só ela ainda pensa em "células" de
 * 9; o resto do jogo desenha pelo Icone, em fator inteiro.
 */
@Deprecated
final class Icones {

    private Icones() {
    }

    /** Faixas de velocidade do borrão. Mais que isso não se distingue girando. */
    static final int NIVEIS_DE_RASTRO = 6;

    /** O fator inteiro do ícone de 16 que ocupa o lugar de uma grade de 9 células de {@code celula}. */
    static int escala(int celula) {
        return Math.max(1, (int) Math.round(celula * 9 / 16.0));
    }

    static int lado(int celula) {
        return Icone.lado(escala(celula));
    }

    static void desenhar(Graphics2D g, Icone icone, int x, int y, int celula) {
        icone.desenhar(g, x, y, escala(celula));
    }

    private static final Map<String, BufferedImage> RASTROS = new ConcurrentHashMap<>();

    /**
     * O ícone com o RASTRO de movimento já embutido: três cópias cada vez mais
     * fracas acima, numa imagem só, pronta por faixa de velocidade — trocar o modo
     * de transparência três vezes por ícone, por quadro, esvaziava a fila do
     * pipeline de vídeo e o giro engasgava.
     */
    static void desenharComRastro(Graphics2D g, Icone icone, int x, int y, int celula, int nivel, int passoPx) {
        int e = escala(celula);
        String k = icone.name() + "|" + e + "|" + nivel + "|" + passoPx;
        BufferedImage img = RASTROS.computeIfAbsent(k, chave -> {
            BufferedImage base = icone.imagem(e, false);
            int fantasmas = 3;
            BufferedImage o = new BufferedImage(base.getWidth(), base.getHeight() + fantasmas * passoPx,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D gi = o.createGraphics();
            int topoDaBase = fantasmas * passoPx;
            float forca = Math.min(0.35f, nivel * 0.07f);
            for (int f = fantasmas; f >= 1; f--) {
                gi.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, forca / f));
                gi.drawImage(base, 0, topoDaBase - f * passoPx, null);
            }
            gi.setComposite(java.awt.AlphaComposite.SrcOver);
            gi.drawImage(base, 0, topoDaBase, null);
            gi.dispose();
            return o;
        });
        g.drawImage(img, x, y - 3 * passoPx, null);
    }
}
