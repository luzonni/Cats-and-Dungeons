package com.retronova.game.map.arena;

import com.retronova.engine.Engine;
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

    /**
     * As arenas de cada dificuldade.
     *
     * ANTES ERA UM MAPA SO POR DIFICULDADE, e o encadeamento reconstruia sempre o
     * mesmo: o quinto turno era identico ao primeiro, e a corrida nao tinha para
     * onde ir visualmente. Agora a facil e um LOTE de seis plantas da cisterna, com
     * a saida em paredes diferentes e a agua em niveis diferentes.
     *
     * O tamanho do lote saiu de como o genero se organiza. Hades tem quinze camaras
     * no Tartaro e dez em Asfodelo; Slay the Spire, quinze a dezessete andares por
     * ato, com a corrida entre quarenta e cinco minutos e uma hora; Isaac fica em
     * vinte e cinco minutos a uma hora. O padrao e tres a cinco regioes por corrida
     * e dez a quinze salas por regiao. Aqui cada sala e uma arena inteira de ondas —
     * mais longa que uma camara de Hades — entao seis por tema da um bloco de turnos
     * sem repeticao imediata, e ainda e um numero que da para cuidar.
     *
     * Normal e dificil continuam com um mapa cada: elas ainda nao tem tema, e
     * inventar variedade sem ambientacao seria seis vezes a mesma caixa.
     */
    private static final String[][] MAPAS = {
            {"cistern_1", "cistern_2", "cistern_3", "cistern_4", "cistern_5", "cistern_6"},
            {"normal"},
            {"hard"},
    };

    /**
     * Qual planta este turno usa.
     *
     * Percorre o lote em ordem a partir do nivel, e nao por sorteio: sorteio repete,
     * e cair duas vezes seguidas na mesma sala e justamente o que se quer evitar.
     * Passado o lote inteiro, ele recomeca — a essa altura o jogador ja andou seis
     * arenas e o retorno nao le como falta de conteudo.
     */
    private static String planta(int dificuldade, int nivel) {
        String[] lote = MAPAS[dificuldade];
        return lote[Math.floorMod(nivel, lote.length)];
    }
    private final Waves waves;
    private boolean ended;

    /** A trilha que a arena subiu. Guardada para saber quando trocar. */
    private Musics trilha;

    public Arena(int difficult) {
        super(planta(difficult, Game.getGame().getLevel()));
        this.ended = false;
        this.waves = new Waves(this, Game.getGame().getLevel(), difficult);
        Sound.stop(Musics.Room);
        // DEVOLVE O VOLUME DAS FAIXAS DE ACAO.
        //
        // A arena anterior terminou baixando-as ate zero na passagem para a calma, e
        // volume e estado da faixa, nao da sala: sem isto a arena seguinte manda
        // tocar a musica de combate e ela toca MUDA, porque continua no volume em
        // que a cena passada a deixou.
        for (Musics m : Musics.COMBATE) {
            Sound.volume(m, 1);
        }
        Sound.volume(Musics.FightBoss, 1);
        // keepPlaying, e nao play: de uma arena para a proxima a trilha de
        // combate segue de onde estava, em vez de recomecar a cada vitoria.
        Sound.keepPlaying(Musics.combate(), true);
        this.trilha = Musics.combate();
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
    /**
     * A trilha de chefe entra quando um chefe entra, e sai quando ele morre.
     *
     * A troca e feita aqui, e nao na hora de invocar o bicho, porque chefe pode
     * aparecer em qualquer onda e por qualquer caminho — e porque ele tambem
     * pode morrer no meio da arena, e ai a briga continua sem chefe nenhum.
     * So mexe no som quando a faixa desejada MUDA: chamar play todo tick
     * rebobinaria a musica sessenta vezes por segundo.
     */
    private void ajustarTrilha() {
        // NAO DECIDE NADA COM O SOM CALADO. Sem foco as faixas ficam pausadas, e
        // pausada responde "nao estou tocando" — o que fazia esta funcao reiniciar
        // a musica do zero a cada F11.
        if (Sound.silenciado()) {
            return;
        }
        // ACABOU A BRIGA, ACABA A MUSICA DE BRIGA.
        //
        // Nao entre ondas — entre ondas o combate continua e cortar a trilha ali
        // faria o intervalo parecer o fim. So quando TODAS as ondas terminaram e a
        // sala esta vazia: dali ate a proxima porta o jogador escolhe carta, compra
        // e caminha, e nada disso e briga. A faixa de acao continuando por cima
        // dessas tres coisas e o que fazia o turno nao ter fim, so pausa.
        //
        // A proxima arena sobe o combate de novo no proprio construtor, entao o
        // ciclo fecha sozinho: briga, silencio, briga.
        if (ended) {
            passagemParaACalma();
            return;
        }
        Musics desejada = temChefe() ? Musics.FightBoss : Musics.combate();
        // A pergunta e "a faixa certa ESTA TOCANDO?", e nao "e a mesma que eu
        // subi?". Comparando com o campo lembrado, trocar de musica pelo menu
        // fazia a arena parar e retocar uma faixa que ja estava no ar — o segundo
        // recomeco que se ouvia ao voltar para o jogo.
        if (Sound.playing(desejada)) {
            trilha = desejada;
            return;
        }
        Musics.pararCombate();
        Sound.play(desejada, true);
        this.trilha = desejada;
    }

    /**
     * Quadros da passagem de uma trilha para a outra.
     *
     * Um segundo e meio. Curto demais e corte com enfeite; longo demais e as duas
     * faixas se sobrepoem tempo suficiente para virar barulho — sao musicas
     * diferentes em andamentos diferentes, e elas nao combinam tocando juntas.
     */
    private static final int PASSAGEM = 90;

    private int passagem;

    /**
     * A briga acabou: a acao SAI e a calma ENTRA, ao mesmo tempo.
     *
     * Cortar de uma para a outra dizia "acabou" no mesmo instante em que o ultimo
     * bicho caia, e o alivio nao tem instante — ele assenta. Com as duas se
     * cruzando, o silencio chega antes de o jogador perceber que chegou, que e como
     * ele funciona fora do jogo tambem.
     *
     * A calma comeca MUDA e so entao sobe: subir do zero e o que faz dela uma
     * chegada. Se ela entrasse no volume cheio, o cruzamento seria dois cortes.
     */
    private void passagemParaACalma() {
        Musics calma = Musics.calma();
        if (passagem == 0) {
            Sound.volume(calma, 0);
            Sound.play(calma, true);
        }
        if (passagem >= PASSAGEM) {
            // Terminou: a acao pode parar de vez, sem ninguem ouvir o corte.
            Musics.pararCombate();
            return;
        }
        passagem++;
        float t = passagem / (float) PASSAGEM;
        for (Musics m : Musics.COMBATE) {
            Sound.volume(m, 1 - t);
        }
        Sound.volume(Musics.FightBoss, 1 - t);
        Sound.volume(calma, t);
    }

    private boolean temChefe() {
        for (Enemy e : Game.getMap().getEntities(Enemy.class)) {
            if (e.chefe()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A camera vai ate a alavanca e fecha nela.
     *
     * O ALVO E A ALAVANCA, e nao a passagem. A passagem e um corredor escuro que
     * so muda quando alguem age; a alavanca e a coisa em que se clica. Apontar para
     * o que o jogador tem de FAZER, e nao para onde ele vai acabar indo, e o que
     * transforma o movimento de camera em instrucao em vez de passeio.
     *
     * Ela nao teleporta: o proprio seguimento da camera ja e suave, entao trocar de
     * alvo produz uma varredura pela sala — e essa varredura mostra de quebra o
     * campo limpo, que e a recompensa do turno.
     */
    /**
     * Um tile livre ao lado da alavanca, para o vendedor.
     *
     * Tenta os quatro vizinhos e fica no primeiro que nao for parede. Sem o teste,
     * um deslocamento fixo poe o vendedor dentro da alvenaria nas plantas em que a
     * alavanca esta encostada nela — que sao todas, porque alavanca e coisa de
     * parede.
     */
    /** Onde fica a passagem, em tiles. O corredor corre no eixo dela. */
    private java.awt.Point bocaDaPassagem() {
        int tile = GameObject.SIZE();
        for (com.retronova.game.objects.entities.furniture.Gate porta
                : getEntities(com.retronova.game.objects.entities.furniture.Gate.class)) {
            return new java.awt.Point((int) Math.round(porta.getX() / tile),
                    (int) Math.round(porta.getY() / tile));
        }
        return null;
    }

    private java.awt.Point juntoDaAlavanca() {
        int tile = GameObject.SIZE();
        for (com.retronova.game.objects.entities.furniture.Lever alavanca
                : getEntities(com.retronova.game.objects.entities.furniture.Lever.class)) {
            int lx = (int) Math.round(alavanca.getX() / tile);
            int ly = (int) Math.round(alavanca.getY() / tile);
            java.awt.Point saida = bocaDaPassagem();
            int[][] vizinhos = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-2, -1}, {2, -1}};
            for (int[] d : vizinhos) {
                int x = lx + d[0];
                int y = ly + d[1];
                if (getTile(x, y).isSolid()) {
                    continue;
                }
                // NUNCA NO CAMINHO DA PASSAGEM.
                //
                // O vendedor apareceu plantado dentro do vao da porta. Alem de
                // feio, e um problema de jogo: ele bloqueia a unica saida da sala,
                // e a coisa que o jogador vai fazer em seguida e passar por ali.
                // O corredor corre no eixo da porta, entao basta recusar qualquer
                // tile que compartilhe linha ou coluna com ela.
                if (saida != null && (x == saida.x || y == saida.y)) {
                    continue;
                }
                return new java.awt.Point(x, y);
            }
            return new java.awt.Point(lx, ly);
        }
        int meio = (getBounds().width / tile) / 2;
        return new java.awt.Point(meio, (getBounds().height / tile) - 5);
    }

    private void mostrarSaida() {
        for (com.retronova.game.objects.entities.furniture.Lever alavanca
                : getEntities(com.retronova.game.objects.entities.furniture.Lever.class)) {
            Game.getCam().velocidadeDeCena(CAMERA_DE_CENA);
            Game.getCam().setFollowed(alavanca);
            Game.getCam().aproximar(1.4f);
            this.olhandoASaida = OLHANDO_A_SAIDA;
            return;
        }
    }

    private void tickCamera() {
        if (olhandoASaida <= 0) {
            return;
        }
        olhandoASaida--;
        if (olhandoASaida == 0) {
            // A VOLTA TAMBEM E LENTA. So depois de chegar no gato a camera recupera
            // a velocidade de jogo — devolver antes faria o retorno ser o corte que
            // a ida deixou de ser.
            Game.getCam().setFollowed(Game.getPlayer());
            Game.getCam().velocidadeDeCena(CAMERA_DE_CENA);
            this.voltando = OLHANDO_A_SAIDA / 2;
        }
        if (voltando > 0) {
            voltando--;
            if (voltando == 0) {
                Game.getCam().velocidadeDeCena(0);
            }
        }
    }

    /**
     * A chegada do gato. Enquanto ela roda, a arena nao solta onda nenhuma.
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
        if (chegando()) {
            chegada.tick();
            tickCamera();
            ajustarTrilha();
            return;
        }
        tickCamera();
        ajustarTrilha();
        if(waves.ended() && !ended && enemiesEmpty()) {
            ended = true;
            abrirRecompensa();
        }else if (!ended) {
            waves.tick();
        }
    }

    /**
     * Fim de turno: escolher uma carta, e so depois seguir.
     *
     * A ORDEM AQUI E O DESENHO DO TURNO. Antes limpar a arena abria o alcapao no
     * mesmo quadro, e a unica coisa entre uma sala e a proxima era andar ate o
     * buraco. Agora o turno fecha em tres tempos:
     *
     *   ESCOLHER — tres cartas, uma leva. Obrigatorio, e por isso vem primeiro:
     *   enquanto a escolha nao acontece nao ha para onde ir, e o jogador nao passa
     *   batido por ela como passaria por um bau no chao.
     *
     *   COMPRAR — o vendedor aparece ao lado, com o dinheiro que o turno rendeu.
     *   Ele e OPCIONAL de proposito: a carta e o que a corrida da, a loja e o que o
     *   jogador decide comprar com o que juntou. Duas decisoes de naturezas
     *   diferentes, e por isso separadas.
     *
     *   SEGUIR — o alcapao. Ele so nasce depois da carta escolhida, senao daria
     *   para pular a recompensa correndo para o buraco.
     */
    private void abrirRecompensa() {
        Engine.pause(new Recompensa(this::liberarSaida));
    }

    /**
     * Quadros que a camera passa olhando a saida.
     *
     * A sala tem trinta e quatro tiles e a saida pode estar em qualquer parede: sem
     * isto, o jogador termina a briga e fica girando no lugar procurando para onde
     * ir. Dois segundos e meio e o bastante para o olho achar a alavanca e entender
     * que ela e o proximo passo — e curto o suficiente para nao virar cinema.
     */
    private static final int OLHANDO_A_SAIDA = 240;

    /**
     * Velocidade da camera durante a cena.
     *
     * Um vinte avos da distancia por quadro, contra um quarto do normal — cinco
     * vezes mais lenta. Com a velocidade de jogo a camera cobre a sala em dez
     * quadros e o efeito e um corte: o jogador ve a alavanca aparecer, nao a camera
     * ir ate ela. Devagar, o caminho ate la e a propria informacao — ele mostra o
     * campo limpo e onde fica a saida, que e tudo o que a cena tem para dizer.
     */
    private static final double CAMERA_DE_CENA = 0.05;

    private int olhandoASaida;
    private int voltando;

    private void liberarSaida() {
        this.saidaLiberada = true;
        // O VENDEDOR ENTRA ANTES DA CAMERA SAIR. Ele nasce agora, com o portal
        // dele, e a camera chega la a tempo de pegar o meio da chegada: a cena
        // passa a ter duas coisas para mostrar — onde e a saida e quem esta ao lado
        // dela — em vez de so apontar para uma alavanca parada.
        mostrarSaida();
        int meioX = (getBounds().width / GameObject.SIZE()) / 2;
        int meioY = (getBounds().height / GameObject.SIZE()) / 2;

        // O vendedor fica ACIMA do alcapao, e nao em cima dele: encostados, abrir a
        // loja e cair para a proxima arena seriam a mesma tecla no mesmo lugar.
        // O VENDEDOR FICA AO LADO DA PORTA, EM TERRA SECA. Duas condicoes, e as
        // duas importam: junto da saida ele e a ultima parada antes de seguir, e
        // fora da agua porque um vendedor de pe no reservatorio nao tem como ser
        // lido como alguem que montou banca ali.
        // AO LADO DA ALAVANCA, ONDE QUER QUE ELA ESTEJA.
        //
        // A posicao estava cravada no rodape do mapa, e por isso o vendedor caia
        // sempre embaixo — nas arenas em que a saida fica ao norte, a leste ou a
        // oeste, ele nascia do outro lado da sala, longe de tudo. A alavanca e a
        // referencia certa porque ela ja acompanha a saida: perguntando a ela, o
        // vendedor acerta nas seis plantas sem ninguem manter uma tabela.
        java.awt.Point junto = juntoDaAlavanca();
        Entity vendedor = Entity.build(EntityIDs.Seller.ordinal(),
                junto.x, junto.y, "FULL", "CHEGANDO");
        if (vendedor != null) {
            put(vendedor);
        }
        // O ALCAPAO SAIU. Quem leva adiante agora e a porta da parede leste, que ja
        // nasce com a sala e fica fechada ate a alavanca ser puxada. Um buraco no
        // meio do chao nao pertence a lugar nenhum — aparecia do nada onde antes
        // havia laje, e nao dava para ambientar em volta dele. A porta pertence a
        // parede, a ponte leva ate ela e a alavanca e o gesto de sair.
    }

    /**
     * O turno acabou: ondas limpas E carta escolhida.
     *
     * Sao as duas condicoes, e nao so a primeira: a carta e obrigatoria, entao
     * liberar a saida antes dela daria como escolher nao escolher.
     */
    public boolean turnoEncerrado() {
        return this.saidaLiberada;
    }

    private boolean saidaLiberada;

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
