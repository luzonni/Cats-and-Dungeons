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
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
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
    /**
     * As cartas abaixo do centro e o título acima delas, em pixels nativos: a
     * folga entre o subtítulo e o topo das cartas é a do brilho e dos cantos da
     * carta em foco. A escala da mão (MaoDeCartas.escala) garante 225 de altura,
     * e 30 + 18 do título acima de 128 + 10 ainda cabe neles.
     */
    public static final int DESCIDA_DAS_CARTAS = 10, TITULO_ACIMA_DAS_CARTAS = 30;
    /** Opacidade do véu no centro da tela (0-255): mais escuro que o do level up. */
    public static final int VEU = 175;
    /** Opacidade da vinheta nas bordas, somada ao véu (0-255). */
    public static final int VINHETA = 200;
    /** Ticks que o segundo anel do pulso espera depois do primeiro. */
    public static final int ATRASO_DO_SEGUNDO_PULSO = 10;
    /** Partículas por tick em volta de cada carta, parada e com o foco. */
    public static final double FAISCAS_PARADA = 0.5, FAISCAS_NO_FOCO = 1.6;
    /** Teto de partículas na tela, para a cerimônia nunca pesar. */
    public static final int MAX_FAISCAS = 360;
    /** Opacidade da aura parada e com o foco (0-255, na camada mais interna). */
    public static final int AURA_PARADA = 30, AURA_NO_FOCO = 70;
    /** Ticks entre duas passagens do brilho diagonal pela carta. */
    public static final int PERIODO_DO_BRILHO = 150;
    /** Ticks da trilha da arena descendo, da trilha da escolha subindo e da volta. */
    public static final int SAIDA_DA_ARENA = 40, ENTRADA_DA_TRILHA = 70, VOLTA_DA_ARENA = 60;
    /** Ticks da escolha até a volta ao jogo (36 = 0,6 s), e em que tick a carta começa a voar. */
    public static final int CONFIRMACAO = 36, INICIO_DO_VOO = 10;
    /** Ticks do clarão na cor do elemento e a opacidade dele no começo (0-255). */
    public static final int CLARAO = 14, FORCA_DO_CLARAO = 150;
    /** Partículas da explosão da carta escolhida. */
    public static final int FAISCAS_DA_EXPLOSAO = 70;

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
                .comEntrada(ATRASO_ENTRE_CARTAS, VIRADA, SUBIDA)
                // Mais vão que o das cartas de buff: a moldura dupla e as pedras saem
                // 4 px para fora de cada lado, e as setas do foco precisam de espaço.
                .comVao(MaoDeCartas.VAO + 10)
                .comEnfeite(new Destaque())
                // O SOM DO ELEMENTO NO FOCO, e não o clique genérico: o jogador ouve
                // o fogo antes de escolher o fogo. É o mesmo som do golpe na corrida.
                .comSons(i -> tocar(oferta.get(i).som()), i -> {
                    tocar(oferta.get(i).somDaEscolha());
                    devolverTrilha();
                    confirmar(i);
                });
        acumulado = new double[oferta.size()];
        posicionar();
    }

    private static void tocar(com.retronova.engine.sound.Sounds som) {
        if (som != null) {
            com.retronova.engine.sound.Sound.play(som);
        }
    }

    // ----------------------------------------------------------------- áudio

    /** As faixas que tocavam antes da escolha e o volume de cada uma, para devolver. */
    private final Map<Musics, Double> trilhaDeAntes = new java.util.EnumMap<>(Musics.class);

    /**
     * Entra a trilha da escolha, sai a da arena, e o stinger marca a virada.
     *
     * CRUZAMENTO, E NÃO CORTE. A faixa da arena DESCE até o silêncio sem parar —
     * continua tocando muda, e volta do ponto em que estava, não do começo —
     * enquanto a da escolha sobe. O corte diria "mudou de tela"; o cruzamento diz
     * "o momento mudou", que é o que se quer. O stinger por cima é o acento que o
     * cruzamento sozinho não dá. Volumes respeitam as opções: Sound aplica a
     * preferência de música e de efeitos.
     */
    private void trocarTrilha() {
        Sound.play(Sounds.RevelacaoDoElemento);
        for (Musics m : Musics.values()) {
            if (m != Musics.EscolhaDeElemento && Sound.playing(m)) {
                trilhaDeAntes.put(m, Sound.fracao(m));
                Sound.desvanecer(m, 0, SAIDA_DA_ARENA, false);
            }
        }
        Sound.play(Musics.EscolhaDeElemento, true);
        Sound.volume(Musics.EscolhaDeElemento, 0);
        Sound.desvanecer(Musics.EscolhaDeElemento, 1, ENTRADA_DA_TRILHA, false);
    }

    /** Na escolha: a trilha da escolha some e a da arena volta ao volume que tinha. */
    private void devolverTrilha() {
        Sound.desvanecer(Musics.EscolhaDeElemento, 0, VOLTA_DA_ARENA, true);
        for (Map.Entry<Musics, Double> e : trilhaDeAntes.entrySet()) {
            Sound.desvanecer(e.getKey(), e.getValue(), VOLTA_DA_ARENA, false);
        }
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
        cartas.posicionar(Engine.window.altura() / 2 - ch / 2 + DESCIDA_DAS_CARTAS * s, s);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        t++;
        if (t == 1) {
            trocarTrilha();
        }
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
        moverFaiscas();
        if (escolhido < 0) {
            soltarFaiscas();
        } else {
            deixarRastro();
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
            // AS PARTÍCULAS POR TRÁS DAS CARTAS: aparecem em volta da silhueta e
            // nunca tapam o texto, que é o que o jogador está lendo para decidir.
            // Depois da escolha, a explosão e o rastro vão NA FRENTE: são o impacto.
            if (escolhido < 0) {
                desenharFaiscas(g, s);
                cartas.render(g, s);
            } else {
                cartas.render(g, s);
                desenharFaiscas(g, s);
            }
        }
        g.setComposite(original);
        desenharClarao(g, w, h);
    }

    // ------------------------------------------------------------ confirmação

    /**
     * A ESCOLHA RESPONDE FORTE E SAI RÁPIDO.
     *
     * Um clarão na cor do elemento cobre a tela, a carta explode em partículas, as
     * outras somem de vez, e a escolhida encolhe e voa até o gato, deixando rastro
     * — o elemento "entra" nele, e na chegada a arma se transmuta ali mesmo (ver
     * Transmutacao), com o anel na mesma cor. O escuro abre enquanto ela voa. Tudo
     * em 0,6 s: o destaque mora na entrada e na escolha, e uma saída arrastada só
     * atrasaria a volta à corrida ("Juice it or lose it", Jonasson e Purho: o
     * impacto é curto e forte, e o controle volta logo).
     */
    private void confirmar(int i) {
        cartas.comFechamento(CONFIRMACAO, gatoNaTela(), INICIO_DO_VOO);
        clarao = 0;
        Rectangle r = cartas.caixa(i);
        int s = MaoDeCartas.escala();
        java.util.Random rnd = Engine.RAND;
        Elemento e = oferta.get(i);
        for (int k = 0; k < FAISCAS_DA_EXPLOSAO; k++) {
            Faisca f = new Faisca();
            f.tipo = e;
            f.explosao = true;
            double ang = rnd.nextDouble() * 2 * Math.PI;
            double vel = (1.5 + rnd.nextDouble() * 3.5) * s;
            f.x = r.getCenterX() + Math.cos(ang) * r.width * 0.3;
            f.y = r.getCenterY() + Math.sin(ang) * r.height * 0.3;
            f.vx = Math.cos(ang) * vel;
            f.vy = Math.sin(ang) * vel;
            f.total = f.vida = 22 + rnd.nextInt(20);
            f.cor = rnd.nextInt(3) == 0 ? Color.WHITE : (rnd.nextBoolean() ? e.cor() : clarear(e.cor(), 80));
            faiscas.add(f);
        }
    }

    /** Ticks desde a escolha, para o clarão; -1 antes dela. */
    private int clarao = -1;

    /** O rastro da carta que voa: partículas paradas que apagam onde ela passou. */
    private void deixarRastro() {
        if (clarao >= 0) {
            clarao++;
        }
        if (cartas.fechamento() < INICIO_DO_VOO) {
            return;
        }
        Point2D c = cartas.centroNaTela(escolhido);
        Elemento e = oferta.get(escolhido);
        for (int k = 0; k < 3; k++) {
            Faisca f = new Faisca();
            f.tipo = e;
            f.explosao = true;
            f.x = c.getX() + (Engine.RAND.nextDouble() - 0.5) * 8;
            f.y = c.getY() + (Engine.RAND.nextDouble() - 0.5) * 8;
            f.total = f.vida = 14 + Engine.RAND.nextInt(8);
            f.cor = k == 0 ? Color.WHITE : e.cor();
            faiscas.add(f);
        }
    }

    private void desenharClarao(Graphics2D g, int w, int h) {
        if (clarao < 0 || escolhido < 0) {
            return;
        }
        double p = (clarao + Engine.alpha()) / CLARAO;
        if (p >= 1) {
            return;
        }
        Color c = oferta.get(escolhido).cor();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (FORCA_DO_CLARAO * (1 - p) * (1 - p))));
        g.fillRect(0, 0, w, h);
    }

    // ---------------------------------------------------- carta em destaque

    /**
     * A VARIANTE DE DESTAQUE da carta comum (ver MaoDeCartas.Enfeite).
     *
     * A HIERARQUIA SE LÊ ANTES DO TEXTO. O Hades separa a raridade das bênçãos
     * pela cor da borda, e o jogador sabe o que tem na mão antes de ler o nome; o
     * Balatro faz o mesmo com o brilho das cartas raras. Aqui a carta de elemento
     * tem o que a de buff não tem: moldura dupla na cor do elemento com pedras nos
     * cantos, uma aura que respira e um brilho que atravessa a carta de tempos em
     * tempos. O foco usa o realce comum (ver Realce) e, por cima dele, a aura cresce
     * e as partículas triplicam.
     */
    private final class Destaque implements MaoDeCartas.Enfeite {

        @Override
        public void atras(Graphics2D g, int i, Rectangle r, float realce, int s) {
            Color cor = oferta.get(i).cor();
            double respiro = 0.5 + 0.5 * Math.sin((t + Engine.alpha()) * 0.08 + i * 1.7);
            double forca = AURA_PARADA + (AURA_NO_FOCO - AURA_PARADA) * realce + 12 * respiro;
            for (int k = 6; k >= 1; k--) {
                int m = k * 3 * s;
                int alfa = (int) (forca * (1 - (k - 1) / 6.0));
                g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), Math.max(0, Math.min(255, alfa))));
                g.fillRoundRect(r.x - m, r.y - m, r.width + 2 * m, r.height + 2 * m, 12 * s + 2 * m, 12 * s + 2 * m);
            }
        }

        @Override
        public void frente(Graphics2D g, int i, Rectangle r, float realce, int s) {
            Color cor = oferta.get(i).cor();
            Color clara = clarear(cor, 70);
            // Moldura dupla: a borda na cor do elemento e um fio claro por dentro.
            g.setColor(cor);
            for (int k = 0; k < 2 * s; k++) {
                g.drawRoundRect(r.x - 4 * s + k, r.y - 4 * s + k, r.width + 8 * s - 2 * k, r.height + 8 * s - 2 * k,
                        14 * s, 14 * s);
            }
            g.setColor(new Color(clara.getRed(), clara.getGreen(), clara.getBlue(), 160));
            g.drawRoundRect(r.x - s, r.y - s, r.width + 2 * s, r.height + 2 * s, 11 * s, 11 * s);
            // Pedras nos cantos: o carimbo de "carta rara".
            int[][] cantos = {{r.x - 3 * s, r.y - 3 * s}, {r.x + r.width + 3 * s, r.y - 3 * s},
                    {r.x - 3 * s, r.y + r.height + 3 * s}, {r.x + r.width + 3 * s, r.y + r.height + 3 * s}};
            for (int[] c : cantos) {
                pedra(g, c[0], c[1], 4 * s, cor, clara);
            }
            // O brilho diagonal que atravessa a carta, preso dentro dela.
            double p = ((t + Engine.alpha() + i * 40) % PERIODO_DO_BRILHO) / 40.0;
            if (p < 1) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.clip(new java.awt.geom.RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10 * s, 10 * s));
                int x = (int) (r.x - r.height + (r.width + 2 * r.height) * p);
                int larg = 10 * s;
                g2.setColor(new Color(255, 255, 255, 46));
                g2.fillPolygon(new int[]{x, x + larg, x + larg + r.height, x + r.height},
                        new int[]{r.y + r.height, r.y + r.height, r.y, r.y}, 4);
                g2.dispose();
            }
        }

        /** Um losango de pixel art, com contorno. */
        private void pedra(Graphics2D g, int cx, int cy, int raio, Color cor, Color clara) {
            int[] xs = {cx, cx + raio, cx, cx - raio};
            int[] ys = {cy - raio, cy, cy + raio, cy};
            g.setColor(Palette.DARKEST);
            g.fillPolygon(new int[]{cx, cx + raio + 2, cx, cx - raio - 2}, new int[]{cy - raio - 2, cy, cy + raio + 2, cy}, 4);
            g.setColor(cor);
            g.fillPolygon(xs, ys, 4);
            g.setColor(clara);
            g.fillRect(cx - raio / 3, cy - raio / 2, Math.max(1, raio / 3), Math.max(1, raio / 3));
        }
    }

    // ---------------------------------------------------------- partículas

    /** Uma partícula em pixels da tela. Mexida no tick, desenhada no quadro. */
    private static final class Faisca {
        double x, y, vx, vy;
        int vida, total;
        Color cor;
        Elemento tipo;
        /** Da explosão ou do rastro: freia no ar, sem o jeito do elemento. */
        boolean explosao;
    }

    private final List<Faisca> faiscas = new ArrayList<>();
    /** Fração de partícula acumulada por carta, para taxas abaixo de uma por tick. */
    private double[] acumulado;

    /**
     * Partículas em volta das cartas que já entraram, no jeito de cada elemento.
     *
     * CADA ELEMENTO SE MEXE DO SEU JEITO, porque a cor sozinha separa mal fogo de
     * terra e gelo de água: a brasa sobe, o floco cai balançando, a bolha sobe
     * ondulando, a poeira cai pesada e o ar corre de lado. Nascem numa faixa FORA
     * da carta, para nunca taparem o texto dela.
     */
    private void soltarFaiscas() {
        int s = MaoDeCartas.escala();
        for (int i = 0; i < oferta.size(); i++) {
            if (t < ANTECIPACAO + i * ATRASO_ENTRE_CARTAS + VIRADA) {
                continue;               // ainda virando
            }
            acumulado[i] += FAISCAS_PARADA + (FAISCAS_NO_FOCO - FAISCAS_PARADA) * cartas.realce(i);
            while (acumulado[i] >= 1 && faiscas.size() < MAX_FAISCAS) {
                acumulado[i] -= 1;
                faiscas.add(nascer(oferta.get(i), cartas.caixa(i), s));
            }
            acumulado[i] = Math.min(acumulado[i], 1);
        }
    }

    private static Faisca nascer(Elemento e, Rectangle r, int s) {
        java.util.Random rnd = Engine.RAND;
        Faisca f = new Faisca();
        f.tipo = e;
        f.total = f.vida = 40 + rnd.nextInt(40);
        f.cor = rnd.nextInt(3) == 0 ? clarear(e.cor(), 90) : e.cor();
        // NASCEM FORA DA SILHUETA: nas laterais, coladas na moldura, e — conforme o
        // sentido do movimento — acima do topo ou abaixo da base, para seguirem para
        // longe da carta. Desenhadas por trás dela, as que nascessem por dentro
        // seriam invisíveis.
        double margem = 10 * s;
        boolean lateral = rnd.nextDouble() < 0.7;
        double xLateral = rnd.nextBoolean() ? r.x - 4 * s - rnd.nextDouble() * margem
                : r.x + r.width + 4 * s + rnd.nextDouble() * margem;
        double xAoLongo = r.x + rnd.nextDouble() * r.width;
        switch (e) {
            case FOGO, AGUA -> {        // sobem
                f.x = lateral ? xLateral : xAoLongo;
                f.y = lateral ? r.y + rnd.nextDouble() * r.height : r.y - 4 * s - rnd.nextDouble() * margem;
                f.vy = -(0.4 + rnd.nextDouble() * 0.7) * s;
                f.vx = (rnd.nextDouble() - 0.5) * 0.3 * s;
            }
            case GELO, TERRA -> {       // caem
                f.x = lateral ? xLateral : xAoLongo;
                f.y = lateral ? r.y + rnd.nextDouble() * r.height : r.y + r.height + 4 * s + rnd.nextDouble() * margem;
                f.vy = (e == Elemento.GELO ? 0.25 : 0.4) * s + rnd.nextDouble() * 0.3 * s;
                f.vx = (rnd.nextDouble() - 0.5) * 0.2 * s;
            }
            default -> {                // o ar corre de lado, por cima e por baixo da carta
                boolean cima = rnd.nextBoolean();
                f.x = r.x - margem * 2;
                f.y = cima ? r.y - 4 * s - rnd.nextDouble() * margem : r.y + r.height + 4 * s + rnd.nextDouble() * margem;
                f.vx = (1.2 + rnd.nextDouble()) * s;
                f.vy = (rnd.nextDouble() - 0.5) * 0.2 * s;
                f.total = f.vida = 40 + rnd.nextInt(30);
            }
        }
        return f;
    }

    private void moverFaiscas() {
        int s = MaoDeCartas.escala();
        for (int k = faiscas.size() - 1; k >= 0; k--) {
            Faisca f = faiscas.get(k);
            if (--f.vida <= 0) {
                faiscas.remove(k);
                continue;
            }
            if (f.explosao) {
                f.vx *= 0.9;                                                 // freia no ar
                f.vy *= 0.9;
            } else switch (f.tipo) {
                case GELO -> f.x += Math.sin(f.vida * 0.15) * 0.3 * s;      // o floco balança
                case AGUA -> f.x += Math.sin(f.vida * 0.25) * 0.4 * s;      // a bolha ondula
                case TERRA -> f.vy += 0.03 * s;                              // a poeira pesa
                case FOGO -> f.vx *= 0.97;                                   // a brasa assenta no ar
                default -> { }
            }
            f.x += f.vx;
            f.y += f.vy;
        }
    }

    /** Pixels de arte: quadradinhos, sem antialiasing, como o resto do jogo. */
    private void desenharFaiscas(Graphics2D g, int s) {
        for (Faisca f : faiscas) {
            double vida = f.vida / (double) f.total;
            int alfa = (int) (230 * Math.min(1, vida * 2.5));
            g.setColor(new Color(f.cor.getRed(), f.cor.getGreen(), f.cor.getBlue(), Math.max(0, alfa)));
            int lado = f.tipo == Elemento.AR ? s : (vida > 0.5 ? 2 * s : s);
            if (f.tipo == Elemento.AR) {
                g.fillRect((int) f.x, (int) f.y, 4 * s, s);          // o risco do vento
            } else {
                g.fillRect((int) f.x, (int) f.y, lado, lado);
            }
        }
    }

    private static Color clarear(Color c, int quanto) {
        return new Color(Math.min(255, c.getRed() + quanto), Math.min(255, c.getGreen() + quanto),
                Math.min(255, c.getBlue() + quanto));
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
        int ty = cartas.caixa(0).y - TITULO_ACIMA_DAS_CARTAS * s;
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
        // O SUBTÍTULO NA FONTE E NO TOM DOS TEXTOS DAS CARTAS, com folga até elas. Em
        // Septem 6 e ACCENT, colado ao topo, o brilho e os cantos da carta em foco
        // passavam por cima e ele não se lia.
        Font sub = FontHandler.font(FontHandler.Game, 7f * s);
        String aviso = "It shapes your whole run";
        TextFx.draw(g2, aviso, sub, w / 2 - FontHandler.getWidth(aviso, sub) / 2, ty + 13 * s, Palette.LIGHT, s);
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
