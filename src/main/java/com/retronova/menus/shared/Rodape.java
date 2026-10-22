package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/**
 * O rodapé de dicas de controle, igual em todas as telas de menu.
 *
 * MOSTRA O QUE O JOGADOR ESTÁ SEGURANDO. No modo controle as dicas falam em
 * botões (A, B, LB/RB); no de mouse e teclado, em teclas. É o que Forza Horizon e
 * RDR2 fazem no rodapé de todo menu, e é o que a Xbox Accessibility Guideline 112
 * pede: a interface inteira navegável pelo controle e pelo teclado — e o jogador
 * só sabe que é navegável se alguém disser como.
 *
 * Uma dica é sempre (tecla, botão, ação). A tela diz quais ações existem nela; o
 * rodapé escolhe o rótulo pelo modo de entrada e desenha tudo na mesma faixa, no
 * mesmo lugar, com o mesmo desenho de tecla.
 */
public final class Rodape {

    private Rodape() {
    }

    /** Uma ação da tela, com o rótulo para o teclado e para o controle. */
    public record Dica(String tecla, String botao, String acao) {
    }

    public static final Dica SELECIONAR = new Dica("Enter", "A", "Select");
    public static final Dica VOLTAR = new Dica("Esc", "B", "Back");
    public static final Dica ABAS = new Dica("Q/E", "LB/RB", "Tabs");
    public static final Dica AJUSTAR = new Dica("Left/Right", "D-pad", "Adjust");

    /** Altura da faixa, em pixels nativos. As telas deixam este espaço livre embaixo. */
    public static final int ALTURA = 12;

    public static void desenhar(Graphics2D g, List<Dica> dicas, float opacidade) {
        if (dicas.isEmpty() || opacidade <= 0.01f) {
            return;
        }
        int s = Configs.UiScale();
        boolean controle = Entrada.usandoControle();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        int folgaInterna = 3 * s, entreTeclaEAcao = 3 * s, entreDicas = 10 * s;
        int alturaTecla = 8 * s;

        int total = 0;
        for (int i = 0; i < dicas.size(); i++) {
            Dica d = dicas.get(i);
            String rotulo = controle ? d.botao() : d.tecla();
            total += FontHandler.getWidth(rotulo, fonte) + 2 * folgaInterna + entreTeclaEAcao
                    + FontHandler.getWidth(d.acao(), fonte);
            if (i < dicas.size() - 1) {
                total += entreDicas;
            }
        }
        int x = Engine.window.largura() / 2 - total / 2;
        int yTecla = Engine.window.altura() - Configs.Margin() - alturaTecla;
        int baseTexto = yTecla + alturaTecla - 2 * s;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                Math.max(0f, Math.min(1f, opacidade))));
        g2.setFont(fonte);
        for (Dica d : dicas) {
            String rotulo = controle ? d.botao() : d.tecla();
            int larguraTecla = FontHandler.getWidth(rotulo, fonte) + 2 * folgaInterna;
            // A tecla: um bloco do contorno com o rótulo claro, como uma capa de tecla.
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(x, yTecla + s, larguraTecla, alturaTecla, 3 * s, 3 * s);
            g2.setColor(Palette.MAIN);
            g2.fillRoundRect(x, yTecla, larguraTecla, alturaTecla, 3 * s, 3 * s);
            g2.setColor(Palette.LIGHT);
            g2.fillRect(x + 2 * s, yTecla + s, larguraTecla - 4 * s, s);
            TextFx.draw(g2, rotulo, x + folgaInterna, baseTexto, Palette.TEXT, s);
            x += larguraTecla + entreTeclaEAcao;
            TextFx.draw(g2, d.acao(), x, baseTexto, Palette.LIGHT, s);
            x += FontHandler.getWidth(d.acao(), fonte) + entreDicas;
        }
        g2.dispose();
    }
}
