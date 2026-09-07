package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
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
        // Atravessável: é peça de parede, e o tile já bloqueia passagem sozinho.
        super(ID, x, y, 1000, false);
        loadSprites("lever");
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
        Sound.play(Sounds.Crack);
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

    @Override
    public void render(Graphics2D g) {
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
