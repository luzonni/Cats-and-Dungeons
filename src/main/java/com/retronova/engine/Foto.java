package com.retronova.engine;

import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Modo de foto, para desenvolvimento: o jogo segue um roteiro e salva as telas em PNG.
 *
 *     -Dcd.foto=<pasta>                      liga o modo e diz onde salvar
 *     -Dcd.foto.tamanho=1280x720             tamanho do canvas (padrao: o das opcoes)
 *     -Dcd.foto.roteiro="espera:60;foto:menu;tecla:Down;foto:foco;mouse:0.5,0.6;foto:hover"
 *
 * Passos do roteiro, separados por ponto e virgula:
 *
 *     espera:N        N ticks sem fazer nada (60 = um segundo)
 *     tecla:Nome      aperta a tecla por um tick (os nomes do KeyBoard: Down, Enter, Escape...)
 *     mouse:x,y       leva o ponteiro a (x, y); com ponto decimal e fracao da tela (0.5 = meio)
 *     clique          clique esquerdo onde o ponteiro esta
 *     foto:nome       salva a tela em <pasta>/nome.png
 *
 * Quando o roteiro acaba, o jogo fecha. Combina com o modo bancada (-Dcd.bench) para
 * fotografar telas de dentro da partida.
 *
 * POR QUE POR DENTRO. Fotografar a janela com uma captura de tela do sistema pega o
 * que estiver na frente dela — e mandar teclas pelo sistema digita no programa que
 * estiver em foco. Aqui as teclas e o mouse entram direto no estado de entrada do
 * jogo, e a imagem sai do mesmo desenho que vai para a tela, so que num
 * BufferedImage. A janela abre fora da area visivel, sem foco e sem icone na barra
 * de tarefas (ver Window), entao rodar isto nao atrapalha quem esta usando a maquina.
 */
public final class Foto {

    private Foto() {
    }

    private static final String PASTA = System.getProperty("cd.foto");

    /** O modo de foto esta ligado? */
    public static boolean ligado() {
        return PASTA != null;
    }

    /**
     * Onde os saves moram. No modo de foto, numa pasta descartavel dentro de build/:
     * o roteiro entra em corridas, mexe nas opcoes e termina partidas, e nada disso
     * pode consumir ou sobrescrever as corridas e o progresso de quem joga nesta
     * maquina. O config.json tambem nao e gravado (ver Configs.update).
     */
    public static String pastaDeSaves() {
        return ligado() ? "build/foto/saves" : "saves";
    }

    /**
     * Com -Dcd.foto.real=true, a janela vai para a tela de verdade (sem pegar o
     * foco), no tamanho das opções: é o modo para testar resolução, tela cheia e
     * perda de superfície, que a janela escondida do modo de foto nunca exercita.
     * Saves e config continuam isolados.
     */
    public static boolean janelaReal() {
        return ligado() && Boolean.getBoolean("cd.foto.real");
    }

    /** O tamanho pedido para o canvas, ou null para usar o das opcoes. */
    public static Dimension tamanho() {
        String t = System.getProperty("cd.foto.tamanho");
        if (t == null) {
            return null;
        }
        String[] p = t.toLowerCase(Locale.ROOT).split("x");
        return new Dimension(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()));
    }

    private static Deque<String> passos;
    private static int esperando;
    /** Tecla apertada no tick anterior, para soltar no seguinte. */
    private static String soltar;
    private static boolean soltarMouse;

    /** Roda um passo do roteiro por tick. Chamado pelo Engine antes das telas. */
    static void tick() {
        if (!ligado()) {
            return;
        }
        if (passos == null) {
            passos = new ArrayDeque<>();
            for (String p : System.getProperty("cd.foto.roteiro", "espera:60;foto:tela").split(";")) {
                if (!p.isBlank()) {
                    passos.add(p.trim());
                }
            }
        }
        if (soltar != null) {
            KeyBoard.simular(soltar, false);
            soltar = null;
        }
        if (soltarMouse) {
            Mouse.simularSoltar();
            soltarMouse = false;
        }
        if (esperando > 0) {
            esperando--;
            return;
        }
        if (passos.isEmpty()) {
            System.out.println("[foto] roteiro concluido");
            System.exit(0);
        }
        String passo = passos.poll();
        String[] partes = passo.split(":", 2);
        String arg = partes.length > 1 ? partes[1] : "";
        switch (partes[0]) {
            case "espera" -> esperando = Integer.parseInt(arg);
            case "tecla" -> {
                KeyBoard.simular(arg, true);
                soltar = arg;
            }
            case "mouse" -> {
                String[] xy = arg.split(",");
                Mouse.simularPosicao(coordenada(xy[0], Engine.window.largura()),
                        coordenada(xy[1], Engine.window.altura()));
            }
            case "clique" -> {
                Mouse.simularClique();
                soltarMouse = true;
            }
            case "foto" -> salvar(arg);
            case "tela" -> salvar(arg, true);
            // video:<L>x<A>:<janela|cheia> — grava e aplica como o Apply das opções.
            case "video" -> {
                String[] v = arg.split(":");
                String[] r = v[0].trim().toLowerCase(Locale.ROOT).split("x");
                Configs.setResolucao(Integer.parseInt(r[0]), Integer.parseInt(r[1]));
                Configs.setFullscreen(v.length > 1 && v[1].trim().equals("cheia"));
                Engine.window.aplicarVideo();
            }
            default -> throw new IllegalArgumentException("Passo desconhecido no roteiro: " + passo);
        }
    }

    private static int coordenada(String v, int total) {
        v = v.trim();
        return v.contains(".") ? (int) Math.round(Double.parseDouble(v) * total) : Integer.parseInt(v);
    }

    private static void salvar(String nome) {
        salvar(nome, false);
    }

    /**
     * Grava o quadro. Com {@code naTela}, o quadro como chega ao canvas — escalado
     * e com as tarjas, quando a imagem do jogo não tem o tamanho dele —, e não só
     * a imagem do jogo: é o que mostra a tela cheia em resolução menor sem
     * capturar a tela do sistema.
     */
    private static void salvar(String nome, boolean naTela) {
        int w = naTela ? Engine.window.getCanvasLargura() : Engine.window.largura();
        int h = naTela ? Engine.window.getCanvasAltura() : Engine.window.altura();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        if (naTela) {
            Engine.desenharNaTela(g);
        } else {
            Engine.desenharTudo(g);
        }
        g.dispose();
        File destino = new File(PASTA, nome + ".png");
        try {
            destino.getParentFile().mkdirs();
            ImageIO.write(img, "png", destino);
            System.out.println("[foto] " + destino.getAbsolutePath() + " (" + w + "x" + h + ")");
        } catch (IOException e) {
            throw new IllegalStateException("Nao consegui salvar " + destino, e);
        }
    }
}
