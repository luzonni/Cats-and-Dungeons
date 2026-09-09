package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.objects.GameObject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Alavanca de parede que levanta o portão da dungeon.
 *
 * SUBSTITUI A PERGUNTA. Antes, pisar no vão abria um "Enter the dungeon?" — e a
 * pergunta voltava toda vez que se passava perto, num lugar por onde se passa o
 * tempo todo para chegar ao vendedor e ao acampamento. Uma pergunta que aparece
 * sozinha e que quase sempre se responde "não" vira ruído, e o jogador aprende a
 * fechá-la sem ler.
 *
 * A alavanca resolve o mesmo problema pelo cenário em vez de pela interface: o
 * portão começa fechado, então passar perto não leva a lugar nenhum e não custa
 * nada; descer é uma coisa que se faz de propósito, com a mão. E a confirmação
 * continua existindo — ela só passou a ser a própria grade subindo, que dura meio
 * segundo e não pede clique nenhum de volta.
 *
 * COMO SE SABE QUE DÁ PRA PUXAR. As mesmas três coisas da placa, porque foram as
 * que funcionaram lá: o cursor vira patinha em cima dela, a peça ganha um
 * contorno que pulsa quando o gato está por perto, e a arte tem punho de madeira
 * no meio do ferro — a única madeira daquela parede, que é o que diz onde a mão
 * vai.
 */
public class Lever extends Furniture {

    /** Alcance para puxar, em tiles. O mesmo da placa e do vendedor. */
    private static final double ALCANCE = 3;

    /** Ticks por quadro do puxão. Curto: a mão é rápida, o portão é que é lento. */
    private static final int TICKS_POR_QUADRO = 4;

    private static final Color REALCE = new Color(0xff, 0xd9, 0x8a);

    /**
     * Só funciona uma vez.
     *
     * Não há por que fechar o portão de novo — a antecâmara é o único lugar de
     * onde se desce, e uma alavanca que alterna só daria ao jogador a chance de
     * trancar a própria saída.
     */
    private boolean puxada;

    private int quadro;
    private int contador;
    private int pulso;

    public Lever(int ID, double x, double y) {
        this(ID, x, y, "lever");
    }

    /**
     * @param sprite qual alavanca desenhar.
     *
     * A da antecamara e azul-acinzentada, como o resto daquela sala. Do lado da
     * porta da cisterna ela aparecia como a unica peca fria da cena — o mesmo
     * problema de paleta que os barris emprestados tinham. A versao repintada vem
     * do mesmo gerador que repinta as paredes, entao as duas combinam por
     * construcao, e nao por coincidencia.
     */
    public Lever(int ID, double x, double y, String sprite) {
        // Atravessável: é peça de parede, e o tile já bloqueia passagem sozinho.
        super(ID, x, y, 1000, false);
        loadSprites(sprite);
    }

    @Override
    public void tick() {
        pulso++;
        if (puxada) {
            animar();
            return;
        }
        if (!perto()) {
            return;
        }
        if (GameMap.mouseOnRect(getBounds())) {
            Engine.window.pointing();
        }
        if (GameMap.clickOnRect(Mouse_Button.LEFT, getBounds())) {
            puxar();
        }
    }

    /**
     * A saida so existe depois que o turno acabou.
     *
     * Na arena dava para chegar na alavanca no primeiro segundo, abrir a porta e
     * seguir para a proxima sala sem enfrentar onda nenhuma — e sem passar pela
     * carta, que e o unico jeito de a corrida crescer. Uma saida aberta o tempo
     * todo transforma cada arena em corredor.
     *
     * A pergunta e feita ao MAPA, e nao guardada aqui: quem sabe se a briga
     * terminou e a arena. Na antecamara nao ha arena nenhuma, e por isso a alavanca
     * do portao principal continua livre — la a porta e a saida de um lugar seguro,
     * nao o premio de uma luta.
     */
    private boolean liberada() {
        if (Game.getMap() instanceof Arena arena) {
            return arena.turnoEncerrado();
        }
        return true;
    }

    private void puxar() {
        if (!liberada()) {
            return;
        }
        Gate portao = portao();
        if (portao == null) {
            System.err.println("Lever sem Gate no mapa: a alavanca não abre nada.");
            return;
        }
        this.puxada = true;
        // ESTALO DE MECANISMO, e nao de cascalho. O Crack e um som de pedra
        // quebrando — servia a grade de ferro que subia arrastando, e ficou para
        // tras quando a grade saiu. O que ha aqui e uma alavanca sendo puxada, e o
        // clique seco do Button e o que uma alavanca faz.
        Sound.play(Sounds.Button);
        portao.abrir();
    }

    /** O cabo desce até o último quadro e fica lá. */
    private void animar() {
        if (quadro >= getSheet().size() - 1) {
            return;
        }
        contador++;
        if (contador >= TICKS_POR_QUADRO) {
            contador = 0;
            quadro++;
            getSheet().setIndex(quadro);
        }
    }

    /**
     * O portão mais próximo do mapa.
     *
     * Procurado em vez de recebido no construtor porque as entidades são criadas
     * na ordem em que aparecem no JSON, e amarrar as duas ali obrigaria a lembrar
     * dessa ordem toda vez que alguém mexesse no arquivo. Com um portão só na
     * sala — que é o caso — "o mais próximo" é sempre o certo.
     */
    private Gate portao() {
        Gate escolhido = null;
        double menor = Double.MAX_VALUE;
        for (Gate g : Game.getMap().getEntities(Gate.class)) {
            double d = getDistance(g);
            if (d < menor) {
                menor = d;
                escolhido = g;
            }
        }
        return escolhido;
    }

    /**
     * O BRILHO DE "E AQUI".
     *
     * A alavanca fica na parede a sala inteira, apagada, e o jogador aprende a nao
     * olhar para ela enquanto briga — e o certo, porque durante a briga ela nao faz
     * nada. O problema e o instante em que ela PASSA a fazer: sem sinal nenhum, ela
     * continua sendo o mesmo pedaco de parede que era ha um segundo.
     *
     * O halo resolve isso e nao pede arte nova. Ele pulsa pela mesma razao da luz
     * da passagem: brilho parado vira mancha e o olho para de ve-lo; um que respira
     * continua chamando enquanto ninguem puxou.
     *
     * E SOME AO SER PUXADA. Depois disso a alavanca nao tem mais nada a dizer, e a
     * luz passa para a passagem — que e para onde o jogador deve olhar em seguida.
     */
    private void brilhar(Graphics2D g) {
        if (puxada || !liberada()) {
            return;
        }
        respiro += 0.08;
        float pulso = (float) (0.7 + 0.3 * Math.sin(respiro));
        int raio = (int) (GameObject.SIZE() * (0.9 + 0.25 * pulso));
        int cx = (int) getX() + getWidth() / 2;
        int cy = (int) getY() + getHeight() / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        int camadas = 4;
        for (int i = camadas; i >= 1; i--) {
            int r = raio * i / camadas;
            int alfa = (int) (34 * pulso * (1f - (i - 1) / (float) camadas));
            g2.setColor(new java.awt.Color(255, 226, 160, Math.max(0, alfa)));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        g2.dispose();
    }

    private double respiro;

    @Override
    public void render(Graphics2D g) {
        brilhar(g);
        super.render(g);
        if (puxada || !perto()) {
            return;
        }
        // Pulso lento, em seno, para o realce respirar em vez de piscar.
        double onda = (Math.sin(pulso / 22d) + 1) / 2;
        int alfa = 60 + (int) (onda * 110);
        Rectangle p = peca();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(new BasicStroke(Math.max(1, Configs.GameScale() / 2f)));
        g2.setColor(new Color(REALCE.getRed(), REALCE.getGreen(), REALCE.getBlue(), alfa));
        g2.drawRect(p.x, p.y, p.width, p.height);
        g2.dispose();
    }

    private boolean perto() {
        return Game.getPlayer().getDistance(this) <= GameObject.SIZE() * ALCANCE;
    }

    /**
     * Retângulo da PEÇA, que não é o do tile.
     *
     * A chapa e o cabo ocupam o miolo da célula; contornar os bounds desenhava o
     * quadro em volta do tile de parede inteiro, e o realce parecia piscar na
     * alvenaria. Mesma razão e mesmos números de grandeza que os da placa.
     */
    private Rectangle peca() {
        int e = Configs.GameScale();
        return new Rectangle((int) getX() + 3 * e, (int) getY() + e, 11 * e, 13 * e);
    }
}
