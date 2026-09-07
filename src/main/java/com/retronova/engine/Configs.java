package com.retronova.engine;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preferências do jogo, persistidas em config.json ao lado do executável.
 *
 * A gravação é atômica — escreve num arquivo temporário e o move por cima do
 * definitivo. A versão anterior truncava o arquivo com {@code FileWriter} antes
 * de escrever: qualquer interrupção no meio (fechar o jogo, travar, matar o
 * processo) deixava um JSON pela metade. E como o erro de leitura era engolido
 * por um {@code catch} vazio, na abertura seguinte tudo voltava calado para os
 * padrões e o primeiro ajuste regravava por cima, perdendo as preferências de
 * vez. Era esse o "não salva nada".
 */
public class Configs {

    private static final String PATH = "config.json";

    /** Valores de fábrica. Também definem o tipo esperado de cada chave. */
    private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();

    private static Map<String, Object> VALUES;
    private static boolean hookRegistrado;

    public static void init() {
        DEFAULTS.clear();
        DEFAULTS.put("vignette", true);
        DEFAULTS.put("fullscreen", false);
        DEFAULTS.put("SCALE", 4);
        DEFAULTS.put("UISCALE", 0);        // 0 = automática, ver autoUiScale()
        DEFAULTS.put("HUDSCALE", 4);
        DEFAULTS.put("MARGIN", 20);
        DEFAULTS.put("ZOOM", 130);        // por cento; ver Zoom()
        DEFAULTS.put("NeatGraphics", false);
        DEFAULTS.put("VOLUM", 20);
        DEFAULTS.put("MUSIC", 20);
        DEFAULTS.put("MaxFrames", 60);
        DEFAULTS.put("indexResolution", 0);

        VALUES = new LinkedHashMap<>(DEFAULTS);
        registrarFlushNoEncerramento();
    }

    /**
     * Rede de segurança: fechar a janela dispara {@code System.exit}, que executa
     * os shutdown hooks. Assim nada depende de o último ajuste ter gravado.
     */
    private static void registrarFlushNoEncerramento() {
        if (hookRegistrado) {
            return;
        }
        hookRegistrado = true;
        Runtime.getRuntime().addShutdownHook(new Thread(Configs::update, "Configs-flush"));
    }

    public static void load() {
        Path arquivo = Paths.get(PATH);
        if (!Files.exists(arquivo)) {
            update();
            return;
        }
        JSONObject objeto;
        try {
            String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
            objeto = (JSONObject) new JSONParser().parse(semBom(conteudo));
        } catch (Exception e) {
            // Falhar em silêncio aqui era o pior dos mundos: o jogador perdia tudo
            // sem entender por quê. Avisa e guarda o arquivo quebrado para análise.
            System.err.println("Configs: não consegui ler " + PATH + " (" + e + ").");
            preservarArquivoQuebrado(arquivo);
            update();
            return;
        }
        if (objeto == null) {
            update();
            return;
        }
        for (Map.Entry<String, Object> padrao : DEFAULTS.entrySet()) {
            aplicar(padrao.getKey(), objeto.get(padrao.getKey()), padrao.getValue());
        }
    }

    /**
     * Remove a marca de ordem de bytes do início do texto.
     *
     * O Bloco de Notas e o {@code Set-Content} do PowerShell gravam UTF-8 com BOM,
     * e o parser de JSON trata esse caractere invisível como lixo na posição 0 —
     * o arquivo parece perfeito na tela e mesmo assim é rejeitado.
     */
    private static String semBom(String texto) {
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }

    /** Aceita o valor lido só quando o tipo bate com o do padrão. */
    private static void aplicar(String chave, Object lido, Object padrao) {
        if (lido == null) {
            return;                 // chave ausente: fica o padrão, sem alarde
        }
        if (padrao instanceof Boolean && lido instanceof Boolean) {
            VALUES.put(chave, lido);
        } else if (padrao instanceof Number && lido instanceof Number) {
            VALUES.put(chave, ((Number) lido).intValue());
        } else {
            System.err.println("Configs: valor inválido para '" + chave + "' (" + lido
                    + "), mantendo o padrão " + padrao + ".");
        }
    }

    private static void preservarArquivoQuebrado(Path arquivo) {
        try {
            Path backup = Paths.get(PATH + ".bak");
            Files.move(arquivo, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Configs: arquivo anterior preservado em " + backup + ".");
        } catch (IOException e) {
            System.err.println("Configs: não consegui preservar o arquivo quebrado: " + e);
        }
    }

    /**
     * Grava as preferências. Escreve num temporário e só então o move sobre o
     * arquivo real, para nunca existir um config.json pela metade no disco.
     */
    @SuppressWarnings("unchecked")
    public static synchronized void update() {
        if (VALUES == null) {
            return;
        }
        JSONObject objeto = new JSONObject();
        objeto.putAll(VALUES);

        Path destino = Paths.get(PATH);
        Path temporario = Paths.get(PATH + ".tmp");
        try {
            Files.writeString(temporario, objeto.toJSONString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporario, destino,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException semAtomico) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Configs: falha ao salvar as configurações: " + e);
        }
    }

    private static int inteiro(String chave) {
        return ((Number) VALUES.get(chave)).intValue();
    }

    public static boolean Vignette() {
        return (boolean) VALUES.get("vignette");
    }

    public static void setVignette(boolean vignette) {
        VALUES.put("vignette", vignette);
        update();
    }

    public static boolean Fullscreen() {
        return (boolean) VALUES.get("fullscreen");
    }

    public static void setFullscreen(boolean fullscreen) {
        VALUES.put("fullscreen", fullscreen);
        update();
    }

    public static int GameScale() {
        return inteiro("SCALE");
    }

    public static void setGameScale(int scale) {
        VALUES.put("SCALE", scale);
        update();
    }

    /** Índice em {@link Engine#resolutions}, sempre dentro dos limites do vetor. */
    public static int getIndexResolution() {
        int indice = inteiro("indexResolution");
        return Math.max(0, Math.min(Engine.resolutions.length - 1, indice));
    }

    public static void setIndexResolution(int resolution) {
        VALUES.put("indexResolution", resolution);
        update();
    }

    /** Resolução lógica de referência para a UI. Múltiplos inteiros dela dão a escala. */
    private static final int REF_W = 320, REF_H = 180;

    /**
     * Escala da interface. Zero significa automática: a UI acompanha o tamanho
     * da janela em vez de ficar minúscula em telas grandes.
     */
    public static int UiScale() {
        return UiScaleSetting() > 0 ? UiScaleSetting() : autoUiScale();
    }

    /** Valor cru da preferência, sem resolver o automático. 0 = automática. */
    public static int UiScaleSetting() {
        return inteiro("UISCALE");
    }

    /**
     * Maior múltiplo inteiro de 320x180 que cabe na janela, limitado entre 2 e 10.
     * Só múltiplos inteiros: fator fracionário desalinha e borra a pixel art.
     */
    public static int autoUiScale() {
        if (Engine.window == null) {
            return 3;
        }
        int largura = Engine.window.getWidth();
        int altura = Engine.window.getHeight();
        if (largura <= 0 || altura <= 0) {
            return 3;
        }
        int escala = Math.min(largura / REF_W, altura / REF_H);
        return Math.max(2, Math.min(10, escala));
    }

    public static void setUiScale(int uiScale) {
        VALUES.put("UISCALE", uiScale);
        update();
    }

    /**
     * Aproximacao da camera, em por cento.
     *
     * Cem por cento e a distancia antiga, em que o salao inteiro cabia na tela e
     * nao sobrava nada para explorar. O padrao e 130: perto o bastante para o
     * gato ser o centro da cena, longe o bastante para ainda se enxergar o que
     * vem pela frente. A faixa e curta de proposito — passar disso deixa de ser
     * escolha de conforto e vira outro jogo.
     */
    public static int Zoom() {
        return inteiro("ZOOM");
    }

    public static void setZoom(int zoom) {
        VALUES.put("ZOOM", zoom);
    }

    public static int HudScale() {
        return inteiro("HUDSCALE");
    }

    public static void setHudScale(int HUDSCALE) {
        VALUES.put("HUDSCALE", HUDSCALE);
        update();
    }

    public static int Margin() {
        return inteiro("MARGIN");
    }

    public static void setMargin(int MARGIN) {
        VALUES.put("MARGIN", MARGIN);
        update();
    }

    public static boolean isNeatGraphics() {
        return (boolean) VALUES.get("NeatGraphics");
    }

    public static void setNeatGraphics(boolean NeatGraphics) {
        VALUES.put("NeatGraphics", NeatGraphics);
        update();
    }

    public static int Volum() {
        return inteiro("VOLUM");
    }

    public static void setVolum(int VOLUM) {
        VALUES.put("VOLUM", VOLUM);
        update();
    }

    public static int Music() {
        return inteiro("MUSIC");
    }

    public static void setMusic(int MUSIC) {
        VALUES.put("MUSIC", MUSIC);
        update();
    }

    public static int MaxFrames() {
        return inteiro("MaxFrames");
    }

    public static void setMaxFrames(int MaxFrames) {
        VALUES.put("MaxFrames", MaxFrames);
        update();
    }

}
