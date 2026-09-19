package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sounds;

import com.retronova.game.objects.Investida;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class CryingCat extends Enemy{

    public CryingCat(int ID, double x, double y) {
        super(ID, x, y, 75);
        setWidth(2);
        setHeight(2);
        loadSprites("cryingcat");
        setLife(1500);
        setSpeed(4);
        setSolid();
        // O CONTATO SAIU DAQUI TAMBEM. Um chefe que machuca por encostar e pior
        // que um rato que machuca por encostar: ele e grande, rapido e persegue, e
        // sem golpe telegrafado a unica leitura possivel e "nunca fique no caminho".
        golpeCorpoACorpo(Investida.media(), 1.3, 22, AttackTypes.Melee,
                10, 50, Sounds.Cat);
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

    public void render(Graphics2D g) {
        super.render(g);
    }
}
