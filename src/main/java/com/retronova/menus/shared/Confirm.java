package com.retronova.menus.shared;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Diálogo de confirmação para ações destrutivas.
 *
 * Existe como componente compartilhado porque menu e pausa divergiam: sair pela
 * pausa perguntava, sair pelo menu inicial fechava o jogo direto. Com uma peça
 * só, as duas telas não voltam a se separar.
 */
public class Confirm {

    private String mensagem;
    private Runnable aoConfirmar;
    private Button sim;
    private Button nao;

    /** Abre o diálogo. Enquanto estiver aberto, a tela de trás não recebe cliques. */
    public void perguntar(String mensagem, Runnable aoConfirmar) {
        this.mensagem = mensagem;
        this.aoConfirmar = aoConfirmar;
        this.sim = new Button(0, 0, 0, 0, "Yes", b -> {
            Runnable acao = this.aoConfirmar;
            fechar();
            acao.run();
        }).primary();
        this.nao = new Button(0, 0, 0, 0, "No", b -> fechar());
    }

    public boolean aberto() {
        return mensagem != null;
    }

    public void fechar() {
        this.mensagem = null;
        this.aoConfirmar = null;
        this.sim = null;
        this.nao = null;
    }

    /**
     * @return true se consumiu a interação; quem chama deve parar o próprio tick
     *         para os botões de trás não responderem através do diálogo.
     */
    public boolean tick() {
        if (!aberto()) {
            return false;
        }
        posicionar();
        if (KeyBoard.KeyPressed("Escape")) {
            fechar();
            return true;
        }
        sim.tick();
        if (aberto()) {          // o "Yes" pode ter fechado o diálogo
            nao.tick();
        }
        if (aberto() && (sim.isHovered() || nao.isHovered())) {
            Engine.window.pointing();
        }
        return true;
    }

    private void posicionar() {
        int s = Configs.UiScale();
        int larguraBotao = 44 * s;
        int alturaBotao = Button.preferredHeight();

        Rectangle painel = painel();
        int y = painel.y + painel.height - alturaBotao - 6 * s;
        int folga = 6 * s;
        int centro = painel.x + painel.width / 2;

        sim.setBounds(new Rectangle(centro - larguraBotao - folga / 2, y, larguraBotao, alturaBotao));
        nao.setBounds(new Rectangle(centro + folga / 2, y, larguraBotao, alturaBotao));
    }

    private Rectangle painel() {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 8f * s);
        int largura = Math.max(130 * s, FontHandler.getWidth(mensagem, fonte) + 24 * s);
        //30*s cobre: folga superior, a linha da mensagem, o respiro até os botões
        //e a folga inferior. Com 20*s os botões subiam por cima do texto.
        int altura = 30 * s + Button.preferredHeight();
        return new Rectangle(
                Engine.window.getWidth() / 2 - largura / 2,
                Engine.window.getHeight() / 2 - altura / 2,
                largura, altura);
    }

    public void render(Graphics2D g2) {
        if (!aberto()) {
            return;
        }
        int s = Configs.UiScale();
        Graphics2D g = (Graphics2D) g2.create();

        // Escurece o que está atrás para deixar claro que só o diálogo responde.
        g.setColor(Palette.VEIL);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());

        Rectangle p = painel();
        g.setColor(Palette.OUTLINE);
        g.fillRect(p.x, p.y, p.width, p.height);
        g.setColor(Palette.DEEP);
        g.fillRect(p.x + s, p.y + s, p.width - 2 * s, p.height - 2 * s);
        g.setColor(Palette.MAIN);
        g.fillRect(p.x + s, p.y + s, p.width - 2 * s, s);

        Font fonte = FontHandler.font(FontHandler.Game, 8f * s);
        int largura = FontHandler.getWidth(mensagem, fonte);
        int altura = FontHandler.getHeight(mensagem, fonte);
        int x = p.x + (p.width - largura) / 2;
        int y = p.y + 8 * s + altura;
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(mensagem, x + s, y + s);
        g.setColor(Palette.TEXT);
        g.drawString(mensagem, x, y);
        g.dispose();

        sim.render(g2);
        nao.render(g2);
    }
}
