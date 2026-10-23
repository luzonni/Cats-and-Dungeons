package com.retronova.game.objects.entities;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.game.Estatisticas;
import com.retronova.game.Game;
import com.retronova.engine.graphics.Brilho;
import com.retronova.engine.graphics.Familia;
import com.retronova.game.items.ArmaExtra;
import com.retronova.game.items.Icone;
import com.retronova.game.items.Melhorias;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * O que as armas extras fazem a cada tick e como aparecem.
 *
 * Fica fora do Player porque nao e do gato: e do que ele ganhou na roleta. O
 * Game chama tick e render; os niveis moram em Melhorias, para viajarem no save.
 *
 * O desenho e procedural, de proposito, e no traco da casa: blocos de pixel do
 * tamanho de GameScale. Nao ha folha de sprite para estas armas, e esperar uma
 * seria deixar o sistema sem rosto.
 */
public final class Arsenal {

    private Arsenal() {
    }

    /** Relogio proprio: gira mesmo com o gato parado. */
    private static long tempo;

    /** Ultimo tick em que cada bicho levou de cada arma, para nao bater todo tick. */
    private static final Map<Enemy, long[]> ULTIMO_GOLPE = new WeakHashMap<>();

    // ------------------------------------------------------------ numeros

    private static double area() {
        return Bonus.area(Game.getPlayer());
    }

    private static boolean evoluiu(ArmaExtra arma) {
        return Game.getPlayer().getMelhorias().evoluiu(arma);
    }

    private static int bolas(int nivel) {
        return nivel <= 0 ? 0 : 1 + nivel / 2;                 // 1,2,2,3,3
    }

    private static int livros(int nivel) {
        return nivel <= 0 ? 0 : Math.min(4, nivel == 4 ? 3 : nivel);   // 1,2,3,3,4
    }

    private static double raioAura(int nivel) {
        return (1.4 + 0.3 * nivel) * GameObject.SIZE() * area() * (evoluiu(ArmaExtra.AURA) ? 1.5 : 1);
    }

    // -------------------------------------------------------------- tick

    public static void tick(Player gato) {
        tempo++;
        ArmasDeArea.tick(gato);   // as de area tem relogio proprio
        Melhorias m = gato.getMelhorias();
        int novelo = m.nivel(ArmaExtra.NOVELO);
        int grimorio = m.nivel(ArmaExtra.GRIMORIO);
        int aura = m.nivel(ArmaExtra.AURA);
        if (novelo + grimorio + aura == 0) {
            return;
        }
        List<Enemy> bichos = Game.getMap().getEntities(Enemy.class);
        double cx = gato.getX() + gato.getWidth() / 2.0;
        double cy = gato.getY() + gato.getHeight() / 2.0;
        int s = Configs.GameScale();

        if (novelo > 0) {
            Estatisticas.fonte(evoluiu(ArmaExtra.NOVELO) ? "Giant Yarn" : ArmaExtra.NOVELO.nome());
            double dano = (5 + 2 * novelo + gato.getDamage() * 0.35) * (evoluiu(ArmaExtra.NOVELO) ? 2 : 1);
            int meio = (evoluiu(ArmaExtra.NOVELO) ? 6 : 4) * s;
            for (double[] p : posicoesNovelo(cx, cy, novelo, 0)) {
                Rectangle r = new Rectangle((int) p[0] - meio, (int) p[1] - meio, meio * 2, meio * 2);
                acertar(bichos, r, 0, 18, dano, AttackTypes.Impact, 0, cx, cy);
            }
        }
        if (grimorio > 0) {
            Estatisticas.fonte(evoluiu(ArmaExtra.GRIMORIO) ? "Ancient Tome" : ArmaExtra.GRIMORIO.nome());
            double dano = (9 + 3 * grimorio + gato.getDamage() * 0.5) * (evoluiu(ArmaExtra.GRIMORIO) ? 2 : 1);
            double empurrao = (grimorio >= 4 ? 7 : 4) + (evoluiu(ArmaExtra.GRIMORIO) ? 3 : 0);
            for (double[] p : posicoesGrimorio(cx, cy, grimorio, 0)) {
                Rectangle r = new Rectangle((int) p[0] - 5 * s, (int) p[1] - 6 * s, 10 * s, 12 * s);
                acertar(bichos, r, 1, 30, dano, AttackTypes.Sorcery, empurrao, cx, cy);
            }
        }
        if (aura > 0 && tempo % Math.max(12, (int) ((42 - 4 * aura) * Bonus.pressa(gato))) == 0) {
            boolean sagrado = evoluiu(ArmaExtra.AURA);
            Estatisticas.fonte(sagrado ? "Holy Catnip" : ArmaExtra.AURA.nome());
            double dano = (3 + 1.8 * aura + gato.getDamage() * 0.15) * (sagrado ? 2 : 1);
            int curados = 0;
            double raio = raioAura(aura);
            for (Enemy e : bichos) {
                if (e.nascendo()) {
                    continue;
                }
                double dx = e.getX() + e.getWidth() / 2.0 - cx;
                double dy = e.getY() + e.getHeight() / 2.0 - cy;
                if (dx * dx + dy * dy < raio * raio) {
                    e.strike(AttackTypes.Poison, dano);
                    // Catnip Sagrado: cada bicho queimado devolve um pouco de vida (ate 3 por pulso).
                    if (sagrado && curados++ < 3) {
                        gato.setLife(Math.min(gato.getLifeSize(), gato.getLife() + 0.5));
                    }
                }
            }
        }
        Estatisticas.fonte(null);   // de volta a arma da mao
    }

    private static void acertar(List<Enemy> bichos, Rectangle area, int arma, int intervalo,
                                double dano, AttackTypes tipo, double empurrao,
                                double cx, double cy) {
        for (Enemy e : bichos) {
            if (e.nascendo() || !e.getBounds().intersects(area)) {
                continue;
            }
            long[] ultimo = ULTIMO_GOLPE.computeIfAbsent(e, k -> new long[]{-999, -999});
            if (tempo - ultimo[arma] < intervalo) {
                continue;
            }
            ultimo[arma] = tempo;
            e.strike(tipo, dano);
            if (empurrao > 0) {
                double ang = Math.atan2(e.getY() - cy, e.getX() - cx);
                e.getPhysical().addForce("arsenal", empurrao, ang);
            }
        }
    }

    // ---------------------------------------------------------- posicoes

    private static double[][] posicoesNovelo(double cx, double cy, int nivel, float alpha) {
        int n = bolas(nivel);
        double raio = 1.7 * GameObject.SIZE() * area();
        double giro = (tempo + alpha) * 0.085;
        double[][] p = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = giro + i * Math.PI * 2 / n;
            p[i] = new double[]{cx + Math.cos(a) * raio, cy + Math.sin(a) * raio};
        }
        return p;
    }

    private static double[][] posicoesGrimorio(double cx, double cy, int nivel, float alpha) {
        int n = livros(nivel);
        double raio = 2.7 * GameObject.SIZE() * area() * (evoluiu(ArmaExtra.GRIMORIO) ? 1.25 : 1);
        double giro = -(tempo + alpha) * 0.05;    // ao contrario do novelo
        double[][] p = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = giro + i * Math.PI * 2 / n;
            p[i] = new double[]{cx + Math.cos(a) * raio, cy + Math.sin(a) * raio};
        }
        return p;
    }

    // ------------------------------------------------------------ render

    /**
     * Desenha as armas em volta do gato. Chamado DENTRO da translacao de
     * interpolacao do gato, entao usa a posicao de tick dele e anda junto.
     */
    public static void render(Graphics2D g, Player gato) {
        Melhorias m = gato.getMelhorias();
        int novelo = m.nivel(ArmaExtra.NOVELO);
        int grimorio = m.nivel(ArmaExtra.GRIMORIO);
        int aura = m.nivel(ArmaExtra.AURA);
        if (novelo + grimorio + aura == 0) {
            return;
        }
        float alpha = Engine.pausado() ? 0f : Engine.alpha();
        double cx = gato.getX() + gato.getWidth() / 2.0;
        double cy = gato.getY() + gato.getHeight() / 2.0;
        int s = Configs.GameScale();

        if (aura > 0) {
            // A AURA É A FOLHA DO CARD: o campo na família verde dos ícones (ouro na
            // evolução, a Holy Catnip), a borda num tom de luz e três folhas do
            // próprio ícone girando devagar na borda. Disco de imagem pronta (Brilho),
            // e não fillOval suavizado por quadro.
            boolean sagrado = evoluiu(ArmaExtra.AURA);
            Familia f = sagrado ? Familia.OURO : Familia.VERDE;
            int r = (int) raioAura(aura);
            double pulso = 0.5 + 0.5 * Math.sin((tempo + alpha) * 0.12);
            Brilho.disco(g, cx, cy, r, f.tom(Familia.BASE, (int) (28 + 22 * pulso)));
            g.setColor(f.tom(Familia.LUZ, (int) (70 + 50 * pulso)));
            g.setStroke(new BasicStroke(s));
            g.drawOval((int) cx - r, (int) cy - r, r * 2, r * 2);
            int folha = Math.max(1, s / 2);
            for (int i = 0; i < FOLHAS_DA_AURA; i++) {
                double a = (tempo + alpha) * GIRO_DAS_FOLHAS + i * 2 * Math.PI / FOLHAS_DA_AURA;
                Icone.AURA.desenharCentrado(g, (int) (cx + Math.cos(a) * r), (int) (cy + Math.sin(a) * r), folha);
            }
        }
        if (grimorio > 0) {
            for (double[] p : posicoesGrimorio(cx, cy, grimorio, alpha)) {
                desenharLivro(g, (int) p[0], (int) p[1], s);
            }
        }
        if (novelo > 0) {
            for (double[] p : posicoesNovelo(cx, cy, novelo, alpha)) {
                desenharNovelo(g, (int) p[0], (int) p[1], s);
            }
        }
    }

    /** Folhas do ícone na borda da aura, e quanto elas giram por tick, em radianos. */
    private static final int FOLHAS_DA_AURA = 3;
    private static final double GIRO_DAS_FOLHAS = 0.01;

    /**
     * ARTE EM JOGO = ARTE DO ÍCONE. O novelo e o livro que orbitam o gato são os
     * PNG do registro (Icone), os mesmos do card e do "Your build", em fator
     * inteiro. Eram desenhados aqui à parte — o grimório era um livro vermelho de
     * cruz dourada, e o card mostrava outro —, e o jogador não reconhecia no chão
     * a carta que tinha escolhido. O tamanho segue o de antes (o alcance de dano
     * não muda; ver posicoesNovelo e posicoesGrimorio).
     */
    private static void desenharNovelo(Graphics2D g, int x, int y, int s) {
        int r = (evoluiu(ArmaExtra.NOVELO) ? 6 : 4) * s;   // Novelo Gigante: bolas maiores
        Icone.NOVELO.desenharCentrado(g, x, y, Math.max(1, (int) Math.ceil(2.0 * r / Icone.LADO)));
    }

    private static void desenharLivro(Graphics2D g, int x, int y, int s) {
        Icone.GRIMORIO.desenharCentrado(g, x, y, Math.max(1, Math.round(11f * s / Icone.LADO)));
    }
}
