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
    private final Color corBotao = Color.GRAY;
    private final Color corTexto = Color.WHITE;
    private final String titulo = "Opções";
    private final String[][] textosBotoes = {
            {"Resolução", "Full Screen", "Save"},
            {"Tamanho do Texto", "Tamanho da Câmera", "Pensando"},
            {"Volume Geral", "Volume da Música", "Volume dos Mobs"},
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
        int alturaBotao = 80;
        int espacamento = 20;

        int larguraTotal = 3 * larguraBotao + 2 * espacamento;
        int alturaTotal = 4 * alturaBotao + 3 * espacamento;

        int xInicio = Engine.window.getWidth() / 2 - larguraTotal / 2;
        int yInicio = Engine.window.getHeight() / 2 - alturaTotal / 2 + 100;

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
                        case "Resolução":
                            System.out.println("Clicou em Resolução");
                            break;
                        case "Full Screen":
                            System.out.println("Clicou em Full Screen");
                            break;
                        case "Save":
                            System.out.println("Clicou em Save");
                            break;
                        case "Tamanho do Texto":
                            System.out.println("Clicou em Tamanho do Texto");
                            break;
                        case "Tamanho da Câmera":
                            System.out.println("Clicou em Tamanho da Câmera");
                            break;
                        case "Pensando":
                            System.out.println("Clicou em Pensando");
                            break;
                        case "Volume Geral":
                            System.out.println("Clicou em Volume Geral");
                            break;
                        case "Volume da Música":
                            System.out.println("Clicou em Volume da Música");
                            break;
                        case "Volume dos Mobs":
                            System.out.println("Clicou em Volume dos Mobs");
                            break;
                        case "Back":
                            System.out.println("Clicou em Back");
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
        desenharBotoes(g);
        desenharSeta(g);
    }

    private void desenharTitulo(Graphics2D g) {
        g.setColor(corTexto);
        g.setFont(FontG.font(24 * Configs.UISCALE));
        FontMetrics fmTitulo = g.getFontMetrics();

        int x = Engine.window.getWidth() / 2 - fmTitulo.stringWidth(titulo) / 2;
        int y = 100;
        g.drawString(titulo, x, y);
    }

    private void desenharBotoes(Graphics2D g) {
        Stroke defaultStroke = g.getStroke();

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null) {
                    Rectangle quadrado = quadrados[linha][coluna];

                    int tamanhoFonte = 12 * Configs.UISCALE;
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