package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Brilho;
import com.retronova.engine.graphics.Familia;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;

import java.awt.Graphics2D;

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
 * COMO SE SABE QUE DÁ PRA PUXAR. O cursor vira patinha em cima dela, a peça ganha
 * uma luz suave sem moldura, e a arte tem punho de madeira
 * no meio do ferro — a única madeira daquela parede, que é o que diz onde a mão
 * vai.
 */
public class Lever extends Furniture {

    /** Alcance para puxar, em tiles. O mesmo da placa e do vendedor. */
    private static final double ALCANCE = 3;

    /** Ticks por quadro do puxão. Curto: a mão é rápida, o portão é que é lento. */
    private static final int TICKS_POR_QUADRO = 4;

    /** Fase do halo em radianos por tick; independe do FPS. */
    private static final double PASSO_DO_BRILHO = 0.08;

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
     * @param sprite qual alavanca desenhar, vindo do JSON do mapa. A da antecamara
     *               e a padrao, azul-acinzentada como o resto daquela sala.
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

    private void puxar() {
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
        if (puxada) {
            return;
        }

        float intensidade = (float) (0.7 + 0.3 * Math.sin(pulso * PASSO_DO_BRILHO));
        int raio = (int) (GameObject.SIZE() * (0.9 + 0.25 * intensidade));
        int cx = (int) getX() + getWidth() / 2;
        int cy = (int) getY() + getHeight() / 2;

        Brilho.disco(g, cx, cy, raio, Familia.OURO.tom(Familia.LUZ, (int) (34 * intensidade)));
    }

    @Override
    public void render(Graphics2D g) {
        brilhar(g);
        super.render(g);
    }

    @Override
    public boolean interagivel() {
        return true;
    }

    private boolean perto() {
        return Game.getPlayer().getDistance(this) <= GameObject.SIZE() * ALCANCE;
    }

}
