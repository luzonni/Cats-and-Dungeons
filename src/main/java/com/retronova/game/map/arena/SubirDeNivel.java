package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Subiu de nivel: escolha um atributo para melhorar.
 *
 * Cada nivel e um ponto, e o jogador decide onde ele entra. Os cinco atributos
 * estao sempre na mesa — a sorte mora no bau; o nivel e onde se constroi o gato.
 *
 * UMA LISTA, E NAO CINCO CARTAS. Cartas lado a lado tinham largura de sobra para
 * "Luck" e falta para "Attack speed", e o numero vazava. Em linhas, cada atributo
 * tem a largura do painel inteiro, e a leitura vira a de uma ficha: icone, nome,
 * antes e depois. O "depois" e o que decide — ver o numero mudar e o premio.
 */
public class SubirDeNivel implements Activity {

    /**
     * @param exibir converte o valor cru do atributo no que o jogador le
     * @param icone  desenho 7x7 em texto: '#' cor do atributo, '+' realce
     */
    private record Atributo(String nome, Modifiers mod, double ganho,
                            Function<Player, Double> atual, Function<Double, String> exibir,
                            Color cor, String[] icone) {
    }

    private static String inteiro(double v) {
        return String.valueOf(Math.round(v));
    }

    private static String umaCasa(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** O intervalo em ticks lido como golpes por segundo: mais e melhor. */
    private static String porSegundo(double ticks) {
        return String.format(Locale.ROOT, "%.1f/s", 60.0 / Math.max(1, ticks));
    }

    private static final List<Atributo> ATRIBUTOS = List.of(
            new Atributo("Health", Modifiers.Life, 10, Player::getLifeSize,
                    SubirDeNivel::inteiro, new Color(0xE0475F), new String[]{
                    ".##.##.",
                    "#++####",
                    "#+#####",
                    "#######",
                    ".#####.",
                    "..###..",
                    "...#..."}),
            new Atributo("Damage", Modifiers.Damage, 2, Player::getDamage,
                    SubirDeNivel::inteiro, new Color(0xF5933D), new String[]{
                    ".....##",
                    "....#+#",
                    "...#+#.",
                    "#.#+#..",
                    ".###...",
                    ".##....",
                    "#..#..."}),
            new Atributo("Move speed", Modifiers.Speed, 0.2, Player::getSpeed,
                    SubirDeNivel::umaCasa, new Color(0x4FC3E8), new String[]{
                    "..##...",
                    "..#+#..",
                    "..##...",
                    "..##...",
                    ".###...",
                    "######.",
                    "#######"}),
            // Cadencia e intervalo em ticks: melhorar e DIMINUIR, como em Melhoria.
            new Atributo("Attack speed", Modifiers.AttackSpeed, -1, Player::getAttackSpeed,
                    SubirDeNivel::porSegundo, new Color(0xB57CF0), new String[]{
                    "...###.",
                    "..###..",
                    ".###...",
                    "######.",
                    "...###.",
                    "..###..",
                    ".##...."}),
            new Atributo("Luck", Modifiers.Luck, 1, Player::getLuck,
                    SubirDeNivel::inteiro, new Color(0x6BD66B), new String[]{
                    ".##.##.",
                    "#+####.",
                    "#######",
                    ".#####.",
                    "#######",
                    "#+##.##",
                    ".##..#."})
    );

    private final List<Rectangle> linhas = new ArrayList<>();
    private Rectangle painel = new Rectangle();
    private int foco = 0;
    private int tempo;
    private static final int ENTRADA = 14;

    @Override
    public void tick() {
        tempo++;
        posicionar();
        // Ja escolheu: so a animacao de confirmacao anda, e depois fecha.
        if (escolhido >= 0) {
            if (++fechando >= FECHAR) {
                // Varios niveis de uma vez: a tela reabre pelo Game.tick.
                Engine.pause(null);
            }
            return;
        }
        if (tempo < ENTRADA) {
            return;
        }
        for (int i = 0; i < linhas.size(); i++) {
            if (linhas.get(i).contains(Mouse.getX(), Mouse.getY())) {
                if (foco != i) {
                    Sound.play(Sounds.Hover);
                }
                foco = i;
                Engine.window.pointing();
                if (Mouse.clickOn(Mouse_Button.LEFT, linhas.get(i))) {
                    escolher(i);
                    return;
                }
            }
        }
        for (int i = 0; i < ATRIBUTOS.size(); i++) {
            if (KeyBoard.KeyPressed(String.valueOf(i + 1))) {
                escolher(i);
                return;
            }
        }
        if (KeyBoard.KeyPressed("Down") || KeyBoard.KeyPressed("S")) {
            foco = (foco + 1) % ATRIBUTOS.size();
            Sound.play(Sounds.Hover);
        } else if (KeyBoard.KeyPressed("Up") || KeyBoard.KeyPressed("W")) {
            foco = foco <= 0 ? ATRIBUTOS.size() - 1 : foco - 1;
            Sound.play(Sounds.Hover);
        } else if (KeyBoard.KeyPressed("Enter")) {
            escolher(foco);
        }
    }

    private void escolher(int i) {
        Player gato = Game.getPlayer();
        Atributo a = ATRIBUTOS.get(i);
        gato.getMelhorias().aplicarPonto(gato, a.mod(), a.ganho());
        if (a.mod() == Modifiers.Life) {
            gato.setLife(gato.getLife() + a.ganho());   // a vida nova ja vem cheia
        }
        gato.gastarNivel();
        Sound.play(Sounds.Conquista);
        // O efeito ja valeu; a tela fica mais um instante para MOSTRAR a escolha.
        // Fechar no mesmo quadro do clique fazia a decisao nao ter momento.
        this.escolhido = i;
        this.foco = i;
    }

    /** A linha escolhida, ou -1. */
    private int escolhido = -1;
    private int fechando;
    private static final int FECHAR = 16;

    private void posicionar() {
        int s = Configs.UiScale();
        int largura = 230 * s;
        int alturaLinha = 20 * s;
        int espaco = 3 * s;
        int topo = 42 * s;           // faixa do titulo dentro do painel
        int rodape = 14 * s;
        int altura = topo + ATRIBUTOS.size() * (alturaLinha + espaco) - espaco + rodape;
        int x = Engine.window.getWidth() / 2 - largura / 2;
        int y = Engine.window.getHeight() / 2 - altura / 2;
        painel = new Rectangle(x, y, largura, altura);
        linhas.clear();
        for (int i = 0; i < ATRIBUTOS.size(); i++) {
            linhas.add(new Rectangle(x + 8 * s, y + topo + i * (alturaLinha + espaco),
                    largura - 16 * s, alturaLinha));
        }
    }

    @Override
    public void render(Graphics2D g) {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int s = Configs.UiScale();
        if (linhas.isEmpty()) {
            posicionar();
        }
        float entrada = Math.min(1f, (tempo + Engine.alpha()) / ENTRADA);
        // Ease-out: o painel sobe rapido e assenta.
        float suave = 1 - (1 - entrada) * (1 - entrada) * (1 - entrada);

        // Saida: tudo some junto nos ultimos ticks da confirmacao.
        float saida = escolhido < 0 ? 1f
                : 1f - Math.max(0f, (fechando + Engine.alpha() - FECHAR * 0.5f) / (FECHAR * 0.5f));
        saida = Math.clamp(saida, 0f, 1f);
        java.awt.Composite compOriginal = g.getComposite();

        g.setColor(new Color(Palette.VEIL.getRed(), Palette.VEIL.getGreen(),
                Palette.VEIL.getBlue(), (int) (Palette.VEIL.getAlpha() * entrada * saida)));
        g.fillRect(0, 0, w, h);
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, saida));

        int dy = (int) ((1 - suave) * 24 * s);
        g.translate(0, dy);

        Player gato = Game.getPlayer();
        Rectangle p = painel;
        double quadro = tempo + Engine.alpha();

        // Sombra projetada do painel, macia: tres camadas que se alargam.
        for (int k = 3; k >= 1; k--) {
            g.setColor(new Color(0, 0, 0, 40));
            g.fillRect(p.x - k * s, p.y + (3 + k) * s, p.width + 2 * k * s, p.height + k * s);
        }
        // Painel: contorno, corpo em gradiente, e uma faixa de titulo mais escura.
        g.setColor(Palette.DARKEST);
        g.fillRect(p.x - 2 * s, p.y - 2 * s, p.width + 4 * s, p.height + 4 * s);
        g.setColor(Palette.OUTLINE);
        g.fillRect(p.x - s, p.y - s, p.width + 2 * s, p.height + 2 * s);
        g.setPaint(new java.awt.GradientPaint(0, p.y, Palette.DEEP.brighter(), 0, p.y + p.height, Palette.DEEP));
        g.fillRect(p.x, p.y, p.width, p.height);
        g.setPaint(new java.awt.GradientPaint(0, p.y, new Color(0x2C2440), 0, p.y + 36 * s, Palette.OUTLINE));
        g.fillRect(p.x, p.y, p.width, 36 * s);
        g.setColor(new Color(255, 214, 90, 90));           // filete dourado sob o titulo
        g.fillRect(p.x + 12 * s, p.y + 36 * s, p.width - 24 * s, s);

        estrelas(g, p, s, quadro);

        // Titulo, com um reflexo de luz cruzando as letras de tempos em tempos.
        Font titulo = FontHandler.font(FontHandler.Game, 14f * s);
        String texto = "LEVEL UP!";
        Color ouro = new Color(255, 214, 90);
        int tx0 = p.x + p.width / 2 - FontHandler.getWidth(texto, titulo) / 2;
        int ty0 = p.y + 18 * s;
        centro(g, texto, titulo, p.x + p.width / 2, ty0, ouro);
        reflexo(g, texto, titulo, tx0, ty0, quadro, s);
        Font sub = FontHandler.font(FontHandler.Septem, 6f * s);
        String resto = "Level " + gato.getLevel() + "  -  choose an attribute"
                + (gato.niveisPendentes() > 1 ? "  (" + gato.niveisPendentes() + " points)" : "");
        centro(g, resto, sub, p.x + p.width / 2, p.y + 30 * s, Palette.ACCENT);

        Font nome = FontHandler.font(FontHandler.Game, 8f * s);
        Font numero = FontHandler.font(FontHandler.Game, 7f * s);
        Font tecla = FontHandler.font(FontHandler.Septem, 6f * s);
        for (int i = 0; i < ATRIBUTOS.size(); i++) {
            Atributo a = ATRIBUTOS.get(i);
            Rectangle r = linhas.get(i);
            // Entrada em cascata: cada linha chega um pouco depois da anterior.
            float chegada = Math.clamp(entrada * 1.8f - i * 0.15f, 0f, 1f);
            float chegadaSuave = 1 - (1 - chegada) * (1 - chegada);
            int dx = (int) ((1 - chegadaSuave) * -20 * s);
            boolean sob = i == foco;
            boolean eleito = i == escolhido;
            // As outras linhas apagam enquanto a escolhida se destaca.
            if (escolhido >= 0 && !eleito) {
                float apaga = Math.min(1f, (fechando + Engine.alpha()) / 6f);
                g.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, saida * (1 - apaga * 0.8f)));
            }
            int x = r.x + dx + (sob ? 3 * s : 0);
            double pulso = 0.5 + 0.5 * Math.sin(quadro * 0.18);

            // Brilho pulsante em volta da linha em foco, na cor do atributo.
            if (sob) {
                for (int k = 3; k >= 1; k--) {
                    int a0 = (int) ((35 + 45 * pulso) / k);
                    g.setColor(new Color(a.cor().getRed(), a.cor().getGreen(), a.cor().getBlue(), a0));
                    g.fillRect(x - (k + 1) * s, r.y - (k + 1) * s, r.width + 2 * (k + 1) * s, r.height + 2 * (k + 1) * s);
                }
                // A setinha, balancando, apontando para a linha.
                int ax = r.x - 9 * s + (int) (Math.sin(quadro * 0.25) * 2 * s);
                int ay = r.y + r.height / 2;
                g.setColor(a.cor());
                g.fillPolygon(new int[]{ax, ax + 5 * s, ax}, new int[]{ay - 4 * s, ay, ay + 4 * s}, 3);
            }
            g.setColor(Palette.OUTLINE);
            g.fillRect(x - s, r.y - s, r.width + 2 * s, r.height + 2 * s);
            // Corpo em gradiente, com filete claro em cima e escuro embaixo: relevo.
            Color topo = sob ? Palette.LIGHT : Palette.MAIN.brighter();
            Color baixo = sob ? Palette.MAIN.brighter() : Palette.MAIN;
            g.setPaint(new java.awt.GradientPaint(0, r.y, topo, 0, r.y + r.height, baixo));
            g.fillRect(x, r.y, r.width, r.height);
            g.setColor(new Color(255, 255, 255, sob ? 70 : 35));
            g.fillRect(x, r.y, r.width, s);
            g.setColor(new Color(0, 0, 0, 60));
            g.fillRect(x, r.y + r.height - s, r.width, s);
            // Clarao branco na linha escolhida, no instante da confirmacao.
            if (eleito) {
                float flash = Math.max(0f, 1f - (fechando + Engine.alpha()) / 8f);
                g.setColor(new Color(255, 255, 255, (int) (200 * flash)));
                g.fillRect(x, r.y, r.width, r.height);
            }
            // Tarja da cor do atributo na esquerda; mais larga sob o foco.
            g.setColor(a.cor());
            g.fillRect(x, r.y, sob ? 4 * s : 2 * s, r.height);

            // Icone num quadrado escuro (tingido da cor do atributo sob o foco).
            int lado = r.height - 6 * s;
            int ix = x + 7 * s;
            int iy = r.y + 3 * s;
            g.setColor(Palette.OUTLINE);
            g.fillRect(ix, iy, lado, lado);
            if (sob) {
                g.setColor(new Color(a.cor().getRed(), a.cor().getGreen(), a.cor().getBlue(), 50));
                g.fillRect(ix, iy, lado, lado);
            }
            int tamIcone = 7 * 2 * s;
            desenharIcone(g, a.icone(), a.cor(), ix + (lado - tamIcone) / 2,
                    iy + (lado - tamIcone) / 2, s);

            int base = r.y + r.height / 2 + 3 * s;
            esquerda(g, a.nome(), nome, ix + lado + 6 * s, base, Palette.TEXT);

            // Antes -> depois, alinhado a direita.
            double agora = a.atual().apply(gato);
            String antes = a.exibir().apply(agora);
            String depois = a.exibir().apply(agora + a.ganho());
            int direita = x + r.width - 18 * s;
            int ld = FontHandler.getWidth(depois, numero);
            int lseta = FontHandler.getWidth(" > ", numero);
            int la = FontHandler.getWidth(antes, numero);
            esquerda(g, depois, numero, direita - ld, base, a.cor());
            esquerda(g, " > ", numero, direita - ld - lseta, base, Palette.ACCENT);
            esquerda(g, antes, numero, direita - ld - lseta - la, base,
                    sob ? Palette.TEXT : Palette.ACCENT);

            // A tecla de atalho, num quadradinho.
            String t = String.valueOf(i + 1);
            int tq = 9 * s;
            int tx = x + r.width - tq - 4 * s;
            int ty = r.y + (r.height - tq) / 2;
            g.setColor(Palette.OUTLINE);
            g.fillRect(tx, ty, tq, tq);
            centro(g, t, tecla, tx + tq / 2, ty + tq / 2 + 2 * s, sob ? a.cor() : Palette.ACCENT);
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, saida));
        }

        Font dica = FontHandler.font(FontHandler.Septem, 5f * s);
        centro(g, "Click, press 1-5, or use arrows + Enter", dica, p.x + p.width / 2,
                p.y + p.height - 5 * s, Palette.LIGHT);
        g.translate(0, -dy);
        g.setComposite(compOriginal);
    }

    /**
     * Estrelinhas de quatro pontas piscando em volta do painel. Posicoes fixas
     * (sementes por indice) e fase propria: cintilam, nao andam.
     */
    private static void estrelas(Graphics2D g, Rectangle p, int s, double quadro) {
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 14; i++) {
            int x = p.x - 14 * s + r.nextInt(p.width + 28 * s);
            int y = p.y - 12 * s + r.nextInt(30 * s);
            if (i % 2 == 1) {
                y = p.y + p.height - 10 * s + r.nextInt(22 * s);
            }
            double brilho = Math.sin(quadro * 0.08 + i * 1.7);
            if (brilho <= 0) {
                continue;
            }
            int tam = (int) (brilho * 3 * s) + s;
            g.setColor(new Color(255, 230, 150, (int) (200 * brilho)));
            g.fillRect(x - tam, y - s / 2, tam * 2, Math.max(1, s));
            g.fillRect(x - s / 2, y - tam, Math.max(1, s), tam * 2);
        }
    }

    /**
     * O reflexo que atravessa o titulo: uma faixa clara inclinada, recortada no
     * formato das letras. Passa a cada tres segundos, rapido.
     */
    private static void reflexo(Graphics2D g, String texto, Font fonte, int x, int y,
                                double quadro, int s) {
        double ciclo = (quadro % 180) / 40.0;      // 0..1 durante a passagem
        if (ciclo > 1) {
            return;
        }
        java.awt.Shape letras = fonte.createGlyphVector(g.getFontRenderContext(), texto)
                .getOutline(x, y);
        Graphics2D r = (Graphics2D) g.create();
        r.clip(letras);
        java.awt.Rectangle b = letras.getBounds();
        int faixa = 8 * s;
        int fx = (int) (b.x - faixa * 2 + ciclo * (b.width + faixa * 4));
        r.setColor(new Color(255, 255, 255, 170));
        r.fillPolygon(new int[]{fx, fx + faixa, fx + faixa - b.height, fx - b.height},
                new int[]{b.y, b.y, b.y + b.height, b.y + b.height}, 4);
        r.dispose();
    }

    /** Desenha um icone 7x7 de texto, cada celula com 2 pixels de UI. */
    private static void desenharIcone(Graphics2D g, String[] icone, Color cor, int x, int y, int s) {
        int c = 2 * s;
        Color realce = cor.brighter();
        for (int ly = 0; ly < icone.length; ly++) {
            for (int lx = 0; lx < icone[ly].length(); lx++) {
                char ch = icone[ly].charAt(lx);
                if (ch == '.') {
                    continue;
                }
                g.setColor(ch == '+' ? Color.WHITE : (ch == '#' ? cor : realce));
                g.fillRect(x + lx * c, y + ly * c, c, c);
            }
        }
    }

    private static void centro(Graphics2D g, String texto, Font fonte, int cx, int y, Color cor) {
        int lg = FontHandler.getWidth(texto, fonte);
        esquerda(g, texto, fonte, cx - lg / 2, y, cor);
    }

    private static void esquerda(Graphics2D g, String texto, Font fonte, int x, int y, Color cor) {
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(texto, x + 1, y + 1);
        g.setColor(cor);
        g.drawString(texto, x, y);
    }

    @Override
    public void dispose() {
    }
}
