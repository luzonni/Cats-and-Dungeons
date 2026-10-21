package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.Engine;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.map.Camera;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.Investida;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.utilities.Skull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * O chefe final da corrida: aparece aos 30 minutos, em três fases.
 *
 * O relógio da corrida vai até 30:00, e é ele que dá o fim. Até aqui os chefes
 * de cinco em cinco minutos eram gatos; o último é o rato que comanda a horda
 * inteira. Cada fase muda o que o jogador precisa fazer:
 *
 *  1. PERSEGUIÇÃO (acima de 66% de vida): só ele, com o golpe pesado. É o
 *     aquecimento — o jogador aprende o ritmo da investida.
 *  2. A CORTE (de 66% a 33%): ele passa a chamar ratos de tempos em tempos.
 *     A luta deixa de ser um contra um e pede espaço para lidar com a horda.
 *  3. A FÚRIA (abaixo de 33%): fica mais rápido e, de tempos em tempos, PARA e
 *     dispara um anel de caveiras. A parada é o aviso: o anel vermelho no chão
 *     cresce por 0,75 s antes de sair, e as caveiras deixam brechas para passar.
 *
 * Ainda não há arte própria: usa o escudeiro ampliado três vezes, pixel a pixel.
 */
public class RatoRei extends Chefe {

    private static final int AMPLIACAO = 3;

    /** Ticks entre duas chamadas de ratos, na segunda fase em diante. */
    private static final int INVOCACAO = 60 * 7;
    /** Ratos chamados de cada vez. */
    private static final int RATOS_POR_CHAMADA = 3;
    /** A corte nunca passa disto: a chamada espera abrir lugar. */
    private static final int LIMITE_DE_RATOS = 14;

    /** Ticks entre duas ondas de caveiras, na terceira fase. */
    private static final int ONDA = 60 * 5;
    /** Quanto dura o aviso antes do anel sair: 0,75 s. */
    private static final int AVISO_DA_ONDA = 45;
    private static final int CAVEIRAS_NO_ANEL = 14;
    /** Alcance do aviso desenhado, em tiles: é onde o anel chega antes de sumir. */
    private static final double ALCANCE_DO_ANEL = 5;

    private int countAnim;
    private int ateAInvocacao = INVOCACAO / 2;
    private int ateAOnda = ONDA;
    /** Ticks desde que o aviso da onda começou; -1 quando não há onda armada. */
    private int ondaTick = -1;
    private final Map<BufferedImage, BufferedImage> ampliados = new IdentityHashMap<>();

    @Override
    public String nome() {
        return "Rat King";
    }

    @Override
    public int fases() {
        return 3;
    }

    public RatoRei(int ID, double x, double y) {
        super(ID, x, y, 120);
        setWidth(AMPLIACAO);
        setHeight(AMPLIACAO);
        loadSprites("mousesquire");
        setSolid();
        setLife(6000);
        setSpeed(2.6);
        setArmadura(400);
        setXpWeight(300d);
        golpeCorpoACorpo(Investida.pesada(), 2.4, 40, AttackTypes.Melee,
                16, 50, Sounds.MouseSquire);
    }

    @Override
    protected void aoMudarDeFase(int nova) {
        Sound.play(Sounds.Cat);
        if (nova == 1) {
            Camera.tremer(5);
            invocar();
        } else if (nova == 2) {
            Camera.tremer(8);
            setSpeed(getSpeed() * 1.3);
            ateAOnda = 60;                 // a primeira onda vem logo: e a mudanca de fase
        }
    }

    @Override
    public void tick() {
        verificarFase();
        animar();

        if (ondaTick >= 0) {
            tickDaOnda();
            return;                        // parado: e o aviso
        }
        if (tickGolpe()) {
            return;
        }
        Player player = Game.getPlayer();
        getPhysical().addForce("move", getSpeed(), player.getAngle(this));

        if (fase() >= 1 && --ateAInvocacao <= 0) {
            ateAInvocacao = INVOCACAO;
            invocar();
        }
        if (fase() >= 2 && --ateAOnda <= 0) {
            ateAOnda = ONDA;
            ondaTick = 0;
        }
    }

    private void animar() {
        if (++countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    // ------------------------------------------------------------ a corte

    /** Chama ratos num círculo em volta dele, se ainda houver lugar na tela. */
    private void invocar() {
        int vivos = Game.getMap().getEntities(MouseSquire.class).size();
        int chamar = Math.min(RATOS_POR_CHAMADA, LIMITE_DE_RATOS - vivos);
        if (chamar <= 0) {
            return;
        }
        double cx = getX() + getWidth() / 2d, cy = getY() + getHeight() / 2d;
        double raio = getWidth() * 0.9;
        double giro = Engine.RAND.nextDouble() * Math.PI * 2;
        for (int i = 0; i < chamar; i++) {
            double a = giro + i * Math.PI * 2 / chamar;
            Entity rato = Entity.build(EntityIDs.MouseSquire.ordinal(), 0, 0);
            rato.setX(cx + Math.cos(a) * raio - rato.getWidth() / 2d);
            rato.setY(cy + Math.sin(a) * raio - rato.getHeight() / 2d);
            Game.getMap().put(rato);
        }
        Sound.play(Sounds.MouseSquire);
    }

    // ------------------------------------------------------------ a furia

    private void tickDaOnda() {
        ondaTick++;
        if (ondaTick < AVISO_DA_ONDA) {
            return;
        }
        // O anel sai: caveiras igualmente espacadas, com o desvio sorteado para
        // as brechas nao cairem sempre nos mesmos pontos.
        double cx = getX() + getWidth() / 2d, cy = getY() + getHeight() / 2d;
        double desvio = Engine.RAND.nextDouble() * Math.PI * 2;
        for (int i = 0; i < CAVEIRAS_NO_ANEL; i++) {
            double a = desvio + i * Math.PI * 2 / CAVEIRAS_NO_ANEL;
            Game.getMap().put(new Skull(cx, cy, a, this));
        }
        Camera.tremer(4);
        Sound.play(Sounds.Woosh);
        ondaTick = -1;
    }

    // ------------------------------------------------------------ desenho

    @Override
    public void render(Graphics2D g) {
        if (ondaTick >= 0) {
            desenharAviso(g);
        }
        int orientation = getPhysical().getOrientation()[0];
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(ampliar(getSprite()), 1, orientation);
        renderComAviso(sprite, g);
    }

    /**
     * O aviso da onda: um anel vermelho que cresce do chefe até o alcance, e um
     * miolo que acende nos últimos quadros. O jogador lê "vai sair, saia da
     * linha" sem ninguém ter de explicar.
     */
    private void desenharAviso(Graphics2D g) {
        double f = Math.min(1, (ondaTick + Engine.alpha()) / AVISO_DA_ONDA);
        double raio = ALCANCE_DO_ANEL * GameObject.SIZE() * f;
        double cx = getX() + getWidth() / 2d, cy = getY() + getHeight() / 2d;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(Math.max(2f, GameObject.SIZE() / 8f)));
        g2.setColor(new Color(255, 70, 70, (int) (90 + 120 * f)));
        g2.drawOval((int) (cx - raio), (int) (cy - raio), (int) (raio * 2), (int) (raio * 2));
        g2.setColor(new Color(255, 40, 40, (int) (30 + 90 * f * f)));
        double miolo = getWidth() * (0.6 + 0.5 * f);
        g2.fillOval((int) (cx - miolo), (int) (cy - miolo), (int) (miolo * 2), (int) (miolo * 2));
        g2.dispose();
    }

    private BufferedImage ampliar(BufferedImage original) {
        return ampliados.computeIfAbsent(original, o -> {
            BufferedImage grande = new BufferedImage(o.getWidth() * AMPLIACAO,
                    o.getHeight() * AMPLIACAO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = grande.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(o, 0, 0, grande.getWidth(), grande.getHeight(), null);
            g.dispose();
            return grande;
        });
    }
}
