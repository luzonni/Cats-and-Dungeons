package com.retronova.game.objects.entities;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Estatisticas;
import com.retronova.game.Game;
import com.retronova.game.items.Passiva;
import com.retronova.game.map.Camera;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.particles.OndaDeChoque;

import java.awt.Color;
import java.util.List;

/**
 * As habilidades exclusivas dos gatos que reagem a algo: a onda do Muffin quando
 * ele apanha, a explosão das bolas do Azrael quando acertam.
 *
 * O Volley do Finn não mora aqui porque ele só muda QUANTAS flechas saem, e isso
 * se resolve inteiro no arco. Estas duas precisam de estado (a recarga) ou de
 * procurar inimigos em volta, e misturar isso no Player, que já passa de mil
 * linhas, ou na varinha, que é de todo mundo, espalharia a regra do gato por
 * arquivos que não são dele.
 *
 * Os números vivem no {@link Bonus}, junto com as outras contas de habilidade.
 */
public final class Habilidades {

    private final Player gato;
    /** Ticks até a próxima onda poder sair. */
    private int recargaDaOnda;

    Habilidades(Player gato) {
        this.gato = gato;
    }

    void tick() {
        if (recargaDaOnda > 0) {
            recargaDaOnda--;
        }
    }

    // ------------------------------------------------------------ Retaliacao

    /**
     * O gato acabou de levar um golpe que doeu (não foi esquivado). Se ele tem a
     * Retaliação e ela não está recarregando, solta a onda.
     */
    void aoLevarDano() {
        int nivel = gato.getMelhorias().nivel(Passiva.RETALIACAO);
        if (nivel <= 0 || recargaDaOnda > 0) {
            return;
        }
        recargaDaOnda = Bonus.recargaDaRetaliacao(nivel);

        double cx = gato.getX() + gato.getWidth() / 2d;
        double cy = gato.getY() + gato.getHeight() / 2d;
        double raio = Bonus.raioDaRetaliacao(nivel) * Bonus.area(gato) * GameObject.SIZE();
        double dano = Bonus.danoDaRetaliacao(nivel, gato.getDamage());
        double empurrao = Bonus.empurraoDaRetaliacao(nivel);

        Estatisticas.fonte(Passiva.RETALIACAO.nome());
        for (Enemy e : emVolta(cx, cy, raio, null)) {
            e.strike(AttackTypes.Impact, dano);
            e.getPhysical().addForce("knockback", empurrao, e.getAngle(gato));
        }
        Estatisticas.fonte(null);

        Game.getMap().put(new OndaDeChoque(cx, cy, raio, Passiva.RETALIACAO.cor(), 0.7f));
        Camera.tremer(1.5);
        Sound.play(Sounds.Woosh);
    }

    /** Os inimigos vivos e já nascidos cujo centro está dentro do raio, menos {@code fora}. */
    private static List<Enemy> emVolta(double cx, double cy, double raio, Entity fora) {
        List<Enemy> perto = new java.util.ArrayList<>();
        for (Enemy e : Game.getMap().getEntities(Enemy.class)) {
            if (e == fora || e.nascendo() || e.getLife() <= 0) {
                continue;
            }
            double ex = e.getX() + e.getWidth() / 2d, ey = e.getY() + e.getHeight() / 2d;
            // Conta a meia largura do bicho: um grandão com o corpo dentro do anel
            // tem de levar, mesmo com o centro um pouco fora.
            if (Math.hypot(ex - cx, ey - cy) <= raio + e.getWidth() / 2d) {
                perto.add(e);
            }
        }
        return perto;
    }
}
