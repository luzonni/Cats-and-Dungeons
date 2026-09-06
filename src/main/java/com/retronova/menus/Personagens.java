package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.entities.Player;
import com.retronova.menus.shared.Button;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Seleção de personagem.
 *
 * Os cartões seguem a mesma linguagem do resto da UI — moldura de contorno
 * navy, preenchimento na paleta, escala inteira — em vez dos retângulos
 * arredondados com gradiente que vinham de outro sistema de design.
 *
 * Clique esquerdo escolhe; clique direito vira o cartão e mostra os atributos.
 */
public class Personagens implements Activity {

    /** Dimensões do cartão em pixels nativos. */
    private static final int CARD_W = 64, CARD_H = 92, CARD_GAP = 6;
    /** Faixa de orelhas do sprite 9-slice; o conteúdo do cartão começa abaixo dela. */
    private static final int EAR_H = 6;
    /** Linha de base do nome dentro do cartão, medida a partir do fim das orelhas. */
    private static final int NOME_BASE = 9;

    private final Rectangle[] cartoes = new Rectangle[3];
    private final Player[] players = new Player[3];
    private final String[] nomes = {"Normal Cat", "Magic Cat", "Lucky Cat"};

    private int selecionado = -1;
    private final boolean[] virado = new boolean[3];
    private final float[] giro = {1, 1, 1};
    private static final float VELOCIDADE_GIRO = 0.12f;

    private final Button jogar;
    private final Button voltar;

    private BufferedImage imagemFundo;
    private int yTitulo, yInstrucao, yLore;

    public Personagens() {
        Sound.stopAll();
        Sound.play(Musics.Geral, true);

        try {
            imagemFundo = ImageIO.read(getClass().getResource(
                    "/com/retronova/resources/icons/Gato_fundo.png"));
        } catch (Exception e) {
            System.err.println("Personagens: fundo não carregado: " + e);
        }

        for (int i = 0; i < players.length; i++) {
            players[i] = Player.TEMPLATES[i];
            cartoes[i] = new Rectangle();
        }

        this.jogar = new Button(0, 0, 0, 0, "Play", b -> iniciar()).primary().meow();
        this.voltar = new Button(0, 0, 0, 0, "Back", b -> Engine.backActivity());
    }

    private void iniciar() {
        if (selecionado == -1) {
            return;         // sem personagem escolhido não há o que iniciar
        }
        Activity novoJogo = new Game(selecionado, new Room("beginning"));
        Engine.heapActivity(novoJogo, () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Sound.stop(Musics.Geral);
    }

    // ---------------------------------------------------------------- layout

    private void posicionar() {
        int s = Configs.UiScale();
        int larguraCartao = CARD_W * s;
        int alturaCartao = CARD_H * s;
        int total = 3 * larguraCartao + 2 * CARD_GAP * s;

        // Faixas verticais abaixo do cartão, em pixels nativos:
        //   +2..+4  barra de seleção
        //   +11     linha de base da lore
        //   +16     topo dos botões
        // A lore ficava em +7 e passava por baixo da barra.
        final int LORE_BASE = 11, BOTOES_TOPO = 16;

        int alturaBotao = Button.preferredHeight();
        int bloco = 16 * s + 10 * s + alturaCartao + BOTOES_TOPO * s + alturaBotao;
        int topo = Math.max(4 * s, (Engine.window.getHeight() - bloco) / 2);

        this.yTitulo = topo + 16 * s;
        this.yInstrucao = yTitulo + 8 * s;

        int x = Engine.window.getWidth() / 2 - total / 2;
        int yCartoes = yInstrucao + 6 * s;
        for (int i = 0; i < cartoes.length; i++) {
            cartoes[i] = new Rectangle(x + i * (larguraCartao + CARD_GAP * s), yCartoes,
                    larguraCartao, alturaCartao);
        }

        this.yLore = yCartoes + alturaCartao + LORE_BASE * s;

        int yBotoes = yCartoes + alturaCartao + BOTOES_TOPO * s;
        int larguraBotao = Button.preferredWidth();
        voltar.setBounds(new Rectangle(x, yBotoes, larguraBotao, alturaBotao));
        jogar.setBounds(new Rectangle(x + total - larguraBotao, yBotoes, larguraBotao, alturaBotao));
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        posicionar();

        if (KeyBoard.KeyPressed("Escape")) {
            Engine.backActivity();
            return;
        }

        for (int i = 0; i < cartoes.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, cartoes[i])) {
                Sound.play(Sounds.Cat);
                selecionado = (selecionado == i) ? -1 : i;
            }
            if (Mouse.clickOn(Mouse_Button.RIGHT, cartoes[i])) {
                virado[i] = !virado[i];
                giro[i] = 0;                       // reinicia a animação de virar
            }
            if (giro[i] < 1) {
                giro[i] = Math.min(1, giro[i] + VELOCIDADE_GIRO);
            }
            if (cartoes[i].contains(Mouse.getX(), Mouse.getY())) {
                Engine.window.pointing();
            }
        }

        if (selecionado != -1 && !virado[selecionado]) {
            players[selecionado].tick();           // anima só o cartão escolhido
        }

        if (selecionado != -1) {
            jogar.tick();
            if (jogar.isHovered()) {
                Engine.window.pointing();
            }
        }
        voltar.tick();
        if (voltar.isHovered()) {
            Engine.window.pointing();
        }
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(Graphics2D g) {
        if (imagemFundo != null) {
            g.drawImage(imagemFundo, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }
        g.setColor(Palette.VEIL);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());

        desenharTitulo(g);
        for (int i = 0; i < cartoes.length; i++) {
            desenharCartao(g, i);
        }
        desenharLore(g);
        if (selecionado != -1) {
            jogar.render(g);
        }
        voltar.render(g);
    }

    private void desenharTitulo(Graphics2D g) {
        int s = Configs.UiScale();
        texto(g, "Choose your character", FontHandler.font(FontHandler.Game, 14f * s),
                Engine.window.getWidth() / 2, yTitulo, Palette.LIGHT, true);
        texto(g, "Left click to choose  -  right click to see the stats",
                FontHandler.font(FontHandler.Game, 5f * s),
                Engine.window.getWidth() / 2, yInstrucao, Palette.MAIN, true);
    }

    private void desenharCartao(Graphics2D g, int i) {
        int s = Configs.UiScale();
        Rectangle r = cartoes[i];
        boolean escolhido = selecionado == i;
        boolean sobre = r.contains(Mouse.getX(), Mouse.getY());

        // Mesma moldura 9-slice dos botões: os cartões ganham as orelhas de graça
        // e não há uma segunda arte para manter em sincronia.
        //
        // A seleção é sinalizada pela própria moldura — variante clara e orelhas
        // em pé — mais uma barra sob o cartão. Um retângulo em volta não serve
        // aqui: ele é reto e a silhueta tem orelhas, então sobrava por fora.
        int estado = (escolhido || sobre) ? 1 : 0;
        Button.frame(escolhido).draw(g, estado, r.x, r.y, r.width, r.height, s);

        if (escolhido) {
            g.setColor(Palette.LIGHT);
            g.fillRect(r.x + 4 * s, r.y + r.height + 2 * s, r.width - 8 * s, s * 2);
        }

        // No estado selecionado a faixa de realce do topo é mais grossa, por isso
        // o nome desce um pouco em vez de encostar nela.
        texto(g, nomes[i], FontHandler.font(FontHandler.Game, 5f * s),
                r.x + r.width / 2, r.y + (EAR_H + NOME_BASE) * s, Palette.TEXT, true);

        // O cartão vira na horizontal: frente encolhe, verso cresce.
        float escala = virado[i] ? giro[i] : 1f - giro[i];
        boolean mostrandoVerso = virado[i] ? giro[i] > 0.5f : giro[i] < 0.5f;

        AffineTransform original = g.getTransform();
        g.translate(r.x + r.width / 2.0, r.y + (r.height + EAR_H * s) / 2.0);
        g.scale(Math.max(mostrandoVerso ? escala : 1f - escala, 0.02), 1);

        if (mostrandoVerso) {
            desenharAtributos(g, i, r);
        } else {
            desenharSprite(g, i, r);
        }
        g.setTransform(original);
    }

    private void desenharSprite(Graphics2D g, int i, Rectangle r) {
        BufferedImage sprite = players[i].getSprite();
        if (sprite == null) {
            return;
        }
        // Escala inteira, calculada para o sprite ocupar cerca de metade do cartão.
        int fator = Math.max(1, (r.height / 2) / sprite.getHeight());
        int w = sprite.getWidth() * fator;
        int h = sprite.getHeight() * fator;

        Graphics2D gs = (Graphics2D) g.create();
        gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gs.drawImage(sprite, -w / 2, -h / 2, w, h, null);
        gs.dispose();
    }

    private void desenharAtributos(Graphics2D g, int i, Rectangle r) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 4f * s);
        String[] info = players[i].getInfo();

        // Este bloco é desenhado dentro da transformação do virar do cartão, cuja
        // origem está no centro do corpo. Converte-se de coordenada absoluta para
        // local uma vez só, para a primeira linha cair logo abaixo do nome — antes
        // ela começava acima dele e os dois textos se sobrepunham.
        int primeiraLinha = (EAR_H + NOME_BASE + 10) * s;
        int y = primeiraLinha - (r.height + EAR_H * s) / 2;

        for (String linha : info) {
            texto(g, linha, fonte, -r.width / 2 + 7 * s, y, Palette.TEXT, false);
            y += 6 * s;
        }
    }

    private void desenharLore(Graphics2D g) {
        if (selecionado == -1) {
            return;
        }
        int s = Configs.UiScale();
        String[] lore = {
                "A common cat with balanced abilities.",
                "A magical cat with special powers.",
                "A lucky cat with unique skills."
        };
        texto(g, lore[selecionado], FontHandler.font(FontHandler.Game, 5f * s),
                Engine.window.getWidth() / 2, yLore, Palette.TEXT, true);
    }

    /** Desenha com a sombra dura de 1px que o resto da UI usa. */
    private void texto(Graphics2D g, String txt, Font fonte, int x, int y, java.awt.Color cor, boolean centralizado) {
        int s = Configs.UiScale();
        int px = centralizado ? x - FontHandler.getWidth(txt, fonte) / 2 : x;
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(txt, px + s, y + s);
        g.setColor(cor);
        g.drawString(txt, px, y);
    }

    @Override
    public void dispose() {
    }
}
