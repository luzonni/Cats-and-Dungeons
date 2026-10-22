package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.Relevo;
import com.retronova.engine.graphics.TextFx;
import com.retronova.game.Game;
import com.retronova.game.items.Elemento;
import com.retronova.menus.shared.MaoDeCartas;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
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
 * A CARTA É A MESMA DO SUBIR DE NÍVEL (ver MaoDeCartas). Antes esta tela tinha
 * cartas menores, numa moldura de botão, com halo e entrada próprios; agora a
 * única diferença é o conteúdo: a faixa diz ELEMENT, o medalhão leva o selo do
 * elemento e a cor da carta é a dele.
 */
public class EscolhaDeElemento implements Activity {

    /** Quantos elementos são oferecidos. */
    private static final int QUANTOS = 3;

    private final List<Elemento> oferta = new ArrayList<>();
    private final MaoDeCartas cartas;
    private final Runnable aoEscolher;
    private int escolhido = -1;

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
        cartas = new MaoDeCartas(oferta.stream().map(EscolhaDeElemento::carta).toList());
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
        cartas.posicionar(Engine.window.altura() / 2 - ch / 2 - 6 * s, s);
    }

    @Override
    public void tick() {
        posicionar();
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

    // ------------------------------------------------------------------ desenho

    @Override
    public void render(Graphics2D g) {
        int w = Engine.window.largura();
        int h = Engine.window.altura();
        int s = MaoDeCartas.escala();
        posicionar();
        float saida = cartas.saida();
        Composite original = g.getComposite();

        // O mesmo véu e o mesmo título das cartas de nível: duas escolhas da
        // corrida, uma tela só.
        float veu = Math.min(1f, (float) cartas.quadro() / 12f) * saida;
        g.setColor(new Color(10, 6, 18, (int) (215 * veu)));
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));

        Font titulo = FontHandler.font(FontHandler.Game, 16f * s);
        String texto = "CHOOSE YOUR ELEMENT";
        int ty = cartas.caixa(0).y - 20 * s;
        TextFx.draw(g, texto, titulo, w / 2 - FontHandler.getWidth(texto, titulo) / 2, ty, Palette.TEXT, s);
        Font sub = FontHandler.font(FontHandler.Septem, 6f * s);
        String aviso = "This lasts the whole run";
        TextFx.draw(g, aviso, sub, w / 2 - FontHandler.getWidth(aviso, sub) / 2, ty + 11 * s, Palette.ACCENT, s);

        cartas.render(g, s);
        g.setComposite(original);
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
