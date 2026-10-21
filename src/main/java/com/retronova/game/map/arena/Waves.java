package com.retronova.game.map.arena;

import com.retronova.engine.Engine;
import com.retronova.engine.io.Resources;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.game.objects.tiles.Void;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Waves implements Runnable {

    // RITMO DE HORDA, a la Vampire Survivors.
    //
    // Antes cada fatia do JSON nascia de uma vez, e a primeira onda inteira eram
    // cinco zumbis em sessenta segundos: o jogador limpava a tela e ficava
    // esperando. Agora cada fatia vira um fio continuo de pequenos grupos, com mais
    // bichos e mais frageis — morrer rapido e em quantidade e o que da o ritmo.
    // O JSON continua dizendo a PROPORCAO entre ondas; estes numeros dizem o ritmo.

    /**
     * RAMPA POR SALA: 0 na primeira arena, 1 da quinta em diante.
     *
     * A horda nascia no ritmo cheio desde a primeira sala, contra um gato de
     * nivel 1 com a arma inicial — muito rato para um gato fraco. O comeco tem de
     * ser facil: e onde o jogador aprende o laco (matar, subir, escolher) e sente
     * o gato crescer. Densidade, endurecimento, elites e cerco sobem juntos com a
     * rampa, e so na quinta sala chegam ao ritmo de antes.
     */
    private double rampa() {
        return Math.min(1.0, nivel / 4.0);
    }

    /** Multiplica a quantidade do JSON: 3x na primeira sala, 5x da quinta em diante. */
    private double densidade() {
        return 3.0 + 2.0 * rampa();
    }
    /** Em quantos grupos cada fatia do JSON e dividida. */
    private static final int GRUPOS_POR_FATIA = 12;
    /** Vida dos bichos de horda; chefe (fatia de um so) fica intacto. */
    private static final double VIDA_HORDA = 0.5;

    private static final int M;

    private static final JSONObject[] controller;

    static {
        M = 60 * 60;
        controller = new JSONObject[3];
        try {
            controller[0] = Resources.getJsonFile("maps/waves", "easy");
            controller[1] = Resources.getJsonFile("maps/waves", "normal");
            controller[2] = Resources.getJsonFile("maps/waves", "hard");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Arena gameMap;

    private final int period;
    private final int[] amount;
    private final EntityIDs[] types;

    /** Em que sala esta onda acontece. Decide a dureza dos bichos. */
    private final int nivel;

    private int counter;
    private int lastCounter;

    private volatile int step;
    /** Quantos grupos ja nasceram nesta onda. step = grupo / GRUPOS_POR_FATIA. */
    private volatile int grupo;
    private volatile List<Entity> listAppend;
    private boolean end;

    public Waves(Arena gameMap, int waveLevel, int difficult) {
        this.gameMap = gameMap;
        this.nivel = waveLevel;
        this.semFim = difficult == 0;
        // Bancada: -Dcd.minuto=N comeca a corrida no minuto N, para medir a horda cheia.
        this.ticksDaCorrida = Long.getLong("cd.minuto", 0L) * 3600;
        JSONArray listWaves = (JSONArray) controller[difficult].get("waves");
        JSONObject wave = (JSONObject) listWaves.get(waveLevel);
        this.period = ((Number)wave.get("period")).intValue();
        this.amount = new int[this.period];
        JSONArray listAmount = (JSONArray) wave.get("amount");
        for(int i = 0; i < amount.length; i++) {
            int currentAmount = ((Number)listAmount.get(i)).intValue();
            this.amount[i] = currentAmount;
        }
        JSONArray listTypes = (JSONArray) wave.get("enemies");
        this.types = new EntityIDs[listTypes.size()];
        EntityIDs[] types = EntityIDs.values();
        for(int i = 0; i < listTypes.size(); i++) {
            String type = String.valueOf(listTypes.get(i));
            for(int j = 0; j < types.length; j++) {
                if(type.equalsIgnoreCase(types[j].name())) {
                    this.types[i] = types[j];
                }
            }
        }
    }

    public int getSeconds() {
        if (semFim) {
            return (int) (ticksDaCorrida / 60);
        }
        return (int)((this.counter/60d));
    }

    // -------------------------------------------------------- corrida sem fim

    /**
     * A CORRIDA SEM FIM. Na dificuldade normal nao ha mais ondas nem salas: a
     * horda nunca para, e o que sobe e o RELOGIO. Cada minuto traz mais ratos,
     * mais depressa, mais fortes e de tipos novos, chefes a cada cinco minutos e
     * cercos cada vez maiores — a corrida termina quando o gato cai. E o laco do
     * Vampire Survivors: a pergunta deixa de ser "passo desta sala?" e vira
     * "quanto tempo eu aguento?".
     */
    private final boolean semFim;
    private long ticksDaCorrida;
    private int abates;
    private int ateOProximoGrupo = 60 * 2;
    private int proximoChefe = 1;
    /** Duração da corrida: quando o relógio chega aqui, nasce o chefe final. */
    public static final int DURACAO_MINUTOS = 30;
    private boolean reiNasceu;
    /** Teto de bichos vivos: acima disto a horda espera, e o jogo nao afoga. */
    private static final int MAX_VIVOS = 170;

    /** Quem entra na horda, por minuto de corrida em que passa a aparecer. */
    private static final Object[][] ESCALA = {
            {0.0, EntityIDs.Zombie},
            {1.0, EntityIDs.Slime},
            {2.0, EntityIDs.Skeleton},
            {3.0, EntityIDs.MouseSquire},
            {4.5, EntityIDs.MouseVampire},
            {6.0, EntityIDs.RatExplode},
            // MonarkMouse fica de fora: o sprite dele (monarkmouse.png) nao existe e
            // ele derrubava o jogo ao nascer. Volta quando a arte existir.
    };
    private static final EntityIDs[] CHEFES = {EntityIDs.KingCursedCatBoss, EntityIDs.CatToyBoss};

    public boolean semFim() {
        return semFim;
    }

    public int abates() {
        return abates;
    }

    /** Chamado a cada bicho de horda morto. */
    public void contarAbate() {
        abates++;
    }

    private double minutos() {
        return ticksDaCorrida / 3600.0;
    }

    private void tickSemFim() {
        ticksDaCorrida++;
        double min = minutos();
        // Aos 30 minutos, o fim da corrida: o Rato Rei no lugar do chefe comum.
        if (!reiNasceu && min >= DURACAO_MINUTOS) {
            reiNasceu = true;
            nascerGrupo(1, EntityIDs.RatoRei, true);
        }
        // Chefe a cada cinco minutos.
        if (min >= proximoChefe * 5 && min < DURACAO_MINUTOS) {
            int qual = (proximoChefe - 1) % CHEFES.length;
            proximoChefe++;
            nascerGrupo(1, CHEFES[qual], true);
        }
        // Cerco a partir de 1min15, a cada 40s, cada vez maior.
        if (ticksDaCorrida >= 60 * 75 && --ateOCerco <= 0) {
            ateOCerco = 60 * 40;
            cercar();
        }
        if (--ateOProximoGrupo > 0) {
            return;
        }
        // Grupos cada vez mais frequentes (2,2s -> 0,4s) e maiores.
        //
        // A RAMPA ERA INCLINADA DEMAIS NO COMECO. Com 130 - 22*min e 3 + 3*min, a
        // horda nascia a ~2 ratos por segundo no minuto zero, 7 no dois e 12 no
        // tres: o ritmo dobrava a cada minuto enquanto o gato ainda estava no
        // nivel cinco, com uma arma. Agora fica em ~4,5/s no minuto dois, ~6,5/s
        // no tres e ~12/s no cinco, e o intervalo so chega ao piso de 0,4s perto
        // do minuto nove — o fim da corrida continua sendo uma enxurrada, o
        // comeco deixa a build se formar.
        ateOProximoGrupo = (int) Math.max(24, 130 - 12 * min);
        int vivos = 0;
        esqueletosVivos = 0;
        for (Entity e : gameMap.getEntities()) {
            if (e instanceof com.retronova.game.objects.entities.enemies.Enemy b && b.alvoAutomatico()) {
                vivos++;
                if (b instanceof com.retronova.game.objects.entities.enemies.Skeleton) {
                    esqueletosVivos++;
                }
            }
        }
        com.retronova.game.Estatisticas.horda(vivos);
        int tamanho = 3 + (int) (min * 2) + Engine.RAND.nextInt(3);
        tamanho = Math.min(tamanho, MAX_VIVOS - vivos);
        if (tamanho > 0) {
            nascerGrupo(tamanho, null, false);
        }
    }

    /** O nivel de dureza pelo tempo: um degrau a cada 24 segundos. */
    private int durezaDoTempo() {
        return (int) (minutos() * 2.5);
    }

    /** Esqueletos vivos na ultima contagem, mais os que nasceram desde entao. */
    private int esqueletosVivos;

    /**
     * Quantos esqueletos a horda aceita ao mesmo tempo: 4 no minuto dois, um a
     * mais por minuto, ate 12.
     *
     * Ele e o unico que bate de longe, e o sorteio favorece o tipo mais novo —
     * entao no minuto em que era liberado, metade da horda virava esqueleto, e
     * dez atiradores juntos eram uma parede de caveiras. Os outros bichos se
     * resolvem chegando perto; atirador demais nao tem resposta.
     */
    private int tetoDeEsqueletos() {
        return Math.min(12, 2 + (int) minutos());
    }

    private EntityIDs sortearTipo() {
        double min = minutos();
        List<EntityIDs> liberados = new ArrayList<>();
        for (Object[] linha : ESCALA) {
            if (min >= (double) linha[0]) {
                liberados.add((EntityIDs) linha[1]);
            }
        }
        if (esqueletosVivos >= tetoDeEsqueletos() && liberados.size() > 1) {
            liberados.remove(EntityIDs.Skeleton);
        }
        // Os mais recentes pesam mais: a horda muda de cara com o tempo.
        int i = liberados.size() - 1 - (int) (Math.abs(Engine.RAND.nextGaussian()) * 1.5);
        EntityIDs tipo = liberados.get(Math.max(0, i));
        if (tipo == EntityIDs.Skeleton) {
            esqueletosVivos++;
        }
        return tipo;
    }

    /** Um grupo nascendo junto, de uma direcao, logo fora da tela. */
    private void nascerGrupo(int quantos, EntityIDs fixo, boolean chefe) {
        List<Entity> grupo = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            EntityIDs tipo = fixo != null ? fixo : sortearTipo();
            grupo.add(prepararBicho(Entity.build(tipo.ordinal(), 0, 0), chefe));
        }
        gameMap.putAll(spawner(grupo));
    }

    /** Dureza pelo tempo, vida de horda e chance de elite, para quem nasce na corrida sem fim. */
    private Entity prepararBicho(Entity e, boolean chefe) {
        if (e instanceof com.retronova.game.objects.entities.enemies.Enemy bicho) {
            bicho.escalarPara(durezaDoTempo());
            if (!chefe) {
                // A "meia vida" da horda so vale no comeco: some ao longo de cinco
                // minutos. Junto com a dureza pelo tempo, e o que impede o gato de
                // atravessar o meio da corrida com uma build de tres minutos.
                bicho.escalarVida(Math.min(1.0, VIDA_HORDA + 0.1 * minutos()));
                // Elites mais raras: cada uma larga um bau garantido.
                double chanceElite = Math.min(0.06, 0.002 + 0.008 * minutos());
                if (Engine.RAND.nextDouble() < chanceElite) {
                    bicho.tornarElite();
                }
            }
        }
        return e;
    }

    public int currentSpawn() {
        return this.step;
    }

    public int amountSpawns() {
        return this.amount.length;
    }

    public boolean ended() {
        if (semFim) {
            return false;   // a corrida sem fim so acaba com o gato
        }
        return this.end;
    }

    private void periodic(){
        if(end){
            return;
        }
        counter++;
        if(this.gameMap.enemiesEmpty()) {
            counter+=5;
        }
        if(counter > M){
            counter = 0;
            end = true;
            return;
        }
        if(counter > lastCounter){
            lastCounter += Math.max(1, M / (period * GRUPOS_POR_FATIA));
            new Thread(this, "Waves-Thread").start();
        }
    }

    private synchronized List<Entity> listEntity(){
        List<Entity> lista = new ArrayList<>();
        int fatia = Math.min(step, amount.length - 1);
        boolean chefe = amount[fatia] <= 1;
        int quantos;
        if (chefe) {
            // Fatia de um so e chefe: nasce uma vez, no primeiro grupo, inteiro.
            quantos = grupo % GRUPOS_POR_FATIA == 0 ? amount[fatia] : 0;
        } else {
            // O total exato da fatia, repartido entre os grupos. Arredondar cada
            // grupo para cima inflava a conta: 6 bichos previstos viravam 12.
            int total = (int) Math.round(amount[fatia] * densidade());
            int g = grupo % GRUPOS_POR_FATIA;
            quantos = (g + 1) * total / GRUPOS_POR_FATIA - g * total / GRUPOS_POR_FATIA;
        }
        for(int i = 0; i < quantos; i++){
            int id = types[Engine.RAND.nextInt(types.length)].ordinal();
            Entity e = Entity.build(id, 0, 0);
            // A DUREZA VEM DA SALA, e e aplicada AQUI porque este e o unico lugar
            // que cria inimigo de onda. Espalhar a conta pelas dez classes daria
            // dez lugares para alguem esquecer, e o bicho esquecido nasceria em
            // nivel de fabrica na sala vinte sem nada acusando.
            if (e instanceof com.retronova.game.objects.entities.enemies.Enemy bicho) {
                bicho.escalarPara(this.nivel);
                if (!chefe) {
                    bicho.escalarVida(VIDA_HORDA);
                    // Endurecimento dentro da onda: 30% do ritmo na primeira sala.
                    bicho.evoluir((int) (getSeconds() * (0.3 + 0.7 * rampa())));
                    // Elite: quase nenhuma na primeira sala; 3% a ~9% da quinta em diante.
                    double chanceElite = (0.005 + 0.025 * rampa()) + 0.001 * getSeconds() * rampa();
                    if (Engine.RAND.nextDouble() < chanceElite) {
                        bicho.tornarElite();
                    }
                }
            }
            lista.add(e);
        }
        return lista;
    }

    private synchronized List<Entity> spawner(List<Entity> entidades) {
        // NASCER AO REDOR, LOGO FORA DA TELA.
        //
        // Antes: o contador de tentativas era um so para o grupo INTEIRO, contando
        // ate as que deram certo; passadas 200, o resto do grupo era sorteado pelo
        // MAPA TODO — e com hordas grandes isso era quase sempre. Era por isso que
        // ratos brotavam em qualquer canto.
        //
        // Agora o grupo chega JUNTO, de uma direcao so (grupos diferentes, direcoes
        // diferentes: e isso que faz a horda vir de todos os lados), numa elipse
        // logo alem da borda da tela — calculada da janela e do zoom, entao o
        // nascimento acompanha a camera. Ponto em parede procura o chao mais
        // proximo dali; nunca cai no sorteio do mapa.
        List<Entity> list = new ArrayList<>();
        com.retronova.game.objects.entities.Player gato = Game.getPlayer();
        if (gato == null) {
            return list;
        }
        int s = GameObject.SIZE();
        double zoom = Math.max(0.3, com.retronova.engine.Configs.Zoom() / 100.0);
        double rx = Engine.window.getWidth() / zoom / 2.0 / s + 1.5;
        double ry = Engine.window.getHeight() / zoom / 2.0 / s + 1.5;
        double gx = (gato.getX() + gato.getWidth() / 2.0) / s;
        double gy = (gato.getY() + gato.getHeight() / 2.0) / s;
        double direcao = Engine.RAND.nextDouble() * Math.PI * 2;
        for (int i = 0; i < entidades.size(); i++) {
            Point lugar = null;
            for (int tentativa = 0; tentativa < 12 && lugar == null; tentativa++) {
                // Espalha o grupo num leque de ~40 graus e um pouco de profundidade;
                // se o leque inteiro for parede, a direcao muda.
                double ang = direcao + (Engine.RAND.nextDouble() - 0.5) * 0.7
                        + (tentativa >= 6 ? Engine.RAND.nextDouble() * Math.PI * 2 : 0);
                double fundo = 1 + Engine.RAND.nextDouble() * 0.25;
                int x = (int) Math.round(gx + Math.cos(ang) * rx * fundo);
                int y = (int) Math.round(gy + Math.sin(ang) * ry * fundo);
                lugar = chaoPerto(x, y, 3);
            }
            if (lugar == null) {
                continue;       // sem chao nenhum por perto: esse fica para o proximo grupo
            }
            Entity e = entidades.get(i);
            e.setX(lugar.x * s);
            e.setY(lugar.y * s);
            list.add(e);
        }
        return list;
    }

    /**
     * Um tile de chao logo alem da borda da tela, numa direcao sorteada em volta
     * do gato; 'folga' empurra mais para fora (1 = colado na borda). Null se nao
     * achar chao em algumas tentativas.
     */
    public Point lugarForaDaTela(double folga) {
        com.retronova.game.objects.entities.Player gato = Game.getPlayer();
        if (gato == null) {
            return null;
        }
        int s = GameObject.SIZE();
        double zoom = Math.max(0.3, com.retronova.engine.Configs.Zoom() / 100.0);
        double rx = Engine.window.getWidth() / zoom / 2.0 / s + 1.5;
        double ry = Engine.window.getHeight() / zoom / 2.0 / s + 1.5;
        double gx = (gato.getX() + gato.getWidth() / 2.0) / s;
        double gy = (gato.getY() + gato.getHeight() / 2.0) / s;
        for (int tentativa = 0; tentativa < 8; tentativa++) {
            double ang = Engine.RAND.nextDouble() * Math.PI * 2;
            double fundo = folga + Engine.RAND.nextDouble() * 0.25;
            Point p = chaoPerto((int) Math.round(gx + Math.cos(ang) * rx * fundo),
                    (int) Math.round(gy + Math.sin(ang) * ry * fundo), 3);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    /** O tile de chao mais proximo de (x, y), ate 'raio' tiles; null se nao houver. */
    private Point chaoPerto(int x, int y, int raio) {
        for (int r = 0; r <= raio; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) {
                        continue;   // so a casca do quadrado: do mais perto para fora
                    }
                    Tile tile = gameMap.getTile(x + dx, y + dy);
                    if (tile != null && !tile.isSolid() && !(tile instanceof Void)) {
                        return new Point(x + dx, y + dy);
                    }
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------- cerco

    /** De quanto em quanto tempo a horda fecha um anel completo em volta do gato. */
    private static final int INTERVALO_DO_CERCO = 60 * 25;
    /** Primeiro cerco: depois de a onda ter tempo de aquecer. */
    private static final int PRIMEIRO_CERCO = 60 * 20;
    private static final double RAIO_DO_CERCO = 8.5;
    private int ateOCerco = PRIMEIRO_CERCO;

    /**
     * O CERCO: um anel inteiro de ratos surgindo em volta do gato ao mesmo tempo.
     *
     * E o pico de tensao da onda, e o momento que o dano em area existe para
     * resolver — dezenas de alvos equidistantes, todos chegando juntos. Copiado
     * do anel de morcegos do Vampire Survivors: a saida e abrir caminho por um
     * lado, ou esperar a Furia encher e explodir o anel inteiro.
     */
    private void cercar() {
        int fatia = Math.min(step, amount.length - 1);
        // Sem cerco na primeira sala: ali o jogador ainda esta aprendendo a andar.
        if (!semFim && (nivel == 0 || amount[fatia] <= 1) || com.retronova.game.Game.getPlayer() == null) {
            return;     // onda de chefe: o chefe ja e o pico
        }
        com.retronova.game.objects.entities.Player gato = Game.getPlayer();
        int s = GameObject.SIZE();
        // Anel menor no comeco: 10 ratos na segunda sala, 18 da quinta em diante.
        int quantos = semFim
                ? Math.min(30, 8 + (int) (3 * minutos()))
                : 10 + (int) Math.round(8 * rampa());
        // Na corrida sem fim o anel e de um tipo so, o mais fraco liberado: e um
        // teste de area, nao de resistencia.
        EntityIDs tipoDoAnel = semFim ? EntityIDs.Zombie : types[0];
        double gx = gato.getX() / s, gy = gato.getY() / s;
        List<Entity> anel = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            double ang = i * Math.PI * 2 / quantos;
            int x = (int) Math.round(gx + Math.cos(ang) * RAIO_DO_CERCO);
            int y = (int) Math.round(gy + Math.sin(ang) * RAIO_DO_CERCO);
            Tile tile = gameMap.getTile(x, y);
            if (tile == null || tile.isSolid() || tile instanceof Void) {
                continue;
            }
            Entity e = Entity.build(tipoDoAnel.ordinal(), 0, 0);
            if (semFim) {
                prepararBicho(e, false);
            } else if (e instanceof com.retronova.game.objects.entities.enemies.Enemy bicho) {
                bicho.escalarPara(this.nivel);
                bicho.escalarVida(VIDA_HORDA);
                bicho.evoluir((int) (getSeconds() * (0.3 + 0.7 * rampa())));
            }
            e.setX(x * s);
            e.setY(y * s);
            anel.add(e);
        }
        if (!anel.isEmpty()) {
            Game.getMap().putAll(anel);
            Game.getMap().put(new com.retronova.game.objects.particles.Word("SURROUNDED!",
                    gato.getX() + gato.getWidth() / 2.0, gato.getY() - s, 2));
            com.retronova.engine.sound.Sound.play(com.retronova.engine.sound.Sounds.Zombie);
        }
    }

    public void tick() {
        if (semFim) {
            tickSemFim();
            return;
        }
        if (!end && --ateOCerco <= 0) {
            ateOCerco = INTERVALO_DO_CERCO;
            cercar();
        }
        periodic();
        // Grupo vazio (o resto de uma fatia de chefe) tambem conta como grupo:
        // senao a contagem empacava e a onda nunca passava para a fatia seguinte.
        if(listAppend != null) {
            Game.getMap().putAll(listAppend);
            listAppend = null;
            synchronized (this) {
                grupo++;
                step = Math.min(grupo / GRUPOS_POR_FATIA, amount.length - 1);
            }
        }
    }

    @Override
    public void run() {
        try {
            List<Entity> enemies = listEntity();
            listAppend = spawner(enemies);
        }catch (Exception e) {
            System.err.println("Erro em waves...");
            e.printStackTrace();
        }
    }
}
