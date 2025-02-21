package com.retronova.game.objects.entities;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.graphics.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

//Criei esse escopo mas não sei se está certo - Por favor não me bata zonzini
public class Slime extends Entity {
    private final BufferedImage[][] sprite; //armazena os estados da imagem
    private int countAnim; //tempo da animação
    private int indexState; //seleção dos estados
    private int indexAnim; // seleciona a animação baseado no estado
    private Random random; //gerar valores aleatórios para pular no mapa
    private int jumpCoolDown; //serve para dar um intervalo entre os saltos
    private boolean isJumping; //verifica se está pulando

    public Slime(int ID, double x, double y) {
        super(ID, x, y, 0.8);
        sprite = new BufferedImage[][] {getSprite("sprite", 0),getSprite("sprite", 1)}; //carregamento das imagens
        jumpCoolDown = 0;
        isJumping = false;
        random = new Random(); //usei random para encontrar valores aleatórios no mapa
        //setResistances(); //não sei qual vai ser a resistência dos ataques
    }

    @Override
    public BufferedImage getSprite(){ //retorna o estado da imagem baseado no estado e na animação
        return sprite[indexState][indexAnim];
    }

    public void tick() {
        moveIA();
        countAnim++;
        if(countAnim > 10) {
            countAnim = 0;
            indexAnim++;
            if(indexAnim >= sprite[indexState].length) {
                indexAnim = 0;
            }
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
            indexState = 0;
            return;
        }


        //caso o jogador esteja perto o bastante ele vai calcular a direção do pulo
        if(distance < GameObject.SIZE() * 6) {
             radians = Math.atan2(player.getY() - getY(), player.getX() - getX());
             getPhysical().addForce(Engine.SCALE * 6, radians);
             indexState = 1;
        }else{
             radians = random.nextDouble() * (2 * Math.PI);
             getPhysical().addForce(Engine.SCALE * 8, radians);
             indexState = 1;//se não estiver perto usa o random pra pular aleatoriamente
        }
        this.indexState = getPhysical().isMoving() ? 1 : 0;

        jumpCoolDown = 30 + random.nextInt(20); //calculo do tempo de espera pra pular
    }

    public void takeDamage(AttackTypes attackTypes, double baseDamage) {
        strike(attackTypes, baseDamage);
        System.out.println("Slime tomou " + baseDamage + " de dano de" + attackTypes + " vida restante" + getLife());
    }

    @Override
    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    @Override
    public void dispose() {

    }

}
