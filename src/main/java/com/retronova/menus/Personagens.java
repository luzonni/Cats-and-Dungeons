package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.entities.Player;
import com.retronova.menus.shared.Button;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Seleção de personagem, no arranjo do Slay the Spire.
 *
 * O QUE A REFERÊNCIA FAZ, E O QUE FOI COPIADO
 *
 * A versão anterior era três cartões iguais lado a lado, com os atributos
 * escondidos no verso. Isso divide a atenção em três e não dá espaço a nenhum.
 * O arranjo da referência resolve o oposto — um personagem por vez, ocupando a
 * tela, com tudo dele visível ao mesmo tempo:
 *
 *  - ARTE GRANDE À DIREITA, sangrando para fora da tela em vez de presa numa
 *    moldura. É o que faz o personagem ter tamanho de verdade; enquadrá-lo num
 *    quadrinho é exatamente o que deixava os gatos pequenos.
 *  - TELA TINGIDA pela cor do escolhido, num halo atrás da arte. Cada gato passa
 *    a ter presença própria antes mesmo de se ler o nome.
 *  - BLOCO DE TEXTO À ESQUERDA sobre uma chapa translúcida, sem moldura dura:
 *    nome grande, linha de atributos com números, descrição, e a arma inicial no
 *    lugar que a referência dá à relíquia inicial — ícone, nome em destaque e uma
 *    linha explicando o que ela faz.
 *  - FILEIRA DE RETRATOS no rodapé central, o escolhido aceso e os outros
 *    apagados.
 *  - BACK à esquerda e EMBARK à direita, nos cantos de baixo.
 *
 * Os números aparecem soltos, como na referência, e não em barras. A comparação
 * entre gatos passa a ser feita trocando de retrato — é o compromisso que este
 * arranjo assume em troca de presença.
 *
 * Nome, papel, lore, cor, arma e atributos saem do JSON do personagem em
 * resources/players — não há texto de personagem escrito nesta classe.
 */
public class Personagens implements Activity {

    /** Lado do retrato pequeno da fileira, em pixels nativos. */
    private static final int MINIATURA = 30;
    private static final int MINIATURA_GAP = 6;
    /**
     * Onde cada gato está DENTRO da ilustração oficial, em fração dela.
     *
     * Em fração e não em pixels para a conta valer em qualquer janela: a arte é
     * escalada para cobrir a tela, e um ponto em fração acompanha a escala
     * sozinho.
     *
     * A ordem é a de {@code Player.TEMPLATES} — Muffin à esquerda da cena, Azrael
     * no meio, Finn à direita. Os três estão desenhados ali; achar cada um deu
     * trabalho porque a ilustração é uma multidão e o que separa rato de gato é o
     * OLHO VERMELHO. O vulto cinza grande do primeiro plano da esquerda parece o
     * Muffin e é um rato: o Muffin de verdade está atrás dele, de costas, com o
     * cachecol verde.
     */
    private static final float[][] FOCO = {
            {0.304f, 0.432f},   // Muffin, cachecol verde
            {0.512f, 0.494f},   // Azrael, coroa e capa roxa
            {0.699f, 0.481f},   // Finn, bandana vermelha
    };

    /** Raio do facho, em fração da diagonal da janela. */
    private static final float RAIO_FACHO = 0.30f;

    /**
     * Fração de cima da ilustração ocupada pelo letreiro "Cats & Dungeons".
     *
     * Ela é RECORTADA fora, e o que sobra é usado inteiro. A primeira solução foi
     * outra: ampliar a arte 30% além do necessário e empurrar a folga para cima
     * até o letreiro sair da tela. Funcionava em janela grande e estragava em
     * janela pequena — ali a ampliação já é grande por causa do "cobrir", e mais
     * 30% deixava tudo enorme, com meia dúzia de ratos ocupando a tela toda.
     *
     * Recortando, a arte é usada em escala de cobertura pura em qualquer tamanho:
     * numa janela pequena aparece a mesma proporção de cena que numa grande. Em
     * janela grande a composição fica praticamente igual à de antes — o que se
     * via começava justamente na altura deste corte.
     */
    private static final double LETREIRO = 360.0 / 1620.0;
    /**
     * A cabeça, em pixels da grade de 16 do sprite.
     *
     * O retrato é RECORTADO nesta caixa antes de qualquer coisa, e não só
     * enquadrado nela. Enquadrar não bastava: quando a miniatura era mais alta do
     * que larga, sobrava espaço embaixo da cabeça e apareciam pedaços de veste e
     * de rabo. Com o recorte não existe o que sobrar — o que não é cabeça não
     * está mais na imagem.
     *
     * A caixa vai da ponta da orelha (linha 0) ao queixo (linha 8), e da coluna 0
     * à 14, que é a largura máxima da cabeça. É o mesmo enquadramento do logotipo
     * do jogo.
     */
    private static final int CABECA_LARGURA = 15, CABECA_ALTURA = 9;

    private final Player[] players = new Player[3];
    private final Rectangle[] miniaturas = new Rectangle[3];
    /** Sprite da arma inicial de cada um, para o bloco de arma. Pode ser nulo. */
    private final BufferedImage[] armas = new BufferedImage[3];
    /**
     * Retrato de cada gato, capturado UMA vez.
     *
     * {@code Player.getSprite(int)} chama {@code setIndex} na folha, que é
     * compartilhada com a arte grande — pedir o quadro zero para a fileira a cada
     * frame zerava a animação do escolhido, e era isso que fazia o gato grande
     * piscar entre um quadro e outro. Capturado no construtor, ninguém mais
     * disputa o índice com a animação.
     */
    private final BufferedImage[] retratos = new BufferedImage[3];

    /**
     * Sempre há um escolhido, e começa no primeiro.
     *
     * Não existe estado vazio: a tela mostra um personagem por vez, então "nenhum
     * selecionado" seria meia tela em branco. Entrar já com o primeiro pronto
     * também deixa o "Embark" sempre válido, como na referência.
     */
    private int selecionado = 0;

    /**
     * Quanto o escolhido já entrou, de 0 a 1. Anima a troca de personagem.
     *
     * A 0,16 por quadro a troca durava seis quadros — um décimo de segundo, curto
     * demais para o rastro chegar a ser visto. A 0,06 são dezessete quadros, quase
     * três décimos, que é a faixa em que uma transição de interface é percebida
     * como movimento e não como corte.
     */
    private float entrada = 0f;      // o primeiro gato tambem entra, ao abrir a tela
    private static final float VELOCIDADE_ENTRADA = 0.06f;

    /**
     * A ilustração oficial inteira, em escala 1.
     *
     * Carregada assim de propósito: é pintura, não pixel art, e a folha de sprites
     * do jogo a multiplicaria por {@code GameScale} na memória — uma imagem de
     * 3260 por 1620 vezes quatro — sem nenhum ganho.
     */
    private final BufferedImage arteOriginal;

    /** A mesma arte já no tamanho da janela. Refeita só quando a janela muda. */
    private BufferedImage arteEscalada;
    private int larguraEscalada, alturaEscalada;

    /** De qual gato o facho está saindo. Junto com {@code entrada}, faz a varredura. */
    private int focoAnterior = 0;

    private final Button jogar;
    private final Button voltar;

    private Rectangle bloco = new Rectangle();
    /**
     * Onde a moldura do modelo ficou neste quadro.
     *
     * Guardada porque as linhas de atributo precisam saber dela: elas são
     * alinhadas à direita do bloco, e é exatamente ali que a moldura está — sem
     * isto os números de HP e ATK ficavam POR BAIXO do gato.
     */
    private Rectangle caixaModelo = new Rectangle();
    private Rectangle arte = new Rectangle();

    public Personagens() {
        // A trilha do menu NAO e trocada aqui. Escolher o gato ainda e a tela
        // inicial: cortar a musica no meio para tocar outra por poucos segundos
        // soava como se o jogador tivesse mudado de lugar quando nao mudou.
        // A troca acontece uma vez so, ao entrar de fato no jogo, em iniciar().

        for (int i = 0; i < players.length; i++) {
            players[i] = Player.TEMPLATES[i];
            miniaturas[i] = new Rectangle();
            armas[i] = spriteDaArma(players[i].getArma());
            retratos[i] = cabeca(nativo(players[i].getSprite(0)));
        }

        this.arteOriginal = semLetreiro(new SpriteHandler("icons", "Gato", 1).getSHEET());
        this.jogar = new Button(0, 0, 0, 0, "Embark", b -> iniciar()).primary().meow(players[selecionado].getVoz());
        this.voltar = new Button(0, 0, 0, 0, "Back", b -> Engine.backActivity());
    }

    /**
     * Ícone da arma inicial, procurado pelo nome vindo do inventário.
     *
     * Falhar aqui não pode derrubar a tela: se o item mudar de nome no JSON, o
     * bloco de arma perde só o ícone e continua mostrando o nome.
     */
    private static BufferedImage spriteDaArma(String nome) {
        try {
            for (ItemIDs id : ItemIDs.values()) {
                if (id.name().equalsIgnoreCase(nome)) {
                    return Item.build(id.ordinal(), 1).getSprite();
                }
            }
        } catch (Exception semIcone) {
            System.err.println("Personagens: ícone da arma " + nome + " não carregado: " + semIcone);
        }
        return null;
    }

    private void iniciar() {
        // Cala o menu ANTES de montar o jogo: o construtor de Room ja sobe a
        // trilha da antecamara, e um stopAll depois dele mataria as duas.
        Sound.stopAll();
        Activity novoJogo = new Game(selecionado, new Room("beginning"));
        // Sem trabalho para fazer aqui: o jogo ja esta montado. Quem segura a
        // transicao e o tempo minimo do proprio Loading.
        Engine.heapActivity(novoJogo, () -> { });
    }

    // ------------------------------------------------------------------ layout

    private void posicionar() {
        int s = Configs.UiScale();
        int janelaW = Engine.window.getWidth();
        int janelaH = Engine.window.getHeight();
        int margem = Math.max(Configs.Margin(), 14 * s);

        int alturaBotao = Button.preferredHeight();
        int larguraBotao = Button.preferredWidth();
        int yBotoes = janelaH - margem - alturaBotao;
        voltar.setBounds(new Rectangle(margem, yBotoes, larguraBotao, alturaBotao));
        jogar.setBounds(new Rectangle(janelaW - margem - larguraBotao, yBotoes,
                larguraBotao, alturaBotao));

        // Fileira no rodapé central, entre os dois botões, como na referência.
        int larguraFileira = 3 * MINIATURA * s + 2 * MINIATURA_GAP * s;
        int xFileira = janelaW / 2 - larguraFileira / 2;
        int yFileira = yBotoes + alturaBotao / 2 - MINIATURA * s / 2;
        for (int i = 0; i < miniaturas.length; i++) {
            miniaturas[i].setBounds(xFileira + i * (MINIATURA + MINIATURA_GAP) * s, yFileira,
                    MINIATURA * s, MINIATURA * s);
        }

        this.arte = new Rectangle(0, 0, janelaW, yFileira);

        // O BLOCO FOGE DO GATO. Ele fica de um lado ou do outro conforme onde o
        // escolhido está na ilustração: com o Muffin, que está à esquerda da cena,
        // o texto vai para a direita; com o Azrael e o Finn, para a esquerda.
        // Texto por cima do bicho é o que estragava as versões anteriores.
        //
        // So a largura e a coluna vem daqui. A ALTURA e definida no desenho, pelo
        // conteudo — a lore muda de tamanho de um gato para outro.
        int larguraBloco = Math.min((int) (janelaW * 0.46), 170 * s);
        float t = suavizado();
        float lado = ladoDoBloco(focoAnterior) + (ladoDoBloco(selecionado) - ladoDoBloco(focoAnterior)) * t;
        int xEsquerda = margem, xDireita = janelaW - margem - larguraBloco;
        int x = Math.round(xEsquerda + (xDireita - xEsquerda) * lado);
        this.bloco = new Rectangle(x, bloco.y, larguraBloco, bloco.height);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        posicionar();

        if (KeyBoard.KeyPressed("Escape")) {
            Engine.backActivity();
            return;
        }

        for (int i = 0; i < miniaturas.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, miniaturas[i]) && selecionado != i) {
                Sound.play(players[i].getVoz());
                focoAnterior = selecionado;
                selecionado = i;
                // Embarcar mia com o gato ESCOLHIDO: o botao troca de voz junto
                // com a fileira, senao ele continuaria miando com o primeiro.
                jogar.meow(players[i].getVoz());
                entrada = 0f;
            }
            if (miniaturas[i].contains(Mouse.getX(), Mouse.getY())) {
                Engine.window.pointing();
            }
        }
        if (entrada < 1f) {
            entrada = Math.min(1f, entrada + VELOCIDADE_ENTRADA);
        }

        players[selecionado].tick();

        jogar.tick();
        if (jogar.isHovered()) {
            Engine.window.pointing();
        }
        voltar.tick();
        if (voltar.isHovered()) {
            Engine.window.pointing();
        }
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(Graphics2D g) {
        desenharAmbiente(g);
        desenharBloco(g);
        desenharFileira(g);
        jogar.render(g);
        voltar.render(g);
    }

    /**
     * A ilustração oficial inteira, com um facho de luz sobre o escolhido.
     *
     * A arte NÃO é mais recortada. Recortar dava sempre errado: a tela enche o
     * quadro pela maior das duas escalas, então um pedaço estreito era ampliado
     * até a largura caber e perdia tudo pelas pontas — sobrava um close sem
     * cabeça. E, pior, jogava fora a multidão, que é o que dá escala aos três.
     *
     * Agora a cena aparece inteira e quem aponta o personagem é a LUZ: tudo
     * escurece menos a região em que ele está desenhado. É o mesmo recurso de
     * palco, e resolve de uma vez o enquadramento e a identificação — não é
     * preciso que o gato esteja grande, só que esteja aceso.
     *
     * A VARREDURA. Ao trocar de gato o facho não corta para o novo lugar: ele
     * corre até lá e freia. O deslocamento usa a mesma curva da entrada anterior,
     * um menos o cubo do que falta — quase todo o caminho nos primeiros quadros e
     * o último pedaço devagar. É o que faz o olho chegar junto com a luz.
     */
    private void desenharAmbiente(Graphics2D g) {
        int w = Engine.window.getWidth(), h = Engine.window.getHeight();
        g.setColor(Palette.DARKEST);
        g.fillRect(0, 0, w, h);

        Rectangle caixa = arteOriginal == null ? null : caixaDaArte(w, h);
        BufferedImage arte = caixa == null ? null : arteNaEscala(caixa.width, caixa.height);
        if (arte != null) {
            g.drawImage(arte, caixa.x, caixa.y, null);
        }

        float t = suavizado();
        float fx = FOCO[focoAnterior][0] + (FOCO[selecionado][0] - FOCO[focoAnterior][0]) * t;
        float fy = FOCO[focoAnterior][1] + (FOCO[selecionado][1] - FOCO[focoAnterior][1]) * t;
        Point2D centro = pontoNaTela(fx, fy, w, h);
        float raio = (float) Math.hypot(w, h) * RAIO_FACHO;

        Graphics2D g2 = (Graphics2D) g.create();

        // Véu: transparente no miolo do facho e fechando para fora. É ele que
        // apaga o resto da cena sem apagar a cena.
        Color veu = Palette.DARKEST;
        g2.setPaint(new RadialGradientPaint(centro, raio,
                new float[]{0f, 0.42f, 1f},
                new Color[]{
                        new Color(veu.getRed(), veu.getGreen(), veu.getBlue(), 0),
                        new Color(veu.getRed(), veu.getGreen(), veu.getBlue(), 90),
                        new Color(veu.getRed(), veu.getGreen(), veu.getBlue(), 232)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, w, h);

        // Um respiro de luz no miolo, antes da tinta. A cena é noturna: só tirar o
        // véu deixava o escolhido no mesmo tom do resto, e o facho não lia como
        // luz, lia como buraco no escuro.
        g2.setPaint(new RadialGradientPaint(centro, raio * 0.75f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 255, 34), new Color(255, 255, 255, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, w, h);

        // Tinta do personagem por cima do aceso, fraca. É o que continua dizendo
        // de quem é a vez antes de o nome ser lido.
        Color cor = new Color(players[selecionado].getCor());
        g2.setPaint(new RadialGradientPaint(centro, raio * 0.9f,
                new float[]{0f, 1f},
                new Color[]{
                        new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 60),
                        new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, w, h);

        // Rodapé escurecido: os botões e a fileira ficam sobre a arte, e sem isto
        // o texto deles disputa com a multidão desenhada atrás.
        int altura = Math.max(1, h - inicioDoRodape());
        g2.setPaint(new GradientPaint(0, inicioDoRodape(),
                new Color(veu.getRed(), veu.getGreen(), veu.getBlue(), 0),
                0, h, new Color(veu.getRed(), veu.getGreen(), veu.getBlue(), 235)));
        g2.fillRect(0, inicioDoRodape(), w, altura);
        g2.dispose();
    }

    /** Onde o rodapé escuro começa: um pouco acima da fileira de retratos. */
    private int inicioDoRodape() {
        return Math.max(0, arte.height - 30 * Configs.UiScale());
    }

    /**
     * Progresso da varredura, já com a freada.
     *
     * Um menos o cubo do que falta: sobe quase todo no começo e se arrasta no
     * fim, que é a curva de quem corre e para. A mesma para o facho e para o
     * bloco de texto, senão os dois chegariam em tempos diferentes.
     */
    private float suavizado() {
        float falta = 1f - entrada;
        return 1f - falta * falta * falta;
    }

    /** 0 põe o bloco à esquerda, 1 à direita. O texto foge do gato aceso. */
    private static float ladoDoBloco(int gato) {
        return FOCO[gato][0] < 0.45f ? 1f : 0f;
    }

    /**
     * Onde a arte é desenhada na janela: origem e tamanho.
     *
     * Existe como método próprio porque DOIS lugares precisam da mesma conta — o
     * desenho da imagem e a posição do facho. Duplicada, bastaria mexer no zoom de
     * um lado para a luz apontar para o lugar errado.
     */
    private Rectangle caixaDaArte(int w, int h) {
        double escala = Math.max(w / (double) arteOriginal.getWidth(),
                h / (double) arteOriginal.getHeight());
        int dw = (int) Math.ceil(arteOriginal.getWidth() * escala);
        int dh = (int) Math.ceil(arteOriginal.getHeight() * escala);

        // A ARTE DESLIZA PARA O GATO ACESO CAIR NA METADE LIVRE.
        //
        // Antes ela ficava centrada e só a luz se movia, e com o gato do meio a
        // luz caía atrás do bloco de texto. Em janela grande passava, porque o
        // bloco tem largura fixa e sobra tela; em janela pequena ele chega a 46%
        // da largura e come o aceso inteiro.
        //
        // Deslizando, o gato escolhido fica sempre no meio do espaço que sobra —
        // e a luz vai junto, porque ela é posicionada por esta mesma caixa.
        float t = suavizado();
        float fx = FOCO[focoAnterior][0] + (FOCO[selecionado][0] - FOCO[focoAnterior][0]) * t;
        int ox = (int) Math.round(centroDoEspacoLivre(w) - fx * dw);
        // Sem passar da borda da arte: fundo vazio seria pior que o bloco em cima.
        ox = Math.max(w - dw, Math.min(0, ox));
        return new Rectangle(ox, -(dh - h) / 2, dw, dh);
    }

    /** Meio da metade da tela que o bloco de texto NAO ocupa. */
    private double centroDoEspacoLivre(int w) {
        if (bloco.width <= 0) {
            return w / 2.0;
        }
        boolean blocoNaEsquerda = bloco.x + bloco.width / 2 < w / 2;
        return blocoNaEsquerda
                ? (bloco.x + bloco.width + w) / 2.0
                : bloco.x / 2.0;
    }

    /**
     * Um ponto em fração da arte INTEIRA, convertido para pixel de tela.
     *
     * As frações de {@link #FOCO} foram medidas na ilustração completa, e é assim
     * que elas continuam — quem mexer nelas vai conferir contra o arquivo, não
     * contra um recorte. A conversão para dentro do recorte acontece aqui.
     */
    private Point2D pontoNaTela(float fx, float fy, int w, int h) {
        if (arteOriginal == null) {
            return new Point2D.Float(w * fx, h * fy);
        }
        double fyRecorte = (fy - LETREIRO) / (1 - LETREIRO);
        Rectangle c = caixaDaArte(w, h);
        return new Point2D.Double(c.x + fx * c.width, c.y + fyRecorte * c.height);
    }

    /** Devolve a ilustração sem a faixa do letreiro. Ver {@link #LETREIRO}. */
    private static BufferedImage semLetreiro(BufferedImage arte) {
        if (arte == null) {
            return null;
        }
        int corte = (int) Math.round(arte.getHeight() * LETREIRO);
        if (corte <= 0 || corte >= arte.getHeight()) {
            return arte;
        }
        return arte.getSubimage(0, corte, arte.getWidth(), arte.getHeight() - corte);
    }

    /**
     * A arte redimensionada, guardada e refeita só quando o tamanho muda.
     *
     * Escalar 3260 por 1620 a cada quadro custaria mais que o resto da tela
     * inteira somada. O que se guarda é a arte NO TAMANHO EM QUE ELA É DESENHADA
     * — maior que a janela, porque ela sangra pelos lados —, e não recortada na
     * janela: o deslize horizontal muda a cada troca de gato, e recortar aqui
     * obrigaria a refazer a escala a cada quadro da varredura.
     */
    private BufferedImage arteNaEscala(int dw, int dh) {
        if (arteOriginal == null || dw <= 0 || dh <= 0) {
            return null;
        }
        if (arteEscalada != null && larguraEscalada == dw && alturaEscalada == dh) {
            return arteEscalada;
        }
        BufferedImage nova = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = nova.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(arteOriginal, 0, 0, dw, dh, null);
        g.dispose();
        arteEscalada = nova;
        larguraEscalada = dw;
        alturaEscalada = dh;
        return arteEscalada;
    }

    /**
     * Medida do bloco: as fontes já escolhidas, o texto já quebrado e a altura
     * que tudo isso ocupa. Serve para medir antes de desenhar.
     */
    private record Medida(Font nome, Font stat, Font papel, Font lore, Font arma, Font nota,
                          List<Atributo> atributos, List<String> lores, int lado, int folga,
                          int entreLinhas, int larguraRotulo, int larguraValor,
                          int altura) { }

    /**
     * Um atributo da ficha: o rótulo, o número e o quanto ele enche o trilho.
     *
     * A FRAÇÃO É O PONTO. Antes a linha dizia "HP 80" e mais nada, e oitenta não
     * quer dizer nada sozinho — quem escolhe não sabe se é muito ou pouco, nem
     * como se compara com os outros dois. É o que a literatura de interface
     * aponta como a falha clássica: sem uma caixa vazia ao lado do número, o
     * jogador não tem contra o que comparar. O trilho é essa caixa.
     */
    private record Atributo(String rotulo, String valor, double fracao) { }

    /**
     * Teto de cada atributo, para o trilho ter escala.
     *
     * Dois critérios somados. O primeiro é um número redondo — 100 de vida, 20 de
     * ataque —, que dá ao jogador uma referência absoluta em vez de só relativa.
     * O segundo é o MAIOR VALOR DO ELENCO: se alguém amanhã der 130 de vida a um
     * gato, o teto sobe junto em vez de a barra mentir estourada. Sorte é
     * probabilidade e o teto dela é sempre um.
     */
    private static double teto(double redondo, java.util.function.ToDoubleFunction<Player> valor) {
        double maior = redondo;
        for (Player p : Player.TEMPLATES) {
            maior = Math.max(maior, valor.applyAsDouble(p));
        }
        return maior;
    }

    /** Os quatro atributos que a ficha mostra, na ordem em que aparecem. */
    private static List<Atributo> atributos(Player p) {
        return List.of(
                new Atributo("HP", String.valueOf((int) p.getLife()),
                        p.getLife() / teto(100, Player::getLife)),
                new Atributo("ATK", String.valueOf((int) p.getDamage()),
                        p.getDamage() / teto(20, Player::getDamage)),
                new Atributo("SPD", String.format(java.util.Locale.ROOT, "%.1f", p.getSpeed()),
                        p.getSpeed() / teto(5, Player::getSpeed)),
                new Atributo("LUCK", Math.round(p.getLuck() * 100) + "%",
                        p.getLuck() / teto(1, Player::getLuck)));
    }

    /**
     * Mede o bloco com as fontes reduzidas por um fator.
     *
     * O fator existe por causa de janela pequena: as fontes crescem com a escala
     * de UI e não com a altura disponível, então numa resolução baixa o bloco
     * ficava maior que o espaço que sobrava e transbordava por baixo. Medir
     * primeiro permite encolher só o quanto for preciso.
     */
    private Medida medir(Player p, float fator) {
        int s = Configs.UiScale();
        Font nome = FontHandler.font(FontHandler.Game, Math.max(7f, 16f * fator) * s);
        Font stat = FontHandler.font(FontHandler.Game, Math.max(3.5f, 5f * fator) * s);
        Font papel = FontHandler.font(FontHandler.Game, Math.max(3f, 4f * fator) * s);
        Font lore = FontHandler.font(FontHandler.Game, Math.max(3.5f, 5f * fator) * s);
        Font arma = FontHandler.font(FontHandler.Game, Math.max(4f, 6f * fator) * s);
        Font nota = FontHandler.font(FontHandler.Game, Math.max(3f, 4f * fator) * s);

        int folga = Math.max(6 * s, (int) (14 * s * fator));
        int entre = Math.max(5 * s, (int) (8 * s * fator));
        int lado = Math.max(8 * s, (int) (14 * s * fator));
        int util = bloco.width - folga * 2;

        List<Atributo> atributos = atributos(p);
        List<String> lores = quebrar(p.getLore(), lore, util);

        // As colunas de rótulo e de número são as MESMAS nas quatro linhas, e
        // medidas sobre o elenco inteiro: assim os trilhos começam e terminam
        // alinhados, e não dançam ao trocar de gato.
        int larguraRotulo = 0, larguraValor = 0;
        for (Player outro : Player.TEMPLATES) {
            for (Atributo a : atributos(outro)) {
                larguraRotulo = Math.max(larguraRotulo, FontHandler.getWidth(a.rotulo(), stat));
                larguraValor = Math.max(larguraValor, FontHandler.getWidth(a.valor(), stat));
            }
        }

        int altura = folga
                + FontHandler.getHeight("Ay", nome)
                + entre + atributos.size() * entre
                + entre
                + entre + lores.size() * entre
                + entre + Math.max(lado, FontHandler.getHeight("Ay", arma) + entre)
                + folga;
        return new Medida(nome, stat, papel, lore, arma, nota, atributos, lores, lado, folga,
                entre, larguraRotulo, larguraValor, altura);
    }

    /**
     * Bloco de texto sobre chapa translúcida.
     *
     * Sem moldura dura de propósito: na referência o painel é só um véu que
     * segura o texto contra a arte, e uma moldura fechada aqui devolveria a
     * sensação de caixa que esta tela estava tentando perder.
     *
     * A CHAPA É MEDIDA PELO CONTEÚDO, e o conteúdo é encolhido até caber no
     * espaço livre acima do rodapé. Com altura fixa o texto de um gato cabia e o
     * do outro vazava; sem teto, qualquer janela baixa fazia o bloco atravessar a
     * fileira de retratos.
     */
    private void desenharBloco(Graphics2D g) {
        int s = Configs.UiScale();
        Player p = players[selecionado];

        int topo = 14 * s;
        int rodape = miniaturas[0].height > 0
                ? miniaturas[0].y
                : (int) (Engine.window.getHeight() * 0.78);
        // Duas travas: nunca menor que um minimo legivel, nunca maior que a
        // janela. A segunda importa quando alguem fixa uma escala de UI grande
        // numa resolucao baixa — ai o rodape sobe tanto que o espaco calculado
        // ficaria negativo, e sem o teto o bloco voltaria a transbordar.
        int disponivel = Math.min(Engine.window.getHeight() - topo - 8 * s,
                Math.max(30 * s, rodape - topo - 8 * s));

        // Encolhe em passos até caber. Cada passo remede, porque mudar a fonte
        // muda também onde o texto quebra — e portanto o número de linhas.
        float fator = 1f;
        Medida m = medir(p, fator);
        for (int i = 0; i < 5 && m.altura() > disponivel; i++) {
            fator = Math.max(0.45f, fator * Math.max(0.7f, disponivel / (float) m.altura()));
            m = medir(p, fator);
        }

        bloco.height = Math.min(m.altura(), disponivel);
        bloco.y = topo + Math.max(0, (disponivel - bloco.height) / 2);

        g.setColor(new Color(9, 13, 20, 170));
        g.fillRect(bloco.x, bloco.y, bloco.width, bloco.height);
        g.setColor(new Color(p.getCor()));
        g.fillRect(bloco.x, bloco.y, 2 * s, bloco.height);

        // O modelo vai ANTES do texto: é ele que define até onde as linhas de
        // atributo podem ir, e para isso a moldura já tem de estar medida.
        desenharModelo(g, p, m, s);

        int x = bloco.x + m.folga();
        int y = bloco.y + m.folga() + FontHandler.getHeight("Ay", m.nome()) - 4 * s;

        texto(g, p.getName().toUpperCase(), m.nome(), x, y, Palette.LIGHT);

        // A coluna dos números recua para a esquerda da moldura, e recua nas
        // QUATRO linhas mesmo que só as de cima passem por trás dela: coluna
        // torta lê pior do que trilho curto.
        int direita = bloco.x + bloco.width - m.folga();
        if (!caixaModelo.isEmpty()) {
            direita = Math.min(direita, caixaModelo.x - 5 * s);
        }

        y += m.entreLinhas() + 3 * s;
        for (Atributo a : m.atributos()) {
            desenharAtributo(g, a, m, x, y, direita, s);
            y += m.entreLinhas();
        }

        texto(g, p.getPapel().toUpperCase(), m.papel(), x, y, new Color(255, 255, 255, 150));

        y += m.entreLinhas() * 2;
        for (String linha : m.lores()) {
            texto(g, linha, m.lore(), x, y, Palette.TEXT);
            y += m.entreLinhas();
        }

        // Bloco da arma, no lugar da relíquia inicial da referência: ícone à
        // esquerda, nome em destaque e uma linha dizendo o que ela é. O ícone é
        // centrado ENTRE AS DUAS LINHAS, e não alinhado pela primeira — alinhado
        // por cima ele ficava jogado acima do texto que descreve.
        y += m.entreLinhas() + 4 * s;
        int lado = m.lado();
        int baseNome = y;
        int baseNota = y + m.entreLinhas();
        int topoNome = baseNome - FontHandler.getHeight("Ay", m.arma());
        if (armas[selecionado] != null) {
            Graphics2D gs = (Graphics2D) g.create();
            gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            gs.drawImage(armas[selecionado], x, (topoNome + baseNota) / 2 - lado / 2,
                    lado, lado, null);
            gs.dispose();
        }
        int xt = x + lado + 7 * s;
        texto(g, p.getArma(), m.arma(), xt, baseNome, Palette.LIGHT);
        texto(g, "Starts the run with this weapon.", m.nota(), xt, baseNota, Palette.TEXT);
    }

    /**
     * O boneco como ele aparece na partida, no canto de cima do bloco.
     *
     * A tela inteira mostra a ILUSTRAÇÃO, que é bonita e não diz nada sobre o que
     * se vai controlar — quem escolhe vê um gato pintado e recebe outro, de
     * dezesseis pixels. Aqui está o sprite de verdade, animado no mesmo ritmo do
     * jogo: a respiração é a mesma que ele terá parado na masmorra.
     *
     * Fica ao lado do nome porque é ali que sobra espaço em todos os três — nome
     * curto, bloco estreito — e porque nome e retrato juntos leem como ficha.
     *
     * Desenhado na grade nativa e ampliado por fator INTEIRO. Ampliar o sprite já
     * multiplicado pela escala do jogo daria fatores grosseiros, e em fator
     * quebrado o pixel deixa de ser quadrado, que é justamente o que se quer
     * mostrar.
     */
    private void desenharModelo(Graphics2D g, Player p, Medida m, int s) {
        caixaModelo = new Rectangle();
        BufferedImage sprite = p.getSprite();
        if (sprite == null) {
            return;
        }
        BufferedImage nativo = nativo(sprite);
        int larguraNome = FontHandler.getWidth(p.getName().toUpperCase(), m.nome());
        int sobra = bloco.width - m.folga() * 2 - larguraNome - 10 * s;
        int zoom = Math.min(26 * s, sobra) / nativo.getHeight();
        if (zoom < 2) {
            return;      // menor que isto não se enxerga: melhor não desenhar
        }
        int w = nativo.getWidth() * zoom, h = nativo.getHeight() * zoom;
        int x = bloco.x + bloco.width - m.folga() - w;
        int y = bloco.y + m.folga();

        // MOLDURA. Sem ela o boneco parecia largado num canto qualquer do painel:
        // a caixa é o que diz "esta área é o modelo", separa o que é sprite do
        // que é texto e dá ao gato um chão para pisar.
        int borda = Math.max(1, s / 2);
        // Nunca maior que a folga do bloco, senao a moldura sangraria para fora
        // dele pela direita e pelo alto.
        int folga = Math.min(m.folga(), Math.max(2 * s, zoom));
        Rectangle caixa = new Rectangle(x - folga, y - folga, w + folga * 2, h + folga * 2);
        this.caixaModelo = caixa;

        Graphics2D gs = (Graphics2D) g.create();
        gs.setColor(new Color(0, 0, 0, 110));
        gs.fillRect(caixa.x, caixa.y, caixa.width, caixa.height);
        gs.setColor(new Color(255, 255, 255, 40));
        gs.drawRect(caixa.x, caixa.y, caixa.width - 1, caixa.height - 1);
        // Cantos na cor do gato: fecham a moldura sem virar um quadro pesado.
        gs.setColor(new Color(p.getCor()));
        int canto = Math.max(2 * s, caixa.width / 5);
        gs.fillRect(caixa.x, caixa.y, canto, borda);
        gs.fillRect(caixa.x, caixa.y, borda, canto);
        gs.fillRect(caixa.x + caixa.width - canto, caixa.y + caixa.height - borda, canto, borda);
        gs.fillRect(caixa.x + caixa.width - borda, caixa.y + caixa.height - canto, borda, canto);

        // Sombra no chão: sem ela o boneco flutua dentro da moldura.
        gs.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        gs.setColor(new Color(0, 0, 0, 90));
        gs.fillOval(x + w / 6, y + h - zoom, w - w / 3, zoom * 2);
        gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gs.drawImage(nativo, x, y, w, h, null);
        gs.dispose();
    }

    /**
     * Uma linha de atributo: rótulo, trilho e número.
     *
     * O trilho é contorno mais preenchimento, e não uma fileira de casinhas. Casa
     * melhor com pixel art, mas em janela pequena com escala de UI grande cada
     * casinha cairia para um ou dois pixels e viraria sujeira — este projeto já
     * teve esse defeito na HUD. Contorno e preenchimento funcionam em qualquer
     * tamanho, e a parte vazia diz o mesmo que as casinhas diriam: quanto falta
     * para o teto.
     */
    private void desenharAtributo(Graphics2D g, Atributo a, Medida m,
                                  int x, int y, int direita, int s) {
        texto(g, a.rotulo(), m.stat(), x, y, new Color(255, 255, 255, 160));

        int xValor = direita - m.larguraValor();
        texto(g, a.valor(), m.stat(), xValor, y, Palette.TEXT);

        int xTrilho = x + m.larguraRotulo() + 5 * s;
        int largura = xValor - 5 * s - xTrilho;
        if (largura < 8 * s) {
            return;             // bloco estreito demais: o número sozinho basta
        }
        int altura = Math.max(2, FontHandler.getHeight("A", m.stat()) / 2);
        int yTrilho = y - altura - Math.max(1, altura / 3);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(xTrilho, yTrilho, largura, altura);
        g2.setColor(new Color(255, 255, 255, 45));
        g2.drawRect(xTrilho, yTrilho, largura - 1, altura - 1);
        int cheio = (int) Math.round(largura * Math.max(0, Math.min(1, a.fracao())));
        g2.setColor(new Color(players[selecionado].getCor()));
        g2.fillRect(xTrilho, yTrilho, cheio, altura);
        g2.setColor(Palette.ACCENT);
        g2.fillRect(xTrilho, yTrilho, cheio, Math.max(1, altura / 3));
        g2.dispose();
    }

    /** Fileira de retratos: o escolhido aceso, os outros apagados. */
    private void desenharFileira(Graphics2D g) {
        int s = Configs.UiScale();
        for (int i = 0; i < miniaturas.length; i++) {
            Rectangle r = miniaturas[i];
            boolean escolhido = selecionado == i;
            boolean sobre = r.contains(Mouse.getX(), Mouse.getY());

            g.setColor(escolhido ? new Color(players[i].getCor()) : new Color(9, 13, 20, 200));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(escolhido ? Palette.LIGHT : new Color(255, 255, 255, sobre ? 120 : 50));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);

            BufferedImage sprite = retratos[i];
            if (sprite != null) {
                desenharRosto(g, sprite, r);
            }
            if (!escolhido) {
                g.setColor(new Color(9, 13, 20, 130));
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        }
    }

    /**
     * Desenha o retrato: a cabeça, e mais nada.
     *
     * Num quadrado de trinta pixels o gato inteiro vira uma manchinha, e os três
     * ficam com a mesma silhueta — a fileira não diz qual é qual. A cabeça sozinha
     * mostra o que de fato os separa: orelha, olho, coroa, bandana. O corpo
     * continua sendo visto na arte grande, que é onde há lugar para ele.
     *
     * A imagem que chega aqui já vem recortada em {@link #cabeca}, então o fator
     * pode encher o quadrado sem risco de trazer junto o que está embaixo.
     */
    private void desenharRosto(Graphics2D g, BufferedImage cabeca, Rectangle r) {
        int s = Configs.UiScale();
        int folga = 2 * s;
        int largura = r.width - 2 * folga, altura = r.height - 2 * folga;
        int fator = Math.max(1, Math.min(largura / cabeca.getWidth(), altura / cabeca.getHeight()));
        int w = cabeca.getWidth() * fator, h = cabeca.getHeight() * fator;

        Graphics2D gs = (Graphics2D) g.create();
        gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gs.clipRect(r.x + folga, r.y + folga, largura, altura);
        gs.drawImage(cabeca, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h, null);
        gs.dispose();
    }

    /** Recorta a cabeça do sprite. Ver {@link #CABECA_LARGURA}. */
    private static BufferedImage cabeca(BufferedImage sprite) {
        int w = Math.min(CABECA_LARGURA, sprite.getWidth());
        int h = Math.min(CABECA_ALTURA, sprite.getHeight());
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        o.createGraphics().drawImage(sprite.getSubimage(0, 0, w, h), 0, 0, null);
        return o;
    }

    /**
     * Devolve o sprite na grade nativa de 16, desfazendo a escala do jogo.
     *
     * A folha é carregada já multiplicada por {@code GameScale}, então ampliar
     * aquela imagem de novo só daria fatores grosseiros — de 1 para 2 o retrato
     * dobra de tamanho, e não há nada no meio. Voltando à grade original, o fator
     * da miniatura passa a ter um degrau por pixel, que é o que permite encher o
     * quadrado sem cortar a cabeça. A redução é exata: cada pixel de arte virou um
     * bloco liso na ampliação, e aqui se lê um pixel de cada bloco.
     */
    private static BufferedImage nativo(BufferedImage sprite) {
        int escala = Math.max(1, sprite.getHeight() / 16);
        int lado = sprite.getHeight() / escala;
        BufferedImage o = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                o.setRGB(x, y, sprite.getRGB(x * escala, y * escala));
            }
        }
        return o;
    }

    /** Quebra pela largura medida da fonte, como o painel das placas. */
    private static List<String> quebrar(String texto, Font fonte, int larguraUtil) {
        List<String> saida = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            String tentativa = linha.length() == 0 ? palavra : linha + " " + palavra;
            if (FontHandler.getWidth(tentativa, fonte) > larguraUtil && linha.length() > 0) {
                saida.add(linha.toString());
                linha.setLength(0);
                linha.append(palavra);
            } else {
                linha.setLength(0);
                linha.append(tentativa);
            }
        }
        if (linha.length() > 0) {
            saida.add(linha.toString());
        }
        return saida;
    }

    /** Desenha com a sombra dura de 1px que o resto da UI usa. */
    private void texto(Graphics2D g, String txt, Font fonte, int x, int y, Color cor) {
        int s = Configs.UiScale();
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(txt, x + s, y + s);
        g.setColor(cor);
        g.drawString(txt, x, y);
    }

    @Override
    public void dispose() {
    }
}
