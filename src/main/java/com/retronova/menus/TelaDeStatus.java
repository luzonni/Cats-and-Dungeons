package com.retronova.menus;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.items.ArmaExtra;
import com.retronova.game.items.Elemento;
import com.retronova.game.items.Evolucao;
import com.retronova.game.items.Item;
import com.retronova.game.items.Melhoria;
import com.retronova.game.items.Melhorias;
import com.retronova.game.items.Passiva;
import com.retronova.game.map.arena.SubirDeNivel;
import com.retronova.game.objects.entities.Bonus;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;
import com.retronova.menus.shared.Realce;
import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A tela de Status do gato, na tecla E (Y no controle): tudo o que se quer
 * consultar no meio da corrida, numa olhada.
 *
 * ---------------------------------------------------------------------------
 * POR QUE ELA SUBSTITUI O INVENTÁRIO
 *
 * O E abria um inventário de grade, mas o jogo não tem inventário como mecânica:
 * o gato começa com a arma dele e cresce por cartas. A grade ficava vazia, e o
 * que o jogador queria saber — "quanto de dano eu tenho agora? quantos níveis de
 * crítico eu peguei?" — não estava em lugar nenhum. O inventário continua no
 * código, só sem porta de entrada (ver Game).
 *
 * ---------------------------------------------------------------------------
 * TRÊS COLUNAS, no molde da tela de pausa do Vampire Survivors e do Brotato, que
 * mostram atributos de um lado e o que foi pego do outro:
 *
 *   Cat      atributos finais, com o bônus entre parênteses, e os números da
 *            corrida (nível, abates, ouro, trocas e pulos de carta).
 *   Weapons  a arma da mão com o elemento, e as armas extras com o nível.
 *   Buffs    passivas com nível e efeito acumulado, cartas de atributo e
 *            melhorias da roleta, cada uma com quantas vezes veio e o total.
 *
 * OS VALORES SÃO OS DO GAMEPLAY, lidos de onde o jogo os lê (Entity.getDamage,
 * Bonus.critico, Melhorias...), e nunca recalculados aqui: uma tela de status que
 * faz a própria conta mente no primeiro balanceamento que esquecer de atualizá-la.
 *
 * ---------------------------------------------------------------------------
 * ELA PAUSA O JOGO. O Vampire Survivors, o Brotato e o Hades (no Codex e na tela
 * de bênçãos) mostram build e atributos com a ação parada; a horda aqui cresce a
 * cada segundo e ler três colunas com o gato apanhando seria punir quem quer
 * entender a própria build. Um overlay sem pausa só faria sentido para uma linha
 * de HUD, não para uma ficha.
 *
 * É uma TelaDeMenu: mesma moldura, véu, rodapé e transição das outras. E, Esc e B
 * fecham. As linhas recebem foco pelas setas e pelo D-pad, com o realce comum, e a
 * linha focada (ou sob o mouse) mostra a explicação dela na faixa de detalhe, no pé do painel.
 */
public class TelaDeStatus extends TelaDeMenu {

    /** Uma linha: rótulo à esquerda, valor à direita, e opcionalmente uma linha de efeito embaixo. */
    private record Linha(String rotulo, String valor, Color cor, String efeito, String ajuda) {
        boolean dupla() {
            return efeito != null && !efeito.isEmpty();
        }
    }

    /** Um bloco de linhas com título, dentro de uma coluna. */
    private record Bloco(String titulo, List<Linha> linhas) {
    }

    private final List<List<Bloco>> colunas = new ArrayList<>();
    /** As caixas de todas as linhas visíveis, por coluna, para foco e mouse. */
    private final List<List<Rectangle>> caixas = new ArrayList<>();
    private final List<List<Linha>> visiveis = new ArrayList<>();
    private Rectangle painel = new Rectangle();
    private int coluna, linha = -1;

    public TelaDeStatus() {
        super("Status");
        montar();
    }

    // --------------------------------------------------------------- conteúdo

    private void montar() {
        Player gato = Game.getPlayer();
        colunas.clear();
        colunas.add(List.of(new Bloco("CAT", atributos(gato))));
        colunas.add(List.of(new Bloco("WEAPONS", armas(gato)), new Bloco("RUN", corrida(gato))));
        colunas.add(List.of(new Bloco("BUFFS", buffs(gato))));
    }

    private static List<Linha> atributos(Player gato) {
        List<Linha> l = new ArrayList<>();
        l.add(new Linha("Health", (int) Math.ceil(gato.getLife()) + "/" + (int) gato.getLifeSize()
                + bonus(gato, Modifiers.Life, 0), Palette.TEXT, null,
                "Current and maximum health. Bonus from cards and upgrades in parentheses."));
        l.add(new Linha("Damage", numero(gato.getDamage(), 0) + bonus(gato, Modifiers.Damage, 0),
                Palette.TEXT, null, "Added to every hit, from every weapon."));
        // A cadência é um INTERVALO em ticks: a tela mostra golpes por segundo, que
        // é o que o jogador sente, e o bônus na mesma unidade.
        double intervalo = gato.getAttackSpeed();
        double base = intervalo - (gato.hasModifier(Modifiers.AttackSpeed) ? gato.valueModifier(Modifiers.AttackSpeed) : 0);
        double porSeg = 60 / Math.max(1, intervalo), porSegBase = 60 / Math.max(1, base);
        l.add(new Linha("Attack speed", numero(porSeg, 1) + "/s" + diferenca(porSeg - porSegBase, 1),
                Palette.TEXT, null, "Hand weapon attacks per second (" + numero(intervalo, 0)
                + " ticks between attacks)."));
        l.add(new Linha("Move speed", numero(gato.getSpeed(), 1) + bonus(gato, Modifiers.Speed, 1),
                Palette.TEXT, null, "How fast the cat runs."));
        l.add(new Linha("Range", numero(gato.getRange(), 1) + bonus(gato, Modifiers.Range, 1),
                Palette.TEXT, null, "Reach of the hand weapon."));
        double resistencia = gato.hasModifier(Modifiers.Resistance) ? gato.valueModifier(Modifiers.Resistance) : 0;
        l.add(new Linha("Damage taken", "-" + porcento(Math.min(0.8, Math.max(0, resistencia))),
                Palette.TEXT, null, "Cut from every hit you take. Capped at 80%."));
        l.add(new Linha("Luck", numero(gato.getLuck(), 0) + bonus(gato, Modifiers.Luck, 0),
                Palette.TEXT, null, "Luck points: " + porcento(gato.chanceDeSorte())
                + " chance roll. Also raises chest drops and critical chance."));
        l.add(new Linha("Critical", porcento(Bonus.critico(gato)), Palette.TEXT, null,
                "Chance for a hit to deal extra damage."));
        l.add(new Linha("Area", porcento(Bonus.area(gato)), Palette.TEXT, null,
                "Size of area weapons, relative to their base."));
        l.add(new Linha("Regen", numero(Bonus.regeneracao(gato), 1) + " HP/s", Palette.TEXT, null,
                "Health recovered every second."));
        l.add(new Linha("XP gain", porcento(Bonus.crescimento(gato)), Palette.TEXT, null,
                "Experience received, relative to the base."));
        l.add(new Linha("Pickup range", porcento(Bonus.ima(gato)), Palette.TEXT, null,
                "How far XP gems fly to you, relative to the base."));
        return l;
    }

    private static List<Linha> corrida(Player gato) {
        List<Linha> l = new ArrayList<>();
        l.add(new Linha("Level", gato.getLevel() + "  (" + (int) gato.getXp() + "/" + (int) gato.getXpLength() + " XP)",
                Palette.TEXT, null, "XP to the next level."));
        if (Game.getWave() != null) {
            l.add(new Linha("Kills", String.valueOf(Game.getWave().abates()), Palette.TEXT, null,
                    "Enemies defeated this run."));
        }
        l.add(new Linha("Gold", String.valueOf(gato.getMoney()), Palette.TEXT, null,
                "Coins carried. Spent only at the merchant before the run."));
        l.add(new Linha("Rerolls", String.valueOf(SubirDeNivel.trocas()), Palette.TEXT, null,
                "Level-up hands you can still reroll this run."));
        l.add(new Linha("Skips", String.valueOf(SubirDeNivel.pulos()), Palette.TEXT, null,
                "Level-ups you can still skip for a small heal."));
        return l;
    }

    private static List<Linha> armas(Player gato) {
        List<Linha> l = new ArrayList<>();
        Item mao = gato.getInventory().getItemHand();
        l.add(new Linha("Hand", mao == null ? "-" : mao.getName(), Palette.TEXT, null,
                "The weapon you started with. It attacks on its own."));
        Elemento e = gato.elemento();
        if (e != Elemento.NENHUM) {
            l.add(new Linha("Element", e.rotulo(), e.cor(),
                    sinal(e.dano(1.0) - 1) + " damage  " + sinal(1 - e.cadencia()) + " speed",
                    "Chosen for the whole run. It changes damage and attack speed together."));
        } else {
            l.add(new Linha("Element", "None yet", Palette.LIGHT, null,
                    "Offered 45 seconds into the run."));
        }
        Melhorias m = gato.getMelhorias();
        for (Map.Entry<ArmaExtra, Integer> a : m.armas().entrySet()) {
            ArmaExtra arma = a.getKey();
            boolean evoluiu = m.evoluiu(arma);
            Evolucao ev = Evolucao.de(arma);
            String nome = evoluiu && ev != null ? ev.nome() : arma.nome();
            String nivel = evoluiu ? "Evolved" : "Lv " + a.getValue() + "/" + ArmaExtra.NIVEL_MAXIMO;
            String ajuda = evoluiu && ev != null ? ev.descricao() : arma.efeito(1);
            l.add(new Linha(nome, nivel, arma.cor(), null, ajuda));
        }
        return l;
    }

    private static List<Linha> buffs(Player gato) {
        List<Linha> l = new ArrayList<>();
        Melhorias m = gato.getMelhorias();
        for (Map.Entry<Passiva, Integer> p : m.passivas().entrySet()) {
            Passiva passiva = p.getKey();
            int nivel = p.getValue();
            String proximo = nivel >= passiva.nivelMaximo() ? "Max level."
                    : "Next level: " + passiva.efeitoNoNivel(nivel + 1) + ".";
            l.add(new Linha(passiva.nome(), "Lv " + nivel + "/" + passiva.nivelMaximo(), passiva.cor(),
                    passiva.efeitoTotal(nivel), passiva.assunto() + ". " + proximo));
        }
        // AS CARTAS DE ATRIBUTO do subir de nível: quantas vieram e o que somaram.
        Object[][] atributos = {
                {Modifiers.Life, "Health cards", "+%.0f max health"},
                {Modifiers.Damage, "Damage cards", "+%.0f damage"},
                {Modifiers.Speed, "Speed cards", "+%.1f move speed"},
                {Modifiers.AttackSpeed, "Attack cards", "%.0f ticks between attacks"},
                {Modifiers.Luck, "Luck cards", "+%.0f luck"},
        };
        for (Object[] at : atributos) {
            Modifiers mod = (Modifiers) at[0];
            int quantas = m.pontos(mod);
            if (quantas > 0) {
                l.add(new Linha((String) at[1], "x" + quantas, Palette.ACCENT,
                        String.format(Locale.ROOT, (String) at[2], m.somaDosPontos(mod)),
                        "Attribute cards taken on level-up, and their total."));
            }
        }
        // AS MELHORIAS DA ROLETA, com raridade e desgaste já somados.
        for (Map.Entry<Melhoria, Integer> mel : m.todas().entrySet()) {
            Melhoria melhoria = mel.getKey();
            l.add(new Linha(melhoria.nome(), "x" + mel.getValue(), Palette.ACCENT, melhoria.total(m.soma(melhoria)),
                    "Upgrade won from the slot machine. Each copy is worth a bit less."));
        }
        if (l.isEmpty()) {
            l.add(new Linha("No buffs yet", "", Palette.LIGHT, null,
                    "Level up and open chests to collect buffs."));
        }
        return l;
    }

    // ---------------------------------------------------------- formatação

    private static String numero(double v, int casas) {
        return casas == 0 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%." + casas + "f", v);
    }

    private static String porcento(double fracao) {
        return Math.round(fracao * 100) + "%";
    }

    private static String sinal(double fracao) {
        long p = Math.round(fracao * 100);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    /** " (+6)" com o que os modificadores somam a este atributo, ou nada. */
    private static String bonus(Player gato, Modifiers mod, int casas) {
        if (!gato.hasModifier(mod)) {
            return "";
        }
        return diferenca(gato.valueModifier(mod), casas);
    }

    private static String diferenca(double v, int casas) {
        if (Math.abs(v) < (casas == 0 ? 0.5 : 0.05)) {
            return "";
        }
        return " (" + (v > 0 ? "+" : "") + numero(v, casas) + ")";
    }

    // --------------------------------------------------------------- layout

    private static final int LINHA = 10, LINHA_DUPLA = 17, TITULO = 13, VAO_DE_BLOCO = 6;
    /** Altura da faixa de detalhe no pé do painel, em pixels nativos. */
    private static final int DETALHE = 20;

    @Override
    protected void posicionar() {
        int s = Configs.UiScale();
        Rectangle area = areaDoConteudo();
        int largura = Math.min(Engine.window.largura() - 16 * s, 340 * s);
        int altura = area.height;
        painel = new Rectangle(Engine.window.largura() / 2 - largura / 2, area.y, largura, altura);

        int pad = 10 * s, vao = 10 * s;
        int colW = (largura - 2 * pad - 2 * vao) / 3;
        caixas.clear();
        visiveis.clear();
        for (int c = 0; c < colunas.size(); c++) {
            int x = painel.x + pad + c * (colW + vao);
            int y = painel.y + pad;
            int fim = painel.y + painel.height - pad - DETALHE * s;
            List<Rectangle> rc = new ArrayList<>();
            List<Linha> lv = new ArrayList<>();
            for (Bloco b : colunas.get(c)) {
                y += TITULO * s;
                for (Linha l : b.linhas()) {
                    int h = (l.dupla() ? LINHA_DUPLA : LINHA) * s;
                    // SEM ROLAGEM: o que não cabe some, e a última linha que cabe
                    // avisa quantas ficaram de fora. Em 720p cabem 14 buffs por coluna.
                    if (y + h > fim) {
                        break;
                    }
                    rc.add(new Rectangle(x, y, colW, h));
                    lv.add(l);
                    y += h;
                }
                y += VAO_DE_BLOCO * s;
            }
            caixas.add(rc);
            visiveis.add(lv);
        }
        if (linha >= 0) {
            coluna = Math.min(coluna, caixas.size() - 1);
            linha = Math.min(linha, caixas.get(coluna).size() - 1);
        }
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void atualizar() {
        // O E FECHA, além do Esc e do B: a mesma tecla que abriu. Com a tela aberta
        // o jogo não recebe ticks, então este E não chega a mais ninguém.
        if (KeyBoard.KeyPressed("E")) {
            voltar();
            return;
        }
        int antes = coluna * 1000 + linha;
        if (KeyBoard.KeyPressed("Down")) {
            linha = linha < 0 ? 0 : Math.min(caixas.get(coluna).size() - 1, linha + 1);
        } else if (KeyBoard.KeyPressed("Up")) {
            linha = Math.max(0, linha - 1);
        } else if (KeyBoard.KeyPressed("Right")) {
            mudarDeColuna(1);
        } else if (KeyBoard.KeyPressed("Left")) {
            mudarDeColuna(-1);
        }
        if (Entrada.porMouse()) {
            for (int c = 0; c < caixas.size(); c++) {
                for (int i = 0; i < caixas.get(c).size(); i++) {
                    if (caixas.get(c).get(i).contains(Mouse.getX(), Mouse.getY())) {
                        coluna = c;
                        linha = i;
                    }
                }
            }
        }
        if (coluna * 1000 + linha != antes && linha >= 0) {
            Sound.play(Sounds.Hover);
        }
    }

    private void mudarDeColuna(int dir) {
        int n = caixas.size();
        for (int k = 1; k <= n; k++) {
            int c = ((coluna + dir * k) % n + n) % n;
            if (!caixas.get(c).isEmpty()) {
                coluna = c;
                linha = Math.max(0, Math.min(linha, caixas.get(c).size() - 1));
                return;
            }
        }
    }

    @Override
    protected List<Rodape.Dica> dicas() {
        return List.of(new Rodape.Dica("Arrows", "D-pad", "Details"), new Rodape.Dica("E/Esc", "Y/B", "Close"));
    }

    // ---------------------------------------------------------------- render

    @Override
    protected void desenharConteudo(Graphics2D g) {
        int s = Configs.UiScale();
        desenharPainel(g, painel);
        desenharDetalhe(g);
        Font titulo = FontHandler.font(FontHandler.Game, 7f * s);
        Font rotulo = FontHandler.font(FontHandler.Game, 6f * s);
        Font efeito = FontHandler.font(FontHandler.Septem, 6f * s);
        int pad = 10 * s;
        for (int c = 0; c < colunas.size(); c++) {
            int i = 0;
            List<Rectangle> rc = caixas.get(c);
            int y = painel.y + pad;
            for (Bloco b : colunas.get(c)) {
                int x = rc.isEmpty() ? painel.x + pad : rc.getFirst().x;
                int w = rc.isEmpty() ? 0 : rc.getFirst().width;
                TextFx.draw(g, b.titulo(), titulo, x, y + 8 * s, Palette.ACCENT, s);
                g.setColor(Palette.OUTLINE);
                g.fillRect(x, y + 10 * s, w, s);
                y += TITULO * s;
                int mostradas = 0;
                for (Linha l : b.linhas()) {
                    if (i >= rc.size() || visiveis.get(c).get(i) != l) {
                        break;
                    }
                    Rectangle r = rc.get(i);
                    desenharLinha(g, l, r, c == coluna && i == linha, rotulo, efeito, s);
                    y = r.y + r.height;
                    i++;
                    mostradas++;
                }
                if (mostradas < b.linhas().size() && !rc.isEmpty()) {
                    String resto = "+" + (b.linhas().size() - mostradas) + " more";
                    TextFx.draw(g, resto, efeito, x, y + 7 * s, Palette.LIGHT, s);
                }
                y += VAO_DE_BLOCO * s;
            }
        }
    }

    private void desenharLinha(Graphics2D g, Linha l, Rectangle r, boolean focada, Font rotulo, Font efeito, int s) {
        if (focada) {
            Rectangle faixa = new Rectangle(r.x - 2 * s, r.y, r.width + 4 * s, r.height);
            Realce.clarear(g, faixa, 0.8f);
            if (Entrada.porFoco()) {
                Realce.marcadorDeFoco(g, faixa, s);
            }
        }
        int base = r.y + 8 * s;
        // O valor encolhe antes de passar por cima do rótulo.
        Font fv = rotulo;
        int espaco = r.width - FontHandler.getWidth(l.rotulo(), rotulo) - 4 * s;
        float tam = fv.getSize2D();
        while (FontHandler.getWidth(l.valor(), fv) > espaco && tam > 4f) {
            tam -= 1f;
            fv = FontHandler.font(FontHandler.Game, tam);
        }
        TextFx.draw(g, l.rotulo(), rotulo, r.x, base, focada ? Palette.TEXT : Palette.LIGHT, s);
        TextFx.draw(g, l.valor(), fv, r.x + r.width - FontHandler.getWidth(l.valor(), fv), base, l.cor(), s);
        if (l.dupla()) {
            TextFx.draw(g, l.efeito(), efeito, r.x + 4 * s, base + 7 * s,
                    l.cor() == Palette.TEXT ? Palette.ACCENT : clarear(l.cor()), s);
        }
    }

    private static Color clarear(Color c) {
        return new Color(Math.min(255, c.getRed() + 50), Math.min(255, c.getGreen() + 50),
                Math.min(255, c.getBlue() + 50));
    }

    /**
     * A FAIXA DE DETALHE, no pé do painel: o que a linha focada (ou sob o mouse)
     * faz. Fixa, e não um balão: com três colunas não há lado livre, e um balão
     * por cima tapava justamente a coluna vizinha que o jogador ia ler em seguida.
     */
    private void desenharDetalhe(Graphics2D g) {
        int s = Configs.UiScale();
        int pad = 10 * s;
        int x = painel.x + pad, w = painel.width - 2 * pad;
        int y = painel.y + painel.height - pad - DETALHE * s;
        g.setColor(Palette.OUTLINE);
        g.fillRect(x, y, w, s);
        String texto = "Select a line to see what it does.";
        Color cor = Palette.LIGHT;
        if (linha >= 0 && coluna < visiveis.size() && linha < visiveis.get(coluna).size()) {
            Linha l = visiveis.get(coluna).get(linha);
            if (l.ajuda() != null && !l.ajuda().isEmpty()) {
                texto = l.rotulo() + ": " + l.ajuda();
                cor = Palette.TEXT;
            }
        }
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        int ty = y + 9 * s;
        List<String> linhas = quebrar(texto, fonte, w);
        for (int i = 0; i < Math.min(2, linhas.size()); i++) {
            TextFx.draw(g, linhas.get(i), fonte, x, ty, cor, s);
            ty += 7 * s;
        }
    }

    private static List<String> quebrar(String texto, Font fonte, int largura) {
        List<String> saida = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String teste = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (FontHandler.getWidth(teste, fonte) > largura && !atual.isEmpty()) {
                saida.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(teste);
            }
        }
        if (!atual.isEmpty()) {
            saida.add(atual.toString());
        }
        return saida;
    }
}
