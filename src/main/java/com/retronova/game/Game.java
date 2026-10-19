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

    /**
     * Em qual espaco de save esta partida grava.
     *
     * ELE E ESCOLHIDO UMA VEZ E NAO MUDA MAIS. E o que impede que os tres espacos
     * virem tres pontos da MESMA corrida: se a partida pudesse trocar de espaco no
     * meio, o jogador guardaria uma copia antes de uma sala perigosa e recarregaria
     * ao morrer, e a permadeath acabaria. Sem escolha na hora de gravar, nao ha
     * como ramificar. Ver Corrida.
     */
    private final int espaco;

    private long seconds;
    private int count;

    private final Player player;
    private Camera gCam;

    private GameMap map;
    private int level;
    private int difficult;

    private final HUD hud;
    private final Inter inter;

    /**
     * A corrida acabou de ser retomada e precisa reescrever o proprio save.
     *
     * A leitura APAGA o arquivo — e o que impede retomar duas vezes o mesmo ponto.
     * Mas isso abre uma janela: entre retomar e trocar de sala, nao existe save
     * nenhum no disco, e quem fechasse o jogo nesse intervalo perderia a corrida
     * que tinha acabado de abrir. Regravar no primeiro tick fecha a janela.
     *
     * No primeiro tick, e nao no construtor, porque a gravacao pergunta qual e a
     * partida corrente — e durante o construtor a resposta ainda e outra.
     */
    private boolean regravarSave;

    /**
     * Retoma a corrida gravada, montando a arena DIRETO.
     *
     * A primeira versao comecava no saguao e trocava de sala no primeiro tick,
     * porque o construtor da Arena perguntava o nivel a {@code Game.getGame()} e
     * ali este objeto ainda nao estava na pilha. O efeito era o jogador aparecer no
     * saguao e ser teleportado um instante depois — parecia defeito, e era. Com a
     * Arena aceitando o nivel de fora, a sala certa e a PRIMEIRA que se ve.
     */
    public static Game retomar(Corrida corrida, int espaco) {
        return new Game(corrida, espaco);
    }

    private Game(Corrida corrida, int espaco) {
        this.espaco = espaco;
        this.indexPlayer = corrida.gato();
        this.inter = new Inter();
        Player player = Player.newPlayer(this.indexPlayer);
        this.inter.put("inventory", player.getInventory(), false);
        this.inter.put("status", new Status(player), false);
        this.player = player;
        // O ESTADO ANTES DO MAPA. A arena e escolhida pelo nivel, e o gato precisa
        // estar reconstituido antes de ser posto nela — o changeMap ja o posiciona.
        this.level = corrida.nivel();
        this.difficult = corrida.dificuldade();
        this.seconds = corrida.segundos();
        corrida.aplicarNoGato(player);
        this.changeMap(new Arena(this.difficult, this.level));
        this.hud = new HUD(player);
        this.regravarSave = true;
    }

    public int getIndexPlayer() {
        return this.indexPlayer;
    }

    public int getEspaco() {
        return this.espaco;
    }

    //Teste
    public Game(int indexPlayer, GameMap map, int espaco) {
        // COMECAR UMA PARTIDA LIMPA O ESPACO ESCOLHIDO — e so ele.
        //
        // Sem isto o espaco mentiria: quem comecasse uma corrida por cima de outra e
        // saisse ainda no saguao veria, na tela de saves, a corrida ANTIGA — uma que
        // ele acabara de mandar apagar. Limpar aqui cobre todos os inicios de uma
        // vez, porque todos passam por este construtor: a selecao de personagem, o
        // "Try Again" e o reiniciar da pausa.
        //
        // OS OUTROS DOIS ESPACOS NAO SAO TOCADOS. Comecar uma corrida nova nao pode
        // custar as corridas dos outros espacos — e o motivo de existirem tres.
        Corrida.apagar(espaco);
        this.espaco = espaco;
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
        // MORREU, NAO HA O QUE RETOMAR. Sem isto o save do inicio da sala ficaria
        // no disco depois da morte, e o botao de continuar do menu ofereceria
        // desfazer a derrota — que e precisamente o que a permadeath proibe.
        Corrida.apagar(this.espaco);
        // LEVA O PERSONAGEM JUNTO. A partida e descartada aqui, entao quem quiser
        // tentar de novo depois nao tem mais de onde descobrir com qual gato o
        // jogador estava — e sem isso o botao de recomecar nao tem o que recomecar.
        int gato = this.indexPlayer;
        int onde = this.espaco;
        Engine.backActivity();
        Engine.heapActivity(new GameOver(gato, onde));
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

        // O PONTO DE RETOMADA E AQUI, ao ENTRAR na sala.
        //
        // Este metodo e o funil por onde toda troca de sala passa — alcapao, porta,
        // saguao — entao um unico ponto cobre todos, inclusive as passagens que
        // ainda nao existem. E gravar na ENTRADA, e nao na saida, e o que faz o
        // arquivo descrever sempre uma sala intacta: uma sala pela metade nao pode
        // ser reconstruida, ja que os bichos e os projeteis nao sao gravados.
        //
        // Fica depois de a camera estar montada porque a gravacao le o estado do
        // jogador, e o jogador so esta no lugar certo depois do addPlayer acima.
        if (ehAPartidaCorrente()) {
            Corrida.gravar();
        }
    }

    /**
     * Esta partida e a que esta rodando?
     *
     * A GRAVACAO PRECISA SABER DE QUEM E O ESTADO QUE ELA ESTA FOTOGRAFANDO.
     * {@code changeMap} tambem roda dentro do construtor, e ali este objeto ainda
     * nao esta na pilha de activities — a corrente e outra. Sem a checagem,
     * reiniciar uma partida podia gravar um ponto de retomada da partida
     * ABANDONADA, e quem saisse antes de entrar na primeira arena voltaria, pelo
     * "Continue", para a corrida que tinha acabado de jogar fora.
     *
     * Hoje o {@code restart} escapa disso por acidente, porque desempilha antes de
     * construir. Depender do acidente e o problema: a checagem aqui vale para
     * qualquer ordem de pilha que alguem venha a escrever.
     */
    private boolean ehAPartidaCorrente() {
        return Engine.getACTIVITY() == this;
    }

    @Override
    public void tick() {
        if (acabou) {
            return;
        }
        if (regravarSave) {
            regravarSave = false;
            Corrida.gravar();
        }
        count++;
        if(count > 60) {
            count = 0;
            seconds++;
        }
        guardarPosicoes();
        quadroDoGato++;
        if (depoisDeUmSegundo != null && quadroDoGato > 90 && !Engine.pausado()) {
            Runnable r = depoisDeUmSegundo;
            depoisDeUmSegundo = null;
            r.run();
            return;
        }
        hud.tick();
        if(KeyBoard.KeyPressed("ESCAPE")) {
            Engine.pause(new Pause());
        }
        if(KeyBoard.KeyPressed("E")) {
            inter.open();
        }
        // Nivel novo: abre a escolha de atributo. Um por vez — com varios niveis
        // pendentes, ela reabre a cada tick ate gastar todos.
        if (player.niveisPendentes() > 0 && !player.morrendo() && !Engine.pausado()) {
            Engine.pause(new com.retronova.game.map.arena.SubirDeNivel());
            return;
        }
        map.tick();
        if (!player.morrendo()) {
            com.retronova.game.objects.entities.Arsenal.tick(player);
        }
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
        long t0 = System.nanoTime();
        hud.render(g);
        com.retronova.engine.Desempenho.secao("hud", System.nanoTime() - t0);
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
        long t = System.nanoTime();
        renderMap(cam);
        t = medir("mapa", t);
        List<Entity> entidades = map.getEntities();
        com.retronova.game.map.Atmosfera.renderSombras(cam, entidades, alphaDoMundo());
        t = medir("sombras", t);
        renderEntities(cam);
        t = medir("entidades", t);
        renderParticles(cam);
        t = medir("particulas", t);
        cam.dispose();
        com.retronova.game.map.Atmosfera.renderLuz(g, entidades, alphaDoMundo());
        medir("luz", t);
    }

    /** Registra quanto o trecho levou e devolve o instante atual. */
    private static long medir(String trecho, long desde) {
        long agora = System.nanoTime();
        com.retronova.engine.Desempenho.secao(trecho, agora - desde);
        return agora;
    }

    private void renderMap(Graphics2D g) {
        Tile[] map = this.map.getMap();
        // SO O QUE ESTA NA TELA. O teste era contra gCam.getBounds(), que e o
        // retangulo do MAPA INTEIRO — todo tile passava, e a arena desenhava os
        // mil e tantos tiles dela a cada quadro, a maioria fora da janela. Agora a
        // janela e levada de volta para coordenadas do mundo, com dois tiles de
        // folga para paredes cujo desenho sobe alem do proprio tile.
        //
        // E SEM PERCORRER O MAPA TODO: com o campo aberto (160x160) testar os 25 mil
        // tiles um a um ja custava. A faixa visivel de colunas e linhas sai direto
        // do retangulo, e so ela e visitada.
        Rectangle visivel = areaVisivel(g, 2);
        int tile = com.retronova.game.objects.GameObject.SIZE();
        int largura = this.map.larguraEmTiles();
        int altura = largura == 0 ? 0 : map.length / largura;
        int x0 = Math.max(0, visivel.x / tile), x1 = Math.min(largura - 1, (visivel.x + visivel.width) / tile);
        int y0 = Math.max(0, visivel.y / tile), y1 = Math.min(altura - 1, (visivel.y + visivel.height) / tile);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = x + y * largura;
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

    /**
     * A janela levada de volta para coordenadas do mundo, com 'folgaEmTiles' de
     * margem. Na duvida (transformacao sem inversa), o mapa inteiro.
     */
    private Rectangle areaVisivel(Graphics2D g, int folgaEmTiles) {
        try {
            Rectangle r = g.getTransform().createInverse().createTransformedShape(
                    new Rectangle(0, 0, Engine.window.getWidth(), Engine.window.getHeight())).getBounds();
            int folga = folgaEmTiles * com.retronova.game.objects.GameObject.SIZE();
            r.grow(folga, folga);
            return r;
        } catch (java.awt.geom.NoninvertibleTransformException semZoom) {
            return gCam.getBounds();
        }
    }

    private void renderEntities(Graphics2D g) {
        List<Entity> entities = map.getEntities();
        // So o que esta na tela (com folga de dois tiles para sprites altos e
        // auras). Com a horda grande, boa parte dela esta fora da janela, e cada
        // um desses ainda passava por sombra, animacao, sprite e barra de vida.
        Rectangle visivel = areaVisivel(g, 2);
        for(int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (entity != player && !entity.getBounds().intersects(visivel)) {
                continue;
            }
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
            // Desenha na posicao interpolada: translada o pincel em vez de mexer
            // no x/y, para tudo que a entidade desenha (arma na mao, barra de
            // vida, aura) andar junto com ela.
            double[] d = entity.deslocamentoNoQuadro(alphaDoMundo());
            g.translate(d[0], d[1]);
            if (entity instanceof Enemy elite) {
                elite.renderAura(g);
            }
            if (entity instanceof com.retronova.game.objects.entities.utilities.Projetil tiro) {
                tiro.renderRastro(g);
            }
            if (entity instanceof Enemy bicho) {
                java.awt.geom.AffineTransform antes = g.getTransform();
                animarBicho(g, bicho);
                entity.render(g);
                g.setTransform(antes);
            } else if (entity == player && !player.morrendo()) {
                java.awt.geom.AffineTransform antes = g.getTransform();
                animarGato(g, player);
                entity.render(g);
                g.setTransform(antes);
            } else {
                entity.render(g);
            }
            if (entity == player && !player.morrendo()) {
                com.retronova.game.objects.entities.Arsenal.render(g, player);
            }
            if(entity instanceof Enemy enemy) {
                enemy.renderLife(g);
            }
            if(Debugging.showEntityHitBox) {
                entity.renderBounds(g);
            }
            g.translate(-d[0], -d[1]);
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
            double[] d = p.deslocamentoNoQuadro(alphaDoMundo());
            g.translate(d[0], d[1]);
            p.render(g);
            if(Debugging.showParticleHitBox) {
                p.renderBounds(g);
            }
            g.translate(-d[0], -d[1]);
        }
    }

    /**
     * Vida no desenho dos bichos, sem precisar de quadro novo de arte.
     *
     * A folha dos ratos tem dois ou tres quadros, e eles andavam deslizando pelo
     * chao como recortes de papel. Aqui o corpo ganha peso: quica a cada passo,
     * achata e estica (squash & stretch), balanca de um lado para o outro,
     * respira parado, e da um tranco quando apanha. Tudo em volta dos PES, para
     * o bicho nao sair do lugar em que a sombra esta.
     *
     * A fase vem da identidade de cada bicho, para a horda nao marchar em
     * uniao — cem ratos pulando no mesmo compasso parecem uma coreografia.
     */
    private static void animarBicho(Graphics2D g, Enemy bicho) {
        int s = com.retronova.engine.Configs.GameScale();
        double t = System.nanoTime() / 1e9;
        double fase = (System.identityHashCode(bicho) % 1000) / 1000.0 * Math.PI * 2;
        // Quanto ele esta andando, de 0 a 1.
        double andando = Engine.pausado() ? 0 : Math.min(1, bicho.passoNoTick() / (0.6 * s));

        double passo = t * 11 + fase;
        double quique = Math.abs(Math.sin(passo)) * 2.2 * s * andando;
        double estica = 1 + 0.08 * Math.sin(passo * 2) * andando
                + 0.035 * Math.sin(t * 3 + fase) * (1 - andando);   // respirar parado
        double inclina = 0.10 * Math.sin(passo) * andando;

        // O tranco do golpe: alarga e achata, voltando em dez ticks.
        float impacto = bicho.impacto();
        double sx = (1 / estica) * (1 + 0.28 * impacto);
        double sy = estica * (1 - 0.18 * impacto);

        double pesX = bicho.getX() + bicho.getWidth() / 2.0;
        double pesY = bicho.getY() + bicho.getHeight();
        g.translate(pesX, pesY - quique);
        g.rotate(inclina);
        g.scale(sx, sy);
        g.translate(-pesX, -pesY);
    }

    /** Gancho do modo de bancada (Engine, -Dcd.bench): roda uma vez, no tick 90. */
    public static Runnable depoisDeUmSegundo;

    /** Lado para o qual o gato olhava, e quando ele virou — para animar a virada. */
    private int ladoAnterior = 0;
    private long viradaEm = -100;
    private long quadroDoGato;

    /**
     * O gato ganha peso sem mexer na passada desenhada na folha.
     *
     * A folha ja tem o balanco da caminhada (cabeca desce no contato, sobe na
     * passada), e deslizar o sprite inteiro desfaria isso — o rabo e a pata
     * apoiada sairiam do chao. Entao aqui so entra o que a folha NAO tem: a
     * inclinacao para o lado em que ele corre, um achata-estica leve ancorado nos
     * pes, e a VIRADA — antes o espelho trocava num quadro so, e o gato "piscava"
     * de lado; agora ele se aperta e abre, como quem gira o corpo.
     */
    private void animarGato(Graphics2D g, Player gato) {
        int s = com.retronova.engine.Configs.GameScale();
        double t = System.nanoTime() / 1e9;
        boolean parado = Engine.pausado();
        double dx = -gato.deslocamentoNoQuadro(0f)[0];
        double andando = parado ? 0 : Math.min(1, gato.passoNoTick() / (0.8 * s));

        int lado = gato.getLadoDoPasso();
        if (lado != ladoAnterior) {
            if (ladoAnterior != 0) {
                viradaEm = quadroDoGato;
            }
            ladoAnterior = lado;
        }
        double virada = Math.min(1, (quadroDoGato - viradaEm + Engine.alpha()) / 7.0);
        double aperto = 0.35 + 0.65 * (1 - Math.pow(1 - virada, 3));   // ease-out

        double estica = 1 + 0.045 * Math.sin(t * 15) * andando
                + 0.02 * Math.sin(t * 2.6) * (1 - andando);             // respirar parado
        double inclina = parado ? 0 : Math.max(-1, Math.min(1, dx / (2.0 * s))) * 0.07;

        double pesX = gato.getX() + gato.getWidth() / 2.0;
        double pesY = gato.getY() + gato.getHeight();
        g.translate(pesX, pesY);
        g.rotate(inclina);
        g.scale(aperto / estica, estica);
        g.translate(-pesX, -pesY);
    }

    /**
     * Fracao do tick para interpolar o mundo. Com uma tela por cima (pausa,
     * cartas, roleta) o mundo esta parado: usar a fracao do relogio faria tudo
     * tremer entre a posicao anterior e a atual.
     */
    private static float alphaDoMundo() {
        return Engine.pausado() ? 1f : Engine.alpha();
    }

    /** Guarda a posicao de todos antes do tick, para o render interpolar. */
    private void guardarPosicoes() {
        List<Entity> entities = map.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).guardarPosicaoAnterior();
        }
        List<Particle> particles = map.getParticles();
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).guardarPosicaoAnterior();
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
        // MESMO ESPACO. Reiniciar troca a corrida, nao o lugar onde ela mora.
        Engine.heapActivity(new Game(game.indexPlayer, map, game.espaco), () -> { });
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
