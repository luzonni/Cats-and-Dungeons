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
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
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
 * O QUE ELA AINDA NAO E. Por enquanto as cartas so oferecem ARMAS, e a oferta e
 * sorteio simples. Bencaos, melhorias de atributo, raridade e cartas que
 * conversam entre si sao o passo seguinte; o encanamento — sortear, mostrar,
 * escolher, entregar — e o mesmo, e ja esta aqui.
 */
public class Recompensa implements Activity {

    /** Quantas cartas sao oferecidas. Tres e o numero que vira decisao sem virar lista. */
    private static final int QUANTAS = 3;

    /**
     * Itens que nao entram no sorteio.
     *
     * Os consumiveis estao em espera no jogo todo, e o ima foi retirado. Cair uma
     * carta de item desativado seria oferecer ao jogador uma escolha que nao vale
     * nada — pior que nao oferecer.
     */
    private static final java.util.Set<ItemIDs> FORA = java.util.EnumSet.of(
            ItemIDs.Feed, ItemIDs.Acorn, ItemIDs.Catnip, ItemIDs.Watermelon,
            ItemIDs.MagneticOrb, ItemIDs.Bomb, ItemIDs.GasBomb);

    private final List<Item> cartas = new ArrayList<>();
    private final List<Rectangle> caixas = new ArrayList<>();
    private final Runnable aoEscolher;

    private int foco = -1;
    private int entrando;

    /** Quadros de entrada. A tela nasce transparente para nao cortar o combate no talo. */
    private static final int ENTRADA = 18;

    public Recompensa(Runnable aoEscolher) {
        this.aoEscolher = aoEscolher;
        for (ItemIDs id : sortear()) {
            cartas.add(Item.build(id.ordinal()));
        }
        posicionar();
    }

    /**
     * Sorteia sem repetir.
     *
     * Embaralhar a lista inteira e tirar as tres primeiras, em vez de sortear tres
     * vezes: sorteio independente repete, e duas cartas iguais na mesma oferta
     * desperdicam uma das tres opcoes.
     */
    private List<ItemIDs> sortear() {
        List<ItemIDs> possiveis = new ArrayList<>();
        for (ItemIDs id : ItemIDs.values()) {
            if (!FORA.contains(id)) {
                possiveis.add(id);
            }
        }
        Collections.shuffle(possiveis, Engine.RAND);
        return possiveis.subList(0, Math.min(QUANTAS, possiveis.size()));
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
        Game.getPlayer().getInventory().give(cartas.get(qual));
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
     * A cor que representa a carta.
     *
     * O ELEMENTO GANHA DA RARIDADE quando existe, porque e a informacao mais
     * especifica: dizer "epico" numa arma que o jogador ja viu ser de gelo repete o
     * selo em vez de acrescentar. Sem elemento, a raridade e o que ha.
     */
    private static java.awt.Color corDe(Item item) {
        return item.elemento() == com.retronova.game.items.Elemento.NENHUM
                ? item.raridade().cor()
                : item.elemento().cor();
    }

    private Item item(int i) {
        return cartas.get(i);
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

        Item carta = item(i);
        java.awt.Color daCarta = corDe(carta);

        // O BRILHO VEM ANTES DA MOLDURA, para ficar por tras dela.
        //
        // E O QUANTO ELA BRILHA VEM DA RARIDADE, nao do mouse. Aceso igual em tudo,
        // o halo era enfeite; numa escada, ele mesmo e a informacao. O foco
        // ACRESCENTA luz mas nao iguala: uma comum sob o mouse continua mais
        // discreta que uma lendaria parada, porque o brilho esta dizendo o valor do
        // item e nao onde esta o cursor.
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

        // OS SELOS EM CANTOS OPOSTOS: raridade a esquerda, elemento a direita.
        //
        // Empilhados no mesmo canto eles competiam — dois losangos coloridos
        // encostados viram um borrao, e o olho precisa separar um do outro antes de
        // ler qualquer um. Nos dois cantos, cada um tem o proprio espaco e a carta
        // ganha simetria: e o mesmo arranjo que carta de jogo usa ha decadas, com o
        // custo no canto esquerdo e o naipe no direito.
        //
        // E ha uma leitura a mais de graca: quando so o canto esquerdo tem selo, o
        // vazio a direita ja diz "esta arma nao tem elemento" sem desenhar nada.
        int selo = 11 * s;
        int cantoY = caixa.y + ORELHAS * s + 2 * s;
        BufferedImage daRaridade = selo(carta.raridade().simbolo());
        if (daRaridade != null) {
            g.drawImage(daRaridade, caixa.x + 3 * s, cantoY, selo, selo, null);
        }
        BufferedImage doElemento = selo(carta.elemento().simbolo());
        if (doElemento != null) {
            g.drawImage(doElemento, caixa.x + caixa.width - selo - 3 * s, cantoY,
                    selo, selo, null);
        }

        // O CONTORNO SEGUE A SILHUETA DA PECA, e nao um retangulo em volta dela.
        //
        // As duas tentativas anteriores desenharam retangulos: o primeiro so no
        // corpo, deixando as orelhas sem cor; o segundo em volta de tudo, passando
        // reto por cima delas e desenhando um quadrado onde ha duas pontas. Nos dois
        // casos a borda descrevia uma forma que a carta nao tem.
        //
        // A solucao e nao inventar forma nenhuma: a moldura JA TEM o contorno certo,
        // entao ele e LIDO dela. A peca e desenhada uma vez fora da tela, os pixels
        // de borda dela sao achados — os que tem tinta e fazem divisa com o vazio —
        // e sao esses que recebem a cor. O tracado desce na orelha, sobe no vinco,
        // corre reto no lado: acompanha porque e ela.
        BufferedImage contorno = contornoDe(caixa.width, caixa.height, ativa, s, daCarta);
        if (contorno != null) {
            g.drawImage(contorno, caixa.x, caixa.y, null);
        }

        // O CORPO COMECA ABAIXO DAS ORELHAS. Elas ficam para fora do retangulo
        // util; desenhar por cima delas jogaria a arte do item na testa da carta.
        int corpoY = caixa.y + ORELHAS * s;
        int esq = caixa.x + 5 * s;
        int dir = caixa.x + caixa.width - 5 * s;
        int util = dir - esq;
        int fundo = caixa.y + caixa.height - 4 * s;

        // ---------------------------------------------------------- 1. a arte
        BufferedImage arte = carta.getSprite();
        int lado = 30 * s;
        if (arte != null) {
            g.drawImage(arte, caixa.x + caixa.width / 2 - lado / 2, corpoY + 2 * s,
                    lado, lado, null);
        }
        int y = corpoY + lado + 9 * s;

        // ---------------------------------------------------------- 2. identidade
        Font nome = FontHandler.font(FontHandler.Game, 6f * s);
        for (String linha : quebrar(carta.getName(), nome, util)) {
            escreverCentrado(g, linha, caixa, y, carta.raridade().cor(), s, nome);
            y += 7 * s;
        }
        Font miuda = FontHandler.font(FontHandler.Game, 4f * s);
        escreverCentrado(g, carta.raridade().rotulo().toUpperCase(), caixa, y,
                Palette.LIGHT, s, miuda);
        y += 3 * s;

        // ---------------------------------------------------------- 3. numeros
        Font ficha = FontHandler.font(FontHandler.Game, 5f * s);
        y = regua(g, esq, dir, y, s, daCarta);
        for (String[] par : atributos(carta)) {
            if (y > fundo) {
                return;
            }
            escrever(g, par[0], esq, y, Palette.LIGHT, s, ficha);
            escreverADireita(g, par[1], dir, y, Palette.TEXT, s, ficha);
            y += 7 * s;
        }

        // ---------------------------------------------------------- 4. efeitos
        List<String> efeitos = efeitos(carta);
        if (efeitos.isEmpty()) {
            return;
        }
        y = regua(g, esq, dir, y - 2 * s, s, daCarta);
        for (String efeito : efeitos) {
            boolean primeira = true;
            for (String linha : quebrar(efeito, ficha, util - 5 * s)) {
                if (y > fundo) {
                    return;
                }
                if (primeira) {
                    g.setColor(daCarta);
                    g.fillRect(esq, y - 2 * s, 2 * s, 2 * s);
                    primeira = false;
                }
                escrever(g, linha, esq + 5 * s, y, Palette.ACCENT, s, ficha);
                y += 6 * s;
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

    /**
     * Os numeros da arma, em pares rotulo/valor.
     *
     * DOIS DELES SAO DERIVADOS DO JOGO, e nao de texto: forca e velocidade saem do
     * elemento e da cadencia que a arma de fato usa para bater, entao a carta nao
     * pode discordar do combate. O dano e o unico que ainda vem escrito a mao nas
     * armas, e por isso e LIDO da linha de ficha em vez de inventado.
     */
    private static List<String[]> atributos(Item item) {
        List<String[]> saida = new ArrayList<>();
        if (item.elemento() != com.retronova.game.items.Elemento.NENHUM
                && item.elemento() != com.retronova.game.items.Elemento.LENDARIA) {
            saida.add(new String[]{"Element", item.elemento().rotulo()});
        }
        String dano = danoExtra(item);
        if (dano != null) {
            saida.add(new String[]{"Damage", "+" + dano});
        }
        double poder = item.elemento().dano(1.0);
        saida.add(new String[]{"Power",
                String.format(java.util.Locale.ROOT, "x%.2f", poder)});
        saida.add(new String[]{"Speed", velocidade(item.cadencia())});
        return saida;
    }

    /**
     * A cadencia dita em palavra, e nao em multiplicador.
     *
     * O numero cru enganaria: cadencia MAIOR e golpe mais LENTO, porque ela mede
     * tempo e nao ritmo. "x1,40" seria lido como "quarenta por cento melhor" por
     * qualquer pessoa que nao tenha lido o codigo.
     */
    private static String velocidade(double cadencia) {
        if (cadencia <= 0.90) {
            return "Very fast";
        }
        if (cadencia < 0.98) {
            return "Fast";
        }
        if (cadencia <= 1.05) {
            return "Normal";
        }
        if (cadencia < 1.25) {
            return "Slow";
        }
        return "Very slow";
    }

    /** O que sobra da ficha depois de os numeros sairem dela. */
    private static List<String> efeitos(Item item) {
        List<String> saida = new ArrayList<>();
        for (String spec : item.getSpecifications()) {
            if (spec == null || spec.isBlank()) {
                continue;
            }
            if (numeroDepoisDeDano(spec) != null) {
                continue;               // ja virou a linha "Damage"
            }
            saida.add(spec);
        }
        return saida;
    }

    private static String danoExtra(Item item) {
        for (String spec : item.getSpecifications()) {
            String numero = numeroDepoisDeDano(spec);
            if (numero != null) {
                return numero;
            }
        }
        return null;
    }

    /** O pedaco que as armas ja escrevem igual ha muito tempo. */
    private static final String MARCA_DE_DANO = "damage +";

    /**
     * "Player damage + 12" vira "12"; qualquer outra linha vira null.
     *
     * SEM EXPRESSAO REGULAR de proposito: a busca e por um pedaco literal, e uma
     * varredura de digitos diz exatamente o que aceita. O ganho e a linha de dano
     * deixar de ser um trecho de texto perdido no meio dos efeitos e virar um valor
     * alinhado com os das cartas vizinhas — que era a queixa de os atributos nao
     * estarem legais.
     */
    private static String numeroDepoisDeDano(String spec) {
        if (spec == null) {
            return null;
        }
        int corte = spec.toLowerCase().indexOf(MARCA_DE_DANO);
        if (corte < 0) {
            return null;
        }
        StringBuilder numero = new StringBuilder();
        for (int k = corte + MARCA_DE_DANO.length(); k < spec.length(); k++) {
            char c = spec.charAt(k);
            if (Character.isDigit(c)) {
                numero.append(c);
            } else if (numero.length() > 0 || c != ' ') {
                break;
            }
        }
        return numero.length() == 0 ? null : numero.toString();
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
