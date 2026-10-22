package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;
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
import com.retronova.menus.shared.MaoDeCartas;
import com.retronova.menus.shared.Realce;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
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

    public static int trocas() {
        return trocas;
    }

    public static int pulos() {
        return pulos;
    }

    /** Retomada. Negativo e "o save nao sabia": fica o valor de corrida nova. */
    public static void repor(int trocasGravadas, int pulosGravados) {
        if (trocasGravadas >= 0) {
            trocas = Math.min(TROCAS, trocasGravadas);
        }
        if (pulosGravados >= 0) {
            pulos = Math.min(PULOS, pulosGravados);
        }
    }

    /** O que uma carta faz e como ela se apresenta. */
    private record Carta(String rotulo, String icone, String nome, String efeito,
                         Color cor, Runnable aplicar) {

        /** A carta como a mão a desenha: o ícone da build no medalhão. */
        MaoDeCartas.Carta naMao() {
            return new MaoDeCartas.Carta(rotulo, cor,
                    (g, x, y, lado, c) -> Icones.desenhar(g, icone, c.darker(), x, y, lado / 9),
                    nome, efeito);
        }
    }

    private final List<Carta> mao = new ArrayList<>();
    private final MaoDeCartas cartas;
    private Rectangle botaoTrocar = new Rectangle(), botaoPular = new Rectangle();
    private final Realce.Estado realceTrocar = new Realce.Estado(), realcePular = new Realce.Estado();

    public SubirDeNivel() {
        sortearMao();
        cartas = new MaoDeCartas(mao.stream().map(Carta::naMao).toList());
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
            if (n < p.nivelMaximo() && p.disponivelPara(gato.getName())) {
                // A exclusiva se anuncia: o jogador precisa saber que aquela carta
                // e do gato dele, e nao uma passiva qualquer que todos tem.
                String rotulo = p.exclusiva()
                        ? (n == 0 ? "CAT SKILL" : "CAT SKILL LV " + (n + 1))
                        : (n == 0 ? "NEW PASSIVE" : "PASSIVE LV " + (n + 1));
                baralho.add(new Carta(rotulo, p.name(), p.nome(), p.efeitoNoNivel(n + 1), p.cor(),
                        () -> m.subirPassiva(gato, p)));
                pesos.add(p.exclusiva() ? 2.5 : 2.0);
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
        posicionar();
        int escolhida = cartas.tick();
        if (cartas.fechou()) {
            Engine.pause(null);   // varios niveis: a tela reabre pelo Game.tick
            return;
        }
        if (escolhida >= 0) {
            escolher(escolhida);
            return;
        }
        boolean ativa = cartas.pronta();
        realceTrocar.tick(ativa && trocas > 0 && Entrada.porMouse() && botaoTrocar.contains(Mouse.getX(), Mouse.getY()));
        realcePular.tick(ativa && pulos > 0 && Entrada.porMouse() && botaoPular.contains(Mouse.getX(), Mouse.getY()));
        if (!ativa) {
            return;
        }
        if (trocas > 0 && (KeyBoard.KeyPressed("Q") || Mouse.clickOn(Mouse_Button.LEFT, botaoTrocar))) {
            trocas--;
            sortearMao();
            cartas.trocar(mao.stream().map(Carta::naMao).toList());   // as cartas viram de novo
            Sound.play(Sounds.Button);
        } else if (pulos > 0 && (KeyBoard.KeyPressed("E") || Mouse.clickOn(Mouse_Button.LEFT, botaoPular))) {
            pulos--;
            Player gato = Game.getPlayer();
            gato.setLife(Math.min(gato.getLifeSize(), gato.getLife() + gato.getLifeSize() * 0.15));
            gato.gastarNivel();
            com.retronova.game.Corrida.gravar();
            Sound.play(Sounds.Button);
            Engine.pause(null);
        }
    }

    private void escolher(int i) {
        mao.get(i).aplicar().run();
        Game.getPlayer().gastarNivel();
        // Grava com a carta ja no gato: fechar o jogo aqui nao devolve a mao.
        com.retronova.game.Corrida.gravar();
    }

    // ------------------------------------------------------------- medidas

    private void posicionar() {
        int s = MaoDeCartas.escala();
        int ch = MaoDeCartas.ALTURA * s;
        int y = Engine.window.getHeight() / 2 - ch / 2 - 6 * s;
        cartas.posicionar(y, s);
        int by = y + ch + 10 * s, bw = 70 * s, bh = 14 * s;
        botaoTrocar = new Rectangle(Engine.window.getWidth() / 2 - bw - 5 * s, by, bw, bh);
        botaoPular = new Rectangle(Engine.window.getWidth() / 2 + 5 * s, by, bw, bh);
    }

    // ------------------------------------------------------------- desenho

    @Override
    public void render(Graphics2D g) {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int s = MaoDeCartas.escala();
        posicionar();
        double quadro = cartas.quadro();
        float saida = cartas.saida();
        Composite original = g.getComposite();

        // O VÉU DA PALETA, o mesmo das telas de menu sobre o jogo (Palette.VEIL).
        float veu = Math.min(1f, (float) quadro / 12f) * saida;
        g.setColor(comAlfa(Palette.VEIL, (int) (Palette.VEIL.getAlpha() * veu)));
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, saida));

        Player gato = Game.getPlayer();
        Rectangle primeira = cartas.caixa(0);
        // TÍTULO NA PALETA DO PROJETO. Era um dourado alaranjado com halo da mesma
        // cor — a única tela da interface fora da paleta fria, e a mais vista da
        // corrida. Agora é o branco do título (Palette.TEXT) com o halo na luz da
        // lua (Palette.ACCENT), pulsando: comemora sem sair da família. A cor
        // quente fica com as próprias cartas, onde ela diz algo (raridade, tipo).
        Font titulo = FontHandler.font(FontHandler.Game, 16f * s);
        String texto = "LEVEL " + gato.getLevel() + "!";
        int tw = FontHandler.getWidth(texto, titulo);
        int ty = primeira.y - 20 * s;
        double pulso = 0.5 + 0.5 * Math.sin(quadro * 0.12);
        g.setFont(titulo);
        g.setColor(comAlfa(Palette.ACCENT, (int) (50 + 40 * pulso)));
        for (int d = -2; d <= 2; d += 2) {
            g.drawString(texto, w / 2 - tw / 2 + d * s / 2, ty + d * s / 2);
        }
        contornado(g, texto, titulo, w / 2 - tw / 2, ty, Palette.TEXT, s);
        Font sub = FontHandler.font(FontHandler.Septem, 6f * s);
        String resto = gato.niveisPendentes() > 1
                ? "Choose one  (" + gato.niveisPendentes() + " level-ups waiting)" : "Choose one";
        centro(g, resto, sub, w / 2, ty + 11 * s, Palette.ACCENT);

        cartas.render(g, s);
        desenharBotao(g, botaoTrocar, realceTrocar, "[Q] Reroll (" + trocas + ")", trocas > 0, s);
        desenharBotao(g, botaoPular, realcePular, "[E] Skip (" + pulos + ")", pulos > 0, s);
        desenharBuild(g, gato, s, botaoTrocar.y + botaoTrocar.height + 12 * s, w);
        g.setComposite(original);
    }

    /** Trocar e Pular: o realce comum (ver Realce), e o tom claro com o mouse em cima. */
    private void desenharBotao(Graphics2D g, Rectangle caixa, Realce.Estado realce, String texto,
                               boolean ativo, int s) {
        float h = realce.noQuadro();
        Rectangle r = Realce.realcar(g, caixa, h, s);
        g.setColor(new Color(0x0E0A14));
        g.fillRoundRect(r.x - s, r.y - s, r.width + 2 * s, r.height + 2 * s, 8 * s, 8 * s);
        g.setColor(!ativo ? new Color(0x2A2438) : h > 0.5f ? Palette.LIGHT : Palette.MAIN);
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
            g.setColor(Palette.OUTLINE);
            g.fillRoundRect(x, y, lado + 6 * s, lado + 6 * s, 6 * s, 6 * s);
            Icones.desenhar(g, (String) it[0], (Color) it[1], x + 3 * s, y + 3 * s, cel);
            String nivel = (String) it[2];
            // A arma evoluída leva a estrela no tom de destaque, e não em dourado.
            contornado(g, nivel, f, x + lado + 2 * s, y + lado + 6 * s,
                    "*".equals(nivel) ? Palette.ACCENT : Palette.TEXT, s);
            x += passo;
        }
    }

    // ------------------------------------------------------------- texto

    private static Color comAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alfa)));
    }

    private static void centro(Graphics2D g, String texto, Font fonte, int cx, int y, Color cor) {
        centro(g, texto, fonte, cx, y, cor, true, Configs.UiScale());
    }

    private static void centro(Graphics2D g, String texto, Font fonte, int cx, int y, Color cor,
                               boolean comContorno, int s) {
        int x = cx - FontHandler.getWidth(texto, fonte) / 2;
        if (comContorno && escuro(cor) == false) {
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
        TextFx.draw(g, texto, fonte, x, y, cor, s);
    }

    /** Texto escuro (o rotulo sobre a faixa colorida) nao leva contorno escuro. */
    private static boolean escuro(Color c) {
        return c.getRed() + c.getGreen() + c.getBlue() < 180;
    }

    @Override
    public void dispose() {
    }
}
