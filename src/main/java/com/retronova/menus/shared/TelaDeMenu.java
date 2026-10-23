package com.retronova.menus.shared;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * A base de toda tela de menu: inicial, saves, seleção de gato, opções, pausa,
 * conquistas, fim de corrida e vitória.
 *
 * ---------------------------------------------------------------------------
 * O PADRÃO, e de onde ele vem
 *
 * Minecraft, Forza Horizon e Red Dead Redemption 2 fazem a tela inicial, a pausa e
 * as opções parecerem partes de um mesmo sistema porque elas SÃO: uma moldura só,
 * com o mesmo título, os mesmos componentes, o mesmo rodapé e a mesma transição.
 * No Minecraft a tela de opções é uma classe só, que recebe "para onde voltar" — a
 * do título e a da pausa são a mesma tela. Os roguelikes de referência (Hades,
 * Dead Cells, Slay the Spire, Vampire Survivors) seguem a mesma regra, com um
 * detalhe que importa aqui: a pausa é uma camada escura sobre o jogo CONGELADO, e
 * não uma tela à parte. O jogador continua vendo onde estava.
 *
 * O que muda entre as telas é só o FUNDO, e ele é decidido pelo contexto, não pela
 * tela: fora da partida, a ilustração do título, viva e contínua (uma instância
 * só para todas as telas, então ela não recomeça a cada troca); dentro da partida,
 * o jogo parado sob o véu. É isso que deixa as Opções abertas pela tela inicial e
 * pela pausa serem a mesma tela, e não duas parecidas.
 *
 * ---------------------------------------------------------------------------
 * O LAYOUT
 *
 *     ┌────────────────────────────────────────┐
 *     │                TÍTULO                  │  faixa do título, fixa no topo
 *     │  ┌──────────────────────────────────┐  │
 *     │  │      área do conteúdo            │  │  a tela centraliza o que é dela aqui
 *     │  └──────────────────────────────────┘  │
 *     │      [Enter] Select    [Esc] Back      │  rodapé de dicas, fixo embaixo
 *     └────────────────────────────────────────┘
 *
 * ---------------------------------------------------------------------------
 * NAVEGAÇÃO
 *
 * {@link #abrir} empilha a tela nova sobre a atual — como Activity, fora da
 * partida; como camada da pausa, dentro dela, lembrando quem estava embaixo. Esc
 * (ou B no controle) chama {@link #voltar}, que desfaz exatamente isso. Não há
 * tela que volte "para um lugar fixo": cada uma volta para quem a abriu.
 */
public abstract class TelaDeMenu implements Activity {

    // -------------------------------------------------------------- ajuste fino

    /**
     * Duração da entrada de toda tela, em ticks: 9 = 0,15 s.
     *
     * Curta de propósito. A Nielsen Norman trata 0,1 s como "instantâneo" e põe
     * em torno de 0,4 s o ponto em que a interface passa a parecer lenta; menus
     * são navegados em sequência, e uma transição que se paga uma vez cobra de
     * novo a cada tela. 0,15 s dá para o olho ver o movimento sem esperar por ele.
     */
    public static final int TRANSICAO = 9;
    /** Quanto o conteúdo sobe ao entrar, em pixels nativos. */
    public static final int SUBIDA = 6;

    // -------------------------------------------------------------- estado

    /** A ilustração do título, uma só para todas as telas fora da partida. */
    private static Cenario cenario;

    /** Tick global das telas de menu, para perceber quando uma volta a ser a corrente. */
    private static long ticks;
    private long ultimoTick = -2;

    protected final Confirm confirmacao = new Confirm();
    private final String titulo;
    private Color corDoTitulo = Palette.TEXT;
    private int idade;

    /** A camada de baixo, quando esta foi aberta por cima de outra dentro da partida. */
    private TelaDeMenu anterior;
    /** Esta é a primeira camada sobre o jogo: o véu entra junto com ela. */
    private boolean primeiraSobreOJogo;

    protected TelaDeMenu(String titulo) {
        this.titulo = titulo;
    }

    protected void corDoTitulo(Color cor) {
        this.corDoTitulo = cor;
    }

    // -------------------------------------------------------------- navegação

    /**
     * Abre uma tela por cima da atual.
     *
     * Na partida ela vira a camada de pausa — a de baixo fica guardada para o
     * "voltar" — e a música é abafada; fora dela, entra na pilha de Activities.
     */
    public static void abrir(TelaDeMenu nova) {
        if (Engine.getACTIVITY() instanceof Game) {
            Activity embaixo = Engine.sobreposta();
            nova.anterior = embaixo instanceof TelaDeMenu t ? t : null;
            nova.primeiraSobreOJogo = embaixo == null;
            if (nova.abafaAMusica()) {
                Sound.abafarParaPausa(true);
            }
            Engine.pause(nova);
        } else {
            Engine.heapActivity(nova);
        }
    }

    /** Volta para quem abriu esta tela. */
    protected void voltar() {
        if (Engine.getACTIVITY() == this) {
            Engine.backActivity();
            return;
        }
        if (anterior == null) {
            Sound.abafarParaPausa(false);
        }
        Engine.pause(anterior);
    }

    /**
     * Desempilha tudo até a tela inicial, com a música dela.
     *
     * Antes cada caminho para o menu fazia a sua conta — a pausa tirava duas telas
     * da pilha, o fim de corrida empilhava um Menu NOVO —, e o resultado dependia
     * de como a corrida tinha começado: às vezes o jogador caía na lista de saves, e
     * a pilha crescia a cada partida.
     */
    public static void voltarAoMenuPrincipal() {
        Engine.pause(null);
        Sound.stopAll();
        while (Engine.telasNaPilha() > 1 && !(Engine.getACTIVITY() instanceof com.retronova.menus.Menu)) {
            Engine.backActivity();
        }
        if (Engine.getACTIVITY() instanceof com.retronova.menus.Menu menu) {
            menu.tocarMusica();
        } else {
            Engine.heapActivity(new com.retronova.menus.Menu());
        }
    }

    /** A música baixa enquanto esta tela está aberta sobre o jogo? */
    protected boolean abafaAMusica() {
        return true;
    }

    /** Esc / B volta? Telas que não podem ser recusadas respondem false. */
    protected boolean podeVoltar() {
        return true;
    }

    /** Esta tela está aberta por cima da partida? */
    protected final boolean sobreOJogo() {
        return Engine.getACTIVITY() != this && Engine.getACTIVITY() instanceof Game;
    }

    // -------------------------------------------------------------- ciclo

    @Override
    public final void tick() {
        long agora = ++ticks;
        if (ultimoTick != agora - 1) {
            idade = 0;          // voltou a ser a tela corrente: entra de novo
            aoEntrar();
        }
        ultimoTick = agora;
        idade++;
        if (!sobreOJogo()) {
            cenario().tick();
        }
        posicionar();
        if (confirmacao.tick()) {
            return;
        }
        if (podeVoltar() && KeyBoard.KeyPressed("Escape")) {
            voltar();
            return;
        }
        atualizar();
    }

    @Override
    public final void render(Graphics2D g) {
        if (idade == 0) {
            posicionar();       // o primeiro quadro pode vir antes do primeiro tick
        }
        desenharFundo(g);
        float p = progresso();
        int s = Configs.UiScale();
        Graphics2D c = (Graphics2D) g.create();
        c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p));
        c.translate(0, Math.round((1 - p) * SUBIDA * s));
        if (titulo != null) {
            desenharCabecalho(c, titulo, corDoTitulo);
        }
        desenharConteudo(c);
        c.dispose();
        Rodape.desenhar(g, dicas(), p);
        confirmacao.render(g);
        desenharPorCima(g);
    }

    /** Hook de reentrada: a tela voltou a ser a corrente. */
    protected void aoEntrar() {
    }

    /** Permite que o encerramento tenha seu próprio gesto, usando o título comum. */
    protected void desenharCabecalho(Graphics2D g, String texto, Color cor) {
        desenharTitulo(g, texto, cor);
    }

    /** Calcula a posição de tudo pela janela atual. Chamado todo tick. */
    protected abstract void posicionar();

    /** O tick próprio da tela: entrada do jogador e animações. */
    protected abstract void atualizar();

    /** O conteúdo, já com a transição aplicada. */
    protected abstract void desenharConteudo(Graphics2D g);

    /** O que fica por cima de tudo, sem transição (balões de ajuda). */
    protected void desenharPorCima(Graphics2D g) {
    }

    /** As ações desta tela, para o rodapé. */
    protected List<Rodape.Dica> dicas() {
        return podeVoltar() ? List.of(Rodape.SELECIONAR, Rodape.VOLTAR) : List.of(Rodape.SELECIONAR);
    }

    // -------------------------------------------------------------- layout base

    /** Linha de base do título: fixa, no topo, igual em toda tela. */
    protected static int baseDoTitulo() {
        int s = Configs.UiScale();
        return Math.max(Configs.Margin(), 6 * s) + 16 * s;
    }

    /**
     * A área entre o título e o rodapé. Cada tela centraliza o próprio conteúdo
     * aqui dentro, e assim nada encosta no título nem passa por baixo das dicas.
     */
    protected static Rectangle areaDoConteudo() {
        int s = Configs.UiScale();
        int topo = baseDoTitulo() + 8 * s;
        // A FOLGA ANTES DO RODAPÉ é parte da área reservada: sem ela, o que senta na
        // base da área (os botões e as miniaturas da seleção de gato) encostava nas
        // dicas, e em telas mais baixas passava por cima delas.
        int base = Engine.window.altura() - Configs.Margin() - Rodape.ALTURA * s - FOLGA_DO_RODAPE * s;
        return new Rectangle(0, topo, Engine.window.largura(), Math.max(0, base - topo));
    }

    /** Vão entre a área do conteúdo e o rodapé, em pixels nativos. */
    public static final int FOLGA_DO_RODAPE = 6;

    /**
     * O painel padrão das telas de menu: borda escura, corpo fundo e um fio
     * escuro no topo. É o que fica atrás das linhas de configuração.
     */
    public static void desenharPainel(Graphics2D g, Rectangle r) {
        int s = Configs.UiScale();
        g.setColor(Palette.OUTLINE);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Palette.DEEP);
        g.fillRect(r.x + s, r.y + s, r.width - 2 * s, r.height - 2 * s);
        g.setColor(Palette.OUTLINE);
        g.fillRect(r.x + 3 * s, r.y + 3 * s, r.width - 6 * s, s);
    }

    /** O título no padrão: fonte do jogo, 14 pixels nativos, centralizado. */
    protected static void desenharTitulo(Graphics2D g, String texto, Color cor) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 14f * s);
        int x = Engine.window.largura() / 2 - FontHandler.getWidth(texto, fonte) / 2;
        TextFx.draw(g, texto, fonte, x, baseDoTitulo(), cor, s);
    }

    /** De 0 (acabou de abrir) a 1 (entrou), com ease-out, entre ticks. */
    protected final float progresso() {
        float f = Math.min(1f, (idade + (idade > 0 ? Engine.alpha() : 0)) / TRANSICAO);
        return 1f - (float) Math.pow(1 - f, 3);
    }

    // -------------------------------------------------------------- fundo

    /**
     * A ilustração animada do título, uma só para todas as telas. Pública porque a
     * abertura (Abertura) faz a cena surgir com ESTA instância: com outra, a bruma e
     * a poeira pulariam de lugar na passagem para a tela inicial.
     */
    public static synchronized Cenario cenario() {
        if (cenario == null) {
            cenario = new Cenario("icons.Gato");
        }
        return cenario;
    }

    /** Fora da partida o véu escurece a ilustração; a tela inicial é a exceção. */
    protected boolean veuSobreAIlustracao() {
        return true;
    }

    /**
     * O fundo pelo contexto: a ilustração do título fora da partida, o jogo
     * congelado sob o véu dentro dela. Quem quiser outro (a seleção de gato
     * acende o personagem na própria ilustração) sobrescreve.
     */
    protected void desenharFundo(Graphics2D g) {
        int w = Engine.window.largura(), h = Engine.window.altura();
        if (sobreOJogo()) {
            // O véu só entra animado na primeira camada; entre a pausa e as opções
            // ele já está lá, e piscar a cada troca leria como corte.
            float veu = primeiraSobreOJogo ? progresso() : 1f;
            g.setColor(new Color(Palette.VEIL.getRed(), Palette.VEIL.getGreen(), Palette.VEIL.getBlue(),
                    (int) (Palette.VEIL.getAlpha() * veu)));
            g.fillRect(0, 0, w, h);
            return;
        }
        cenario().render(g);
        if (veuSobreAIlustracao()) {
            g.setColor(Palette.VEIL);
            g.fillRect(0, 0, w, h);
        }
    }

    @Override
    public void dispose() {
    }
}
