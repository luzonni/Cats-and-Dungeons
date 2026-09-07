package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel de leitura das placas da dungeon.
 *
 * Não usa a moldura de menu do jogo de propósito. A placa é um objeto do mundo,
 * de madeira e pedra; se o texto dela abrisse na chapa carmesim da interface,
 * pareceria que o jogo saiu de cena para exibir uma caixa de sistema. Aqui a
 * tábua cresce até o tamanho da leitura e o jogador continua olhando para a
 * mesma coisa que clicou.
 *
 * As cores são as mesmas rampas de {@code tools/GenKenney.java} — se a madeira
 * do cenário mudar, estas precisam mudar junto.
 */
public class Leitura implements Activity {

    private static final Color MADEIRA_CONTORNO = new Color(0x1c130f);
    private static final Color MADEIRA_ESCURA   = new Color(0x33211a);
    private static final Color MADEIRA          = new Color(0x63432d);
    private static final Color MADEIRA_LUZ      = new Color(0x7d5738);
    private static final Color PREGO            = new Color(0x5a7d81);
    private static final Color TINTA            = new Color(0xf0e0c8);
    private static final Color VEU              = new Color(0, 0, 0, 150);

    /** Largura da tábua, em pixels lógicos. */
    private static final int LARGURA_LOGICA = 150;
    /** Folga de cada lado do texto, em pixels lógicos. */
    private static final int MARGEM_LOGICA = 10;

    private final String texto;

    /**
     * Ticks de carencia antes de aceitar input.
     *
     * Sem eles o mesmo clique que abriu a placa poderia fecha-la no tick
     * seguinte, e o texto piscaria sem dar tempo de ler nada.
     */
    private int carencia = 8;

    private Leitura(String texto) {
        this.texto = texto;
    }

    /** Abre a leitura por cima do jogo. O mundo para enquanto ela estiver aberta. */
    public static void abrir(String texto) {
        Engine.pause(new Leitura(texto));
    }

    /**
     * Fecha em qualquer clique ou tecla.
     *
     * Ler uma placa não é uma decisão: não faz sentido exigir mira num botão de
     * fechar para sair de um parágrafo.
     */
    @Override
    public void tick() {
        if (carencia > 0) {
            carencia--;
            return;
        }
        if (Mouse.click(Mouse_Button.LEFT) || Mouse.click(Mouse_Button.RIGHT)
                || KeyBoard.KeyPressed("Escape") || KeyBoard.KeyPressed("E")) {
            Engine.pause(null);
        }
    }

    @Override
    public void render(Graphics2D g2) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 6f * s);
        int alturaLinha = FontHandler.getHeight("Ay", fonte) + 2 * s;
        int folga = MARGEM_LOGICA * s;

        int largura = LARGURA_LOGICA * s;
        List<String> linhas = quebrar(texto, fonte, largura - folga * 2);
        int altura = folga * 2 + alturaLinha * linhas.size();
        Rectangle p = new Rectangle(
                Engine.window.getWidth() / 2 - largura / 2,
                Engine.window.getHeight() / 2 - altura / 2,
                largura, altura);

        Graphics2D g = (Graphics2D) g2.create();
        g.setColor(VEU);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());

        // Tábua: contorno, corpo, e o mesmo par de faces do cenário — o topo
        // pega luz, a base fica na sombra.
        g.setColor(MADEIRA_CONTORNO);
        g.fillRect(p.x, p.y, p.width, p.height);
        g.setColor(MADEIRA);
        g.fillRect(p.x + s, p.y + s, p.width - 2 * s, p.height - 2 * s);
        g.setColor(MADEIRA_LUZ);
        g.fillRect(p.x + s, p.y + s, p.width - 2 * s, s);
        g.setColor(MADEIRA_ESCURA);
        g.fillRect(p.x + s, p.y + p.height - 2 * s, p.width - 2 * s, s);

        // Pregos nos quatro cantos, como os da placa no chão.
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                g.setColor(PREGO);
                g.fillRect(p.x + 3 * s + dx * (p.width - 8 * s),
                        p.y + 3 * s + dy * (p.height - 8 * s), 2 * s, 2 * s);
            }
        }

        g.setFont(fonte);
        int y = p.y + folga + alturaLinha - 2 * s;
        for (String linha : linhas) {
            int largTexto = FontHandler.getWidth(linha, fonte);
            int x = p.x + (p.width - largTexto) / 2;
            g.setColor(MADEIRA_CONTORNO);
            g.drawString(linha, x + s, y + s);
            g.setColor(TINTA);
            g.drawString(linha, x, y);
            y += alturaLinha;
        }
        g.dispose();
    }

    @Override
    public void dispose() {
    }

    /**
     * Quebra o texto em linhas que cabem na tábua.
     *
     * A medida é a largura REAL da fonte, não a contagem de caracteres. Contando
     * caracteres o texto vazava do painel, e tinha que vazar mesmo: a fonte não é
     * monoespaçada, então quarenta e dois "m" e quarenta e dois "i" ocupam
     * larguras completamente diferentes.
     */
    private static List<String> quebrar(String texto, Font fonte, int larguraUtil) {
        List<String> saida = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            String tentativa = linha.length() == 0 ? palavra : linha + " " + palavra;
            boolean cabe = FontHandler.getWidth(tentativa, fonte) <= larguraUtil;
            if (!cabe && linha.length() > 0) {
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
}
