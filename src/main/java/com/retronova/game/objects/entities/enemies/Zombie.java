package com.retronova.game.objects.entities.enemies;

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
    }

    @Override
    public void tick() {
        moveIA();
        countAnim++;
        if (countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
        Player player = Game.getPlayer();
        cooldown++;
        if (player.getBounds().intersects(this.getBounds()) && cooldown > 45) {
            cooldown = 0;
            player.strike(AttackTypes.Melee, 2);
            Sound.play(Sounds.Zombie);
            player.getPhysical().addForce("knockback_zombie", 0.82d, getPhysical().getAngleForce());
        }
    }

    /** Converte uma posição em pixels para índice de tile. */
    private static Point emTiles(double x, double y) {
        return new Point((int) x / GameObject.SIZE(), (int) y / GameObject.SIZE());
    }

    private void moveIA() {
        Player player = Game.getPlayer();

        // O A* trabalha inteiramente em índices de tile: origem, destino e alcance.
        // Passar pixels aqui fazia a checagem de alcance do WayMap reprovar toda
        // chamada — o zumbi nunca chegava a calcular caminho nenhum.
        if (path.isEmpty()) {
            Point origem = emTiles(getBounds().getCenterX(), getBounds().getCenterY());
            Point destino = emTiles(player.getBounds().getCenterX(), player.getBounds().getCenterY());
            path.buildPath(origem, destino, ALCANCE_TILES);
        }

        Point proximo = path.getFollow();
        if (proximo == null) {
            return;
        }

        int size = GameObject.SIZE();
        Point alvo = new Point(proximo.x * size + size / 2, proximo.y * size + size / 2);
        Point aqui = new Point((int) getBounds().getCenterX(), (int) getBounds().getCenterY());
        double radianos = Math.atan2(alvo.y - aqui.y, alvo.x - aqui.x);
        getPhysical().addForce("a_star", getSpeed(), radianos);

        if (getBounds().intersects(new Rectangle(alvo.x - size / 2, alvo.y - size / 2, size, size))) {
            path.arrived();
        }
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0) {
            orientation = -1;
        }
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
    }
}
