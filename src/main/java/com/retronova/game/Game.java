package com.retronova.game;

import com.retronova.engine.Activity;
import com.retronova.engine.Debugging;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.exceptions.NotInActivity;
import com.retronova.engine.exceptions.NotInMap;
import com.retronova.game.hud.HUD;
import com.retronova.game.interfaces.Inter;
import com.retronova.game.interfaces.shared.Status;
import com.retronova.game.map.*;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.arena.Waves;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.furniture.Furniture;
import com.retronova.game.objects.entities.utilities.Utility;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.menus.GameOver;
import com.retronova.menus.Pause;

import java.awt.*;
import java.util.List;

public class Game implements Activity {

    /**
     * Cor por tras do mundo.
     *
     * Antes havia um campo de estrelas correndo no fundo, que e leitura de espaco
     * e nao de masmorra. Agora o que aparece alem das bordas do mapa e rocha nao
     * iluminada, no mesmo tom do preenchimento que cerca a sala: o jogador nunca
     * ve o vazio preto, so pedra.
     */
    private static final Color ROCHA = new Color(0x0e1218);

    private final int indexPlayer;

    private long seconds;
    private int count;

    private final Player player;
    private Camera gCam;

    private GameMap map;
    private int level;
    private int difficult;

    private final HUD hud;
    private final Inter inter;

    //Teste
    public Game(int indexPlayer, GameMap map) {
        this.indexPlayer = indexPlayer;
        this.inter = new Inter();
        Player player = Player.newPlayer(indexPlayer);
        this.inter.put("inventory", player.getInventory(), false);
        this.inter.put("status", new Status(player), false);
        this.player = player;
        this.changeMap(map);
        this.hud = new HUD(player);
    }

    public static Camera getCam() {
        return getGame().gCam;
    }

    public static void focus(GameObject obj) {
        getGame().gCam.setFollowed(obj);
    }

    public int getLevel() {
        return this.level;
    }

    public void plusLevel() {
        this.level++;
    }

    public int getDifficult() {
        return this.difficult;
    }

    public void setDifficult(int difficult) {
        this.difficult = difficult;
    }

    /** Quadros decorridos dentro do segundo atual. Serve para espacar efeitos. */
    public int getCount() {
        return this.count;
    }

    public long getSeconds() {
        return this.seconds;
    }

    /**
     * A partida acabou; nada mais do jogo deve rodar.
     *
     * Sem esta trava a partida seguia recebendo ticks ate a troca de tela
     * acontecer de fato, e um desses ticks passava pelo ajuste de trilha da arena.
     * Ele pergunta "a faixa de combate esta tocando?", ouve que nao — porque a tela
     * de fim acabou de calar tudo — e SOBE A MUSICA DE BATALHA DE NOVO, agora por
     * cima do game over. Era so as vezes porque depende de quantos ticks ainda
     * cabem antes da troca.
     */
    private boolean acabou;

    private void gameOver() {
        this.acabou = true;
        // LEVA O PERSONAGEM JUNTO. A partida e descartada aqui, entao quem quiser
        // tentar de novo depois nao tem mais de onde descobrir com qual gato o
        // jogador estava — e sem isso o botao de recomecar nao tem o que recomecar.
        int gato = this.indexPlayer;
        Engine.backActivity();
        Engine.heapActivity(new GameOver(gato));
    }

    public void changeMap(GameMap newMap) {
        if(newMap == null) {
            return;
        }
        if(this.map != null) {
            this.map.remove(player);
            this.map.dispose();
        }
        // NENHUM EFEITO ATRAVESSA A TROCA DE SALA.
        //
        // O som da passagem abrindo continuava tocando dentro da fase seguinte,
        // porque um efeito nao sabe que a sala mudou — ele so sabe que ainda tem
        // audio pela frente. Cortar aqui vale para qualquer som, e nao so para
        // aquele: qualquer efeito longo disparado no fim de um turno teria o mesmo
        // problema, e o proximo a aparecer ja nasce resolvido.
        Sound.stopAllSounds();
        this.map = newMap;
        this.map.addPlayer(player);
        this.gCam = new Camera(this.map.getBounds(), 0.25d);
        this.gCam.setX((int)player.getX() + player.getWidth()/2 - Engine.window.getWidth()/2);
        this.gCam.setY((int)player.getY() + player.getHeight()/2 - Engine.window.getHeight()/2);
        this.gCam.setFollowed(player);
    }

    @Override
    public void tick() {
        if (acabou) {
            return;
        }
        count++;
        if(count > 60) {
            count = 0;
            seconds++;
        }
        hud.tick();
        if(KeyBoard.KeyPressed("ESCAPE")) {
            Engine.pause(new Pause());
        }
        if(KeyBoard.KeyPressed("E")) {
            inter.open();
        }
        map.tick();
        map.depth();
        List<Entity> entities = map.getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            // OS BICHOS IGNORAM QUEM JA MORREU.
            //
            // Enquanto a queda roda, nenhum inimigo age: ninguem persegue, ninguem
            // ataca. E o padrao dos jogos do genero — a morte e um momento que
            // pertence ao jogador, e mob batendo em cadaver rouba a cena dele.
            //
            // A TRAVA FICA AQUI, no unico laco que roda todos, e nao dentro de cada
            // bicho. Sao dez classes de inimigo, cada uma com a propria perseguicao;
            // uma lista dessas envelhece no primeiro bicho novo que alguem escrever
            // sem lembrar da regra.
            if (entity instanceof Enemy bicho) {
                // O bicho conta o proprio nascimento mesmo sem agir.
                bicho.tickNascimento();
                if (player.morrendo() || bicho.nascendo()) {
                    continue;
                }
                // A COLEIRA. Longe demais do gato, o inimigo simplesmente para de
                // agir — nao persegue, nao atira. E o suficiente para que sair da
                // area dele seja uma jogada: dentro da arena nada muda, porque a
                // coleira e maior que a tela.
                //
                // A trava fica aqui pela mesma razao da morte: sao dez classes de
                // inimigo, cada uma com a propria perseguicao, e uma regra copiada
                // dez vezes deixa de valer no primeiro bicho novo.
                if (bicho.getDistance(player)
                        > Enemy.COLEIRA_EM_TILES * com.retronova.game.objects.GameObject.SIZE()) {
                    continue;
                }
            }
            entity.preTick();
            entity.tick();
            entity.postTick();
            if(entity instanceof Utility || entity instanceof Furniture)
                continue;
            Tile tile = map.getTile((int) entity.getX() + entity.getWidth() / 2, (int) entity.getY() + entity.getHeight());
            tile.effect(entity);
        }
        List<Particle> particles = map.getParticles();
        for(int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            p.tick();
            p.setDepth();
        }
        for (Tile tile : this.map.getMap()) {
            tile.tick();
        }
        if(!this.map.getEntities().contains(player)) {
            gameOver();
        }
        synchronized (this.map.getRepulsion()) {
            this.map.getRepulsion().notify();
        }
        gCam.tick();

        if(Debugging.running) {
            int entitiesSize = Game.getMap().getEntities().size();
            Debugging.setInfo("Amount of entities", String.valueOf(entitiesSize));
            int particlesSize = Game.getMap().getParticles().size();
            Debugging.setInfo("Amount of particles", String.valueOf(particlesSize));
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(ROCHA);
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
        renderWorld(g);
        hud.render(g);
        apagarNaMorte(g);
    }

    /**
     * A luz se fechando enquanto o gato cai.
     *
     * POR CIMA DE TUDO, INCLUSIVE DO HUD. A barra de vida e a hotbar sao a
     * interface de quem esta jogando, e durante a queda ja nao ha o que jogar —
     * deixa-las acesas sobre uma arena escura seria manter em cena a unica parte da
     * imagem que ainda promete controle. Escurecendo tambem elas, a tela inteira se
     * despede junto.
     *
     * FICA NO GAME, e nao no Player, porque o veu cobre a JANELA e nao o mundo: o
     * desenho do gato acontece dentro da transformacao da camera, que esta com zoom
     * e deslocada, e um retangulo pintado ali cobriria um pedaco da arena em vez da
     * tela. Aqui fora, a conta e o tamanho da janela e mais nada.
     */
    private void apagarNaMorte(Graphics2D g) {
        float escuro = player.escuridao();
        if (escuro <= 0f) {
            return;
        }
        g.setColor(new java.awt.Color(0, 0, 0, (int) (255 * escuro)));
        g.fillRect(0, 0, Engine.window.getWidth(), Engine.window.getHeight());
    }

    private void renderWorld(Graphics2D g) {
        Graphics2D cam = (Graphics2D) g.create();
        cam.setTransform(Camera.getAt());
        renderMap(cam);
        renderEntities(cam);
        renderParticles(cam);
        cam.dispose();
    }

    private void renderMap(Graphics2D g) {
        Tile[] map = this.map.getMap();
        for(int i = 0; i < map.length; i++) {
            Tile tile = map[i];
            if(tile.getBounds().intersects(gCam.getBounds())) {
                map[i].render(g);
                if(Debugging.showTileBox) {
                    map[i].renderBounds(i, g);
                }
            }
        }
    }

    /** A folha do portal, carregada uma vez. */
    private studio.retrozoni.sheeter.SpriteSheet portal;

    private java.awt.image.BufferedImage portalDosBichos() {
        if (portal == null) {
            portal = new studio.retrozoni.sheeter.SpriteSheet(
                    "sprites/objects/furniture", new String[]{"portalCistern"});
        }
        // Roda os quadros junto com o relogio do jogo, para os varios portais de uma
        // onda nao piscarem todos no mesmo compasso de um sprite parado.
        if (count % 6 == 0) {
            portal.plusIndex();
        }
        return portal.getSprite();
    }

    private void renderEntities(Graphics2D g) {
        List<Entity> entities = map.getEntities();
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            // O PORTAL DOS BICHOS, desenhado ANTES do inimigo para ficar por baixo
            // dele. Mesmo desenho da chegada do gato: os dois chegam do mesmo jeito,
            // entao chegam com a mesma imagem — duas animacoes diferentes para o
            // mesmo acontecimento fariam o jogador procurar uma diferenca que nao
            // existe. Fica aqui, no unico laco que desenha todos, e nao nas dez
            // classes de inimigo.
            if (entity instanceof com.retronova.game.objects.entities.Nascente nascente
                    && nascente.nascendo()) {
                com.retronova.game.map.arena.Chegada.desenhar(g, portalDosBichos(),
                        (int) entity.getX() + entity.getWidth() / 2,
                        (int) entity.getY() + entity.getHeight() / 2,
                        nascente.aberturaDoPortal());
            }
            if (entity instanceof com.retronova.game.objects.entities.Nascente chegando
                    && !chegando.corpoVisivel()) {
                continue;                 // so o portal, ainda: o corpo vem depois
            }
            entity.render(g);
            if(entity instanceof Enemy enemy) {
                enemy.renderLife(g);
            }
            if(Debugging.showEntityHitBox) {
                entity.renderBounds(g);
            }
        }
        // A linha que a arma usou para decidir se atirava: verde quer dizer "ha
        // caminho", vermelha quer dizer "nao ha".
        //
        // FORA DO LACO e DENTRO DA CONDICAO. Estava nos dois lugares errados: era
        // redesenhada uma vez por entidade, e aparecia mesmo com as hitboxes
        // desligadas nas opcoes — ferramenta de depuracao vazando para quem so
        // queria jogar.
        if(Debugging.showEntityHitBox) {
            com.retronova.game.items.Item.renderLinhaDeTiro(g);
        }
    }

    private void renderParticles(Graphics2D g) {
        List<Particle> particles = map.getParticles();
        for(int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            p.render(g);
            if(Debugging.showParticleHitBox) {
                p.renderBounds(g);
            }
        }
    }

    @Override
    public void dispose() {
        System.out.println("Dispose Game");
        map.dispose();
        inter.dispose();
    }

    public static void restart() {
        Game.getInter().dispose();
        Game game = getGame();
        GameMap map = new Room("beginning");
        Engine.backActivity();
        // Passa pela tela de transicao, como o inicio de partida: reiniciar e
        // comecar uma corrida nova, e o corte seco fazia parecer um bug.
        Engine.heapActivity(new Game(game.indexPlayer, map), () -> { });
    }

    public static Game getGame() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game;
        }
        throw new NotInActivity("Não é possível retornar o mapa pois a activity atual não é o jogo!");
    }

    public static GameMap getMap() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.map;
        }
        throw new NotInActivity("Não é possível retornar o mapa pois a activity atual não é o jogo!");
    }

    public static Player getPlayer() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.player;
        }
        throw new NotInActivity("Não é possível retornar o player pois a activity atual não é o jogo!");
    }

    public static Waves getWave(){
        if(getMap() instanceof Arena arena) {
            return arena.getWaves();
        }
        throw new NotInMap("O mapa atual não é uma Arena!");
    }

    public static Arena getArena() {
        if(getMap() instanceof Arena arena) {
            return arena;
        }
        throw new NotInMap("O mapa atual não é uma room!");
    }

    public static Room getRoom() {
        if(getMap() instanceof Room room) {
            return room;
        }
        throw new NotInMap("O mapa atual não é uma room!");
    }

    public static Inter getInter() {
        if(Engine.getACTIVITY() instanceof Game game) {
            return game.inter;
        }
        throw new NotInActivity("Não é possível retornar a UI pois a activity atual não é um jogo");
    }

}
