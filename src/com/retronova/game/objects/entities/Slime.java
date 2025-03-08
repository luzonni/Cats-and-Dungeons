package com.retronova.game.objects.entities;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

//Criei esse escopo mas não sei se está certo - Por favor não me bata zonzini
public class Slime extends Entity {

    private int countAnim; //tempo da animação
    private Random random; //gerar valores aleatórios para pular no mapa
    private int jumpCoolDown; //serve para dar um intervalo entre os saltos
    private boolean isJumping; //verifica se está pulando

    Slime(int ID, double x, double y) {
        super(ID, x, y, 0.4);
        loadSprites("slime");
        jumpCoolDown = 0;
        isJumping = false;
        random = new Random(); //usei random para encontrar valores aleatórios no mapa
        //setResistances(); //não sei qual vai ser a resistência dos ataques
        setSolid();
        setAlive();
        //valor alto apenas para testar ganho de xp alto
        setXpWeight(800000.6d);
    }

    public void tick() {
        moveIA();
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            getSheet().plusIndex();
        }
    }

    private void moveIA() {
        double radians;
        //impedir que o slime fique pulando o tempo todo
        if(jumpCoolDown > 0) {
            jumpCoolDown--;
            return;
        }
        //captura a posição do jogador
        Player player = Game.getPlayer();
        double distance = Math.sqrt(Math.pow((player.getX() - getX()), 2) + Math.pow(player.getY() - getY(), 2)); //calcula a distância entre o slime e o player

        if(distance < GameObject.SIZE() * 1.0) {
            return;
        }


        //caso o jogador esteja perto o bastante ele vai calcular a direção do pulo
        if(distance < GameObject.SIZE() * 6) {
             radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
             getPhysical().addForce(6, radians);
        }else{
             radians = random.nextDouble() * (2 * Math.PI);
             getPhysical().addForce(8, radians);
        }

        jumpCoolDown = 30 + random.nextInt(20); //calculo do tempo de espera pra pular
    }

}
