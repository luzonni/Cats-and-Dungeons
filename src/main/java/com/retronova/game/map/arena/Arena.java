package com.retronova.game.map.arena;

import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.util.List;

public class Arena extends GameMap {

    private static final String[] maps = {"easy", "normal", "hard"};
    private final Waves waves;
    private boolean ended;

    public Arena(int difficult) {
        super(maps[difficult]);
        this.ended = false;
        this.waves = new Waves(this, Game.getGame().getLevel(), difficult);
        Sound.stop(Musics.Room);
        // keepPlaying, e nao play: de uma arena para a proxima a trilha de
        // combate segue de onde estava, em vez de recomecar a cada vitoria.
        Sound.keepPlaying(Musics.Fight, true);
    }

    public Waves getWaves() {
        return waves;
    }

    /**
     * Limpou a arena, abre o alcapao para a PROXIMA ARENA.
     *
     * Antes ele levava a uma sala de compras entre uma arena e outra. A sala saiu
     * e o alcapao passou a encadear as arenas direto, com o nivel subindo a cada
     * uma: e o laco de um roguelike, onde a corrida so anda para a frente e fica
     * mais dificil, em vez de parar num intervalo seguro a cada vitoria.
     *
     * Consequencia assumida: por enquanto nao ha onde comprar nem se curar no
     * meio da corrida. So a antecamara tem loja, e dela nao se volta.
     */
    @Override
    public void tick() {
        if(waves.ended() && !ended && enemiesEmpty()) {
            int x = (getBounds().width / GameObject.SIZE()) / 2;
            int y = (getBounds().height / GameObject.SIZE()) / 2;
            Entity trapdoor = Entity.build(EntityIDs.TrapDoor.ordinal(), x, y, "NEXT ARENA");
            put(trapdoor);
            ended = true;
        }else {
            waves.tick();
        }
    }

    boolean enemiesEmpty() {
        List<Enemy> entities = Game.getMap().getEntities(Enemy.class);
        return entities.isEmpty();
    }

    /**
     * Nao para a trilha de combate.
     *
     * Quem entra e que decide o que toca: a Room silencia o combate ao carregar,
     * e a derrota passa por GameOver, que corta tudo. Parar aqui deixava a
     * proxima arena muda, porque o dispose da anterior roda DEPOIS do construtor
     * da nova e desligava a musica que ela tinha acabado de subir.
     */
    @Override
    public void dispose() {
    }
}
