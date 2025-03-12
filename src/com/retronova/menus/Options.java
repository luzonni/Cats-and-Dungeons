package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Options implements Activity {

    private Rectangle[][] quadrados;
    private final Color corBotao = new Color(0xc17564);
    private final Color corTexto = Color.WHITE;
    private final String titulo = "Options";
    private final String[][] textosBotoes = {
            {"Resolution", "Pensando", "Save"},
            {"Text Size", "Camera Size", "Full Screen"},
            {"Volume", "Music", "Mobs"},
            {null, "Back", null}
    };
    private int botaoSelecionadoLinha = -1;
    private int botaoSelecionadoColuna = -1;

    public Options() {
        inicializarBotoes();
    }

    private void inicializarBotoes() {
        quadrados = new Rectangle[4][3];

        int larguraBotao = 200;
        int alturaBotao = 50;
        int espacamento = 80;

        int larguraTotal = 3 * larguraBotao + 2 * espacamento;
        int alturaTotal = 4 * alturaBotao + 3 * espacamento;

        int xInicio = Engine.window.getWidth() / 2 - larguraTotal / 2;
        int yInicio = Engine.window.getHeight() / 2 - alturaTotal / 2 + 50;

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (textosBotoes[linha][coluna] != null) {
                    int x = xInicio + coluna * (larguraBotao + espacamento);
                    int y = yInicio + linha * (alturaBotao + espacamento);
                    quadrados[linha][coluna] = new Rectangle(x, y, larguraBotao, alturaBotao);
                }
            }
        }
    }

    @Override
    public void tick() {
        inicializarBotoes();
        atualizarBotaoSelecionado();

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null && Mouse.clickOn(Mouse_Button.LEFT, quadrados[linha][coluna])) {
                    String textoBotao = textosBotoes[linha][coluna];
                    switch (textoBotao) {
                        case "Resolution":
                            System.out.println("Clicou em Resolução");
                            break;
                        case "Back":
                            System.out.println("Clicou em Back");
                            Engine.setActivity(new Menu());
                            break;
                        default:
                            System.out.println("Botão desconhecido: " + textoBotao);
                            break;
                    }
                }
            }
        }
    }

    private void atualizarBotaoSelecionado() {
        botaoSelecionadoLinha = -1;
        botaoSelecionadoColuna = -1;
        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null && quadrados[linha][coluna].contains(Mouse.getX(), Mouse.getY())) {
                    botaoSelecionadoLinha = linha;
                    botaoSelecionadoColuna = coluna;
                    return;
                }
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        desenharTitulo(g);
        desenharTextosTeste(g);
        desenharBotoes(g);
        desenharSeta(g);
    }

    private void desenharTitulo(Graphics2D g) {
        g.setColor(corTexto);
        g.setFont(FontG.font(15 * Configs.UISCALE));
        FontMetrics fmTitulo = g.getFontMetrics();

        int x = Engine.window.getWidth() / 2 - fmTitulo.stringWidth(titulo) / 2;
        int y = 80;
        g.drawString(titulo, x, y);
    }

    private void desenharTextosTeste(Graphics2D g) {
        g.setColor(corTexto);
        g.setFont(FontG.font(11 * Configs.UISCALE));
        FontMetrics fmTeste = g.getFontMetrics();

        String[] titulosTeste = {"General", "Screen", "Sounds"};
        int[] colunasTeste = {1, 1, 1};
        int[] linhasTeste = {0, 1, 2};

        for (int i = 0; i < titulosTeste.length; i++) {
            int coluna = colunasTeste[i];
            int linha = linhasTeste[i];
            if (quadrados[linha][coluna] != null) {
                int x = quadrados[linha][coluna].x + quadrados[linha][coluna].width / 2 - fmTeste.stringWidth(titulosTeste[i]) / 2;
                int y = quadrados[linha][coluna].y - 20;
                g.drawString(titulosTeste[i], x, y);
            }
        }
    }

    private void desenharBotoes(Graphics2D g) {
        Stroke defaultStroke = g.getStroke();

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null) {
                    Rectangle quadrado = quadrados[linha][coluna];

                    int tamanhoFonte = 8 * Configs.UISCALE;
                    Font fonteAtual = FontG.font(tamanhoFonte);
                    FontMetrics fmQuadrados = g.getFontMetrics(fonteAtual);

                    while (fmQuadrados.stringWidth(textosBotoes[linha][coluna]) > quadrado.width - 20) {
                        tamanhoFonte--;
                        fonteAtual = FontG.font(tamanhoFonte);
                        fmQuadrados = g.getFontMetrics(fonteAtual);
                    }

                    RoundRectangle2D arredondar = new RoundRectangle2D.Double(quadrado.x, quadrado.y, quadrado.width, quadrado.height, 15, 15);

                    GradientPaint gradient = new GradientPaint(quadrado.x, quadrado.y, corBotao.brighter(), quadrado.x, quadrado.y + quadrado.height, corBotao.darker());

                    g.setPaint(gradient);
                    g.fill(arredondar);

                    g.setColor(corBotao.brighter().darker());
                    g.setStroke(new BasicStroke(1.0f));
                    g.draw(arredondar);

                    if (quadrado.contains(Mouse.getX(), Mouse.getY())) {
                        g.setColor(Color.WHITE);
                        g.setStroke(new BasicStroke(2.0f));
                        g.draw(arredondar);
                    }

                    g.setStroke(defaultStroke);

                    g.setColor(corTexto);
                    g.setFont(fonteAtual);
                    g.drawString(textosBotoes[linha][coluna], quadrado.x + (quadrado.width - fmQuadrados.stringWidth(textosBotoes[linha][coluna])) / 2, quadrado.y + (quadrado.height - fmQuadrados.getHeight()) / 2 + fmQuadrados.getAscent());
                }
            }
        }
    }

    private void desenharSeta(Graphics2D g) {
        if (botaoSelecionadoLinha != -1 && botaoSelecionadoColuna != -1) {
            if (quadrados[botaoSelecionadoLinha][botaoSelecionadoColuna] != null) {
                Rectangle quadradoSelecionado = quadrados[botaoSelecionadoLinha][botaoSelecionadoColuna];
                int setaX = quadradoSelecionado.x - 20;
                int setaY = quadradoSelecionado.y + quadradoSelecionado.height / 2;
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3.0f));
                g.drawLine(setaX, setaY, setaX - 10, setaY - 8);
                g.drawLine(setaX, setaY, setaX - 10, setaY + 8);
            }
        }
    }

    @Override
    public void dispose() {
    }
}