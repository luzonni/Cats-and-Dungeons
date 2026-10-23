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
        DEFAULTS.put("lighting", true);
        DEFAULTS.put("vsync", true);
        DEFAULTS.put("fullscreen", false);
        DEFAULTS.put("SCALE", 4);
        // Tamanhos em PASSOS a partir do padrão da resolução e margem em pixels de
        // referência (ver EscalaDaInterface): o mesmo valor, a mesma proporção em
        // qualquer resolução.
        DEFAULTS.put("HUD_TAMANHO", EscalaDaInterface.TAMANHO_PADRAO);
        DEFAULTS.put("MENU_TAMANHO", EscalaDaInterface.TAMANHO_PADRAO);
        DEFAULTS.put("MARGEM_REF", EscalaDaInterface.MARGEM_PADRAO);
        DEFAULTS.put("ZOOM", 80);         // por cento; ver Zoom(). Era 130: perto demais para ler a horda chegando
        DEFAULTS.put("NeatGraphics", true);   // barato desde que o OpenGL liga de verdade (Main)
        // OITENTA, E NAO OS VINTE DE ANTES. Vinte nao era gosto de ninguem: era o
        // conserto que sobrou de os arquivos chegarem desnivelados e alguns
        // clipados, entao o jeito de a faixa do chefe nao machucar era cortar tudo
        // a um quinto. Com a regua do tools/GenMixagem.java aplicada, o topo do
        // slider passou a ser o nivel de referencia da mixagem, e oitenta e o que a
        // pesquisa recomenda como padrao: alto o bastante para se jogar assim mesmo
        // e com folga para subir.
        DEFAULTS.put("VOLUM", 80);
        DEFAULTS.put("MUSIC", 80);
        DEFAULTS.put("MASTER", 100);
        // Marca de qual mundo o arquivo veio. Ver migrarAudio.
        DEFAULTS.put("AUDIOVERSION", 2);
        DEFAULTS.put("hitboxes", false);
        // PADRÃO: A TAXA DO MONITOR. Um jogador novo nunca começa com o jogo
        // inconstante: sem VSync, o limite cai no ritmo exato do monitor, e não num
        // número fixo que pode estar acima do que a tela mostra (ver Engine).
        DEFAULTS.put("MaxFrames", LIMITE_MONITOR);
        DEFAULTS.put("resW", 0);           // 0x0 = a resolução do monitor; ver resolucaoLargura
        DEFAULTS.put("resH", 0);
        DEFAULTS.put("BATTLEMUSIC", 0);    // indice em Musics.COMBATE
        DEFAULTS.put("rumble", true);      // vibração do controle, ver Gamepad.vibrar
        DEFAULTS.put("permitirPularAbertura", false);

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
        if (objeto.get("AUDIOVERSION") == null) {
            migrarAudio();
        }
        if (objeto.get("MENU_TAMANHO") == null) {
            migrarEscala(objeto);
        }
        if (objeto.get("resW") == null) {
            migrarResolucao(objeto.get("indexResolution"));
        }
        invalidarCache();
    }

    /**
     * SÓ PARA MEDIR: -Dcd.video.vsync=true|false e -Dcd.video.fps=N trocam o VSync
     * e o limite de quadros EM MEMÓRIA, sem gravar o config.json. Servem para
     * comparar ritmos de quadro (com -Dcd.perf) sem mexer na configuração de quem
     * joga.
     */
    static void sobreporParaDiagnostico() {
        String vsync = System.getProperty("cd.video.vsync");
        if (vsync != null) {
            VALUES.put("vsync", Boolean.parseBoolean(vsync));
        }
        Integer fps = Integer.getInteger("cd.video.fps");
        if (fps != null) {
            VALUES.put("MaxFrames", fps);
        }
        invalidarCache();
    }

    // ------------------------------------------------- leituras de todo quadro

    /**
     * A escala do jogo e o VSync, guardados fora do mapa.
     *
     * Os dois sao lidos o tempo todo: a escala por cada bicho desenhado, varias
     * vezes (tamanho do tile, posicao interpolada, animacao), e o VSync a cada
     * volta do laco principal, inclusive na espera entre quadros. Cada leitura do
     * mapa era um hash de texto mais uma busca — somadas, ~6% do tempo da thread
     * do jogo com a horda cheia, medido. Guardados num campo, viram uma leitura de
     * memoria. O cache e jogado fora sempre que o mapa muda (update, load e os
     * setters), entao nunca devolve um valor velho.
     */
    private static volatile int escalaEmCache = -1;
    private static volatile int vsyncEmCache = -1;

    private static void invalidarCache() {
        escalaEmCache = -1;
        vsyncEmCache = -1;
    }

    /**
     * Converte volumes gravados no mundo linear para a curva nova.
     *
     * A AUSENCIA DA CHAVE E A PROPRIA MARCA. Um config.json gravado antes desta
     * mudanca nao tem "AUDIOVERSION"; um recem-criado tem, porque sai dos padroes.
     * Nao ha como confundir os dois casos, e por isso a migracao nao precisa de
     * nenhum estado extra — ela roda uma vez e o proximo update() ja grava a
     * chave, o que a desliga para sempre.
     *
     * POR QUE MIGRAR EM VEZ DE DEIXAR. Os numeros antigos valiam ganho direto:
     * MUSIC 20 era 0,20, ou -14 dB. Lidos pela curva nova os mesmos 20 dariam
     * -32 dB, dezoito decibeis abaixo. Quem ja jogava abriria o jogo, ouviria silencio e
     * concluiria — com razao — que a atualizacao quebrou o som. Convertido, 20
     * vira 65 e a pessoa continua ouvindo o que ouvia.
     */
    private static void migrarAudio() {
        for (String chave : new String[] {"VOLUM", "MUSIC"}) {
            int antigo = inteiro(chave);
            VALUES.put(chave, com.retronova.engine.sound.Ganho.posicaoDe(antigo / 100d));
        }
        update();
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
        invalidarCache();
        if (Foto.ligado()) {
            return;     // o roteiro de fotos mexe nas opcoes; o config.json de quem joga fica intacto
        }
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

    /**
     * Mostrar as caixas de colisão.
     *
     * Fica gravado, e não só na tecla, porque quem liga isso está depurando e
     * costuma reabrir o jogo várias vezes seguidas — reapertar F4 toda vez é
     * atrito à toa.
     */
    public static boolean Hitboxes() {
        return (boolean) VALUES.get("hitboxes");
    }

    public static void setHitboxes(boolean mostrar) {
        VALUES.put("hitboxes", mostrar);
    }

    public static boolean Vignette() {
        return (boolean) VALUES.get("vignette");
    }

    public static void setVignette(boolean vignette) {
        VALUES.put("vignette", vignette);
        update();
    }

    /** Sombras e iluminacao dinamica no mundo. Ver game.map.Atmosfera. */
    public static boolean Lighting() {
        Object v = VALUES.get("lighting");
        return v == null || (boolean) v;
    }

    public static void setLighting(boolean lighting) {
        VALUES.put("lighting", lighting);
        update();
    }

    /** Quadro sincronizado com o monitor. Ver Engine.criarComVSync. */
    public static boolean VSync() {
        int c = vsyncEmCache;
        if (c < 0) {
            Object v = VALUES.get("vsync");
            c = (v == null || (boolean) v) ? 1 : 0;
            vsyncEmCache = c;
        }
        return c == 1;
    }

    public static void setVSync(boolean vsync) {
        invalidarCache();
        VALUES.put("vsync", vsync);
        update();
        Engine.invalidateBuffer();   // o buffer e recriado no proximo quadro
    }

    public static boolean Fullscreen() {
        return (boolean) VALUES.get("fullscreen");
    }

    public static void setFullscreen(boolean fullscreen) {
        VALUES.put("fullscreen", fullscreen);
        update();
    }

    public static int GameScale() {
        int c = escalaEmCache;
        if (c < 0) {
            c = inteiro("SCALE");
            escalaEmCache = c;
        }
        return c;
    }

    public static void setGameScale(int scale) {
        invalidarCache();
        VALUES.put("SCALE", scale);
        update();
    }

    /**
     * A resolução escolhida, em pixels: o tamanho da imagem do jogo. 0x0 é "a do
     * monitor", o padrão da primeira execução.
     *
     * GRAVADA COMO LARGURA E ALTURA, e não como índice. O índice apontava para uma
     * lista fixa, e a lista agora depende do monitor (ver Window.resolucoesDoMonitor):
     * o mesmo índice viraria outra resolução ao trocar de monitor. Quem lê a
     * resolução de fato é {@link Window#resolucao()}, que cai na do monitor quando a
     * gravada não cabe nele.
     */
    public static int resolucaoLargura() {
        return inteiro("resW");
    }

    public static int resolucaoAltura() {
        return inteiro("resH");
    }

    public static void setResolucao(int largura, int altura) {
        VALUES.put("resW", largura);
        VALUES.put("resH", altura);
        update();
    }

    /** Arquivo de antes da resolução por tamanho: traduz o índice da lista antiga. */
    private static void migrarResolucao(Object indiceAntigo) {
        int[][] antiga = {{1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};
        int i = indiceAntigo instanceof Number n ? n.intValue() : -1;
        if (i >= 0 && i < antiga.length) {
            VALUES.put("resW", antiga[i][0]);
            VALUES.put("resH", antiga[i][1]);
        }
    }

    // ------------------------------------------------- escala da interface
    //
    // TODA A CONTA MORA EM EscalaDaInterface; aqui só as preferências gravadas e as
    // leituras de todo quadro, que passam por lá com o tamanho da imagem do jogo.
    // Os tamanhos são PASSOS a partir do padrão da resolução (0 = Default), e a
    // margem em pixels de referência: o mesmo valor dá a mesma proporção em
    // qualquer resolução.

    private static int largura() {
        return Engine.window == null ? 0 : Engine.window.largura();
    }

    private static int altura() {
        return Engine.window == null ? 0 : Engine.window.altura();
    }

    /** Pixels de tela por pixel de arte dos menus: o padrão da tela mais o passo escolhido. */
    public static int UiScale() {
        return faixaDosMenus().escala(MenuTamanho());
    }

    /**
     * Pixels de tela por pixel de arte do HUD EM VIGOR: o padrão da tela mais o
     * passo escolhido, no teto do que cabe sem sobrepor peças (ver LayoutDoHud).
     */
    public static int HudScale() {
        return faixaDoHud().escala(HudTamanho());
    }

    public static EscalaDaInterface.Faixa faixaDoHud() {
        return EscalaDaInterface.faixaDoHud(largura(), altura(), HudScaleMaxima());
    }

    public static EscalaDaInterface.Faixa faixaDosMenus() {
        return EscalaDaInterface.faixaDosMenus(largura(), altura());
    }

    /** A maior escala do HUD que cabe na imagem do jogo agora. */
    public static int HudScaleMaxima() {
        if (Engine.window == null) {
            return Integer.MAX_VALUE;
        }
        return com.retronova.game.hud.LayoutDoHud.escalaMaxima(largura(), altura(), Margin());
    }

    /** A margem das bordas em pixels de tela: a de referência, na escala da tela. */
    public static int Margin() {
        return EscalaDaInterface.margem(largura(), altura(), MargemDeReferencia());
    }

    /** O passo gravado do HUD (0 = Default); a tela pode comportar menos, ver faixaDoHud. */
    public static int HudTamanho() {
        return EscalaDaInterface.tamanhoValido(inteiro("HUD_TAMANHO"));
    }

    public static int MenuTamanho() {
        return EscalaDaInterface.tamanhoValido(inteiro("MENU_TAMANHO"));
    }

    public static int MargemDeReferencia() {
        return Math.max(0, Math.min(EscalaDaInterface.MARGEM_MAXIMA, inteiro("MARGEM_REF")));
    }

    public static void setHudTamanho(int tamanho) {
        VALUES.put("HUD_TAMANHO", EscalaDaInterface.tamanhoValido(tamanho));
        update();
    }

    public static void setMenuTamanho(int tamanho) {
        VALUES.put("MENU_TAMANHO", EscalaDaInterface.tamanhoValido(tamanho));
        update();
    }

    public static void setMargemDeReferencia(int margem) {
        VALUES.put("MARGEM_REF", Math.max(0, Math.min(EscalaDaInterface.MARGEM_MAXIMA, margem)));
        update();
    }

    /** Volta tamanhos e margem da interface ao padrão. */
    public static void restaurarInterface() {
        VALUES.put("HUD_TAMANHO", EscalaDaInterface.TAMANHO_PADRAO);
        VALUES.put("MENU_TAMANHO", EscalaDaInterface.TAMANHO_PADRAO);
        VALUES.put("MARGEM_REF", EscalaDaInterface.MARGEM_PADRAO);
        update();
    }

    /**
     * Arquivo de antes dos passos de tamanho: traduz as preferências antigas pela
     * escala que davam em 1080p, onde o padrão do HUD e dos menus é 3.
     *
     *   - Interface/HUD/Menus em por cento (a primeira versão desta escala): o
     *     passo é a escala de 1080p menos 3;
     *   - HUD size, o multiplicador cru, vira passo a partir de 3;
     *   - GUI size vira Default: o "3" automático era o tamanho grande demais;
     *   - Margin em pixels vira pixels de referência (um terço, o fator de 1080p).
     */
    private static void migrarEscala(JSONObject antigo) {
        double ui = antigo.get("UI_PCT") instanceof Number n ? n.doubleValue() / 100 : 1;
        if (antigo.get("HUD_PCT") instanceof Number n) {
            VALUES.put("HUD_TAMANHO", EscalaDaInterface.tamanhoValido(
                    (int) Math.round(3 * ui * n.doubleValue() / 100) - 3));
        } else if (antigo.get("HUDSCALE") instanceof Number n) {
            VALUES.put("HUD_TAMANHO", EscalaDaInterface.tamanhoValido(n.intValue() - 3));
        }
        if (antigo.get("MENUS_PCT") instanceof Number n) {
            VALUES.put("MENU_TAMANHO", EscalaDaInterface.tamanhoValido((int) Math.round(
                    3 * EscalaDaInterface.MENUS_POR_REFERENCIA * ui * n.doubleValue() / 100) - 3));
        }
        if (antigo.get("MARGEM_REF") == null && antigo.get("MARGIN") instanceof Number n) {
            VALUES.put("MARGEM_REF", Math.max(0, Math.min(EscalaDaInterface.MARGEM_MAXIMA,
                    (int) Math.round(n.intValue() / 3.0))));
        }
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

    /**
     * O controle que manda em tudo, musica e efeito.
     *
     * Existe porque baixar o jogo inteiro exigia mexer em dois controles, e quem
     * so quer abaixar o volume nao esta querendo remixar nada. E o primeiro item
     * das recomendacoes de tela de audio justamente por isso.
     */
    public static int Master() {
        return inteiro("MASTER");
    }

    public static void setMaster(int MASTER) {
        VALUES.put("MASTER", MASTER);
        update();
    }

    public static int Music() {
        return inteiro("MUSIC");
    }

    public static void setMusic(int MUSIC) {
        VALUES.put("MUSIC", MUSIC);
        update();
    }

    /** Qual das trilhas de combate tocar. Ver Musics.COMBATE. */
    public static int BattleMusic() {
        return inteiro("BATTLEMUSIC");
    }

    public static void setBattleMusic(int indice) {
        VALUES.put("BATTLEMUSIC", indice);
        update();
    }

    /** Vibração do controle. Ver inputs.gamepad.Gamepad. */
    public static boolean Rumble() {
        Object v = VALUES.get("rumble");
        return v == null || (boolean) v;
    }

    public static void setRumble(boolean rumble) {
        VALUES.put("rumble", rumble);
        update();
    }

    /** PULO SÓ COM PERMISSÃO: uma configuração antiga ou nova começa sem liberar. */
    public static boolean PermitirPularAbertura() {
        return Boolean.TRUE.equals(VALUES.get("permitirPularAbertura"));
    }

    public static void setPermitirPularAbertura(boolean permitir) {
        VALUES.put("permitirPularAbertura", permitir);
        update();
    }

    /** Limite de quadros "igual ao monitor": a taxa detectada (ver Engine.taxaDoMonitor). */
    public static final int LIMITE_MONITOR = 0;
    /** Sem limite de quadros: desenha o mais rápido que der. */
    public static final int SEM_LIMITE = -1;

    /**
     * O limite de quadros sem VSync: um número de quadros por segundo,
     * {@link #LIMITE_MONITOR} ou {@link #SEM_LIMITE}. Com VSync ligado ele não vale:
     * quem dita o ritmo é o monitor.
     */
    public static int MaxFrames() {
        return inteiro("MaxFrames");
    }

    public static void setMaxFrames(int MaxFrames) {
        VALUES.put("MaxFrames", MaxFrames);
        update();
    }

}
