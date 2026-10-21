package com.retronova.menus;

import com.retronova.engine.Activity;
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
 * Ela PAUSA a corrida (como a roleta e as cartas) para o jogador respirar e ver
 * o que ganhou, e pergunta o que fazer: seguir na corrida sem fim, para quem
 * quer ver até onde o gato aguenta, ou encerrar agora na tela de resumo, com o
 * bônus já pago. O bônus é depositado ao vencer, e não ao escolher "terminar":
 * fechar o jogo nesta tela não perde nada.
 */
public class Vitoria implements Activity {

    private static final int ENTRADA = 40;

    private static final Color OURO = new Color(0xFFD23F);

    private final List<Button> botoes = new ArrayList<>();
    private int foco = -1;
    private int entrando;

    public Vitoria() {
        Sound.play(Sounds.Conquista);
        botoes.add(new Button(0, 0, 0, 0, "Finish Run", b -> terminar()).primary());
        botoes.add(new Button(0, 0, 0, 0, "Keep Playing", b -> Engine.pause(null)));
        posicionar();
    }

    private void terminar() {
        Engine.pause(null);
        Game.getGame().encerrarCorrida();
    }

    private int escala() {
        int cabe = Math.max(1, (Engine.window.getHeight() - 20) / 150);
        return Math.max(1, Math.min(Configs.UiScale(), cabe));
    }

    private Rectangle painel() {
        int s = escala();
        int w = 170 * s, h = 84 * s;
        return new Rectangle(Engine.window.getWidth() / 2 - w / 2,
                Engine.window.getHeight() / 2 - h / 2 - 12 * s, w, h);
    }

    private void posicionar() {
        Rectangle p = painel();
        int s = escala();
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int y = p.y + p.height + 8 * s;
        int x = Engine.window.getWidth() / 2 - largura - 4 * s;
        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(x + i * (largura + 8 * s), y, largura, altura));
        }
    }

    @Override
    public void tick() {
        entrando++;
        posicionar();
        navegarPeloTeclado();
        for (int i = 0; i < botoes.size(); i++) {
            Button botao = botoes.get(i);
            botao.tick();
            if (botao.isHovered()) {
                foco = -1;
            }
            botao.setFocused(foco == i);
        }
    }

    private void navegarPeloTeclado() {
        if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("Down")) {
            foco = (foco + 1) % botoes.size();
        } else if (KeyBoard.KeyPressed("Left") || KeyBoard.KeyPressed("Up")) {
            foco = (foco <= 0 ? botoes.size() : foco) - 1;
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            botoes.get(foco).activate();
        }
    }

    @Override
    public void render(Graphics2D g) {
        float entrada = Math.min(1f, (entrando + Engine.alpha()) / ENTRADA);
        int s = escala();
        g.setColor(new Color(Palette.VEIL.getRed(), Palette.VEIL.getGreen(), Palette.VEIL.getBlue(),
                (int) (Palette.VEIL.getAlpha() * entrada)));
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());

        Rectangle p = painel();
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(p.x + 3 * s, p.y + 5 * s, p.width, p.height, 12 * s, 12 * s);
        g.setColor(new Color(0x0E0A14));
        g.fillRoundRect(p.x - 2 * s, p.y - 2 * s, p.width + 4 * s, p.height + 4 * s, 14 * s, 14 * s);
        g.setPaint(new GradientPaint(0, p.y, new Color(0x3A2E56), 0, p.y + p.height, new Color(0x15101F)));
        g.fillRoundRect(p.x, p.y, p.width, p.height, 12 * s, 12 * s);
        g.setColor(OURO);
        g.fillRoundRect(p.x, p.y, p.width, 4 * s, 12 * s, 12 * s);

        int cx = p.x + p.width / 2;
        Font titulo = FontHandler.font(FontHandler.Game, 18f * s);
        TextFx.draw(g, "VICTORY!", titulo, cx - FontHandler.getWidth("VICTORY!", titulo) / 2,
                p.y + 28 * s, OURO, s);

        Font texto = FontHandler.font(FontHandler.Septem, 7f * s);
        String frase = "The Rat King has fallen";
        TextFx.draw(g, frase, texto, cx - FontHandler.getWidth(frase, texto) / 2, p.y + 42 * s,
                Palette.TEXT, s);

        Font valor = FontHandler.font(FontHandler.Game, 10f * s);
        String bonus = "+" + Meta.BONUS_DE_VITORIA + " gold";
        TextFx.draw(g, bonus, valor, cx - FontHandler.getWidth(bonus, valor) / 2, p.y + 60 * s, OURO, s);

        Font miuda = FontHandler.font(FontHandler.Septem, 6f * s);
        String dica = Estatisticas.venceu() ? "Already paid - closing the game keeps it" : "";
        TextFx.draw(g, dica, miuda, cx - FontHandler.getWidth(dica, miuda) / 2, p.y + 74 * s, Palette.LIGHT, s);

        for (Button botao : botoes) {
            botao.render(g);
        }
    }

    @Override
    public void dispose() {
    }
}
