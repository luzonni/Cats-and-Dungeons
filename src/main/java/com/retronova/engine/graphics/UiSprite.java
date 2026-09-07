package com.retronova.engine.graphics;

import com.retronova.engine.Configs;

import java.awt.Font;
import java.awt.image.BufferedImage;

/**
 * Sprite de interface que acompanha a escala do HUD.
 *
 * O PROBLEMA QUE ELE RESOLVE
 *
 * As telas de interface escalavam o PNG uma vez, no construtor, e guardavam a
 * imagem pronta. As posições dentro delas, ao contrário, liam
 * {@code Configs.HudScale()} a cada quadro. Enquanto ninguém mexia na opção, as
 * duas coisas concordavam. Ao mudar o "HUD size" em Options, a moldura continuava
 * no tamanho antigo e os slots iam para onde a escala nova mandava — o inventário
 * e o status apareciam inteiros fora do lugar.
 *
 * Aqui a imagem é reescalada sozinha quando a escala muda, e quem desenha só
 * chama {@link #imagem()}. Não há sinal para propagar nem tela para reconstruir:
 * o conserto acontece no primeiro quadro depois do ajuste.
 */
public class UiSprite {

    private final String modulo;
    private final String nome;

    private BufferedImage imagem;
    private int escala;

    public UiSprite(String modulo, String nome) {
        this.modulo = modulo;
        this.nome = nome;
    }

    public BufferedImage imagem() {
        int atual = Configs.HudScale();
        if (imagem == null || escala != atual) {
            escala = atual;
            imagem = new SpriteHandler(modulo, nome, atual).getSHEET();
        }
        return imagem;
    }

    public int largura() {
        return imagem().getWidth();
    }

    public int altura() {
        return imagem().getHeight();
    }

    /** Fonte na escala do HUD. Derivar por quadro é barato e nunca fica velho. */
    public static Font fonte(int tipo, float tamanhoLogico) {
        return FontHandler.font(tipo, Configs.HudScale() * tamanhoLogico);
    }
}
