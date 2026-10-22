package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma mão de cartas para escolher uma: o componente ÚNICO de carta do jogo.
 *
 * O PADRÃO É A CARTA DE BUFF do subir de nível: faixa colorida em cima com o
 * rótulo, medalhão claro com o ícone, nome grande, efeito na cor da carta e a
 * tecla de atalho no pé. Antes as cartas de elemento eram outra coisa — menores,
 * numa moldura de botão, com hover e entrada próprios —, e escolher um elemento
 * parecia uma tela de outro jogo. Agora as duas são esta classe, e o que muda
 * entre elas é só o conteúdo de cada carta (ver {@link Carta}).
 *
 * O COMPORTAMENTO VEM JUNTO, e não só o desenho: as cartas viram uma depois da
 * outra ao entrar, a de baixo do mouse ou do foco recebe o realce comum do jogo
 * (cresce, brilha suave na cor dela e clareia; no teclado, as setas), os
 * números 1, 2 e 3 escolhem direto, e a escolhida pisca e fica enquanto as outras
 * apagam antes de a tela fechar.
 */
public final class MaoDeCartas {

    /** Desenha o ícone de uma carta dentro do medalhão, num quadrado de lado dado. */
    @FunctionalInterface
    public interface Icone {
        void desenhar(Graphics2D g, int x, int y, int lado, Color cor);
    }

    /**
     * O conteúdo de uma carta.
     *
     * @param rotulo o texto da faixa (raridade, PASSIVE, ELEMENT...)
     * @param cor    a cor da carta: faixa, borda do medalhão, efeito e brilho
     * @param icone  quem desenha o ícone no medalhão
     * @param nome   o nome, grande
     * @param efeito o efeito, na cor da carta; pode ter várias frases separadas por \n
     */
    public record Carta(String rotulo, Color cor, Icone icone, String nome, String efeito) {
    }

    // ----------------------------------------------------------- ajuste fino

    /** Ticks até as cartas aceitarem escolha (a entrada vira cada uma 5 ticks depois da anterior). */
    public static final int ENTRADA = 26;
    /** Ticks entre escolher e fechar: a escolhida pisca e as outras apagam. */
    public static final int FECHAR = 18;
    /** Tamanho da carta, em pixels nativos. */
    public static final int LARGURA = 88, ALTURA = 128;
    /** O vão entre cartas: o bastante para as setas do foco de uma não encostarem na outra. */
    public static final int VAO = 2 * (Realce.SETA + Realce.AFASTAMENTO + 2);

    // ----------------------------------------------------------- estado

    private final List<Carta> cartas = new ArrayList<>();
    private final List<Rectangle> caixas = new ArrayList<>();
    /** O realce de cada carta, em rampa (ver Realce). */
    private final List<Realce.Estado> realces = new ArrayList<>();
    private int foco = -1;
    private int tempo;
    private int escolhido = -1;
    private int fechando;

    public MaoDeCartas(List<Carta> cartas) {
        trocar(cartas);
        tempo = 0;
    }

    /** Uma mão nova (o reroll): as cartas viram de novo, a partir do meio da entrada. */
    public void trocar(List<Carta> novas) {
        cartas.clear();
        cartas.addAll(novas);
        realces.clear();
        for (int i = 0; i < novas.size(); i++) {
            realces.add(new Realce.Estado());
        }
        caixas.clear();
        tempo = ENTRADA / 2;
        foco = Math.min(foco, cartas.size() - 1);
    }

    /**
     * Escala da mão: a da interface, reduzida se a carta mais o título e o que
     * vem embaixo dela não couberem na altura da janela.
     */
    public static int escala() {
        int cabe = Math.max(1, (Engine.window.getHeight() - 20) / 225);
        return Math.max(1, Math.min(Configs.UiScale(), cabe));
    }

    /** Centraliza a fileira na horizontal, com o topo das cartas em y. */
    public void posicionar(int y, int s) {
        int cw = LARGURA * s, ch = ALTURA * s, vao = VAO * s;
        int total = cartas.size() * cw + (cartas.size() - 1) * vao;
        int x = Engine.window.getWidth() / 2 - total / 2;
        caixas.clear();
        for (int i = 0; i < cartas.size(); i++) {
            caixas.add(new Rectangle(x + i * (cw + vao), y, cw, ch));
        }
    }

    public Rectangle caixa(int i) {
        return caixas.get(i);
    }

    public boolean escolheu() {
        return escolhido >= 0;
    }

    /** Já entrou e ainda não escolheu: o que estiver em volta da mão também aceita entrada. */
    public boolean pronta() {
        return escolhido < 0 && tempo >= ENTRADA;
    }

    /** A escolha terminou de animar: quem usa a mão pode fechar a tela. */
    public boolean fechou() {
        return escolhido >= 0 && fechando >= FECHAR;
    }

    /** O quanto a tela ainda está visível, de 1 a 0 na segunda metade do fechamento. */
    public float saida() {
        if (escolhido < 0) {
            return 1f;
        }
        return Math.clamp(1f - (fechando + Engine.alpha() - FECHAR * 0.5f) / (FECHAR * 0.5f), 0f, 1f);
    }

    /** Ticks desde que a mão abriu, com a fração do quadro, para animações de quem a usa. */
    public double quadro() {
        return tempo + Engine.alpha();
    }

    /**
     * Mouse, setas, A/D, números e Enter.
     *
     * @return o índice escolhido NESTE tick, ou -1
     */
    public int tick() {
        tempo++;
        for (int i = 0; i < realces.size(); i++) {
            realces.get(i).tick(i == foco && escolhido < 0 && tempo >= ENTRADA);
        }
        if (escolhido >= 0) {
            fechando++;
            return -1;
        }
        if (tempo < ENTRADA) {
            return -1;
        }
        for (int i = 0; i < caixas.size(); i++) {
            if (Entrada.porMouse() && caixas.get(i).contains(Mouse.getX(), Mouse.getY())) {
                if (foco != i) {
                    Sound.play(Sounds.Hover);
                }
                foco = i;
                Engine.window.pointing();
            }
            if (Mouse.clickOn(Mouse_Button.LEFT, caixas.get(i))) {
                return escolher(i);
            }
        }
        for (int i = 0; i < cartas.size(); i++) {
            if (KeyBoard.KeyPressed(String.valueOf(i + 1))) {
                return escolher(i);
            }
        }
        if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("D")) {
            foco = foco < 0 ? 0 : (foco + 1) % cartas.size();
            Sound.play(Sounds.Hover);
        } else if (KeyBoard.KeyPressed("Left") || KeyBoard.KeyPressed("A")) {
            foco = foco <= 0 ? cartas.size() - 1 : foco - 1;
            Sound.play(Sounds.Hover);
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            return escolher(foco);
        }
        return -1;
    }

    private int escolher(int i) {
        Sound.play(Sounds.Conquista);
        escolhido = i;
        foco = i;
        return i;
    }

    // ----------------------------------------------------------- desenho

    /** As cartas, com a entrada, o realce e o fechamento. */
    public void render(Graphics2D g, int s) {
        float saida = saida();
        double quadro = quadro();
        for (int i = 0; i < cartas.size() && i < caixas.size(); i++) {
            desenharCarta(g, i, s, quadro, saida);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));
    }

    private void desenharCarta(Graphics2D g, int i, int s, double quadro, float saida) {
        Carta c = cartas.get(i);
        Rectangle r = caixas.get(i);
        // Entrada: cada carta vira (escala horizontal de 0 a 1) um pouco depois da
        // anterior, com um passo além do fim e a volta (ease-out-back).
        double t = Math.clamp((quadro - i * 5) / 12.0, 0, 1);
        double c1 = 1.5, c3 = c1 + 1;
        double vira = t <= 0 ? 0 : 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        if (vira <= 0.02) {
            return;
        }
        boolean sob = i == foco;
        boolean eleita = i == escolhido;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));
        if (escolhido >= 0 && !eleita) {
            float apaga = (float) Math.min(1, (fechando + Engine.alpha()) / 6.0);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida * (1 - apaga * 0.85f)));
        }
        // O HOVER E O FOCO SÃO O PADRÃO DA TELA INICIAL (ver Realce): a carta cresce
        // a partir do centro, ganha um brilho suave por trás e clareia, em rampa.
        // Antes ela subia e ganhava um halo grosso e pulsante na cor dela — o
        // contorno grosso que o padrão aboliu, e que ficou só aqui.
        float h = eleita ? 1f : realces.get(i).noQuadro();
        double realce = 1 + h * (2.0 * Realce.CRESCIMENTO) / LARGURA;
        double cresce = eleita ? 1 + 0.08 * Math.sin(Math.min(1, fechando / 8.0) * Math.PI) : realce;
        AffineTransform antes = g.getTransform();
        g.translate(r.getCenterX(), r.getCenterY());
        g.scale(vira * cresce, cresce);
        g.translate(-r.getCenterX(), -r.getCenterY());

        int x = r.x, y = r.y, cw = r.width, ch = r.height;
        Color cor = c.cor();
        Realce.brilhoArredondado(g, new Rectangle(x - 2 * s, y - 2 * s, cw + 4 * s, ch + 4 * s), h, s, cor, 12 * s);
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(x + 2 * s, y + 4 * s, cw, ch, 10 * s, 10 * s);
        g.setColor(Palette.DARKEST);
        g.fillRoundRect(x - 2 * s, y - 2 * s, cw + 4 * s, ch + 4 * s, 12 * s, 12 * s);
        g.setPaint(new GradientPaint(0, y, Palette.DEEP, 0, y + ch, Palette.DARKEST));
        g.fillRoundRect(x, y, cw, ch, 10 * s, 10 * s);
        // Faixa do rótulo.
        g.setColor(cor);
        g.fillRoundRect(x, y, cw, 13 * s, 10 * s, 10 * s);
        g.fillRect(x, y + 7 * s, cw, 6 * s);
        Font rotulo = FontHandler.font(FontHandler.Septem, 6f * s);
        centro(g, c.rotulo(), rotulo, x + cw / 2, y + 10 * s, Palette.DARKEST, s);

        // Ícone grande num medalhão claro.
        int lado = 36 * s;
        int ix = x + (cw - lado) / 2, iy = y + 22 * s;
        g.setColor(Palette.ACCENT);
        g.fillRoundRect(ix - 5 * s, iy - 5 * s, lado + 10 * s, lado + 10 * s, 12 * s, 12 * s);
        g.setColor(cor);
        g.drawRoundRect(ix - 5 * s, iy - 5 * s, lado + 10 * s, lado + 10 * s, 12 * s, 12 * s);
        if (c.icone() != null) {
            c.icone().desenhar(g, ix, iy, lado, cor);
        }

        Font nome = FontHandler.font(FontHandler.Game, 9f * s);
        int ny = iy + lado + 18 * s;
        for (String linha : quebrar(c.nome(), nome, cw - 8 * s)) {
            centro(g, linha, nome, x + cw / 2, ny, Color.WHITE, s);
            ny += 11 * s;
        }
        Font efeito = FontHandler.font(FontHandler.Septem, 6f * s);
        ny += 2 * s;
        Color corEfeito = new Color(Math.min(255, cor.getRed() + 80), Math.min(255, cor.getGreen() + 80),
                Math.min(255, cor.getBlue() + 80));
        for (String frase : c.efeito().split("\n")) {
            for (String linha : quebrar(frase, efeito, cw - 10 * s)) {
                centro(g, linha, efeito, x + cw / 2, ny, corEfeito, s);
                ny += 8 * s;
            }
        }
        // Tecla de atalho no pé da carta.
        Font tecla = FontHandler.font(FontHandler.Septem, 6f * s);
        int tq = 10 * s;
        g.setColor(Palette.DARKEST);
        g.fillRoundRect(x + cw / 2 - tq / 2, y + ch - tq - 4 * s, tq, tq, 4 * s, 4 * s);
        centro(g, String.valueOf(i + 1), tecla, x + cw / 2, y + ch - 6 * s, sob ? cor : Palette.ACCENT, s);
        // O estado claro do realce, por cima da carta inteira.
        if (h > 0.02f && !eleita) {
            g.setColor(new Color(255, 255, 255, (int) (Realce.CLAREAMENTO * h)));
            g.fillRoundRect(x, y, cw, ch, 10 * s, 10 * s);
        }
        if (eleita && fechando < 8) {
            g.setColor(new Color(255, 255, 255, (int) (200 * (1 - fechando / 8.0))));
            g.fillRoundRect(x, y, cw, ch, 10 * s, 10 * s);
        }
        g.setTransform(antes);
        // O foco do teclado e do controle soma as setas, como em todo o jogo.
        if (sob && !eleita && escolhido < 0 && Entrada.porFoco()) {
            int cx = (int) r.getCenterX(), cy = (int) r.getCenterY();
            int w = (int) (cw * realce), hh = (int) (ch * realce);
            Realce.marcadorDeFoco(g, new Rectangle(cx - w / 2, cy - hh / 2, w, hh), s);
        }
    }

    // ----------------------------------------------------------- texto

    private static List<String> quebrar(String texto, Font fonte, int largura) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (FontHandler.getWidth(tentativa, fonte) > largura && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(tentativa);
            }
        }
        if (!atual.isEmpty()) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    /** Texto centrado; o escuro (o rótulo sobre a faixa colorida) vai sem contorno escuro. */
    private static void centro(Graphics2D g, String texto, Font fonte, int cx, int y, Color cor, int s) {
        int x = cx - FontHandler.getWidth(texto, fonte) / 2;
        if (cor.getRed() + cor.getGreen() + cor.getBlue() >= 180) {
            TextFx.draw(g, texto, fonte, x, y, cor, s);
            return;
        }
        g.setFont(fonte);
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(texto, x + 1, y + 1);
        g.setColor(cor);
        g.drawString(texto, x, y);
    }
}
