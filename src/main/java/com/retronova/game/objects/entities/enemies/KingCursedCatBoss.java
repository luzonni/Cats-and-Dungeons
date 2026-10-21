package com.retronova.game.objects.entities.enemies;

import com.retronova.engine.sound.Sounds;

import com.retronova.game.objects.Investida;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class KingCursedCatBoss extends Chefe {

    @Override
    public String nome() {
        return "Cursed Cat King";
    }

    @Override
    public int fases() {
        return 2;
    }

    /** Na segunda fase ele acelera: o mesmo golpe, só que sem folga para respirar. */
    @Override
    protected void aoMudarDeFase(int nova) {
        setSpeed(getSpeed() * 1.25);
    }


    public KingCursedCatBoss(int ID, double x, double y) {
        super(ID, x, y, 80);
        setWidth(2);
        setHeight(2);
        loadSprites("kingcursedcatboss");
        setLife(1300);
        setSpeed(4);
        setSolid();
        setArmadura(300);
        // O CONTATO SAIU DAQUI TAMBEM. Um chefe que machuca por encostar e pior
        // que um rato que machuca por encostar: ele e grande, rapido e persegue, e
        // sem golpe telegrafado a unica leitura possivel e "nunca fique no caminho".
        golpeCorpoACorpo(Investida.pesada(), 1.8, 20, AttackTypes.Melee,
                10, 60, Sounds.Cat);
    }

    @Override
    public void tick() {
        verificarFase();
        if (tickGolpe()) {
            return;
        }
        Player player = Game.getPlayer();
        double angle = player.getAngle(this);
        getPhysical().addForce("Moving",getSpeed(), angle);


    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
    }



}
