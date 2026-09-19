package com.retronova.game.objects.entities.enemies;

import com.retronova.game.objects.Investida;

import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.a_star.CheckerNode;
import com.retronova.game.objects.a_star.PathFinder;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.tiles.Tile;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class Zombie extends Enemy {

    /**
     * Observador de tiles usado pelo A*: responde se a célula é intransponível.
     * Tile ausente conta como sólido, para o caminho não sair do mapa.
     */
    public static final CheckerNode CHECKER = (x, y) -> {
        try {
            Tile tile = Game.getMap().getTile(x, y);
            return tile.isSolid();
        } catch (RuntimeException foraDoMapa) {
            return true;
        }
    };

    /**
     * Raio de busca do A*, em tiles.
     *
     * Tem de cobrir a maior arena, senao o alvo cai FORA da janela de busca e o
     * bicho nunca calcula rota — ele fica andando reto contra a parede enquanto o
     * jogador esta do outro lado. Vinte e cinco cobria a arena antiga pela metade;
     * a cisterna tem trinta e quatro tiles de lado, e a diagonal e maior ainda.
     */
    private static final int ALCANCE_TILES = 40;

    private final PathFinder path;
    private int countAnim;
    private int cooldown;

    public Zombie(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousezombie");
        setLife(40);
        setSpeed(1);
        setWidth(0.5);
        setHeight(0.8);
        addResistances(AttackTypes.Fire, 0.5);
        setSolid();
        this.path = new PathFinder(CHECKER);
        // O MAIS LENTO GANHA O GOLPE MAIS LENTO. Ele anda a um decimo da
        // velocidade do gato; se o bote dele fosse rapido, a unica leitura possivel
        // seria "nao chegue perto", e nao ha decisao nisso. Com preparo pesado, dar
        // a volta nele passa a ser uma jogada.
        golpeCorpoACorpo(Investida.pesada(), 1.1, 4, AttackTypes.Melee,
                1.5, 40, Sounds.Zombie);
    }

    @Override
    public void tick() {
        // O GOLPE PRENDE O BICHO. Enquanto ele esta no preparo ou no corte, nao
        // anda — e o que torna o aviso visivel e o que permite sair de perto.
        if (!tickGolpe()) {
            moveIA();
        }
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    /** Converte uma posição em pixels para índice de tile. */
    private static Point emTiles(double x, double y) {
        return new Point((int) x / GameObject.SIZE(), (int) y / GameObject.SIZE());
    }

    /**
     * De quantos em quantos quadros a rota e refeita.
     *
     * O jogador anda, entao a rota envelhece — mas refazer TODO quadro era o que
     * derrubava o sistema: cada chamada abria uma thread, e sessenta por segundo
     * nunca terminavam a tempo de servir. Vinte quadros e um terco de segundo, que
     * e menos do que o gato leva para sair de um tile.
     */
    private static final int REFAZER = 20;

    private int desdeARota;

    private void moveIA() {
        Player player = Game.getPlayer();
        Point aqui = new Point((int) getBounds().getCenterX(), (int) getBounds().getCenterY());

        if (++desdeARota >= REFAZER || path.isEmpty()) {
            desdeARota = 0;
            path.buildPath(emTiles(aqui.x, aqui.y),
                    emTiles(player.getBounds().getCenterX(), player.getBounds().getCenterY()),
                    ALCANCE_TILES);
        }

        // SEM ROTA, ELE VAI RETO — e nao fica parado.
        //
        // Nao ter caminho e normal: o jogador pode estar fora da janela de busca ou
        // atras de um bloco sem volta. Antes isso virava imobilidade total, e um
        // bicho imovel nao le como "perdido", le como quebrado. Ir na direcao do
        // gato e a resposta certa mesmo quando ela esbarra numa parede: pelo menos
        // ele esta tentando, e as arenas sao abertas o bastante para a reta servir
        // na maior parte do tempo.
        Point proximo = path.getFollow();
        int size = GameObject.SIZE();
        double radianos;
        if (proximo == null) {
            radianos = Math.atan2(player.getBounds().getCenterY() - aqui.y,
                    player.getBounds().getCenterX() - aqui.x);
        } else {
            Point alvo = new Point(proximo.x * size + size / 2, proximo.y * size + size / 2);
            radianos = Math.atan2(alvo.y - aqui.y, alvo.x - aqui.x);
            if (getBounds().intersects(new Rectangle(alvo.x - size / 2, alvo.y - size / 2,
                    size, size))) {
                path.arrived();
            }
        }
        getPhysical().addForce("a_star", getSpeed(), radianos);
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderComAviso(sprite, g);
    }
}
