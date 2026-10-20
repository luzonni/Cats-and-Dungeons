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
import com.retronova.game.items.ArmaExtra;
import com.retronova.game.items.Melhorias;
import com.retronova.game.items.Passiva;
import com.retronova.game.items.Raridade;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Subiu de nivel: tres cartas sorteadas, uma leva.
 *
 * A VERSAO ANTERIOR MOSTRAVA SEMPRE OS MESMOS CINCO ATRIBUTOS. Bonita, mas sem
 * decisao: depois de tres niveis o jogador ja sabia o que ia escolher, e subir de
 * nivel virava um clique. Agora a mao e SORTEADA de um baralho maior — os
 * atributos, sete passivas e o proximo nivel das armas extras que o gato ja tem
 * — e cada carta de atributo tem raridade (quanto vale). A pergunta passa a ser
 * "o que esta corrida precisa AGORA?", e duas corridas nunca sobem igual.
 *
 * Duas valvulas, contadas por corrida: TROCAR (sorteia outra mao) e PULAR (cura
 * um pouco em vez de escolher). Sao poucas de proposito — uma decisao a mais, nao
 * uma saida para nunca decidir.
 *
 * A faixa da BUILD no rodape mostra armas e passivas com os niveis: escolher
 * sabendo o que ja se tem, e ver as evolucoes chegando (ver Evolucao).
 */
public class SubirDeNivel implements Activity {

    /** Trocas e pulos que ainda restam nesta corrida. */
    private static int trocas, pulos;
    private static final int TROCAS = 2, PULOS = 1;

    /** Partida nova: valvulas cheias. Chamado por Estatisticas.reiniciar. */
    public static void reiniciar() {
        trocas = TROCAS;
        pulos = PULOS;
    }

    /** O que uma carta faz e como ela se apresenta. */
    private record Carta(String rotulo, String icone, String nome, String efeito,
                         Color cor, Runnable aplicar) {
    }

    private final List<Carta> mao = new ArrayList<>();
    private final List<Rectangle> caixas = new ArrayList<>();
    private Rectangle botaoTrocar = new Rectangle(), botaoPular = new Rectangle();
    private int foco = -1;
    private int tempo;
    private int escolhido = -1;
    private int fechando;
    private static final int ENTRADA = 26;
    private static final int FECHAR = 18;

    public SubirDeNivel() {
        sortearMao();
    }

    // ------------------------------------------------------------- baralho

    private static final double[] PELA_RARIDADE = {1.0, 1.5, 2.2, 3.2};

    private void sortearMao() {
        Player gato = Game.getPlayer();
        Melhorias m = gato.getMelhorias();
        double promocao = gato.hasModifier(Modifiers.Fortune) ? gato.valueModifier(Modifiers.Fortune) : 0;
        List<Carta> baralho = new ArrayList<>();
        List<Double> pesos = new ArrayList<>();

        // Armas extras que o gato ja tem e ainda sobem: as mais desejadas.
        for (ArmaExtra a : ArmaExtra.values()) {
            int n = m.nivel(a);
            if (n > 0 && n < ArmaExtra.NIVEL_MAXIMO) {
                baralho.add(new Carta("WEAPON LV " + (n + 1), a.name(), a.nome(), a.efeito(n + 1), a.cor(),
                        () -> m.subirArma(a)));
                pesos.add(3.0);
            }
        }
        // Passivas: metade de cada evolucao.
        for (Passiva p : Passiva.values()) {
            int n = m.nivel(p);
            if (n < Passiva.NIVEL_MAXIMO) {
                String rotulo = n == 0 ? "NEW PASSIVE" : "PASSIVE LV " + (n + 1);
                baralho.add(new Carta(rotulo, p.name(), p.nome(), p.efeitoDeUmNivel(), p.cor(),
                        () -> m.subirPassiva(gato, p)));
                pesos.add(2.0);
            }
        }
        // Atributos, com raridade: a mesma carta vale mais quando sai rara.
        Object[][] atributos = {
                {"Health", "VIGOR", Modifiers.Life, 10.0, "+%.0f max health", new Color(0xE0475F)},
                {"Damage", "FURIA", Modifiers.Damage, 2.0, "+%.0f damage", new Color(0xF5933D)},
                {"Move speed", "PATAS", Modifiers.Speed, 0.2, "+%.1f move speed", new Color(0x4FC3E8)},
                {"Attack speed", "FRENESI", Modifiers.AttackSpeed, -1.0, "%.0f ticks between attacks", new Color(0xB57CF0)},
                {"Luck", "INSTINTO", Modifiers.Luck, 1.0, "+%.0f luck", new Color(0x6BD66B)},
        };
        for (Object[] at : atributos) {
            Raridade r = Raridade.sortear(promocao);
            double valor = (double) at[3] * PELA_RARIDADE[r.ordinal()];
            if (at[2] == Modifiers.Life || at[2] == Modifiers.Damage || at[2] == Modifiers.Luck
                    || at[2] == Modifiers.AttackSpeed) {
                valor = Math.round(valor);
            }
            double v = valor;
            Modifiers mod = (Modifiers) at[2];
            baralho.add(new Carta(r.rotulo().toUpperCase(Locale.ROOT), (String) at[1], (String) at[0],
                    String.format(Locale.ROOT, (String) at[4], v), r == Raridade.COMUM ? (Color) at[5] : r.cor(),
                    () -> {
                        m.aplicarPonto(gato, mod, v);
                        if (mod == Modifiers.Life) {
                            gato.setLife(gato.getLife() + v);   // a vida nova ja vem cheia
                        }
                    }));
            pesos.add(1.5);
        }

        mao.clear();
        for (int i = 0; i < 3 && !baralho.isEmpty(); i++) {
            double total = pesos.stream().mapToDouble(Double::doubleValue).sum();
            double sorteio = Engine.RAND.nextDouble() * total;
            int k = 0;
            while (k < pesos.size() - 1 && (sorteio -= pesos.get(k)) > 0) {
                k++;
            }
            mao.add(baralho.remove(k));
            pesos.remove(k);
        }
    }

    // --------------------------------------------------------------- tick

    @Override
    public void tick() {
        tempo++;
        posicionar();
        if (escolhido >= 0) {
            if (++fechando >= FECHAR) {
                Engine.pause(null);   // varios niveis: a tela reabre pelo Game.tick
            }
            return;
        }
        if (tempo < ENTRADA) {
            return;
        }
        for (int i = 0; i < caixas.size(); i++) {
            if (caixas.get(i).contains(Mouse.getX(), Mouse.getY())) {
                if (foco != i) {
                    Sound.play(Sounds.Hover);
                }
                foco = i;
                Engine.window.pointing();
                if (Mouse.clickOn(Mouse_Button.LEFT, caixas.get(i))) {
                    escolher(i);
                    return;
                }
            }
        }
        for (int i = 0; i < mao.size(); i++) {
            if (KeyBoard.KeyPressed(String.valueOf(i + 1))) {
                escolher(i);
                return;
            }
        }
        if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("D")) {
            foco = foco < 0 ? 0 : (foco + 1) % mao.size();
            Sound.play(Sounds.Hover);
        } else if (KeyBoard.KeyPressed("Left") || KeyBoard.KeyPressed("A")) {
            foco = foco <= 0 ? mao.size() - 1 : foco - 1;
            Sound.play(Sounds.Hover);
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            escolher(foco);
        }
        if (trocas > 0 && (KeyBoard.KeyPressed("Q") || Mouse.clickOn(Mouse_Button.LEFT, botaoTrocar))) {
            trocas--;
            sortearMao();
            tempo = ENTRADA / 2;     // as cartas viram de novo
            Sound.play(Sounds.Button);
        } else if (pulos > 0 && (KeyBoard.KeyPressed("E") || Mouse.clickOn(Mouse_Button.LEFT, botaoPular))) {
            pulos--;
            Player gato = Game.getPlayer();
            gato.setLife(Math.min(gato.getLifeSize(), gato.getLife() + gato.getLifeSize() * 0.15));
            gato.gastarNivel();
            Sound.play(Sounds.Button);
            Engine.pause(null);
        }
    }

    private void escolher(int i) {
        mao.get(i).aplicar().run();
        Game.getPlayer().gastarNivel();
        Sound.play(Sounds.Conquista);
        escolhido = i;
        foco = i;
    }

    // ------------------------------------------------------------- medidas

    /** Escala da tela: a da interface, reduzida se nao couber na janela. */
    private int escala() {
        int cabe = Math.max(1, (Engine.window.getHeight() - 20) / 225);
        return Math.max(1, Math.min(Configs.UiScale(), cabe));
    }

    private void posicionar() {
        int s = escala();
        int cw = 88 * s, ch = 128 * s, vao = 10 * s;
        int total = mao.size() * cw + (mao.size() - 1) * vao;
        int x = Engine.window.getWidth() / 2 - total / 2;
        int y = Engine.window.getHeight() / 2 - ch / 2 - 6 * s;
        caixas.clear();
        for (int i = 0; i < mao.size(); i++) {
            caixas.add(new Rectangle(x + i * (cw + vao), y, cw, ch));
        }
        int by = y + ch + 10 * s, bw = 70 * s, bh = 14 * s;
        botaoTrocar = new Rectangle(Engine.window.getWidth() / 2 - bw - 5 * s, by, bw, bh);
        botaoPular = new Rectangle(Engine.window.getWidth() / 2 + 5 * s, by, bw, bh);
    }

    // ------------------------------------------------------------- desenho

    @Override
    public void render(Graphics2D g) {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int s = escala();
        if (caixas.isEmpty()) {
            posicionar();
        }
        float a = Engine.alpha();
        double quadro = tempo + a;
        float saida = escolhido < 0 ? 1f
                : Math.clamp(1f - (fechando + a - FECHAR * 0.5f) / (FECHAR * 0.5f), 0f, 1f);
        Composite original = g.getComposite();

        float veu = Math.min(1f, (float) quadro / 12f) * saida;
        g.setColor(new Color(10, 6, 18, (int) (215 * veu)));
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));

        Player gato = Game.getPlayer();
        Rectangle primeira = caixas.getFirst();
        // Titulo com halo dourado pulsando.
        Font titulo = FontHandler.font(FontHandler.Game, 16f * s);
        String texto = "LEVEL " + gato.getLevel() + "!";
        int tw = FontHandler.getWidth(texto, titulo);
        int ty = primeira.y - 20 * s;
        double pulso = 0.5 + 0.5 * Math.sin(quadro * 0.12);
        g.setFont(titulo);
        g.setColor(new Color(255, 200, 60, (int) (50 + 40 * pulso)));
        for (int d = -2; d <= 2; d += 2) {
            g.drawString(texto, w / 2 - tw / 2 + d * s / 2, ty + d * s / 2);
        }
        contornado(g, texto, titulo, w / 2 - tw / 2, ty, new Color(255, 214, 90), s);
        Font sub = FontHandler.font(FontHandler.Septem, 6f * s);
        String resto = gato.niveisPendentes() > 1
                ? "Choose one  (" + gato.niveisPendentes() + " level-ups waiting)" : "Choose one";
        centro(g, resto, sub, w / 2, ty + 11 * s, Palette.ACCENT);

        for (int i = 0; i < mao.size(); i++) {
            desenharCarta(g, i, s, quadro, saida);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));
        desenharBotao(g, botaoTrocar, "[Q] Reroll (" + trocas + ")", trocas > 0, s);
        desenharBotao(g, botaoPular, "[E] Skip (" + pulos + ")", pulos > 0, s);
        desenharBuild(g, gato, s, botaoTrocar.y + botaoTrocar.height + 12 * s, w);
        g.setComposite(original);
    }

    private void desenharCarta(Graphics2D g, int i, int s, double quadro, float saida) {
        Carta c = mao.get(i);
        Rectangle r = caixas.get(i);
        // Entrada: cada carta vira (escala horizontal de 0 a 1) um pouco depois da anterior.
        double t = Math.clamp((quadro - i * 5) / 12.0, 0, 1);
        double c1 = 1.5, c3 = c1 + 1;
        double vira = t <= 0 ? 0 : 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        if (vira <= 0.02) {
            return;
        }
        boolean sob = i == foco;
        boolean eleita = i == escolhido;
        if (escolhido >= 0 && !eleita) {
            float apaga = (float) Math.min(1, (fechando + Engine.alpha()) / 6.0);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida * (1 - apaga * 0.85f)));
        }
        double sobe = sob ? 5 * s : 0;
        double cresce = eleita ? 1 + 0.08 * Math.sin(Math.min(1, fechando / 8.0) * Math.PI) : 1;
        java.awt.geom.AffineTransform antes = g.getTransform();
        g.translate(r.getCenterX(), r.getCenterY() - sobe);
        g.scale(vira * cresce, cresce);
        g.translate(-r.getCenterX(), -r.getCenterY());

        int x = r.x, y = r.y, cw = r.width, ch = r.height;
        Color cor = c.cor();
        if (sob || eleita) {
            double brilho = 0.5 + 0.5 * Math.sin(quadro * 0.2);
            for (int k = 3; k >= 1; k--) {
                g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), (int) ((45 + 45 * brilho) / k)));
                g.fillRoundRect(x - 3 * k * s, y - 3 * k * s, cw + 6 * k * s, ch + 6 * k * s, 14 * s, 14 * s);
            }
        }
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(x + 2 * s, y + 4 * s, cw, ch, 10 * s, 10 * s);
        g.setColor(new Color(0x0E0A14));
        g.fillRoundRect(x - 2 * s, y - 2 * s, cw + 4 * s, ch + 4 * s, 12 * s, 12 * s);
        g.setPaint(new GradientPaint(0, y, new Color(0x2E2546), 0, y + ch, new Color(0x15101F)));
        g.fillRoundRect(x, y, cw, ch, 10 * s, 10 * s);
        // Faixa do rotulo (raridade, ou PASSIVE / WEAPON).
        g.setColor(cor);
        g.fillRoundRect(x, y, cw, 13 * s, 10 * s, 10 * s);
        g.fillRect(x, y + 7 * s, cw, 6 * s);
        Font rotulo = FontHandler.font(FontHandler.Septem, 6f * s);
        centro(g, c.rotulo(), rotulo, x + cw / 2, y + 10 * s, new Color(0x14081C));

        // Icone grande num medalhao claro.
        int cel = 4 * s;
        int lado = Icones.lado(cel);
        int ix = x + (cw - lado) / 2, iy = y + 22 * s;
        g.setColor(new Color(0xF4EBDD));
        g.fillRoundRect(ix - 5 * s, iy - 5 * s, lado + 10 * s, lado + 10 * s, 12 * s, 12 * s);
        g.setColor(cor);
        g.drawRoundRect(ix - 5 * s, iy - 5 * s, lado + 10 * s, lado + 10 * s, 12 * s, 12 * s);
        Icones.desenhar(g, c.icone(), cor.darker(), ix, iy, cel);

        Font nome = FontHandler.font(FontHandler.Game, 9f * s);
        int ny = iy + lado + 18 * s;
        for (String linha : quebrar(c.nome(), nome, cw - 8 * s)) {
            centro(g, linha, nome, x + cw / 2, ny, Color.WHITE, true, s);
            ny += 11 * s;
        }
        Font efeito = FontHandler.font(FontHandler.Septem, 6f * s);
        ny += 2 * s;
        Color corEfeito = new Color(Math.min(255, cor.getRed() + 80), Math.min(255, cor.getGreen() + 80),
                Math.min(255, cor.getBlue() + 80));
        for (String linha : quebrar(c.efeito(), efeito, cw - 10 * s)) {
            centro(g, linha, efeito, x + cw / 2, ny, corEfeito, true, s);
            ny += 8 * s;
        }
        // Tecla de atalho no pe da carta.
        Font tecla = FontHandler.font(FontHandler.Septem, 6f * s);
        int tq = 10 * s;
        g.setColor(new Color(0x0E0A14));
        g.fillRoundRect(x + cw / 2 - tq / 2, y + ch - tq - 4 * s, tq, tq, 4 * s, 4 * s);
        centro(g, String.valueOf(i + 1), tecla, x + cw / 2, y + ch - 6 * s, sob ? cor : Palette.ACCENT);
        if (eleita && fechando < 8) {
            g.setColor(new Color(255, 255, 255, (int) (200 * (1 - fechando / 8.0))));
            g.fillRoundRect(x, y, cw, ch, 10 * s, 10 * s);
        }
        g.setTransform(antes);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));
    }

    private void desenharBotao(Graphics2D g, Rectangle r, String texto, boolean ativo, int s) {
        boolean sob = ativo && r.contains(Mouse.getX(), Mouse.getY());
        g.setColor(new Color(0x0E0A14));
        g.fillRoundRect(r.x - s, r.y - s, r.width + 2 * s, r.height + 2 * s, 8 * s, 8 * s);
        g.setColor(!ativo ? new Color(0x2A2438) : sob ? Palette.LIGHT : Palette.MAIN);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8 * s, 8 * s);
        Font f = FontHandler.font(FontHandler.Septem, 6f * s);
        centro(g, texto, f, r.x + r.width / 2, r.y + r.height / 2 + 3 * s, ativo ? Color.WHITE : Palette.LIGHT);
    }

    /** A build atual: armas e passivas, com nivel; evoluidas com uma estrela. */
    private void desenharBuild(Graphics2D g, Player gato, int s, int y, int w) {
        Melhorias m = gato.getMelhorias();
        List<Object[]> itens = new ArrayList<>();
        for (var e : m.armas().entrySet()) {
            String sufixo = m.evoluiu(e.getKey()) ? "*" : String.valueOf(e.getValue());
            itens.add(new Object[]{e.getKey().name(), e.getKey().cor(), sufixo});
        }
        for (var e : m.passivas().entrySet()) {
            itens.add(new Object[]{e.getKey().name(), e.getKey().cor(), String.valueOf(e.getValue())});
        }
        if (itens.isEmpty()) {
            return;
        }
        int cel = Math.max(1, 2 * s);
        int lado = Icones.lado(cel);
        int passo = lado + 12 * s;
        int x = w / 2 - (itens.size() * passo) / 2;
        Font f = FontHandler.font(FontHandler.Septem, 6f * s);
        Font rotulo = FontHandler.font(FontHandler.Septem, 5f * s);
        centro(g, "YOUR BUILD", rotulo, w / 2, y - 3 * s, Palette.LIGHT);
        for (Object[] it : itens) {
            g.setColor(new Color(0x1A1428));
            g.fillRoundRect(x, y, lado + 6 * s, lado + 6 * s, 6 * s, 6 * s);
            Icones.desenhar(g, (String) it[0], (Color) it[1], x + 3 * s, y + 3 * s, cel);
            String nivel = (String) it[2];
            contornado(g, nivel, f, x + lado + 2 * s, y + lado + 6 * s,
                    "*".equals(nivel) ? new Color(255, 214, 90) : Color.WHITE, s);
            x += passo;
        }
    }

    // ------------------------------------------------------------- texto

    private static List<String> quebrar(String texto, Font fonte, int largura) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (FontHandler.getWidth(tentativa, fonte) > largura && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(tentativa);
            }
        }
        if (!atual.isEmpty()) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    private static void centro(Graphics2D g, String texto, Font fonte, int cx, int y, Color cor) {
        centro(g, texto, fonte, cx, y, cor, false, 1);
    }

    private static void centro(Graphics2D g, String texto, Font fonte, int cx, int y, Color cor,
                               boolean comContorno, int s) {
        int x = cx - FontHandler.getWidth(texto, fonte) / 2;
        if (comContorno) {
            contornado(g, texto, fonte, x, y, cor, s);
            return;
        }
        g.setFont(fonte);
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(texto, x + 1, y + 1);
        g.setColor(cor);
        g.drawString(texto, x, y);
    }

    private static void contornado(Graphics2D g, String texto, Font fonte, int x, int y, Color cor, int s) {
        g.setFont(fonte);
        int c = Math.max(1, s / 2 + 1);
        g.setColor(new Color(0x0A0610));
        for (int[] d : new int[][]{{-c, 0}, {c, 0}, {0, -c}, {0, c}, {c, c}}) {
            g.drawString(texto, x + d[0], y + d[1]);
        }
        g.setColor(cor);
        g.drawString(texto, x, y);
    }

    @Override
    public void dispose() {
    }
}
