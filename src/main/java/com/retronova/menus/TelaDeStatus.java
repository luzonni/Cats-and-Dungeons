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
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.arena.SubirDeNivel;
import com.retronova.game.objects.entities.Bonus;
import com.retronova.game.espelho.Espelho;
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
    public record Linha(String rotulo, String valor, Color cor, String efeito, String ajuda) {
        boolean dupla() {
            return efeito != null && !efeito.isEmpty();
        }
    }

    /** Um bloco de linhas com título, dentro de uma coluna. */
    private record Bloco(String titulo, List<Linha> linhas) {
    }

    private final List<List<Bloco>> colunas = new ArrayList<>();
    /** Todas as linhas de cada coluna, em ordem, sem os títulos. O foco é um índice aqui. */
    private final List<List<Linha>> todas = new ArrayList<>();
    /** A caixa de cada linha de {@link #todas}, ou null se ela está rolada para fora. */
    private final List<List<Rectangle>> caixas = new ArrayList<>();
    /** Os títulos de bloco que aparecem, com a caixa: o desenho percorre só o que coube. */
    private final List<List<Titulo>> titulos = new ArrayList<>();
    /** Por coluna: a primeira linha mostrada, e quantas ficaram acima e abaixo do que cabe. */
    private int[] rolagem = new int[0], acima = new int[0], abaixo = new int[0];
    private Rectangle painel = new Rectangle();
    private int coluna, linha = -1;

    private record Titulo(String texto, Rectangle caixa) {
    }

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

    static List<Linha> atributos(Player gato) {
        List<Linha> l = new ArrayList<>();
        l.add(new Linha("Health", (int) Math.ceil(gato.getLife()) + "/" + (int) gato.getLifeSize(), Palette.TEXT,
                camadas(gato, Modifiers.Life, Espelho.Efeito.VIDA, gato.getLifeSize(), 0),
                "Current and maximum health. Max = base + mirror upgrades + this run's cards."));
        l.add(new Linha("Damage", numero(gato.getDamage(), 0), Palette.TEXT,
                camadas(gato, Modifiers.Damage, Espelho.Efeito.DANO, gato.getDamage(), 0),
                "Added to every hit, from every weapon."));
        // A cadência é um INTERVALO em ticks: a tela mostra golpes por segundo, que
        // é o que o jogador sente, e o bônus na mesma unidade.
        double intervalo = gato.getAttackSpeed();
        double base = intervalo - (gato.hasModifier(Modifiers.AttackSpeed) ? gato.valueModifier(Modifiers.AttackSpeed) : 0);
        double porSeg = 60 / Math.max(1, intervalo), porSegBase = 60 / Math.max(1, base);
        l.add(new Linha("Attack speed", numero(porSeg, 1) + "/s" + diferenca(porSeg - porSegBase, 1),
                Palette.TEXT, null, "Hand weapon attacks per second (" + numero(intervalo, 0)
                + " ticks between attacks)."));
        l.add(new Linha("Move speed", numero(gato.getSpeed(), 1), Palette.TEXT,
                camadas(gato, Modifiers.Speed, Espelho.Efeito.VELOCIDADE, gato.getSpeed(), 2),
                "How fast the cat runs."));
        l.add(new Linha("Range", numero(gato.getRange(), 1), Palette.TEXT,
                camadas(gato, Modifiers.Range, Espelho.Efeito.ALCANCE, gato.getRange(), 1),
                "Reach of the hand weapon."));
        double resistencia = gato.hasModifier(Modifiers.Resistance) ? gato.valueModifier(Modifiers.Resistance) : 0;
        double resEspelho = Espelho.valor(gato, Espelho.Efeito.RESISTENCIA), resInata = Bonus.resistenciaInata(gato);
        l.add(new Linha("Damage taken", "-" + porcento(Math.min(0.8, Math.max(0, resistencia))), Palette.TEXT,
                camadasEmPorcento(resInata, resEspelho, resistencia - resEspelho - resInata),
                "Cut from every hit you take. Capped at 80%. Base is the cat's own skill."));
        l.add(new Linha("Luck", numero(gato.getLuck(), 0), Palette.TEXT,
                camadas(gato, Modifiers.Luck, Espelho.Efeito.SORTE, gato.getLuck(), 0),
                "Luck points: " + porcento(gato.chanceDeSorte())
                + " chance roll. Also raises chest drops and critical chance."));
        double critico = Bonus.critico(gato), critEspelho = Espelho.valor(gato, Espelho.Efeito.CRITICO);
        double critInato = Bonus.criticoInato(gato);
        l.add(new Linha("Critical", porcento(critico), Palette.TEXT,
                camadasEmPorcento(critInato, critEspelho, critico - critEspelho - critInato),
                "Chance for a hit to deal extra damage. Run includes passives and luck."));
        l.add(new Linha("Area", porcento(Bonus.area(gato)), Palette.TEXT, null,
                "Size of area weapons, relative to their base."));
        l.add(new Linha("Regen", numero(Bonus.regeneracao(gato), 1) + " HP/s", Palette.TEXT, null,
                "Health recovered every second."));
        l.add(new Linha("XP gain", porcento(Bonus.crescimento(gato)), Palette.TEXT, null,
                "Experience received, relative to the base."));
        double ima = Bonus.ima(gato), imaEspelho = Espelho.valor(gato, Espelho.Efeito.COLETA);
        l.add(new Linha("Pickup range", porcento(ima), Palette.TEXT,
                camadasEmPorcento(1, imaEspelho, ima - 1 - imaEspelho),
                "How far XP gems fly to you, relative to the base."));
        return l;
    }

    // ---------------------------------------------------- base + espelho + corrida

    /**
     * AS TRÊS CAMADAS DE UM ATRIBUTO, na linha de baixo: o que é do gato, o que o
     * espelho do vendedor soma e o que esta corrida somou (cartas, passivas). O
     * número grande continua sendo o total; isto responde "de onde veio".
     *
     * A base é o total menos os modificadores; o espelho vem da tabela; a corrida
     * é o resto dos modificadores (ver Espelho.decompor).
     */
    private static String camadas(Player gato, Modifiers mod, Espelho.Efeito efeito, double total, int casas) {
        double modificador = gato.hasModifier(mod) ? gato.valueModifier(mod) : 0;
        Espelho.Camadas c = Espelho.decompor(total - modificador, modificador, Espelho.valor(gato, efeito));
        return "Base " + numero(c.base(), casas) + "   Mirror " + comSinal(c.espelho(), casas)
                + "   Run " + comSinal(c.corrida(), casas);
    }

    private static String camadasEmPorcento(double base, double espelho, double corrida) {
        return "Base " + porcento(base) + "   Mirror " + sinal(espelho) + "   Run " + sinal(corrida);
    }

    private static String comSinal(double v, int casas) {
        if (Math.abs(v) < (casas == 0 ? 0.5 : Math.pow(10, -casas) / 2)) {
            return "+0";
        }
        return (v > 0 ? "+" : "") + numero(v, casas);
    }

    private static List<Linha> corrida(Player gato) {
        List<Linha> l = new ArrayList<>();
        l.add(new Linha("Level", gato.getLevel() + "  (" + (int) gato.getXp() + "/" + (int) gato.getXpLength() + " XP)",
                Palette.TEXT, null, "XP to the next level."));
        // SÓ NA ARENA: fora dela getWave lança NotInMap, e não devolve null. Abrir o
        // Status (E) na sala do começo derrubava o jogo.
        if (Game.getMap() instanceof Arena) {
            l.add(new Linha("Kills", String.valueOf(Game.getWave().abates()), Palette.TEXT, null,
                    "Enemies defeated this run."));
        }
        // O SALDO DO ESPELHO, e nao mais o dinheiro da corrida (Player.money), que
        // ficou sem uso com a loja de armas em standby.
        l.add(new Linha("Coins", String.valueOf(com.retronova.game.save.Progresso.moedas(gato.getName())),
                Palette.TEXT, null, "Saved coins. This run's coins are added when it ends. "
                + "Spend them at the merchant's mirror."));
        l.add(new Linha("Rerolls", String.valueOf(SubirDeNivel.trocas()), Palette.TEXT, null,
                "Level-up hands you can still reroll this run."));
        l.add(new Linha("Skips", String.valueOf(SubirDeNivel.pulos()), Palette.TEXT, null,
                "Level-ups you can still skip for a small heal."));
        return l;
    }

    static List<Linha> armas(Player gato) {
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
            if (!ArmaExtra.ATIVAS) continue;
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

    static List<Linha> buffs(Player gato) {
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
    /** Altura dos avisos "N more above / below" das colunas roladas. */
    private static final int AVISO = 9;

    @Override
    protected void posicionar() {
        int s = Configs.UiScale();
        Rectangle area = areaDoConteudo();
        int largura = Math.min(Engine.window.largura() - 16 * s, 340 * s);
        int altura = area.height;
        painel = new Rectangle(Engine.window.largura() / 2 - largura / 2, area.y, largura, altura);

        if (rolagem.length != colunas.size()) {
            rolagem = new int[colunas.size()];
            acima = new int[colunas.size()];
            abaixo = new int[colunas.size()];
        }
        todas.clear();
        caixas.clear();
        titulos.clear();
        for (int c = 0; c < colunas.size(); c++) {
            List<Linha> lc = new ArrayList<>();
            for (Bloco b : colunas.get(c)) {
                lc.addAll(b.linhas());
            }
            todas.add(lc);
            caixas.add(new ArrayList<>());
            titulos.add(new ArrayList<>());
            rolagem[c] = Math.max(0, Math.min(rolagem[c], lc.size() - 1));
            // COM ROLAGEM, e não mais cortando: a coluna dos buffs passava de 14 linhas
            // em 720p no meio da corrida, e o resto virava um "+4 more" que não havia
            // como abrir. Primeiro tenta caber; se não couber, reserva o aviso de baixo.
            if (!disporColuna(c, s, false)) {
                disporColuna(c, s, true);
            }
        }
        if (linha >= 0) {
            coluna = Math.min(coluna, todas.size() - 1);
            linha = Math.min(linha, todas.get(coluna).size() - 1);
        }
    }

    /**
     * Põe a coluna na tela a partir da linha {@code rolagem[c]}: títulos dos blocos
     * que ainda têm linha à mostra, e as linhas até onde cabem. Devolve se todas as
     * linhas a partir da rolagem couberam.
     */
    private boolean disporColuna(int c, int s, boolean reservarAbaixo) {
        int pad = 10 * s, vao = 10 * s;
        int colW = (painel.width - 2 * pad - 2 * vao) / 3;
        int x = painel.x + pad + c * (colW + vao);
        int y = painel.y + pad + (rolagem[c] > 0 ? AVISO * s : 0);
        int fim = painel.y + painel.height - pad - DETALHE * s - (reservarAbaixo ? AVISO * s : 0);
        List<Rectangle> rc = caixas.get(c);
        List<Titulo> tc = titulos.get(c);
        rc.clear();
        tc.clear();
        int k = 0, mostradas = 0;
        boolean cheia = false;
        for (Bloco b : colunas.get(c)) {
            int ultimo = k + b.linhas().size();
            if (ultimo <= rolagem[c] || cheia) {          // bloco todo acima da rolagem, ou já sem espaço
                for (int i = k; i < ultimo; i++) {
                    rc.add(null);
                }
                k = ultimo;
                continue;
            }
            if (y + TITULO * s > fim) {
                cheia = true;
                for (int i = k; i < ultimo; i++) {
                    rc.add(null);
                }
                k = ultimo;
                continue;
            }
            tc.add(new Titulo(b.titulo(), new Rectangle(x, y, colW, TITULO * s)));
            y += TITULO * s;
            for (Linha l : b.linhas()) {
                int h = (l.dupla() ? LINHA_DUPLA : LINHA) * s;
                if (k < rolagem[c] || cheia || y + h > fim) {
                    cheia |= k >= rolagem[c];
                    rc.add(null);
                } else {
                    rc.add(new Rectangle(x, y, colW, h));
                    y += h;
                    mostradas++;
                }
                k++;
            }
            y += VAO_DE_BLOCO * s;
        }
        acima[c] = rolagem[c];
        abaixo[c] = k - rolagem[c] - mostradas;
        return abaixo[c] == 0;
    }

    /** Rola a coluna até a linha i aparecer inteira. */
    private void mostrar(int c, int i) {
        if (i < rolagem[c]) {
            rolagem[c] = i;
            posicionar();
        }
        while (caixas.get(c).get(i) == null && rolagem[c] < i) {
            rolagem[c]++;
            posicionar();
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
        // O FOCO LEVA A ROLAGEM: descer da última linha à mostra rola a coluna, e
        // subir da primeira também. Assim teclado e controle chegam a todos os buffs.
        if (KeyBoard.KeyPressed("Down")) {
            linha = linha < 0 ? rolagem[coluna] : Math.min(todas.get(coluna).size() - 1, linha + 1);
            mostrar(coluna, linha);
        } else if (KeyBoard.KeyPressed("Up")) {
            linha = Math.max(0, linha - 1);
            mostrar(coluna, linha);
        } else if (KeyBoard.KeyPressed("Right")) {
            mudarDeColuna(1);
        } else if (KeyBoard.KeyPressed("Left")) {
            mudarDeColuna(-1);
        }
        // A roda do mouse rola a coluna que está debaixo dele.
        int roda = Mouse.Scroll();
        if (roda != 0) {
            for (int c = 0; c < caixas.size(); c++) {
                if (sobreAColuna(c)) {
                    if (roda > 0 && abaixo[c] > 0) {
                        rolagem[c]++;
                    } else if (roda < 0 && rolagem[c] > 0) {
                        rolagem[c]--;
                    }
                    posicionar();
                }
            }
        }
        if (Entrada.porMouse()) {
            for (int c = 0; c < caixas.size(); c++) {
                for (int i = 0; i < caixas.get(c).size(); i++) {
                    Rectangle r = caixas.get(c).get(i);
                    if (r != null && r.contains(Mouse.getX(), Mouse.getY())) {
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
                // Na mesma altura, mas dentro do que a coluna nova mostra agora.
                int ultima = rolagem[c];
                for (int i = rolagem[c]; i < caixas.get(c).size() && caixas.get(c).get(i) != null; i++) {
                    ultima = i;
                }
                linha = Math.max(rolagem[c], Math.min(Math.max(linha, 0), ultima));
                return;
            }
        }
    }

    /** O mouse está na faixa vertical da coluna c, dentro do painel. */
    private boolean sobreAColuna(int c) {
        int s = Configs.UiScale();
        int pad = 10 * s, vao = 10 * s;
        int colW = (painel.width - 2 * pad - 2 * vao) / 3;
        int x = painel.x + pad + c * (colW + vao);
        return Mouse.getX() >= x - vao / 2 && Mouse.getX() < x + colW + vao / 2
                && painel.contains(Mouse.getX(), Mouse.getY());
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
        // Os avisos da rolagem na fonte dos valores: na Septem o 7 lia como 1.
        Font aviso = FontHandler.font(FontHandler.Game, 5f * s);
        int pad = 10 * s;
        for (int c = 0; c < colunas.size(); c++) {
            for (Titulo t : titulos.get(c)) {
                Rectangle r = t.caixa();
                TextFx.draw(g, t.texto(), titulo, r.x, r.y + 8 * s, Palette.ACCENT, s);
                g.setColor(Palette.OUTLINE);
                g.fillRect(r.x, r.y + 10 * s, r.width, s);
            }
            List<Rectangle> rc = caixas.get(c);
            int base = Integer.MIN_VALUE;
            for (int i = 0; i < rc.size(); i++) {
                Rectangle r = rc.get(i);
                if (r != null) {
                    desenharLinha(g, todas.get(c).get(i), r, c == coluna && i == linha, rotulo, efeito, s);
                    base = Math.max(base, r.y + r.height);
                }
            }
            // OS AVISOS DA ROLAGEM: quantas linhas há acima e abaixo do que se vê.
            if (titulos.get(c).isEmpty() && base == Integer.MIN_VALUE) {
                continue;
            }
            int x = titulos.get(c).isEmpty() ? firstX(rc) : titulos.get(c).getFirst().caixa().x;
            if (acima[c] > 0) {
                TextFx.draw(g, acima[c] + " more above", aviso, x, painel.y + pad + 6 * s, Palette.LIGHT, s);
            }
            if (abaixo[c] > 0) {
                TextFx.draw(g, abaixo[c] + " more below", aviso, x, base + 7 * s, Palette.LIGHT, s);
            }
        }
    }

    private int firstX(List<Rectangle> rc) {
        for (Rectangle r : rc) {
            if (r != null) {
                return r.x;
            }
        }
        return painel.x + 10 * Configs.UiScale();
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
        if (linha >= 0 && coluna < todas.size() && linha < todas.get(coluna).size()) {
            Linha l = todas.get(coluna).get(linha);
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
