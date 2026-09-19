package com.retronova.game.objects.entities.enemies;

import com.retronova.game.objects.Investida;

import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.util.Random;

public class Slime extends Enemy {

    private int countAnim;
    private Random random;
    private int jumpCoolDown;
    private int attackCooldown = 0;

    public Slime(int ID, double x, double y) {
        super(ID, x, y, 25);
        loadSprites("slime");
        jumpCoolDown = 0;
        random = new Random();
        setSolid();
        setSpeed(6);
        setLife(10);
        // O PESO DE XP ERA 800000.6, e nao era exagero de balanceamento: era erro
        // de digitacao com consequencia. O XP cai multiplicado por sorte e por um
        // aleatorio, e getXpLength() e cerca de 157 por nivel — um slime morto
        // subia o gato uns dois mil niveis de uma vez. O vampiro e o escudeiro
        // tinham 500000 pelo mesmo motivo.
        setXpWeight(6d);
        golpeCorpoACorpo(Investida.rapida(), 1.0, 3, AttackTypes.Melee,
                0, 24, Sounds.Slime);
    }

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

    private void moveIA() {
        double radians;
        if (jumpCoolDown > 0) {
            jumpCoolDown--;
            return;
        }

        Player player = Game.getPlayer();

        //    return;

        radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
        getPhysical().addForce("jump", getSpeed(), radians);
        //    radians = random.nextDouble() * (2 * Math.PI);
        //    getPhysical().addForce("follow", getSpeed(), radians);

        jumpCoolDown = 30 + random.nextInt(20);
    }
}