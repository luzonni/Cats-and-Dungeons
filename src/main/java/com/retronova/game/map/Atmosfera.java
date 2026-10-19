package com.retronova.game.map;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Nascente;
import com.retronova.game.objects.entities.NPCs.NPC;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.furniture.Brazier;
import com.retronova.game.objects.entities.furniture.Furniture;
import com.retronova.game.objects.entities.furniture.Torch;
import com.retronova.game.objects.entities.utilities.Bau;
import com.retronova.game.objects.entities.utilities.Projetil;
import com.retronova.game.objects.entities.utilities.Xp;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Sombra e luz: o que tira o mundo do papel.
 *
 * O MUNDO ERA CHAPADO. Todo sprite colado no chao, todo canto da arena com a
 * mesma claridade — a imagem lia como um mapa visto de cima, e nao como um lugar.
 * Duas camadas resolvem quase tudo:
 *
 * SOMBRA DE CONTATO. Uma elipse escura e macia sob os pes de quem esta de pe. E o
 * que mais separa um personagem do chao, e e quase de graca: uma imagem so,
 * pre-pintada, ampliada para cada um.
 *
 * LUZ. O ambiente escurece um pouco, e a luz abre buracos nele — em volta do
 * gato, das tochas, dos baus, das elites e do XP. O mapa de luz e pintado a um
 * quarto da resolucao e ampliado com suavizacao: e por isso que as bordas saem
 * tao macias, e e por isso que custa pouco.
 */
public final class Atmosfera {

    private Atmosfera() {
    }

    /** Escuridao do ambiente, de 0 a 255. Baixa: e clima, nao jogo de terror. */
    private static final int AMBIENTE = 125;
    private static final Color COR_AMBIENTE = new Color(12, 8, 26, AMBIENTE);
    private static final int REDUCAO = 4;

    private static java.awt.image.VolatileImage mapaDeLuz;
    private static BufferedImage sombra;

    public static boolean ligada() {
        return Configs.Lighting();
    }

    // ------------------------------------------------------------ sombras

    /** A elipse de sombra, pintada uma vez e ampliada para cada entidade. */
    private static BufferedImage sombra() {
        if (sombra == null) {
            int w = 64, h = 64;
            sombra = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sombra.createGraphics();
            g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), w / 2f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(0, 0, 0, 150), new Color(0, 0, 0, 90), new Color(0, 0, 0, 0)}));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
        return sombra;
    }

    /** Quem projeta sombra: o que esta de pe ou voando. Tapete e alcapao, nao. */
    private static boolean temSombra(Entity e) {
        if (e instanceof Nascente n && !n.corpoVisivel()) {
            return false;
        }
        return e instanceof Player || e instanceof Enemy || e instanceof NPC
                || e instanceof Bau || e instanceof Xp || e instanceof Projetil
                || (e instanceof Furniture && e.isSolid());
    }

    /**
     * Desenha as sombras de todos, ANTES das entidades: assim nenhuma sombra cai
     * por cima do corpo de outro. Chamado no espaco do mundo.
     */
    public static void renderSombras(Graphics2D g, List<Entity> entidades, float alpha) {
        if (!ligada()) {
            return;
        }
        Object antes = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        BufferedImage img = sombra();
        int s = Configs.GameScale();
        // So a parte do mundo que esta na tela (mais um tile de folga).
        java.awt.Rectangle visivel;
        try {
            visivel = g.getTransform().createInverse().createTransformedShape(new java.awt.Rectangle(
                    0, 0, Engine.window.getWidth(), Engine.window.getHeight())).getBounds();
            visivel.grow(GameObject.SIZE(), GameObject.SIZE());
        } catch (java.awt.geom.NoninvertibleTransformException semZoom) {
            visivel = null;
        }
        for (Entity e : entidades) {
            if (!temSombra(e) || (visivel != null && !e.getBounds().intersects(visivel))) {
                continue;
            }
            double[] d = e.deslocamentoNoQuadro(alpha);
            double largura = Math.max(6 * s, e.getWidth() * 1.25);
            double altura = largura * 0.38;
            // Projetil voa: a sombra fica no chao, menor e mais abaixo.
            double queda = e instanceof Projetil ? 6 * s : 0;
            if (e instanceof Projetil || e instanceof Xp) {
                largura *= 0.6;
                altura *= 0.6;
            }
            double cx = e.getX() + d[0] + e.getWidth() / 2.0;
            double base = e.getY() + d[1] + e.getHeight() + queda;
            g.drawImage(img, (int) (cx - largura / 2), (int) (base - altura / 2),
                    (int) largura, (int) altura, null);
        }
        if (antes != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, antes);
        }
    }

    // --------------------------------------------------------------- luz

    /**
     * O mapa de luz, por cima do mundo e por baixo do HUD. Em espaco de TELA.
     */
    public static void renderLuz(Graphics2D g, List<Entity> entidades, float alpha) {
        if (!ligada()) {
            return;
        }
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int lw = Math.max(1, w / REDUCAO);
        int lh = Math.max(1, h / REDUCAO);
        java.awt.GraphicsConfiguration gc = Engine.window.getGraphicsConfiguration();
        if (mapaDeLuz == null || mapaDeLuz.getWidth() != lw || mapaDeLuz.getHeight() != lh
                || mapaDeLuz.validate(gc) == java.awt.image.VolatileImage.IMAGE_INCOMPATIBLE) {
            mapaDeLuz = gc.createCompatibleVolatileImage(lw, lh, java.awt.Transparency.TRANSLUCENT);
        }
        AffineTransform cam = Camera.getAt();
        double zoom = cam.getScaleX();
        int tile = GameObject.SIZE();

        // TUDO NA GPU. A primeira versao pintava cada luz com RadialGradientPaint,
        // que o Java2D calcula pixel a pixel na CPU: medido, 14,6 ms por quadro —
        // a luz sozinha derrubava o jogo para menos de 60 FPS. Agora a mancha de
        // luz e pintada UMA vez e so colada, ampliada, num mapa que mora na placa.
        Graphics2D m = mapaDeLuz.createGraphics();
        m.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        m.setComposite(AlphaComposite.Src);
        m.setColor(COR_AMBIENTE);
        m.fillRect(0, 0, lw, lh);
        BufferedImage mancha = mancha();

        Graphics2D brilho = (Graphics2D) g.create();
        brilho.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int xpComBrilho = 0;
        for (Entity e : entidades) {
            double raio;      // em tiles
            float forca;
            Color cor = null;  // brilho colorido por cima, alem de abrir a luz
            if (e instanceof Player) {
                raio = 9.5; forca = 1f;   // visao mais ampla com a camera afastada
            } else if (e instanceof Torch || e instanceof Brazier) {
                raio = 4.5; forca = 0.95f; cor = new Color(255, 150, 60);
            } else if (e instanceof Bau) {
                raio = 2.5; forca = 0.9f; cor = new Color(255, 210, 90);
            } else if (e instanceof Enemy inimigo && inimigo.elite()) {
                raio = 2.2; forca = 0.7f; cor = new Color(255, 200, 60);
            } else if (e instanceof Xp && xpComBrilho < 80) {
                xpComBrilho++;
                raio = 0.9; forca = 0.6f; cor = new Color(120, 220, 255);
            } else {
                continue;
            }
            double[] d = e.deslocamentoNoQuadro(alpha);
            Point2D tela = cam.transform(new Point2D.Double(
                    e.getX() + d[0] + e.getWidth() / 2.0,
                    e.getY() + d[1] + e.getHeight() / 2.0), null);
            float rTela = (float) (raio * tile * zoom);
            if (tela.getX() < -rTela || tela.getY() < -rTela
                    || tela.getX() > w + rTela || tela.getY() > h + rTela) {
                continue;
            }
            float rMapa = rTela / REDUCAO;
            // Cada luz APAGA a escuridao (DstOut), na forca dela.
            m.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT, forca));
            m.drawImage(mancha, (int) (tela.getX() / REDUCAO - rMapa), (int) (tela.getY() / REDUCAO - rMapa),
                    (int) (rMapa * 2), (int) (rMapa * 2), null);
            if (cor != null) {
                brilhos.add(new Object[]{tela, rTela * 0.8f, cor});
            }
        }
        m.dispose();

        Graphics2D t = (Graphics2D) g.create();
        t.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        t.drawImage(mapaDeLuz, 0, 0, w, h, null);
        t.dispose();

        // O brilho colorido vai DEPOIS da escuridao: por baixo dela, apagava.
        for (Object[] b : brilhos) {
            Point2D p = (Point2D) b[0];
            int rb = (int) (float) b[1];
            brilho.drawImage(brilhoColorido((Color) b[2]), (int) p.getX() - rb, (int) p.getY() - rb,
                    rb * 2, rb * 2, null);
        }
        brilhos.clear();
        brilho.dispose();
    }

    private static final List<Object[]> brilhos = new java.util.ArrayList<>();

    private static BufferedImage manchaPronta;

    /** A mancha de luz: alfa cheio no meio, caindo ate zero na borda. Pintada uma vez. */
    private static BufferedImage mancha() {
        if (manchaPronta == null) {
            manchaPronta = gradiente(new Color[]{new Color(0, 0, 0, 255),
                    new Color(0, 0, 0, 170), new Color(0, 0, 0, 0)}, new float[]{0f, 0.45f, 1f});
        }
        return manchaPronta;
    }

    private static final java.util.Map<Integer, BufferedImage> BRILHOS = new java.util.HashMap<>();

    /** Um halo colorido por cor, pintado uma vez. */
    private static BufferedImage brilhoColorido(Color cor) {
        return BRILHOS.computeIfAbsent(cor.getRGB(), k -> gradiente(new Color[]{
                new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 55),
                new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 0)}, new float[]{0f, 1f}));
    }

    private static BufferedImage gradiente(Color[] cores, float[] fracoes) {
        int lado = 128;
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new RadialGradientPaint(new Point2D.Float(lado / 2f, lado / 2f), lado / 2f,
                fracoes, cores, MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, lado, lado);
        g.dispose();
        return com.retronova.engine.graphics.Acelerada.de(img);
    }
}
