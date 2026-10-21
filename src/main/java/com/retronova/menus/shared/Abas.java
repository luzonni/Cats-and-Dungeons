package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.gamepad.Gamepad;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Uma fileira de abas: clique, Q/E no teclado, LB/RB no controle.
 *
 * O destaque da aba ativa DESLIZA até a nova em vez de pular, e quem usa as abas
 * pergunta {@link #direcao()} e {@link #progressoDaTroca()} para o conteúdo entrar
 * pelo lado de onde a aba nova está. Q e E porque são as teclas vizinhas de quem
 * já está com a mão no WASD, e é o par que a maioria dos jogos de PC usa para
 * trocar de aba; LB e RB pelo mesmo motivo no controle.
 */
public class Abas {

    private static final int TROCA_TICKS = 12;

    private final String[] nomes;
    private final Rectangle[] caixas;
    private int atual;
    private double destaqueX = -1, destaqueXAntes = -1;
    private int desdeATroca = Integer.MAX_VALUE;
    private int direcao = 1;

    public Abas(String... nomes) {
        this.nomes = nomes;
        this.caixas = new Rectangle[nomes.length];
        for (int i = 0; i < caixas.length; i++) {
            caixas[i] = new Rectangle();
        }
    }

    public int atual() {
        return atual;
    }

    /** Altura de uma aba, em pixels de tela. */
    public static int altura() {
        return 13 * Configs.UiScale();
    }

    /** Divide a largura dada entre as abas, lado a lado. */
    public void posicionar(int x, int y, int largura) {
        int cada = largura / nomes.length;
        for (int i = 0; i < nomes.length; i++) {
            caixas[i].setBounds(x + i * cada, y, cada, altura());
        }
    }

    /** @return true se a aba mudou neste tick */
    public boolean tick() {
        int nova = atual;
        for (int i = 0; i < caixas.length; i++) {
            if (caixas[i].contains(Mouse.getX(), Mouse.getY())) {
                Engine.window.pointing();
                if (Mouse.clickOn(Mouse_Button.LEFT, caixas[i])) {
                    nova = i;
                }
            }
        }
        int passo = Gamepad.pressionou(Gamepad.Botao.RB) || KeyBoard.KeyPressed("E") ? 1
                : Gamepad.pressionou(Gamepad.Botao.LB) || KeyBoard.KeyPressed("Q") ? -1 : 0;
        if (passo != 0) {
            nova = (atual + passo + nomes.length) % nomes.length;
        }
        boolean mudou = nova != atual;
        if (mudou) {
            direcao = nova > atual ? 1 : -1;
            if (passo != 0) {
                direcao = passo;
            }
            desdeATroca = 0;
            atual = nova;
            Sound.play(Sounds.Button);
        }
        animar();
        return mudou;
    }

    private void animar() {
        double alvo = caixas[atual].x;
        destaqueXAntes = destaqueX < 0 ? alvo : destaqueX;
        destaqueX = destaqueX < 0 ? alvo : destaqueX + (alvo - destaqueX) * 0.3;
        if (Math.abs(alvo - destaqueX) < 0.5) {
            destaqueX = alvo;
        }
        if (desdeATroca < TROCA_TICKS) {
            desdeATroca++;
        }
    }

    /** +1 se a aba nova está à direita da anterior, -1 se à esquerda. */
    public int direcao() {
        return direcao;
    }

    /** 0 no começo da troca, 1 quando acabou; ease-out cúbica, entre ticks. */
    public double progressoDaTroca() {
        if (desdeATroca >= TROCA_TICKS) {
            return 1;
        }
        double f = Math.min(1, (desdeATroca + Engine.alpha()) / TROCA_TICKS);
        return 1 - Math.pow(1 - f, 3);
    }

    public void render(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 7f * s);
        g.setFont(fonte);
        // Fundo das abas e o realce de passagem; o destaque da ativa vem por cima,
        // à parte, porque é ele que desliza de uma aba para a outra.
        for (int i = 0; i < nomes.length; i++) {
            Rectangle r = caixas[i];
            boolean sobre = r.contains(Mouse.getX(), Mouse.getY()) && i != atual;
            g.setColor(Palette.OUTLINE);
            g.fillRect(r.x, r.y, r.width, r.height);
            if (sobre) {
                g.setColor(Palette.MAIN);
                g.fillRect(r.x + s, r.y + s, r.width - 2 * s, r.height - s);
            }
        }
        Rectangle ativa = caixas[atual];
        double dx = destaqueX < 0 ? ativa.x : destaqueXAntes + (destaqueX - destaqueXAntes) * Engine.alpha();
        int hx = (int) Math.round(dx);
        g.setColor(Palette.DEEP);
        g.fillRect(hx + s, ativa.y + s, ativa.width - 2 * s, ativa.height - s);
        g.setColor(Palette.LIGHT);
        g.fillRect(hx + s, ativa.y + s, ativa.width - 2 * s, s);
        for (int i = 0; i < nomes.length; i++) {
            Rectangle r = caixas[i];
            int w = FontHandler.getWidth(nomes[i], fonte);
            int h = FontHandler.getHeight(nomes[i], fonte);
            TextFx.draw(g, nomes[i], r.x + (r.width - w) / 2, r.y + (r.height + h) / 2,
                    i == atual ? Palette.TEXT : Palette.LIGHT, s);
        }
    }
}
