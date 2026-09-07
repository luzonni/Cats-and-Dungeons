package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Dá vida à arte da tela de título sem tocar nela.
 *
 * A ARTE NÃO É ALTERADA, e agora TAMBÉM NÃO SE MOVE.
 *
 * A primeira versão movia a ilustração — parallax com o mouse, zoom lento e uma
 * respiração vertical. Não funcionou, e o motivo está na própria arte: é uma
 * multidão em plano fechado, sem céu nem horizonte para onde deslizar, e com o
 * logotipo gravado dentro dela. Deslocar uma cena assim não sugere profundidade;
 * só revela que a imagem está sendo esticada, e arrasta o título junto.
 *
 * O que sobrou é o que funciona sobre uma ilustração fechada: a cena fica parada,
 * como o artista compôs, e o movimento acontece NO AR À FRENTE DELA —
 *
 *  - bruma em duas velocidades, uma atrás e outra na frente;
 *  - poeira em suspensão subindo devagar;
 *  - a luz da lua pulsando por cima da pintura;
 *  - vinheta puxando o olho para o centro.
 *
 * É a mesma ideia da névoa parada de Hollow Knight: a silhueta não anda, o ar
 * anda. A única coisa que este código sabe sobre o conteúdo da ilustração é onde
 * fica a lua.
 */
public class Cenario {

    /** Posição da lua na ilustração, em fração da largura e da altura. */
    private static final float LUA_X = 0.46f, LUA_Y = 0.17f;

    private final String nomeArte;

    private BufferedImage arte;
    private BufferedImage bruma;
    private int larguraPreparada, alturaPreparada;

    private final Poeira[] poeiras = new Poeira[70];
    private final Random rand = new Random(0xCA7);

    private float tempo;

    public Cenario(String nomeArte) {
        this.nomeArte = nomeArte;
    }

    // ------------------------------------------------------------------- tick

    public void tick() {
        tempo += 1f / 60f;
        int w = Math.max(1, Engine.window.getWidth());
        int h = Math.max(1, Engine.window.getHeight());
        for (Poeira p : poeiras) {
            if (p != null) {
                p.tick(w, h, rand);
            }
        }
    }

    // ----------------------------------------------------------------- render

    public void render(Graphics2D g) {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        preparar(w, h);

        // 1. A ilustração, parada e no enquadramento original.
        g.drawImage(arte, 0, 0, null);

        // 2. Bruma de trás, lenta.
        desenharBruma(g, tempo * 6f, h * 0.42f, 46);

        // 3. Luz pulsando na lua.
        desenharLuz(g, w, h);

        // 4. Bruma da frente, mais rápida e mais fraca: passa entre o jogador e a
        //    cena, que é o que dá a sensação de estar dentro dela.
        desenharBruma(g, -tempo * 14f, h * 0.72f, 30);

        // 5. Poeira em suspensão.
        desenharPoeira(g);

        // 6. Vinheta, para o olho cair no centro e nos botões.
        desenharVinheta(g, w, h);
    }

    /**
     * Redimensiona a arte uma vez por tamanho de janela.
     *
     * A original tem 3260x1620. Reescalar cinco megapixels a cada quadro seria
     * desperdício puro — e como a cena não se move mais, uma cópia no tamanho da
     * tela basta para sempre.
     */
    private void preparar(int w, int h) {
        if (arte != null && larguraPreparada == w && alturaPreparada == h) {
            return;
        }
        larguraPreparada = w;
        alturaPreparada = h;
        arte = escalar(SpriteSheet.getImage(nomeArte), w, h);
        bruma = gerarBruma(Math.max(256, w / 2), Math.max(128, h / 3));
        for (int i = 0; i < poeiras.length; i++) {
            poeiras[i] = Poeira.nova(w, h, rand, true);
        }
    }

    private static BufferedImage escalar(BufferedImage origem, int w, int h) {
        BufferedImage destino = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(origem, 0, 0, w, h, null);
        g.dispose();
        return destino;
    }

    /**
     * Luz pulsando na lua da ilustração.
     *
     * Somada por cima, nunca substituindo: o brilho apenas soma sobre o que o
     * artista pintou. É o efeito mais barato da lista e um dos que mais mudam a
     * leitura da cena, porque luz que varia é a diferença entre uma cena acesa e
     * uma cena impressa.
     */
    private void desenharLuz(Graphics2D g, int w, int h) {
        float pulso = 0.55f + 0.45f * (float) ((1 + Math.sin(tempo * 0.7)) / 2);
        Point2D centro = new Point2D.Float(w * LUA_X, h * LUA_Y);
        float raio = Math.max(w, h) * 0.34f * (0.92f + 0.08f * pulso);
        int alfa = (int) (58 * pulso);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new RadialGradientPaint(centro, raio,
                new float[]{0f, 0.45f, 1f},
                new Color[]{
                        new Color(226, 232, 255, alfa),
                        new Color(180, 196, 240, alfa / 3),
                        new Color(120, 140, 200, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    /**
     * Faixa de bruma rolando na horizontal.
     *
     * A textura é gerada por código uma vez e repetida lado a lado, então não há
     * arte nova para manter. Duas passagens em velocidades diferentes bastam para
     * ler como névoa com profundidade.
     */
    private void desenharBruma(Graphics2D g, float deslocamento, float y, int alfa) {
        if (bruma == null) {
            return;
        }
        int w = Engine.window.getWidth();
        int largura = bruma.getWidth();
        int inicio = (int) (deslocamento % largura) - largura;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa / 255f));
        for (int x = inicio; x < w + largura; x += largura) {
            g2.drawImage(bruma, x, (int) y, null);
        }
        g2.dispose();
    }

    /** Ruído suave e emendável, para servir de névoa. */
    private static BufferedImage gerarBruma(int w, int h) {
        Random r = new Random(0xB014);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 26; i++) {
            int raio = h / 3 + r.nextInt(Math.max(1, h / 2));
            int cx = r.nextInt(w);
            int cy = r.nextInt(h);
            int a = 14 + r.nextInt(22);
            mancha(g, cx, cy, raio, a);
            // A mesma mancha é repetida do outro lado da folha para a emenda não
            // aparecer quando a faixa se repete.
            mancha(g, cx > w / 2 ? cx - w : cx + w, cy, raio, a);
        }
        g.dispose();
        return img;
    }

    private static void mancha(Graphics2D g, int cx, int cy, int raio, int alfa) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), raio,
                new float[]{0f, 1f},
                new Color[]{new Color(200, 214, 240, alfa), new Color(200, 214, 240, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillOval(cx - raio, cy - raio, raio * 2, raio * 2);
    }

    private void desenharPoeira(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        for (Poeira p : poeiras) {
            if (p == null) {
                continue;
            }
            int a = (int) (p.alfa * 255);
            g2.setColor(new Color(236, 240, 255, Math.max(0, Math.min(255, a))));
            g2.fillRect((int) p.x, (int) p.y, p.tamanho, p.tamanho);
        }
        g2.dispose();
    }

    private void desenharVinheta(Graphics2D g, int w, int h) {
        Point2D centro = new Point2D.Float(w / 2f, h * 0.52f);
        float raio = Math.max(w, h) * 0.72f;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new RadialGradientPaint(centro, raio,
                new float[]{0.35f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    /**
     * Partícula de poeira em suspensão.
     *
     * Sobe devagar com uma oscilação lateral, some no topo e volta embaixo. É
     * deliberadamente discreta: poeira que se nota vira chuva, e a cena não é de
     * chuva.
     */
    private static final class Poeira {
        float x, y, vy, fase, velFase, alfa, alfaMax;
        int tamanho;

        static Poeira nova(int w, int h, Random r, boolean espalhada) {
            Poeira p = new Poeira();
            p.x = r.nextInt(Math.max(1, w));
            p.y = espalhada ? r.nextInt(Math.max(1, h)) : h + r.nextInt(40);
            p.vy = 0.15f + r.nextFloat() * 0.45f;
            p.fase = r.nextFloat() * 6.28f;
            p.velFase = 0.01f + r.nextFloat() * 0.03f;
            p.alfaMax = 0.18f + r.nextFloat() * 0.35f;
            p.alfa = p.alfaMax;
            p.tamanho = Math.max(1, Configs.UiScale() / 2 + r.nextInt(2));
            return p;
        }

        void tick(int w, int h, Random r) {
            y -= vy;
            fase += velFase;
            x += (float) Math.sin(fase) * 0.35f;
            // Some ao chegar no terço superior, para não haver poeira riscando o
            // logotipo.
            float limite = h * 0.30f;
            alfa = y < limite ? alfaMax * Math.max(0, y / limite) : alfaMax;
            if (y < -8) {
                Poeira nova = nova(w, h, r, false);
                this.x = nova.x;
                this.y = nova.y;
                this.vy = nova.vy;
                this.fase = nova.fase;
                this.velFase = nova.velFase;
                this.alfaMax = nova.alfaMax;
                this.tamanho = nova.tamanho;
            }
        }
    }
}
