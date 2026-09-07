package com.retronova.menus;

import com.retronova.engine.ActionBack;
import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.SpriteHandler;

import java.awt.AlphaComposite;
import com.retronova.engine.graphics.Palette;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Tela de transição entre uma activity e outra.
 *
 * POR QUE ELA DEMORA DE PROPÓSITO
 *
 * A carga em si termina em milissegundos, e antes esta tela aparecia por 100 ms
 * — tempo em que o olho não registra nada. O resultado era um piscar entre o
 * menu e o jogo, e a partida começava sem que ninguém tivesse chegado a lugar
 * nenhum.
 *
 * A literatura de UX é consistente nisso: um indicador que aparece e some em
 * dezenas de milissegundos é lido como falha de renderização, não como
 * velocidade, e a recomendação é um tempo mínimo de exibição na casta dos
 * 300–600 ms só para o estado ser percebido. Em jogo o argumento vai além do
 * feedback: a pausa antes de entrar é ritmo — dá ao jogador o momento de
 * antecipação que separar duas telas exige. É o mesmo princípio das portas do
 * Resident Evil, que existiam para carregar e viraram tensão.
 *
 * O limite do outro lado também é conhecido: espera longa cansa. Por isso o
 * mínimo aqui é pouco mais de um segundo — o bastante para a transição existir,
 * curto o bastante para ninguém reclamar dela na décima corrida.
 */
public class Loading implements Activity, Runnable {

    /** Tempo mínimo em tela, em ticks de 60 Hz. */
    private static final int MINIMO = 75;
    /** Ticks das aberturas e fechamentos de véu. */
    private static final int FADE = 14;

    /** O tom mais fundo da arte: tela cheia pede o fundo do fundo. */
    private static final Color FUNDO = Palette.DARKEST;
    private static final Color TEXTO = Palette.TEXT;

    /**
     * Quem anda na tela.
     *
     * Sorteia entre os tres gatos jogaveis a cada transicao. O indicador antes
     * era um anel girando, que nao dizia nada sobre este jogo — e um gato andando
     * e o simbolo que o jogo ja tem, sem precisar de arte nova.
     */
    private static final String[] GATOS = {"azrael", "finn", "muffin"};
    /** Ticks por quadro da caminhada. Sete quadros nesse passo dao um andar calmo. */
    private static final int TICKS_POR_QUADRO = 8;
    /** Quantas patinhas o rastro chega a mostrar antes de recomecar. */
    private static final int RASTRO = 4;

    /**
     * Frases exibidas durante a espera.
     *
     * São o motivo de a pausa não ser tempo morto: dão o que ler e plantam
     * contexto antes da corrida. Textos provisórios — a lore ainda vai ser
     * escrita.
     */
    private static final String[] FRASES = {
        "Three brothers went down. Only the walls remember why.",
        "The deeper floors were carved by something that did not need stairs.",
        "Every torch in the antechamber was lit by someone who came back.",
        "The gate only opens one way.",
        "Whatever wears your brother's face is not your brother.",
    };

    private final Thread thread;

    private final List<Activity> stack;
    private final Activity next;
    private final ActionBack action;
    private volatile boolean finish;
    private boolean start;

    private final BufferedImage[] passos;
    private final BufferedImage patinha;
    private final String frase;
    private int ticks;
    /** Tick em que o fechamento comecou. -1 enquanto a tela ainda esta cheia. */
    private int saida = -1;

    public Loading(List<Activity> stack, Activity next, ActionBack action) {
        this.thread = new Thread(this, "Loading-Thread");
        this.stack = stack;
        this.next = next;
        this.action = action;
        this.finish = false;
        this.start = false;
        String gato = GATOS[Engine.RAND.nextInt(GATOS.length)];
        SpriteHandler folha = new SpriteHandler("sprites/objects/player", "player_" + gato + "_walking", 1);
        this.passos = new BufferedImage[folha.getWidth() / 16];
        for (int i = 0; i < passos.length; i++) {
            this.passos[i] = folha.getSpriteWithIndex(i, 0);
        }
        this.patinha = new SpriteHandler("ui", "cursor_hover", 1).getSHEET();
        this.frase = FRASES[Engine.RAND.nextInt(FRASES.length)];
    }

    @Override
    public synchronized void tick() {
        if (!start) {
            start = true;
            this.thread.start();
        }
        ticks++;
        // O fechamento so comeca quando o trabalho acabou E o tempo minimo
        // passou. As duas condicoes: uma garante que esta pronto, a outra que
        // foi visto. Marcar o instante, em vez de comparar com MINIMO na hora de
        // desenhar, mantem o fade correto tambem quando a carga demora mais que
        // o minimo — ai nao ha espera artificial nenhuma, so o fade.
        if (finish && saida < 0 && ticks >= MINIMO - FADE) {
            saida = ticks;
        }
        if (saida >= 0 && ticks - saida >= FADE) {
            stack.remove(this);
            stack.add(next);
        }
    }

    @Override
    public void render(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setColor(FUNDO);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacidade()));

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int s = Configs.UiScale();
        int lado = 16 * s * 2;
        int meio = Engine.window.getWidth() / 2;
        int chao = Engine.window.getHeight() / 2;

        // Rastro de patinhas atras do gato, aparecendo uma a uma. E o que da a
        // leitura de progresso que o anel girando dava, so que dentro do jogo.
        int quantas = (ticks / (TICKS_POR_QUADRO * 2)) % (RASTRO + 1);
        java.awt.Composite cheio = g.getComposite();
        for (int i = 0; i < quantas; i++) {
            int px = meio - lado - (i + 1) * 10 * s;
            int py = chao - 8 * s + (i % 2 == 0 ? 0 : 5 * s);
            // As mais antigas ficam mais fracas: o rastro apaga para tras, como
            // pegada que o chao vai comendo. Alem de bonito, mantem a patinha
            // branca do cursor de coadjuvante, sem competir com o gato.
            float alfa = opacidade() * (0.5f - i * 0.09f);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.08f, alfa)));
            g.drawImage(patinha, px, py, patinha.getWidth() * s, patinha.getHeight() * s, null);
        }
        g.setComposite(cheio);

        BufferedImage quadro = passos[(ticks / TICKS_POR_QUADRO) % passos.length];
        g.drawImage(quadro, meio - lado / 2, chao - lado, lado, lado, null);

        // A frase quebra em linhas se nao couber: em janela pequena com escala de
        // UI grande, uma linha so passava da largura da tela.
        Font fonte = FontHandler.font(FontHandler.Game, 6f * s);
        int util = Engine.window.getWidth() - 40 * s;
        g.setFont(fonte);
        g.setColor(TEXTO);
        int y = chao + 22 * s;
        for (String linha : quebrar(frase, fonte, util)) {
            int largura = FontHandler.getWidth(linha, fonte);
            g.drawString(linha, meio - largura / 2, y);
            y += FontHandler.getHeight("Ay", fonte) + 3 * s;
        }
        g.dispose();
    }

    /**
     * Véu de entrada e de saída.
     *
     * O conteúdo aparece e some em rampa em vez de aparecer chapado. Sem isso a
     * transição ganha duas cortes secos onde deveria haver um respiro só.
     */
    private float opacidade() {
        if (ticks < FADE) {
            return ticks / (float) FADE;
        }
        if (saida >= 0) {
            return Math.max(0f, 1f - (ticks - saida) / (float) FADE);
        }
        return 1f;
    }

    private static List<String> quebrar(String texto, Font fonte, int larguraUtil) {
        List<String> saida = new java.util.ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            String tentativa = linha.length() == 0 ? palavra : linha + " " + palavra;
            if (FontHandler.getWidth(tentativa, fonte) > larguraUtil && linha.length() > 0) {
                saida.add(linha.toString());
                linha.setLength(0);
                linha.append(palavra);
            } else {
                linha.setLength(0);
                linha.append(tentativa);
            }
        }
        if (linha.length() > 0) {
            saida.add(linha.toString());
        }
        return saida;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void run() {
        action.action();
        finish = true;
    }
}
