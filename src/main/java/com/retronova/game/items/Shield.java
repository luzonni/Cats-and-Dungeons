package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * O escudo que orbita o gato.
 *
 * Era a "Dangerous Wand", que nao era varinha nenhuma: uma pedra amarrada numa
 * linha marrom girando em volta do personagem. A orbita e boa — e um item que
 * defende sem exigir mira, o que combina com um bicho pequeno e rapido —, mas o
 * desenho e o nome nao diziam isso. Agora o que gira e um escudo, e ele faz as
 * duas coisas que um escudo faz: empurra quem encosta e absorve parte do dano
 * que chega, mesmo pelas costas.
 *
 * O identificador segue {@code ItemIDs.DangerousWand}. Ali o nome e gravado nos
 * JSON de mapa e de personagem, entao renomear a constante apagaria o item dos
 * saves existentes; so o que o jogador ve mudou.
 */
public class Shield extends Item {

    private final Rectangle boundsAttack;
    private double angle;
    private final double orbitRadius = GameObject.SIZE() * 2;

    /** Ultimo inimigo golpeado nesta volta, para nao raspar dano de graca. */
    private Enemy atingido;

    /** Quanto do dano recebido o escudo come, enquanto estiver na mao. */
    private static final double ABSORCAO = 0.25;

    Shield(int id) {
        super(id, "Shield", "shield");
        addSpecifications("Orbits the cat", "absorbs 25% damage", "knocks on touch");
        this.boundsAttack = new Rectangle(GameObject.SIZE(), GameObject.SIZE());
    }

    /** A fracao do dano que o escudo absorve. Ver Player.strike. */
    public static double absorcao() {
        return ABSORCAO;
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        double x = player.getX() + player.getWidth() / 2d
                + Math.cos(this.angle) * orbitRadius - this.boundsAttack.width / 2d;
        double y = player.getY() + player.getHeight() / 2d
                + Math.sin(this.angle) * orbitRadius - this.boundsAttack.height / 2d;
        this.boundsAttack.setLocation((int) x, (int) y);
        this.angle += Math.PI / 26;
        if (this.angle > Math.PI * 2) {
            this.angle = 0;
            this.atingido = null;
        }
        bater(player);
    }

    private void bater(Player player) {
        List<Enemy> inimigos = Game.getMap().getEntities(Enemy.class);
        for (Enemy e : inimigos) {
            if (e.colliding(this.boundsAttack) && e != this.atingido) {
                this.atingido = e;
                e.strike(AttackTypes.Melee, player.getDamage() * 0.5);
                break;
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        BufferedImage sprite = getSprite();
        if (sprite == null) {
            return;
        }
        // EM PE, sem girar. A varinha antiga rodava junto com a orbita porque
        // uma pedra na ponta de uma corda gira mesmo; escudo que cambalhota
        // deixa de parecer escudo, e a 16 pixels a silhueta e tudo o que se le.
        // A linha marrom que ligava o item ao gato tambem saiu: nao ha corda.
        int x = boundsAttack.x + (boundsAttack.width - sprite.getWidth()) / 2;
        int y = boundsAttack.y + (boundsAttack.height - sprite.getHeight()) / 2;
        g.drawImage(sprite, x, y, null);
    }
}
