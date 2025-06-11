package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.a_star.CheckerNode;
import com.retronova.game.objects.a_star.PathFinder;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.tiles.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Zombie extends Enemy {

    private PathFinder path;
    private int countAnim;
    private int cooldown;

    /*
        CheckerNode é o observador de tiles para saber se ele é acessivel ou não
        caso o tile n for acessivel, retorne true (solido), caso contra, false (não solido)

        caso o tile n seja encontrado pelo mapa, retona true (solido / não acessivel)

        *caso queira verifificar se existe uma entidade por cima do tile que é solida.
    */
    public static CheckerNode cN = (x, y) -> {
        try {
            Tile tile = Game.getMap().getTile(x, y);
            return tile.isSolid();
        } catch (RuntimeException e) {
            return true;
        }
    };

    public Zombie(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousezombie");
        setLife(40);
        setSpeed(1);
        setWidth(0.5);
        setHeight(0.8);
        addResistances(AttackTypes.Fire, 0.5);
        setSolid();
        this.path = new PathFinder(cN);
    }

    public void tick() {
        moveIA();
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
        Player player = Game.getPlayer();
        //Função de dar dano ao jogador.
        cooldown++;
        if(player.getBounds().intersects(this.getBounds()) && cooldown > 45) {
            cooldown = 0;
            player.strike(AttackTypes.Melee, 2);
            Sound.play(Sounds.Zombie);
            player.getPhysical().addForce("knockback_zombie", 0.82d, getPhysical().getAngleForce());
        }
    }

    private void moveIA() {
        Player player = Game.getPlayer();

        if(path.isEmpty()) {
            Point start = this.getBounds().getLocation();
            Point goal = new Point((int)player.getX() + getWidth()/2, (int)player.getY() + getHeight()/2);
            int rangeTiles = 25;
            path.buildPath(start, goal, rangeTiles);
        }

        Point last = path.getFollow();
        if(last == null)
            return;
        Point target = new Point(last.x * GameObject.SIZE() + Tile.SIZE()/2, last.y * GameObject.SIZE() + Tile.SIZE()/2);
        Point self = new Point((int)getBounds().getCenterX(), (int)getBounds().getCenterY());
        double r = Math.atan2((target.y - self.y), (target.x - self.x));
        getPhysical().addForce("a_star", getSpeed(), r);
        if(getBounds().intersects(new Rectangle(target.x, target.y, Tile.SIZE(), Tile.SIZE()))) {
            path.arrived();
        }
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
    }

}
