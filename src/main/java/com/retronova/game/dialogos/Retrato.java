package com.retronova.game.dialogos;

import com.retronova.engine.Engine;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * O retrato de um personagem: uma folha com uma coluna por emoção e três linhas
 * (olhos abertos, piscada, boca aberta). Ver tools/GenRetratos.java.
 *
 * FALTOU A EXPRESSÃO, VALE A NEUTRA. Personagem novo pode nascer só com a
 * coluna neutra (uma folha de 32 pixels de largura) e ganhar as outras aos
 * poucos: coluna que não existe, ou que está toda transparente, cai na primeira.
 * Folha sem a linha de piscar ou de falar usa a de olhos abertos.
 *
 * Os quadros ficam prontos e ampliados por fator inteiro, sem suavização, por
 * escala pedida: o desenho da caixa não cria imagem por quadro.
 */
public final class Retrato {

    /** Lado de um quadro na folha, em pixels de arte. */
    public static final int LADO = 32;
    /** As linhas da folha. */
    public static final int ABERTO = 0, PISCANDO = 1, FALANDO = 2;

    private final BufferedImage folha;
    private final Map<Integer, BufferedImage> prontos = new HashMap<>();

    private static final Map<String, Retrato> CARREGADOS = new HashMap<>();

    private Retrato(BufferedImage folha) {
        this.folha = folha;
    }

    /** O retrato pelo nome do PNG em sprites/retratos/, ou null se não existir. */
    public static synchronized Retrato de(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        if (CARREGADOS.containsKey(nome)) {
            return CARREGADOS.get(nome);
        }
        Retrato r = null;
        try (InputStream in = Retrato.class.getResourceAsStream(
                Engine.resPath + "sprites/retratos/" + nome + ".png")) {
            if (in != null) {
                r = new Retrato(ImageIO.read(in));
            }
        } catch (Exception falha) {
            System.err.println("[dialogos] retrato ilegivel '" + nome + "': " + falha);
        }
        CARREGADOS.put(nome, r);
        return r;
    }

    /** Monta um retrato de uma folha já carregada (para os testes). */
    static Retrato daFolha(BufferedImage folha) {
        return new Retrato(folha);
    }

    /** A coluna que a emoção usa de fato: a dela, ou a neutra se ela não existe. */
    public int coluna(Emocao e) {
        int c = e.ordinal();
        if ((c + 1) * LADO > folha.getWidth() || vazia(c, ABERTO)) {
            return 0;
        }
        return c;
    }

    /** A linha que existe de fato: a pedida, ou a de olhos abertos. */
    int linha(int linha) {
        return (linha + 1) * LADO > folha.getHeight() ? ABERTO : linha;
    }

    /** O quadro da emoção e da linha, ampliado {@code fator} vezes. */
    public BufferedImage quadro(Emocao e, int linha, int fator) {
        int c = coluna(e), l = linha(linha);
        int chave = (fator * 64 + c) * 8 + l;
        BufferedImage pronto = prontos.get(chave);
        if (pronto == null) {
            BufferedImage q = folha.getSubimage(c * LADO, l * LADO, LADO, LADO);
            pronto = com.retronova.engine.graphics.Acelerada.de(ampliar(q, Math.max(1, fator)));
            prontos.put(chave, pronto);
        }
        return pronto;
    }

    /** O quadro sem ampliar, para os testes compararem pixels. */
    BufferedImage cru(Emocao e, int linha) {
        return folha.getSubimage(coluna(e) * LADO, linha(linha) * LADO, LADO, LADO);
    }

    private boolean vazia(int coluna, int linha) {
        if ((linha + 1) * LADO > folha.getHeight()) {
            return true;
        }
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                if ((folha.getRGB(coluna * LADO + x, linha * LADO + y) >>> 24) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BufferedImage ampliar(BufferedImage q, int fator) {
        BufferedImage o = new BufferedImage(q.getWidth() * fator, q.getHeight() * fator, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(q, 0, 0, o.getWidth(), o.getHeight(), null);
        g.dispose();
        return o;
    }
}
