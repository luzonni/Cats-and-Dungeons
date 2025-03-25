package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Options implements Activity {

    private Rectangle[][] quadrados;
    private final Color corTexto = Color.WHITE;
    private final String titulo = "Options";
    private final String[][] textosBotoes = {
            {"Res", "FPS", "Save"},
            {"Text Size", "Margin", "Full Screen"},
            {"Volume", "Music", "Mobs"},
            {null, "Save Changes", "Back"}
    };
    private int botaoSelecionadoLinha = -1;
    private int botaoSelecionadoColuna = -1;

    private int tempFps = Configs.MaxFrames();
    private int[] fpsOptions = {30, 60, 120};
    private int fpsIndex = 0;

    private boolean tempFullScreen = Configs.Fullscreen();

    private int resolutionIndex = Engine.index_res;
    private int[] tempResolution = Engine.resolutions[resolutionIndex];

    private int tempUiScale = Configs.UiScale();

    private int tempMargin = Configs.Margin();

    private int tempMusicVolume = Configs.Music();
    private int tempMobsVolume = Configs.Volum();

    public Options() {
        inicializarBotoes();
        atualizarTextosBotoes();
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

    private void atualizarTextosBotoes() {
        textosBotoes[0][0] = "Res: " + tempResolution[0] + "x" + tempResolution[1];
        textosBotoes[0][1] = "FPS: " + tempFps;
        textosBotoes[1][0] = "Text Size: " + tempUiScale;
        textosBotoes[1][1] = "Margin: " + tempMargin;
        textosBotoes[1][2] = "Full Screen: " + (tempFullScreen ? "On" : "Off");
        textosBotoes[2][1] = "Music: " + tempMusicVolume;
        textosBotoes[2][2] = "Mobs: " + tempMobsVolume;
    }

    @Override
    public void tick() {
        inicializarBotoes();
        atualizarBotaoSelecionado();

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null) {
                    boolean clickEsquerdo = Mouse.clickOn(Mouse_Button.LEFT, quadrados[linha][coluna]);
                    boolean clickDireito = Mouse.clickOn(Mouse_Button.RIGHT, quadrados[linha][coluna]);

                    if (clickEsquerdo || clickDireito) {
                        String textoBotao = textosBotoes[linha][coluna];
                        int direcao = clickEsquerdo ? 1 : -1;

                        switch (textoBotao.split(":")[0].trim()) {
                            case "Res":
                                resolutionIndex = (resolutionIndex + direcao + Engine.resolutions.length) % Engine.resolutions.length;
                                tempResolution = Engine.resolutions[resolutionIndex];
                                break;
                            case "FPS":
                                fpsIndex = (fpsIndex + direcao + fpsOptions.length) % fpsOptions.length;
                                tempFps = fpsOptions[fpsIndex];
                                break;
                            case "Text Size":
                                tempUiScale = ((tempUiScale - 1 + direcao + 3) % 3) + 1;
                                break;
                            case "Margin":
                                tempMargin += direcao * 5;
                                if (tempMargin < 0) {
                                    tempMargin = 0;
                                } else if (tempMargin > 20) {
                                    tempMargin = 20;
                                }
                                break;
                            case "Full Screen":
                                if (clickEsquerdo) tempFullScreen = !tempFullScreen;
                                break;
                            case "Volume":
                                int volumeChange = (clickEsquerdo ? 10 : -10);
                                tempMusicVolume = (tempMusicVolume + volumeChange + 110) % 110;
                                tempMobsVolume = (tempMobsVolume + volumeChange + 110) % 110;
                                break;
                            case "Music":
                                tempMusicVolume = (tempMusicVolume + (clickEsquerdo ? 10 : -10) + 110) % 110;
                                break;
                            case "Mobs":
                                tempMobsVolume = (tempMobsVolume + (clickEsquerdo ? 10 : -10) + 110) % 110;
                                break;
                            case "Save Changes":
                                Configs.setMaxFrames(tempFps);
                                Configs.setFullscreen(tempFullScreen);
                                Configs.setUiScale(tempUiScale);
                                Configs.setMusic(tempMusicVolume);
                                Configs.setVolum(tempMobsVolume);
                                Configs.setMargin(tempMargin);
                                Engine.index_res = resolutionIndex;
                                Engine.window.resetWindow();
                                Sound.updateVolumes();
                                System.out.println("Opções Aplicadas!");
                                break;
                            case "Back":
                                Engine.backActivity();
                                break;
                            default:
                                System.out.println("Botão desconhecido: " + textoBotao);
                                break;
                        }
                        atualizarTextosBotoes();
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
    }

    private void desenharTitulo(Graphics2D g) {
        g.setColor(corTexto);
        g.setFont(FontG.font(FontG.Game, 15 * Configs.UiScale()));
        FontMetrics fmTitulo = g.getFontMetrics();

        int x = Engine.window.getWidth() / 2 - fmTitulo.stringWidth(titulo) / 2;
        int y = 80;
        g.drawString(titulo, x, y);
    }

    private void desenharTextosTeste(Graphics2D g) {
        g.setColor(corTexto);
        g.setFont(FontG.font(FontG.Game, 11 * Configs.UiScale()));
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

                    int tamanhoFonte = 8 * Configs.UiScale();
                    Font fonteAtual = FontG.font(FontG.Game, tamanhoFonte);
                    FontMetrics fmQuadrados = g.getFontMetrics(fonteAtual);

                    while (fmQuadrados.stringWidth(textosBotoes[linha][coluna]) > quadrado.width - 20) {
                        tamanhoFonte--;
                        fonteAtual = FontG.font(FontG.Game, tamanhoFonte);
                        fmQuadrados = g.getFontMetrics(fonteAtual);
                    }

                    int larguraBotao = quadrado.width;
                    int alturaBotao = quadrado.height;
                    int x = quadrado.x;
                    int y = quadrado.y;

                    if (quadrado.contains(Mouse.getX(), Mouse.getY())) {
                        larguraBotao = (int) (quadrado.width * 1.1);
                        alturaBotao = (int) (quadrado.height * 1.1);
                        x = quadrado.x - (larguraBotao - quadrado.width) / 2;
                        y = quadrado.y - (alturaBotao - quadrado.height) / 2;
                    }

                    g.setColor(new Color(0xF0A59B));
                    g.fillRect(x, y, larguraBotao, 8);

                    g.setColor(new Color(0x6A2838));
                    g.fillRect(x, y + alturaBotao - 6, larguraBotao, 6);

                    RoundRectangle2D arredondar = new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25);

                    g.setColor(new Color(0x6A2838));
                    RoundRectangle2D shadowRect = new RoundRectangle2D.Double(x + 3, y + 3, larguraBotao, alturaBotao, 15, 15);
                    g.fill(shadowRect);

                    RoundRectangle2D shadowRectLeft = new RoundRectangle2D.Double(x - 3, y + 3, larguraBotao, alturaBotao, 15, 15);
                    g.fill(shadowRectLeft);

                    g.setColor(new Color(0xCC4154));
                    g.fill(arredondar);

                    g.setStroke(defaultStroke);

                    g.setColor(corTexto);
                    g.setFont(fonteAtual);
                    g.drawString(
                            textosBotoes[linha][coluna], x + (larguraBotao - fmQuadrados.stringWidth(textosBotoes[linha][coluna])) / 2,
                            y + (alturaBotao - fmQuadrados.getHeight()) / 2 + fmQuadrados.getAscent());
                }
            }
        }
    }

    @Override
    public void dispose() {
    }
}
