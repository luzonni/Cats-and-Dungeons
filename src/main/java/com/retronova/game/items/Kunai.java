package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.Rotate;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.KunaiThrown;

import java.awt.*;

public class Kunai extends Item {

    private double angle;
    private final double damage = 2;
    private int countAttack;
    private final Point kunaiPosition;
    private final Point spriteRotatePosition;

    public Kunai(int id) {
        super(id, "Kunai", "kunai");
        this.kunaiPosition = new Point();
        this.spriteRotatePosition = new Point((int)(2.5*Configs.GameScale()), (int)(13.5*Configs.GameScale()));
        addSpecifications("Throwable", "Player damage + " + this.damage, "medium speed");
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        // A kunai fica NA PATA, e nao orbitando o corpo. O ponto de rotacao dela
        // e a argola do cabo, no canto de baixo do sprite; encostar essa argola na
        // pata e o que faz a arma parecer segura em vez de flutuando ao lado. O
        // arremesso tambem sai dai, que e de onde a mao a soltaria.
        Point mao = player.getMao();
        this.kunaiPosition.setLocation(mao.x - spriteRotatePosition.x,
                mao.y - spriteRotatePosition.y);
        Enemy target = player.getNearest(player.getRange(), Enemy.class);
        if(target != null) {
            this.angle = target.getAngle(player);
            countAttack++;
            if(countAttack >= player.getAttackSpeed()*5) {
                countAttack = 0;
                double currentDamage = player.getDamage() + this.damage;
                KunaiThrown kunai = new KunaiThrown(kunaiPosition.getX(), kunaiPosition.getY(), currentDamage, this.angle);
                Game.getMap().put(kunai);
            }
        }else {
            countAttack = 0;
        }
    }

    @Override
    public void render(Graphics2D g) {
        Rotate.draw(getSprite(), kunaiPosition.x, kunaiPosition.y, this.angle + Math.PI/4, spriteRotatePosition, g);
    }
}