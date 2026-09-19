package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sounds;

import com.retronova.game.objects.Investida;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class CatToyBoss extends Enemy {

    @Override
    public boolean chefe() {
        return true;
    }


    public CatToyBoss(int ID, double x, double y) {
        super(ID, x, y, 70);
        setWidth(2);
        setHeight(2);
        setLife(900);
        loadSprites("cattoyboss");
        setSpeed(4);
        setSolid();
        setArmadura(200);
        // O CONTATO SAIU DAQUI TAMBEM. Um chefe que machuca por encostar e pior
        // que um rato que machuca por encostar: ele e grande, rapido e persegue, e
        // sem golpe telegrafado a unica leitura possivel e "nunca fique no caminho".
        golpeCorpoACorpo(Investida.pesada(), 1.6, 18, AttackTypes.Melee,
                10, 55, Sounds.Cat);

    }

    @Override
    public void tick() {
        if (tickGolpe()) {
            return;
        }
        Player player = Game.getPlayer();
        double angle = player.getAngle(this);
        getPhysical().addForce("Moving",getSpeed(), angle);

    }

    @Override
    public void render(Graphics2D g){
        super.render(g);
    }

}
