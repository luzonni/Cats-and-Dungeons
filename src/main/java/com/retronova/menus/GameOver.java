package com.retronova.menus;

import com.retronova.menus.shared.Realce;
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

import java.awt.Color;
import java.awt.Font;
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

    // Geometria em pixels nativos; tudo é multiplicado pela escala da tela.
    /** Largura e altura de cada painel. */
    private static final int PAINEL_W = 150, PAINEL_H = 154;
    /**
     * As linhas da loja: onde começam, a altura e o passo entre elas. 18 de altura
     * e 3 de vão, e não 17 e 3 com o painel de 140: o nome encostava no topo da
     * linha, o efeito na base, e a dica do rodapé do painel colava na última. A
     * dica vai em duas linhas: numa só, passava da largura do painel.
     */
    private static final int LOJA_TOPO = 33, LINHA_H = 18, LINHA_PASSO = 21;
    /** Altura do cabeçalho de cada painel, a faixa como a aba ativa. */
    private static final int CABECALHO = 13;
    /** O fundo: preto, sem a ilustração do título, para os números serem o centro. */
    private static final Color FUNDO = Color.BLACK;
    /** Ticks que cada aviso de conquista fica parado antes de sair (4 s). */
    private static final int AVISO_FICA = 240;

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
        // O título no branco de toda tela de menu: o vermelho e o ouro próprios
        // eram as únicas cores quentes da tela, fora da Palette.
        super(Estatisticas.venceu() ? "Victory" : "Run Over");
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
        int cabe = Math.max(1, areaDoConteudo().height / (PAINEL_H + 32));   // painel, vão e botões
        return Math.max(1, Math.min(Configs.UiScale(), cabe));
    }

    private Rectangle painelResumo, painelLoja;

    @Override
    protected void posicionar() {
        int s = escala();
        int w = Engine.window.largura();
        Rectangle area = areaDoConteudo();
        int pw = PAINEL_W * s, ph = PAINEL_H * s, vao = 10 * s;
        int total = ph + 10 * s + Button.preferredHeight();
        int x0 = w / 2 - pw - vao / 2;
        int y0 = area.y + Math.max(0, (area.height - total) / 2);
        painelResumo = new Rectangle(x0, y0, pw, ph);
        painelLoja = new Rectangle(w / 2 + vao / 2, y0, pw, ph);
        linhasDaLoja.clear();
        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; i < todos.length; i++) {
            linhasDaLoja.add(new Rectangle(painelLoja.x + 6 * s, painelLoja.y + (LOJA_TOPO + i * LINHA_PASSO) * s,
                    painelLoja.width - 12 * s, LINHA_H * s));
        }
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int by = y0 + ph + 10 * s;
        int vaoBotoes = Realce.folgaEntreVizinhos(Configs.UiScale());
        int bx = w / 2 - largura - vaoBotoes / 2;
        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(bx + i * (largura + vaoBotoes), by, largura, altura));
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
     * Entram deslizando pela direita, um depois do outro, e SAEM depois de
     * {@link #AVISO_FICA} ticks. Ficavam para sempre, e em 720p cobriam o ouro
     * ganho e o cabeçalho da loja. Mostram só o nome: o que ela pede e o resto
     * está na tela de conquistas, que o jogador abre pelo troféu do menu.
     */
    private void desenharConquistas(Graphics2D g, int s) {
        List<Conquistas.Conquista> novas = Estatisticas.conquistasDaCorrida();
        if (novas.isEmpty()) {
            return;
        }
        int h = 22 * s, vao = 4 * s;
        int margem = Configs.Margin();
        int escala = Math.max(1, s);
        int lado = com.retronova.game.items.Icone.lado(escala);
        Font rotulo = FontHandler.font(FontHandler.Septem, 5f * s);
        // A LARGURA VEM DO MAIOR NOME, e não de um valor fixo: com 96 pixels, nomes
        // como "Treasure Hunter" passavam da borda do aviso. Todos os avisos ficam
        // com a mesma largura, para a pilha não ficar serrilhada; se nem a janela
        // inteira couber, a fonte do nome desce um ponto por vez.
        int maxW = Engine.window.largura() - 2 * margem;
        int fixo = 4 * s + lado + 4 * s + 6 * s;
        float tamNome = 7f;
        Font fonteNome = FontHandler.font(FontHandler.Game, tamNome * s);
        int texto;
        while (true) {
            texto = FontHandler.getWidth("Achievement unlocked", rotulo);
            for (Conquistas.Conquista c : novas) {
                texto = Math.max(texto, FontHandler.getWidth(c.nome(), fonteNome));
            }
            if (fixo + texto <= maxW || tamNome <= 4f) {
                break;
            }
            tamNome -= 1f;
            fonteNome = FontHandler.font(FontHandler.Game, tamNome * s);
        }
        int w = Math.min(maxW, Math.max(96 * s, fixo + texto));
        for (int i = 0; i < novas.size(); i++) {
            // 0 a 1: cada aviso começa 14 ticks depois do anterior e leva 18 para entrar.
            double agora = desdeOFim + Engine.alpha() - 10 - i * 14;
            double t = Math.clamp(agora / 18.0, 0, 1);
            double saida = Math.clamp((agora - AVISO_FICA) / 18.0, 0, 1);
            if (t <= 0 || saida >= 1) {
                continue;
            }
            double suave = (1 - Math.pow(1 - t, 3)) * (1 - saida * saida * saida);
            int x = (int) (Engine.window.largura() - margem - w + (1 - suave) * (w + margem));
            int y = margem + i * (h + vao);

            // A caixa das dicas de Settings: contorno, corpo DEEP e fio MAIN no topo.
            // Era dourada, arredondada e em gradiente, fora da Palette.
            g.setColor(Palette.OUTLINE);
            g.fillRect(x, y, w, h);
            g.setColor(Palette.DEEP);
            g.fillRect(x + s, y + s, w - 2 * s, h - 2 * s);
            g.setColor(Palette.MAIN);
            g.fillRect(x + s, y + s, w - 2 * s, s);

            com.retronova.game.items.Icone.TROFEU.desenhar(g, x + 4 * s, y + (h - lado) / 2, escala);

            int tx = x + 4 * s + lado + 4 * s;
            TextFx.draw(g, "Achievement unlocked", rotulo, tx, y + 8 * s, Palette.LIGHT, s);
            TextFx.draw(g, novas.get(i).nome(), fonteNome, tx, y + 17 * s, Palette.TEXT, s);
        }
    }

    /**
     * O painel no padrão dos menus (ver TelaDeMenu.desenharPainel e Abas): contorno
     * OUTLINE, corpo DEEP e o cabeçalho como a aba ativa, faixa MAIN com fio LIGHT.
     *
     * ERA UM SISTEMA À PARTE: cabeçalhos ciano e amarelo, corpo em gradiente roxo,
     * cantos arredondados e sombra, nenhuma cor da Palette. Ao lado das Settings e
     * da pausa, a tela parecia de outro jogo; e os cantos e o gradiente passavam
     * pelo Marlin na CPU a cada quadro.
     */
    private void painel(Graphics2D g, Rectangle p, String titulo, int s) {
        g.setColor(Palette.OUTLINE);
        g.fillRect(p.x, p.y, p.width, p.height);
        g.setColor(Palette.DEEP);
        g.fillRect(p.x + s, p.y + s, p.width - 2 * s, p.height - 2 * s);
        g.setColor(Palette.MAIN);
        g.fillRect(p.x + s, p.y + s, p.width - 2 * s, CABECALHO * s);
        g.setColor(Palette.LIGHT);
        g.fillRect(p.x + s, p.y + s, p.width - 2 * s, s);
        g.setColor(Palette.OUTLINE);
        g.fillRect(p.x + s, p.y + (CABECALHO + 1) * s, p.width - 2 * s, s);
        Font f = FontHandler.font(FontHandler.Game, 7f * s);
        contornadoCentro(g, titulo, f, p.x + p.width / 2, p.y + 10 * s, Palette.TEXT, s);
    }

    private void desenharResumo(Graphics2D g, int s) {
        Rectangle p = painelResumo;
        painel(g, p, "Run summary", s);
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
            contornado(g, l[0], f, p.x + 8 * s, y, Palette.ACCENT, s);
            int vw = FontHandler.getWidth(l[1], valor);
            contornado(g, l[1], valor, p.x + p.width - 8 * s - vw, y, Palette.TEXT, s);
            y += 10 * s;
        }
        String habilidade = Bonus.habilidade(gato);
        if (!habilidade.isEmpty()) {
            contornadoCentro(g, habilidade, f, p.x + p.width / 2, y, Palette.ACCENT, s);
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
            // A barra no padrão das Settings: contorno, miolo fundo e o trecho cheio,
            // a maior arma mais clara que as outras.
            g.setColor(Palette.OUTLINE);
            g.fillRect(p.x + 8 * s, y, p.width - 16 * s, 7 * s);
            g.setColor(i == 0 ? Palette.LIGHT : Palette.MAIN);
            g.fillRect(p.x + 9 * s, y + s, Math.max(0, bw - 2 * s), 5 * s);
            String rot = e.getKey() + "  " + Math.round(e.getValue());
            contornado(g, rot, pequena, p.x + 10 * s, y + 6 * s, Palette.TEXT, s);
            y += 9 * s;
        }
    }

    private void desenharLoja(Graphics2D g, int s) {
        Rectangle p = painelLoja;
        painel(g, p, "Upgrades", s);
        Font f = FontHandler.font(FontHandler.Septem, 6f * s);
        Font grande = FontHandler.font(FontHandler.Game, 8f * s);
        String ouro = "Gold: " + Meta.ouro();
        String ganho = "+" + Estatisticas.ouroGanho() + " this run";
        contornado(g, ouro, grande, p.x + 8 * s, p.y + 26 * s, Palette.TEXT, s);
        int gw = FontHandler.getWidth(ganho, f);
        contornado(g, ganho, f, p.x + p.width - 8 * s - gw, p.y + 26 * s, Palette.ACCENT, s);

        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; i < todos.length; i++) {
            Meta.Permanente m = todos[i];
            Rectangle r = linhasDaLoja.get(i);
            int nivel = Meta.nivel(m);
            boolean cheio = nivel >= Meta.Permanente.NIVEL_MAXIMO;
            boolean podeComprar = !cheio && Meta.ouro() >= Meta.preco(m);
            boolean sob = Entrada.porFoco() ? focoLoja == i : r.contains(Mouse.getX(), Mouse.getY());
            // O realce comum (ver Realce) marca a linha sob o mouse ou o foco, MESMO
            // SEM OURO: a cor clara diz "da para comprar", o realce diz "e aqui".
            if (sob) {
                Realce.brilho(g, r, 1f, s);
            }
            // A linha como um botão da casa: contorno, corpo MAIN (LIGHT com o mouse
            // e ouro para comprar) e o fio claro no topo.
            g.setColor(Palette.OUTLINE);
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(sob && podeComprar ? Palette.LIGHT : Palette.MAIN);
            g.fillRect(r.x + s, r.y + s, r.width - 2 * s, r.height - 2 * s);
            g.setColor(sob && podeComprar ? Palette.ACCENT : Palette.LIGHT);
            g.fillRect(r.x + s, r.y + s, r.width - 2 * s, s);
            if (sob && Entrada.porFoco()) {
                Realce.marcadorDeFoco(g, r, s);
            }
            if (brilhoDaCompra == i && desdeACompra < 20) {
                Realce.clarear(g, r, 1f - desdeACompra / 20f);
            }
            contornado(g, m.nome(), f, r.x + 5 * s, r.y + 8 * s, Palette.TEXT, s);
            contornado(g, m.efeito(), FontHandler.font(FontHandler.Septem, 5f * s), r.x + 5 * s, r.y + 15 * s,
                    Palette.ACCENT, s);
            // Pips de nivel, na altura do nome; o preço, na do efeito.
            for (int k = 0; k < Meta.Permanente.NIVEL_MAXIMO; k++) {
                g.setColor(Palette.OUTLINE);
                g.fillRect(r.x + r.width - 31 * s + k * 5 * s, r.y + 4 * s, 4 * s, 4 * s);
                if (k < nivel) {
                    g.setColor(Palette.TEXT);
                    g.fillRect(r.x + r.width - 30 * s + k * 5 * s, r.y + 5 * s, 2 * s, 2 * s);
                }
            }
            // O preço: claro se dá para comprar, o vermelho de aviso se falta ouro,
            // numa plaquinha OUTLINE: o WARN direto sobre o MAIN da linha não se lia.
            String preco = cheio ? "MAX" : Meta.preco(m) + "g";
            int pw = FontHandler.getWidth(preco, f);
            g.setColor(Palette.OUTLINE);
            g.fillRect(r.x + r.width - 8 * s - pw, r.y + 9 * s, pw + 4 * s, 8 * s);
            contornado(g, preco, f, r.x + r.width - 6 * s - pw, r.y + 15 * s,
                    cheio ? Palette.ACCENT : podeComprar ? Palette.TEXT : Palette.WARN, s);
        }
        Font dica = FontHandler.font(FontHandler.Septem, 5f * s);
        contornadoCentro(g, "Click or press Enter to buy.", dica, p.x + p.width / 2, p.y + p.height - 11 * s,
                Palette.LIGHT, s);
        contornadoCentro(g, "Upgrades apply to every new run.", dica, p.x + p.width / 2, p.y + p.height - 4 * s,
                Palette.LIGHT, s);
    }

    /** Fundo preto: nem a ilustração do título, nem o jogo congelado. */
    @Override
    protected void desenharFundo(Graphics2D g) {
        g.setColor(FUNDO);
        g.fillRect(0, 0, Engine.window.largura(), Engine.window.altura());
    }

    // ------------------------------------------------------------- texto

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
