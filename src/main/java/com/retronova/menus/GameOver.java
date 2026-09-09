package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.room.Room;
import com.retronova.menus.shared.Button;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * A tela de fim de partida.
 *
 * O QUE ELA ERA. Preto cheio, "Game Over" em branco no meio, e uma linha piscando
 * pedindo ESC. Preto puro e branco puro nao aparecem em lugar nenhum da paleta do
 * jogo; a fonte era a certa, mas todo o resto vinha de outro lugar — e o resultado
 * era a unica tela que nao parecia do mesmo jogo que as outras. A pausa e as
 * opcoes ja tinham sido refeitas sobre {@link Palette} e sobre {@link Button}, e
 * esta ficou para tras.
 *
 * O QUE MUDA, e por que cada coisa.
 *
 * O FUNDO E O VEU, e nao preto. O mesmo veu da pausa, sobre o mesmo tom de fundo
 * do jogo. Isso ja e metade da diferenca: a tela passa a pertencer a familia.
 *
 * O TITULO GANHA SOMBRA DURA, como o da pausa — contorno deslocado de um pixel de
 * interface. E o tratamento de texto do jogo inteiro, e sem ele o titulo flutua.
 *
 * A LINHA PISCANDO SAI, E ENTRAM BOTOES. Texto piscando pedindo uma tecla e
 * herança de fliperama, e aqui era ainda a unica tela do jogo em que o mouse nao
 * servia para nada. Com os mesmos botoes das outras telas, o jogador reconhece o
 * que fazer sem ler instrucao — e ganha o que faltava de verdade: poder recomecar
 * dali, sem passar pelo menu principal.
 *
 * A ENTRADA E ATRASADA. A tela nasce transparente e ganha corpo em meio segundo,
 * e os botoes so respondem depois disso. Sao duas razoes: a queda do gato acabou
 * de acontecer e merece um instante de silencio antes de virar interface, e
 * qualquer tecla ou clique que o jogador ainda estivesse apertando na hora da
 * morte cairia direto num botao — escolhendo por ele.
 */
public class GameOver implements Activity {

    /** Quadros ate a tela estar inteira, e ate ela aceitar comando. */
    private static final int ENTRADA = 30;

    private final List<Button> botoes = new ArrayList<>();
    private int foco = -1;
    private int entrando;

    /**
     * O titulo pisca, como na versao antiga da tela.
     *
     * Contado em QUADROS e nao em relogio: a versao anterior comparava
     * System.currentTimeMillis a cada passagem, o que faz o piscar andar num ritmo
     * proprio, independente do jogo. Preso ao tick ele bate com tudo o mais que se
     * move na tela, e continua igual se a maquina engasgar.
     *
     * Meio segundo aceso, meio apagado. Menos que isso vira tremor; mais, vira
     * espera.
     */
    private static final int PISCA = 30;
    private int relogioDoPisca;
    private boolean titutoVisivel = true;

    /** Com qual gato a partida perdida foi jogada. */
    private final int gato;

    public GameOver(int gato) {
        this.gato = gato;
        Sound.stopAll();
        Sound.play(Musics.GameOver, true);
        botoes.add(new Button(0, 0, 0, 0, "Try Again", b -> reiniciar()).primary());
        botoes.add(new Button(0, 0, 0, 0, "Main Menu", b -> Engine.heapActivity(new Menu())));
        posicionar();
    }

    /**
     * Comeca uma partida NOVA, e nao um restart.
     *
     * Game.restart() serve a pausa, onde a partida ainda esta viva por baixo: ele
     * pede a interface do jogo em andamento para descartar. Aqui nao ha jogo em
     * andamento — o gameOver ja o tinha dispensado antes desta tela subir — e a
     * chamada estourava com "a activity atual nao e um jogo". O caminho certo e o
     * mesmo da selecao de personagem: montar um jogo do zero com o gato escolhido.
     */
    private void reiniciar() {
        Sound.stopAll();
        Engine.heapActivity(new Game(gato, new Room("beginning")), () -> { });
    }

    private void posicionar() {
        // AS MEDIDAS DO PROPRIO BOTAO, e nao numeros escolhidos aqui.
        //
        // Eu tinha cravado noventa por dezoito, e era pequeno demais e deformado:
        // a moldura do botao e uma imagem de nove fatias, entao ela so fica certa
        // no tamanho para o qual foi cortada. Fora dele, os cantos esticam — era
        // aquele contorno errado no alto de cada botao. A pausa e o menu ja pediam
        // a medida ao Button; esta tela nao pedia.
        int s = Configs.UiScale();
        int largura = Button.preferredWidth();
        int altura = Button.preferredHeight();
        int espaco = altura + 4 * s;
        int x = Engine.window.getWidth() / 2 - largura / 2;
        int y = Engine.window.getHeight() / 2 + 6 * s;
        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setBounds(new Rectangle(x, y + i * espaco, largura, altura));
        }
    }

    @Override
    public void tick() {
        posicionar();
        if (entrando < ENTRADA) {
            entrando++;
            return;
        }
        if (++relogioDoPisca >= PISCA) {
            relogioDoPisca = 0;
            titutoVisivel = !titutoVisivel;
        }
        if (KeyBoard.KeyPressed("Escape")) {
            Engine.heapActivity(new Menu());
            return;
        }
        navegarPeloTeclado();

        // O MOUSE APAGA O FOCO DE TECLADO, e nao o assume.
        //
        // Eu estava fazendo o contrario — passar o mouse marcava foco — e foco
        // desenha um ANEL em volta do botao. Dai o quadrado que so aparecia aqui:
        // nas outras telas o anel e exclusivo da navegacao por teclado, e quem usa
        // o mouse ve apenas o brilho e o realce. Sao duas linguagens diferentes de
        // "este e o botao da vez", uma para cada dispositivo, e misturar as duas
        // deixa a tela fora do padrao do resto do jogo.
        for (int i = 0; i < botoes.size(); i++) {
            Button botao = botoes.get(i);
            botao.tick();
            if (botao.isHovered()) {
                foco = -1;
            }
            botao.setFocused(foco == i);
        }
    }

    private void navegarPeloTeclado() {
        if (KeyBoard.KeyPressed("Down")) {
            foco = (foco + 1) % botoes.size();
        } else if (KeyBoard.KeyPressed("Up")) {
            foco = (foco <= 0 ? botoes.size() : foco) - 1;
        } else if (KeyBoard.KeyPressed("Enter") && foco >= 0) {
            botoes.get(foco).activate();
        }
    }

    @Override
    public void render(Graphics2D g) {
        float entrada = Math.min(1f, entrando / (float) ENTRADA);
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();

        g.setColor(Palette.DARKEST);
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(Palette.VEIL.getRed(), Palette.VEIL.getGreen(),
                Palette.VEIL.getBlue(), (int) (Palette.VEIL.getAlpha() * entrada)));
        g.fillRect(0, 0, w, h);

        if (entrada < 1f) {
            return;
        }
        desenharTitulo(g, w, h);
        for (Button botao : botoes) {
            botao.render(g);
        }
    }

    private void desenharTitulo(Graphics2D g, int w, int h) {
        int s = Configs.UiScale();
        Font fonte = FontHandler.font(FontHandler.Game, 28f * s);
        String titulo = "Game Over";
        int largura = FontHandler.getWidth(titulo, fonte);
        int x = w / 2 - largura / 2;
        int y = h / 2 - 14 * s;
        // So o TITULO pisca. O risco e a legenda ficam, senao a tela inteira
        // cintila e o olho nao tem onde descansar enquanto le as opcoes.
        if (titutoVisivel) {
            g.setFont(fonte);
            g.setColor(Palette.OUTLINE);
            g.drawString(titulo, x + s, y + s);
            g.setColor(Palette.TEXT);
            g.drawString(titulo, x, y);
        }

        // Um risco sob o titulo, na cor de realce: separa o fim da partida das
        // escolhas que vem depois dele, sem precisar de mais uma caixa na tela.
        g.setColor(Palette.LIGHT);
        g.setStroke(new BasicStroke(s));
        int meio = w / 2;
        g.drawLine(meio - largura / 3, y + 5 * s, meio + largura / 3, y + 5 * s);

        Font legenda = FontHandler.font(FontHandler.Game, 9f * s);
        String texto = "The dungeon keeps the floor.";
        int lg = FontHandler.getWidth(texto, legenda);
        g.setFont(legenda);
        g.setColor(Palette.OUTLINE);
        g.drawString(texto, meio - lg / 2 + s, y + 15 * s + s);
        g.setColor(Palette.LIGHT);
        g.drawString(texto, meio - lg / 2, y + 15 * s);
    }

    @Override
    public void dispose() {
    }
}
