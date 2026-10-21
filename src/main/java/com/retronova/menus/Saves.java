package com.retronova.menus;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Corrida;
import com.retronova.game.Game;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Os três espaços de corrida.
 *
 * POR QUE ESTA TELA EXISTE, e não dois botões no menu.
 *
 * Com um save só dava para resolver tudo com botões: "Continue" quando havia
 * corrida, "Play" quando não. Com três, essa saída acaba — seriam três botões de
 * continuar mais um de começar, todos com o mesmo rótulo e nenhuma forma de dizer
 * qual é qual sem entrar. O que o jogador precisa aqui não é escolher uma AÇÃO, é
 * escolher uma CORRIDA; e corrida se escolhe vendo.
 *
 * Por isso cada espaço se descreve por inteiro — gato, nível e minuto da corrida —
 * e o clique faz a coisa óbvia para aquele espaço: ocupado retoma, vazio
 * começa. É o mesmo desenho que console usa desde sempre, e ninguém precisa
 * aprender.
 *
 * APAGAR FICA SEPARADO DO CLIQUE PRINCIPAL, num alvo próprio e pequeno, e ainda
 * pergunta antes. Apagar é a única ação aqui que destrói alguma coisa, e a única
 * que não pode acontecer por engano de mira.
 */
public class Saves extends TelaDeMenu {

    private final Button voltar;

    /** As caixas de cada espaço, e o alvo de apagar dentro delas. */
    private final Rectangle[] caixas = new Rectangle[Corrida.ESPACOS];
    private final Rectangle[] lixeiras = new Rectangle[Corrida.ESPACOS];
    private final com.retronova.menus.shared.Realce.Estado[] realces = {
            new com.retronova.menus.shared.Realce.Estado(), new com.retronova.menus.shared.Realce.Estado(),
            new com.retronova.menus.shared.Realce.Estado()};

    /** O que cada espaço guarda agora, ou null. Relido de tempos em tempos. */
    private final Corrida[] corridas = new Corrida[Corrida.ESPACOS];

    /** Espaço sob foco de teclado; -1 quando o mouse manda. */
    private int foco = -1;
    /** O foco está no X de apagar do espaço em foco, e não na caixa. */
    private boolean focoNoApagar;

    /**
     * Quantos ticks entre uma releitura do disco e outra.
     *
     * A tela não é reconstruída quando se volta para ela — o mesmo problema que o
     * menu tinha com o "Continue". Reler de meio em meio segundo resolve para todos
     * os caminhos de volta sem que nenhum deles precise avisar esta tela.
     */
    private static final int ENTRE_LEITURAS = 30;
    private int desdeALeitura = ENTRE_LEITURAS;

    public Saves() {
        super("Your Runs");
        this.voltar = new Button(0, 0, 0, 0, "Back", b -> voltar());
        for (int i = 0; i < Corrida.ESPACOS; i++) {
            caixas[i] = new Rectangle();
            lixeiras[i] = new Rectangle();
        }
        reler();
        posicionar();
    }

    private void reler() {
        for (int i = 0; i < Corrida.ESPACOS; i++) {
            corridas[i] = Corrida.espiar(i + 1);
        }
    }

    // ---------------------------------------------------------------- medidas
    //
    // Tudo em PIXELS NATIVOS, multiplicado pela escala na hora de usar. Foi assim
    // que a primeira versao errou: os numeros foram escolhidos no escuro, e numa
    // janela grande a escala e seis — um texto de "8" vira quarenta e oito pixels,
    // e o que parecia folga virava estouro. Medidas nativas sao conferiveis
    // desenhando a linha fora do jogo, que foi como estas foram acertadas.

    /** Altura da faixa de orelhas da moldura. Nada de texto pode subir ate aqui. */
    private static final int ORELHAS = 6;

    private static final int ALTURA = 32;
    private static final int LARGURA = 170;

    /** Coluna do numero, a esquerda do risco. */
    private static final int GUTTER = 20;
    private static final int MARGEM = 6;
    private static final int LIXEIRA = 13;

    private static int larguraDaCaixa(int s) {
        // Nunca mais larga que a janela: numa tela estreita a moldura sairia pelos
        // dois lados, e o corte por reticencias so protege o texto, nao a caixa.
        return Math.min(LARGURA * s, Engine.window.getWidth() - 16 * s);
    }

    private static int alturaDaCaixa(int s) {
        return ALTURA * s;
    }

    /** Altura do titulo e folga entre blocos, em pixels nativos. */
    private static final int FOLGA = 5;

    /**
     * Monta a coluna inteira a partir de UMA conta de altura, centralizada na area
     * de conteudo (entre o titulo, que agora e o da moldura comum, e o rodape).
     * Somando tudo antes de centralizar, o conjunto cabe por construcao e nao por
     * sorte de resolucao.
     */
    @Override
    protected void posicionar() {
        int s = Configs.UiScale();
        int largura = larguraDaCaixa(s);
        int altura = alturaDaCaixa(s);
        int alturaDoVoltar = Button.preferredHeight();
        Rectangle area = areaDoConteudo();

        int total = Corrida.ESPACOS * altura + (Corrida.ESPACOS - 1) * FOLGA * s
                + 8 * s + alturaDoVoltar;
        int x = Engine.window.getWidth() / 2 - largura / 2;
        int y = area.y + Math.max(0, (area.height - total) / 2);
        for (int i = 0; i < Corrida.ESPACOS; i++) {
            caixas[i].setBounds(x, y + i * (altura + FOLGA * s), largura, altura);
            int lado = LIXEIRA * s;
            int corpoY = caixas[i].y + ORELHAS * s;
            int meio = corpoY + (altura - ORELHAS * s) / 2;
            lixeiras[i].setBounds(caixas[i].x + largura - (MARGEM + LIXEIRA) * s,
                    meio - lado / 2, lado, lado);
        }
        int yVoltar = y + Corrida.ESPACOS * (altura + FOLGA * s) + 3 * s;
        voltar.setBounds(new Rectangle(
                Engine.window.getWidth() / 2 - Button.preferredWidth() / 2,
                yVoltar, Button.preferredWidth(), alturaDoVoltar));
    }

    // ------------------------------------------------------------------ tick

    /** Voltou para esta tela (de uma corrida, da selecao de gato): rele na hora. */
    @Override
    protected void aoEntrar() {
        reler();
        desdeALeitura = 0;
    }

    @Override
    protected void atualizar() {
        if (++desdeALeitura >= ENTRE_LEITURAS) {
            desdeALeitura = 0;
            reler();
        }
        navegarPeloTeclado();
        for (int i = 0; i < Corrida.ESPACOS; i++) {
            realces[i].tick(foco == i
                    || (com.retronova.engine.inputs.Entrada.porMouse() && Mouse.on(caixas[i])));
        }
        voltar.tick();
        if (voltar.isHovered()) {
            foco = -1;
        }
        voltar.setFocused(foco == Corrida.ESPACOS);

        for (int i = 0; i < Corrida.ESPACOS; i++) {
            if (com.retronova.engine.inputs.Entrada.porMouse() && Mouse.on(caixas[i])) {
                foco = i;
                focoNoApagar = false;
                Engine.window.pointing();
            }
            // A LIXEIRA E TESTADA ANTES DA CAIXA, porque esta dentro dela: na ordem
            // inversa, todo clique em apagar seria tambem um clique em jogar, e o
            // jogador entraria na corrida que estava tentando descartar.
            if (corridas[i] != null && Mouse.clickOn(Mouse_Button.LEFT, lixeiras[i])) {
                perguntarSeApaga(i + 1);
                return;
            }
            if (Mouse.clickOn(Mouse_Button.LEFT, caixas[i])) {
                acionar(i + 1);
                return;
            }
        }
    }

    /**
     * O X DE APAGAR TAMBEM SE ALCANCA SEM MOUSE. Ele mora dentro da caixa, e a
     * navegacao so subia e descia entre caixas — pelo teclado ou pelo controle
     * nao havia como chegar nele. Agora a direita leva o foco ao X e a esquerda
     * volta; o F (o X do controle, que e o proprio desenho do botao) apaga direto.
     * Os dois caminhos ainda passam pela pergunta, que abre com o foco no No.
     */
    private void navegarPeloTeclado() {
        int quantos = Corrida.ESPACOS + 1;          // os espaços mais o "Back"
        boolean naCorrida = foco >= 0 && foco < Corrida.ESPACOS && corridas[foco] != null;
        if (!naCorrida) {
            focoNoApagar = false;       // a corrida foi apagada: o X sumiu junto
        }
        if (KeyBoard.KeyPressed("Down") || KeyBoard.KeyPressed("S")) {
            foco = foco < 0 ? 0 : (foco + 1) % quantos;
            focoNoApagar = false;
        } else if (KeyBoard.KeyPressed("Up") || KeyBoard.KeyPressed("W")) {
            foco = (foco <= 0 ? quantos : foco) - 1;
            focoNoApagar = false;
        } else if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("D")) {
            focoNoApagar = naCorrida;
        } else if (KeyBoard.KeyPressed("Left") || KeyBoard.KeyPressed("A")) {
            focoNoApagar = false;
        } else if (KeyBoard.KeyPressed("F") && naCorrida) {
            perguntarSeApaga(foco + 1);
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            if (foco == Corrida.ESPACOS) {
                voltar.activate();
            } else if (focoNoApagar && naCorrida) {
                perguntarSeApaga(foco + 1);
            } else {
                acionar(foco + 1);
            }
        }
    }

    /**
     * O clique faz a coisa óbvia para aquele espaço.
     *
     * Ocupado retoma, vazio começa. Não há um botão "carregar" e outro "novo jogo"
     * porque nunca há dúvida: um espaço com corrida dentro só pode ser continuado,
     * e um vazio só pode ser começado. Dois botões onde só um é possível de cada
     * vez é trabalho para o jogador sem escolha em troca.
     */
    private void acionar(int espaco) {
        Corrida corrida = corridas[espaco - 1];
        if (corrida == null) {
            Sound.play(Sounds.Button);
            abrir(new Personagens(espaco));
            return;
        }
        // O CLIQUE E IRREVERSIVEL, ainda que nao pareca: retomar consome o save, e a
        // partir dali a unica forma de voltar aquele ponto e jogar ate ele de novo.
        confirmacao.perguntar("Continue your run?", corrida.resumo(),
                () -> retomar(espaco));
    }

    private void retomar(int espaco) {
        Corrida corrida = Corrida.consumir(espaco);
        if (corrida == null) {
            reler();                // sumiu no meio do caminho; a tela se corrige
            return;
        }
        // A MUSICA DO MENU MORRE AQUI. O caminho da partida nova passa pela selecao
        // de personagem, que ja faz isto — e era o unico lugar que fazia. Retomar
        // pula aquela tela, e sem esta linha a faixa do menu seguia tocando POR CIMA
        // da trilha de combate.
        Sound.stopAll();
        Engine.heapActivity(Game.retomar(corrida, espaco), () -> { });
    }

    private void perguntarSeApaga(int espaco) {
        Corrida corrida = corridas[espaco - 1];
        if (corrida == null) {
            return;
        }
        String[] detalhes = new String[corrida.resumo().length + 1];
        detalhes[0] = "This cannot be undone:";
        System.arraycopy(corrida.resumo(), 0, detalhes, 1, corrida.resumo().length);
        confirmacao.perguntar("Delete this run?", detalhes, () -> {
            Corrida.apagar(espaco);
            reler();
        });
    }
    // ---------------------------------------------------------------- render

    @Override
    protected List<Rodape.Dica> dicas() {
        return List.of(Rodape.SELECIONAR, new Rodape.Dica("F", "X", "Delete"), Rodape.VOLTAR);
    }

    @Override
    protected void desenharConteudo(Graphics2D g) {
        int s = Configs.UiScale();
        for (int i = 0; i < Corrida.ESPACOS; i++) {
            desenharEspaco(g, i, s);
        }
        voltar.render(g);
    }
    /**
     * Uma linha da lista: numero, nome, ficha e o alvo de apagar.
     *
     * O QUE ESTAVA ERRADO NA PRIMEIRA VERSAO, e vale registrar porque a causa se
     * repete: o texto era escrito a partir do topo da caixa, e o topo da caixa nao
     * e o topo do CONTEUDO — a moldura do jogo tem seis pixels de orelhas de gato
     * ali. O nome era desenhado por cima delas.
     *
     * E a ficha era uma frase so, "Easy - Room 2 - 0:47 played", ancorada a
     * esquerda. Frase de comprimento variavel ancorada num lado unico exige uma
     * caixa larga o bastante para o PIOR caso, e vazava assim que o pior caso
     * aparecia. Agora sao duas ancoras — a ficha a esquerda, o tempo a direita — e
     * o espaco que sobra vira folga no meio em vez de estouro. O que ainda assim
     * nao couber e cortado com reticencias, que e uma perda legivel.
     */
    private void desenharEspaco(Graphics2D g, int i, int s) {
        boolean ativo = foco == i
                || (com.retronova.engine.inputs.Entrada.porMouse() && Mouse.on(caixas[i]));
        // O REALCE DOS BOTOES (ver Realce): a caixa e uma moldura de botao, entao
        // cresce, brilha e clareia como eles. No foco das setas, as setas laterais.
        float intensidade = realces[i].noQuadro();
        Rectangle caixa = com.retronova.menus.shared.Realce.crescer(caixas[i], intensidade, s);
        Rectangle corpoDaCaixa = new Rectangle(caixa.x, caixa.y + (ORELHAS - 1) * s,
                caixa.width, caixa.height - (ORELHAS - 1) * s);
        com.retronova.menus.shared.Realce.brilho(g, corpoDaCaixa, intensidade, s);
        Button.frame(ativo).draw(g, ativo ? 1 : 0,
                caixa.x, caixa.y, caixa.width, caixa.height, s);
        if (foco == i && !focoNoApagar && com.retronova.engine.inputs.Entrada.porFoco()) {
            com.retronova.menus.shared.Realce.marcadorDeFoco(g, corpoDaCaixa, s);
        }

        int corpoY = caixa.y + ORELHAS * s;
        int corpoH = caixa.height - ORELHAS * s;
        int meio = corpoY + corpoH / 2;

        // O NUMERO DO ESPACO, sempre visivel. E o unico jeito de o jogador dizer
        // "perdi a corrida do dois" e de a frase significar alguma coisa.
        Font fonteDoNumero = FontHandler.font(FontHandler.Game, 10f * s);
        String numero = String.valueOf(i + 1);
        int larguraDoNumero = FontHandler.getWidth(numero, fonteDoNumero);
        int nx = caixa.x + MARGEM * s + (GUTTER * s - MARGEM * s - larguraDoNumero) / 2;
        // A ALTURA E MEDIDA, e nao chutada: com um deslocamento fixo o algarismo
        // descia do centro e encostava no rodape da moldura.
        int alturaDoNumero = FontHandler.getHeight(numero, fonteDoNumero);
        escrever(g, numero, nx, meio + alturaDoNumero / 2 - s,
                ativo ? Palette.TEXT : Palette.LIGHT, s, fonteDoNumero);

        g.setColor(new Color(Palette.OUTLINE.getRed(), Palette.OUTLINE.getGreen(),
                Palette.OUTLINE.getBlue(), 120));
        g.fillRect(caixa.x + GUTTER * s, corpoY + 3 * s, s, corpoH - 7 * s);

        Corrida corrida = corridas[i];
        boolean temLixeira = corrida != null;
        int tx = caixa.x + (GUTTER + 6) * s;
        int dir = caixa.x + caixa.width - MARGEM * s
                - (temLixeira ? (LIXEIRA + 4) * s : 0);

        Font fonteDoNome = FontHandler.font(FontHandler.Game, 8f * s);
        Font fonteDaFicha = FontHandler.font(FontHandler.Game, 5f * s);

        String nome = corrida == null ? "Empty" : corrida.nomeDoGato();
        String ficha = corrida == null ? "Start a new run" : corrida.descricao();
        String tempo = corrida == null ? null : corrida.tempo();

        escrever(g, cortar(nome, fonteDoNome, dir - tx), tx, corpoY + 10 * s,
                corrida == null ? Palette.LIGHT : (ativo ? Palette.TEXT : Palette.ACCENT),
                s, fonteDoNome);

        int larguraDoTempo = tempo == null ? 0 : FontHandler.getWidth(tempo, fonteDaFicha);
        if (tempo != null) {
            escrever(g, tempo, dir - larguraDoTempo, corpoY + 20 * s,
                    Palette.LIGHT, s, fonteDaFicha);
        }
        int limite = dir - tx - larguraDoTempo - (tempo == null ? 0 : 6 * s);
        escrever(g, cortar(ficha, fonteDaFicha, limite), tx, corpoY + 20 * s,
                Palette.LIGHT, s, fonteDaFicha);

        if (temLixeira) {
            desenharLixeira(g, i, s);
        }
    }

    /**
     * O X, desenhado em PIXELS.
     *
     * A primeira versao usava duas diagonais de {@code drawLine}: um traco de um
     * pixel de tela, com as pontas suavizadas, no meio de uma interface em que
     * cada outro elemento e arte ampliada em escala inteira. Era a unica coisa da
     * tela que nao parecia do jogo — foi o "o X nao ta combinando".
     *
     * Uma matriz ampliada pela mesma escala do resto resolve: o X passa a ter a
     * mesma espessura de traco e o mesmo serrilhado de tudo em volta.
     */
    private static final String[] XIS = {
            "##...##",
            "###.###",
            ".#####.",
            "..###..",
            ".#####.",
            "###.###",
            "##...##",
    };

    /**
     * PEQUENO DE PROPOSITO. Ele divide a caixa com a acao de jogar, e as duas tem
     * pesos opostos — uma abre a corrida, a outra a destroi. Um alvo generoso para
     * apagar seria um alvo generoso para errar.
     */
    private void desenharLixeira(Graphics2D g, int i, int s) {
        Rectangle r = lixeiras[i];
        boolean sobre = Mouse.on(r) || (foco == i && focoNoApagar);
        g.setColor(sobre ? Palette.WARN : Palette.OUTLINE);
        g.fillRect(r.x, r.y, r.width, r.height);
        int px = Math.max(1, r.width / 9);
        int ox = r.x + (r.width - XIS[0].length() * px) / 2;
        int oy = r.y + (r.height - XIS.length * px) / 2;
        g.setColor(sobre ? Palette.TEXT : Palette.LIGHT);
        for (int y = 0; y < XIS.length; y++) {
            for (int x = 0; x < XIS[y].length(); x++) {
                if (XIS[y].charAt(x) == '#') {
                    g.fillRect(ox + x * px, oy + y * px, px, px);
                }
            }
        }
        if (sobre) {
            Engine.window.pointing();
        }
    }

    /**
     * Corta com reticencias em vez de deixar vazar.
     *
     * Um nome comprido tem de perder o fim, e nao a caixa: texto que sai pela borda
     * suja o que esta ao lado e nao avisa que foi cortado. As reticencias avisam.
     */
    private static String cortar(String texto, Font fonte, int limite) {
        if (texto == null || FontHandler.getWidth(texto, fonte) <= limite) {
            return texto;
        }
        String curto = texto;
        while (curto.length() > 1
                && FontHandler.getWidth(curto + "...", fonte) > limite) {
            curto = curto.substring(0, curto.length() - 1);
        }
        return curto + "...";
    }

    private void escrever(Graphics2D g, String texto, int x, int y,
                          Color cor, int s, Font fonte) {
        g.setFont(fonte);
        TextFx.draw(g, texto, x, y, cor, s);
    }

    @Override
    public void dispose() {
    }
}
