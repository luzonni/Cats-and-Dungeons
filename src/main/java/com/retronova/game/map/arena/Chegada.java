package com.retronova.game.map.arena;

import com.retronova.engine.graphics.Alpha;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A CHEGADA NA ARENA: o portal abre, o gato aparece, e só então a briga começa.
 *
 * O QUE ACONTECIA ANTES. O jogador caía no meio da sala já pronto, e a primeira
 * onda nascia no mesmo instante. Não havia um segundo entre estar na sala anterior
 * e estar cercado — o turno começava antes de o jogador ter olhado para ele.
 *
 * A ORDEM AQUI É O PONTO, e é o que foi pedido: primeiro o portal com o gato,
 * DEPOIS os bichos. Os dois juntos anulam um ao outro — o clarão do portal e o
 * branco dos inimigos nascendo competem pela mesma atenção, e o jogador não lê
 * nenhum dos dois. Em sequência, cada um tem a sua vez: "cheguei", e então "eles
 * estão vindo dali".
 *
 * TRÊS TEMPOS, pela mesma razão da queda na morte — o que dá peso a um momento é
 * segurá-lo, não enchê-lo de quadros:
 *
 *   ABRIR (24) — o portal cresce no chão, sozinho. O gato ainda não está lá.
 *   SURGIR (30) — ele aparece de dentro, ganhando cor, como os inimigos ganham.
 *   FECHAR (18) — o portal encolhe e some, e a arena solta as ondas.
 *
 * Enquanto isso o gato não anda e não apanha: {@link Player#morrendo()} já ensinou
 * que estado que suspende o controle precisa também suspender o dano, senão o
 * jogador perde vida numa cena em que não podia reagir.
 */
public class Chegada {

    private static final int ABRIR = 24;
    private static final int SURGIR = 30;
    private static final int FECHAR = 18;
    private static final int TOTAL = ABRIR + SURGIR + FECHAR;

    private final SpriteSheet folha;
    private int t;

    public Chegada() {
        this.folha = new SpriteSheet("sprites/objects/furniture", new String[]{"portalCistern"});
    }

    /** A chegada terminou; a arena pode soltar as ondas. */
    public boolean acabou() {
        return t >= TOTAL;
    }

    /** O gato já está visível, mesmo que o portal ainda não tenha fechado. */
    public boolean gatoVisivel() {
        return t >= ABRIR;
    }

    public void tick() {
        if (acabou()) {
            return;
        }
        // O SOM SAI NO PRIMEIRO TICK, E NAO NO CONSTRUTOR.
        //
        // A arena e construida ANTES de o jogo trocar de mapa, e a troca de mapa
        // corta todos os efeitos — para que nenhum som da sala anterior vaze para a
        // seguinte. O portal do gato nascia nesse intervalo e era cortado pela
        // propria limpeza, meio segundo depois de tocar: era por isso que a chegada
        // era silenciosa. Tocando no primeiro tick, a limpeza ja passou.
        if (t == 0) {
            Sound.play(Sounds.Portal);
        }
        t++;
        // O portal roda os proprios quadros enquanto existe.
        if (t % 6 == 0) {
            folha.plusIndex();
        }
    }

    /**
     * Quanto do gato já apareceu, de 0 a 1.
     *
     * Serve ao mesmo clarão que os inimigos usam ao nascer: um só recurso para
     * "está se formando", em vez de dois jeitos diferentes de dizer a mesma coisa.
     */
    public float formacao() {
        if (t <= ABRIR) {
            return 0f;
        }
        return Math.min(1f, (t - ABRIR) / (float) SURGIR);
    }

    /**
     * Desenha o portal no chão, sob o gato.
     *
     * ELE CRESCE E ENCOLHE em vez de aparecer inteiro: um disco que surge do nada
     * no tamanho final lê como um decalque colado ali, e não como algo abrindo.
     */
    public void render(Graphics2D g, Player jogador) {
        if (acabou()) {
            return;
        }
        float escala;
        if (t < ABRIR) {
            escala = t / (float) ABRIR;
        } else if (t < ABRIR + SURGIR) {
            escala = 1f;
        } else {
            escala = 1f - (t - ABRIR - SURGIR) / (float) FECHAR;
        }
        if (escala <= 0.01f) {
            return;
        }
        // NO CENTRO EXATO DO TILE em que o gato nasce.
        //
        // A primeira versão ancorava no PÉ do desenho, deslocada dois pixels, para
        // o portal parecer estar no chão. Ficou fora de centro: quem olha compara o
        // portal com o tile embaixo dele, e qualquer desvio salta. A caixa do gato
        // ocupa exatamente um tile, então o centro dela É o centro do tile — e é o
        // único ponto que não precisa de ajuste nenhum para bater.
        desenhar(g, folha.getSprite(),
                (int) jogador.getX() + jogador.getWidth() / 2,
                (int) jogador.getY() + jogador.getHeight() / 2, escala);
    }

    // ------------------------------------------------------------ cor do portal

    /**
     * De quem é o portal: o MESMO DESENHO, em duas cores.
     *
     * O desenho continua um só — o gato e os bichos chegam do mesmo jeito, e duas
     * animações para o mesmo acontecimento fariam o jogador procurar uma diferença
     * que não existe. A COR, porém, passou a dizer quem vem: o portal dos bichos é
     * um aviso de perigo, e o do gato (e do vendedor) não é.
     */
    public enum Tom {
        /**
         * O carmim das bandeiras da arte (ver Palette.WARN), levado ao claro.
         *
         * POR QUE VERMELHO E NÃO O VERDE DE ANTES. A ambientação nova é fria —
         * ardósia, azul-lua e roxo-noite, medidos da ilustração do título em
         * Palette —, e o verde ácido do portal era a única cor da cena que não
         * vinha dela. A arte tem UMA família quente, o vermelho das bandeiras, e
         * a interface já a reserva para aviso (Palette.WARN): é a cor que, numa
         * cena toda fria, lê como exceção. É também a convenção do gênero para
         * "inimigo vai surgir aqui" — o Brotato marca o chão com cruzes vermelhas
         * antes de cada bicho nascer —, e o jogador chega sabendo ler.
         *
         * POR QUE TÃO CLARO. O WARN da interface é escuro (é texto sobre painel);
         * sobre o chão de pedra cinza e as manchas de mato do campo ele sumiria. A
         * rampa é a da família da vida (Familia.VIDA: o vermelho das barras e do
         * golpe), do vinho ao rosa-claro, e o miolo claro mais o brilho que
         * pulsa por baixo dão contraste de VALOR, além de matiz, sobre qualquer
         * chão — o que importa no meio da horda, onde se olha de relance.
         */
        PERIGO(com.retronova.engine.graphics.Familia.VIDA.tom(com.retronova.engine.graphics.Familia.SOMBRA_FUNDA),
                com.retronova.engine.graphics.Familia.VIDA.tom(com.retronova.engine.graphics.Familia.BASE),
                com.retronova.engine.graphics.Familia.VIDA.tom(com.retronova.engine.graphics.Familia.BRILHO), true),
        /** A luz da lua da arte (Palette.DEEP → LIGHT → TEXT): chegada sem ameaça. */
        CHEGADA(com.retronova.engine.graphics.Palette.DEEP, com.retronova.engine.graphics.Palette.LIGHT,
                com.retronova.engine.graphics.Palette.TEXT, false);

        /** As três paradas da rampa: sombra, meio e luz do portal. Ajuste fino aqui. */
        final Color escura, media, clara;
        /** Brilho pulsante por baixo, para o aviso furar o chão. */
        final boolean brilha;
        private final java.util.Map<BufferedImage, BufferedImage> tingidos = new java.util.IdentityHashMap<>();

        Tom(Color escura, Color media, Color clara, boolean brilha) {
            this.escura = escura;
            this.media = media;
            this.clara = clara;
            this.brilha = brilha;
        }

        /**
         * O quadro do portal nesta cor, pintado uma vez e guardado.
         *
         * A arte é verde; aqui cada pixel vira a cor da rampa pela LUMINOSIDADE
         * dele, e o alfa fica. Assim o desenho, o sombreado e a animação continuam
         * os da arte, e trocar a cor é trocar três números.
         */
        BufferedImage tingir(BufferedImage arte) {
            return tingidos.computeIfAbsent(arte, a -> {
                BufferedImage saida = new BufferedImage(a.getWidth(), a.getHeight(), BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < a.getHeight(); y++) {
                    for (int x = 0; x < a.getWidth(); x++) {
                        int argb = a.getRGB(x, y);
                        int alfa = argb >>> 24;
                        if (alfa == 0) {
                            continue;
                        }
                        int r = (argb >> 16) & 0xFF, gr = (argb >> 8) & 0xFF, b = argb & 0xFF;
                        double l = (0.299 * r + 0.587 * gr + 0.114 * b) / 255.0;
                        Color c = l < 0.5 ? misturar(escura, media, l * 2) : misturar(media, clara, (l - 0.5) * 2);
                        saida.setRGB(x, y, (alfa << 24) | (c.getRGB() & 0xFFFFFF));
                    }
                }
                return saida;
            });
        }

        private static Color misturar(Color a, Color b, double t) {
            t = Math.max(0, Math.min(1, t));
            return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }
    }

    /** Tamanho do portal em tiles, no tamanho cheio. */
    public static final double TAMANHO = 1.4;
    /** Opacidade máxima do brilho por baixo do portal de perigo (0-255). */
    public static final int ALFA_DO_BRILHO = 70;
    /** Quanto o brilho passa da borda do portal, em fração do lado. */
    public static final double ALCANCE_DO_BRILHO = 0.35;
    /** Segundos de um pulso do brilho: rápido o bastante para ler como "agora". */
    public static final double PULSO = 0.5;

    /** O portal da chegada do gato. */
    public static void desenhar(Graphics2D g, BufferedImage arte,
                                int centroX, int centroY, float escala) {
        desenhar(g, arte, centroX, centroY, escala, Tom.CHEGADA);
    }

    /**
     * Desenha um portal centrado num ponto, na cor de quem chega. Compartilhado
     * com o nascimento dos inimigos (ver Tom).
     */
    public static void desenhar(Graphics2D g, BufferedImage arte,
                                int centroX, int centroY, float escala, Tom tom) {
        if (arte == null || escala <= 0.01f) {
            return;
        }
        int lado = (int) (GameObject.SIZE() * TAMANHO * escala);
        if (tom.brilha) {
            desenharBrilho(g, centroX, centroY, lado, escala, tom);
        }
        BufferedImage esmaecido = Alpha.getImage(tom.tingir(arte), Math.min(1f, escala + 0.2f));
        g.drawImage(esmaecido, centroX - lado / 2, centroY - lado / 2, lado, lado, null);
    }

    /**
     * Um halo achatado no chão, pulsando: elipses concêntricas de opacidade
     * decrescente, na cor do meio da rampa. É ele que segura a leitura sobre o
     * mato e sobre a pedra clara, onde o desenho sozinho perderia contraste.
     */
    private static void desenharBrilho(Graphics2D g, int cx, int cy, int lado, float escala, Tom tom) {
        double pulso = 0.6 + 0.4 * Math.sin(Game.tempoVisual() * 2 * Math.PI / PULSO);
        int camadas = 4;
        int alcance = (int) (lado * ALCANCE_DO_BRILHO);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = tom.media;
        for (int k = camadas; k >= 1; k--) {
            int m = alcance * k / camadas;
            int alfa = (int) (ALFA_DO_BRILHO * pulso * escala * (1f - (k - 1) / (float) camadas));
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alfa))));
            int w = lado + 2 * m, h = (int) ((lado + 2 * m) * 0.7);
            g2.fillOval(cx - w / 2, cy - h / 2 + lado / 8, w, h);
        }
        g2.dispose();
    }
}
