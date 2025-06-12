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
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Options implements Activity {

    private Rectangle[][] squares;

    private Rectangle sliderMusicBar;
    private Rectangle sliderMusicThumb;
    private Rectangle sliderSfxBar;
    private Rectangle sliderSfxThumb;

    private boolean isMusicThumbDragging = false;
    private boolean isSfxThumbDragging = false;

    private final Color textColor = Color.WHITE;
    private final String title = "Options";
    private final String[][] baseButtonTexts = {
            {"Res", "FPS", "Save"},
            {"Text Size", "Margin", "Full Screen"},
            {"Volume", "Music", "SFX"},
            {null, "Save Changes", "Back"}
    };
    private String[][] buttonTexts;

    private Map<String, String> tooltipDescriptions;
    private String activeTooltipText = null;

    private int tempFps = Configs.MaxFrames();
    private final int[] fpsOptions = {30, 60, 120};
    private int fpsIndex = 0;

    private boolean tempFullScreen;
    private boolean configFullScreen;

    private int resolutionIndex = Configs.getIndexResolution();
    private int[] tempResolution = Engine.resolutions[resolutionIndex];

    private int tempUiScale = Configs.UiScale();
    private final int[] uiScaleOptions = {2, 3};
    private int uiScaleIndex = 0;

    private int tempMargin = Configs.Margin();

    private int tempMusicVolume = Configs.Music();
    private int tempSfxVolume = Configs.Volum();

    private BufferedImage backgroundImage;

    public Options() {
        configFullScreen = Configs.Fullscreen();
        tempFullScreen = configFullScreen;

        for (int i = 0; i < fpsOptions.length; i++) {
            if (fpsOptions[i] == tempFps) {
                fpsIndex = i;
                break;
            }
        }
        for (int i = 0; i < uiScaleOptions.length; i++) {
            if (uiScaleOptions[i] == tempUiScale) {
                uiScaleIndex = i;
                break;
            }
        }

        buttonTexts = new String[baseButtonTexts.length][baseButtonTexts[0].length];
        initializeTooltips();
        initializeButtons();
        updateButtonTexts();

        try {
            backgroundImage = ImageIO.read(getClass().getResource("/com/retronova/resources/icons/Gato_fundo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeTooltips() {
        tooltipDescriptions = new HashMap<>();
        tooltipDescriptions.put("Res", "Sets the game window resolution. Click to cycle.");
        tooltipDescriptions.put("FPS", "Controls the game's frame rate limit (FPS). Click to cycle.");
        tooltipDescriptions.put("Save", "Saves the current game progress (if applicable).");
        tooltipDescriptions.put("Text Size", "Adjusts the size of texts in the interface. Click to cycle.");
        tooltipDescriptions.put("Margin", "Adjusts the spacing of the interface margins.");
        tooltipDescriptions.put("Full Screen", "Toggles between full screen and windowed mode.");
        tooltipDescriptions.put("Volume", "Controls the master volume for all game sounds. Click to toggle.");
        tooltipDescriptions.put("Music", "Adjusts the volume of in-game music.");
        tooltipDescriptions.put("SFX", "Adjusts the volume of sound effects (e.g., footsteps, actions, entities).");
        tooltipDescriptions.put("Save Changes", "Saves all current settings and applies them to the game.");
        tooltipDescriptions.put("Back", "Discards changes and returns to the game.");
    }

    private void initializeButtons() {
        squares = new Rectangle[4][3];

        int buttonWidth = 200;
        int buttonHeight = 50;
        int spacing = 80;

        int totalWidth = 3 * buttonWidth + 2 * spacing;
        int totalHeight = 4 * buttonHeight + 3 * spacing;

        int xStart = Engine.window.getWidth() / 2 - totalWidth / 2;
        int yStart = Engine.window.getHeight() / 2 - totalHeight / 2 + 50;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                if (baseButtonTexts[row][col] != null) {
                    int x = xStart + col * (buttonWidth + spacing);
                    int y = yStart + row * (buttonHeight + spacing);
                    squares[row][col] = new Rectangle(x, y, buttonWidth, buttonHeight);

                    if (baseButtonTexts[row][col].equals("Music")) {
                        sliderMusicBar = new Rectangle(x, y, buttonWidth, buttonHeight);
                        int thumbWidth = 20;
                        int thumbHeight = buttonHeight;
                        int thumbX = x + (int) (sliderMusicBar.width * (tempMusicVolume / 100.0)) - (thumbWidth / 2);
                        thumbX = Math.max(x, Math.min(x + buttonWidth - thumbWidth, thumbX));
                        sliderMusicThumb = new Rectangle(thumbX, y, thumbWidth, thumbHeight);
                    } else if (baseButtonTexts[row][col].equals("SFX")) {
                        sliderSfxBar = new Rectangle(x, y, buttonWidth, buttonHeight);
                        int thumbWidth = 20;
                        int thumbHeight = buttonHeight;
                        int thumbX = x + (int) (sliderSfxBar.width * (tempSfxVolume / 100.0)) - (thumbWidth / 2);
                        thumbX = Math.max(x, Math.min(x + buttonWidth - thumbWidth, thumbX));
                        sliderSfxThumb = new Rectangle(thumbX, y, thumbWidth, thumbHeight);
                    }
                }
            }
        }
    }

    private void updateButtonTexts() {
        for (int row = 0; row < baseButtonTexts.length; row++) {
            for (int col = 0; col < baseButtonTexts[0].length; col++) {
                if (baseButtonTexts[row][col] != null) {
                    String baseText = baseButtonTexts[row][col];
                    switch (baseText) {
                        case "Res":
                            buttonTexts[row][col] = "Res: " + tempResolution[0] + "x" + tempResolution[1];
                            break;
                        case "FPS":
                            buttonTexts[row][col] = "FPS: " + tempFps;
                            break;
                        case "Text Size":
                            buttonTexts[row][col] = "Text Size: " + tempUiScale;
                            break;
                        case "Margin":
                            buttonTexts[row][col] = "Margin: " + tempMargin;
                            break;
                        case "Full Screen":
                            buttonTexts[row][col] = "Full Screen: " + (tempFullScreen ? "On" : "Off");
                            break;
                        case "Volume":
                            buttonTexts[row][col] = "Volume: " + tempMusicVolume + "% / " + tempSfxVolume + "%";
                            break;
                        case "Music":
                            buttonTexts[row][col] = "Music: " + tempMusicVolume + "%";
                            break;
                        case "SFX":
                            buttonTexts[row][col] = "SFX: " + tempSfxVolume + "%";
                            break;
                        default:
                            buttonTexts[row][col] = baseText;
                            break;
                    }
                } else {
                    buttonTexts[row][col] = null;
                }
            }
        }
    }

    @Override
    public void tick() {
        initializeButtons();
        activeTooltipText = null;

        if (Configs.Fullscreen() != configFullScreen) {
            tempFullScreen = Configs.Fullscreen();
            configFullScreen = Configs.Fullscreen();
            updateButtonTexts();
        }

        if (sliderMusicThumb != null && sliderMusicBar != null) {
            if (Mouse.pressing(Mouse_Button.LEFT) && (sliderMusicThumb.contains(Mouse.getX(), Mouse.getY()) || isMusicThumbDragging)) {
                isMusicThumbDragging = true;
                int mouseX = Mouse.getX();
                int newThumbX = mouseX - sliderMusicThumb.width / 2;

                int minX = sliderMusicBar.x;
                int maxX = sliderMusicBar.x + sliderMusicBar.width - sliderMusicThumb.width;
                newThumbX = Math.max(minX, Math.min(maxX, newThumbX));

                sliderMusicThumb.x = newThumbX;

                double percentage = (double) (sliderMusicThumb.x - sliderMusicBar.x) / (sliderMusicBar.width - sliderMusicThumb.width);
                tempMusicVolume = (int) (percentage * 100);
                tempMusicVolume = Math.max(0, Math.min(100, tempMusicVolume));
                updateButtonTexts();
            } else {
                isMusicThumbDragging = false;

                if (sliderMusicBar.contains(Mouse.getX(), Mouse.getY()) && Mouse.clickOn(Mouse_Button.LEFT, sliderMusicBar)) {
                    Sound.play(Sounds.Button);
                    int mouseX = Mouse.getX();
                    double percentage = (double) (mouseX - sliderMusicBar.x) / sliderMusicBar.width;
                    tempMusicVolume = (int) (percentage * 100);
                    tempMusicVolume = Math.max(0, Math.min(100, tempMusicVolume));

                    int thumbX = sliderMusicBar.x + (int) (sliderMusicBar.width * (tempMusicVolume / 100.0)) - (sliderMusicThumb.width / 2);
                    thumbX = Math.max(sliderMusicBar.x, Math.min(sliderMusicBar.x + sliderMusicBar.width - sliderMusicThumb.width, thumbX));
                    sliderMusicThumb.x = thumbX;

                    updateButtonTexts();
                }
            }
            if (sliderMusicBar.contains(Mouse.getX(), Mouse.getY())) {
                activeTooltipText = tooltipDescriptions.get("Music");
            }
        }

        if (sliderSfxThumb != null && sliderSfxBar != null) {
            if (Mouse.pressing(Mouse_Button.LEFT) && (sliderSfxThumb.contains(Mouse.getX(), Mouse.getY()) || isSfxThumbDragging)) {
                isSfxThumbDragging = true;
                int mouseX = Mouse.getX();
                int newThumbX = mouseX - sliderSfxThumb.width / 2;

                int minX = sliderSfxBar.x;
                int maxX = sliderSfxBar.x + sliderSfxBar.width - sliderSfxThumb.width;
                newThumbX = Math.max(minX, Math.min(maxX, newThumbX));

                sliderSfxThumb.x = newThumbX;

                double percentage = (double) (sliderSfxThumb.x - sliderSfxBar.x) / (sliderSfxBar.width - sliderSfxThumb.width);
                tempSfxVolume = (int) (percentage * 100);
                tempSfxVolume = Math.max(0, Math.min(100, tempSfxVolume));
                updateButtonTexts();
            } else {
                isSfxThumbDragging = false;

                if (sliderSfxBar.contains(Mouse.getX(), Mouse.getY()) && Mouse.clickOn(Mouse_Button.LEFT, sliderSfxBar)) {
                    Sound.play(Sounds.Button);
                    int mouseX = Mouse.getX();
                    double percentage = (double) (mouseX - sliderSfxBar.x) / sliderSfxBar.width;
                    tempSfxVolume = (int) (percentage * 100);
                    tempSfxVolume = Math.max(0, Math.min(100, tempSfxVolume));

                    int thumbX = sliderSfxBar.x + (int) (sliderSfxBar.width * (tempSfxVolume / 100.0)) - (sliderSfxThumb.width / 2);
                    thumbX = Math.max(sliderSfxBar.x, Math.min(sliderSfxBar.x + sliderSfxBar.width - sliderSfxThumb.width, thumbX));
                    sliderSfxThumb.x = thumbX;

                    updateButtonTexts();
                }
            }
            if (sliderSfxBar.contains(Mouse.getX(), Mouse.getY())) {
                activeTooltipText = tooltipDescriptions.get("SFX");
            }
        }

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                if (baseButtonTexts[row][col] != null) {
                    String baseText = baseButtonTexts[row][col];

                    if (baseText.equals("Music") || baseText.equals("SFX")) {
                        continue;
                    }

                    if (squares[row][col] != null) {
                        if (squares[row][col].contains(Mouse.getX(), Mouse.getY())) {
                            activeTooltipText = tooltipDescriptions.get(baseText);
                        }

                        if (Mouse.clickOn(Mouse_Button.LEFT, squares[row][col])) {
                            Sound.play(Sounds.Button);

                            switch (baseText) {
                                case "Res":
                                    resolutionIndex = (resolutionIndex + 1) % Engine.resolutions.length;
                                    tempResolution = Engine.resolutions[resolutionIndex];
                                    break;
                                case "FPS":
                                    fpsIndex = (fpsIndex + 1) % fpsOptions.length;
                                    tempFps = fpsOptions[fpsIndex];
                                    break;
                                case "Text Size":
                                    uiScaleIndex = (uiScaleIndex + 1) % uiScaleOptions.length;
                                    tempUiScale = uiScaleOptions[uiScaleIndex];
                                    break;
                                case "Margin":
                                    tempMargin = (tempMargin + 5) % 25;
                                    break;
                                case "Full Screen":
                                    tempFullScreen = !tempFullScreen;
                                    break;
                                case "Volume":
                                    if (tempMusicVolume == 0 && tempSfxVolume == 0) {
                                        tempMusicVolume = 50;
                                        tempSfxVolume = 50;
                                    } else if (tempMusicVolume == 50 && tempSfxVolume == 50) {
                                        tempMusicVolume = 100;
                                        tempSfxVolume = 100;
                                    } else {
                                        tempMusicVolume = 0;
                                        tempSfxVolume = 0;
                                    }
                                    if (sliderMusicThumb != null && sliderMusicBar != null) {
                                        int thumbX = sliderMusicBar.x + (int) (sliderMusicBar.width * (tempMusicVolume / 100.0)) - (sliderMusicThumb.width / 2);
                                        thumbX = Math.max(sliderMusicBar.x, Math.min(sliderMusicBar.x + sliderMusicBar.width - sliderMusicThumb.width, thumbX));
                                    }
                                    if (sliderSfxThumb != null && sliderSfxBar != null) {
                                        int thumbX = sliderSfxBar.x + (int) (sliderSfxBar.width * (tempSfxVolume / 100.0)) - (sliderSfxThumb.width / 2);
                                        thumbX = Math.max(sliderSfxBar.x, Math.min(sliderSfxBar.x + sliderSfxBar.width - sliderSfxThumb.width, thumbX));
                                        sliderSfxThumb.x = thumbX;
                                    }
                                    break;
                                case "Save Changes":
                                    applySettings();
                                    Engine.backActivity();
                                    break;
                                case "Back":
                                    Engine.backActivity();
                                    break;
                            }
                            updateButtonTexts();
                        }
                    }
                }
            }
        }
    }

    private void applySettings() {
        Configs.setMaxFrames(tempFps);
        Configs.setFullscreen(tempFullScreen);
        Configs.setUiScale(tempUiScale);
        Configs.setMusic(tempMusicVolume);
        Configs.setVolum(tempSfxVolume);
        Configs.setMargin(tempMargin);
        Configs.setIndexResolution(resolutionIndex);
        Engine.window.resetWindow();
        Sound.updateVolumes();
        Configs.update();
    }

    @Override
    public void render(Graphics2D g) {
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, Engine.window.getWidth(), Engine.window.getHeight(), null);
        }
        drawTitle(g);
        drawOptionButtons(g);
        drawSliders(g);

        if (activeTooltipText != null) {
            drawTooltip(g, activeTooltipText, Mouse.getX(), Mouse.getY());
        }
    }

    private void drawTitle(Graphics2D g) {
        g.setColor(textColor);
        g.setFont(FontHandler.font(FontHandler.Game, 15 * Configs.UiScale()));
        FontMetrics fmTitle = g.getFontMetrics();

        int x = Engine.window.getWidth() / 2 - fmTitle.stringWidth(title) / 2;
        int y = 80;
        g.drawString(title, x, y);
    }

    private void drawOptionButtons(Graphics2D g) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                if (squares[row][col] != null && buttonTexts[row][col] != null) {
                    String baseText = baseButtonTexts[row][col];
                    if (baseText.equals("Music") || baseText.equals("SFX")) {
                        continue;
                    }

                    Rectangle square = squares[row][col];

                    int fontSize = 8 * Configs.UiScale();
                    Font currentFont = FontHandler.font(FontHandler.Game, fontSize);
                    FontMetrics fmSquares = g.getFontMetrics(currentFont);

                    while (fmSquares.stringWidth(buttonTexts[row][col]) > square.width - 40) {
                        fontSize--;
                        currentFont = FontHandler.font(FontHandler.Game, fontSize);
                        fmSquares = g.getFontMetrics(currentFont);
                    }

                    int buttonWidth = square.width;
                    int buttonHeight = square.height;
                    int x = square.x;
                    int y = square.y;

                    boolean selected = square.contains(Mouse.getX(), Mouse.getY());

                    if (selected) {
                        buttonWidth = (int) (square.width * 1.1);
                        buttonHeight = (int) (square.height * 1.1);
                        x = square.x - (buttonWidth - square.width) / 2;
                        y = square.y - (buttonHeight - square.height) / 2;
                    }

                    g.setColor(new Color(0x4A5364));
                    g.fillRect(x, y + buttonHeight - 5, buttonWidth, 5);

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g.setColor(new Color(0x000000));
                    RoundRectangle2D shadowRect = new RoundRectangle2D.Double(x + 2, y + 2, buttonWidth, buttonHeight, 20, 20);
                    g.fill(shadowRect);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                    RoundRectangle2D roundedRect = new RoundRectangle2D.Double(x, y, buttonWidth, buttonHeight, 25, 25);
                    g.setColor(new Color(0x6B7A8F));
                    g.fill(roundedRect);

                    GradientPaint lightTop = new GradientPaint(
                            x, y, new Color(255, 255, 255, 60),
                            x, y + buttonHeight / 2, new Color(255, 255, 255, 0)
                    );
                    g.setPaint(lightTop);
                    g.fill(new RoundRectangle2D.Double(x, y, buttonWidth, buttonHeight, 25, 25));

                    g.setColor(textColor);
                    g.setFont(currentFont);
                    g.drawString(
                            buttonTexts[row][col], x + (buttonWidth - fmSquares.stringWidth(buttonTexts[row][col])) / 2,
                            y + (buttonHeight - fmSquares.getHeight()) / 2 + fmSquares.getAscent());
                }
            }
        }
    }

    private void drawSliders(Graphics2D g) {
        if (sliderMusicBar != null && sliderMusicThumb != null) {
            g.setColor(new Color(0x4A5364));
            g.fill(new RoundRectangle2D.Double(sliderMusicBar.x, sliderMusicBar.y, sliderMusicBar.width, sliderMusicBar.height, 10, 10));

            g.setColor(new Color(0x6B7A8F));
            if (sliderMusicThumb.contains(Mouse.getX(), Mouse.getY()) || isMusicThumbDragging) {
                g.setColor(new Color(0x80899B));
            }
            g.fill(new RoundRectangle2D.Double(sliderMusicThumb.x, sliderMusicThumb.y, sliderMusicThumb.width, sliderMusicThumb.height, 8, 8));

            g.setColor(textColor);
            g.setFont(FontHandler.font(FontHandler.Game, 8 * Configs.UiScale()));
            String text = "Music: " + tempMusicVolume + "%";
            FontMetrics fm = g.getFontMetrics();
            int textX = sliderMusicBar.x + (sliderMusicBar.width - fm.stringWidth(text)) / 2;
            int textY = sliderMusicBar.y + (sliderMusicBar.height - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, textX, textY);
        }

        if (sliderSfxBar != null && sliderSfxThumb != null) {
            g.setColor(new Color(0x4A5364));
            g.fill(new RoundRectangle2D.Double(sliderSfxBar.x, sliderSfxBar.y, sliderSfxBar.width, sliderSfxBar.height, 10, 10));

            g.setColor(new Color(0x6B7A8F));
            if (sliderSfxThumb.contains(Mouse.getX(), Mouse.getY()) || isSfxThumbDragging) {
                g.setColor(new Color(0x80899B));
            }
            g.fill(new RoundRectangle2D.Double(sliderSfxThumb.x, sliderSfxThumb.y, sliderSfxThumb.width, sliderSfxThumb.height, 8, 8));

            g.setColor(textColor);
            g.setFont(FontHandler.font(FontHandler.Game, 8 * Configs.UiScale()));
            String text = "SFX: " + tempSfxVolume + "%";
            FontMetrics fm = g.getFontMetrics();
            int textX = sliderSfxBar.x + (sliderSfxBar.width - fm.stringWidth(text)) / 2;
            int textY = sliderSfxBar.y + (sliderSfxBar.height - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, textX, textY);
        }
    }

    private void drawTooltip(Graphics2D g, String text, int mouseX, int mouseY) {
        int padding = 10;
        int borderRadius = 10;
        Color bgColor = new Color(30, 30, 30, 200);
        Color textColor = Color.WHITE;
        Font font = FontHandler.font(FontHandler.Game, 7 * Configs.UiScale());

        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        int panelWidth = textWidth + padding * 2;
        int panelHeight = textHeight + padding * 2;

        int tooltipX = mouseX + 15;
        int tooltipY = mouseY + 15;

        if (tooltipX + panelWidth > Engine.window.getWidth()) {
            tooltipX = Engine.window.getWidth() - panelWidth - 5;
        }
        if (tooltipY + panelHeight > Engine.window.getHeight()) {
            tooltipY = mouseY - panelHeight - 5;
            if (tooltipY < 0) tooltipY = 5;
        }

        g.setColor(bgColor);
        g.fill(new RoundRectangle2D.Double(tooltipX, tooltipY, panelWidth, panelHeight, borderRadius, borderRadius));

        g.setColor(new Color(100, 100, 100, 200));
        g.draw(new RoundRectangle2D.Double(tooltipX, tooltipY, panelWidth, panelHeight, borderRadius, borderRadius));

        g.setColor(textColor);
        g.drawString(text, tooltipX + padding, tooltipY + padding + fm.getAscent());
    }

    @Override
    public void dispose() {
    }
}