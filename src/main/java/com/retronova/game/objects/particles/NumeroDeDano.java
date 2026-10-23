package com.retronova.game.objects.particles;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Acelerada;
import com.retronova.engine.graphics.Familia;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.TextFx;
import com.retronova.game.Game;
import com.retronova.game.items.Icone;
import com.retronova.game.objects.entities.AttackTypes;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * O número que salta do bicho atingido (e o "+N" da cura): um desenho só, com cor
 * que quer dizer alguma coisa.
 *
 * ---------------------------------------------------------------------------
 * O QUE A COR DIZ (ver docs/ARQUITETURA.md, "Números de dano")
 *
 *   dano comum       o claro da casa (TextFx.CLARO)
 *   dano de elemento a luz da família do elemento: fogo, gelo, água, terra, ar
 *   crítico          ouro, maior, com o estouro do ícone de crítico ao lado e um
 *                    "pop" de dois tamanhos ao surgir
 *   veneno           a família épica (roxa), para não se confundir com a cura
 *   cura             verde, com o "+"
 *
 * As cores são as das famílias dos ícones (Familia): o número de fogo tem a cor
 * do selo de fogo. O crítico NÃO depende só da cor — é maior e leva o estouro —,
 * e o rótulo "CRIT" saiu: na horda cheia, dez etiquetas iguais empilhadas
 * cobriam o gato e não diziam nada que o tamanho não dissesse. Vampire Survivors
 * e Brotato fazem o mesmo: número curto, o crítico pelo tamanho e pela cor.
 *
 * GOLPES SEGUIDOS SOMAM. Dois golpes do mesmo tipo no mesmo bicho, a menos de
 * {@link #JUNTAR} ticks um do outro, viram um número só que cresce
 * ({@link #mostrar}): o veneno e a aura batem várias vezes por segundo, e um
 * número por batida era ruído puro.
 *
 * DESENHADO UMA VEZ, COPIADO DEPOIS. Cada algarismo de cada estilo é pintado com
 * o contorno e a sombra da casa (TextFx) numa imagem pequena, por escala; o
 * quadro só copia imagens. O texto com contorno chamava drawString treze vezes
 * por número, todo quadro, e uma horda cheia tem dezenas no ar.
 */
public final class NumeroDeDano extends Particle {

    // ----------------------------------------------------------- ajuste fino

    /** Tamanho da fonte em pixels de arte: o comum, o crítico e o crítico no instante do pop. */
    public static final int TAMANHO = 5, TAMANHO_CRITICO = 7, TAMANHO_POP = 8;
    /** Ticks do pop do crítico (o tamanho maior antes de assentar). */
    public static final int POP = 5;
    /** Golpes do mesmo tipo no mesmo bicho a menos disso, em ticks, somam no mesmo número. */
    public static final int JUNTAR = 12;
    /** Vida do número, em segundos: o comum e o crítico. */
    public static final double VIDA = 0.6, VIDA_CRITICO = 0.9;
    /**
     * Números no ar ao mesmo tempo. Acima disso, um golpe comum só aparece se somar
     * num número que já está no ar; o crítico sempre aparece. Na horda cheia eram
     * mais de cem números, por cima do gato e dos bichos — a hierarquia de leitura
     * (gato, perigo, projétil, coletável, efeito) pede que o efeito ceda.
     */
    public static final int MAXIMO_NO_AR = 36;
    /** Quanto ele sobe no primeiro tick, em pixels de arte, desacelerando até {@link #SUBIDA_MINIMA}. */
    private static final double SUBIDA = 1.0, SUBIDA_MINIMA = 0.15, FREIO = 0.07;

    public enum Estilo {
        NORMAL(TextFx.CLARO), CRITICO(Familia.OURO.tom(Familia.LUZ)), CURA(Familia.VERDE.tom(Familia.LUZ)),
        VENENO(Familia.EPICO.tom(Familia.LUZ)),
        FOGO(Familia.FOGO.tom(Familia.LUZ)), GELO(Familia.GELO.tom(Familia.LUZ)),
        AGUA(Familia.AGUA.tom(Familia.LUZ)), TERRA(Familia.TERRA.tom(Familia.LUZ)), AR(Familia.AR.tom(Familia.BASE));

        final Color cor;

        Estilo(Color cor) {
            this.cor = cor;
        }
    }

    /** O estilo de um golpe pelo tipo de dano: os elementos na cor deles, o resto no claro da casa. */
    public static Estilo estiloDe(AttackTypes tipo) {
        return switch (tipo) {
            case Fire -> Estilo.FOGO;
            case Ice -> Estilo.GELO;
            case Water -> Estilo.AGUA;
            case Earth -> Estilo.TERRA;
            case Air -> Estilo.AR;
            case Poison -> Estilo.VENENO;
            default -> Estilo.NORMAL;
        };
    }

    // ----------------------------------------------------------- estado

    private final Estilo estilo;
    private int valor;
    private String texto;
    private final boolean cura;
    private int idade;
    private double subida;
    private boolean vivo = true;

    private NumeroDeDano(int valor, Estilo estilo, double x, double y) {
        super(x, y, estilo == Estilo.CRITICO ? VIDA_CRITICO : VIDA);
        this.estilo = estilo;
        this.cura = estilo == Estilo.CURA;
        definir(valor);
    }

    /**
     * Mostra um golpe: soma no número anterior do mesmo bicho, se ele ainda estiver
     * no ar, for do mesmo estilo e tiver saído há pouco; senão, põe um número novo.
     *
     * @param anterior o último número deste bicho (ou nulo)
     * @return o número que está mostrando o golpe, para o bicho guardar
     */
    public static NumeroDeDano mostrar(NumeroDeDano anterior, int valor, Estilo estilo, double x, double y) {
        if (anterior != null && anterior.vivo && anterior.estilo == estilo && anterior.idade < JUNTAR) {
            anterior.definir(anterior.valor + valor);
            anterior.idade = 0;           // recomeça: o número que cresce é golpe novo
            anterior.subida = SUBIDA;
            return anterior;
        }
        if (Game.getMap() != mapaDoContador) {
            // Mapa novo (corrida nova, retomada): os números do mapa velho foram com ele.
            mapaDoContador = Game.getMap();
            noAr = 0;
        }
        if (noAr >= MAXIMO_NO_AR && estilo != Estilo.CRITICO) {
            return anterior;
        }
        NumeroDeDano n = new NumeroDeDano(valor, estilo, x, y);
        Game.getMap().put(n);
        noAr++;
        return n;
    }

    /** A cura: "+N" em verde, sobre quem foi curado. Sempre aparece. */
    public static void cura(int valor, double x, double y) {
        Game.getMap().put(new NumeroDeDano(valor, Estilo.CURA, x, y));
        noAr++;
    }

    /** Quantos números estão no ar agora (ver {@link #MAXIMO_NO_AR}), e em que mapa. */
    private static int noAr;
    private static Object mapaDoContador;

    private void definir(int novo) {
        valor = novo;
        texto = (cura ? "+" : "") + novo;
        subida = SUBIDA;
    }

    @Override
    public void tick() {
        idade++;
        if (idade >= getSeconds()) {
            vivo = false;
            noAr = Math.max(0, noAr - 1);
            Game.getMap().remove(this);
            return;
        }
        // Sobe rápido e desacelera: o golpe "salta" do bicho.
        setY(getY() - subida * Configs.GameScale());
        subida = Math.max(SUBIDA_MINIMA, subida - FREIO);
    }

    @Override
    public void render(Graphics2D g) {
        int s = Configs.GameScale();
        boolean pop = estilo == Estilo.CRITICO && idade < POP;
        int tamanho = estilo != Estilo.CRITICO ? TAMANHO : pop ? TAMANHO_POP : TAMANHO_CRITICO;
        BufferedImage[] glifos = glifos(estilo, tamanho, s);
        int largura = 0;
        for (int i = 0; i < texto.length(); i++) {
            largura += glifos[indice(texto.charAt(i))].getWidth() - 2 * s;
        }
        int altura = glifos[0].getHeight();
        int x = (int) getX() - largura / 2;
        int y = (int) getY() - altura / 2;
        if (estilo == Estilo.CRITICO) {
            // O estouro do ícone de crítico ao lado: o crítico não depende só da cor.
            int e = Math.max(1, s / 2);
            Icone.CRITICO.desenhar(g, x - Icone.lado(e), y + (altura - Icone.lado(e)) / 2, e);
        }
        for (int i = 0; i < texto.length(); i++) {
            BufferedImage gl = glifos[indice(texto.charAt(i))];
            g.drawImage(gl, x - s, y, null);
            x += gl.getWidth() - 2 * s;
        }
    }

    // ----------------------------------------------------------- algarismos

    private static final String CARACTERES = "0123456789+";
    /** [estilo][tamanho][caractere], para a escala em {@link #escalaDoCache}. */
    private static final BufferedImage[][][] CACHE = new BufferedImage[Estilo.values().length][TAMANHO_POP + 1][];
    private static int escalaDoCache = -1;

    private static int indice(char c) {
        return c == '+' ? 10 : c - '0';
    }

    private static synchronized BufferedImage[] glifos(Estilo estilo, int tamanho, int s) {
        if (s != escalaDoCache) {
            for (BufferedImage[][] porEstilo : CACHE) {
                java.util.Arrays.fill(porEstilo, null);
            }
            escalaDoCache = s;
        }
        BufferedImage[] g = CACHE[estilo.ordinal()][tamanho];
        if (g == null) {
            g = new BufferedImage[CARACTERES.length()];
            Font fonte = FontHandler.font(FontHandler.Game, tamanho * s);
            for (int i = 0; i < g.length; i++) {
                g[i] = pintar(String.valueOf(CARACTERES.charAt(i)), fonte, estilo.cor, s);
            }
            CACHE[estilo.ordinal()][tamanho] = g;
        }
        return g;
    }

    /** Um algarismo com o contorno e a sombra da casa, numa imagem com folga para os dois. */
    private static BufferedImage pintar(String c, Font fonte, Color cor, int s) {
        int w = FontHandler.getWidth(c, fonte) + 2 * s;
        int h = FontHandler.getHeight("0", fonte) + 4 * s;
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        // CONTORNO UM DEGRAU MAIS FINO QUE A ESCALA: no mundo, com a fonte já
        // multiplicada pela escala do jogo, o contorno cheio virava um bloco escuro
        // atrás de cada número, e a horda ficava coberta de tarjas.
        TextFx.draw(g, c, fonte, s, FontHandler.getHeight("0", fonte) + s, cor, Math.max(1, s - 1));
        g.dispose();
        return Acelerada.de(img);
    }
}
