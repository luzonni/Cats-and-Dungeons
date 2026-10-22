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
    /** F7: caixas, âncoras e margem do HUD (ver LayoutDoHud.desenharDepuracao). */
    public static boolean showHudLayout = false;

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
        if(KeyBoard.KeyPressed("F7")) {
            Debugging.showHudLayout = !Debugging.showHudLayout;
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
        int yGrafico = position.y + (int) (FontHandler.getHeight("Ay", font) * 1.5) * keys.length + 4;
        renderGrafico(g, position.x, yGrafico, Engine.periodoAlvoNs());
        g.dispose();
    }

    // ------------------------------------------------- gráfico de tempo de quadro

    /** Intervalos entre os últimos quadros mostrados, em ns, num anel. */
    private static final long[] INTERVALOS = new long[240];
    private static int posicao;
    private static long ultimoQuadro;

    /** Chamado a cada quadro que chega à tela. Barato: um nanoTime e uma escrita. */
    static void quadroMostrado() {
        long agora = System.nanoTime();
        if (ultimoQuadro != 0) {
            INTERVALOS[posicao] = agora - ultimoQuadro;
            posicao = (posicao + 1) % INTERVALOS.length;
        }
        ultimoQuadro = agora;
    }

    /**
     * O gráfico do F3: uma barra por quadro, a altura é o intervalo até o anterior.
     *
     * A linha clara é o período alvo (o do monitor ou do limite); barras acima dela
     * são quadros atrasados. Uma fileira lisa é ritmo bom, mesmo se o contador de
     * FPS for igual ao de uma fileira serrilhada — que é o que se sente como
     * engasgo. Ao lado, média, desvio e o pior 1%.
     */
    private static void renderGrafico(Graphics2D g, int x, int y, long periodoAlvoNs) {
        int s = Math.max(1, Configs.UiScale());
        int n = INTERVALOS.length;
        int altura = 40 * s;
        double msPorPixel = 33.4 / altura;      // o topo do gráfico é 33 ms (30 FPS)
        g.setColor(background);
        g.fillRect(x, y, n * s + 2 * s, altura + 2 * s);
        long[] copia = new long[n];
        int usados = 0;
        double soma = 0;
        for (int i = 0; i < n; i++) {
            long v = INTERVALOS[(posicao + i) % n];
            if (v <= 0) {
                continue;
            }
            copia[usados++] = v;
            soma += v;
            int h = (int) Math.min(altura, (v / 1e6) / msPorPixel);
            g.setColor(periodoAlvoNs > 0 && v > periodoAlvoNs * 1.5 ? Color.ORANGE : Color.GREEN);
            g.fillRect(x + s + i * s, y + s + altura - h, s, h);
        }
        if (periodoAlvoNs > 0) {
            int ya = y + s + altura - (int) Math.min(altura, (periodoAlvoNs / 1e6) / msPorPixel);
            g.setColor(Color.WHITE);
            g.fillRect(x, ya, n * s + 2 * s, Math.max(1, s / 2));
        }
        if (usados == 0) {
            return;
        }
        double media = soma / usados / 1e6;
        double var = 0;
        for (int i = 0; i < usados; i++) {
            double d = copia[i] / 1e6 - media;
            var += d * d;
        }
        java.util.Arrays.sort(copia, 0, usados);
        double p99 = copia[Math.min(usados - 1, (int) (usados * 0.99))] / 1e6;
        String texto = String.format(java.util.Locale.ROOT, "quadro %.2fms  desvio %.2fms  pior 1%% %.2fms",
                media, Math.sqrt(var / usados), p99);
        renderInfo(texto, x, y + altura + 4 * s, g);
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
