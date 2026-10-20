package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Estatisticas;
import com.retronova.game.Game;
import com.retronova.game.items.ArmaExtra;
import com.retronova.game.items.Melhorias;
import com.retronova.game.map.Camera;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * As tres armas extras de AREA: Arco Eletrico, Tremor e Chuva de Meteoros.
 *
 * Moram fora do Arsenal porque o jeito delas e outro: as do Arsenal estao
 * SEMPRE ativas (orbitam, queimam); estas DISPARAM de tempos em tempos e deixam
 * um efeito na tela por alguns quadros — o raio, a onda, a cratera. Cada uma
 * tem o proprio relogio e a propria lista de efeitos vivos.
 *
 * Todas miram so em alvos automaticos (nada de gastar raio em caixote) e o
 * dano cresce com o nivel da arma e com o dano do gato, como as do Arsenal.
 */
public final class ArmasDeArea {

    private ArmasDeArea() {
    }

    private static long tempo;
    private static int ateRaio, ateTremor, ateMeteoro;

    // --------------------------------------------------------------- tick

    static void tick(Player gato) {
        tempo++;
        Melhorias m = gato.getMelhorias();
        int raio = m.nivel(ArmaExtra.RAIO);
        int tremor = m.nivel(ArmaExtra.TREMOR);
        int meteoro = m.nivel(ArmaExtra.METEORO);
        if (raio > 0 && --ateRaio <= 0) {
            ateRaio = (int) Math.max(15, (75 - 9 * raio) * Bonus.pressa(gato));
            dispararRaio(gato, raio);
        }
        if (tremor > 0 && --ateTremor <= 0) {
            ateTremor = (int) Math.max(45, (170 - 20 * tremor) * Bonus.pressa(gato));
            dispararTremor(gato, tremor);
        }
        if (meteoro > 0 && --ateMeteoro <= 0) {
            ateMeteoro = (int) Math.max(30, (130 - 14 * meteoro) * Bonus.pressa(gato));
            marcarMeteoros(gato, meteoro);
        }
        tickTremores();
        tickMeteoros(gato, meteoro);
        Estatisticas.fonte(null);   // de volta a arma da mao
        relampagos.removeIf(r -> ++r.idade > Relampago.VIDA);
    }

    private static List<Enemy> alvos() {
        List<Enemy> lista = new ArrayList<>();
        for (Enemy e : Game.getMap().getEntities(Enemy.class)) {
            if (!e.nascendo() && e.alvoAutomatico()) {
                lista.add(e);
            }
        }
        return lista;
    }

    private static double cx(Entity e) {
        return e.getX() + e.getWidth() / 2.0;
    }

    private static double cy(Entity e) {
        return e.getY() + e.getHeight() / 2.0;
    }

    // ---------------------------------------------------- arco eletrico

    /** Um raio desenhado: a lista de pontos (x, y) do zigue-zague e a idade. */
    private static final class Relampago {
        static final int VIDA = 9;
        final List<double[]> pontos;
        int idade;

        Relampago(List<double[]> pontos) {
            this.pontos = pontos;
        }
    }

    private static final List<Relampago> relampagos = new ArrayList<>();

    /**
     * Pula do gato para o bicho mais proximo e dali para o mais proximo ainda nao
     * atingido, 2 + nivel vezes. E dano em area que PERSEGUE: acerta a fila
     * inteira que vem atras, onde a aura e o tremor nao alcancam.
     */
    private static void dispararRaio(Player gato, int nivel) {
        int tile = GameObject.SIZE();
        boolean tempestade = gato.getMelhorias().evoluiu(ArmaExtra.RAIO);
        Estatisticas.fonte(tempestade ? "Thunderstorm" : ArmaExtra.RAIO.nome());
        double dano = (8 + 4 * nivel + gato.getDamage() * 0.4) * (tempestade ? 2 : 1);
        int saltos = (2 + nivel) * (tempestade ? 2 : 1);
        List<Enemy> livres = alvos();
        double x = cx(gato), y = cy(gato);
        double alcance = 7 * tile;
        List<double[]> caminho = new ArrayList<>();
        caminho.add(new double[]{x, y});
        for (int i = 0; i <= saltos; i++) {
            Enemy perto = null;
            double menor = alcance;
            for (Enemy e : livres) {
                double d = Math.hypot(cx(e) - x, cy(e) - y);
                if (d < menor) {
                    menor = d;
                    perto = e;
                }
            }
            if (perto == null) {
                break;
            }
            livres.remove(perto);
            perto.strike(AttackTypes.Laser, dano);
            x = cx(perto);
            y = cy(perto);
            caminho.add(new double[]{x, y});
            alcance = 4 * tile;      // os saltos seguintes sao mais curtos
        }
        if (caminho.size() > 1) {
            relampagos.add(new Relampago(zigueZague(caminho)));
            Sound.play(Sounds.Laser);
        }
    }

    /** Quebra cada trecho reto em pedacos tortos: e o que faz uma linha virar raio. */
    private static List<double[]> zigueZague(List<double[]> caminho) {
        List<double[]> pontos = new ArrayList<>();
        int s = Configs.GameScale();
        for (int i = 0; i < caminho.size() - 1; i++) {
            double[] a = caminho.get(i), b = caminho.get(i + 1);
            int pedacos = 5;
            for (int k = 0; k < pedacos; k++) {
                double t = k / (double) pedacos;
                double desvio = k == 0 ? 0 : (Engine.RAND.nextDouble() - 0.5) * 10 * s;
                double nx = -(b[1] - a[1]), ny = b[0] - a[0];
                double n = Math.max(1, Math.hypot(nx, ny));
                pontos.add(new double[]{a[0] + (b[0] - a[0]) * t + nx / n * desvio,
                        a[1] + (b[1] - a[1]) * t + ny / n * desvio});
            }
        }
        pontos.add(caminho.getLast());
        return pontos;
    }

    // ------------------------------------------------------------ tremor

    private static final class Onda {
        static final int VIDA = 16;
        final double x, y, raioFinal;
        int idade;

        Onda(double x, double y, double raioFinal) {
            this.x = x;
            this.y = y;
            this.raioFinal = raioFinal;
        }
    }

    private static final List<Onda> ondas = new ArrayList<>();

    /** O gato bate no chao: fere e empurra tudo num raio em volta, de uma vez. */
    private static void dispararTremor(Player gato, int nivel) {
        int tile = GameObject.SIZE();
        boolean terremoto = gato.getMelhorias().evoluiu(ArmaExtra.TREMOR);
        Estatisticas.fonte(terremoto ? "Earthshaker" : ArmaExtra.TREMOR.nome());
        double raio = (2.6 + 0.45 * nivel) * tile * Bonus.area(gato) * (terremoto ? 2 : 1);
        double dano = (10 + 5 * nivel + gato.getDamage() * 0.5) * (terremoto ? 2 : 1);
        double x = cx(gato), y = cy(gato);
        for (Enemy e : alvos()) {
            double d = Math.hypot(cx(e) - x, cy(e) - y);
            if (d <= raio) {
                e.strike(AttackTypes.Impact, dano);
                e.getPhysical().addForce("tremor", 6 + nivel, Math.atan2(cy(e) - y, cx(e) - x));
            }
        }
        ondas.add(new Onda(x, y, raio));
        Camera.tremer(3);
        Sound.play(Sounds.Crack);
    }

    private static void tickTremores() {
        ondas.removeIf(o -> ++o.idade > Onda.VIDA);
    }

    // ----------------------------------------------------------- meteoro

    private static final class Meteoro {
        static final int QUEDA = 36;       // aviso no chao antes do impacto
        static final int FOGO = 14;        // explosao depois do impacto
        final double x, y, raio;
        int idade;

        Meteoro(double x, double y, double raio) {
            this.x = x;
            this.y = y;
            this.raio = raio;
        }
    }

    private static final List<Meteoro> meteoros = new ArrayList<>();

    /**
     * Marca o chao embaixo de bichos sorteados por perto; o meteoro cai depois de
     * um instante. O aviso e de proposito: da para LER onde vai cair, e a horda
     * que esta andando pode sair — o premio e acertar onde ela esta mais junta.
     */
    private static void marcarMeteoros(Player gato, int nivel) {
        int tile = GameObject.SIZE();
        List<Enemy> perto = new ArrayList<>();
        for (Enemy e : alvos()) {
            if (e.getDistance(gato) < 9 * tile) {
                perto.add(e);
            }
        }
        int quantos = (1 + nivel / 2) * (gato.getMelhorias().evoluiu(ArmaExtra.METEORO) ? 2 : 1);
        double raio = (1.4 + 0.2 * nivel) * tile * Bonus.area(gato);
        for (int i = 0; i < quantos && !perto.isEmpty(); i++) {
            Enemy alvo = perto.remove(Engine.RAND.nextInt(perto.size()));
            meteoros.add(new Meteoro(cx(alvo), cy(alvo), raio));
        }
    }

    private static void tickMeteoros(Player gato, int nivel) {
        boolean armagedom = gato.getMelhorias().evoluiu(ArmaExtra.METEORO);
        Estatisticas.fonte(armagedom ? "Armageddon" : ArmaExtra.METEORO.nome());
        double dano = (14 + 6 * nivel + gato.getDamage() * 0.6) * (armagedom ? 2 : 1);
        Iterator<Meteoro> it = meteoros.iterator();
        while (it.hasNext()) {
            Meteoro m = it.next();
            m.idade++;
            if (m.idade == Meteoro.QUEDA) {
                for (Enemy e : alvos()) {
                    if (Math.hypot(cx(e) - m.x, cy(e) - m.y) <= m.raio) {
                        e.strike(AttackTypes.Fire, dano);
                    }
                }
                Camera.tremer(2);
                Sound.play(Sounds.Crack);
            }
            if (m.idade > Meteoro.QUEDA + Meteoro.FOGO) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------ render

    public static void render(Graphics2D g) {
        if (relampagos.isEmpty() && ondas.isEmpty() && meteoros.isEmpty()) {
            return;
        }
        float a = Engine.pausado() ? 0f : Engine.alpha();
        int s = Configs.GameScale();
        Graphics2D o = (Graphics2D) g.create();
        for (Onda onda : ondas) {
            double t = Math.min(1, (onda.idade + a) / Onda.VIDA);
            double r = onda.raioFinal * (1 - Math.pow(1 - t, 3));
            float vida = (float) (1 - t);
            o.setColor(new Color(208, 138, 74, (int) (45 * vida)));
            o.fillOval((int) (onda.x - r), (int) (onda.y - r), (int) (r * 2), (int) (r * 2));
            o.setStroke(new BasicStroke(4f * s * vida + s));
            o.setColor(new Color(240, 200, 150, (int) (200 * vida)));
            o.drawOval((int) (onda.x - r), (int) (onda.y - r), (int) (r * 2), (int) (r * 2));
        }
        for (Meteoro m : meteoros) {
            if (m.idade < Meteoro.QUEDA) {
                // Aviso: um circulo vermelho que se fecha, e o meteoro descendo.
                double t = (m.idade + a) / Meteoro.QUEDA;
                o.setColor(new Color(255, 60, 40, (int) (40 + 60 * t)));
                o.fillOval((int) (m.x - m.raio), (int) (m.y - m.raio), (int) (m.raio * 2), (int) (m.raio * 2));
                o.setStroke(new BasicStroke(s));
                o.setColor(new Color(255, 120, 80, 200));
                double rr = m.raio * (1 - t * 0.6);
                o.drawOval((int) (m.x - rr), (int) (m.y - rr), (int) (rr * 2), (int) (rr * 2));
                double altura = (1 - t) * 14 * GameObject.SIZE() / 4.0;
                int tam = 4 * s;
                o.setColor(new Color(255, 200, 90));
                o.fillOval((int) (m.x - tam), (int) (m.y - altura - tam), tam * 2, tam * 2);
                o.setColor(new Color(255, 110, 50, 160));
                o.fillOval((int) (m.x - tam * 1.6), (int) (m.y - altura - tam * 3), (int) (tam * 3.2), tam * 3);
            } else {
                // Explosao: disco laranja que cresce e apaga.
                double t = (m.idade - Meteoro.QUEDA + a) / Meteoro.FOGO;
                double r = m.raio * (0.8 + 0.4 * t);
                float vida = (float) Math.max(0, 1 - t);
                o.setColor(new Color(255, 150, 60, (int) (180 * vida)));
                o.fillOval((int) (m.x - r), (int) (m.y - r), (int) (r * 2), (int) (r * 2));
                o.setColor(new Color(255, 240, 200, (int) (220 * vida)));
                double r2 = r * 0.5;
                o.fillOval((int) (m.x - r2), (int) (m.y - r2), (int) (r2 * 2), (int) (r2 * 2));
            }
        }
        for (Relampago rel : relampagos) {
            float vida = 1f - rel.idade / (float) Relampago.VIDA;
            int[] xs = new int[rel.pontos.size()], ys = new int[rel.pontos.size()];
            for (int i = 0; i < xs.length; i++) {
                xs[i] = (int) rel.pontos.get(i)[0];
                ys[i] = (int) rel.pontos.get(i)[1];
            }
            o.setStroke(new BasicStroke(4f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            o.setColor(new Color(127, 216, 255, (int) (110 * vida)));
            o.drawPolyline(xs, ys, xs.length);
            o.setStroke(new BasicStroke(Math.max(1f, 1.2f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            o.setColor(new Color(255, 255, 255, (int) (255 * vida)));
            o.drawPolyline(xs, ys, xs.length);
        }
        o.dispose();
    }

    /** Partida nova: nada do que estava no ar continua. */
    public static void reiniciar() {
        relampagos.clear();
        ondas.clear();
        meteoros.clear();
        ateRaio = ateTremor = ateMeteoro = 0;
    }
}
