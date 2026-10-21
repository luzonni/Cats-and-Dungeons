package com.retronova.game.map.arena;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.game.objects.tiles.Void;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * A CORRIDA SEM FIM. Nao ha ondas nem salas: a horda nunca para, e o que sobe e o
 * RELOGIO. Cada minuto traz mais ratos, mais depressa, mais fortes e de tipos
 * novos, chefes a cada cinco minutos e cercos cada vez maiores, ate o Rato Rei aos
 * trinta. E o laco do Vampire Survivors: a pergunta deixa de ser "passo desta
 * sala?" e vira "quanto tempo eu aguento?".
 *
 * O nome ficou do tempo em que o jogo era dividido em ondas, lidas de JSON, cada
 * uma numa sala. Aquele modo saiu inteiro do codigo quando a corrida sem fim
 * passou a ser o unico caminho do jogo.
 */
public class Waves {

    /** Vida dos bichos de horda no minuto zero; chefe fica intacto. */
    private static final double VIDA_HORDA = 0.5;

    private final Arena gameMap;

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
    };
    private static final EntityIDs[] CHEFES = {EntityIDs.KingCursedCatBoss, EntityIDs.CatToyBoss};

    public Waves(Arena gameMap) {
        this.gameMap = gameMap;
        // Bancada: -Dcd.minuto=N comeca a corrida no minuto N, para medir a horda cheia.
        this.ticksDaCorrida = Long.getLong("cd.minuto", 0L) * 3600;
    }

    public int getSeconds() {
        return (int) (ticksDaCorrida / 60);
    }

    // ------------------------------------------------------------ retomada

    /**
     * O que o relogio da corrida precisa para continuar de onde parou.
     *
     * SEM ISTO A RETOMADA RECOMECAVA A CORRIDA. O gato voltava com a build do
     * minuto vinte num relogio zerado: horda de minuto zero, chefe do minuto cinco
     * de novo, e os trinta minutos inteiros pela frente outra vez. Os bichos vivos
     * nao entram — a horda se refaz sozinha em segundos a partir do relogio —, mas
     * o que o relogio JA DEU tem de ir junto: os chefes que ja nasceram nao nascem
     * de novo, e o anel do cerco segue no compasso em que estava.
     */
    public record Estado(long ticks, int abates, int proximoChefe, boolean reiNasceu, int ateOCerco) {
    }

    public Estado estado() {
        return new Estado(ticksDaCorrida, abates, proximoChefe, reiNasceu, ateOCerco);
    }

    public void repor(Estado e) {
        this.ticksDaCorrida = Math.max(0, e.ticks());
        this.abates = Math.max(0, e.abates());
        this.proximoChefe = Math.max(1, e.proximoChefe());
        this.reiNasceu = e.reiNasceu();
        this.ateOCerco = e.ateOCerco() > 0 ? e.ateOCerco() : PRIMEIRO_CERCO;
    }

    // ------------------------------------------------------------ chefes

    /**
     * Quantos segundos faltam para o próximo chefe nascer.
     *
     * É o que deixa a música se recolher antes da hora (ver TrilhaDaArena).
     * {@link Integer#MAX_VALUE} quando não há chefe por vir: depois de o Rato Rei
     * já ter nascido.
     */
    public int segundosAteOProximoChefe() {
        return segundosAteOProximoChefe(minutos(), proximoChefe, reiNasceu);
    }

    /** A mesma conta, pura, para teste: chefe gato a cada 5 min antes dos 30, o rei aos 30. */
    static int segundosAteOProximoChefe(double minutos, int proximoChefe, boolean reiNasceu) {
        double proximo = reiNasceu ? Double.MAX_VALUE : DURACAO_MINUTOS;
        if (proximoChefe * 5 < DURACAO_MINUTOS) {
            proximo = Math.min(proximo, proximoChefe * 5);
        }
        if (proximo == Double.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(0, Math.ceil((proximo - minutos) * 60));
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

    public void tick() {
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
        // Cerco a partir de 1min35, a cada 40s, cada vez maior.
        if (ticksDaCorrida >= INICIO_DO_CERCO && --ateOCerco <= 0) {
            ateOCerco = INTERVALO_DO_CERCO;
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

    /**
     * Devolve a corrida um chefe que estava vivo quando ela foi gravada.
     *
     * O relogio retomado ja passou do minuto em que ele nasceu, entao sem isto
     * ele simplesmente sumiria — e fechar o jogo viraria um jeito de pular chefe.
     * Ele volta na dureza do minuto gravado e com a fracao de vida que tinha.
     */
    public void devolverChefe(EntityIDs tipo, double fracaoDeVida) {
        Entity e = prepararBicho(Entity.build(tipo.ordinal(), 0, 0), true);
        e.setLife(Math.max(1, e.getLifeSize() * Math.min(1, fracaoDeVida)));
        gameMap.putAll(spawner(List.of(e)));
    }

    /** Dureza pelo tempo, vida de horda e chance de elite, para quem nasce. */
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

    private List<Entity> spawner(List<Entity> entidades) {
        // NASCER AO REDOR, LOGO FORA DA TELA.
        //
        // O grupo chega JUNTO, de uma direcao so (grupos diferentes, direcoes
        // diferentes: e isso que faz a horda vir de todos os lados), numa elipse
        // logo alem da borda da tela — calculada da janela e do zoom, entao o
        // nascimento acompanha a camera. Ponto em parede procura o chao mais
        // proximo dali; sem chao por perto, o bicho fica para o proximo grupo.
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
                continue;
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
    private static final int INTERVALO_DO_CERCO = 60 * 40;
    /** A contagem do cerco so anda depois de 1min15, para a horda ter tempo de aquecer. */
    private static final int INICIO_DO_CERCO = 60 * 75;
    /** Primeira contagem: o primeiro anel fecha 20 s depois disso, perto de 1min35. */
    private static final int PRIMEIRO_CERCO = 60 * 20;
    private static final double RAIO_DO_CERCO = 8.5;
    private int ateOCerco = PRIMEIRO_CERCO;

    /**
     * O CERCO: um anel inteiro de ratos surgindo em volta do gato ao mesmo tempo.
     *
     * E o pico de tensao da corrida, e o momento que o dano em area existe para
     * resolver — dezenas de alvos equidistantes, todos chegando juntos. Copiado
     * do anel de morcegos do Vampire Survivors: a saida e abrir caminho por um
     * lado, ou esperar a Furia encher e explodir o anel inteiro.
     *
     * O anel e de um tipo so, o mais fraco: e um teste de area, nao de resistencia.
     */
    private void cercar() {
        com.retronova.game.objects.entities.Player gato = Game.getPlayer();
        if (gato == null) {
            return;
        }
        int s = GameObject.SIZE();
        int quantos = Math.min(30, 8 + (int) (3 * minutos()));
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
            Entity e = prepararBicho(Entity.build(EntityIDs.Zombie.ordinal(), 0, 0), false);
            e.setX(x * s);
            e.setY(y * s);
            anel.add(e);
        }
        if (!anel.isEmpty()) {
            gameMap.putAll(anel);
            gameMap.put(new com.retronova.game.objects.particles.Word("SURROUNDED!",
                    gato.getX() + gato.getWidth() / 2.0, gato.getY() - s, 2));
            com.retronova.engine.sound.Sound.play(com.retronova.engine.sound.Sounds.Zombie);
        }
    }
}
