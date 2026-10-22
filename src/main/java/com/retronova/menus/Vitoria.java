package com.retronova.menus;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Estatisticas;
import com.retronova.game.Game;
import com.retronova.game.Meta;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * A tela por cima do jogo quando o Rato Rei cai.
 *
 * Ela PAUSA a corrida para o jogador respirar e ver o que ganhou, e pergunta o
 * que fazer: seguir na corrida sem fim, para quem quer ver até onde o gato
 * aguenta, ou encerrar agora na tela de resumo, com o bônus já pago. O bônus é
 * depositado ao vencer, e não ao escolher "terminar": fechar o jogo nesta tela não
 * perde nada.
 *
 * Mesma moldura das outras telas de menu (ver TelaDeMenu). Esc e B valem "seguir":
 * voltar, aqui, é voltar para a partida.
 */
public class Vitoria extends TelaDeMenu {

    private static final Color OURO = new Color(0xFFD23F);

    private final List<Button> botoes = new ArrayList<>();
    private int foco = -1;
    private Rectangle painel = new Rectangle();

    public Vitoria() {
        super("Victory!");
        corDoTitulo(OURO);
        Sound.play(Sounds.Conquista);
        botoes.add(new Button(0, 0, 0, 0, "Finish Run", b -> terminar()).primary());
        botoes.add(new Button(0, 0, 0, 0, "Keep Playing", b -> voltar()));
    }

    private void terminar() {
        Engine.pause(null);
        Game.getGame().encerrarCorrida();
    }

    /** Comemoração: a música segue cheia. */
    @Override
    protected boolean abafaAMusica() {
        return false;
    }

    @Override
    protected void posicionar() {
        int s = Configs.UiScale();
        Rectangle area = areaDoConteudo();
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int pw = 170 * s, ph = 58 * s;
        int total = ph + 8 * s + altura;
        int y = area.y + Math.max(0, (area.height - total) / 2);
        painel = new Rectangle(Engine.window.getWidth() / 2 - pw / 2, y, pw, ph);
        int by = y + ph + 8 * s;
        int vao = com.retronova.menus.shared.Realce.folgaEntreVizinhos(s);
        int x = Engine.window.getWidth() / 2 - largura - vao / 2;
        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(x + i * (largura + vao), by, largura, altura));
        }
    }

    @Override
    protected void atualizar() {
        if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("Down")) {
            foco = (foco + 1) % botoes.size();
        } else if (KeyBoard.KeyPressed("Left") || KeyBoard.KeyPressed("Up")) {
            foco = (foco <= 0 ? botoes.size() : foco) - 1;
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            botoes.get(foco).activate();
            return;
        }
        for (int i = 0; i < botoes.size(); i++) {
            Button botao = botoes.get(i);
            botao.tick();
            if (botao.isHovered()) {
                foco = -1;
            }
            botao.setFocused(foco == i);
        }
    }

    @Override
    protected List<Rodape.Dica> dicas() {
        return List.of(Rodape.SELECIONAR, new Rodape.Dica("Esc", "B", "Keep playing"));
    }

    @Override
    protected void desenharConteudo(Graphics2D g) {
        int s = Configs.UiScale();
        Rectangle p = painel;
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(p.x + 3 * s, p.y + 5 * s, p.width, p.height, 12 * s, 12 * s);
        g.setColor(new Color(0x0E0A14));
        g.fillRoundRect(p.x - 2 * s, p.y - 2 * s, p.width + 4 * s, p.height + 4 * s, 14 * s, 14 * s);
        g.setPaint(new GradientPaint(0, p.y, new Color(0x3A2E56), 0, p.y + p.height, new Color(0x15101F)));
        g.fillRoundRect(p.x, p.y, p.width, p.height, 12 * s, 12 * s);
        g.setColor(OURO);
        g.fillRoundRect(p.x, p.y, p.width, 4 * s, 12 * s, 12 * s);

        int cx = p.x + p.width / 2;
        Font texto = FontHandler.font(FontHandler.Septem, 7f * s);
        String frase = "The Rat King has fallen";
        TextFx.draw(g, frase, texto, cx - FontHandler.getWidth(frase, texto) / 2, p.y + 16 * s,
                Palette.TEXT, s);

        Font valor = FontHandler.font(FontHandler.Game, 10f * s);
        String bonus = "+" + Meta.BONUS_DE_VITORIA + " gold";
        TextFx.draw(g, bonus, valor, cx - FontHandler.getWidth(bonus, valor) / 2, p.y + 34 * s, OURO, s);

        Font miuda = FontHandler.font(FontHandler.Septem, 6f * s);
        String dica = Estatisticas.venceu() ? "Already paid - closing the game keeps it" : "";
        TextFx.draw(g, dica, miuda, cx - FontHandler.getWidth(dica, miuda) / 2, p.y + 48 * s, Palette.LIGHT, s);

        for (Button botao : botoes) {
            botao.render(g);
        }
    }
}
