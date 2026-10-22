package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.Relevo;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.items.Elemento;
import com.retronova.game.map.Camera;
import com.retronova.game.objects.entities.Player;
import com.retronova.menus.shared.MaoDeCartas;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A escolha que abre a corrida: de que elemento ela vai ser.
 *
 * ---------------------------------------------------------------------------
 * POR QUE ELA EXISTE, E POR QUE AQUI
 *
 * Até agora o elemento pertencia à ARMA: ter gelo significava ter achado uma
 * espada de gelo, e a decisão acontecia na loja, uma arma de cada vez. O efeito
 * era que o elemento nunca chegava a definir uma corrida — ele era um adjetivo do
 * item que estava na mão naquele momento.
 *
 * No Hades a primeira câmara tem recompensa FORÇADA: ou um Martelo de Dédalo, ou
 * uma bênção de um olimpiano. A corrida sempre começa com uma escolha que decide
 * como ela vai ser jogada, e é dela que sai a sensação de que duas partidas com o
 * mesmo personagem são partidas diferentes. É esse o papel desta tela.
 *
 * ---------------------------------------------------------------------------
 * TRÊS DE CINCO, E NÃO OS CINCO
 *
 * O sorteio é de QUEM OFERECE, e a escolha é do jogador — a mesma divisão do
 * Hades, onde o deus que aparece é sorteado mas as bênçãos dele você escolhe.
 *
 * Oferecer os cinco seria mais generoso e pior: o jogador acharia o elemento
 * preferido dele e pegaria sempre o mesmo, e as corridas voltariam a se parecer —
 * que é exatamente o problema que os elementos existem para resolver. Três de
 * cinco garante variação entre partidas sem tirar o volante da mão de ninguém.
 *
 * ---------------------------------------------------------------------------
 * A ESCOLHA É OBRIGATÓRIA, como as cartas de nível. Sem Escape, sem clicar
 * fora: poder recusar transformaria a tela num aviso, e avisos se fecham no
 * automático.
 *
 * ---------------------------------------------------------------------------
 * O MOMENTO MAIS MARCANTE DA CORRIDA, ACIMA DO LEVEL UP
 *
 * Acontece UMA vez por corrida (aos 45 s) e muda a corrida inteira; o level up
 * acontece dezenas de vezes e muda um número. Com a mesma tela para os dois, a
 * escolha rara se apresentava como a comum — a hierarquia estava invertida.
 * Agora ela tem uma CERIMÔNIA própria, que o level up não tem:
 *
 *   ANTECIPAÇÃO  o jogo congela, a tela se fecha numa vinheta sobre o gato e um
 *                pulso sai dele; só então o título cresce. Antes de ler qualquer
 *                coisa, o jogador já sabe que este não é um level up.
 *   REVELAÇÃO    as cartas sobem uma a uma, mais espaçadas que as de buff.
 *
 * A carta é a MESMA (MaoDeCartas, o padrão das cartas de buff), com o ritmo de
 * entrada trocado. Quem já viu a cerimônia pula com Enter, Espaço, Esc, clique ou
 * A/B — a cerimônia, nunca a escolha (o Vampire Survivors faz o mesmo com o baú de
 * evolução: Esc, botão direito ou B pulam a animação, não o prêmio).
 *
 * Todos os tempos estão no topo, em "ajuste fino".
 */
public class EscolhaDeElemento implements Activity {

    /** Quantos elementos são oferecidos. */
    private static final int QUANTOS = 3;

    // ----------------------------------------------------------- ajuste fino

    /** Ticks da antecipação: vinheta fechando e pulso, antes das cartas (42 = 0,7 s). */
    public static final int ANTECIPACAO = 42;
    /** Ticks entre uma carta e a seguinte, ao entrar (as de buff usam 5). */
    public static final int ATRASO_ENTRE_CARTAS = 9;
    /** Ticks da virada de cada carta (as de buff usam 12). */
    public static final int VIRADA = 16;
    /** Quanto cada carta sobe ao entrar, em pixels nativos (as de buff não sobem). */
    public static final int SUBIDA = 40;
    /** Opacidade do véu no centro da tela (0-255): mais escuro que o do level up. */
    public static final int VEU = 175;
    /** Opacidade da vinheta nas bordas, somada ao véu (0-255). */
    public static final int VINHETA = 200;
    /** Ticks que o segundo anel do pulso espera depois do primeiro. */
    public static final int ATRASO_DO_SEGUNDO_PULSO = 10;

    private final List<Elemento> oferta = new ArrayList<>();
    private final MaoDeCartas cartas;
    private final Runnable aoEscolher;
    private int escolhido = -1;
    /** Ticks desde que a tela abriu. */
    private int t;

    public EscolhaDeElemento(Runnable aoEscolher) {
        this.aoEscolher = aoEscolher;
        List<Elemento> possiveis = new ArrayList<>();
        for (Elemento e : Elemento.values()) {
            // NENHUM não é um elemento, é a ausência de um; e LENDARIA é raridade
            // morando neste enum por conveniência de sufixo de arquivo. Nenhum dos
            // dois é uma corrida que alguém possa querer jogar.
            if (e != Elemento.NENHUM && e != Elemento.LENDARIA) {
                possiveis.add(e);
            }
        }
        Collections.shuffle(possiveis, Engine.RAND);
        oferta.addAll(possiveis.subList(0, Math.min(QUANTOS, possiveis.size())));
        cartas = new MaoDeCartas(oferta.stream().map(EscolhaDeElemento::carta).toList())
                .comEntrada(ATRASO_ENTRE_CARTAS, VIRADA, SUBIDA);
        posicionar();
    }

    /**
     * A carta de um elemento.
     *
     * OS DOIS NÚMEROS QUE DEFINEM O ELEMENTO, e não uma frase de sabor. Dano e
     * cadência andam juntos em Elemento, de propósito: quem bate forte bate
     * devagar. É essa troca que faz a escolha ser uma decisão de estilo em vez de
     * uma de força, e ela precisa estar na carta — senão o jogador escolhe pela
     * cor e descobre o resto no susto.
     */
    private static MaoDeCartas.Carta carta(Elemento e) {
        BufferedImage marca = selo(e.simbolo());
        // A cadência é um INTERVALO: menor é mais rápido. O texto inverte para o
        // jogador ler o que sente, e não o que o campo guarda.
        String efeito = porcento(e.dano(1.0)) + " damage\n" + porcento(2 - e.cadencia()) + " attack speed";
        return new MaoDeCartas.Carta("ELEMENT", e.cor(),
                // Fator inteiro e sem suavizar, com contorno e relevo: ver Relevo.
                (g, x, y, lado, cor) -> Relevo.desenhar(g, marca, x + lado / 2, y + lado / 2, lado),
                e.rotulo(), efeito);
    }

    private void posicionar() {
        int s = MaoDeCartas.escala();
        int ch = MaoDeCartas.ALTURA * s;
        cartas.posicionar(Engine.window.altura() / 2 - ch / 2 + 6 * s, s);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        t++;
        posicionar();
        if (emCerimonia() && pediuParaPular()) {
            pularCerimonia();
            return;
        }
        if (t <= ANTECIPACAO) {
            return;             // as cartas esperam a antecipação terminar
        }
        int i = cartas.tick();
        if (i >= 0) {
            escolhido = i;
        }
        if (cartas.fechou()) {
            Game.getPlayer().escolherElemento(oferta.get(escolhido));
            Engine.pause(null);
            if (aoEscolher != null) {
                aoEscolher.run();
            }
        }
    }

    /** Antecipação ou cartas ainda entrando: o trecho que se pode pular. */
    private boolean emCerimonia() {
        return t <= ANTECIPACAO || cartas.entrando();
    }

    /**
     * Enter, Espaço, Esc, clique (esquerdo ou direito), A ou B. Tudo isso é lido e
     * CONSUMIDO aqui: o mesmo aperto não chega às cartas e não escolhe nada.
     */
    private boolean pediuParaPular() {
        Rectangle tela = new Rectangle(0, 0, Engine.window.largura(), Engine.window.altura());
        return KeyBoard.KeyPressed("Enter") | KeyBoard.KeyPressed("Space") | KeyBoard.KeyPressed("Escape")
                | Mouse.clickOn(Mouse_Button.LEFT, tela) | Mouse.clickOn(Mouse_Button.RIGHT, tela);
    }

    private void pularCerimonia() {
        t = Math.max(t, ANTECIPACAO + 1);
        cartas.pularEntrada();
    }

    // ------------------------------------------------------------------ desenho

    @Override
    public void render(Graphics2D g) {
        int w = Engine.window.largura();
        int h = Engine.window.altura();
        int s = MaoDeCartas.escala();
        posicionar();
        Composite original = g.getComposite();
        double tempo = t + Engine.alpha();
        float chegada = (float) Math.min(1, tempo / ANTECIPACAO);
        float saida = cartas.saida();

        desenharEscuro(g, w, h, chegada * saida);
        Point gato = gatoNaTela();
        desenharPulso(g, gato, tempo, w, h, s);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));
        desenharTitulo(g, tempo, w, s);
        if (t > ANTECIPACAO) {
            cartas.render(g, s);
        }
        g.setComposite(original);
    }

    /**
     * O véu e a vinheta, entrando juntos.
     *
     * O LEVEL UP SÓ VELA; ESTA TELA FECHA. O véu liso diz "pausa"; a vinheta, que
     * escurece as bordas mais que o centro, puxa o olho para o meio e diz "algo vai
     * acontecer aqui" — é o recurso de antecipação mais barato que existe, e o
     * level up não o usa justamente para esta tela ter algo que ele não tem.
     */
    private void desenharEscuro(Graphics2D g, int w, int h, float quanto) {
        g.setColor(new Color(Palette.DARKEST.getRed(), Palette.DARKEST.getGreen(), Palette.DARKEST.getBlue(),
                (int) (VEU * quanto)));
        g.fillRect(0, 0, w, h);
        BufferedImage v = vinheta(w, h);
        Composite antes = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, quanto))));
        g.drawImage(v, 0, 0, null);
        g.setComposite(antes);
    }

    /** A vinheta, pintada uma vez por tamanho de tela. */
    private static BufferedImage vinheta;

    private static BufferedImage vinheta(int w, int h) {
        if (vinheta == null || vinheta.getWidth() != w || vinheta.getHeight() != h) {
            vinheta = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = vinheta.createGraphics();
            float raio = (float) Math.hypot(w, h) / 2f;
            Color borda = new Color(Palette.DARKEST.getRed(), Palette.DARKEST.getGreen(), Palette.DARKEST.getBlue(), VINHETA);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), raio,
                    new float[]{0.35f, 1f}, new Color[]{new Color(0, 0, 0, 0), borda},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
        return vinheta;
    }

    /**
     * Dois anéis que saem do gato e varrem a tela, na luz da lua da paleta.
     *
     * SAEM DO GATO, e não do centro: a escolha é sobre ELE, e é para ele que o
     * elemento escolhido volta no fim (ver a confirmação).
     */
    private void desenharPulso(Graphics2D g, Point origem, double tempo, int w, int h, int s) {
        // Até o canto mais distante do gato, para o anel sair da tela inteira.
        double alcance = Math.max(Math.max(Math.hypot(origem.x, origem.y), Math.hypot(w - origem.x, origem.y)),
                Math.max(Math.hypot(origem.x, h - origem.y), Math.hypot(w - origem.x, h - origem.y)));
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // O clarão no gato, curto, antes de o anel sair: o "estalo" da antecipação.
        double clarao = 1 - tempo / (ANTECIPACAO * 0.3);
        if (clarao > 0) {
            int r = (int) (28 * s * (1.4 - clarao));
            g2.setColor(new Color(255, 255, 255, (int) (150 * clarao)));
            g2.fillOval(origem.x - r, origem.y - r, 2 * r, 2 * r);
        }
        for (int anel = 0; anel < 2; anel++) {
            double p = (tempo - anel * ATRASO_DO_SEGUNDO_PULSO) / ANTECIPACAO;
            if (p <= 0 || p >= 1) {
                continue;
            }
            // Quase linear: o anel precisa ser VISTO andando, e não só ter passado.
            double raio = alcance * (1 - Math.pow(1 - p, 1.4));
            int alfa = (int) (230 * (1 - p));
            g2.setStroke(new BasicStroke((anel == 0 ? 4 : 2) * s));
            g2.setColor(new Color(Palette.ACCENT.getRed(), Palette.ACCENT.getGreen(), Palette.ACCENT.getBlue(), alfa));
            g2.drawOval((int) (origem.x - raio), (int) (origem.y - raio), (int) (2 * raio), (int) (2 * raio));
        }
        g2.dispose();
    }

    /**
     * O título cresce e assenta no fim da antecipação, com um halo que passa
     * pelas cores dos três elementos oferecidos.
     *
     * MAIOR QUE O DO LEVEL UP (18 contra 16 pixels nativos), e com as cores do que
     * está em jogo, e não da paleta fixa: o halo já antecipa as três cartas.
     */
    private void desenharTitulo(Graphics2D g, double tempo, int w, int s) {
        double inicio = ANTECIPACAO * 0.55;
        double p = Math.min(1, Math.max(0, (tempo - inicio) / (ANTECIPACAO - inicio)));
        if (p <= 0) {
            return;
        }
        double escala = 1 + 0.6 * Math.pow(1 - p, 3);
        Font titulo = FontHandler.font(FontHandler.Game, 18f * s);
        String texto = "CHOOSE YOUR ELEMENT";
        int tw = FontHandler.getWidth(texto, titulo);
        int ty = cartas.caixa(0).y - 22 * s;
        Graphics2D g2 = (Graphics2D) g.create();
        float base = g.getComposite() instanceof AlphaComposite a ? a.getAlpha() : 1f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) p * base));
        g2.translate(w / 2.0, ty);
        g2.scale(escala, escala);
        g2.translate(-w / 2.0, -ty);
        // Halo: as cores dos três elementos oferecidos, uma depois da outra.
        double fase = tempo * 0.03;
        Color halo = misturar(oferta.get((int) fase % oferta.size()).cor(),
                oferta.get(((int) fase + 1) % oferta.size()).cor(), fase - Math.floor(fase));
        g2.setFont(titulo);
        g2.setColor(new Color(halo.getRed(), halo.getGreen(), halo.getBlue(), 90));
        for (int d = -2; d <= 2; d += 2) {
            g2.drawString(texto, w / 2 - tw / 2 + d * s / 2, ty + d * s / 2);
        }
        TextFx.draw(g2, texto, titulo, w / 2 - tw / 2, ty, Palette.TEXT, s);
        Font sub = FontHandler.font(FontHandler.Septem, 6f * s);
        String aviso = "It shapes your whole run";
        TextFx.draw(g2, aviso, sub, w / 2 - FontHandler.getWidth(aviso, sub) / 2, ty + 12 * s, Palette.ACCENT, s);
        g2.dispose();
    }

    /** Onde o gato está na tela, para o pulso sair dele. */
    private static Point gatoNaTela() {
        Player gato = Game.getPlayer();
        if (gato == null || Game.getCam() == null) {
            return new Point(Engine.window.largura() / 2, Engine.window.altura() / 2);
        }
        Point2D p = Camera.getAt().transform(new Point2D.Double(gato.getX() + gato.getWidth() / 2.0,
                gato.getY() + gato.getHeight() / 2.0), null);
        return new Point((int) p.getX(), (int) p.getY());
    }

    private static Color misturar(Color a, Color b, double f) {
        f = Math.max(0, Math.min(1, f));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * f),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    /** Selos já carregados, um por elemento. */
    private static final Map<String, BufferedImage> SELOS = new HashMap<>();

    private static BufferedImage selo(String nome) {
        if (nome == null) {
            return null;
        }
        return SELOS.computeIfAbsent(nome, n -> {
            try {
                return ImageIO.read(EscolhaDeElemento.class.getResourceAsStream(
                        "/com/retronova/resources/sprites/items/elementos/" + n + ".png"));
            } catch (Exception naoTem) {
                return null;
            }
        });
    }

    /** 1.18 vira "+18%", 0.88 vira "-12%". */
    private static String porcento(double fator) {
        int p = (int) Math.round((fator - 1) * 100);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    @Override
    public void dispose() {
    }
}
