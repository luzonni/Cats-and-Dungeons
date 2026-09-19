package com.retronova.game.objects.particles;

import com.retronova.engine.graphics.Alpha;
import com.retronova.engine.graphics.DrawSprite;
import com.retronova.game.Game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * O rastro que o gato deixa no arranco.
 *
 * ---------------------------------------------------------------------------
 * POR QUE O DASH PARECIA TELEPORTE
 *
 * Ele nunca foi instantâneo — o impulso dura quatro quadros e o gato de fato
 * percorre o caminho. O problema é que nada no caminho ficava. A sessenta quadros
 * por segundo, quatro quadros são 66 milissegundos: o olho não acompanha um
 * objeto que atravessa quatro tiles nesse tempo, ele apenas registra o gato aqui
 * e, no instante seguinte, o gato ali. Sem nada ligando os dois pontos, a
 * conclusão do olho é a única possível: ele não andou, ele pulou.
 *
 * A SOLUÇÃO NÃO É DESACELERAR O DASH. Um arranco lento deixa de ser arranco, e o
 * valor dele é justamente sair de uma enrascada antes que ela feche. O que falta
 * não é tempo, é RASTRO: deixar cópias do gato pelo caminho, apagando, para que o
 * trajeto continue na tela depois de o corpo já ter passado. É a técnica de
 * *afterimage* — fotografar o sprite e desvanecer a foto —, e ela funciona porque
 * devolve ao olho a informação que a velocidade tirou.
 *
 * ---------------------------------------------------------------------------
 * AS ESCOLHAS
 *
 * FICA PARADO ONDE NASCEU. Toda outra partícula daqui deriva — a poeira sai do
 * pé, a faísca sobe. Esta não pode: ela É o lugar por onde o gato passou. Se ela
 * andasse, deixaria de marcar o caminho e viraria um segundo gato correndo atrás
 * do primeiro.
 *
 * BRILHA NA COR DA CORRIDA. {@link DrawSprite#draw} soma cor, então o rastro sai
 * mais CLARO que o gato, e não mais escuro — é luz, não sombra. Duas razões: uma
 * silhueta escura em cima do chão escuro da cisterna some, e a cor do elemento
 * transforma um efeito genérico de movimento numa coisa que também diz de que
 * corrida esta é, sem custar um pixel a mais de arte.
 *
 * A COR ENTRA PELA METADE. Somada cheia, elementos claros como o ar estouram o
 * sprite em branco e o rastro deixa de ter forma de gato — e um rastro que não é
 * reconhecível como o personagem não conta a história do deslocamento.
 *
 * NASCE VELHO QUANDO PRECISA. Quem cria passa o quanto da vida já se foi: é o que
 * permite a várias cópias saírem no MESMO quadro, espalhadas pelo caminho que o
 * gato cobriu naquele quadro, e ainda assim desvanecerem em cascata da mais
 * antiga para a mais nova. Sem isso elas apagariam todas juntas, como um bloco
 * piscando, em vez de um rastro se recolhendo na direção do gato.
 */
public class Rastro extends Particle {

    /** Quanto tempo uma cópia leva para sumir, em quadros. */
    private static final int VIDA = 18;

    /** Quanto da cor do elemento entra na cópia. */
    private static final double TINTA = 0.5;

    /** O brilho de quem não escolheu elemento: um azul frio de velocidade. */
    private static final Color SEM_ELEMENTO = new Color(0x4C6E8C);

    private final BufferedImage copia;
    private final int x;
    private final int y;
    private double idade;

    /**
     * @param sprite  o gato exatamente como foi desenhado neste quadro
     * @param x       canto esquerdo do desenho, em pixels de mundo
     * @param y       topo do desenho, em pixels de mundo
     * @param cor     a cor do elemento da corrida, ou nula
     * @param adianto quanto da vida já se foi, de 0 a 1
     */
    public Rastro(BufferedImage sprite, int x, int y, Color cor, double adianto) {
        super(x, y, VIDA / 60d);
        this.x = x;
        this.y = y;
        this.idade = adianto * VIDA;
        Color base = cor == null ? SEM_ELEMENTO : cor;
        this.copia = DrawSprite.draw(sprite, new Color(
                (int) (base.getRed() * TINTA),
                (int) (base.getGreen() * TINTA),
                (int) (base.getBlue() * TINTA)));
    }

    @Override
    public void tick() {
        idade++;
        if (idade >= VIDA) {
            Game.getMap().remove(this);
        }
    }

    @Override
    public void render(Graphics2D g) {
        // A QUEDA É AO QUADRADO, e não reta. Desvanecendo linearmente, as cópias do
        // meio do trajeto ficam visíveis tempo demais e o que se vê é uma fila de
        // gatos parados; ao quadrado elas somem depressa e sobra a impressão de
        // borrão, que é o que o movimento rápido de fato produz no olho.
        float resto = (float) (1 - idade / VIDA);
        if (resto <= 0) {
            return;
        }
        Alpha.draw(copia, x, y, resto * resto, g);
    }
}
