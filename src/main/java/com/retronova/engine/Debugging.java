package com.retronova.engine;

import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import oshi.SystemInfo;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class Debugging {

    public static boolean running = false;

    /**
     * Vitrine: modo de revisão de itens.
     *
     * O vendedor passa a oferecer TODOS os itens do jogo, o dinheiro não cai ao
     * comprar e a prateleira não esvazia. Serve para olhar cada item na mão do
     * gato de uma sentada só — sem isso a revisão vira uma partida inteira de
     * garimpo para ver vinte e um sprites.
     *
     * Vem de uma propriedade de sistema, e não de uma constante editada à mão:
     * assim ninguém esquece ligada. Liga com
     *
     *     ./gradlew run -Pvitrine
     */
    public static final boolean VITRINE = Boolean.getBoolean("vitrine");

    public static boolean showEntityHitBox = false;
    public static boolean showParticleHitBox = false;
    public static boolean showTileBox = false;

    private static Point position;
    private static Font font;
    private static Color background;

    private static Map<String, String> infos;

    public static void init() {
        // A preferência gravada vale desde o primeiro quadro.
        showEntityHitBox = Configs.Hitboxes();
        font = FontHandler.font(FontHandler.Septem, Configs.UiScale()*8);
        position = new Point(10, 10);
        background = new Color(100, 100, 100, 180);
        infos = new LinkedHashMap<>();
        new Thread(() -> {
            setInfo("Version", Engine.GameTag + " - " + Engine.VERSION);
            try {
                SystemInfo systemInfo = new SystemInfo();
                setInfo("Processor", systemInfo.getHardware().getProcessor().getProcessorIdentifier().getName());
                setInfo("Arquitetura", System.getProperty("os.arch"));
                setInfo("GraphicsCard", systemInfo.getHardware().getGraphicsCards().getFirst().getName());
                long memoria = systemInfo.getHardware().getMemory().getTotal();
                setInfo("Memory", formatBytes(memoria));
            }catch (Exception e) {
                System.err.println("Erro ao carregar as infoemaçẽos do processador: \n" + e);
            }
            Debugging.setInfo("OpenGL", System.getProperty("sun.java2d.opengl"));
        }).start();
    }

    public static void setInfo(String key, String info) {
        infos.put(key, info);
    }

    public static void tick() {
        if(KeyBoard.KeyPressed("F3")) {
            Debugging.toggleRunning();
        }
        if(KeyBoard.KeyPressed("F4")) {
            mostrarHitBox(!showEntityHitBox);
        }
        if(KeyBoard.KeyPressed("F5")) {
            Debugging.showTileBox = !Debugging.showTileBox;
        }
        if(KeyBoard.KeyPressed("F6")) {
            Debugging.showParticleHitBox = !Debugging.showParticleHitBox;
        }
    }

    /**
     * Liga e desliga as caixas de colisão, gravando a escolha.
     *
     * Existe para a tecla e o menu de opções não divergirem: os dois passam por
     * aqui, então apertar F4 marca a caixinha nas opções e vice-versa.
     */
    public static void mostrarHitBox(boolean mostrar) {
        showEntityHitBox = mostrar;
        Configs.setHitboxes(mostrar);
    }

    public static void toggleRunning() {
        running = !running;
    }

    public static void render(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        String[] keys = infos.keySet().toArray(new String[0]);
        for(int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String text = key + " : " + infos.get(key);
            int x = position.x;
            int y = position.y + (int)(FontHandler.getHeight(text, font)*1.5)*i;
            renderInfo(text, x, y, g);
        }
        g.dispose();
    }

    private static void renderInfo(String text, int x, int y, Graphics2D g) {
        int padding = Configs.UiScale();
        g.setColor(background);
        int wF = FontHandler.getWidth(text, font);
        int hF = FontHandler.getHeight(text, font);
        g.fillRect(x, y, wF + padding*2, hF + padding*2);
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawString(text, x + padding*2, y + hF + padding);
        g.setColor(Color.WHITE);
        g.drawString(text, x + padding, y + hF);
    }

    static String formatBytes(long bytes) {
        final long GB = 1024 * 1024 * 1024;
        final long MB = 1024 * 1024;
        if (bytes >= GB)
            return String.format("%.2f GB", bytes / (double) GB);
        else
            return String.format("%.2f MB", bytes / (double) MB);
    }

}
