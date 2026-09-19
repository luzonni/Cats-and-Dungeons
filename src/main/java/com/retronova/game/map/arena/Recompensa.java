package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.items.Melhorias;
import com.retronova.menus.shared.Button;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A escolha de fim de turno: tres cartas, uma leva.
 *
 * O QUE ELA E, NO DESENHO DA CORRIDA.
 *
 * Ate agora limpar uma arena abria um alcapao e mais nada: a recompensa por
 * sobreviver era poder enfrentar a proxima. Isso faz a corrida andar, mas nao a
 * faz CRESCER — o gato termina o quinto turno com o mesmo equipamento do
 * primeiro, e a unica coisa que muda de sala para sala e a quantidade de bicho.
 *
 * A escolha entre tres cartas resolve isso com uma pergunta em vez de um premio.
 * Um premio dado e so um numero que sobe; uma escolha entre tres e o jogador
 * dizendo que corrida ele quer fazer — e como a oferta e sorteada, duas corridas
 * nunca sao iguais. E a peca central do genero, e a razao de ela vir antes de
 * temas de arena e portas: sem ela, mais salas sao mais do mesmo.
 *
 * A ESCOLHA E OBRIGATORIA. Nao ha como sair sem pegar uma carta — sem Escape,
 * sem clicar fora. Poder recusar transformaria a tela num aviso, e avisos se
 * fecham no automatico; obrigando, cada fim de turno vira uma decisao pequena
 * que o jogador de fato toma.
 *
 * AS CARTAS DEIXARAM DE SER ARMAS. Trocar de arma e uma decisao que se toma UMA
 * vez: depois que a espada de fogo entra na mao, as proximas quinze cartas de
 * arma sao ruido. E raridade nao significava nada — uma "espada epica" era uma
 * espada de outra cor, porque a arma nao tinha grau.
 *
 * Agora a carta entrega uma MELHORIA, e a mesma melhoria existe em quatro
 * intensidades. Isso conserta as duas coisas de uma vez: a raridade passa a ser
 * QUANTO em vez de O QUE, e toda carta soma em cima do que ja foi escolhido em
 * vez de substituir. As armas ficaram com o vendedor. Ver Melhoria e Melhorias.
 */
public class Recompensa implements Activity {

    /** Quantas cartas sao oferecidas. Tres e o numero que vira decisao sem virar lista. */
    private static final int QUANTAS = 3;

    private final List<Melhorias.Carta> cartas = new ArrayList<>();
    private final List<Rectangle> caixas = new ArrayList<>();
    private final Runnable aoEscolher;

    private int foco = -1;
    private int entrando;

    /** Quadros de entrada. A tela nasce transparente para nao cortar o combate no talo. */
    private static final int ENTRADA = 18;

    public Recompensa(Runnable aoEscolher) {
        this.aoEscolher = aoEscolher;
        com.retronova.game.objects.entities.Player gato = Game.getPlayer();
        double promocao = gato.hasModifier(
                com.retronova.game.objects.entities.Modifiers.Fortune)
                ? gato.valueModifier(com.retronova.game.objects.entities.Modifiers.Fortune)
                : 0;
        this.cartas.addAll(gato.getMelhorias()
                .oferecer(QUANTAS, Game.getGame().getLevel(), promocao));
        posicionar();
    }

    private void posicionar() {
        int s = Configs.UiScale();
        // MAIORES, porque a ficha ganhou faixas.
        //
        // As antigas nao tinham espaco para dividir nada: nome e atributos vinham
        // um debaixo do outro, com o mesmo espacamento e quase o mesmo tamanho, e a
        // carta lia como um paragrafo. O que separa faixas nao e a linha desenhada
        // entre elas, e a MARGEM que sobra dos dois lados dela — e margem precisa de
        // altura. Trinta pixels de arte a mais e o preco de a ficha ser legivel.
        int largura = 76 * s;
        int altura = 126 * s;
        int espaco = 8 * s;
        int total = cartas.size() * largura + (cartas.size() - 1) * espaco;
        int x = Engine.window.getWidth() / 2 - total / 2;
        int y = Engine.window.getHeight() / 2 - altura / 2 + 4 * s;
        caixas.clear();
        for (int i = 0; i < cartas.size(); i++) {
            caixas.add(new Rectangle(x + i * (largura + espaco), y, largura, altura));
        }
    }

    @Override
    public void tick() {
        posicionar();
        if (entrando < ENTRADA) {
            entrando++;
            return;
        }
        int sobreOMouse = -1;
        for (int i = 0; i < caixas.size(); i++) {
            if (caixas.get(i).contains(Mouse.getX(), Mouse.getY())) {
                sobreOMouse = i;
            }
        }
        if (sobreOMouse >= 0) {
            foco = sobreOMouse;
            Engine.window.pointing();
            if (Mouse.clickOn(Mouse_Button.LEFT, caixas.get(sobreOMouse))) {
                escolher(sobreOMouse);
                return;
            }
        }
        if (KeyBoard.KeyPressed("Right") || KeyBoard.KeyPressed("D")) {
            foco = foco < 0 ? 0 : (foco + 1) % cartas.size();
        } else if (KeyBoard.KeyPressed("Left") || KeyBoard.KeyPressed("A")) {
            foco = foco <= 0 ? cartas.size() - 1 : foco - 1;
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            escolher(foco);
        }
        // NAO HA SAIDA POR ESCAPE. A escolha e obrigatoria, e por isso a tecla que
        // fecha tudo no resto do jogo nao faz nada aqui.
    }

    private void escolher(int qual) {
        Sound.play(Sounds.Button);
        Melhorias.Carta carta = cartas.get(qual);
        Game.getPlayer().getMelhorias()
                .aplicar(Game.getPlayer(), carta.melhoria(), carta.raridade());
        Engine.pause(null);
        if (aoEscolher != null) {
            aoEscolher.run();
        }
    }

    @Override
    public void render(Graphics2D g) {
        float entrada = Math.min(1f, entrando / (float) ENTRADA);
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int s = Configs.UiScale();

        // O VEU DEIXA A ARENA APARECER POR TRAS. A escolha acontece DENTRO da
        // corrida, e nao numa tela a parte: ver o campo recem-limpo atras das
        // cartas e o que liga a recompensa ao que acabou de ser feito.
        g.setColor(new Color(Palette.VEIL.getRed(), Palette.VEIL.getGreen(),
                Palette.VEIL.getBlue(), (int) (Palette.VEIL.getAlpha() * entrada)));
        g.fillRect(0, 0, w, h);
        if (entrada < 1f) {
            return;
        }

        Font titulo = FontHandler.font(FontHandler.Game, 14f * s);
        String texto = "Choose your reward";
        int lg = FontHandler.getWidth(texto, titulo);
        int ty = caixas.get(0).y - 10 * s;
        g.setFont(titulo);
        g.setColor(Palette.OUTLINE);
        g.drawString(texto, w / 2 - lg / 2 + s, ty + s);
        g.setColor(Palette.TEXT);
        g.drawString(texto, w / 2 - lg / 2, ty);

        for (int i = 0; i < cartas.size(); i++) {
            desenharCarta(g, i, s);
        }
    }

    /** Altura da faixa de orelhas da moldura, em pixels nativos. */
    private static final int ORELHAS = 6;

    /**
     * Desenha a carta com a MOLDURA DO JOGO, e nao com um retangulo.
     *
     * A primeira versao pintava um retangulo liso com borda. Funcionava, mas era a
     * unica caixa do jogo sem as orelhinhas de gato — o painel de personagem, os
     * botoes e a loja todos usam a mesma moldura de nove fatias, e ela e a
     * assinatura visual da interface. Reusar a mesma peca custa menos codigo e
     * mantem a tela dentro da familia.
     *
     * O estado da moldura acompanha o foco, do mesmo jeito que num botao: e o
     * vocabulario que o jogador ja aprendeu no resto do jogo.
     */
    /** Selos ja carregados, um por elemento. */
    private static final java.util.Map<String, BufferedImage> SELOS = new java.util.HashMap<>();

    private static BufferedImage selo(String nome) {
        if (nome == null) {
            return null;
        }
        return SELOS.computeIfAbsent(nome, n -> {
            try {
                return javax.imageio.ImageIO.read(Recompensa.class.getResourceAsStream(
                        "/com/retronova/resources/sprites/items/elementos/" + n + ".png"));
            } catch (Exception naoTem) {
                return null;
            }
        });
    }

    private double respiro;

    /** Contornos ja tracados, por tamanho, estado e cor. */
    private static final java.util.Map<String, BufferedImage> CONTORNOS =
            new java.util.HashMap<>();

    /**
     * Traca a borda da moldura na cor pedida.
     *
     * GUARDADO EM CACHE porque varrer a imagem pixel a pixel nao pode acontecer
     * sessenta vezes por segundo para tres cartas. A chave leva tamanho, estado e
     * cor: e tudo o que muda o desenho, e nada mais muda.
     */
    private static BufferedImage contornoDe(int largura, int altura, boolean ativa,
                                            int escala, java.awt.Color cor) {
        if (largura <= 0 || altura <= 0) {
            return null;
        }
        String chave = largura + "x" + altura + ":" + ativa + ":" + cor.getRGB();
        return CONTORNOS.computeIfAbsent(chave, k -> {
            BufferedImage peca = new BufferedImage(largura, altura,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D gp = peca.createGraphics();
            Button.frame(ativa).draw(gp, ativa ? 1 : 0, 0, 0, largura, altura, escala);
            gp.dispose();

            BufferedImage saida = new BufferedImage(largura, altura,
                    BufferedImage.TYPE_INT_ARGB);
            int rgb = cor.getRGB() | 0xFF000000;
            for (int y = 0; y < altura; y++) {
                for (int x = 0; x < largura; x++) {
                    if ((peca.getRGB(x, y) >>> 24) < 16) {
                        continue;                       // vazio nao e borda
                    }
                    if (naBorda(peca, x, y, escala * 2)) {
                        saida.setRGB(x, y, rgb);
                    }
                }
            }
            return saida;
        });
    }

    /**
     * Tem tinta e esta a no maximo {@code raio} pixels do vazio.
     *
     * O RAIO EXISTE PORQUE A MOLDURA E AMPLIADA. Ela e desenhada em escala inteira,
     * entao cada pixel de arte vira um quadrado de {@code escala} pixels na tela; um
     * contorno de um pixel de TELA e um sexto de pixel de ARTE numa janela grande, e
     * some. Foi o "ta 1p so". Dois pixels de arte — {@code escala * 2} — e a
     * espessura que a propria moldura usa nas linhas dela, entao o contorno passa a
     * ter o mesmo peso do desenho que ele acompanha.
     */
    private static boolean naBorda(BufferedImage im, int x, int y, int raio) {
        for (int dy = -raio; dy <= raio; dy++) {
            for (int dx = -raio; dx <= raio; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= im.getWidth() || ny >= im.getHeight()) {
                    return true;
                }
                if ((im.getRGB(nx, ny) >>> 24) < 16) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A cor da carta.
     *
     * AGORA E SEMPRE A RARIDADE, e nao mais o elemento. Enquanto a carta era uma
     * arma, o elemento ganhava por ser a informacao mais especifica; uma melhoria
     * nao tem elemento, e a raridade voltou a ser o unico eixo — que e tambem o
     * unico que o jogador precisa comparar entre as tres.
     */
    private static java.awt.Color corDe(Melhorias.Carta carta) {
        return carta.raridade().cor();
    }

    /**
     * A carta, dividida em faixas — e a divisao e o assunto aqui.
     *
     * O QUE ESTAVA ERRADO. Tudo abaixo da arte era a mesma coisa: nome, dano,
     * velocidade e efeitos desciam centralizados, com o mesmo tamanho e o mesmo
     * espaco entre linhas. Nao havia como o olho saber onde acaba a identidade do
     * item e onde comecam os numeros dele — a carta virava um paragrafo curto, e
     * paragrafo se le do inicio ao fim, o que e o oposto do que uma escolha de tres
     * cartas em alguns segundos precisa.
     *
     * COMO FICOU, que e o arranjo que ficha de item usa desde os primeiros RPG de
     * saque e que todo jogador ja sabe ler sem ser ensinado:
     *
     *   1. ARTE, com os selos de raridade e elemento nos cantos.
     *   2. IDENTIDADE — o nome na cor da raridade e, abaixo, o degrau por extenso.
     *      Responde "o que e isto e vale a pena?" com uma cor e uma palavra.
     *   3. NUMEROS — rotulo a esquerda, valor a direita, uma linha por atributo. O
     *      alinhamento em duas colunas e o que faz esta faixa ser COMPARAVEL: as
     *      tres cartas tem os valores na mesma altura e na mesma coluna, entao da
     *      para correr o olho na vertical em vez de reler cada carta inteira.
     *   4. EFEITOS — o texto livre da arma, alinhado a esquerda e com marcador. E a
     *      unica faixa que se le como frase, e por isso e a ultima.
     *
     * As reguas entre as faixas valem menos que a margem em volta delas: quem separa
     * e o espaco, a linha so confirma.
     */
    private void desenharCarta(Graphics2D g, int i, int s) {
        Rectangle r = caixas.get(i);
        boolean ativa = i == foco;
        Rectangle caixa = ativa
                ? new Rectangle(r.x - 2 * s, r.y - 2 * s, r.width + 4 * s, r.height + 4 * s)
                : r;

        Melhorias.Carta carta = cartas.get(i);
        java.awt.Color daCarta = corDe(carta);

        // O BRILHO VEM ANTES DA MOLDURA, para ficar por tras dela.
        //
        // E O QUANTO ELA BRILHA VEM DA RARIDADE, nao do mouse. Aceso igual em tudo,
        // o halo era enfeite; numa escada, ele mesmo e a informacao. O foco
        // ACRESCENTA luz mas nao iguala: uma comum sob o mouse continua mais
        // discreta que uma lendaria parada, porque o brilho esta dizendo o valor da
        // carta e nao onde esta o cursor.
        respiro += 0.05;
        float pulso = (float) (0.75 + 0.25 * Math.sin(respiro + i));
        float luz = Math.min(1f, carta.raridade().brilho() + (ativa ? 0.3f : 0f));
        if (luz > 0.01f) {
            int camadas = 5;
            int alcance = (int) (8 * s * pulso * luz);
            Graphics2D halo = (Graphics2D) g.create();
            halo.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            for (int k = camadas; k >= 1; k--) {
                int m = alcance * k / camadas;
                int alfa = (int) (52 * luz * pulso * (1f - (k - 1) / (float) camadas));
                halo.setColor(new java.awt.Color(daCarta.getRed(), daCarta.getGreen(),
                        daCarta.getBlue(), Math.max(0, alfa)));
                halo.fillRoundRect(caixa.x - m, caixa.y - m,
                        caixa.width + m * 2, caixa.height + m * 2, 6 * s, 6 * s);
            }
            halo.dispose();
        }

        Button.frame(ativa).draw(g, ativa ? 1 : 0,
                caixa.x, caixa.y, caixa.width, caixa.height, s);

        // UM SELO SO, no canto esquerdo: o da raridade.
        //
        // O canto direito era do elemento, e melhoria nao tem elemento. Deixar um
        // desenho ali so para preencher seria inventar informacao — o vazio diz a
        // verdade, e o olho para de procurar um segundo eixo que nao existe.
        int selo = 11 * s;
        int cantoY = caixa.y + ORELHAS * s + 2 * s;
        BufferedImage daRaridade = selo(carta.raridade().simbolo());
        if (daRaridade != null) {
            g.drawImage(daRaridade, caixa.x + 3 * s, cantoY, selo, selo, null);
        }

        BufferedImage contorno = contornoDe(caixa.width, caixa.height, ativa, s, daCarta);
        if (contorno != null) {
            g.drawImage(contorno, caixa.x, caixa.y, null);
        }

        int corpoY = caixa.y + ORELHAS * s;
        int esq = caixa.x + 5 * s;
        int dir = caixa.x + caixa.width - 5 * s;
        int util = dir - esq;
        int fundo = caixa.y + caixa.height - 4 * s;

        // ---------------------------------------------------------- 1. o icone
        BufferedImage arte = icone(carta.melhoria());
        int lado = 30 * s;
        if (arte != null) {
            g.drawImage(arte, caixa.x + caixa.width / 2 - lado / 2, corpoY + 2 * s,
                    lado, lado, null);
        }
        int y = corpoY + lado + 9 * s;

        // ---------------------------------------------------------- 2. identidade
        Font nome = FontHandler.font(FontHandler.Game, 6f * s);
        for (String linha : quebrar(carta.melhoria().nome(), nome, util)) {
            escreverCentrado(g, linha, caixa, y, carta.raridade().cor(), s, nome);
            y += 7 * s;
        }
        Font miuda = FontHandler.font(FontHandler.Game, 4f * s);
        escreverCentrado(g, carta.raridade().rotulo().toUpperCase(), caixa, y,
                Palette.LIGHT, s, miuda);
        y += 3 * s;

        // ---------------------------------------------------------- 3. o efeito
        //
        // E A UNICA LINHA QUE IMPORTA, e por isso ela fica sozinha na propria
        // faixa e no maior corpo depois do nome. Uma carta de melhoria diz uma
        // coisa so — "+6 de dano" — e enterrar isso numa lista de atributos, como
        // a carta de arma fazia, seria esconder o unico dado da decisao.
        Font ficha = FontHandler.font(FontHandler.Game, 5f * s);
        y = regua(g, esq, dir, y, s, daCarta);
        for (String linha : quebrar(carta.efeito(), ficha, util)) {
            if (y > fundo) {
                return;
            }
            escreverCentrado(g, linha, caixa, y, Palette.TEXT, s, ficha);
            y += 7 * s;
        }

        // ---------------------------------------------------------- 4. o que ja tem
        //
        // SO APARECE A PARTIR DA SEGUNDA COPIA, e existe para explicar um numero
        // que de outra forma pareceria defeito: a terceira "Furia" rende menos que
        // a primeira, de proposito, e sem esta linha o jogador veria a mesma carta
        // valendo menos sem entender por que. Dizer quantas ele tem transforma o
        // desgaste de surpresa em regra.
        if (carta.jaTinha() > 0) {
            y = regua(g, esq, dir, y - 2 * s, s, daCarta);
            if (y <= fundo) {
                escreverCentrado(g, "You have " + carta.jaTinha(), caixa, y,
                        Palette.ACCENT, s, ficha);
            }
        }
    }

    /**
     * A regua entre duas faixas, e o espaco que ela reserva.
     *
     * Devolve a linha de base seguinte ja afastada: quem chama nunca precisa saber
     * quanto de margem uma divisao pede, e por isso as divisoes da carta tem
     * exatamente a mesma respiracao.
     */
    private static int regua(Graphics2D g, int esq, int dir, int y, int s, Color cor) {
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 100));
        g.fillRect(esq, y, dir - esq, s);
        return y + 8 * s;
    }

    /** Icones ja carregados, um por melhoria. */
    private static final java.util.Map<String, BufferedImage> ICONES =
            new java.util.HashMap<>();

    private static BufferedImage icone(com.retronova.game.items.Melhoria melhoria) {
        return ICONES.computeIfAbsent(melhoria.name().toLowerCase(), n -> {
            try {
                return javax.imageio.ImageIO.read(Recompensa.class.getResourceAsStream(
                        "/com/retronova/resources/sprites/items/melhorias/" + n + ".png"));
            } catch (Exception naoTem) {
                return null;
            }
        });
    }

    /** Quebra pela largura medida da fonte, como o painel de personagem. */
    private static List<String> quebrar(String texto, Font fonte, int larguraUtil) {
        List<String> saida = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split("\s+")) {
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

    /** Texto com a sombra de um pixel, ancorado a esquerda. */
    private void escrever(Graphics2D g, String texto, int x, int y,
                          Color cor, int s, Font fonte) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        g.setFont(fonte);
        g.setColor(Palette.OUTLINE);
        g.drawString(texto, x + s, y + s);
        g.setColor(cor);
        g.drawString(texto, x, y);
    }

    /**
     * O mesmo, ancorado a direita.
     *
     * E o que faz a faixa de numeros ser comparavel entre as tres cartas: com os
     * valores terminando todos na mesma coluna, o olho desce em linha reta em vez
     * de procurar onde cada um comeca.
     */
    private void escreverADireita(Graphics2D g, String texto, int dir, int y,
                                  Color cor, int s, Font fonte) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        escrever(g, texto, dir - FontHandler.getWidth(texto, fonte), y, cor, s, fonte);
    }

    private void escreverCentrado(Graphics2D g, String texto, Rectangle caixa, int y,
                                  Color cor, int s, Font fonte) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        g.setFont(fonte);
        int lg = FontHandler.getWidth(texto, fonte);
        int x = caixa.x + caixa.width / 2 - lg / 2;
        g.setColor(Palette.OUTLINE);
        g.drawString(texto, x + s, y + s);
        g.setColor(cor);
        g.drawString(texto, x, y);
    }

    @Override
    public void dispose() {
    }
}
