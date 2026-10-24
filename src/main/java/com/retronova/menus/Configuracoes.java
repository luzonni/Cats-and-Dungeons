package com.retronova.menus;

import com.retronova.engine.Configs;
import com.retronova.engine.Debugging;
import com.retronova.engine.Engine;
import com.retronova.engine.EscalaDaInterface;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Corrida;
import com.retronova.game.Game;
import com.retronova.game.items.Elemento;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.objects.entities.Player;
import com.retronova.menus.shared.Abas;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.OptionRow;
import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;
import com.retronova.menus.controles.PainelDeControles;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings — O PAINEL ÚNICO DE CONFIGURAÇÕES, e também a pausa.
 *
 * DUAS PORTAS, UM COMPONENTE. Pela tela inicial ele abre com as abas de
 * configuração (Video, Audio, Interface). Dentro da corrida ele É a pausa: ganha
 * na frente a aba Run, com Continue, Restart, Main Menu e Quit, um resumo curto da
 * corrida e o aviso do save. Antes a pausa era outra interface — uma coluna de
 * botões grandes — e "Options" levava a uma segunda tela; o jogador mudava de
 * gramática no meio do caminho.
 *
 * Por que assim: o Hades e o Dead Cells abrem a pausa direto num painel e
 * guardam as configurações nele; o Forza Horizon e o Red Dead Redemption 2 põem
 * a partida e as configurações em abas de um mesmo menu, trocadas por LB/RB; o
 * Minecraft usa uma tela de opções só, que sabe de onde foi aberta. A aba Run
 * fica primeiro e com o foco em Continue: quem pausa quer, antes de tudo, voltar
 * a jogar (Game Accessibility Guidelines, "allow the game to be paused" e
 * retomar sem passos extras).
 *
 * O fundo vem do contexto (ver TelaDeMenu): a ilustração do título fora da
 * corrida, o jogo congelado dentro dela.
 *
 * Organizada como lista vertical dentro de abas, e não como grade 4x3. Na grade,
 * o "+" de uma opção acabava a 10px do "-" da opção vizinha e a 200px do próprio
 * rótulo, o que fazia os controles parecerem pertencer à coluna errada.
 *
 * Quase tudo se aplica na hora. As exceções são resolução e tela cheia, que
 * exigem recriar a janela; essas ficam pendentes até o botão Apply.
 */
public class Configuracoes extends TelaDeMenu {

    /** Largura máxima do painel, em pixels nativos. */
    private static final int LARGURA_DO_PAINEL = 288;

    /** Limites de quadros, na ordem do ciclo: o do monitor primeiro, sem limite por último. */
    private static final int[] FPS_VALORES = {Configs.LIMITE_MONITOR, 30, 60, 120, 144, 165, 240, Configs.SEM_LIMITE};

    /** O aviso do save, na aba Run, em duas linhas para caber no painel. */
    private static final String[] AVISO = {"Leaving saves your run.", "Enemies on screen are not kept."};

    /** Aberta dentro da corrida: é a pausa, com a aba Run na frente. */
    private final boolean naCorrida;
    private final Abas abas;
    private final List<List<OptionRow>> linhas = new ArrayList<>();
    /** Onde começa o resumo e o aviso da aba Run, abaixo das ações. */
    private int yDoResumo;
    /**
     * O foco do teclado e do controle: as linhas da aba, e depois delas o Apply
     * (quando há o que aplicar) e o Back. Antes os dois botões só respondiam ao
     * mouse — quem jogava de controle não tinha como aplicar uma resolução.
     */
    private int foco;
    private Rectangle painel = new Rectangle();

    // Mudanças que só podem ser aplicadas recriando a janela.
    /** As resoluções deste monitor (ver Window.resolucoesDoMonitor) e a escolhida, por índice nela. */
    private final List<java.awt.Dimension> resolucoes = Engine.window.resolucoesDoMonitor();
    private int pendResolucao = indiceDaResolucaoAtual();
    private int pendTelaCheia = Configs.Fullscreen() ? 1 : 0;

    private final Button aplicar;
    private final Button voltar;
    /** O conteúdo da aba Controls, abaixo da linha Device. */
    private final PainelDeControles controles = new PainelDeControles();

    /** As configurações, pela tela inicial. */
    public Configuracoes() {
        this(false);
    }

    /** A pausa: o mesmo painel, com a aba Run na frente e o foco em Continue. */
    public static Configuracoes pausa() {
        return new Configuracoes(true);
    }

    private Configuracoes(boolean naCorrida) {
        super(naCorrida ? "Paused" : "Settings");
        this.naCorrida = naCorrida;
        this.abas = naCorrida
                ? new Abas("Run", "Video", "Audio", "Interface", "Controls")
                : new Abas("Video", "Audio", "Interface", "Controls");
        if (naCorrida) {
            linhas.add(linhasDaCorrida());
        }
        montarLinhas();
        this.aplicar = new Button(0, 0, 0, 0, "Apply", b -> aplicarPendentes()).primary();
        // Na pausa o botão de fora do painel RETOMA, como o Esc: "Back" ali não
        // diria para onde se volta.
        this.voltar = new Button(0, 0, 0, 0, naCorrida ? "Resume" : "Back", b -> voltar());
    }

    // ------------------------------------------------------------ aba Run

    /**
     * As ações da corrida, como linhas do painel. As três que saem dela pedem
     * confirmação: um Enter a mais não pode custar uma corrida.
     */
    private List<OptionRow> linhasDaCorrida() {
        List<OptionRow> run = new ArrayList<>();
        run.add(OptionRow.acao("Continue", this::voltar)
                .desc("Back to the run. Esc does the same."));
        run.add(OptionRow.acao("Restart",
                () -> confirmacao.perguntar("Restart the run?", this::reiniciar))
                .desc("Start over from the beginning. This run is lost."));
        run.add(OptionRow.acao("Main Menu",
                () -> confirmacao.perguntar("Return to main menu?", this::irAoMenu))
                .desc("Save the run and go to the title screen. Resume it from Play."));
        run.add(OptionRow.acao("Quit",
                () -> confirmacao.perguntar("Do you want to quit?", this::sair))
                .desc("Save the run and close the game."));
        return run;
    }

    private void reiniciar() {
        encerrarAudicao(true);
        Engine.pause(null);
        Game.restart();
    }

    private void irAoMenu() {
        // Grava ANTES de desempilhar: depois disso a partida ja nao e a corrente,
        // e a gravacao nao teria de quem fotografar o estado.
        encerrarAudicao(false);
        FalhaDeSave.tentar(() -> {
            Corrida.gravarObrigatorio();
            voltarAoMenuPrincipal();
        });
    }

    private void sair() {
        FalhaDeSave.tentar(() -> {
            Corrida.gravarObrigatorio();
            Engine.CLOSE();
        });
    }

    private boolean naAbaDaCorrida() {
        return naCorrida && abas.atual() == 0;
    }

    /** Ha uma previa de musica tocando por cima do que o jogo tocava. */
    private boolean auditando;

    /** Qual faixa a previa esta tocando agora. */
    private Musics previa;

    private void montarLinhas() {
        java.awt.Dimension monitor = Engine.window.tamanhoDoMonitor();
        String[] rotulosDeResolucao = new String[resolucoes.size()];
        for (int i = 0; i < rotulosDeResolucao.length; i++) {
            java.awt.Dimension r = resolucoes.get(i);
            rotulosDeResolucao[i] = r.width + " x " + r.height + (r.equals(monitor) ? " *" : "");
        }
        String[] fps = new String[FPS_VALORES.length];
        for (int i = 0; i < fps.length; i++) {
            int v = FPS_VALORES[i];
            // ACIMA DO MONITOR, AVISADO. Sem VSync, 240 quadros num monitor de 180 Hz
            // saem desencontrados da tela (rasgo e trepidação) por mais preciso que
            // seja o limitador; o rótulo diz isso antes de o jogador descobrir.
            fps[i] = v == Configs.LIMITE_MONITOR ? "Monitor (" + Engine.taxaDoMonitor() + ")"
                    : v == Configs.SEM_LIMITE ? "Unlimited"
                    : v > Engine.taxaDoMonitor() ? v + " !" : String.valueOf(v);
        }

        List<OptionRow> video = new ArrayList<>();
        video.add(OptionRow.cycle("Resolution", () -> pendResolucao, v -> pendResolucao = v, rotulosDeResolucao)
                .desc("Size of the game image, in screen pixels. * marks your monitor, the sharpest in fullscreen. "
                        + "Takes effect after Apply."));
        video.add(OptionRow.toggle("Fullscreen", () -> pendTelaCheia, v -> pendTelaCheia = v)
                .desc("Fills the whole screen with a borderless window. Needs Apply. F11 also toggles it."));
        video.add(OptionRow.cycle("Frame rate", Configuracoes::indiceFps,
                v -> Configs.setMaxFrames(FPS_VALORES[v]), fps)
                .desc("Frame limit when VSync is off. Monitor matches your screen and is the smoothest; ! marks "
                        + "limits above your monitor, which only add tearing. "
                        + "The game plays the same at any frame rate."));
        video.add(OptionRow.toggle("Smooth graphics",
                () -> Configs.isNeatGraphics() ? 1 : 0, v -> Configs.setNeatGraphics(v != 0))
                .desc("Antialiasing and smooth filtering. Softens the pixel art; off keeps the sharp look."));
        video.add(OptionRow.stepper("Camera zoom", Configs::Zoom, Configs::setZoom, 50, 160, 5, "%")
                .desc("How close the camera is to the cat. Lower shows more of the arena; higher is more immersive."));
        video.add(OptionRow.toggle("Vignette",
                () -> Configs.Vignette() ? 1 : 0, v -> Configs.setVignette(v != 0))
                .desc("Darkens the corners of the screen during gameplay, to focus the eye on the center."));
        video.add(OptionRow.toggle("VSync",
                () -> Configs.VSync() ? 1 : 0, v -> Configs.setVSync(v != 0))
                .desc("Syncs every frame to the monitor refresh. Smoothest motion, no tearing; "
                        + "the frame rate limit is ignored while it is on."));
        video.add(OptionRow.toggle("Lighting",
                () -> Configs.Lighting() ? 1 : 0, v -> Configs.setLighting(v != 0))
                .desc("Soft shadows under everything and dynamic light from the cat, torches and chests."));

        List<OptionRow> audio = new ArrayList<>();
        audio.add(OptionRow.slider("Master", Configs::Master, v -> {
            Configs.setMaster(v);
            Sound.updateVolumes();
        }, 0, 100, 5, "%").desc("Overall loudness of the game. Scales music and sound effects together."));
        audio.add(OptionRow.slider("Music", Configs::Music, v -> {
            Configs.setMusic(v);
            Sound.updateVolumes();     // aplica ao vivo: é preciso ouvir para ajustar
        }, 0, 100, 5, "%").desc("Volume of the background music. Applies immediately."));
        audio.add(OptionRow.slider("Sound effects", Configs::Volum, Configs::setVolum, 0, 100, 5, "%")
                .desc("Volume of hits, footsteps, items and enemies. Menu sounds included."));
        // Toca a faixa escolhida na hora: escolher musica de ouvido e o unico
        // jeito que funciona — ler "Spooky" numa lista nao diz nada a ninguem.
        audio.add(OptionRow.cycle("Battle music", Configs::BattleMusic, v -> {
            Configs.setBattleMusic(v);
            if (!auditando) {
                // O que estava tocando fica PAUSADO, nunca parado: parar rebobina
                // na TinySound, e faixa rebobinada volta do comeco.
                Sound.pausarParaAudicao();
                auditando = true;
            }
            Musics escolhida = Musics.combate();
            if (previa != null && previa != escolhida) {
                Sound.stop(previa);          // so a previa anterior e cortada
            }
            previa = escolhida;
            Sound.tocarOuRetomar(escolhida);
        }, Musics.COMBATE_NOMES)
                .desc("Track for normal arenas. Plays here so you can compare. Boss fights have their own."));

        List<OptionRow> ui = new ArrayList<>();
        // TAMANHOS EM PASSOS A PARTIR DO PADRÃO DA RESOLUÇÃO, um bloco por tamanho
        // que se vê e só os que cabem na tela atual (ver EscalaDaInterface). Eram
        // por cento de 50 a 200: com a escala inteira, a maioria dos passos não
        // mudava nada.
        ui.add(OptionRow.tamanho("Menu size",
                () -> Configs.faixaDosMenus().efetivo(Configs.MenuTamanho()), Configs::setMenuTamanho,
                () -> Configs.faixaDosMenus().menor(), () -> Configs.faixaDosMenus().maior(),
                EscalaDaInterface::nome)
                .desc("Size of menus, buttons and cards. Default follows your resolution; "
                        + "only the sizes that fit this screen are offered."));
        ui.add(OptionRow.tamanho("HUD size",
                () -> Configs.faixaDoHud().efetivo(Configs.HudTamanho()), Configs::setHudTamanho,
                () -> Configs.faixaDoHud().menor(), () -> Configs.faixaDoHud().maior(),
                EscalaDaInterface::nome)
                .desc("Size of the in-game HUD: life, clock, XP, hotbar. Default follows your resolution; "
                        + "larger sizes appear only when they fit without overlapping."));
        ui.add(OptionRow.stepper("Margin", Configs::MargemDeReferencia, Configs::setMargemDeReferencia,
                0, EscalaDaInterface.MARGEM_MAXIMA, 1)
                .desc("Distance between the interface and the edges of the screen. Scales with your resolution."));
        ui.add(OptionRow.acao("Reset interface", Configs::restaurarInterface)
                .desc("Back to the default sizes and margin for your resolution."));
        ui.add(OptionRow.toggle("Show hitboxes",
                () -> Configs.Hitboxes() ? 1 : 0, v -> Debugging.mostrarHitBox(v != 0))
                .desc("Draws the collision box over entities. Same as pressing F4 in game."));
        ui.add(OptionRow.toggle("Controller vibration",
                () -> Configs.Rumble() ? 1 : 0, v -> Configs.setRumble(v != 0))
                .desc("Rumble on an Xbox controller when the cat takes damage."));
        ui.add(OptionRow.toggle("Allow intro skip",
                () -> Configs.PermitirPularAbertura() ? 1 : 0, v -> Configs.setPermitirPularAbertura(v != 0))
                .desc("Allow a key, click or controller button to skip the opening. Off by default."));
        // APAGAR A PROGRESSÃO SÓ DO MENU PRINCIPAL. Na pausa, com uma corrida
        // aberta, o gato em jogo já carrega as melhorias aplicadas; apagar ali
        // deixaria a corrida com números que o save não tem mais.
        if (!naCorrida) {
            ui.add(OptionRow.acao("Reset progress",
                    () -> confirmacao.perguntar("Erase all progress?",
                            new String[]{"Coins and upgrades of every cat.",
                                    "Saved runs are kept. This cannot be undone."},
                            () -> FalhaDeSave.tentar(com.retronova.game.save.Progresso::apagarTudo)))
                    .desc("Erase the coins and permanent upgrades of all cats. Asks first."));
        }

        // CONTROLS: só a escolha do aparelho é uma linha; o resto é o painel de
        // controles logo abaixo dela (ver PainelDeControles).
        List<OptionRow> controle = new ArrayList<>();
        controle.add(OptionRow.cycle("Device", controles::visao, controles::setVisao,
                PainelDeControles.rotulosDasVisoes())
                .desc("Show the keyboard and mouse or the controller. Opens on the one you used last."));

        linhas.add(video);
        linhas.add(audio);
        linhas.add(ui);
        linhas.add(controle);
    }

    private boolean naAbaDeControles() {
        return abas.atual() == linhas.size() - 1;
    }

    /** O painel de controles conta como um item a mais no foco, logo depois das linhas da aba. */
    private int extras() {
        return naAbaDeControles() ? 1 : 0;
    }

    private int focoDoPainel() {
        return naAbaDeControles() ? atuais().size() : -1;
    }

    private static int indiceFps() {
        int atual = Configs.MaxFrames();
        for (int i = 0; i < FPS_VALORES.length; i++) {
            if (FPS_VALORES[i] == atual) {
                return i;
            }
        }
        return 0;   // valor desconhecido cai no do monitor
    }

    /** A resolução em vigor na lista deste monitor; a do monitor se ela não estiver lá. */
    private int indiceDaResolucaoAtual() {
        int[] atual = Engine.window.resolucao();
        int i = resolucoes.indexOf(new java.awt.Dimension(atual[0], atual[1]));
        return i >= 0 ? i : resolucoes.size() - 1;
    }

    private boolean temPendencia() {
        return pendResolucao != indiceDaResolucaoAtual()
                || (pendTelaCheia != 0) != Configs.Fullscreen();
    }

    /** Resolução e tela cheia gravadas juntas e aplicadas numa passada só (ver Window.aplicarVideo). */
    private void aplicarPendentes() {
        if (!temPendencia()) {
            return;
        }
        java.awt.Dimension r = resolucoes.get(pendResolucao);
        Configs.setResolucao(r.width, r.height);
        Configs.setFullscreen(pendTelaCheia != 0);
        Engine.window.aplicarVideo();
    }

    /**
     * Volta para quem abriu — a pausa ou a tela inicial —, encerrando antes a
     * prévia de música.
     *
     * DENTRO DO JOGO a prévia não é cortada: ela vira a música da partida e segue
     * tocando sem emenda. Fora do jogo ela é cortada e a música do menu volta de
     * onde parou. Nos dois casos nada recomeça do zero — ver Sound.encerrarAudicao.
     */
    @Override
    protected void voltar() {
        encerrarAudicao(Engine.getACTIVITY() instanceof Game);
        super.voltar();
    }

    private void encerrarAudicao(boolean seguir) {
        if (auditando) {
            Sound.encerrarAudicao(previa, seguir);
            previa = null;
            auditando = false;
        }
    }

    private List<OptionRow> atuais() {
        return linhas.get(abas.atual());
    }

    // ---------------------------------------------------------------- layout

    /** Maior número de linhas entre as abas. Define uma altura fixa para o painel. */
    private int maxLinhas() {
        int maior = 0;
        for (List<OptionRow> aba : linhas) {
            maior = Math.max(maior, aba.size());
        }
        return maior;
    }

    /**
     * Abas, painel e botões são medidos primeiro e então centralizados na área de
     * conteúdo, entre o título e o rodapé.
     */
    @Override
    protected void posicionar() {
        int s = Configs.UiScale();
        Rectangle area = areaDoConteudo();
        // 288 E NÃO 240: a aba Controls põe o desenho do controle entre duas colunas
        // de rótulos, e em 240 os rótulos dos menus ("Skip level-up") não cabiam.
        int larguraPainel = Math.min(Engine.window.largura() - 16 * s, LARGURA_DO_PAINEL * s);
        int x = Engine.window.largura() / 2 - larguraPainel / 2;

        int alturaAbas = Abas.altura();
        // RESPIRO. Antes era tudo de 2 a 6 pixels: as linhas encostavam umas nas
        // outras e na borda do painel, e a tela inteira lia como amontoada.
        int entreLinhas = 7 * s;
        int margemX = 12 * s;
        int margemTopo = 10 * s;
        int margemBase = 12 * s;
        int entreBlocos = 10 * s;
        int alturaBotao = Button.preferredHeight();
        // A altura vem da aba MAIS LONGA, e não da atual: o painel e os botões ficam
        // parados ao alternar entre Video, Audio e Interface.
        int alturaLinhas, total;
        // O RESPIRO CEDE ANTES DO BOTÃO. Se não couber, os vãos encolhem um passo
        // por vez, em vez de o Back sair da janela.
        while (true) {
            alturaLinhas = margemTopo + maxLinhas() * (OptionRow.height() + entreLinhas)
                    - entreLinhas + margemBase;
            total = alturaAbas + alturaLinhas + entreBlocos + alturaBotao;
            if (total <= area.height || entreLinhas <= 2 * s) {
                break;
            }
            entreLinhas -= s;
            margemTopo = Math.max(4 * s, margemTopo - s);
            margemBase = Math.max(4 * s, margemBase - s);
            entreBlocos = Math.max(6 * s, entreBlocos - s);
        }

        // A lista do teclado da aba Controls é a mais alta de todas: o painel
        // cresce por ela, em todas as abas, para não mudar de altura na troca. Só
        // até onde a tela deixa; numa tela baixa a lista rola (PainelDeControles).
        int dosControles = margemTopo + OptionRow.height() + entreLinhas
                + PainelDeControles.alturaNecessaria() * s + margemBase;
        int cresce = Math.max(0, Math.min(dosControles - alturaLinhas, area.height - total));
        alturaLinhas += cresce;
        total += cresce;

        int yAbas = area.y + Math.max(0, (area.height - total) / 2);
        abas.posicionar(x, yAbas, larguraPainel);
        this.painel = new Rectangle(x, yAbas + alturaAbas, larguraPainel, alturaLinhas);

        // Posiciona as linhas de TODAS as abas, não só a visível: a aba recém-aberta
        // desenhava um quadro com as coordenadas zeradas — a piscada no canto.
        int y = yAbas + alturaAbas + margemTopo;
        for (List<OptionRow> aba : linhas) {
            for (int i = 0; i < aba.size(); i++) {
                aba.get(i).setBounds(x + margemX, y + i * (OptionRow.height() + entreLinhas),
                        larguraPainel - 2 * margemX);
            }
        }
        if (naCorrida) {
            Rectangle ultima = linhas.getFirst().getLast().getBounds();
            yDoResumo = ultima.y + ultima.height + entreLinhas;
        }
        Rectangle device = linhas.getLast().getFirst().getBounds();
        int topoDosControles = device.y + device.height + entreLinhas;
        controles.posicionar(new Rectangle(x + margemX, topoDosControles, larguraPainel - 2 * margemX,
                painel.y + painel.height - margemBase - topoDosControles));

        int larguraBotao = Button.preferredWidth();
        int yBotoes = yAbas + alturaAbas + alturaLinhas + entreBlocos;
        aplicar.setBounds(new Rectangle(x, yBotoes, larguraBotao, alturaBotao));
        voltar.setBounds(new Rectangle(x + larguraPainel - larguraBotao, yBotoes,
                larguraBotao, alturaBotao));
    }

    // ------------------------------------------------------------------ tick

    /** Índice do foco que cai no Apply, ou -1 quando ele não está na tela. */
    private int focoDoAplicar() {
        return temPendencia() ? atuais().size() + extras() : -1;
    }

    private int focoDoVoltar() {
        return atuais().size() + extras() + (temPendencia() ? 1 : 0);
    }

    @Override
    protected void atualizar() {
        if (abas.tick()) {
            foco = 0;
            if (naAbaDeControles()) {
                controles.mostrarUltimoDispositivo();
            }
        }
        navegarPeloTeclado();
        if (naAbaDeControles()) {
            controles.tick(foco == focoDoPainel());
        }

        List<OptionRow> rows = atuais();
        for (int i = 0; i < rows.size(); i++) {
            OptionRow linha = rows.get(i);
            linha.setFocado(foco == i);
            linha.tick();
            if (linha.estaSobreControles()) {
                Engine.window.pointing();
            }
        }

        if (temPendencia()) {
            aplicar.setFocused(foco == focoDoAplicar());
            aplicar.tick();
            if (aplicar.isHovered()) {
                Engine.window.pointing();
            }
        }
        voltar.setFocused(foco == focoDoVoltar());
        voltar.tick();
        if (voltar.isHovered()) {
            Engine.window.pointing();
        }
    }

    private void navegarPeloTeclado() {
        List<OptionRow> rows = atuais();
        int quantos = focoDoVoltar() + 1;
        // DENTRO DO PAINEL DE CONTROLES as setas são dele: andam pelas ações e só
        // devolvem o foco ao passar da primeira ou da última.
        if (foco == focoDoPainel()) {
            int sai = controles.navegar();
            if (sai != 0) {
                foco += sai;
            }
            return;
        }
        int antes = foco;
        navegarNaLista(rows, quantos);
        if (foco == focoDoPainel() && foco != antes) {
            controles.entrar(antes < foco);
        }
    }

    private void navegarNaLista(List<OptionRow> rows, int quantos) {
        if (KeyBoard.KeyPressed("Down")) {
            foco = (foco + 1) % quantos;
        } else if (KeyBoard.KeyPressed("Up")) {
            foco = (foco <= 0 ? quantos : foco) - 1;
        } else if (KeyBoard.KeyPressed("Right") && foco < rows.size()) {
            rows.get(foco).ajustarPeloTeclado(1);
        } else if (KeyBoard.KeyPressed("Left") && foco < rows.size()) {
            rows.get(foco).ajustarPeloTeclado(-1);
        } else if (KeyBoard.KeyPressed("Enter")) {
            if (foco < rows.size()) {
                rows.get(foco).ativar();
            } else if (foco == focoDoAplicar()) {
                aplicar.activate();
            } else if (foco == focoDoVoltar()) {
                voltar.activate();
            }
        }
        foco = Math.min(foco, focoDoVoltar());
    }

    @Override
    protected List<Rodape.Dica> dicas() {
        Rodape.Dica sair = naCorrida ? new Rodape.Dica("Esc", "B", "Resume") : Rodape.VOLTAR;
        if (naAbaDeControles() && foco == focoDoPainel()) {
            return List.of(new Rodape.Dica("Arrows", "D-pad", "Browse"), Rodape.ABAS, sair);
        }
        return List.of(naAbaDaCorrida() ? Rodape.SELECIONAR : Rodape.AJUSTAR, Rodape.ABAS, sair);
    }

    // ---------------------------------------------------------------- render

    @Override
    protected void desenharConteudo(Graphics2D g) {
        desenharPainel(g);
        abas.render(g);
        desenharLinhas(g);
        if (temPendencia()) {
            aplicar.render(g);
            desenharAvisoPendente(g);
        }
        voltar.render(g);
    }

    @Override
    protected void desenharPorCima(Graphics2D g) {
        desenharTooltip(g);
    }

    /**
     * As opções da aba atual. Logo depois de uma troca elas entram deslizando do
     * lado da aba nova e aparecem num fade, presas ao painel para não vazarem por
     * cima da borda durante o deslize.
     */
    private void desenharLinhas(Graphics2D g) {
        double p = abas.progressoDaTroca();
        if (p >= 1) {
            for (OptionRow linha : atuais()) {
                linha.render(g);
            }
            if (naAbaDaCorrida()) {
                desenharResumo(g);
            }
            if (naAbaDeControles()) {
                controles.render(g);
            }
            return;
        }
        int s = Configs.UiScale();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clipRect(painel.x + s, painel.y + s, painel.width - 2 * s, painel.height - 2 * s);
        g2.translate((int) Math.round(abas.direcao() * (1 - p) * 24 * s), 0);
        float base = g.getComposite() instanceof AlphaComposite a ? a.getAlpha() : 1f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) p * base));
        for (OptionRow linha : atuais()) {
            linha.render(g2);
        }
        if (naAbaDaCorrida()) {
            desenharResumo(g2);
        }
        if (naAbaDeControles()) {
            controles.render(g2);
        }
        g2.dispose();
    }

    /**
     * Embaixo das ações da aba Run: um fio, o resumo curto da corrida e o aviso
     * do save.
     *
     * O RESUMO É CURTO DE PROPÓSITO: tempo, nível e elemento, o que se quer saber
     * de relance antes de decidir se sai. O detalhe (atributos, inventário) é da
     * tela de Status, na tecla E — repetir aqui seria manter duas fichas.
     *
     * O AVISO EXISTE porque não há botão de "Save and Quit": sair pelo Main Menu
     * ou pelo Quit já grava. Sem esta linha, sair parece perder tudo, e quem acha
     * que vai perder tudo não sai — fica jogando cansado.
     */
    private void desenharResumo(Graphics2D g) {
        int s = Configs.UiScale();
        int esq = painel.x + 12 * s, larg = painel.width - 24 * s;
        int y = yDoResumo;
        g.setColor(Palette.OUTLINE);
        g.fillRect(esq, y, larg, s);
        int cx = painel.x + painel.width / 2;

        String resumo = resumoDaCorrida();
        if (!resumo.isEmpty()) {
            Font f = FontHandler.font(FontHandler.Game, 6f * s);
            y += 12 * s;
            TextFx.draw(g, resumo, f, cx - FontHandler.getWidth(resumo, f) / 2, y, Palette.TEXT, s);
        }
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        y += 10 * s;
        for (String linha : AVISO) {
            TextFx.draw(g, linha, fonte, cx - FontHandler.getWidth(linha, fonte) / 2, y, Palette.LIGHT, s);
            y += 8 * s;
        }
    }

    private static String resumoDaCorrida() {
        Player gato = Game.getPlayer();
        if (gato == null) {
            return "";
        }
        List<String> partes = new ArrayList<>();
        if (Game.getMap() instanceof Arena && Game.getWave() != null) {
            int seg = Game.getWave().getSeconds();
            partes.add(String.format("%02d:%02d", seg / 60, seg % 60));
        }
        partes.add("Level " + gato.getLevel());
        if (gato.elemento() != Elemento.NENHUM) {
            partes.add(gato.elemento().rotulo());
        }
        return String.join("   ", partes);
    }

    /** Painel de fundo atrás das linhas. Altura fixa, igual em todas as abas. */
    private void desenharPainel(Graphics2D g) {
        TelaDeMenu.desenharPainel(g, painel);
    }

    private void desenharAvisoPendente(Graphics2D g) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        String aviso = "Apply resizes the window";
        int x = aplicar.getBounds().x;          // alinhado à esquerda, sob o botão
        int y = aplicar.getBounds().y - 2 * s;
        g.setFont(fonte);
        TextFx.draw(g, aviso, x, y, Palette.LIGHT, s);
    }

    /**
     * Balão de ajuda da linha sob o mouse — ou da linha em foco, no teclado e no
     * controle, que antes ficavam sem explicação nenhuma. Desenhado por último,
     * acima de tudo, e preso dentro da janela para não vazar pelas bordas.
     */
    private void desenharTooltip(Graphics2D g) {
        String texto = null;
        Rectangle alvo = null;
        boolean peloMouse = false;
        for (OptionRow linha : atuais()) {
            if (linha.estaSobreLinha() && !linha.getDescricao().isEmpty()) {
                texto = linha.getDescricao();
                alvo = linha.getBounds();
                peloMouse = true;
                break;
            }
        }
        // NA ABA CONTROLS O BALÃO É SÓ DO MOUSE. Pelo foco ele ficaria parado em
        // cima do desenho do controle ou da coluna vizinha — o painel ocupa a tela
        // toda na largura —, e o rótulo e a peça acesa já dizem o que é.
        if (texto == null && naAbaDeControles() && controles.ajudaPeloMouse()) {
            texto = controles.descricao();
            alvo = controles.alvoDaDescricao();
            peloMouse = true;
        }
        if (texto == null && !naAbaDeControles() && com.retronova.engine.inputs.Entrada.porFoco()
                && foco < atuais().size()) {
            texto = atuais().get(foco).getDescricao();
            alvo = atuais().get(foco).getBounds();
        }
        if (texto == null || texto.isEmpty() || alvo == null) {
            return;
        }
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 5f * s);
        int larguraMax = 96 * s;
        List<String> linhasTexto = quebrar(texto, fonte, larguraMax);

        int alturaLinha = 7 * s;
        int pad = 4 * s;
        int largura = 0;
        for (String t : linhasTexto) {
            largura = Math.max(largura, FontHandler.getWidth(t, fonte));
        }
        int caixaW = largura + pad * 2;
        int caixaH = linhasTexto.size() * alturaLinha + pad * 2;

        int x, y;
        if (peloMouse) {
            x = Mouse.getX() + 4 * s;
            y = Mouse.getY() + 6 * s;
        } else {
            // Pelo foco, o balão fica ao lado do painel, na altura da linha: à
            // direita se couber, senão à esquerda. Só sem espaço nos dois lados ele
            // desce para baixo da linha — e aí cobre a de baixo, o mal menor.
            Rectangle r = alvo;
            y = r.y;
            if (painel.x + painel.width + 4 * s + caixaW <= Engine.window.largura() - 2 * s) {
                x = painel.x + painel.width + 4 * s;
            } else if (painel.x - 4 * s - caixaW >= 2 * s) {
                x = painel.x - 4 * s - caixaW;
            } else {
                x = painel.x + painel.width - caixaW - 4 * s;
                y = r.y + r.height + 2 * s;
            }
        }
        x = Math.min(x, Engine.window.largura() - caixaW - 2 * s);
        y = Math.min(y, Engine.window.altura() - caixaH - 2 * s);
        x = Math.max(2 * s, x);
        y = Math.max(2 * s, y);

        g.setColor(Palette.OUTLINE);
        g.fillRect(x, y, caixaW, caixaH);
        g.setColor(Palette.DEEP);
        g.fillRect(x + s, y + s, caixaW - 2 * s, caixaH - 2 * s);
        g.setColor(Palette.MAIN);
        g.fillRect(x + s, y + s, caixaW - 2 * s, s);

        g.setFont(fonte);
        for (int i = 0; i < linhasTexto.size(); i++) {
            int ty = y + pad + (i + 1) * alturaLinha - 2 * s;
            TextFx.draw(g, linhasTexto.get(i), x + pad, ty, Palette.TEXT, s);
        }
    }

    /** Quebra o texto em linhas que caibam na largura dada. */
    private List<String> quebrar(String texto, Font fonte, int largura) {
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
