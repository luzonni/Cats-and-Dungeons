package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.a_star.Path;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Zombie extends Enemy {

    private Path path;
    private int countAnim;
    private int cooldown;

    public Zombie(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("mousezombie");
        setLife(40);
        setSpeed(1);
        addResistances(AttackTypes.Fire, 0.5);
        setSolid();
        this.path = new Path();
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
        if(System.currentTimeMillis()/1000 % 2 == 0) {
            Point target = new Point((int)player.getX() + getWidth()/2, (int)player.getY() + getHeight()/2);
            path.buildPath(this, target, 25);
        }
        path.follow();
    }

    @Override
    public void render(Graphics2D g) {
        int orientation = getPhysical().getOrientation()[0] * -1;
        if (orientation == 0)
            orientation = -1;
        BufferedImage sprite = SpriteHandler.flip(getSprite(), 1, orientation);
        renderSprite(sprite, g);
        //Debug dos caminhos
        //this.path.render(g);
    }

}
