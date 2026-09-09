package com.retronova.game.items;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Os selinhos de raridade e de elemento, num lugar só.
 *
 * Eles nasceram na carta de recompensa e agora precisam aparecer também no
 * inventário e na loja — que são telas escritas por outra parte do código e que
 * não têm nada a ver com a arena. Sem isto, o carregamento das imagens e a regra
 * de "qual selo vai onde" existiriam em três lugares, e o terceiro já entraria
 * ligeiramente diferente dos outros dois.
 *
 * A REGRA É SEMPRE A MESMA, e é o que este arquivo garante: raridade sempre,
 * elemento só quando houver. Um item de terra mostra dois selos; a espada inicial
 * mostra um. É essa diferença de quantidade que deixa o inventário legível de
 * relance, antes de qualquer texto ser lido.
 */
public final class Selos {

    private Selos() {
    }

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    /** Carrega um selo pelo nome, ou devolve null se ele não existir. */
    public static BufferedImage de(String nome) {
        if (nome == null) {
            return null;
        }
        return CACHE.computeIfAbsent(nome, n -> {
            try {
                return ImageIO.read(Selos.class.getResourceAsStream(
                        "/com/retronova/resources/sprites/items/elementos/" + n + ".png"));
            } catch (Exception naoTem) {
                return null;
            }
        });
    }

    /**
     * Desenha os selos de um item no CANTO SUPERIOR ESQUERDO de uma área.
     *
     * Esquerda, e não direita: no inventário o canto direito já é do número de
     * pilha dos consumíveis, e dois carimbos disputando a mesma quina fariam um
     * cobrir o outro na primeira poção empilhada.
     *
     * @param lado tamanho do selo em pixels; quem chama decide, porque um slot de
     *             inventário e uma carta de recompensa têm escalas diferentes
     */
    public static void desenhar(Graphics2D g, Item item, int x, int y, int lado) {
        if (item == null) {
            return;
        }
        int cursor = x;
        BufferedImage raridade = de(item.raridade().simbolo());
        if (raridade != null) {
            g.drawImage(raridade, cursor, y, lado, lado, null);
            cursor += lado + Math.max(1, lado / 8);
        }
        BufferedImage elemento = de(item.elemento().simbolo());
        if (elemento != null) {
            g.drawImage(elemento, cursor, y, lado, lado, null);
        }
    }
}
