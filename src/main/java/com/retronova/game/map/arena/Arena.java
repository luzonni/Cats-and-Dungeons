package com.retronova.game.map.arena;

import com.retronova.engine.Engine;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.enemies.Enemy;

/**
 * O campo da corrida sem fim: gerado na hora (ver Gerador), com a horda do
 * Waves, a chegada do gato pelo portal, a musica que acompanha a briga, a
 * escolha de elemento aos 45 s e os quebraveis que aparecem em volta do gato.
 *
 * Ja houve arenas desenhadas a mao, uma por sala, encadeadas por uma porta que
 * abria ao fim das ondas. Esse modo saiu do codigo junto com as ondas: a corrida
 * nao tem mais fim de sala, so o relogio.
 */
public class Arena extends GameMap {

    private final Waves waves;

    public Arena() {
        super(Gerador.cisterna(Engine.RAND.nextLong()));
        this.waves = new Waves(this);
        Sound.stop(Musics.Room);
        // DEVOLVE O VOLUME DAS FAIXAS DE ACAO.
        //
        // Volume e estado da faixa, nao da corrida: uma faixa baixada por uma cena
        // anterior (o fim de uma corrida, a calmaria antes de um chefe) continuaria
        // naquele volume, e a corrida nova mandaria tocar uma musica muda.
        for (Musics m : Musics.COMBATE) {
            Sound.volume(m, 1);
        }
        Sound.volume(Musics.FightBoss, 1);
        Sound.keepPlaying(Musics.combate(), true);
    }

    public Waves getWaves() {
        return waves;
    }

    /**
     * Qual faixa toca e a que volume: intensidade da horda, calmaria antes do
     * chefe e cruzamento nas trocas. Ver TrilhaDaArena.
     */
    private void ajustarTrilha() {
        // NAO DECIDE NADA COM O SOM CALADO. Sem foco as faixas ficam pausadas, e
        // pausada responde "nao estou tocando" — o que fazia a trilha reiniciar
        // do zero a cada F11.
        if (Sound.silenciado()) {
            return;
        }
        direcaoMusical.tick();
    }

    private final TrilhaDaArena direcaoMusical = new TrilhaDaArena();

    /**
     * A chegada do gato. Enquanto ela roda, a arena nao solta inimigo nenhum.
     *
     * A ORDEM E O PEDIDO: portal com o jogador PRIMEIRO, bichos depois. Os dois ao
     * mesmo tempo competem pela mesma atencao — o clarao do portal e o branco dos
     * inimigos nascendo dizem a mesma coisa com a mesma cor, e juntos nao dizem
     * nada. Em sequencia, cada um tem a sua vez.
     */
    private final Chegada chegada = new Chegada();

    public boolean chegando() {
        return !chegada.acabou();
    }

    /** O quanto o gato ja se formou no portal, para quem o desenha. */
    public float formacaoDoGato() {
        return chegada.formacao();
    }

    public void renderChegada(java.awt.Graphics2D g) {
        chegada.render(g, Game.getPlayer());
    }

    @Override
    public void tick() {
        ajustarTrilha();
        if (chegando()) {
            chegada.tick();
            return;
        }
        manterQuebraveis();
        oferecerElemento();
        waves.tick();
        gravarACadaMinuto();
    }

    /** O ultimo minuto do relogio em que a corrida foi gravada. */
    private int minutoGravado = -1;

    /**
     * Um ponto de retomada por minuto de corrida.
     *
     * Os outros pontos (escolhas, saida pela pausa) cobrem quem sai pelo menu. Este
     * cobre quem nao sai: o jogo fechado a forca, a janela fechada no X, a luz que
     * cai. Sem ele, qualquer um desses custaria tudo o que veio depois da ultima
     * carta — e numa corrida de trinta minutos isso pode ser muito.
     */
    private void gravarACadaMinuto() {
        int minuto = waves.getSeconds() / 60;
        if (minutoGravado < 0) {
            minutoGravado = minuto;     // o da entrada ja foi gravado no changeMap
            return;
        }
        if (minuto != minutoGravado) {
            minutoGravado = minuto;
            com.retronova.game.Corrida.gravar();
        }
    }

    // ------------------------------------------------- escolha do elemento

    /**
     * O ELEMENTO E OFERECIDO AOS 45 SEGUNDOS, e nao antes de a corrida comecar:
     * cedo o bastante para definir a corrida, tarde o bastante para o jogador ja
     * ter sentido como o gato dele bate — o alcance da espada, o ritmo do arco.
     * Escolhendo antes de jogar, ele estaria apostando em palavras.
     *
     * O teste e "o gato ainda nao tem elemento", e nao um sinal guardado: uma
     * corrida retomada ja escolheu, e o elemento veio gravado junto.
     */
    private void oferecerElemento() {
        if (waves.getSeconds() < 45 || Engine.pausado()
                || Game.getPlayer().elemento() != com.retronova.game.items.Elemento.NENHUM) {
            return;
        }
        // Grava DEPOIS da escolha: retomar antes dela ofereceria tudo de novo, e o
        // save viraria uma maquina de re-sortear.
        Engine.pause(new EscolhaDeElemento(com.retronova.game.Corrida::gravar));
    }

    // ------------------------------------------------------ quebraveis

    /** Quantos quebraveis manter perto do gato, e o que conta como "perto". */
    private static final int QUEBRAVEIS_PERTO = 7;
    private static final double RAIO_PERTO_EM_TILES = 22;
    /** Longe disto, o quebravel e recolhido: o gato ja passou por ali. */
    private static final double RAIO_DE_RECOLHER_EM_TILES = 45;
    private int ateRepor;

    /**
     * Mantem caixotes e barris aparecendo em volta do gato enquanto ele anda.
     *
     * No campo aberto nao da para espalhar tudo de uma vez: ou seriam centenas
     * pelo mapa, ou o gato andaria minutos sem achar nenhum. Entao a arena
     * garante um punhado sempre por perto, nascendo FORA da tela — o jogador
     * nunca ve um aparecer, so encontra — e recolhe os que ficaram muito para
     * tras. Andar passa a render alguma coisa em qualquer direcao.
     */
    private void manterQuebraveis() {
        if (--ateRepor > 0 || Game.getPlayer() == null) {
            return;
        }
        ateRepor = 90;
        com.retronova.game.objects.entities.Player gato = Game.getPlayer();
        int tile = GameObject.SIZE();
        int perto = 0;
        for (Enemy e : getEntities(Enemy.class)) {
            if (e.alvoAutomatico()) {
                continue;
            }
            double d = e.getDistance(gato) / tile;
            if (d > RAIO_DE_RECOLHER_EM_TILES) {
                e.disappear();
            } else if (d <= RAIO_PERTO_EM_TILES) {
                perto++;
            }
        }
        if (perto < QUEBRAVEIS_PERTO) {
            java.awt.Point p = waves.lugarForaDaTela(1.2);
            if (p != null) {
                put(new com.retronova.game.objects.entities.enemies.Quebravel(
                        p.x * (double) tile, p.y * (double) tile, Engine.RAND.nextDouble() < 0.35));
            }
        }
    }

    /**
     * Traz de volta um bicho que ficou para tras: ele reaparece logo fora da tela,
     * em volta do gato. E o que o Vampire Survivors faz — no campo aberto o
     * jogador pode correr para longe, e a horda nao pode ficar perdida la atras.
     */
    public void trazerDePerto(Enemy bicho) {
        java.awt.Point p = waves.lugarForaDaTela(1.05);
        if (p != null) {
            int tile = GameObject.SIZE();
            bicho.setX(p.x * (double) tile);
            bicho.setY(p.y * (double) tile);
            bicho.getPhysical().parar();
        }
    }

    /**
     * Nao para a trilha de combate.
     *
     * Quem entra e que decide o que toca: a Room silencia o combate ao carregar,
     * e a derrota passa por GameOver, que corta tudo. Parar aqui deixava a tela
     * seguinte muda, porque o dispose do mapa antigo roda DEPOIS do construtor do
     * novo e desligava a musica que ele tinha acabado de subir.
     */
    @Override
    public void dispose() {
    }
}
