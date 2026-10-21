package com.retronova.menus;

import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Conquistas;
import com.retronova.game.Conquistas.Conquista;
import com.retronova.game.Conquistas.Progresso;
import com.retronova.game.map.arena.Icones;
import com.retronova.menus.shared.Button;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * A tela de conquistas, aberta pelo troféu do menu principal.
 *
 * Cada conquista é um cartão: ícone, nome, descrição e uma barra de quanto falta.
 * As desbloqueadas acendem em dourado; as bloqueadas ficam apagadas MAS MOSTRAM o
 * que pedem e quanto falta, porque uma meta escondida não puxa ninguém a jogar de
 * outro jeito. As recém-desbloqueadas levam a etiqueta NEW até esta tela fechar.
 */
public class TelaDeConquistas extends TelaDeMenu {

    private static final Color OURO = new Color(0xFFD23F);
    private static final Color APAGADO = new Color(0x5A5A6E);

    private static final int COLUNAS = 2;
    private static final Conquista[] TODAS = Conquista.values();
    private static final int LINHAS = (TODAS.length + COLUNAS - 1) / COLUNAS;

    private final Button voltar = new Button(0, 0, 0, 0, "Back", b -> voltar()).primary();
    private final Progresso progresso = Conquistas.progresso();

    public TelaDeConquistas() {
        super("Achievements");
        corDoTitulo(OURO);
        Sound.play(Sounds.Button);
    }

    @Override
    protected void voltar() {
        Conquistas.marcarComoVistas();
        super.voltar();
    }

    // ------------------------------------------------------------ medidas

    /** Escala que cabe na área de conteúdo, em largura e em altura. */
    private int escala() {
        Rectangle area = areaDoConteudo();
        int porAltura = area.height / (14 + LINHAS * 36 + 26);
        int porLargura = (Engine.window.getWidth() - 20) / (COLUNAS * 112 + 8);
        return Math.max(1, Math.min(Configs.UiScale(), Math.min(porAltura, porLargura)));
    }

    private int larguraCartao(int s) {
        return 112 * s;
    }

    private int alturaCartao(int s) {
        return 30 * s;
    }

    /** O bloco inteiro — contagem, cartões e o Back — centralizado na área. */
    private Rectangle cartao(int i, int s) {
        int w = larguraCartao(s), h = alturaCartao(s), vao = 6 * s;
        int total = COLUNAS * w + (COLUNAS - 1) * vao;
        int x0 = Engine.window.getWidth() / 2 - total / 2;
        Rectangle area = areaDoConteudo();
        int alturaTotal = 14 * s + LINHAS * (h + vao) + 10 * s + com.retronova.menus.shared.Button.preferredHeight();
        int y0 = area.y + Math.max(0, (area.height - alturaTotal) / 2) + 14 * s;
        int coluna = i % COLUNAS, linha = i / COLUNAS;
        return new Rectangle(x0 + coluna * (w + vao), y0 + linha * (h + vao), w, h);
    }

    @Override
    protected void posicionar() {
        int s = escala();
        Rectangle ultimo = cartao(TODAS.length - 1, s);
        int largura = Button.preferredWidth();
        voltar.setBounds(new Rectangle(Engine.window.getWidth() / 2 - largura / 2,
                ultimo.y + ultimo.height + 10 * s, largura, Button.preferredHeight()));
    }

    // ------------------------------------------------------------ ciclo

    @Override
    protected void atualizar() {
        // O Back é a única ação da tela: o foco mora nele, e o Enter o aciona.
        if (KeyBoard.KeyPressed("Enter")) {
            voltar.activate();
            return;
        }
        voltar.setFocused(com.retronova.engine.inputs.Entrada.porFoco());
        voltar.tick();
        if (voltar.isHovered()) {
            Engine.window.pointing();
        }
    }

    @Override
    protected List<Rodape.Dica> dicas() {
        return List.of(Rodape.VOLTAR);
    }

    @Override
    protected void desenharConteudo(Graphics2D g) {
        int s = escala();
        desenharContagem(g, s);
        for (int i = 0; i < TODAS.length; i++) {
            desenharCartao(g, TODAS[i], cartao(i, s), s);
        }
        voltar.render(g);
    }

    private void desenharContagem(Graphics2D g, int s) {
        Rectangle primeiro = cartao(0, s);
        Font miuda = FontHandler.font(FontHandler.Septem, 7f * s);
        String contagem = progresso.totalDesbloqueadas() + " / " + TODAS.length + " unlocked";
        TextFx.draw(g, contagem, miuda, Engine.window.getWidth() / 2 - FontHandler.getWidth(contagem, miuda) / 2,
                primeiro.y - 6 * s, Palette.LIGHT, s);
    }

    private void desenharCartao(Graphics2D g, Conquista c, Rectangle r, int s) {
        boolean feita = c.alcancada(progresso);

        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(r.x + 2 * s, r.y + 3 * s, r.width, r.height, 8 * s, 8 * s);
        g.setColor(feita ? new Color(0x8A6A14) : new Color(0x0E0A14));
        g.fillRoundRect(r.x - s, r.y - s, r.width + 2 * s, r.height + 2 * s, 9 * s, 9 * s);
        g.setPaint(new GradientPaint(0, r.y, feita ? new Color(0x46391E) : new Color(0x2A2238),
                0, r.y + r.height, feita ? new Color(0x201A12) : new Color(0x15101F)));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8 * s, 8 * s);

        // O ícone, na sua caixa: aceso se feita, apagado se não.
        int caixa = 22 * s;
        int ix = r.x + 4 * s, iy = r.y + (r.height - caixa) / 2;
        g.setColor(feita ? new Color(0x2A200C) : new Color(0x0E0A14));
        g.fillRoundRect(ix, iy, caixa, caixa, 6 * s, 6 * s);
        int celula = Math.max(1, 2 * s);
        int lado = Icones.lado(celula);
        Icones.desenhar(g, "TROFEU", feita ? OURO : APAGADO, ix + (caixa - lado) / 2, iy + (caixa - lado) / 2, celula);

        int tx = ix + caixa + 5 * s;
        int larguraTexto = r.x + r.width - 5 * s - tx;

        Font nome = FontHandler.font(FontHandler.Game, 7f * s);
        TextFx.draw(g, c.nome(), nome, tx, r.y + 11 * s, feita ? Palette.TEXT : Palette.LIGHT, s);

        Font desc = FontHandler.font(FontHandler.Septem, 5f * s);
        TextFx.draw(g, c.descricao(), desc, tx, r.y + 18 * s, Palette.LIGHT, s);

        // A barra de quanto falta, com o número ao lado.
        int by = r.y + r.height - 7 * s;
        int bh = 3 * s;
        String numero = feita ? "DONE" : c.valorMostrado(progresso) + "/" + c.meta();
        Font fn = FontHandler.font(FontHandler.Septem, 5f * s);
        int nw = FontHandler.getWidth(numero, fn);
        int bw = larguraTexto - nw - 4 * s;
        g.setColor(Palette.OUTLINE);
        g.fillRect(tx - s, by - s, bw + 2 * s, bh + 2 * s);
        g.setColor(Palette.DEEP);
        g.fillRect(tx, by, bw, bh);
        g.setColor(feita ? OURO : new Color(0x7E849C));
        g.fillRect(tx, by, (int) (bw * c.fracao(progresso)), bh);
        TextFx.draw(g, numero, fn, tx + bw + 4 * s, by + bh + s, feita ? OURO : Palette.LIGHT, s);

        // NEW: até o jogador fechar a tela.
        if (Conquistas.ehNova(c)) {
            Font fnew = FontHandler.font(FontHandler.Game, 5f * s);
            int nwd = FontHandler.getWidth("NEW", fnew) + 4 * s;
            int nx = r.x + r.width - nwd - 3 * s, ny = r.y + 3 * s;
            g.setColor(new Color(0xE0475F));
            g.fillRoundRect(nx, ny, nwd, 8 * s, 4 * s, 4 * s);
            TextFx.draw(g, "NEW", fnew, nx + 2 * s, ny + 6 * s, Palette.TEXT, s);
        }
    }

    @Override
    public void dispose() {
    }
}
