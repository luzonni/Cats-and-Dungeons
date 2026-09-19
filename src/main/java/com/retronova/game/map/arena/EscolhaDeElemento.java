package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.items.Elemento;
import com.retronova.menus.shared.Button;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * A ESCOLHA É OBRIGATÓRIA, como a carta de fim de turno. Sem Escape, sem clicar
 * fora: poder recusar transformaria a tela num aviso, e avisos se fecham no
 * automático.
 */
public class EscolhaDeElemento implements Activity {

    /** Quantos elementos são oferecidos. */
    private static final int QUANTOS = 3;

    /** Altura da faixa de orelhas da moldura. */
    private static final int ORELHAS = 6;

    /** Quadros de entrada, para a tela não cortar a cena no talo. */
    private static final int ENTRADA = 18;

    private final List<Elemento> oferta = new ArrayList<>();
    private final List<Rectangle> caixas = new ArrayList<>();
    private final Runnable aoEscolher;

    private int foco = -1;
    private int entrando;
    private double respiro;

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
        posicionar();
    }

    private void posicionar() {
        int s = Configs.UiScale();
        int largura = 66 * s;
        int altura = 86 * s;
        int folga = 8 * s;
        int total = oferta.size() * largura + (oferta.size() - 1) * folga;
        int x = Engine.window.getWidth() / 2 - total / 2;
        int y = Engine.window.getHeight() / 2 - altura / 2 + 4 * s;
        caixas.clear();
        for (int i = 0; i < oferta.size(); i++) {
            caixas.add(new Rectangle(x + i * (largura + folga), y, largura, altura));
        }
    }

    @Override
    public void tick() {
        posicionar();
        if (entrando < ENTRADA) {
            entrando++;
            return;
        }
        int sobre = -1;
        for (int i = 0; i < caixas.size(); i++) {
            if (caixas.get(i).contains(Mouse.getX(), Mouse.getY())) {
                sobre = i;
            }
        }
        if (sobre >= 0) {
            foco = sobre;
            Engine.window.pointing();
            if (Mouse.clickOn(Mouse_Button.LEFT, caixas.get(sobre))) {
                escolher(sobre);
                return;
            }
        }
        if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("D")) {
            foco = foco < 0 ? 0 : (foco + 1) % oferta.size();
        } else if (KeyBoard.KeyPressed("Left") || KeyBoard.KeyPressed("A")) {
            foco = foco <= 0 ? oferta.size() - 1 : foco - 1;
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            escolher(foco);
        }
    }

    private void escolher(int qual) {
        Sound.play(Sounds.Button);
        Game.getPlayer().escolherElemento(oferta.get(qual));
        Engine.pause(null);
        if (aoEscolher != null) {
            aoEscolher.run();
        }
    }

    // ------------------------------------------------------------------ desenho

    @Override
    public void render(Graphics2D g) {
        float entrada = Math.min(1f, entrando / (float) ENTRADA);
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int s = Configs.UiScale();

        g.setColor(new Color(Palette.VEIL.getRed(), Palette.VEIL.getGreen(),
                Palette.VEIL.getBlue(), (int) (Palette.VEIL.getAlpha() * entrada)));
        g.fillRect(0, 0, w, h);
        if (entrada < 1f) {
            return;
        }

        Font titulo = FontHandler.font(FontHandler.Game, 14f * s);
        String texto = "Choose your element";
        int lg = FontHandler.getWidth(texto, titulo);
        int ty = caixas.get(0).y - 16 * s;
        escrever(g, texto, w / 2 - lg / 2, ty, Palette.TEXT, s, titulo);

        Font legenda = FontHandler.font(FontHandler.Game, 5f * s);
        String aviso = "This lasts the whole run";
        escrever(g, aviso, w / 2 - FontHandler.getWidth(aviso, legenda) / 2,
                ty + 7 * s, Palette.LIGHT, s, legenda);

        for (int i = 0; i < oferta.size(); i++) {
            desenharCarta(g, i, s);
        }
    }

    /** Selos já carregados, um por elemento. */
    private static final java.util.Map<String, BufferedImage> SELOS =
            new java.util.HashMap<>();

    private static BufferedImage selo(String nome) {
        if (nome == null) {
            return null;
        }
        return SELOS.computeIfAbsent(nome, n -> {
            try {
                return javax.imageio.ImageIO.read(EscolhaDeElemento.class.getResourceAsStream(
                        "/com/retronova/resources/sprites/items/elementos/" + n + ".png"));
            } catch (Exception naoTem) {
                return null;
            }
        });
    }

    private void desenharCarta(Graphics2D g, int i, int s) {
        Rectangle r = caixas.get(i);
        boolean ativa = i == foco;
        Rectangle caixa = ativa
                ? new Rectangle(r.x - 2 * s, r.y - 2 * s, r.width + 4 * s, r.height + 4 * s)
                : r;
        Elemento e = oferta.get(i);
        Color cor = e.cor();

        // O HALO NA COR DO ELEMENTO. Aqui ele não é escada de raridade — os três
        // valem o mesmo — e sim o jeito mais rápido de a cor da escolha chegar ao
        // olho antes de qualquer palavra ser lida.
        respiro += 0.05;
        float pulso = (float) (0.75 + 0.25 * Math.sin(respiro + i));
        int camadas = 5;
        int alcance = (int) (8 * s * pulso * (ativa ? 1f : 0.55f));
        Graphics2D halo = (Graphics2D) g.create();
        halo.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        for (int k = camadas; k >= 1; k--) {
            int m = alcance * k / camadas;
            int alfa = (int) ((ativa ? 60 : 34) * pulso * (1f - (k - 1) / (float) camadas));
            halo.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(),
                    Math.max(0, alfa)));
            halo.fillRoundRect(caixa.x - m, caixa.y - m,
                    caixa.width + m * 2, caixa.height + m * 2, 6 * s, 6 * s);
        }
        halo.dispose();

        Button.frame(ativa).draw(g, ativa ? 1 : 0,
                caixa.x, caixa.y, caixa.width, caixa.height, s);

        int corpoY = caixa.y + ORELHAS * s;
        int esq = caixa.x + 5 * s;
        int dir = caixa.x + caixa.width - 5 * s;

        BufferedImage marca = selo(e.simbolo());
        int lado = 30 * s;
        if (marca != null) {
            g.drawImage(marca, caixa.x + caixa.width / 2 - lado / 2, corpoY + 3 * s,
                    lado, lado, null);
        }

        int y = corpoY + lado + 12 * s;
        Font nome = FontHandler.font(FontHandler.Game, 8f * s);
        String rotulo = e.rotulo();
        escrever(g, rotulo, caixa.x + caixa.width / 2 - FontHandler.getWidth(rotulo, nome) / 2,
                y, cor, s, nome);

        // OS DOIS NÚMEROS QUE DEFINEM O ELEMENTO, e não uma frase de sabor.
        //
        // Dano e cadência andam juntos em Elemento, de propósito: quem bate forte
        // bate devagar. É essa troca que faz a escolha ser uma decisão de estilo em
        // vez de uma de força, e ela precisa estar na carta — senão o jogador
        // escolhe pela cor e descobre o resto no susto.
        y += 10 * s;
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 100));
        g.fillRect(esq, y - 4 * s, dir - esq, s);

        Font ficha = FontHandler.font(FontHandler.Game, 5f * s);
        escrever(g, "Damage", esq, y + 4 * s, Palette.LIGHT, s, ficha);
        escreverADireita(g, porcento(e.dano(1.0)), dir, y + 4 * s, Palette.TEXT, s, ficha);
        escrever(g, "Speed", esq, y + 12 * s, Palette.LIGHT, s, ficha);
        // A cadência é um INTERVALO: menor é mais rápido. O texto inverte para o
        // jogador ler o que sente, e não o que o campo guarda.
        escreverADireita(g, porcento(2 - e.cadencia()), dir, y + 12 * s,
                Palette.TEXT, s, ficha);
    }

    /** 1.18 vira "+18%", 0.88 vira "-12%". */
    private static String porcento(double fator) {
        int p = (int) Math.round((fator - 1) * 100);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    private void escrever(Graphics2D g, String t, int x, int y, Color cor, int s, Font f) {
        g.setFont(f);
        g.setColor(Palette.OUTLINE);
        g.drawString(t, x + s, y + s);
        g.setColor(cor);
        g.drawString(t, x, y);
    }

    private void escreverADireita(Graphics2D g, String t, int dir, int y,
                                  Color cor, int s, Font f) {
        escrever(g, t, dir - FontHandler.getWidth(t, f), y, cor, s, f);
    }

    @Override
    public void dispose() {
    }
}
