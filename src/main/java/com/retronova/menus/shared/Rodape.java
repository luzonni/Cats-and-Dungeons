package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Glyphs;
import com.retronova.engine.graphics.Glyphs.Glyph;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *
 * O DESENHO É O DA TELA DE CONTROLES (Glyphs): o A verde do controle, a tecla Esc
 * do teclado. Cada pedaço do rótulo separado por "/" vira um desenho; o rótulo
 * que não tem desenho (ou tem um pedaço sem) continua como antes, o texto numa
 * capa de tecla, para nenhuma dica sumir.
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
    /** Vão entre dois desenhos da mesma dica (Q E, LB RB), em pixels nativos. */
    private static final int VAO_ENTRE_GLYPHS = 1;

    /** Os desenhos de cada rótulo, já resolvidos: o rodapé é desenhado todo quadro. */
    private static final Map<String, List<Glyph>> DO_TECLADO = new HashMap<>(), DO_CONTROLE = new HashMap<>();
    private static final List<Glyph> SEM_DESENHO = List.of();

    public static void desenhar(Graphics2D g, List<Dica> dicas, float opacidade) {
        if (dicas.isEmpty() || opacidade <= 0.01f) {
            return;
        }
        int s = Configs.UiScale();
        int x = Engine.window.largura() / 2 - largura(dicas) / 2;
        int yTecla = Engine.window.altura() - Configs.Margin() - 8 * s;
        desenharEm(g, dicas, x, yTecla, opacidade);
    }

    /** Largura total das dicas, em pixels de tela, no aparelho em uso. */
    public static int largura(List<Dica> dicas) {
        int s = Configs.UiScale();
        boolean controle = Entrada.usandoControle();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        int total = 0;
        for (int i = 0; i < dicas.size(); i++) {
            Dica d = dicas.get(i);
            String rotulo = controle ? d.botao() : d.tecla();
            total += larguraDaTecla(rotulo, controle, fonte, 3 * s, s) + 3 * s
                    + FontHandler.getWidth(d.acao(), fonte);
            if (i < dicas.size() - 1) {
                total += 10 * s;
            }
        }
        return total;
    }

    /**
     * As mesmas dicas, a partir de um ponto: quem tem uma faixa própria (a caixa
     * de diálogo) desenha o "continuar" dentro dela, com os glyphs de sempre.
     *
     * @param yTecla topo da tecla; a faixa tem 8 pixels nativos de altura
     */
    public static void desenharEm(Graphics2D g, List<Dica> dicas, int x, int yTecla, float opacidade) {
        if (dicas.isEmpty() || opacidade <= 0.01f) {
            return;
        }
        int s = Configs.UiScale();
        boolean controle = Entrada.usandoControle();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        int folgaInterna = 3 * s, entreTeclaEAcao = 3 * s, entreDicas = 10 * s;
        int alturaTecla = 8 * s;
        int baseTexto = yTecla + alturaTecla - 2 * s;
        int meio = yTecla + alturaTecla / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                Math.max(0f, Math.min(1f, opacidade))));
        g2.setFont(fonte);
        for (Dica d : dicas) {
            String rotulo = controle ? d.botao() : d.tecla();
            List<Glyph> desenhos = desenhos(rotulo, controle);
            if (!desenhos.isEmpty()) {
                for (Glyph glyph : desenhos) {
                    Glyphs.desenhar(g2, glyph, x, meio - Glyphs.altura(glyph) * s / 2, s);
                    x += (Glyphs.largura(glyph) + VAO_ENTRE_GLYPHS) * s;
                }
                x -= VAO_ENTRE_GLYPHS * s;
            } else {
                int larguraTecla = FontHandler.getWidth(rotulo, fonte) + 2 * folgaInterna;
                // A tecla: um bloco do contorno com o rótulo claro, como uma capa de tecla.
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRoundRect(x, yTecla + s, larguraTecla, alturaTecla, 3 * s, 3 * s);
                g2.setColor(Palette.MAIN);
                g2.fillRoundRect(x, yTecla, larguraTecla, alturaTecla, 3 * s, 3 * s);
                g2.setColor(Palette.LIGHT);
                g2.fillRect(x + 2 * s, yTecla + s, larguraTecla - 4 * s, s);
                TextFx.draw(g2, rotulo, x + folgaInterna, baseTexto, Palette.TEXT, s);
                x += larguraTecla;
            }
            x += entreTeclaEAcao;
            TextFx.draw(g2, d.acao(), x, baseTexto, Palette.LIGHT, s);
            x += FontHandler.getWidth(d.acao(), fonte) + entreDicas;
        }
        g2.dispose();
    }

    private static int larguraDaTecla(String rotulo, boolean controle, Font fonte, int folgaInterna, int s) {
        List<Glyph> desenhos = desenhos(rotulo, controle);
        if (desenhos.isEmpty()) {
            return FontHandler.getWidth(rotulo, fonte) + 2 * folgaInterna;
        }
        int w = 0;
        for (Glyph glyph : desenhos) {
            w += (Glyphs.largura(glyph) + VAO_ENTRE_GLYPHS) * s;
        }
        return w - VAO_ENTRE_GLYPHS * s;
    }

    /** Os desenhos de um rótulo ("Q/E", "LB/RB"), ou vazio se algum pedaço não tem desenho. */
    static List<Glyph> desenhos(String rotulo, boolean controle) {
        Map<String, List<Glyph>> cache = controle ? DO_CONTROLE : DO_TECLADO;
        List<Glyph> pronto = cache.get(rotulo);
        if (pronto != null) {
            return pronto;
        }
        List<Glyph> saida = new ArrayList<>();
        for (String parte : rotulo.split("/")) {
            List<Glyph> daParte = controle ? doBotao(parte.trim()) : daTecla(parte.trim());
            if (daParte.isEmpty()) {
                saida = SEM_DESENHO;
                break;
            }
            saida.addAll(daParte);
        }
        List<Glyph> fixo = saida.isEmpty() ? SEM_DESENHO : List.copyOf(saida);
        cache.put(rotulo, fixo);
        return fixo;
    }

    private static List<Glyph> daTecla(String nome) {
        return switch (nome.toLowerCase()) {
            case "enter" -> List.of(Glyph.ENTER);
            case "esc", "escape" -> List.of(Glyph.ESC);
            case "space" -> List.of(Glyph.ESPACO);
            case "click" -> List.of(Glyph.MOUSE_ESQ);
            case "q" -> List.of(Glyph.Q);
            case "e" -> List.of(Glyph.E);
            case "f" -> List.of(Glyph.F);
            case "left" -> List.of(Glyph.ESQUERDA);
            case "right" -> List.of(Glyph.DIREITA);
            case "up" -> List.of(Glyph.CIMA);
            case "down" -> List.of(Glyph.BAIXO);
            case "arrows" -> List.of(Glyph.ESQUERDA, Glyph.CIMA, Glyph.BAIXO, Glyph.DIREITA);
            default -> List.of();
        };
    }

    private static List<Glyph> doBotao(String nome) {
        return switch (nome.toLowerCase()) {
            case "a" -> List.of(Glyph.PAD_A);
            case "b" -> List.of(Glyph.PAD_B);
            case "x" -> List.of(Glyph.PAD_X);
            case "y" -> List.of(Glyph.PAD_Y);
            case "lb" -> List.of(Glyph.PAD_LB);
            case "rb" -> List.of(Glyph.PAD_RB);
            case "lt" -> List.of(Glyph.PAD_LT);
            case "rt" -> List.of(Glyph.PAD_RT);
            case "d-pad" -> List.of(Glyph.PAD_DPAD);
            case "menu", "start" -> List.of(Glyph.PAD_MENU);
            case "view" -> List.of(Glyph.PAD_VIEW);
            default -> List.of();
        };
    }
}
