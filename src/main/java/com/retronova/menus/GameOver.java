package com.retronova.menus;

import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Conquistas;
import com.retronova.game.Estatisticas;
import com.retronova.game.Game;
import com.retronova.game.Meta;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.entities.Bonus;
import com.retronova.menus.shared.Button;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fim de corrida: o resumo do que aconteceu e a loja do que fica.
 *
 * ANTES ERA SO "GAME OVER" E DOIS BOTOES. A corrida acabava sem dizer nada — nem
 * quanto tempo o gato aguentou, nem que arma carregou a build — e nada passava
 * para a proxima. Agora a tela fecha a corrida com os numeros dela (tempo,
 * abates, nivel, baus, maior horda e o dano de cada arma em barras) e abre a
 * loja de upgrades permanentes com o ouro ganho (ver Meta). E o momento em que
 * a derrota vira "so mais uma".
 */
public class GameOver extends TelaDeMenu {

    private final List<Button> botoes = new ArrayList<>();
    private final List<Rectangle> linhasDaLoja = new ArrayList<>();
    private int foco = -1;
    /** Linha da loja em foco pelas setas e pelo controle, ou -1 com o foco nos botões. */
    private int focoLoja = -1;
    private int brilhoDaCompra = -1, desdeACompra;
    /** Ticks desde que a tela abriu; anima os avisos de conquista. */
    private int desdeOFim;

    private final int gato;
    private final int espaco;

    public GameOver(int gato, int espaco) {
        // Vencer o chefe final muda o titulo, mesmo que o gato tenha caido depois.
        super(Estatisticas.venceu() ? "Victory" : "Run Over");
        corDoTitulo(Estatisticas.venceu() ? new Color(0xFFD23F) : new Color(0xE0475F));
        this.gato = gato;
        this.espaco = espaco;
        Sound.stopAll();
        Sound.play(Musics.GameOver, true);
        botoes.add(new Button(0, 0, 0, 0, "Try Again", b -> reiniciar()).primary());
        botoes.add(new Button(0, 0, 0, 0, "Main Menu", b -> voltarAoMenuPrincipal()));
    }

    /**
     * Uma corrida nova com o mesmo gato, no mesmo espaço.
     *
     * TIRA ESTA TELA DA PILHA ANTES. Empilhando o jogo por cima dela, cada nova
     * tentativa deixava um GameOver para trás, e a pilha crescia a cada derrota.
     */
    private void reiniciar() {
        Sound.stopAll();
        Engine.backActivity();
        Engine.heapActivity(new Game(gato, new Room("beginning"), espaco), () -> { });
    }

    /** Esc e B levam ao menu principal, como o botão. */
    @Override
    protected void voltar() {
        voltarAoMenuPrincipal();
    }

    private int escala() {
        int cabe = Math.max(1, areaDoConteudo().height / 172);
        return Math.max(1, Math.min(Configs.UiScale(), cabe));
    }

    private Rectangle painelResumo, painelLoja;

    @Override
    protected void posicionar() {
        int s = escala();
        int w = Engine.window.getWidth();
        Rectangle area = areaDoConteudo();
        int pw = 150 * s, ph = 140 * s, vao = 10 * s;
        int total = ph + 10 * s + Button.preferredHeight();
        int x0 = w / 2 - pw - vao / 2;
        int y0 = area.y + Math.max(0, (area.height - total) / 2);
        painelResumo = new Rectangle(x0, y0, pw, ph);
        painelLoja = new Rectangle(w / 2 + vao / 2, y0, pw, ph);
        linhasDaLoja.clear();
        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; i < todos.length; i++) {
            linhasDaLoja.add(new Rectangle(painelLoja.x + 6 * s, painelLoja.y + 34 * s + i * 20 * s,
                    painelLoja.width - 12 * s, 17 * s));
        }
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int by = y0 + ph + 10 * s;
        int bx = w / 2 - largura - 4 * s;
        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(bx + i * (largura + 8 * s), by, largura, altura));
        }
    }

    @Override
    protected void atualizar() {
        desdeACompra++;
        desdeOFim++;
        // Loja: clicar numa linha compra o proximo nivel, se houver ouro.
        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; i < linhasDaLoja.size(); i++) {
            Rectangle r = linhasDaLoja.get(i);
            if (r.contains(Mouse.getX(), Mouse.getY())) {
                Engine.window.pointing();
                if (Mouse.clickOn(Mouse_Button.LEFT, r)) {
                    comprar(i);
                }
            }
        }
        // AS SETAS ANDAM NAS DUAS PARTES DA TELA. Cima e baixo percorrem a loja,
        // e descer da ultima melhoria cai nos botoes; esquerda e direita escolhem
        // o botao. A loja era so de clique, e o controle nao chegava nela.
        if (KeyBoard.KeyPressed("Up")) {
            focoLoja = focoLoja < 0 ? todos.length - 1 : Math.max(0, focoLoja - 1);
            foco = -1;
        } else if (KeyBoard.KeyPressed("Down")) {
            if (focoLoja >= 0 && focoLoja < todos.length - 1) {
                focoLoja++;
            } else {
                focoLoja = -1;
                foco = Math.max(0, foco);
            }
        } else if (KeyBoard.KeyPressed("Right")) {
            focoLoja = -1;
            foco = (foco + 1) % botoes.size();
        } else if (KeyBoard.KeyPressed("Left")) {
            focoLoja = -1;
            foco = (foco <= 0 ? botoes.size() : foco) - 1;
        } else if (KeyBoard.KeyPressed("Enter")) {
            if (focoLoja >= 0) {
                comprar(focoLoja);
            } else if (foco >= 0) {
                botoes.get(foco).activate();
                return;
            }
        }
        for (int i = 0; i < botoes.size(); i++) {
            Button botao = botoes.get(i);
            botao.tick();
            if (botao.isHovered()) {
                foco = -1;
                focoLoja = -1;
            }
            botao.setFocused(foco == i);
        }
    }

    private void comprar(int linha) {
        if (Meta.comprar(Meta.Permanente.values()[linha])) {
            Sound.play(Sounds.Conquista);
            brilhoDaCompra = linha;
            desdeACompra = 0;
        } else {
            Sound.play(Sounds.Button);
        }
    }

    // ------------------------------------------------------------- desenho

    @Override
    protected List<Rodape.Dica> dicas() {
        return List.of(new Rodape.Dica("Enter", "A", "Select / Buy"),
                new Rodape.Dica("Esc", "B", "Main menu"));
    }

    @Override
    protected void desenharConteudo(Graphics2D g) {
        int s = escala();
        desenharResumo(g, s);
        desenharLoja(g, s);
        for (Button botao : botoes) {
            botao.render(g);
        }
    }

    /** Os avisos entram pela direita, por cima de tudo, fora da transição da tela. */
    @Override
    protected void desenharPorCima(Graphics2D g) {
        desenharConquistas(g, escala());
    }

    /**
     * Os avisos de conquista desbloqueada, no canto superior direito.
     *
     * Entram deslizando pela direita, um depois do outro, e ficam. Mostram só o
     * nome: o que ela pede e o resto está na tela de conquistas, que o jogador
     * abre pelo troféu do menu.
     */
    private void desenharConquistas(Graphics2D g, int s) {
        List<Conquistas.Conquista> novas = Estatisticas.conquistasDaCorrida();
        if (novas.isEmpty()) {
            return;
        }
        int w = 96 * s, h = 22 * s, vao = 4 * s;
        int margem = Configs.Margin();
        for (int i = 0; i < novas.size(); i++) {
            // 0 a 1: cada aviso começa 14 ticks depois do anterior e leva 18 para entrar.
            double t = Math.clamp((desdeOFim + Engine.alpha() - 10 - i * 14) / 18.0, 0, 1);
            if (t <= 0) {
                continue;
            }
            double suave = 1 - Math.pow(1 - t, 3);
            int x = (int) (Engine.window.getWidth() - margem - w + (1 - suave) * (w + margem));
            int y = margem + i * (h + vao);

            g.setColor(new Color(0, 0, 0, 100));
            g.fillRoundRect(x + 2 * s, y + 3 * s, w, h, 8 * s, 8 * s);
            g.setColor(new Color(0x8A6A14));
            g.fillRoundRect(x - s, y - s, w + 2 * s, h + 2 * s, 9 * s, 9 * s);
            g.setPaint(new GradientPaint(0, y, new Color(0x46391E), 0, y + h, new Color(0x201A12)));
            g.fillRoundRect(x, y, w, h, 8 * s, 8 * s);

            int celula = Math.max(1, (int) Math.round(1.6 * s));
            int lado = com.retronova.game.map.arena.Icones.lado(celula);
            com.retronova.game.map.arena.Icones.desenhar(g, "TROFEU", new Color(0xFFD23F),
                    x + 4 * s, y + (h - lado) / 2, celula);

            int tx = x + 4 * s + lado + 4 * s;
            TextFx.draw(g, "Achievement unlocked", FontHandler.font(FontHandler.Septem, 5f * s),
                    tx, y + 8 * s, Palette.LIGHT, s);
            TextFx.draw(g, novas.get(i).nome(), FontHandler.font(FontHandler.Game, 7f * s),
                    tx, y + 17 * s, new Color(0xFFD23F), s);
        }
    }

    private void painel(Graphics2D g, Rectangle p, String titulo, Color cor, int s) {
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(p.x + 3 * s, p.y + 5 * s, p.width, p.height, 12 * s, 12 * s);
        g.setColor(new Color(0x0E0A14));
        g.fillRoundRect(p.x - 2 * s, p.y - 2 * s, p.width + 4 * s, p.height + 4 * s, 14 * s, 14 * s);
        g.setPaint(new GradientPaint(0, p.y, new Color(0x2E2546), 0, p.y + p.height, new Color(0x15101F)));
        g.fillRoundRect(p.x, p.y, p.width, p.height, 12 * s, 12 * s);
        g.setColor(cor);
        g.fillRoundRect(p.x, p.y, p.width, 14 * s, 12 * s, 12 * s);
        g.fillRect(p.x, p.y + 7 * s, p.width, 7 * s);
        Font f = FontHandler.font(FontHandler.Septem, 7f * s);
        centro(g, titulo, f, p.x + p.width / 2, p.y + 10 * s, new Color(0x14081C));
    }

    private void desenharResumo(Graphics2D g, int s) {
        Rectangle p = painelResumo;
        painel(g, p, "RUN SUMMARY", new Color(0x4FC3E8), s);
        Font f = FontHandler.font(FontHandler.Septem, 6f * s);
        Font valor = FontHandler.font(FontHandler.Game, 7f * s);
        int seg = Estatisticas.segundos();
        String gato = Estatisticas.gato();
        String[][] linhas = {
                {"Cat", gato},
                {"Time", String.format("%02d:%02d", seg / 60, seg % 60)},
                {"Kills", String.valueOf(Estatisticas.abates())},
                {"Level", String.valueOf(Estatisticas.nivel())},
                {"Chests", String.valueOf(Estatisticas.baus())},
                {"Peak horde", String.valueOf(Estatisticas.maiorHorda())},
        };
        int y = p.y + 26 * s;
        for (String[] l : linhas) {
            esquerda(g, l[0], f, p.x + 8 * s, y, Palette.ACCENT);
            int vw = FontHandler.getWidth(l[1], valor);
            contornado(g, l[1], valor, p.x + p.width - 8 * s - vw, y, Color.WHITE, s);
            y += 10 * s;
        }
        String habilidade = Bonus.habilidade(gato);
        if (!habilidade.isEmpty()) {
            centro(g, habilidade, f, p.x + p.width / 2, y, new Color(0xB57CF0));
        }
        y += 8 * s;
        // Dano por arma, em barras proporcionais a maior.
        Map<String, Double> danos = Estatisticas.danoPorFonte();
        List<Map.Entry<String, Double>> ordenados = new ArrayList<>(danos.entrySet());
        ordenados.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        double maior = ordenados.isEmpty() ? 1 : Math.max(1, ordenados.getFirst().getValue());
        Font pequena = FontHandler.font(FontHandler.Septem, 5f * s);
        for (int i = 0; i < Math.min(4, ordenados.size()); i++) {
            var e = ordenados.get(i);
            int bw = (int) ((p.width - 16 * s) * (e.getValue() / maior));
            g.setColor(new Color(0x3A2E55));
            g.fillRect(p.x + 8 * s, y, p.width - 16 * s, 7 * s);
            g.setColor(i == 0 ? new Color(0xFFD23F) : new Color(0x7FD8FF));
            g.fillRect(p.x + 8 * s, y, bw, 7 * s);
            String rot = e.getKey() + "  " + Math.round(e.getValue());
            contornado(g, rot, pequena, p.x + 10 * s, y + 6 * s, Color.WHITE, s);
            y += 9 * s;
        }
    }

    private void desenharLoja(Graphics2D g, int s) {
        Rectangle p = painelLoja;
        painel(g, p, "UPGRADES", new Color(0xFFD23F), s);
        Font f = FontHandler.font(FontHandler.Septem, 6f * s);
        Font grande = FontHandler.font(FontHandler.Game, 8f * s);
        String ouro = "Gold: " + Meta.ouro();
        String ganho = "+" + Estatisticas.ouroGanho() + " this run";
        contornado(g, ouro, grande, p.x + 8 * s, p.y + 26 * s, new Color(0xFFD23F), s);
        int gw = FontHandler.getWidth(ganho, f);
        esquerda(g, ganho, f, p.x + p.width - 8 * s - gw, p.y + 26 * s, Palette.ACCENT);

        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; i < todos.length; i++) {
            Meta.Permanente m = todos[i];
            Rectangle r = linhasDaLoja.get(i);
            int nivel = Meta.nivel(m);
            boolean cheio = nivel >= Meta.Permanente.NIVEL_MAXIMO;
            boolean podeComprar = !cheio && Meta.ouro() >= Meta.preco(m);
            boolean sob = Entrada.porFoco() ? focoLoja == i : r.contains(Mouse.getX(), Mouse.getY());
            g.setColor(sob && podeComprar ? Palette.LIGHT : Palette.MAIN);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 6 * s, 6 * s);
            // Contorno do foco, mesmo sem ouro: o realce claro so diz "da para
            // comprar", e sem o contorno uma linha cara nao mostraria onde o foco esta.
            if (sob && Entrada.porFoco()) {
                g.setColor(Palette.ACCENT);
                g.setStroke(new BasicStroke(s));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 6 * s, 6 * s);
            }
            if (brilhoDaCompra == i && desdeACompra < 20) {
                g.setColor(new Color(255, 255, 255, (int) (200 * (1 - desdeACompra / 20.0))));
                g.fillRoundRect(r.x, r.y, r.width, r.height, 6 * s, 6 * s);
            }
            g.setColor(m.cor());
            g.fillRect(r.x, r.y, 3 * s, r.height);
            contornado(g, m.nome(), f, r.x + 6 * s, r.y + 7 * s, Color.WHITE, s);
            esquerda(g, m.efeito(), FontHandler.font(FontHandler.Septem, 5f * s), r.x + 6 * s, r.y + 14 * s, Palette.ACCENT);
            // Pips de nivel.
            for (int k = 0; k < Meta.Permanente.NIVEL_MAXIMO; k++) {
                g.setColor(k < nivel ? m.cor() : new Color(0x1A1428));
                g.fillRect(r.x + r.width - 62 * s + k * 5 * s, r.y + 4 * s, 4 * s, 4 * s);
            }
            String preco = cheio ? "MAX" : Meta.preco(m) + "g";
            int pw = FontHandler.getWidth(preco, f);
            contornado(g, preco, f, r.x + r.width - 6 * s - pw, r.y + 14 * s,
                    cheio ? Palette.LIGHT : podeComprar ? new Color(0xFFD23F) : new Color(0x9A5050), s);
        }
        centro(g, "Click or press Enter to buy. Upgrades apply to every new run.", FontHandler.font(FontHandler.Septem, 5f * s),
                p.x + p.width / 2, p.y + p.height - 4 * s, Palette.LIGHT);
    }

    // ------------------------------------------------------------- texto

    private static void esquerda(Graphics2D g, String t, Font f, int x, int y, Color cor) {
        // Texto escuro (rótulo sobre faixa colorida) fica sem contorno escuro.
        if (cor.getRed() + cor.getGreen() + cor.getBlue() < 180) {
            g.setFont(f);
            g.setColor(cor);
            g.drawString(t, x, y);
            return;
        }
        int cabe = Math.max(1, (Engine.window.getHeight() - 20) / 210);
        TextFx.draw(g, t, f, x, y, cor, Math.max(1, Math.min(Configs.UiScale(), cabe)));
    }

    private static void centro(Graphics2D g, String t, Font f, int cx, int y, Color cor) {
        esquerda(g, t, f, cx - FontHandler.getWidth(t, f) / 2, y, cor);
    }

    private static void contornado(Graphics2D g, String t, Font f, int x, int y, Color cor, int s) {
        TextFx.draw(g, t, f, x, y, cor, s);
    }

    private static void contornadoCentro(Graphics2D g, String t, Font f, int cx, int y, Color cor, int s) {
        contornado(g, t, f, cx - FontHandler.getWidth(t, f) / 2, y, cor, s);
    }

    @Override
    public void dispose() {
    }
}
