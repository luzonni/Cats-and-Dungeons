package com.retronova.game.dialogos;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.NineSlice;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Acao;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.Rodape;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A caixa de diálogo: retrato numa moldura à esquerda, aba com o nome, a fala
 * escrevendo à direita e a dica de continuar embaixo.
 *
 * O PADRÃO É O DAS REFERÊNCIAS (Stardew Valley, Eastward, Sea of Stars), na
 * língua da interface do jogo: painel 9-slice na paleta, aba de nome no mesmo
 * desenho dos botões, com as orelhas, e a moldura do retrato também com elas.
 * Texto pela TextFx e glyphs do Rodapé, os mesmos da tela de Controles.
 *
 * O JOGO PARA POR BAIXO (Engine.pause), como na placa (Leitura): conversa é
 * momento de leitura, e os Game Accessibility Guidelines pedem que o texto
 * avance no ritmo de quem lê, por ação do jogador e nunca por tempo.
 *
 * AVANÇAR: clique, Enter, Espaço ou A. O primeiro aperto completa a fala, o
 * segundo passa para a próxima. PULAR: Esc ou B, que encerra a conversa e
 * dispara os eventos do fim — pular o vendedor abre a loja do mesmo jeito.
 *
 * TUDO EM TICKS, desenhado com a fração entre ticks: entrada e saída, máquina de
 * escrever, piscar, respirar e o pulo de troca de emoção. Nada é criado por
 * quadro: fontes, retratos e molduras ficam prontos.
 */
public final class CaixaDeDialogo implements Activity {

    // ------------------------------------------------------------ ajuste fino
    /** Ticks de entrada e de saída da caixa. */
    static final int ENTRADA = 10, SAIDA = 8;
    /** Quanto a caixa sobe ao entrar, em pixels nativos. */
    static final int SUBIDA = 10;
    /** Ticks sem aceitar aperto depois de abrir: o clique que abriu não pode passar a fala. */
    static final int CARENCIA = 10;
    /** Retrato: 32 pixels de arte, ampliados duas vezes a escala da interface. */
    static final int FATOR_DO_RETRATO = 2;
    /** Borda da moldura do retrato, em pixels nativos (contorno, 3 de borda, contorno). */
    static final int BORDA_DO_RETRATO = 5, ORELHA_DO_RETRATO = 5;
    /** Largura máxima da caixa toda, em pixels nativos. */
    static final int LARGURA_MAXIMA = 392;
    /** Vão entre o retrato e o painel, e o recuo do texto dentro do painel. */
    static final int VAO = 4, RECUO_X = 10, RECUO_Y = 12;
    /** Tamanho da letra da fala, em pixels nativos (a fonte Septem do corpo das telas). */
    static final float TAMANHO_DO_TEXTO = 8f;
    /** Distância entre linhas de texto, em pixels nativos. */
    static final int ENTRELINHA = 13;
    /** Piscada: intervalo sorteado entre os dois valores e a duração, em ticks. */
    static final int PISCAR_MIN = 150, PISCAR_MAX = 320, PISCADA = 7;
    /** Respiro do retrato: um pixel de arte para baixo e para cima, neste período. */
    static final int RESPIRO = 150;
    /** A boca troca entre aberta e fechada a cada tantos ticks enquanto escreve. */
    static final int BOCA = 5;
    /** O pulo do retrato quando a emoção muda, em ticks. */
    static final int PULO = 6;
    /** Blip a cada tantas letras. */
    static final int LETRAS_POR_BLIP = 2;
    /** Véu sobre o jogo. */
    private static final Color VEU = new Color(0, 0, 0, 90);

    private static final NineSlice PAINEL = new NineSlice("dialogo", 24, 8, 8, 8, 8);
    private static final NineSlice MOLDURA = new NineSlice("retrato", 24, 8, 8, 13, 8);

    private final Conversa conversa;
    private final Object falante;
    private final CatalogoDeDialogos catalogo = CatalogoDeDialogos.get();
    private final Random sorteio = new Random();

    private int indice = -1;
    private MaquinaDeEscrever maquina;
    private Fala fala;
    private Emocao emocaoAnterior;

    private int tempo;
    private int entrada;
    private int saida = -1;
    private int carencia = CARENCIA;
    private int letras;
    private int bocaAberta;
    private int piscando;
    private int proximaPiscada;
    private int pulo;

    /** Linhas da fala atual, quebradas para a largura do painel na escala atual. */
    private List<String> linhas = List.of();
    private int larguraDasLinhas = -1;

    private final Map<String, Sounds[]> vozes = new HashMap<>();

    public CaixaDeDialogo(Conversa conversa, Object falante) {
        this.conversa = conversa;
        this.falante = falante;
        this.proximaPiscada = PISCAR_MIN + sorteio.nextInt(PISCAR_MAX - PISCAR_MIN);
        proximaFala();
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        tempo++;
        if (saida >= 0) {
            if (++saida >= SAIDA) {
                encerrar();
            }
            return;
        }
        if (entrada < ENTRADA) {
            entrada++;
        }
        piscar();
        if (pulo > 0) {
            pulo--;
        }
        if (maquina != null) {
            int novas = maquina.tick();
            if (novas > 0 && maquina.ultimaEhLetra()) {
                letras += novas;
                bocaAberta = BOCA * 2;
                if (letras >= LETRAS_POR_BLIP) {
                    letras = 0;
                    blip();
                }
            } else if (bocaAberta > 0) {
                bocaAberta--;
            }
        }
        if (carencia > 0) {
            carencia--;
            return;
        }
        if (Acao.VOLTAR.apertou()) {
            sair();
            return;
        }
        if (Acao.SELECIONAR.apertou() || KeyBoard.KeyPressed("Space")) {
            if (maquina != null && !maquina.completo()) {
                maquina.completar();
            } else {
                proximaFala();
            }
        }
    }

    private void proximaFala() {
        indice++;
        if (indice >= conversa.falas().size()) {
            sair();
            return;
        }
        emocaoAnterior = fala == null ? null : fala.emocao();
        fala = conversa.falas().get(indice);
        maquina = new MaquinaDeEscrever(catalogo.texto(fala.texto()), Configs.VelocidadeDoTexto());
        larguraDasLinhas = -1;
        letras = LETRAS_POR_BLIP;   // a primeira letra já soa
        if (emocaoAnterior != null && emocaoAnterior != fala.emocao()) {
            pulo = PULO;
        }
        for (String ev : fala.eventos()) {
            EventosDoDialogo.disparar(ev, conversa, falante);
        }
    }

    private void piscar() {
        if (piscando > 0) {
            piscando--;
            return;
        }
        if (--proximaPiscada <= 0) {
            piscando = PISCADA;
            proximaPiscada = PISCAR_MIN + sorteio.nextInt(PISCAR_MAX - PISCAR_MIN);
        }
    }

    private void blip() {
        Personagem p = catalogo.personagem(fala.personagem());
        if (p == null || p.voz().isEmpty()) {
            return;
        }
        Sounds[] sons = vozes.computeIfAbsent(p.id(), k -> resolverVoz(p));
        if (sons.length > 0) {
            Sound.play(sons[sorteio.nextInt(sons.length)]);
        }
    }

    private static Sounds[] resolverVoz(Personagem p) {
        List<Sounds> sons = new ArrayList<>();
        for (String nome : p.voz()) {
            try {
                sons.add(Sounds.valueOf(nome));
            } catch (IllegalArgumentException semEsse) {
                System.err.println("[dialogos] voz desconhecida: " + nome);
            }
        }
        return sons.toArray(new Sounds[0]);
    }

    private void sair() {
        if (saida < 0) {
            saida = 0;
        }
    }

    /** A caixa sumiu: devolve o jogo e dispara os eventos do fim (a loja, por exemplo). */
    private void encerrar() {
        if (Engine.sobreposta() == this) {
            Engine.pause(null);
        }
        for (String ev : conversa.aoTerminar()) {
            EventosDoDialogo.disparar(ev, conversa, falante);
        }
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void render(Graphics2D g) {
        if (fala == null) {
            return;
        }
        float alpha = Engine.alpha();
        int s = Configs.UiScale();
        double presenca = presenca(alpha);
        if (presenca <= 0) {
            return;
        }
        int largura = Engine.window.largura(), altura = Engine.window.altura();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) presenca));
        g2.setColor(VEU);
        g2.fillRect(0, 0, largura, altura);

        // Medidas em pixels nativos, multiplicadas pela escala da interface.
        int retrato = Retrato.LADO * FATOR_DO_RETRATO;
        int molduraW = retrato + 2 * BORDA_DO_RETRATO;
        int molduraH = molduraW + ORELHA_DO_RETRATO;
        int total = Math.min(LARGURA_MAXIMA, largura / s - 2 * Configs.Margin() / s);
        int painelW = total - molduraW - VAO;
        int painelH = molduraW;

        int x0 = (largura - total * s) / 2;
        int subida = (int) Math.round((1 - suave(presenca)) * SUBIDA * s);
        int base = altura - Configs.Margin() - Rodape.ALTURA * s / 2 + subida;
        int yPainel = base - painelH * s;
        int yMoldura = base - molduraH * s;

        Personagem p = catalogo.personagem(fala.personagem());
        String moldura = p == null ? Personagem.MOLDURA_PADRAO : p.moldura();

        // O retrato na moldura com orelhas.
        MOLDURA.draw(g2, 0, x0, yMoldura, molduraW * s, molduraH * s, s);
        desenharRetrato(g2, p, x0 + BORDA_DO_RETRATO * s, yMoldura + (ORELHA_DO_RETRATO + BORDA_DO_RETRATO) * s,
                retrato * s, s, alpha);

        // O painel de texto e a aba com o nome.
        int xPainel = x0 + (molduraW + VAO) * s;
        painel(moldura).draw(g2, 0, xPainel, yPainel, painelW * s, painelH * s, s);
        desenharNome(g2, p, xPainel + 8 * s, yPainel, s);
        desenharTexto(g2, xPainel + RECUO_X * s, yPainel + RECUO_Y * s, (painelW - 2 * RECUO_X) * s, s);
        desenharDicas(g2, xPainel + painelW * s, yPainel + painelH * s, s, (float) presenca);
        g2.dispose();
    }

    /** De 0 a 1: entrando, aberta, saindo. */
    private double presenca(float alpha) {
        if (saida >= 0) {
            return Math.max(0, 1 - (saida + alpha) / SAIDA);
        }
        return Math.min(1, (entrada + (entrada < ENTRADA ? alpha : 0)) / ENTRADA);
    }

    private static double suave(double f) {
        return 1 - Math.pow(1 - f, 3);
    }

    /** As molduras de painel por nome (Personagem.moldura), mesma geometria da padrão. */
    private static final Map<String, NineSlice> PAINEIS = new HashMap<>();

    private static NineSlice painel(String nome) {
        return PAINEIS.computeIfAbsent(nome, n -> n.equals(Personagem.MOLDURA_PADRAO) ? PAINEL
                : new NineSlice(n, 24, 8, 8, 8, 8));
    }

    private void desenharRetrato(Graphics2D g, Personagem p, int x, int y, int lado, int s, float alpha) {
        Retrato r = p == null ? null : Retrato.de(p.retrato());
        if (r == null) {
            return;
        }
        int linha = piscando > 0 ? Retrato.PISCANDO
                : bocaAberta > 0 && (tempo / BOCA) % 2 == 0 ? Retrato.FALANDO : Retrato.ABERTO;
        int fator = FATOR_DO_RETRATO * s;
        BufferedImage q = r.quadro(fala.emocao(), linha, fator);
        // Respira um pixel de arte, e pula um quando a emoção muda.
        int respiro = Math.sin(2 * Math.PI * (tempo + alpha) / RESPIRO) > 0.35 ? fator : 0;
        int salto = pulo > PULO / 2 ? -fator : 0;
        Graphics2D c = (Graphics2D) g.create();
        c.clipRect(x, y, lado, lado);
        c.drawImage(q, x, y + respiro + salto, null);
        c.dispose();
    }

    private void desenharNome(Graphics2D g, Personagem p, int x, int yPainel, int s) {
        String nome = p == null ? fala.personagem() : p.nome();
        Font fonte = FontHandler.font(FontHandler.Game, 7f * s);
        int w = FontHandler.getWidth(nome, fonte) + 18 * s;
        int h = 20 * s;
        int y = yPainel - h + 8 * s;
        Button.frame(true).draw(g, 0, x, y, w, h, s);
        TextFx.draw(g, nome, fonte, x + 9 * s, y + h - 6 * s, Palette.TEXT, s);
    }

    private void desenharTexto(Graphics2D g, int x, int y, int largura, int s) {
        Font fonte = FontHandler.font(FontHandler.Septem, TAMANHO_DO_TEXTO * s);
        g.setFont(fonte);
        if (larguraDasLinhas != largura) {
            linhas = MaquinaDeEscrever.quebrar(maquina.texto(), largura, t -> FontHandler.getWidth(t, fonte));
            larguraDasLinhas = largura;
        }
        int restante = maquina.visiveis();
        int base = y + 9 * s;
        for (String l : linhas) {
            if (restante <= 0) {
                break;
            }
            String parte = l.length() <= restante ? l : l.substring(0, restante);
            TextFx.draw(g, parte, x, base, Palette.TEXT, s);
            restante -= l.length() + 1;   // o espaço que a quebra comeu
            base += ENTRELINHA * s;
        }
    }

    private static final List<Rodape.Dica> CONTINUAR = List.of(
            new Rodape.Dica("Enter", "A", "Continue"), new Rodape.Dica("Esc", "B", "Skip"));
    private static final List<Rodape.Dica> SO_PULAR = List.of(new Rodape.Dica("Esc", "B", "Skip"));

    private void desenharDicas(Graphics2D g, int direita, int fundo, int s, float opacidade) {
        List<Rodape.Dica> dicas = maquina.completo() ? CONTINUAR : SO_PULAR;
        int w = Rodape.largura(dicas);
        // A dica de continuar sobe e desce um pixel quando a fala acabou: é o
        // "pode apertar" das referências, sem precisar de seta piscando.
        int bob = maquina.completo() && (tempo / 20) % 2 == 0 ? -s : 0;
        // A opacidade vai junto: o Rodape troca a composicao do desenho, e sem ela
        // a dica ficava acesa enquanto a caixa sumia.
        Rodape.desenharEm(g, dicas, direita - w - 8 * s, fundo - 13 * s + bob, opacidade);
    }

    @Override
    public void dispose() {
    }
}
