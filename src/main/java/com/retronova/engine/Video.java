package com.retronova.engine;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;

/**
 * Diagnóstico de vídeo, ligado com -Dcd.video=true.
 *
 * "A resolução não aplica" tem várias causas que se veem igual na tela: a escala
 * de DPI do sistema esticando a janela, a janela encolhida para caber na área
 * útil, a imagem interna de outro tamanho, o monitor errado. Este registro põe
 * os números lado a lado a cada mudança de vídeo: resolução pedida, imagem
 * interna, canvas, janela, monitor, escala de DPI e taxa do monitor.
 */
public final class Video {

    public static final boolean LIGADO = Boolean.getBoolean("cd.video");

    private Video() {
    }

    static void registrar(String momento, Window w) {
        if (!LIGADO || w == null || w.getFrame() == null) {
            return;
        }
        try {
            GraphicsConfiguration gc = w.getFrame().getGraphicsConfiguration();
            Rectangle monitor = gc.getBounds();
            Insets barras = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            AffineTransform dpi = gc.getDefaultTransform();
            DisplayMode modo = gc.getDevice().getDisplayMode();
            int[] pedida = {Configs.resolucaoLargura(), Configs.resolucaoAltura()};
            System.out.printf(java.util.Locale.ROOT,
                    "[video] %s: pedida=%dx%d imagem=%dx%d canvas=%dx%d janela=%s modo=%s%n"
                            + "[video]   monitor=%s (%s) barras=%s dpi=%.2fx%.2f (uiScale=%s) taxa=%s%n",
                    momento, pedida[0], pedida[1], w.largura(), w.altura(),
                    w.getCanvasLargura(), w.getCanvasAltura(), w.getFrame().getBounds(),
                    Configs.Fullscreen() ? "tela cheia sem bordas" : "janela",
                    monitor, gc.getDevice().getIDstring(), barras,
                    dpi.getScaleX(), dpi.getScaleY(), System.getProperty("sun.java2d.uiScale"),
                    modo.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN ? "desconhecida"
                            : modo.getRefreshRate() + " Hz");
        } catch (Exception falha) {
            System.out.println("[video] " + momento + ": " + falha);
        }
    }
}
