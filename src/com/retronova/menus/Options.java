package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Options implements Activity {

    private Rectangle[][] quadrados;
    private Rectangle[][] botoesAjuste;
    private final Color corTexto = Color.WHITE;
    private final String titulo = "Options";
    private final String[][] textosBaseBotoes = {
            {"Res", "FPS", "Save"},
            {"Text Size", "Margin", "Full Screen"},
            {"Volume", "Music", "Mobs"},
            {null, "Save Changes", "Back"}
    };
    private String[][] textosBotoes;

    private int tempFps = Configs.MaxFrames();
    private final int[] fpsOptions = {30, 60, 120};
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
        textosBotoes = new String[textosBaseBotoes.length][textosBaseBotoes[0].length];
        inicializarBotoes();
        atualizarTextosBotoes();

        try {
            imagemFundo = ImageIO.read(getClass().getResource("/com/retronova/resources/icons/Gato_fundo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void inicializarBotoes() {
        quadrados = new Rectangle[4][3];
        botoesAjuste = new Rectangle[4][3 * 2];

        int larguraBotao = 200;
        int alturaBotao = 50;
        int larguraBotaoAjuste = 30;
        int espacamento = 80;
        int espacamentoAjuste = 5;

        int larguraTotal = 3 * larguraBotao + 2 * espacamento;
        int alturaTotal = 4 * alturaBotao + 3 * espacamento;

        int xInicio = Engine.window.getWidth() / 2 - larguraTotal / 2;
        int yInicio = Engine.window.getHeight() / 2 - alturaTotal / 2 + 50;

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (textosBaseBotoes[linha][coluna] != null) {
                    int x = xInicio + coluna * (larguraBotao + espacamento);
                    int y = yInicio + linha * (alturaBotao + espacamento);
                    quadrados[linha][coluna] = new Rectangle(x, y, larguraBotao, alturaBotao);

                    if (!textosBaseBotoes[linha][coluna].equals("Save") &&
                            !textosBaseBotoes[linha][coluna].equals("Save Changes") &&
                            !textosBaseBotoes[linha][coluna].equals("Back")) {
                        int xMenos = x - larguraBotaoAjuste - espacamentoAjuste;
                        int xMais = x + larguraBotao + espacamentoAjuste;
                        botoesAjuste[linha][coluna * 2] = new Rectangle(xMenos, y + (alturaBotao - larguraBotaoAjuste) / 2, larguraBotaoAjuste, larguraBotaoAjuste);
                        botoesAjuste[linha][coluna * 2 + 1] = new Rectangle(xMais, y + (alturaBotao - larguraBotaoAjuste) / 2, larguraBotaoAjuste, larguraBotaoAjuste);
                    } else {
                        botoesAjuste[linha][coluna * 2] = null;
                        botoesAjuste[linha][coluna * 2 + 1] = null;
                    }
                } else {
                    botoesAjuste[linha][coluna * 2] = null;
                    botoesAjuste[linha][coluna * 2 + 1] = null;
                }
            }
        }
    }

    private void atualizarTextosBotoes() {
        for (int linha = 0; linha < textosBaseBotoes.length; linha++) {
            for (int coluna = 0; coluna < textosBaseBotoes[0].length; coluna++) {
                if (textosBaseBotoes[linha][coluna] != null) {
                    String textoBase = textosBaseBotoes[linha][coluna];
                    switch (textoBase) {
                        case "Res":
                            textosBotoes[linha][coluna] = "Res: " + tempResolution[0] + "x" + tempResolution[1];
                            break;
                        case "FPS":
                            textosBotoes[linha][coluna] = "FPS: " + tempFps;
                            break;
                        case "Text Size":
                            textosBotoes[linha][coluna] = "Text Size: " + tempUiScale;
                            break;
                        case "Margin":
                            textosBotoes[linha][coluna] = "Margin: " + tempMargin;
                            break;
                        case "Full Screen":
                            textosBotoes[linha][coluna] = "Full Screen: " + (tempFullScreen ? "On" : "Off");
                            break;
                        case "Volume":
                            textosBotoes[linha][coluna] = "Volume: " + tempMusicVolume + "% / " + tempMobsVolume + "%";
                            break;
                        case "Music":
                            textosBotoes[linha][coluna] = "Music: " + tempMusicVolume + "%";
                            break;
                        case "Mobs":
                            textosBotoes[linha][coluna] = "Mobs: " + tempMobsVolume + "%";
                            break;
                        default:
                            textosBotoes[linha][coluna] = textoBase;
                            break;
                    }
                } else {
                    textosBotoes[linha][coluna] = null;
                }
            }
        }
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
                if (textosBaseBotoes[linha][coluna] != null) {
                    if (botoesAjuste[linha][coluna * 2] != null && Mouse.clickOn(Mouse_Button.LEFT, botoesAjuste[linha][coluna * 2])) {
                        Sound.play(Sounds.Button);
                        switch (textosBaseBotoes[linha][coluna]) {
                            case "Res":
                                resolutionIndex = (resolutionIndex - 1 + Engine.resolutions.length) % Engine.resolutions.length;
                                tempResolution = Engine.resolutions[resolutionIndex];
                                break;
                            case "FPS":
                                fpsIndex = (fpsIndex - 1 + fpsOptions.length) % fpsOptions.length;
                                tempFps = fpsOptions[fpsIndex];
                                break;
                            case "Text Size":
                                tempUiScale = (tempUiScale == 2) ? 3 : 2;
                                break;
                            case "Margin":
                                tempMargin = Math.max(0, tempMargin - 5);
                                break;
                            case "Full Screen":
                                tempFullScreen = !tempFullScreen;
                                break;
                            case "Volume":
                                tempMusicVolume = Math.max(0, tempMusicVolume - 10);
                                tempMobsVolume = Math.max(0, tempMobsVolume - 10);
                                break;
                            case "Music":
                                tempMusicVolume = Math.max(0, tempMusicVolume - 10);
                                break;
                            case "Mobs":
                                tempMobsVolume = Math.max(0, tempMobsVolume - 10);
                                break;
                        }
                        atualizarTextosBotoes();
                    }

                    if (botoesAjuste[linha][coluna * 2 + 1] != null && Mouse.clickOn(Mouse_Button.LEFT, botoesAjuste[linha][coluna * 2 + 1])) {
                        Sound.play(Sounds.Button);
                        switch (textosBaseBotoes[linha][coluna]) {
                            case "Res":
                                resolutionIndex = (resolutionIndex + 1 + Engine.resolutions.length) % Engine.resolutions.length;
                                tempResolution = Engine.resolutions[resolutionIndex];
                                break;
                            case "FPS":
                                fpsIndex = (fpsIndex + 1 + fpsOptions.length) % fpsOptions.length;
                                tempFps = fpsOptions[fpsIndex];
                                break;
                            case "Text Size":
                                tempUiScale = (tempUiScale == 2) ? 3 : 2;
                                break;
                            case "Margin":
                                tempMargin = Math.min(20, tempMargin + 5);
                                break;
                            case "Full Screen":
                                tempFullScreen = !tempFullScreen;
                                break;
                            case "Volume":
                                tempMusicVolume = Math.min(100, tempMusicVolume + 10);
                                tempMobsVolume = Math.min(100, tempMobsVolume + 10);
                                break;
                            case "Music":
                                tempMusicVolume = Math.min(100, tempMusicVolume + 10);
                                break;
                            case "Mobs":
                                tempMobsVolume = Math.min(100, tempMobsVolume + 10);
                                break;
                        }
                        atualizarTextosBotoes();
                    }

                    if (quadrados[linha][coluna] != null && Mouse.clickOn(Mouse_Button.LEFT, quadrados[linha][coluna])) {
                        if (textosBaseBotoes[linha][coluna].equals("Save Changes")) {
                            Sound.play(Sounds.Button);
                            aplicarConfiguracoes();
                            Engine.backActivity();
                        } else if (textosBaseBotoes[linha][coluna].equals("Back")) {
                            Sound.play(Sounds.Button);
                            Engine.backActivity();
                        }
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
    }

    @Override
    public void render(Graphics2D g) {
        if (imagemFundo != null) {
            g.drawImage(imagemFundo, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }
        desenharTitulo(g);
        desenharBotoesOpcao(g);
        desenharBotoesAjuste(g);
    }

    private void desenharTitulo(Graphics2D g) {
        g.setColor(corTexto);
        g.setFont(FontHandler.font(FontHandler.Game, 15 * Configs.UiScale()));
        FontMetrics fmTitulo = g.getFontMetrics();

        int x = Engine.window.getWidth() / 2 - fmTitulo.stringWidth(titulo) / 2;
        int y = 80;
        g.drawString(titulo, x, y);
    }

    private void desenharBotoesOpcao(Graphics2D g) {
        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (quadrados[linha][coluna] != null && textosBotoes[linha][coluna] != null) {
                    Rectangle quadrado = quadrados[linha][coluna];

                    int tamanhoFonte = 8 * Configs.UiScale();
                    Font fonteAtual = FontHandler.font(FontHandler.Game, tamanhoFonte);
                    FontMetrics fmQuadrados = g.getFontMetrics(fonteAtual);

                    while (fmQuadrados.stringWidth(textosBotoes[linha][coluna]) > quadrado.width - 40) {
                        tamanhoFonte--;
                        fonteAtual = FontHandler.font(FontHandler.Game, tamanhoFonte);
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

    private void desenharBotoesAjuste(Graphics2D g) {
        g.setColor(corTexto);
        Font fonteAjuste = FontHandler.font(FontHandler.Game, 12 * Configs.UiScale());
        FontMetrics fmAjuste = g.getFontMetrics(fonteAjuste);

        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                if (botoesAjuste[linha][coluna * 2] != null) {
                    Rectangle botaoMenos = botoesAjuste[linha][coluna * 2];
                    desenharBotaoAjusteIndividual(g, botaoMenos, "-", fonteAjuste, fmAjuste);
                }
                if (botoesAjuste[linha][coluna * 2 + 1] != null) {
                    Rectangle botaoMais = botoesAjuste[linha][coluna * 2 + 1];
                    desenharBotaoAjusteIndividual(g, botaoMais, "+", fonteAjuste, fmAjuste);
                }
            }
        }
    }

    private void desenharBotaoAjusteIndividual(Graphics2D g, Rectangle botao, String texto, Font fonte, FontMetrics fm) {
        int larguraBotao = botao.width;
        int alturaBotao = botao.height;
        int x = botao.x;
        int y = botao.y;

        boolean selecionado = botao.contains(Mouse.getX(), Mouse.getY());

        if (selecionado) {
            g.setColor(new Color(0x80899B));
        } else {
            g.setColor(new Color(0x6B7A8F));
        }
        g.fill(new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 10, 10));

        g.setColor(corTexto);
        g.setFont(fonte);
        Rectangle2D textoBounds = fm.getStringBounds(texto, g);
        int textoX = x + (int) ((larguraBotao - textoBounds.getWidth()) / 2);
        int textoY = y + (int) ((alturaBotao - textoBounds.getHeight()) / 2 + fm.getAscent());
        g.drawString(texto, textoX, textoY);
    }

    @Override
    public void dispose() {
    }
}