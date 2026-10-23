package com.retronova.menus.controles;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Glyphs;
import com.retronova.engine.graphics.Glyphs.Glyph;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Acao;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.gamepad.Comando;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.menus.shared.Realce;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O conteúdo da aba Controls do Settings: o que cada tecla e cada botão fazem.
 *
 * ---------------------------------------------------------------------------
 * DUAS VISÕES, A DO APARELHO NA MÃO. Teclado e mouse de um lado, controle do
 * outro, trocadas pela linha Device logo acima (um OptionRow, como todo o resto
 * do Settings). A aba abre na do último aparelho usado (Entrada): quem chega com
 * o controle na mão quer ver o controle. É o que Hades e Dead Cells fazem.
 *
 * TECLADO: uma lista em duas colunas, por grupo (Movement, In a run, Menus,
 * System), com a ação à esquerda e as teclas à direita, alinhadas na mesma borda
 * para o olho correr a coluna. WASD e as setas do movimento aparecem em cruz,
 * como ficam na mão. SEM MINI-TECLADO: o jogo usa umas vinte teclas espalhadas de
 * F3 a Enter, e um teclado inteiro na largura deste painel deixaria cada tecla
 * com 4 ou 5 pixels — ilegível —, para dizer o mesmo que a lista diz melhor. Dead
 * Cells e Hollow Knight também listam, e é o que a Game Accessibility Guidelines
 * pede de uma tela de controles: ação e entrada lado a lado, em texto legível.
 *
 * CONTROLE: o desenho no meio (DesenhoDoControle) com uma linha de cada peça até
 * o rótulo dela, gatilhos e bumpers no alto, analógico esquerdo e direcional à
 * esquerda, botões de face e analógico direito à direita (LigacoesDoControle). O
 * rótulo diz o que a peça faz na corrida em branco e, abaixo, o que faz nos
 * menus em cinza — a mesma peça muda de papel (o B interage em jogo e volta nas
 * telas), e a legenda no alto explica as duas cores.
 *
 * Passar o foco ou o mouse por uma ação acende a peça no desenho, e passar o
 * mouse pela peça acende a ação. Nada aqui é escrito à mão: as ações saem de
 * {@link Acao} e as teclas e botões, dos mapas reais do teclado e do controle.
 */
public final class PainelDeControles {

    // ----------------------------------------------------------- ajuste fino

    /** Vão entre as duas colunas da visão do teclado, em pixels nativos. */
    private static final int VAO_ENTRE_COLUNAS = 12;
    /** Folga acima e abaixo de cada linha de ação, e antes de cada grupo. */
    private static final int FOLGA_DA_LINHA = 2, FOLGA_ANTES_DO_GRUPO = 3;
    /** Altura do título de um grupo. */
    private static final int ALTURA_DO_GRUPO = 9;
    /** Espaço entre um desenho de tecla e o seguinte, e entre teclas e mouse. */
    private static final int VAO_ENTRE_TECLAS = 1, VAO_ANTES_DO_MOUSE = 6;
    /** Distância da borda do desenho do controle até a ponta da linha, e entre rótulos. */
    private static final int FOLGA_DA_LINHA_DO_CONTROLE = 10, VAO_ENTRE_ROTULOS = 2;
    /** Altura do seletor de contexto (In a run / In menus) acima do desenho, com o vão até ele. */
    private static final int ALTURA_DA_LEGENDA = 16;
    /** Altura de cada metade do seletor, e a folga do texto dentro dela. */
    private static final int ALTURA_DO_SELETOR = 11, FOLGA_DO_SELETOR = 6;
    /** Os dois contextos do seletor, na ordem da tela. */
    private static final String[] CONTEXTOS = {"In a run", "In menus"};

    public enum Visao { TECLADO, CONTROLE }

    // ----------------------------------------------------------- estado

    private Visao visao = Visao.TECLADO;
    private Rectangle area = new Rectangle();
    /** A escala de cada visão: a da interface, ou menor quando ela não cabe (ver escalaQueCabe). */
    private int escalaTeclado = 1, escalaControle = 1;
    /** A escala do desenho do controle, que pode passar da do texto (ver posicionarControle). */
    private int escalaDoDesenho = 1;

    /** Uma ação da visão do teclado, com a caixa dela na tela. */
    private static final class Linha {
        final Acao acao;
        final int coluna;
        final Rectangle caixa = new Rectangle();
        final Realce.Estado realce = new Realce.Estado();

        Linha(Acao acao, int coluna) {
            this.acao = acao;
            this.coluna = coluna;
        }
    }

    private record Cabecalho(String texto, int x, int y, int largura) {
    }

    private final List<Linha> linhas = new ArrayList<>();
    private final List<Cabecalho> cabecalhos = new ArrayList<>();
    /** Quanto a lista do teclado está rolada, quando não cabe (telas baixas). */
    private int rolagem, alturaDaLista;

    private Map<Comando, LigacoesDoControle.Ligacao> ligacoes = new EnumMap<>(Comando.class);
    private final List<Comando> ordemDoControle = new ArrayList<>();
    private final Map<Comando, Realce.Estado> realceDaPeca = new EnumMap<>(Comando.class);
    private final Set<Comando> semAcao = EnumSet.noneOf(Comando.class);
    private int desenhoX, desenhoY, yDaLegenda;

    /**
     * O CONTEXTO DO CONTROLE: o que os botões fazem na corrida ou nos menus. A
     * mesma peça muda de papel (o B interage em jogo e volta nas telas, o Y abre o
     * Status e pula o nível), e mostrar os dois papéis de uma vez, um embaixo do
     * outro, pedia uma legenda de cores que se lia como botão — e não era. Agora é:
     * o jogador escolhe o contexto, cada rótulo diz uma coisa só, e a peça sem papel
     * naquele contexto fica esmaecida e sem rótulo.
     */
    private boolean nosMenus;
    private final Rectangle[] segmentos = {new Rectangle(), new Rectangle()};
    private final Realce.Estado[] realceDoSegmento = {new Realce.Estado(), new Realce.Estado()};
    /** O foco do teclado e do controle está no seletor de contexto. */
    private boolean focoNoSeletor;
    private int segmentoSobMouse = -1;

    /** O foco do teclado e do controle dentro da visão (-1: o foco está fora do painel). */
    private int foco = -1;
    /** O que está sob o mouse, na visão atual (-1: nada). */
    private int sobMouse = -1;

    public PainelDeControles() {
        for (Acao a : Acao.values()) {
            if (a.noTeclado()) {
                linhas.add(new Linha(a, a.grupo.ordinal() <= Acao.Grupo.CORRIDA.ordinal() ? 0 : 1));
            }
        }
        for (Comando c : DesenhoDoControle.PECAS.keySet()) {
            realceDaPeca.put(c, new Realce.Estado());
        }
        recalcularSemAcao();
    }

    private void recalcularSemAcao() {
        semAcao.clear();
        for (Comando c : DesenhoDoControle.PECAS.keySet()) {
            if (acaoNoContexto(c, nosMenus) == null) {
                semAcao.add(c);
            }
        }
    }

    /** Troca o contexto do controle e refaz os rótulos. */
    private void mudarContexto(boolean menus) {
        if (menus == nosMenus) {
            return;
        }
        nosMenus = menus;
        recalcularSemAcao();
        posicionarControle();
        foco = -1;
        Sound.play(Sounds.Button);
    }

    // ----------------------------------------------------------- visão

    /** A visão do último aparelho usado. Chamado ao abrir a aba. */
    public void mostrarUltimoDispositivo() {
        visao = Entrada.usandoControle() ? Visao.CONTROLE : Visao.TECLADO;
        foco = -1;
    }

    public int visao() {
        return visao.ordinal();
    }

    public void setVisao(int indice) {
        visao = Visao.values()[Math.floorMod(indice, Visao.values().length)];
        foco = -1;
        sobMouse = -1;
        focoNoSeletor = false;
    }

    public static String[] rotulosDasVisoes() {
        return new String[] {"Keyboard", "Controller"};
    }

    // ----------------------------------------------------------- layout

    /** A altura que o painel precisa para a lista do teclado caber sem rolar, em pixels nativos. */
    public static int alturaNecessaria() {
        int[] colunas = new int[2];
        Acao.Grupo grupo = null;
        for (Acao a : Acao.values()) {
            if (!a.noTeclado()) {
                continue;
            }
            int c = a.grupo.ordinal() <= Acao.Grupo.CORRIDA.ordinal() ? 0 : 1;
            if (a.grupo != grupo) {
                colunas[c] += (colunas[c] > 0 ? FOLGA_ANTES_DO_GRUPO : 0) + ALTURA_DO_GRUPO;
                grupo = a.grupo;
            }
            colunas[c] += alturaDasTeclas(a) + 2 * FOLGA_DA_LINHA;
        }
        return Math.max(colunas[0], colunas[1]);
    }

    public void posicionar(Rectangle area) {
        this.area = new Rectangle(area);
        int ui = Configs.UiScale();
        escalaTeclado = escalaQueCabe(ui, area, larguraDaLista(), alturaNecessaria());
        escalaControle = escalaQueCabe(ui, area, larguraDoControle(), alturaDoControle());
        posicionarTeclado();
        posicionarControle();
    }

    /** A escala da visão que está na tela. */
    private int escala() {
        return visao == Visao.TECLADO ? escalaTeclado : escalaControle;
    }

    /**
     * A MAIOR ESCALA INTEIRA EM QUE A VISÃO CABE, até a da interface, e cada visão
     * com a sua. Com o Menu size acima do padrão, a lista do teclado passava da
     * altura do painel e rolava; um passo abaixo ela cabe inteira, ainda nítida (a
     * regra das telas que podem não caber, como GameOver e Vitoria). O controle,
     * mais baixo, não desce junto com a lista. Só em 1x, sem ter para onde
     * descer, a lista rola.
     */
    static int escalaQueCabe(int ui, Rectangle area, int largura, int altura) {
        int e = Math.max(1, ui);
        while (e > 1 && (altura * e > area.height || largura * e > area.width)) {
            e--;
        }
        return e;
    }

    /** A largura da visão do controle: o desenho e a coluna de rótulos mais larga, dos dois lados. */
    static int larguraDoControle() {
        int rotulo = 0;
        for (boolean menus : new boolean[] {false, true}) {
            for (Comando c : DesenhoDoControle.PECAS.keySet()) {
                if (acaoNoContexto(c, menus) != null) {
                    rotulo = Math.max(rotulo, larguraDoRotulo(c, 1, menus));
                }
            }
        }
        return DesenhoDoControle.LARGURA + 2 * (4 + FOLGA_DA_LINHA_DO_CONTROLE + rotulo);
    }

    /** A altura da visão do controle: o seletor e o maior entre o desenho e a coluna de rótulos. */
    static int alturaDoControle() {
        int maior = 0;
        for (boolean menus : new boolean[] {false, true}) {
            maior = Math.max(maior, alturaDoBloco(menus, 1, 1));
        }
        return ALTURA_DA_LEGENDA + maior;
    }

    /**
     * A altura do bloco do controle, sem o seletor: a fileira de rótulos de cima, o
     * maior entre o desenho e as colunas, e a fileira de baixo.
     */
    private static int alturaDoBloco(boolean menus, int s, int escalaDoDesenho) {
        int[] coluna = new int[2];
        int cima = 0, baixo = 0;
        for (Map.Entry<Comando, DesenhoDoControle.Peca> e : DesenhoDoControle.PECAS.entrySet()) {
            if (acaoNoContexto(e.getKey(), menus) == null) {
                continue;
            }
            int h = alturaDoRotulo(e.getKey()) * s;
            switch (e.getValue().lado()) {
                case ESQUERDA -> coluna[0] += h + VAO_ENTRE_ROTULOS * s;
                case DIREITA -> coluna[1] += h + VAO_ENTRE_ROTULOS * s;
                case CIMA -> cima = Math.max(cima, h + VAO_ENTRE_ROTULOS * s);
                case BAIXO -> baixo = Math.max(baixo, h + VAO_ENTRE_ROTULOS * s);
            }
        }
        return cima + Math.max(DesenhoDoControle.ALTURA * escalaDoDesenho, Math.max(coluna[0], coluna[1])) + baixo;
    }

    /** A largura da lista do teclado: duas vezes a linha mais larga, e o vão entre as colunas. */
    static int larguraDaLista() {
        Font fonte = FontHandler.font(FontHandler.Game, 6f);
        int linha = 0;
        for (Acao a : Acao.values()) {
            if (a.noTeclado()) {
                int teclas = 0;
                for (Parte p : partes(a)) {
                    teclas += p.largura() + VAO_ENTRE_TECLAS;
                }
                linha = Math.max(linha, FontHandler.getWidth(a.rotulo, fonte) + 6 + teclas);
            }
        }
        return 2 * linha + VAO_ENTRE_COLUNAS;
    }

    private void posicionarTeclado() {
        int s = escalaTeclado;
        int largura = (area.width - VAO_ENTRE_COLUNAS * s) / 2;
        int[] y = {area.y, area.y};
        cabecalhos.clear();
        Acao.Grupo grupo = null;
        for (Linha l : linhas) {
            int x = area.x + l.coluna * (largura + VAO_ENTRE_COLUNAS * s);
            if (l.acao.grupo != grupo) {
                if (y[l.coluna] > area.y) {
                    y[l.coluna] += FOLGA_ANTES_DO_GRUPO * s;
                }
                cabecalhos.add(new Cabecalho(l.acao.grupo.rotulo, x, y[l.coluna], largura));
                y[l.coluna] += ALTURA_DO_GRUPO * s;
                grupo = l.acao.grupo;
            }
            int h = (alturaDasTeclas(l.acao) + 2 * FOLGA_DA_LINHA) * s;
            l.caixa.setBounds(x, y[l.coluna], largura, h);
            y[l.coluna] += h;
        }
        alturaDaLista = Math.max(y[0], y[1]) - area.y;
        rolagem = Math.max(0, Math.min(rolagem, alturaDaLista - area.height));
    }

    private void posicionarControle() {
        int s = escalaControle;
        Map<Comando, Integer> alturas = new EnumMap<>(Comando.class);
        int rotulo = 0;
        for (Comando c : DesenhoDoControle.PECAS.keySet()) {
            if (!semAcao.contains(c)) {
                alturas.put(c, alturaDoRotulo(c) * s);
                if (DesenhoDoControle.PECAS.get(c).lado().ordinal() <= DesenhoDoControle.Lado.DIREITA.ordinal()) {
                    rotulo = Math.max(rotulo, larguraDoRotulo(c, s, nosMenus));
                }
            }
        }
        // O CONTROLE EM DESTAQUE: o desenho cresce até o maior fator inteiro que
        // caiba entre as duas colunas de rótulos, até o dobro da escala do texto.
        // Na escala do texto ele ficava do tamanho de um ícone no meio do painel.
        int lateral = (4 + FOLGA_DA_LINHA_DO_CONTROLE) * s + rotulo;
        int alturaUtil = area.height - ALTURA_DA_LEGENDA * s;
        int d = s;
        while (d < 2 * s && DesenhoDoControle.LARGURA * (d + 1) + 2 * lateral <= area.width
                && alturaDoBloco(nosMenus, s, d + 1) <= alturaUtil) {
            d++;
        }
        escalaDoDesenho = d;
        // Seletor, fileiras, desenho e colunas formam um bloco só, centrado na altura.
        int cima = LigacoesDoControle.fileira(alturas, DesenhoDoControle.Lado.CIMA);
        int baixo = LigacoesDoControle.fileira(alturas, DesenhoDoControle.Lado.BAIXO);
        int vao = VAO_ENTRE_ROTULOS * s;
        int bloco = Math.min(alturaUtil, alturaDoBloco(nosMenus, s, d));
        yDaLegenda = area.y + Math.max(0, (area.height - ALTURA_DA_LEGENDA * s - bloco) / 2);
        int topo = yDaLegenda + ALTURA_DA_LEGENDA * s;
        int base = topo + bloco;
        int mioloTopo = topo + (cima > 0 ? cima + vao : 0);
        int miolo = base - (baixo > 0 ? baixo + vao : 0) - mioloTopo;
        desenhoX = area.x + (area.width - DesenhoDoControle.LARGURA * d) / 2;
        desenhoY = mioloTopo + Math.max(0, (miolo - DesenhoDoControle.ALTURA * d) / 2);
        ligacoes = LigacoesDoControle.ligar(desenhoX, desenhoY, d, alturas,
                FOLGA_DA_LINHA_DO_CONTROLE * s, VAO_ENTRE_ROTULOS * s, topo, base);
        ordemDoControle.clear();
        for (boolean esquerda : new boolean[] {true, false}) {
            ligacoes.values().stream().filter(l -> l.aEsquerda() == esquerda)
                    .sorted(java.util.Comparator.comparingInt(LigacoesDoControle.Ligacao::topo))
                    .forEach(l -> ordemDoControle.add(l.comando()));
        }
        // O seletor, centrado no alto do bloco: duas metades do tamanho do texto mais largo.
        Font f = FontHandler.font(FontHandler.Game, 6f * s);
        int metade = 0;
        for (String c : CONTEXTOS) {
            metade = Math.max(metade, FontHandler.getWidth(c, f) + 2 * FOLGA_DO_SELETOR * s);
        }
        int x = area.x + area.width / 2 - metade;
        for (int i = 0; i < segmentos.length; i++) {
            segmentos[i].setBounds(x + i * metade, yDaLegenda, metade, ALTURA_DO_SELETOR * s);
        }
    }

    // ----------------------------------------------------------- conteúdo

    /** O que uma linha do teclado mostra, da esquerda para a direita. */
    private sealed interface Parte permits Tecla, Cruz, Separador {
        int largura();

        int altura();
    }

    private record Tecla(Glyph glyph) implements Parte {
        public int largura() {
            return Glyphs.largura(glyph);
        }

        public int altura() {
            return Glyphs.altura(glyph);
        }
    }

    /** Quatro teclas em cruz: a de cima sobre a do meio, as três de baixo lado a lado. */
    private record Cruz(Glyph cima, Glyph esquerda, Glyph baixo, Glyph direita) implements Parte {
        public int largura() {
            return Glyphs.largura(esquerda) + Glyphs.largura(baixo) + Glyphs.largura(direita) + 2 * VAO_ENTRE_TECLAS;
        }

        public int altura() {
            return Glyphs.altura(cima) + Glyphs.altura(baixo) + VAO_ENTRE_TECLAS;
        }
    }

    private record Separador(int largura) implements Parte {
        public int altura() {
            return 0;
        }
    }

    private static List<Parte> partes(Acao a) {
        List<Parte> saida = new ArrayList<>();
        List<Integer> codigos = new ArrayList<>(a.codigosDeTecla());
        int[] wasd = {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D};
        int[] setas = {KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT};
        // EM CRUZ SÓ QUANDO HÁ AS DUAS: o movimento. As setas sozinhas (Navigate)
        // ficam em fila, que cabe numa linha e lê do mesmo jeito.
        if (contem(codigos, wasd) && contem(codigos, setas)) {
            saida.add(cruz(wasd));
            saida.add(new Separador(VAO_ANTES_DO_MOUSE));
            saida.add(cruz(setas));
            for (int c : wasd) {
                codigos.remove(Integer.valueOf(c));
            }
            for (int c : setas) {
                codigos.remove(Integer.valueOf(c));
            }
        }
        for (int c : codigos) {
            Glyph g = Glyphs.daTecla(c);
            if (g != null) {
                saida.add(new Tecla(g));
            }
        }
        boolean primeiro = true;
        for (Acao.Mouse m : a.mouse()) {
            if (primeiro && !saida.isEmpty()) {
                saida.add(new Separador(VAO_ANTES_DO_MOUSE));
            }
            primeiro = false;
            saida.add(new Tecla(Glyphs.doMouse(m)));
        }
        return saida;
    }

    private static boolean contem(List<Integer> codigos, int[] todos) {
        for (int c : todos) {
            if (!codigos.contains(c)) {
                return false;
            }
        }
        return true;
    }

    private static Cruz cruz(int[] codigos) {
        return new Cruz(Glyphs.daTecla(codigos[0]), Glyphs.daTecla(codigos[1]),
                Glyphs.daTecla(codigos[2]), Glyphs.daTecla(codigos[3]));
    }

    private static int alturaDasTeclas(Acao a) {
        int h = 0;
        for (Parte p : partes(a)) {
            h = Math.max(h, p.altura());
        }
        return Math.max(h, 8);
    }

    /** A altura do rótulo de uma peça: o botão e uma linha de texto. */
    private static int alturaDoRotulo(Comando c) {
        return Math.max(Glyphs.altura(Glyphs.doComando(c)), 7);
    }

    /** O que a peça faz no contexto: a primeira ação dela na corrida, ou nos menus; nulo se nada. */
    static Acao acaoNoContexto(Comando c, boolean menus) {
        for (Acao a : Acao.doComando(c)) {
            if (a.grupo.emJogo != menus) {
                return a;
            }
        }
        return null;
    }

    private Acao principal(Comando c) {
        return acaoNoContexto(c, nosMenus);
    }

    // ----------------------------------------------------------- tick

    /** @param focado o foco do Settings está neste painel */
    public void tick(boolean focado) {
        if (!focado) {
            foco = -1;
            focoNoSeletor = false;
        }
        int antes = sobMouse;
        sobMouse = -1;
        if (Entrada.porMouse() && area.contains(Mouse.getX(), Mouse.getY())) {
            sobMouse = itemSobOMouse();
        }
        if (sobMouse >= 0 && sobMouse != antes) {
            Sound.play(Sounds.Hover);
        }
        if (sobMouse >= 0) {
            Engine.window.pointing();
        }
        if (visao == Visao.CONTROLE) {
            tickDoSeletor();
        }
        if (visao == Visao.TECLADO) {
            int roda = Mouse.Scroll();
            if (roda != 0 && area.contains(Mouse.getX(), Mouse.getY())) {
                rolar(roda * (12 * escalaTeclado));
            }
            for (int i = 0; i < linhas.size(); i++) {
                linhas.get(i).realce.tick(i == foco || i == sobMouse);
            }
        } else {
            Comando aceso = pecaAcesa();
            for (Map.Entry<Comando, Realce.Estado> e : realceDaPeca.entrySet()) {
                e.getValue().tick(e.getKey() == aceso);
            }
        }
    }

    /** O seletor de contexto: o mouse em cima acende a metade, o clique escolhe. */
    private void tickDoSeletor() {
        int antes = segmentoSobMouse;
        segmentoSobMouse = -1;
        for (int i = 0; i < segmentos.length; i++) {
            if (Entrada.porMouse() && segmentos[i].contains(Mouse.getX(), Mouse.getY())) {
                segmentoSobMouse = i;
                Engine.window.pointing();
                if (Mouse.clickOn(Mouse_Button.LEFT, segmentos[i])) {
                    mudarContexto(i == 1);
                }
            }
        }
        if (segmentoSobMouse >= 0 && segmentoSobMouse != antes && (segmentoSobMouse == 1) != nosMenus) {
            Sound.play(Sounds.Hover);
        }
        for (int i = 0; i < segmentos.length; i++) {
            realceDoSegmento[i].tick(i == segmentoSobMouse && (i == 1) != nosMenus);
        }
    }

    private int itemSobOMouse() {
        int mx = Mouse.getX(), my = Mouse.getY();
        if (visao == Visao.TECLADO) {
            for (int i = 0; i < linhas.size(); i++) {
                Rectangle r = linhas.get(i).caixa;
                if (r.contains(mx, my + rolagem)) {
                    return i;
                }
            }
            return -1;
        }
        for (int i = 0; i < ordemDoControle.size(); i++) {
            Comando c = ordemDoControle.get(i);
            Rectangle peca = DesenhoDoControle.PECAS.get(c).area();
            int d = escalaDoDesenho;
            Rectangle naTela = new Rectangle(desenhoX + peca.x * d, desenhoY + peca.y * d, peca.width * d, peca.height * d);
            if (naTela.contains(mx, my) || caixaDoRotulo(ligacoes.get(c)).contains(mx, my)) {
                return i;
            }
        }
        return -1;
    }

    /** A peça acesa no desenho: a do foco, ou a do mouse. */
    private Comando pecaAcesa() {
        int i = foco >= 0 ? foco : sobMouse;
        return visao == Visao.CONTROLE && i >= 0 && i < ordemDoControle.size() ? ordemDoControle.get(i) : null;
    }

    private void rolar(int dy) {
        rolagem = Math.max(0, Math.min(alturaDaLista - area.height, rolagem + dy));
    }

    // ----------------------------------------------------------- foco

    /** O foco entra no painel, pelo primeiro item (de cima) ou pelo último (de baixo). */
    public void entrar(boolean deCima) {
        int n = quantos();
        foco = n == 0 ? -1 : deCima ? 0 : n - 1;
        focoNoSeletor = false;
        if (deCima && visao == Visao.CONTROLE) {
            // De cima, o primeiro é o seletor de contexto, que fica acima do desenho.
            foco = -1;
            focoNoSeletor = true;
        }
        mostrarFoco();
    }

    private int quantos() {
        return visao == Visao.TECLADO ? linhas.size() : ordemDoControle.size();
    }

    /**
     * As setas com o foco aqui dentro.
     *
     * @return -1 para o foco sair por cima, 1 por baixo, 0 se ficou no painel
     */
    public int navegar() {
        if (foco < 0 && !focoNoSeletor) {
            entrar(true);
        }
        if (focoNoSeletor) {
            return navegarNoSeletor();
        }
        int antes = foco;
        int saida = 0;
        if (KeyBoard.KeyPressed("Down")) {
            int prox = vizinhoNaColuna(1);
            if (prox < 0) {
                saida = 1;
            } else {
                foco = prox;
            }
        } else if (KeyBoard.KeyPressed("Up")) {
            int prox = vizinhoNaColuna(-1);
            if (prox >= 0) {
                foco = prox;
            } else if (visao == Visao.CONTROLE) {
                foco = -1;
                focoNoSeletor = true;      // do alto de uma coluna, sobe para o seletor
                Sound.play(Sounds.Hover);
                return 0;
            } else {
                saida = -1;
            }
        } else if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("Left")) {
            foco = naOutraColuna();
        }
        if (saida != 0) {
            foco = -1;
            return saida;
        }
        if (foco != antes) {
            Sound.play(Sounds.Hover);
            mostrarFoco();
        }
        return 0;
    }

    /** No seletor: esquerda e direita (ou Enter) trocam o contexto; para baixo entra nos rótulos. */
    private int navegarNoSeletor() {
        if (KeyBoard.KeyPressed("Up")) {
            focoNoSeletor = false;
            return -1;
        }
        if (KeyBoard.KeyPressed("Down")) {
            focoNoSeletor = false;
            foco = ordemDoControle.isEmpty() ? -1 : 0;
            if (foco < 0) {
                return 1;
            }
            Sound.play(Sounds.Hover);
        } else if (KeyBoard.KeyPressed("Left")) {
            mudarContexto(false);
        } else if (KeyBoard.KeyPressed("Right")) {
            mudarContexto(true);
        } else if (KeyBoard.KeyPressed("Enter")) {
            mudarContexto(!nosMenus);
        }
        return 0;
    }

    private int coluna(int i) {
        if (visao == Visao.TECLADO) {
            return linhas.get(i).coluna;
        }
        return ligacoes.get(ordemDoControle.get(i)).aEsquerda() ? 0 : 1;
    }

    private int vizinhoNaColuna(int passo) {
        int c = coluna(foco);
        for (int i = foco + passo; i >= 0 && i < quantos(); i += passo) {
            if (coluna(i) == c) {
                return i;
            }
        }
        return -1;
    }

    /** O item da outra coluna mais perto da altura do foco. */
    private int naOutraColuna() {
        int c = coluna(foco), y = centroY(foco);
        int melhor = foco, distancia = Integer.MAX_VALUE;
        for (int i = 0; i < quantos(); i++) {
            if (coluna(i) != c && Math.abs(centroY(i) - y) < distancia) {
                distancia = Math.abs(centroY(i) - y);
                melhor = i;
            }
        }
        return melhor;
    }

    private int centroY(int i) {
        return visao == Visao.TECLADO ? linhas.get(i).caixa.y + linhas.get(i).caixa.height / 2
                : ligacoes.get(ordemDoControle.get(i)).centro();
    }

    /** Rola a lista do teclado até o foco aparecer inteiro. */
    private void mostrarFoco() {
        if (visao != Visao.TECLADO || foco < 0) {
            return;
        }
        Rectangle r = linhas.get(foco).caixa;
        if (r.y - rolagem < area.y) {
            rolar(r.y - rolagem - area.y);
        } else if (r.y + r.height - rolagem > area.y + area.height) {
            rolar(r.y + r.height - rolagem - area.y - area.height);
        }
    }

    // ----------------------------------------------------------- ajuda

    /** A frase da ação sob o foco ou o mouse, para o balão de ajuda do Settings; nulo se nenhuma. */
    public String descricao() {
        Acao a = acaoAtual();
        return a == null ? null : a.descricao;
    }

    /** Onde ancorar o balão: a linha ou o rótulo do item atual. */
    public Rectangle alvoDaDescricao() {
        int i = sobMouse >= 0 ? sobMouse : foco;
        if (i < 0) {
            return null;
        }
        if (visao == Visao.TECLADO) {
            Rectangle r = new Rectangle(linhas.get(i).caixa);
            r.translate(0, -rolagem);
            return r;
        }
        return caixaDoRotulo(ligacoes.get(ordemDoControle.get(i)));
    }

    public boolean ajudaPeloMouse() {
        return sobMouse >= 0;
    }

    private Acao acaoAtual() {
        int i = sobMouse >= 0 ? sobMouse : foco;
        if (i < 0 || i >= quantos()) {
            return null;
        }
        return visao == Visao.TECLADO ? linhas.get(i).acao : principal(ordemDoControle.get(i));
    }

    // ----------------------------------------------------------- desenho

    public void render(Graphics2D g) {
        if (visao == Visao.TECLADO) {
            renderTeclado(g);
        } else {
            renderControle(g);
        }
    }

    private void renderTeclado(Graphics2D g) {
        int s = escala();
        Graphics2D c = (Graphics2D) g.create();
        c.clipRect(area.x - 8 * s, area.y, area.width + 16 * s, area.height);
        c.translate(0, -rolagem);
        Font grupo = FontHandler.font(FontHandler.Game, 5f * s);
        for (Cabecalho h : cabecalhos) {
            c.setFont(grupo);
            int base = h.y() + ALTURA_DO_GRUPO * s - 3 * s;
            TextFx.draw(c, h.texto(), h.x(), base, Palette.LIGHT, s);
            int fim = h.x() + FontHandler.getWidth(h.texto(), grupo) + 4 * s;
            c.setColor(Palette.OUTLINE);
            c.fillRect(fim, base - 2 * s, h.x() + h.largura() - fim, s);
        }
        Font fonte = FontHandler.font(FontHandler.Game, 6f * s);
        for (int i = 0; i < linhas.size(); i++) {
            Linha l = linhas.get(i);
            float intensidade = l.realce.noQuadro();
            Rectangle faixa = new Rectangle(l.caixa.x - 2 * s, l.caixa.y, l.caixa.width + 4 * s, l.caixa.height);
            Realce.clarear(c, faixa, 0.8f * intensidade);
            if (i == foco && Entrada.porFoco()) {
                Realce.marcadorDeFoco(c, faixa, s);
            }
            boolean aceso = i == foco || intensidade > 0.5f;
            c.setFont(fonte);
            int base = l.caixa.y + (l.caixa.height + FontHandler.getHeight(l.acao.rotulo, fonte)) / 2 - s;
            TextFx.draw(c, l.acao.rotulo, l.caixa.x, base, aceso ? Palette.TEXT : TextFx.CLARO, s);
            desenharPartes(c, partes(l.acao), l.caixa.x + l.caixa.width, l.caixa.y + l.caixa.height / 2, s);
        }
        c.dispose();
        if (alturaDaLista > area.height) {
            desenharRolagem(g, s);
        }
    }

    /** As teclas de uma linha, alinhadas pela direita em {@code direita} e centradas em {@code cy}. */
    private static void desenharPartes(Graphics2D g, List<Parte> partes, int direita, int cy, int s) {
        int x = direita;
        for (int i = partes.size() - 1; i >= 0; i--) {
            Parte p = partes.get(i);
            x -= p.largura() * s;
            switch (p) {
                case Tecla t -> Glyphs.desenhar(g, t.glyph(), x, cy - t.altura() * s / 2, s);
                case Cruz z -> {
                    int topo = cy - z.altura() * s / 2;
                    int baixo = topo + (Glyphs.altura(z.cima()) + VAO_ENTRE_TECLAS) * s;
                    int xMeio = x + (Glyphs.largura(z.esquerda()) + VAO_ENTRE_TECLAS) * s;
                    Glyphs.desenhar(g, z.cima(), xMeio, topo, s);
                    Glyphs.desenhar(g, z.esquerda(), x, baixo, s);
                    Glyphs.desenhar(g, z.baixo(), xMeio, baixo, s);
                    Glyphs.desenhar(g, z.direita(), xMeio + (Glyphs.largura(z.baixo()) + VAO_ENTRE_TECLAS) * s, baixo, s);
                }
                case Separador sep -> {
                }
            }
            if (i > 0 && !(p instanceof Separador) && !(partes.get(i - 1) instanceof Separador)) {
                x -= VAO_ENTRE_TECLAS * s;
            }
        }
    }

    /** Uma trilha fina à direita, só quando a lista não cabe: diz que há mais. */
    private void desenharRolagem(Graphics2D g, int s) {
        int x = area.x + area.width + 5 * s;
        g.setColor(Palette.OUTLINE);
        g.fillRect(x, area.y, s, area.height);
        int alca = Math.max(6 * s, area.height * area.height / alturaDaLista);
        int y = area.y + (area.height - alca) * rolagem / Math.max(1, alturaDaLista - area.height);
        g.setColor(Palette.LIGHT);
        g.fillRect(x, y, s, alca);
    }

    private void renderControle(Graphics2D g) {
        int s = escala();
        desenharLegenda(g, s);
        Comando aceso = pecaAcesa();
        if (aceso != null) {
            Rectangle p = DesenhoDoControle.PECAS.get(aceso).area();
            int d = escalaDoDesenho;
            Realce.brilho(g, new Rectangle(desenhoX + p.x * d, desenhoY + p.y * d, p.width * d, p.height * d),
                    realceDaPeca.get(aceso).noQuadro(), s);
        }
        g.drawImage(DesenhoDoControle.imagem(escalaDoDesenho, aceso, semAcao), desenhoX, desenhoY, null);
        // Primeiro todas as linhas, depois os rótulos: a acesa por cima das outras.
        for (LigacoesDoControle.Ligacao l : ligacoes.values()) {
            if (l.comando() != aceso) {
                desenharLinha(g, l, Palette.LIGHT, s);
            }
        }
        if (aceso != null) {
            desenharLinha(g, ligacoes.get(aceso), Palette.TEXT, s);
        }
        Font fonte = FontHandler.font(FontHandler.Game, 6f * s);
        for (int i = 0; i < ordemDoControle.size(); i++) {
            Comando c = ordemDoControle.get(i);
            LigacoesDoControle.Ligacao l = ligacoes.get(c);
            float intensidade = realceDaPeca.get(c).noQuadro();
            Rectangle caixa = caixaDoRotulo(l);
            Realce.clarear(g, new Rectangle(caixa.x - s, caixa.y - s, caixa.width + 2 * s, caixa.height + 2 * s),
                    0.8f * intensidade);
            if (i == foco && Entrada.porFoco()) {
                Realce.marcadorDeFoco(g, new Rectangle(caixa.x - s, caixa.y, caixa.width + 2 * s, caixa.height), s);
            }
            desenharRotulo(g, c, l, caixa, fonte, c == aceso, s);
        }
    }

    /**
     * O seletor de contexto, no desenho das abas do Settings: fundo de contorno, a
     * metade escolhida no tom fundo com o fio claro no alto. Pelo foco, as setas do
     * Realce em volta do seletor inteiro.
     */
    private void desenharLegenda(Graphics2D g, int s) {
        Font f = FontHandler.font(FontHandler.Game, 6f * s);
        Rectangle tudo = segmentos[0].union(segmentos[1]);
        g.setColor(Palette.OUTLINE);
        g.fillRect(tudo.x, tudo.y, tudo.width, tudo.height);
        g.setFont(f);
        for (int i = 0; i < segmentos.length; i++) {
            Rectangle r = segmentos[i];
            boolean escolhido = (i == 1) == nosMenus;
            if (escolhido) {
                g.setColor(Palette.DEEP);
                g.fillRect(r.x + s, r.y + s, r.width - 2 * s, r.height - 2 * s);
                g.setColor(Palette.LIGHT);
                g.fillRect(r.x + s, r.y + s, r.width - 2 * s, s);
            } else {
                Realce.clarear(g, new Rectangle(r.x + s, r.y + s, r.width - 2 * s, r.height - 2 * s),
                        0.8f * realceDoSegmento[i].noQuadro());
            }
            String texto = CONTEXTOS[i];
            int w = FontHandler.getWidth(texto, f), h = FontHandler.getHeight(texto, f);
            TextFx.draw(g, texto, r.x + (r.width - w) / 2, r.y + (r.height + h) / 2,
                    escolhido ? Palette.TEXT : Palette.LIGHT, s);
        }
        if (focoNoSeletor && Entrada.porFoco()) {
            Realce.marcadorDeFoco(g, tudo, s);
        }
    }

    /** A linha em pixels inteiros: a reta da peça até a borda, e a diagonal em degraus até o rótulo. */
    private static void desenharLinha(Graphics2D g, LigacoesDoControle.Ligacao l, Color cor, int s) {
        g.setColor(cor);
        Point a = l.ancora(), d = l.dobra(), p = l.ponta();
        if (a.y == d.y) {
            g.fillRect(Math.min(a.x, d.x), a.y - s / 2, Math.abs(d.x - a.x) + s, s);
        } else {
            g.fillRect(a.x - s / 2, Math.min(a.y, d.y), s, Math.abs(d.y - a.y) + s);   // reta para cima ou para baixo
        }
        int passos = Math.max(Math.abs(p.x - d.x), Math.abs(p.y - d.y)) / s;
        for (int i = 0; i <= passos; i++) {
            double t = passos == 0 ? 1 : i / (double) passos;
            int x = (int) Math.round((d.x + (p.x - d.x) * t) / s) * s;
            int y = (int) Math.round((d.y + (p.y - d.y) * t) / s) * s;
            g.fillRect(x, y - s / 2, s, s);
        }
    }

    /** A caixa do rótulo, encostada na ponta da linha: à esquerda, termina nela; à direita, começa nela. */
    private Rectangle caixaDoRotulo(LigacoesDoControle.Ligacao l) {
        int s = escalaControle;
        int w = larguraDoRotulo(l.comando(), s, nosMenus);
        int x = l.aEsquerda() ? l.ponta().x - 2 * s - w : l.ponta().x + 2 * s;
        return new Rectangle(x, l.topo(), w, l.altura());
    }

    private static int larguraDoRotulo(Comando c, int s, boolean menus) {
        Font fonte = FontHandler.font(FontHandler.Game, 6f * s);
        Acao p = acaoNoContexto(c, menus);
        int texto = p == null ? 0 : FontHandler.getWidth(p.rotulo, fonte);
        return Glyphs.largura(Glyphs.doComando(c)) * s + 3 * s + texto;
    }

    /** O botão e o texto: à esquerda, o botão fica do lado da linha; à direita também. */
    private void desenharRotulo(Graphics2D g, Comando c, LigacoesDoControle.Ligacao l, Rectangle caixa,
                                Font fonte, boolean aceso, int s) {
        Glyph glyph = Glyphs.doComando(c);
        int gw = Glyphs.largura(glyph) * s, gh = Glyphs.altura(glyph) * s;
        Acao p = principal(c);
        int xGlyph = l.aEsquerda() ? caixa.x + caixa.width - gw : caixa.x;
        Glyphs.desenhar(g, glyph, xGlyph, caixa.y + (caixa.height - gh) / 2, s);
        if (p != null) {
            int larg = FontHandler.getWidth(p.rotulo, fonte);
            int x = l.aEsquerda() ? xGlyph - 3 * s - larg : xGlyph + gw + 3 * s;
            g.setFont(fonte);
            TextFx.draw(g, p.rotulo, x, caixa.y + (caixa.height + 6 * s) / 2 - s, aceso ? Palette.TEXT : TextFx.CLARO, s);
        }
    }
}
