package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.menus.shared.TelaDeMenu;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * A abertura do jogo: do preto ao crédito da equipe, do crédito à logo, e da logo
 * à tela inicial, que surge em volta dela.
 *
 * ---------------------------------------------------------------------------
 * POR QUE NESTA ORDEM
 *
 * Crédito primeiro e logo depois, e não o contrário: é a convenção de quem
 * apresenta antes do que é apresentado (o "EXOK presents" antes do Celeste, o
 * estúdio antes do título em quase todo jogo), e é a única ordem que deixa a logo
 * VIRAR a tela inicial. A logo é extraída da própria ilustração (tools/GenLogo) e
 * desenhada exatamente onde ela está na arte: na última fase a cena surge em volta
 * dela, as letras pintadas aparecem por baixo das letras da abertura, e a camada
 * some sem que nada pule de lugar. Não há corte entre a abertura e o menu.
 *
 * AS FASES, em ticks de 60 Hz desenhados com Engine.alpha (iguais a 60 e a 240+
 * FPS); as durações estão no topo e somam 4,4 s na versão completa:
 *
 *   PRETO    a janela abre preta (Window) e o ar da masmorra entra.
 *   CRÉDITO  "a game by" e o nome da equipe sobem do escuro e somem.
 *   LOGO     a luz da lua acende atrás, a logo surge e um brilho a atravessa.
 *   CENA     a ilustração da tela inicial aparece em volta da logo, e a música
 *            da tela inicial entra por desvanecimento.
 *
 * Depois, o Menu assume e faz os botões surgirem um a um (Menu.entrarEmSequencia).
 *
 * VERSÃO CURTA A PARTIR DA SEGUNDA ABERTURA (Configs.IntroVista): sem crédito,
 * logo e cena mais rápidas, 1,6 s. Quem abre o jogo muitas vezes não quer ver o
 * crédito toda vez — há quem faça mod só para pular os logos de abertura —, e a
 * logo virando a tela inicial continua sendo a chegada do jogo.
 *
 * PULÁVEL SEMPRE: qualquer tecla, clique ou botão do controle leva direto à tela
 * inicial, já pronta e com a música entrando. A abertura não esconde carregamento
 * nenhum que o pulo fosse obrigar a esperar: as músicas carregam por trás (Sound)
 * e a tela inicial toca a dela assim que chegar.
 *
 * SEM CLARÃO: só desvanecimentos e um brilho de baixo contraste, longe do limite
 * de três flashes por segundo da WCAG 2.3.1.
 */
public class Abertura implements Activity {

    // ----------------------------------------------------------- o crédito

    /**
     * O CRÉDITO DA EQUIPE, num lugar só. A equipe ainda não tem nome: o texto
     * abaixo é um marcador, para trocar quando houver um. Com CREDITO_EQUIPE vazio,
     * a fase do crédito não acontece (a abertura vai do preto direto à logo).
     */
    public static final String CREDITO_ACIMA = "a game by";
    public static final String CREDITO_EQUIPE = "[team name]";

    // ----------------------------------------------------------- ajuste fino

    /** Ticks de cada fase na versão completa (60 por segundo). */
    public static final int PRETO = 18, CREDITO_ENTRA = 24, CREDITO_FICA = 42, CREDITO_SAI = 24,
            LOGO_ENTRA = 54, LOGO_FICA = 36, CENA = 66;
    /** Ticks de cada fase na versão curta, das aberturas seguintes. */
    public static final int PRETO_CURTA = 10, LOGO_ENTRA_CURTA = 30, LOGO_FICA_CURTA = 12, CENA_CURTA = 42;
    /** Ticks da música da tela inicial subindo, contados do começo da cena. */
    public static final int MUSICA_SOBE = 120;
    /**
     * O tom da logo antes de a luz passar, de 0 a 1 do branco: ela surge acinzentada
     * e a passagem da luz a deixa branca, o tom do letreiro pintado. Mudança suave de
     * luminosidade, sem clarão.
     */
    public static final float TOM_NO_ESCURO = 0.62f;
    /** Força máxima da luz da lua atrás da logo antes da cena (0 a 1). */
    public static final float LUA = 0.45f;

    /** Onde o letreiro está na ilustração (3260x1620): saída de tools/GenLogo. */
    private static final int ARTE_W = 3260, ARTE_H = 1620, LOGO_X = 502, LOGO_Y = 25;
    /** A lua da ilustração, em fração da arte (a mesma de Cenario). */
    private static final float LUA_X = 0.46f, LUA_Y = 0.17f;

    // ----------------------------------------------------------- estado

    private enum Fase { PRETO, CREDITO, LOGO, CENA }

    private final Menu menu;
    private final boolean completa;
    private Fase fase = Fase.PRETO;
    private int tempo;
    private boolean musicaPedida;
    private final long apertosNoInicio = KeyBoard.apertos();

    private static BufferedImage logoOriginal;
    private BufferedImage logo, logoNoEscuro;
    private int larguraDaLogo, alturaDaLogo;
    private static BufferedImage luz;

    /**
     * @param menu     a tela inicial, já criada e embaixo desta na pilha
     * @param completa a versão completa (primeira abertura) ou a curta
     */
    public Abertura(Menu menu, boolean completa) {
        this.menu = menu;
        this.completa = completa;
        if (logoOriginal == null) {
            logoOriginal = new SpriteHandler("icons", "logo", 1).getSHEET();
        }
        // A logo no tamanho da tela já agora, com a janela ainda preta, e não no
        // meio da fase dela, onde o preparo seria um tropeço visível.
        Rectangle r = caixaDoLetreiro(Engine.window.largura(), Engine.window.altura());
        prepararLogo(r.width, r.height);
        // A cena da tela inicial prepara por trás enquanto o crédito e a logo passam.
        TelaDeMenu.cenario().prepararEmSegundoPlano(Engine.window.largura(), Engine.window.altura());
        Sound.play(Sounds.AberturaVento);
    }

    private int duracao(Fase f) {
        return switch (f) {
            case PRETO -> completa ? PRETO : PRETO_CURTA;
            case CREDITO -> completa && !CREDITO_EQUIPE.isEmpty() ? CREDITO_ENTRA + CREDITO_FICA + CREDITO_SAI : 0;
            case LOGO -> completa ? LOGO_ENTRA + LOGO_FICA : LOGO_ENTRA_CURTA + LOGO_FICA_CURTA;
            case CENA -> completa ? CENA : CENA_CURTA;
        };
    }

    // ----------------------------------------------------------- tick

    @Override
    public void tick() {
        if (KeyBoard.apertos() != apertosNoInicio
                || Mouse.clickOn(Mouse_Button.LEFT, new Rectangle(0, 0, Engine.window.largura(), Engine.window.altura()))) {
            terminar(true);
            return;
        }
        tempo++;
        if (fase == Fase.CENA) {
            TelaDeMenu.cenario().tick();
        }
        if (tempo >= duracao(fase)) {
            switch (fase) {
                case PRETO -> entrar(Fase.CREDITO);
                case CREDITO -> entrar(Fase.LOGO);
                case LOGO -> entrar(Fase.CENA);
                case CENA -> terminar(false);
            }
        }
    }

    private void entrar(Fase nova) {
        fase = nova;
        tempo = 0;
        if (duracao(nova) == 0) {           // o crédito, sem equipe ou na versão curta
            tick0(nova);
            return;
        }
        switch (nova) {
            case CREDITO -> Sound.play(Sounds.AberturaCredito);
            case LOGO -> Sound.play(Sounds.AberturaLogo);
            case CENA -> pedirMusica(duracao(Fase.CENA) + MUSICA_SOBE);
            default -> { }
        }
    }

    /** Fase vazia: passa direto para a seguinte, no mesmo tick. */
    private void tick0(Fase vazia) {
        entrar(vazia == Fase.CREDITO ? Fase.LOGO : Fase.CENA);
    }

    /** A música da tela inicial e a ambiência, subindo do zero (pendentes se ainda carregam). */
    private void pedirMusica(int ticks) {
        if (musicaPedida) {
            return;
        }
        musicaPedida = true;
        for (Musics m : new Musics[]{Musics.Ambiente, Musics.Menu}) {
            Sound.play(m, true);
            Sound.volume(m, 0);
            Sound.desvanecer(m, 1, ticks, false);
        }
    }

    /**
     * Passa a vez para a tela inicial. Pulando, ela chega pronta (sem a entrada em
     * sequência) e a música sobe mais depressa; no fim natural, os botões surgem um
     * a um, como continuação da cena.
     */
    private void terminar(boolean pulou) {
        pedirMusica(pulou ? 30 : MUSICA_SOBE);
        if (pulou) {
            Sound.stop(Sounds.AberturaVento);
        } else {
            menu.entrarEmSequencia();
        }
        Configs.marcarIntroVista();
        Engine.backActivity();
    }

    // ----------------------------------------------------------- desenho

    @Override
    public void render(Graphics2D g) {
        int w = Engine.window.largura(), h = Engine.window.altura();
        double t = tempo + Engine.alpha();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        switch (fase) {
            case PRETO -> { }
            case CREDITO -> desenharCredito(g, w, h, t);
            case LOGO -> desenharLogo(g, w, h, t);
            case CENA -> desenharCena(g, w, h, t);
        }
    }

    private void desenharCredito(Graphics2D g, int w, int h, double t) {
        double a;
        if (t < CREDITO_ENTRA) {
            a = suave(t / CREDITO_ENTRA);
        } else if (t < CREDITO_ENTRA + CREDITO_FICA) {
            a = 1;
        } else {
            a = 1 - suave((t - CREDITO_ENTRA - CREDITO_FICA) / CREDITO_SAI);
        }
        int s = Configs.UiScale();
        Font acima = FontHandler.font(FontHandler.Game, 6f * s);
        Font equipe = FontHandler.font(FontHandler.Game, 12f * s);
        int sobe = (int) Math.round((1 - Math.min(1, t / CREDITO_ENTRA)) * 4 * s);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, a))));
        int y = h / 2 + sobe;
        String linha1 = CREDITO_ACIMA;
        TextFx.draw(g2, linha1, acima, w / 2 - FontHandler.getWidth(linha1, acima) / 2, y - 10 * s, Palette.LIGHT, s);
        TextFx.draw(g2, CREDITO_EQUIPE, equipe, w / 2 - FontHandler.getWidth(CREDITO_EQUIPE, equipe) / 2,
                y + 8 * s, Palette.TEXT, s);
        g2.dispose();
    }

    private void desenharLogo(Graphics2D g, int w, int h, double t) {
        int entra = completa ? LOGO_ENTRA : LOGO_ENTRA_CURTA;
        int fica = completa ? LOGO_FICA : LOGO_FICA_CURTA;
        double a = suave(Math.min(1, t / entra));
        desenharLua(g, w, h, (float) (LUA * a));
        // A LUZ PASSA PELA LOGO: ela surge acinzentada e, parada, uma frente de luz a
        // atravessa uma vez e a deixa branca, o tom do letreiro na ilustração.
        double luzPassou = t > entra && fica > 0 ? suave((t - entra) / fica) : 0;
        desenharPassagem(g, w, h, (float) a, luzPassou);
    }

    private void desenharCena(Graphics2D g, int w, int h, double t) {
        double c = suave(Math.min(1, t / duracao(Fase.CENA)));
        // A CENA INTEIRA E UM VÉU PRETO POR CIMA, que se abre — e não a cena com
        // transparência: as camadas dela (bruma, luz) põem a própria transparência e
        // passavam por cima da do fade, aparecendo em faixas antes da ilustração.
        TelaDeMenu.cenario().render(g);
        g.setColor(new Color(0, 0, 0, (int) Math.round(255 * (1 - c))));
        g.fillRect(0, 0, w, h);
        desenharLua(g, w, h, (float) (LUA * (1 - c)));
        // A camada das letras fica inteira enquanto a cena chega e some na segunda
        // metade, quando as letras pintadas já estão por baixo, no mesmo lugar.
        desenharLetreiro(g, w, h, (float) (1 - suave(Math.max(0, (c - 0.5) / 0.5))));
    }

    /** O letreiro onde ele está na ilustração esticada para a janela (como em Cenario). */
    private void desenharLetreiro(Graphics2D g, int w, int h, float alfa) {
        if (alfa <= 0) {
            return;
        }
        Rectangle r = caixaDoLetreiro(w, h);
        prepararLogo(r.width, r.height);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alfa)));
        g2.drawImage(logo, r.x, r.y, null);
        g2.dispose();
    }

    /**
     * A logo com a frente de luz em {@code p} (0: toda no escuro, 1: toda acesa):
     * à esquerda da frente, branca; à direita, no tom do escuro. A frente é
     * inclinada, como a luz da lua vindo do alto.
     */
    private void desenharPassagem(Graphics2D g, int w, int h, float alfa, double p) {
        Rectangle r = caixaDoLetreiro(w, h);
        prepararLogo(r.width, r.height);
        int inclinacao = r.height / 2;
        int x = r.x - inclinacao + (int) Math.round(p * (r.width + 2 * inclinacao));
        Polygon acesa = new Polygon(new int[]{r.x - inclinacao, x + inclinacao, x, r.x - inclinacao},
                new int[]{r.y, r.y, r.y + r.height, r.y + r.height}, 4);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alfa))));
        g2.drawImage(logoNoEscuro, r.x, r.y, null);
        if (p > 0) {
            g2.clip(acesa);
            g2.drawImage(logo, r.x, r.y, null);
        }
        g2.dispose();
    }

    /** A luz da lua acendendo no escuro, no lugar em que a lua está na ilustração. */
    private static void desenharLua(Graphics2D g, int w, int h, float forca) {
        if (forca <= 0) {
            return;
        }
        if (luz == null) {
            int lado = 96;
            luz = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gl = luz.createGraphics();
            Color c = Palette.ACCENT;
            gl.setPaint(new RadialGradientPaint(lado / 2f, lado / 2f, lado / 2f, new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), 120),
                            new Color(c.getRed(), c.getGreen(), c.getBlue(), 40),
                            new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            gl.fillRect(0, 0, lado, lado);
            gl.dispose();
        }
        int raio = (int) (Math.max(w, h) * 0.32);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, forca)));
        g2.drawImage(luz, (int) (w * LUA_X) - raio, (int) (h * LUA_Y) - raio, raio * 2, raio * 2, null);
        g2.dispose();
    }

    private static Rectangle caixaDoLetreiro(int w, int h) {
        int x = (int) Math.round(LOGO_X * (double) w / ARTE_W);
        int y = (int) Math.round(LOGO_Y * (double) h / ARTE_H);
        int lw = (int) Math.round(logoOriginal.getWidth() * (double) w / ARTE_W);
        int lh = (int) Math.round(logoOriginal.getHeight() * (double) h / ARTE_H);
        return new Rectangle(x, y, lw, lh);
    }

    /**
     * A logo no tamanho da tela e a versão no escuro, antes de a luz passar, refeitas só se o
     * tamanho mudar. É pintura e não pixel art (como a ilustração inteira), então
     * a redução é suavizada, igual à da Cenario: assim as duas coincidem.
     */
    private void prepararLogo(int lw, int lh) {
        if (logo != null && larguraDaLogo == lw && alturaDaLogo == lh) {
            return;
        }
        larguraDaLogo = lw;
        alturaDaLogo = lh;
        logo = new BufferedImage(Math.max(1, lw), Math.max(1, lh), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gl = logo.createGraphics();
        gl.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gl.drawImage(logoOriginal, 0, 0, lw, lh, null);
        gl.dispose();
        logoNoEscuro = new BufferedImage(logo.getWidth(), logo.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < logo.getHeight(); y++) {
            for (int x = 0; x < logo.getWidth(); x++) {
                int p = logo.getRGB(x, y);
                if ((p >>> 24) > 0) {
                    int r = (int) (((p >> 16) & 255) * TOM_NO_ESCURO), gg = (int) (((p >> 8) & 255) * TOM_NO_ESCURO);
                    int b = (int) ((p & 255) * Math.min(1f, TOM_NO_ESCURO + 0.08f));   // um pouco mais azul: luar
                    logoNoEscuro.setRGB(x, y, (p & 0xFF000000) | (r << 16) | (gg << 8) | b);
                }
            }
        }
    }

    private static double suave(double x) {
        double c = Math.max(0, Math.min(1, x));
        return c * c * (3 - 2 * c);    // smoothstep: sai e chega devagar
    }

    @Override
    public void dispose() {
    }
}
