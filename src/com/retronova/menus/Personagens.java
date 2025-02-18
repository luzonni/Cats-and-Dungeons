package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.io.File;

public class Personagens implements Activity {

    Player[] player = new Player[] {
            new Player(0, 0, "cinzento", 0.8, 12, 5),
            new Player(0, 0, "balofo", 0.8, 12, 10),

    };
    /*

        GameMap map = new GameMap(new File("maps/playground"));
        double x = (map.getBounds().getWidth() / GameObject.SIZE()) / 2;
        double y = (map.getBounds().getHeight() / GameObject.SIZE()) / 2;

        Player[] player = new Player[] {
                new Player(x, y, 0.8, 12, 5),
                new Player(x, y, 0.8, 12, 10),
        };

        Player player1 = player[1];

        System.out.println("Jogo Iniciado");
        Engine.setActivity(new Game(player[0],map));

     */
    public Personagens() {

    }

    @Override
    public void tick() {
    }

    @Override
    public void render(Graphics2D g) {

    }

    @Override
    public void dispose() {

    }
}
