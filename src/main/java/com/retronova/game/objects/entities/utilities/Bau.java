package com.retronova.game.objects.entities.utilities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.arena.AberturaDoBau;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * O bau que cai de um bicho morto: sempre da elite, e dos comuns conforme os
 * pontos de sorte do gato (Player.chanceDeBau). Abri-lo mostra o baú abrindo na
 * tela (AberturaDoBau); antes rodava um caça-níquel (Roleta), hoje desligado.
 *
 * E UM OBJETO NO CHAO, e nao uma tela que abre sozinha, de proposito: interromper
 * o combate no meio de uma horda, sem o jogador pedir, e o jeito mais facil de
 * mata-lo. Assim ele decide a hora de ir buscar.
 */
public class Bau extends Utility {

    private int idade;

    public Bau(double x, double y) {
        super(x, y, 1);
        setSheet(new SpriteSheet("sprites/objects/furniture", new String[]{"chest"}));
    }

    @Override
    public void tick() {
        idade++;
        Player player = Game.getPlayer();
        // Meio segundo de carencia: se ele cair em cima do gato, nao abre no mesmo
        // quadro em que apareceu, antes de o jogador ver que ele existe.
        if (idade > 30 && player != null && player.colliding(this)) {
            // Sem som aqui: a tela do baú toca o dela ao aparecer (BauAparece), e a
            // moeda por cima virava dois sons no mesmo instante.
            disappear();
            Engine.pause(new AberturaDoBau());
        }
    }

    @Override
    public void render(Graphics2D g) {
        // Pulinho e brilho: um bau parado no chao some no meio de cem ratos.
        float t = (idade + Engine.alpha()) / 60f;
        int pulo = (int) (Math.abs(Math.sin(t * 4)) * 3 * GameObject.SIZE() / 16);
        BufferedImage sprite = getSprite();
        int x = (int) getX() + (getWidth() - sprite.getWidth()) / 2;
        int y = (int) getY() - sprite.getHeight() + getHeight() - pulo;
        int brilho = 60 + (int) (40 * Math.sin(t * 6));
        g.setColor(new Color(255, 214, 90, Math.max(0, brilho)));
        int r = sprite.getWidth();
        g.fillOval(x + sprite.getWidth() / 2 - r, y + sprite.getHeight() / 2 - r, r * 2, r * 2);
        g.drawImage(sprite, x, y, null);
    }
}
