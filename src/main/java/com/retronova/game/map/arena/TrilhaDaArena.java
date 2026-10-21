package com.retronova.game.map.arena;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.enemies.Enemy;

/**
 * A música da arena durante a briga: QUAL faixa toca e A QUE VOLUME.
 *
 * Antes a trilha tinha dois estados, combate ou chefe, trocados por corte seco, e
 * o volume era o mesmo do primeiro ao trigésimo minuto. Aqui ela acompanha a
 * corrida sem ninguém notar o mecanismo:
 *
 *  - INTENSIDADE. Com pouca horda a faixa toca um pouco abaixo (72%) e sobe até
 *    o volume cheio conforme a tela enche. A pressão sonora segue a pressão na
 *    tela, e o começo da corrida respira.
 *  - CALMARIA ANTES DO CHEFE. Nos últimos 20 s antes de um chefe nascer a faixa
 *    se recolhe até 35%. Ninguém avisa o jogador; ele só sente que algo mudou, e
 *    quando o chefe entra a trilha dele vem cheia, por contraste.
 *  - CRUZAMENTO. Chefe entrando ou caindo, ou a trilha trocada nas opções, cruza
 *    as faixas em um segundo em vez de parar uma e começar a outra.
 *
 * Usa só as faixas que já existem. Vive fora do Arena para o Arena só perguntar
 * "e a música?" a cada tick, sem carregar a regra.
 */
final class TrilhaDaArena {

    /** Ticks do cruzamento entre duas faixas. */
    private static final int CRUZAMENTO = 60;
    /** Segundos antes do chefe em que a calmaria começa. */
    static final int CALMARIA = 20;
    /** Volume mínimo, como fração, no fundo da calmaria. */
    static final double FUNDO_DA_CALMARIA = 0.35;
    /** Volume com a tela vazia; sobe até 1 com a horda cheia. */
    static final double VOLUME_CALMO = 0.72;
    /** Quantos bichos vivos contam como "horda cheia". */
    static final int HORDA_CHEIA = 80;

    private Musics atual;
    private Musics saindo;
    private double volumeSaindo;
    /** Ticks do cruzamento em curso; -1 quando não há. */
    private int cruzando = -1;
    /** Fração aplicada agora à faixa atual. */
    private double volume = -1;
    /** O último volume mandado ao som, para não reaplicar a cada tick. */
    private double aplicado = -1;

    void tick() {
        boolean chefe = false;
        int vivos = 0;
        for (Enemy e : Game.getMap().getEntities(Enemy.class)) {
            if (e.chefe()) {
                chefe = true;
            } else if (e.alvoAutomatico()) {
                vivos++;
            }
        }
        Musics desejada = chefe ? Musics.FightBoss : Musics.combate();
        int ateOChefe = Game.getWave() == null ? Integer.MAX_VALUE : Game.getWave().segundosAteOProximoChefe();
        tocar(desejada, volumeAlvo(vivos / (double) HORDA_CHEIA, ateOChefe, chefe));
    }

    /**
     * O volume que a faixa deve ter, como fração da preferência do jogador.
     *
     * @param intensidade 0 com a tela vazia, 1 ou mais com a horda cheia
     * @param ateOChefe   segundos até o próximo chefe ({@code Integer.MAX_VALUE} se nenhum)
     * @param chefe       há chefe na arena agora? Então é a trilha dele, sempre cheia.
     */
    static double volumeAlvo(double intensidade, int ateOChefe, boolean chefe) {
        if (chefe) {
            return 1.0;
        }
        double i = Math.max(0, Math.min(1, intensidade));
        double v = VOLUME_CALMO + (1 - VOLUME_CALMO) * i;
        if (ateOChefe < CALMARIA) {
            double t = Math.max(0, ateOChefe) / (double) CALMARIA;     // 1 longe, 0 na hora
            v *= FUNDO_DA_CALMARIA + (1 - FUNDO_DA_CALMARIA) * t;
        }
        return v;
    }

    private void tocar(Musics desejada, double alvo) {
        if (atual == null) {
            // Primeira vez: o Arena já subiu a faixa no construtor; só a adota.
            atual = desejada;
            if (!Sound.playing(atual)) {
                Sound.play(atual, true);
            }
            volume = alvo;
            aplicar(atual, volume);
            return;
        }
        if (desejada != atual && cruzando < 0) {
            if (Sound.playing(desejada)) {
                // Já está no ar (a prévia das opções virou a música do jogo):
                // adota sem cruzar, senão ela afundaria e voltaria do zero.
                pararMenos(desejada);
                atual = desejada;
                return;
            }
            saindo = atual;
            volumeSaindo = volume;
            atual = desejada;
            Sound.play(atual, true);
            Sound.volume(atual, 0);
            volume = 0;
            aplicado = 0;
            cruzando = 0;
        }
        if (cruzando >= 0) {
            cruzando++;
            double t = cruzando / (double) CRUZAMENTO;
            Sound.volume(saindo, volumeSaindo * (1 - t));
            volume = alvo * t;
            aplicar(atual, volume);
            if (cruzando >= CRUZAMENTO) {
                pararMenos(atual);
                saindo = null;
                cruzando = -1;
            }
            return;
        }
        // Sem troca: o volume escorrega até o alvo em uns dois segundos.
        volume += (alvo - volume) * 0.02;
        aplicar(atual, volume);
        // Se a faixa parou por fora (troca de sala, outra tela), sobe de novo.
        if (!Sound.playing(atual)) {
            pararMenos(atual);
            Sound.play(atual, true);
            aplicado = -1;
            aplicar(atual, volume);
        }
    }

    /** Manda o volume ao som só quando ele mudou de fato: mexer no mixer a cada tick é à toa. */
    private void aplicar(Musics faixa, double fracao) {
        if (Math.abs(fracao - aplicado) > 0.004) {
            Sound.volume(faixa, fracao);
            aplicado = fracao;
        }
    }

    /** Para todas as faixas de briga menos a que deve continuar. */
    private static void pararMenos(Musics fica) {
        for (Musics m : Musics.COMBATE) {
            if (m != fica) {
                Sound.stop(m);
            }
        }
        if (fica != Musics.FightBoss) {
            Sound.stop(Musics.FightBoss);
        }
    }
}
