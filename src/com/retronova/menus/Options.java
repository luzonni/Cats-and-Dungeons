package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

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

    private int tempFps = Configs.MaxFrames();
    private int[] fpsOptions = {30, 60, 120};
    private int fpsIndex = 0;

    private boolean tempFullScreen;
    private boolean configFullScreen;

    private int resolutionIndex = Configs.getIndexResolution();
    private int[] tempResolution = Engine.resolutions[resolutionIndex];

    private int tempUiScale = Configs.UiScale();

    private int tempMargin = Configs.Margin();

    private int tempMusicVolume = Configs.Music();
    private int tempMobsVolume = Configs.Volum();

    private BufferedImage imagemFundo;

    public Options() {
        configFullScreen = Configs.Fullscreen();
        tempFullScreen = configFullScreen;
        inicializarBotoes();
        atualizarTextosBotoes();

        try {
            imagemFundo = ImageIO.read(getClass().getResource("/com/retronova/res/icons/Gato_fundo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String uiScaleText;
        if (tempUiScale == 2) {
            uiScaleText = "1";
        } else if (tempUiScale == 3) {
            uiScaleText = "2";
        } else {
            uiScaleText = String.valueOf(tempUiScale); // Fallback
        }
        textosBotoes[1][0] = "Text Size: " + uiScaleText;
        textosBotoes[0][0] = "Res: " + tempResolution[0] + "x" + tempResolution[1];
        textosBotoes[0][1] = "FPS: " + tempFps;
        textosBotoes[1][1] = "Margin: " + tempMargin;
        textosBotoes[1][2] = "Full Screen: " + (tempFullScreen ? "On" : "Off");
        textosBotoes[2][1] = "Music: " + tempMusicVolume;
        textosBotoes[2][2] = "Mobs: " + tempMobsVolume;
    }

    @Override
    public void tick() {
        inicializarBotoes();
        atualizarBotaoSelecionado();

        if (Configs.Fullscreen() != configFullScreen) {
            tempFullScreen = Configs.Fullscreen();
            configFullScreen = Configs.Fullscreen();
            atualizarTextosBotoes();
        }

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null) {
                    boolean clickEsquerdo = Mouse.clickOn(Mouse_Button.LEFT, quadrados[linha][coluna]);
                    boolean clickDireito = Mouse.clickOn(Mouse_Button.RIGHT, quadrados[linha][coluna]);

                    if (clickEsquerdo) {
                        String textoBotao = textosBotoes[linha][coluna];

                        switch (textoBotao.split(":")[0].trim()) {
                            case "Res":
                                Sound.play(Sounds.Button);
                                resolutionIndex = (resolutionIndex + 1 + Engine.resolutions.length) % Engine.resolutions.length;
                                tempResolution = Engine.resolutions[resolutionIndex];
                                break;
                            case "FPS":
                                Sound.play(Sounds.Button);
                                fpsIndex = (fpsIndex + 1 + fpsOptions.length) % fpsOptions.length;
                                tempFps = fpsOptions[fpsIndex];
                                break;
                            case "Text Size":
                                Sound.play(Sounds.Button);
                                tempUiScale = (tempUiScale == 2) ? 3 : 2;
                                break;
                            case "Margin":
                                Sound.play(Sounds.Button);
                                tempMargin += 5;
                                if (tempMargin > 20) tempMargin = 20;
                                break;
                            case "Full Screen":
                                Sound.play(Sounds.Button);
                                tempFullScreen = !tempFullScreen;
                                break;
                            case "Volume":
                                Sound.play(Sounds.Button);
                                tempMusicVolume = Math.min(100, tempMusicVolume + 10);
                                tempMobsVolume = Math.min(100, tempMobsVolume + 10);
                                break;
                            case "Music":
                                Sound.play(Sounds.Button);
                                tempMusicVolume = Math.min(100, tempMusicVolume + 10);
                                break;
                            case "Mobs":
                                Sound.play(Sounds.Button);
                                tempMobsVolume = Math.min(100, tempMobsVolume + 10);
                                break;
                            case "Save Changes":
                                Sound.play(Sounds.Button);
                                aplicarConfiguracoes();
                                Engine.backActivity();
                                break;
                            case "Back":
                                Sound.play(Sounds.Button);
                                Engine.backActivity();
                                break;
                            default:
                                System.out.println("Botão desconhecido: " + textoBotao);
                                break;
                        }
                        atualizarTextosBotoes();
                    } else if (clickDireito) {
                        String textoBotao = textosBotoes[linha][coluna];

                        switch (textoBotao.split(":")[0].trim()) {
                            case "Res":
                                Sound.play(Sounds.Button);
                                resolutionIndex = (resolutionIndex - 1 + Engine.resolutions.length) % Engine.resolutions.length;
                                tempResolution = Engine.resolutions[resolutionIndex];
                                break;
                            case "FPS":
                                Sound.play(Sounds.Button);
                                fpsIndex = (fpsIndex - 1 + fpsOptions.length) % fpsOptions.length;
                                tempFps = fpsOptions[fpsIndex];
                                break;
                            case "Text Size":
                                Sound.play(Sounds.Button);
                                tempUiScale = (tempUiScale == 2) ? 3 : 2;
                                break;
                            case "Margin":
                                Sound.play(Sounds.Button);
                                tempMargin -= 5;
                                if (tempMargin < 0) tempMargin = 0;
                                break;
                            case "Full Screen":
                                Sound.play(Sounds.Button);
                                tempFullScreen = !tempFullScreen;
                                break;
                            case "Volume":
                                Sound.play(Sounds.Button);
                                tempMusicVolume = Math.max(0, tempMusicVolume - 10);
                                tempMobsVolume = Math.max(0, tempMobsVolume - 10);
                                break;
                            case "Music":
                                Sound.play(Sounds.Button);
                                tempMusicVolume = Math.max(0, tempMusicVolume - 10);
                                break;
                            case "Mobs":
                                Sound.play(Sounds.Button);
                                tempMobsVolume = Math.max(0, tempMobsVolume - 10);
                                break;
                        }
                        atualizarTextosBotoes();
                    }
                }
            }
        }
    }

    private void aplicarConfiguracoes() {
        Configs.setMaxFrames(tempFps);
        Configs.setFullscreen(tempFullScreen);
        Configs.setUiScale(tempUiScale);
        Configs.setMusic(tempMusicVolume);
        Configs.setVolum(tempMobsVolume);
        Configs.setMargin(tempMargin);
        Configs.setIndexResolution(resolutionIndex);
        Engine.window.resetWindow();
        Sound.updateVolumes();
        Configs.update();
    }

    private void atualizarBotaoSelecionado() {

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null && quadrados[linha][coluna].contains(Mouse.getX(), Mouse.getY())) {
                    return;
                }
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (imagemFundo != null) {
            g.drawImage(imagemFundo, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }
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

                    boolean selecionado = quadrado.contains(Mouse.getX(), Mouse.getY());

                    if (selecionado) {
                        larguraBotao = (int) (quadrado.width * 1.1);
                        alturaBotao = (int) (quadrado.height * 1.1);
                        x = quadrado.x - (larguraBotao - quadrado.width) / 2;
                        y = quadrado.y - (alturaBotao - quadrado.height) / 2;
                    }

                    g.setColor(new Color(0x4A5364));
                    g.fillRect(x, y + alturaBotao - 5, larguraBotao, 5);

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g.setColor(new Color(0x000000));
                    RoundRectangle2D shadowRect = new RoundRectangle2D.Double(x + 2, y + 2, larguraBotao, alturaBotao, 20, 20);
                    g.fill(shadowRect);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                    RoundRectangle2D arredondar = new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25);
                    g.setColor(new Color(0x6B7A8F));
                    g.fill(arredondar);

                    GradientPaint lightTop = new GradientPaint(
                            x, y, new Color(255, 255, 255, 60),
                            x, y + alturaBotao / 2, new Color(255, 255, 255, 0)
                    );
                    g.setPaint(lightTop);
                    g.fill(new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25));

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