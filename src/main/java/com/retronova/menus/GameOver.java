package com.retronova.menus;

import com.retronova.menus.shared.Realce;
import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;
import com.retronova.menus.shared.Abas;
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
import com.retronova.game.objects.entities.Player;
import com.retronova.menus.shared.Button;

import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
    /** O relatório usa três colunas fluidas; a loja ocupa as duas da direita. */
    /**
     * As duas linhas precisam de espaço para corpo, contorno e sombra da fonte.
     * A versão de 18 pixels apertava o efeito contra a borda inferior.
     */
    private static final int LOJA_TOPO = 64, LINHA_H = 36, LINHA_PASSO = 42;
    private static final int MARGEM_INTERNA = 12, RECUO_DA_LINHA = 8;
    private static final int ATRIBUTOS_TOPO = 38, ATRIBUTOS_PASSO = 16;
    private static final int DANO_TOPO = 78, DANO_PASSO = 24;
    private static final int BUILD_TOPO = 30, BUILD_PASSO = 32;
    private static final int PORTRAIT = 48;
    /** Altura do cabeçalho de cada painel, a faixa como a aba ativa. */
    private static final int CABECALHO = 20;
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
    private final ApresentacaoDoFim apresentacao = new ApresentacaoDoFim();
    private BufferedImage tituloPronto;
    private int escalaDoTitulo, baseDoTituloPronto;
    private final Abas abas = new Abas("Run report", "Final build", "Upgrades");
    private final ResumoFinalDoGato ficha;
    private final List<Map.Entry<String, Double>> danos;
    private int rolagemDanos, rolagemAtributos, rolagemArmas, rolagemMelhorias, colunaRolagem, rolagemLoja;
    private LayoutDoFim layout;

    private final int gato;
    private final int espaco;

    /** O nome do gato da corrida: as moedas e os upgrades sao dele (ver save/Progresso). */
    private String nomeDoGato() {
        return gato >= 0 && gato < com.retronova.game.objects.entities.Player.TEMPLATES.length
                ? com.retronova.game.objects.entities.Player.TEMPLATES[gato].getName() : "";
    }

    public GameOver(int gato, int espaco) {
        this(gato, espaco, Engine.getACTIVITY() instanceof Game
                ? ResumoFinalDoGato.capturar(Game.getPlayer()) : ResumoFinalDoGato.vazio());
    }

    public GameOver(int gato, int espaco, ResumoFinalDoGato ficha) {
        // Vencer o chefe final muda o titulo, mesmo que o gato tenha caido depois.
        // O título no branco de toda tela de menu: o vermelho e o ouro próprios
        // eram as únicas cores quentes da tela, fora da Palette.
        super(Estatisticas.venceu() ? "Victory" : "Run Over");
        this.gato = gato;
        this.espaco = espaco;
        this.ficha = ficha;
        danos = new ArrayList<>(Estatisticas.danoPorFonte().entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue())).toList());
        danos.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
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
        if (!apresentacao.terminou()) {
            if (apresentacao.aceitaPulo()) apresentacao.concluir();
            return;
        }
        voltarAoMenuPrincipal();
    }

    private int escala() {
        return Configs.UiScale();
    }

    private Rectangle painelResumo, painelLoja;

    @Override
    protected void posicionar() {
        int s = escala();
        int w = Engine.window.largura();
        Rectangle area = areaDoConteudo();
        layout = LayoutDoFim.calcular(w, s, area.y, area.y + area.height,
                Button.preferredHeight(), Configs.Margin());
        painelResumo = layout.personagem();
        painelLoja = layout.loja();
        abas.posicionar(layout.abas().x, layout.abas().y, layout.abas().width);
        linhasDaLoja.clear();
        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; i < todos.length; i++) {
            int larguraLinha = (painelLoja.width - (2 * MARGEM_INTERNA + LayoutDoFim.VAO) * s) / 2;
            linhasDaLoja.add(new Rectangle(painelLoja.x + MARGEM_INTERNA * s + (i / 3) * (larguraLinha + LayoutDoFim.VAO * s),
                    painelLoja.y + (LOJA_TOPO + (i % 3) * LINHA_PASSO - rolagemLoja) * s, larguraLinha, LINHA_H * s));
        }
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int by = layout.botoesY();
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
        apresentacao.tick();
        if (desdeOFim == 1 && !Estatisticas.venceu()) Sound.play(Sounds.FimDaRun);
        if (!apresentacao.terminou()) {
            // Consome AS DUAS marcas do mouse: o clique que revela não compra nada.
            boolean clique = Mouse.clickOn(Mouse_Button.LEFT,
                    new Rectangle(0, 0, Engine.window.largura(), Engine.window.altura()));
            clique |= Mouse.click(Mouse_Button.LEFT);
            boolean confirmar = KeyBoard.KeyPressed("Enter");
            if (apresentacao.aceitaPulo() && (clique || confirmar)) apresentacao.concluir();
            return;
        }
        if (abas.tick()) { foco = -1; focoLoja = -1; }
        if (abas.atual() == 2) {
            int roda = Integer.signum(Mouse.Scroll());
            if (roda != 0) {
                rolagemLoja = Math.clamp(rolagemLoja + roda * LINHA_PASSO, 0, maximoRolagemLoja());
                posicionar();
            }
        }
        if (abas.atual() != 2) {
            int passo = Integer.signum(Mouse.Scroll());
            if (KeyBoard.KeyPressed("Down")) passo = 1;
            if (KeyBoard.KeyPressed("Up")) passo = -1;
            if (passo != 0) {
                boolean centro = Entrada.porFoco() ? colunaRolagem == 0 : layout.atributos().contains(Mouse.getX(), Mouse.getY());
                if (abas.atual() == 0 && centro) rolagemAtributos = limitarRolagem(rolagemAtributos + passo, ficha.atributos().size(), layout.atributos(), ATRIBUTOS_TOPO, ATRIBUTOS_PASSO);
                else if (abas.atual() == 0) rolagemDanos = limitarRolagem(rolagemDanos + passo, danos.size(), layout.danos(), DANO_TOPO, DANO_PASSO);
                else if (centro)
                    rolagemArmas = limitarRolagem(rolagemArmas + passo, ficha.armas().size(), layout.atributos(), BUILD_TOPO, BUILD_PASSO);
                else rolagemMelhorias = limitarRolagem(rolagemMelhorias + passo, ficha.melhorias().size(), layout.danos(), BUILD_TOPO, BUILD_PASSO);
            }
        }
        // Loja: clicar numa linha compra o proximo nivel, se houver ouro.
        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; abas.atual() == 2 && i < linhasDaLoja.size(); i++) {
            Rectangle r = linhasDaLoja.get(i);
            if (areaDaLoja().contains(Mouse.getX(), Mouse.getY()) && r.contains(Mouse.getX(), Mouse.getY())) {
                Engine.window.pointing();
                if (Mouse.clickOn(Mouse_Button.LEFT, r)) {
                    comprar(i);
                }
            }
        }
        // AS SETAS ANDAM NAS DUAS PARTES DA TELA. Cima e baixo percorrem a loja,
        // e descer da ultima melhoria cai nos botoes; esquerda e direita escolhem
        // o botao. A loja era so de clique, e o controle nao chegava nela.
        if (abas.atual() == 2 && KeyBoard.KeyPressed("Up")) {
            focoLoja = focoLoja < 0 ? todos.length - 1 : Math.max(0, focoLoja - 1);
            foco = -1;
        } else if (abas.atual() == 2 && KeyBoard.KeyPressed("Down")) {
            if (focoLoja >= 0 && focoLoja < todos.length - 1) {
                focoLoja++;
            } else {
                focoLoja = -1;
                foco = Math.max(0, foco);
            }
        } else if (KeyBoard.KeyPressed("Right")) {
            focoLoja = -1;
            if (abas.atual() != 2 && foco < 0 && colunaRolagem == 0) colunaRolagem = 1;
            else foco = (foco + 1) % botoes.size();
        } else if (KeyBoard.KeyPressed("Left")) {
            focoLoja = -1;
            if (abas.atual() != 2 && foco < 0 && colunaRolagem == 1) colunaRolagem = 0;
            else if (abas.atual() != 2 && foco == 0) { foco = -1; colunaRolagem = 1; }
            else foco = (foco <= 0 ? botoes.size() : foco) - 1;
        } else if (KeyBoard.KeyPressed("Enter")) {
            if (abas.atual() == 2 && focoLoja >= 0) {
                comprar(focoLoja);
            } else if (foco >= 0) {
                botoes.get(foco).activate();
                return;
            }
        }
        if (abas.atual() == 2 && focoLoja >= 0) {
            Rectangle linha = linhasDaLoja.get(focoLoja), vista = areaDaLoja();
            if (linha.y < vista.y) rolagemLoja -= (vista.y - linha.y) / escala();
            if (linha.y + linha.height > vista.y + vista.height)
                rolagemLoja += (linha.y + linha.height - vista.y - vista.height) / escala();
            rolagemLoja = Math.clamp(rolagemLoja, 0, maximoRolagemLoja());
            posicionar();
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
        if (Meta.comprar(nomeDoGato(), Meta.Permanente.values()[linha])) {
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
        if (!apresentacao.terminou()) return List.of(new Rodape.Dica("Enter", "A", "Reveal summary"));
        return List.of(new Rodape.Dica("Q / E", "LB / RB", "Report / Build / Upgrades"),
                new Rodape.Dica("Enter", "A", "Select"),
                new Rodape.Dica("Esc", "B", "Main menu"));
    }

    @Override
    protected void desenharConteudo(Graphics2D g) {
        int s = escala();
        abas.render(g);
        if (apresentacao.terminou() && abas.atual() != 2 && Entrada.porFoco() && foco < 0)
            Realce.marcadorDeFoco(g, colunaRolagem == 0 ? layout.atributos() : layout.danos(), s);
        Graphics2D resumo = camada(g, ApresentacaoDoFim.RESUMO, ApresentacaoDoFim.PAINEL, s);
        if (resumo != null) {
            desenharResumo(resumo, s);
            resumo.dispose();
        }
        Graphics2D loja = camada(g, ApresentacaoDoFim.LOJA, ApresentacaoDoFim.PAINEL, s);
        if (loja != null) {
            if (abas.atual() == 2) desenharLoja(loja, s);
            else if (abas.atual() == 1) {
                desenharBuild(loja, layout.atributos(), "Weapons & element", ficha.armas(), rolagemArmas, s);
                desenharBuild(loja, layout.danos(), "Buffs & upgrades", ficha.melhorias(), rolagemMelhorias, s);
            } else {
                desenharAtributos(loja, s);
                desenharDanos(loja, s);
            }
            loja.dispose();
        }
        Graphics2D acoes = camada(g, ApresentacaoDoFim.OURO, ApresentacaoDoFim.BRILHO, s);
        if (acoes != null) {
            for (Button botao : botoes) botao.render(acoes);
            acoes.dispose();
        }
    }

    private double tempoDaApresentacao() { return apresentacao.tempo(Engine.alpha()); }

    private Graphics2D camada(Graphics2D g, int inicio, int duracao, int s) {
        float p = ApresentacaoDoFim.progresso(tempoDaApresentacao(), inicio, duracao);
        if (p <= 0) return null;
        Graphics2D c = (Graphics2D) g.create();
        float alfa = ((AlphaComposite) g.getComposite()).getAlpha();
        c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa * p));
        c.translate(0, Math.round((1 - p) * ApresentacaoDoFim.DESLOCAMENTO * s));
        return c;
    }

    @Override
    protected void desenharCabecalho(Graphics2D g, String texto, Color cor) {
        Graphics2D c = camada(g, 0, ApresentacaoDoFim.TITULO, Configs.UiScale());
        if (c == null) return;
        int s = Configs.UiScale();
        if (tituloPronto == null || escalaDoTitulo != s) {
            // TEXTO OPACO EM CACHE. A entrada anima a imagem; evita rerasterizar
            // glifos com alfa e deslocamento diferentes em cada quadro.
            Font fonte = FontHandler.font(FontHandler.Game, 14f * s);
            var metricas = g.getFontMetrics(fonte);
            baseDoTituloPronto = metricas.getAscent() + 2 * s;
            tituloPronto = new BufferedImage(FontHandler.getWidth(texto, fonte) + 4 * s,
                    metricas.getHeight() + 4 * s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D imagem = tituloPronto.createGraphics();
            TextFx.draw(imagem, texto, fonte, 2 * s, baseDoTituloPronto, cor, s);
            imagem.dispose();
            escalaDoTitulo = s;
        }
        // O título desce e assenta, em vez de subir junto com os painéis.
        float p = ApresentacaoDoFim.progresso(tempoDaApresentacao(), 0, ApresentacaoDoFim.TITULO);
        c.translate(0, -2 * Math.round((1 - p) * ApresentacaoDoFim.DESLOCAMENTO * Configs.UiScale()));
        c.drawImage(tituloPronto, (Engine.window.largura() - tituloPronto.getWidth()) / 2,
                baseDoTitulo() - baseDoTituloPronto, null);
        c.dispose();
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
            double agora = desdeOFim + Engine.alpha() - ApresentacaoDoFim.TOTAL - i * 14;
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
        Font f = FontHandler.font(FontHandler.Game, 8f * s);
        contornadoCentro(g, titulo, f, p.x + p.width / 2, p.y + 14 * s, Palette.TEXT, s);
    }

    /** Identidade e resultado ocupam uma coluna inteira, sem competir com a loja. */
    private void desenharResumo(Graphics2D g, int s) {
        Rectangle p = painelResumo;
        painel(g, p, "Run summary", s);
        int cx = p.x + p.width / 2;
        BufferedImage retrato = Player.TEMPLATES[gato].getSprite();
        boolean compacto = p.height / s < 190;
        int retratoNativo = compacto ? 0 : Math.min(PORTRAIT, Math.max(16, p.height / s - 190));
        int lado = retratoNativo * s;
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (!compacto) g.drawImage(retrato, cx - lado / 2, p.y + 30 * s, lado, lado, null);
        Font nome = FontHandler.font(FontHandler.Game, 12f * s);
        int baseNome = compacto ? 40 : 30 + retratoNativo + 18;
        contornadoCentro(g, Estatisticas.gato(), nome, cx, p.y + baseNome * s, Palette.TEXT, s);
        Font texto = FontHandler.font(FontHandler.Septem, 8f * s);
        int y = p.y + (baseNome + 14) * s;
        for (String trecho : compacto ? new String[0] : Bonus.habilidade(Estatisticas.gato()).split(", ", 2)) {
            contornadoCentro(g, trecho, FontHandler.font(FontHandler.Septem, 6f * s), cx, y, Palette.ACCENT, s);
            y += 10 * s;
        }
        String[][] valores = {
                {"Time", String.format("%02d:%02d", Estatisticas.segundos() / 60, Estatisticas.segundos() % 60)},
                {"Kills", String.valueOf(Estatisticas.abates())},
                {"Level", String.valueOf(Estatisticas.nivel())},
                {"Chests", String.valueOf(Estatisticas.baus())},
                {"Peak horde", String.valueOf(Estatisticas.maiorHorda())},
                {"Coins earned", "+" + Estatisticas.ouroGanho()}
        };
        // Em janelas mais baixas o retrato cede espaço aos números, sem reduzir a fonte.
        int passo = Math.min(16 * s, Math.max(10 * s, (p.y + p.height - 12 * s - y) / valores.length));
        for (String[] par : valores) {
            y += passo;
            par(g, p, par[0], par[1], y, texto, s);
        }
    }

    private void par(Graphics2D g, Rectangle p, String nome, String valor, int y, Font fonte, int s) {
        contornado(g, nome, fonte, p.x + MARGEM_INTERNA * s, y, Palette.ACCENT, s);
        int larguraNome = FontHandler.getWidth(nome, fonte);
        float tamanho = fonte.getSize2D() / s;
        while (tamanho > 6 && larguraNome + FontHandler.getWidth(valor, fonte)
                > p.width - (2 * MARGEM_INTERNA + 8) * s) {
            tamanho--;
            fonte = FontHandler.font(FontHandler.Septem, tamanho * s);
        }
        contornado(g, valor, fonte, p.x + p.width - MARGEM_INTERNA * s - FontHandler.getWidth(valor, fonte),
                y, Palette.TEXT, s);
    }

    private int capacidade(Rectangle p, int topo, int passo) {
        int fimDaLinha = passo == ATRIBUTOS_PASSO ? 3 : passo == DANO_PASSO ? 11 : 26;
        return Math.max(1, 1 + (p.height / escala() - topo - 20 - fimDaLinha) / passo);
    }

    private int limitarRolagem(int valor, int quantas, Rectangle p, int topo, int passo) {
        return Math.clamp(valor, 0, Math.max(0, quantas - capacidade(p, topo, passo)));
    }

    private void rodapeDaLista(Graphics2D g, Rectangle p, int inicio, int total, int capacidade, int s) {
        if (total <= capacidade) return;
        String dica = (inicio + 1) + "-" + Math.min(total, inicio + capacidade) + " / " + total + "   Up / Down / Scroll";
        contornadoCentro(g, dica, FontHandler.font(FontHandler.Septem, 6f * s),
                p.x + p.width / 2, p.y + p.height - 9 * s, Palette.LIGHT, s);
    }

    private void desenharAtributos(Graphics2D g, int s) {
        Rectangle p = layout.atributos();
        painel(g, p, "Final stats", s);
        Font f = FontHandler.font(FontHandler.Septem, 8f * s);
        int capacidade = capacidade(p, ATRIBUTOS_TOPO, ATRIBUTOS_PASSO);
        rolagemAtributos = limitarRolagem(rolagemAtributos, ficha.atributos().size(), p, ATRIBUTOS_TOPO, ATRIBUTOS_PASSO);
        if (ficha.atributos().isEmpty()) {
            contornado(g, "No final stats recorded.", f, p.x + MARGEM_INTERNA * s, p.y + 40 * s, Palette.LIGHT, s);
        }
        for (int i = 0; i < capacidade && i + rolagemAtributos < ficha.atributos().size(); i++) {
            var dado = ficha.atributos().get(i + rolagemAtributos);
            int y = p.y + (ATRIBUTOS_TOPO + i * ATRIBUTOS_PASSO) * s;
            par(g, p, dado.rotulo(), dado.valor(), y, f, s);
        }
        rodapeDaLista(g, p, rolagemAtributos, ficha.atributos().size(), capacidade, s);
    }

    private void desenharDanos(Graphics2D g, int s) {
        Rectangle p = layout.danos();
        painel(g, p, "Damage dealt", s);
        double total = danos.stream().mapToDouble(Map.Entry::getValue).sum();
        Font numero = FontHandler.font(FontHandler.Game, 14f * s);
        contornado(g, String.valueOf(Math.round(total)), numero, p.x + MARGEM_INTERNA * s, p.y + 45 * s, Palette.TEXT, s);
        contornado(g, "TOTAL DAMAGE", FontHandler.font(FontHandler.Septem, 6f * s),
                p.x + MARGEM_INTERNA * s, p.y + 59 * s, Palette.LIGHT, s);
        int capacidade = capacidade(p, DANO_TOPO, DANO_PASSO);
        rolagemDanos = limitarRolagem(rolagemDanos, danos.size(), p, DANO_TOPO, DANO_PASSO);
        Font fonte = FontHandler.font(FontHandler.Septem, 8f * s);
        if (danos.isEmpty()) contornado(g, "No damage recorded.", fonte,
                p.x + MARGEM_INTERNA * s, p.y + DANO_TOPO * s, Palette.LIGHT, s);
        double maior = danos.isEmpty() ? 1 : Math.max(1, danos.getFirst().getValue());
        float crescimento = ApresentacaoDoFim.progresso(tempoDaApresentacao(), ApresentacaoDoFim.BARRAS, ApresentacaoDoFim.CRESCIMENTO);
        int largura = p.width - 2 * MARGEM_INTERNA * s;
        for (int i = 0; i < capacidade && i + rolagemDanos < danos.size(); i++) {
            var dado = danos.get(i + rolagemDanos);
            int y = p.y + (DANO_TOPO + i * DANO_PASSO) * s;
            par(g, p, dado.getKey(), String.valueOf(Math.round(dado.getValue())), y, fonte, s);
            g.setColor(Palette.OUTLINE);
            g.fillRect(p.x + MARGEM_INTERNA * s, y + 5 * s, largura, 6 * s);
            g.setColor(i + rolagemDanos == 0 ? Palette.ACCENT : Palette.MAIN);
            g.fillRect(p.x + (MARGEM_INTERNA + 1) * s, y + 6 * s,
                    (int) ((largura - 2 * s) * dado.getValue() / maior * crescimento), 4 * s);
        }
        rodapeDaLista(g, p, rolagemDanos, danos.size(), capacidade, s);
    }

    private void desenharBuild(Graphics2D g, Rectangle p, String titulo,
                              List<TelaDeStatus.Linha> linhas, int inicio, int s) {
        painel(g, p, titulo, s);
        int capacidade = capacidade(p, BUILD_TOPO, BUILD_PASSO);
        inicio = limitarRolagem(inicio, linhas.size(), p, BUILD_TOPO, BUILD_PASSO);
        if (linhas.isEmpty()) contornado(g, "None acquired this run.", FontHandler.font(FontHandler.Septem, 8f * s),
                p.x + MARGEM_INTERNA * s, p.y + 43 * s, Palette.LIGHT, s);
        for (int i = 0; i < capacidade && i + inicio < linhas.size(); i++) {
            var dado = linhas.get(i + inicio);
            int y = p.y + (BUILD_TOPO + i * BUILD_PASSO) * s;
            par(g, p, dado.rotulo(), dado.valor(), y + 8 * s, FontHandler.font(FontHandler.Septem, 8f * s), s);
            if (dado.efeito() != null) contornado(g, dado.efeito(), FontHandler.font(FontHandler.Septem, 6f * s),
                    p.x + MARGEM_INTERNA * s, y + 20 * s, Palette.LIGHT, s);
            g.setColor(Palette.MAIN);
            g.fillRect(p.x + MARGEM_INTERNA * s, y + 26 * s, p.width - 2 * MARGEM_INTERNA * s, s);
        }
        rodapeDaLista(g, p, inicio, linhas.size(), capacidade, s);
    }

    private void desenharLoja(Graphics2D g, int s) {
        Rectangle p = painelLoja;
        painel(g, p, "Upgrades", s);
        Font f = FontHandler.font(FontHandler.Septem, 8f * s);
        Font grande = FontHandler.font(FontHandler.Game, 8f * s);
        String ouro = "Coins: " + Meta.ouro(nomeDoGato());
        String ganho = "+" + Estatisticas.ouroGanho() + " this run";
        contornado(g, ouro, grande, p.x + MARGEM_INTERNA * s, p.y + 34 * s, Palette.TEXT, s);
        contornado(g, ganho, f, p.x + MARGEM_INTERNA * s, p.y + 47 * s, Palette.ACCENT, s);
        double fase = (tempoDaApresentacao() - ApresentacaoDoFim.OURO) / ApresentacaoDoFim.BRILHO;
        if (fase > 0 && fase < 1) {
            Realce.clarear(g, new Rectangle(p.x + s, p.y + 24 * s, p.width - 2 * s, 27 * s),
                    (float) Math.sin(fase * Math.PI) * 0.35f);
        }

        Graphics2D antes = g;
        g = (Graphics2D) g.create();
        g.clip(areaDaLoja());
        Meta.Permanente[] todos = Meta.Permanente.values();
        for (int i = 0; i < todos.length; i++) {
            Meta.Permanente m = todos[i];
            Rectangle r = linhasDaLoja.get(i);
            int nivel = Meta.nivel(nomeDoGato(), m);
            boolean cheio = nivel >= Meta.Permanente.NIVEL_MAXIMO;
            boolean podeComprar = !cheio && Meta.ouro(nomeDoGato()) >= Meta.preco(nomeDoGato(), m);
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
            contornado(g, m.nome(), f, r.x + RECUO_DA_LINHA * s, r.y + 13 * s, Palette.TEXT, s);
            contornado(g, m.efeito(), FontHandler.font(FontHandler.Septem, 7f * s), r.x + RECUO_DA_LINHA * s, r.y + 27 * s,
                    Palette.ACCENT, s);
            // Pips de nivel, na altura do nome; o preço, na do efeito.
            for (int k = 0; k < Meta.Permanente.NIVEL_MAXIMO; k++) {
                g.setColor(Palette.OUTLINE);
                g.fillRect(r.x + r.width - 37 * s + k * 5 * s, r.y + 6 * s, 4 * s, 4 * s);
                if (k < nivel) {
                    g.setColor(Palette.TEXT);
                    g.fillRect(r.x + r.width - 36 * s + k * 5 * s, r.y + 7 * s, 2 * s, 2 * s);
                }
            }
            // O preço: claro se dá para comprar, o vermelho de aviso se falta ouro,
            // numa plaquinha OUTLINE: o WARN direto sobre o MAIN da linha não se lia.
            String preco = cheio ? "MAX" : Meta.preco(nomeDoGato(), m) + "c";
            int pw = FontHandler.getWidth(preco, f);
            g.setColor(Palette.OUTLINE);
            g.fillRect(r.x + r.width - (RECUO_DA_LINHA + 2) * s - pw, r.y + 18 * s, pw + 4 * s, 13 * s);
            contornado(g, preco, f, r.x + r.width - RECUO_DA_LINHA * s - pw, r.y + 27 * s,
                    cheio ? Palette.ACCENT : podeComprar ? Palette.TEXT : Palette.WARN, s);
        }
        g.dispose();
        g = antes;
        Font dica = FontHandler.font(FontHandler.Septem, 6f * s);
        contornadoCentro(g, "Upgrades apply to every new run.", dica, p.x + p.width / 2, p.y + p.height - 13 * s,
                Palette.LIGHT, s);
    }

    private Rectangle areaDaLoja() {
        int s = escala();
        return new Rectangle(painelLoja.x, painelLoja.y + LOJA_TOPO * s, painelLoja.width,
                Math.max(1, painelLoja.height - (LOJA_TOPO + 24) * s));
    }

    private int maximoRolagemLoja() {
        return Math.max(0, LOJA_TOPO + 2 * LINHA_PASSO + LINHA_H - (painelLoja.height / escala() - 24));
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
