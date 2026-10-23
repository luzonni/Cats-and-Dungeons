package com.retronova.game.items;

import com.retronova.engine.graphics.Acelerada;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.SpriteHandler;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * O REGISTRO DOS ÍCONES: buffs, passivas, armas extras, troféu, elementos e
 * raridades, num lugar só.
 *
 * Cada constante é um PNG de 16x16 em icones/, gerado por tools/GenIcones.java a
 * partir do pacote DB32 (CC0) e de desenhos no mesmo molde, todos na paleta do
 * jogo (ver docs/ARQUITETURA.md, "Ícones"). Quem desenha um ícone pede aqui — pela
 * melhoria, passiva, arma, elemento ou raridade — e nunca carrega o arquivo
 * direto: trocar um ícone é trocar o PNG, e acrescentar um é uma linha aqui e uma
 * entrada no gerador.
 *
 * ESCALA INTEIRA SEMPRE. O ícone tem 16 pixels de arte e é desenhado a 16 vezes
 * um fator inteiro ({@link #lado}); a cópia ampliada de cada fator é feita uma vez
 * e guardada, e a versão apagada (conquista por fazer, prévia do baú fora do
 * destaque) também. Nada de busca por nome nem de imagem nova por quadro.
 */
public enum Icone {
    // Atributos (Melhoria)
    VIGOR, FURIA, COURO, PATAS, FRENESI, INSTINTO, PRESAGIO,
    // Passivas
    IMA, ARMADURA, REGENERACAO, CRITICO, AREA, PRESSA, CRESCIMENTO, VOLEIO, RETALIACAO, EXPLOSAO_ARCANA,
    // Armas extras
    NOVELO, GRIMORIO, AURA, RAIO, TREMOR, METEORO,
    // Interface
    TROFEU, DESCONHECIDO,
    // Elementos
    FOGO, GELO, AGUA, TERRA, AR,
    // Raridades
    COMUM, RARO, EPICO, LENDARIO,
    // Coletáveis no chão
    XP, XP_GRANDE;

    /** Lado do ícone, em pixels de arte. */
    public static final int LADO = 16;
    /** Maior fator guardado; acima disso a cópia é feita na hora (não acontece nas telas do jogo). */
    private static final int MAIOR_ESCALA_GUARDADA = 16;

    private BufferedImage nativa, apagada;
    private final BufferedImage[] ampliadas = new BufferedImage[MAIOR_ESCALA_GUARDADA + 1];
    private final BufferedImage[] apagadasAmpliadas = new BufferedImage[MAIOR_ESCALA_GUARDADA + 1];

    // ----------------------------------------------------------------- de quem

    private static final Icone[] DA_MELHORIA = new Icone[Melhoria.values().length];
    private static final Icone[] DA_PASSIVA = new Icone[Passiva.values().length];
    private static final Icone[] DA_ARMA = new Icone[ArmaExtra.values().length];

    static {
        for (Melhoria m : Melhoria.values()) {
            DA_MELHORIA[m.ordinal()] = valueOf(m.name());
        }
        for (Passiva p : Passiva.values()) {
            DA_PASSIVA[p.ordinal()] = valueOf(p.name());
        }
        for (ArmaExtra a : ArmaExtra.values()) {
            DA_ARMA[a.ordinal()] = valueOf(a.name());
        }
    }

    public static Icone de(Melhoria m) {
        return DA_MELHORIA[m.ordinal()];
    }

    public static Icone de(Passiva p) {
        return DA_PASSIVA[p.ordinal()];
    }

    public static Icone de(ArmaExtra a) {
        return DA_ARMA[a.ordinal()];
    }

    /** O símbolo do elemento, ou nulo para a arma sem elemento (e para a lendária, que é raridade). */
    public static Icone de(Elemento e) {
        return switch (e) {
            case FOGO -> FOGO;
            case GELO -> GELO;
            case AGUA -> AGUA;
            case TERRA -> TERRA;
            case AR -> AR;
            case NENHUM, LENDARIA -> null;
        };
    }

    public static Icone de(Raridade r) {
        return switch (r) {
            case COMUM -> COMUM;
            case RARO -> RARO;
            case EPICO -> EPICO;
            case LENDARIO -> LENDARIO;
        };
    }

    /**
     * Pelo nome da melhoria, passiva ou arma (o que as cartas guardam), ou o ponto
     * de interrogação se não houver. Para montar uma carta, não para cada quadro.
     */
    public static Icone pelaChave(String chave) {
        for (Icone i : values()) {
            if (i.name().equalsIgnoreCase(chave)) {
                return i;
            }
        }
        return DESCONHECIDO;
    }

    // ----------------------------------------------------------------- desenho

    /** O lado do ícone desenhado no fator {@code escala}, em pixels de tela. */
    public static int lado(int escala) {
        return LADO * Math.max(1, escala);
    }

    /** O maior fator inteiro em que o ícone cabe em {@code lado} pixels (pelo menos 1). */
    public static int escalaPara(int lado) {
        return Math.max(1, lado / LADO);
    }

    /** Desenha com o canto de cima à esquerda em (x, y). */
    public void desenhar(Graphics2D g, int x, int y, int escala) {
        g.drawImage(imagem(escala, false), x, y, null);
    }

    /** O mesmo ícone apagado, nos cinzas da interface: o que ainda não foi conquistado ou não está em destaque. */
    public void desenharApagado(Graphics2D g, int x, int y, int escala) {
        g.drawImage(imagem(escala, true), x, y, null);
    }

    /** Desenha com o centro em (cx, cy): o ícone no mundo (a arma em volta do gato, a gema no chão). */
    public void desenharCentrado(Graphics2D g, int cx, int cy, int escala) {
        int l = lado(escala);
        desenhar(g, cx - l / 2, cy - l / 2, escala);
    }

    /** Desenha centrado numa caixa de {@code lado} pixels, no maior fator inteiro que caiba. */
    public void desenharNoCentro(Graphics2D g, int x, int y, int lado) {
        int e = escalaPara(lado);
        int l = lado(e);
        desenhar(g, x + (lado - l) / 2, y + (lado - l) / 2, e);
    }

    /** O ícone em 16x16, como está no arquivo. */
    public synchronized BufferedImage nativa() {
        if (nativa == null) {
            nativa = SpriteHandler.getSprite("icones", name().toLowerCase(), 1);
        }
        return nativa;
    }

    public synchronized BufferedImage imagem(int escala, boolean apagado) {
        int e = Math.max(1, escala);
        BufferedImage[] guardadas = apagado ? apagadasAmpliadas : ampliadas;
        if (e < guardadas.length && guardadas[e] != null) {
            return guardadas[e];
        }
        BufferedImage feita = Acelerada.de(ampliar(apagado ? apagada() : nativa(), e));
        if (e < guardadas.length) {
            guardadas[e] = feita;
        }
        return feita;
    }

    /**
     * A versão apagada: cada pixel vai para o cinza da interface de mesma
     * luminância (DEEP, MAIN, LIGHT), com o contorno mantido. A forma continua
     * lendo, sem a cor dizer que está ativo.
     */
    private BufferedImage apagada() {
        if (apagada == null) {
            BufferedImage n = nativa();
            BufferedImage o = new BufferedImage(n.getWidth(), n.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int contorno = Palette.OUTLINE.getRGB() & 0xFFFFFF;
            int[] cinzas = {Palette.DEEP.getRGB(), Palette.MAIN.getRGB(), Palette.LIGHT.getRGB()};
            for (int y = 0; y < n.getHeight(); y++) {
                for (int x = 0; x < n.getWidth(); x++) {
                    int p = n.getRGB(x, y);
                    if ((p >>> 24) == 0) {
                        continue;
                    }
                    if ((p & 0xFFFFFF) == contorno) {
                        o.setRGB(x, y, p);
                        continue;
                    }
                    double l = (0.299 * (p >> 16 & 255) + 0.587 * (p >> 8 & 255) + 0.114 * (p & 255)) / 255.0;
                    o.setRGB(x, y, cinzas[l < 0.4 ? 0 : l < 0.7 ? 1 : 2]);
                }
            }
            apagada = o;
        }
        return apagada;
    }

    private static BufferedImage ampliar(BufferedImage img, int s) {
        BufferedImage o = new BufferedImage(img.getWidth() * s, img.getHeight() * s, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) == 0) {
                    continue;
                }
                for (int dy = 0; dy < s; dy++) {
                    for (int dx = 0; dx < s; dx++) {
                        o.setRGB(x * s + dx, y * s + dy, p);
                    }
                }
            }
        }
        return o;
    }
}
