package com.retronova.game.objects;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.inputs.keyboard.KeyBoard;

import java.awt.*;

public class Physical {

    private final Entity entity;
    private double speed;
    private double angleForce;
    private final double friction; //taxa de fricção do mapa
    private boolean isMoving;


    public Physical(Entity entity, double friction) {
        this.entity = entity;
        this.friction = friction;
    }

    public void moment(){
        calcFriction();
        double vectorX = Math.cos(angleForce) * speed;
        double vectorY = Math.sin(angleForce) * speed;
        this.isMoving = moveSystem(vectorX, vectorY);
    }

    public void addForce(double force, double radians){
        this.angleForce = radians;
        this.speed = force;
    }

    private boolean moveSystem(double vectorX, double vectorY){
        double x = entity.getX();
        double y = entity.getY();
        boolean[] colliders = colliding(x + vectorX, y + vectorY); //analisando a próxima posição
        if(!colliders[0]) {
            entity.setX(x + vectorX);
        }
        if(!colliders[1]) {
            entity.setY(y + vectorY);
        }

        return (int)vectorX != 0 || (int)vectorY != 0;
    }

    private boolean[] colliding(double nextX, double nextY){
        double currentX = entity.getX();
        double currentY = entity.getY();
        double speedX = nextX - currentX;
        double speedY = nextY - currentY;
        int dirX = (int)Math.signum(speedX);
        int dirY = (int)Math.signum(speedY);
        boolean[] colliders = {false, false};

        if(collidingTile(currentX+dirX, currentY)) {
            colliders[0] = true;
        }else {
            while((int)currentX != (int)nextX) {
                if(collidingTile(currentX+dirX, currentY)) {
                    colliders[0] = true;
                    entity.setX(currentX);
                    break;
                }
                currentX+=dirX;
            }
        }

        if(collidingTile(currentX, currentY+dirY)) {
            colliders[1] = true;
        }else {
            while((int)currentY != (int)nextY) {
                if(collidingTile(currentX, currentY+dirY)) {
                    colliders[1] = true;
                    entity.setY(currentY);
                    break;
                }
                currentY+=dirY;
            }
        }
        return colliders;
    }

    private boolean collidingTile(double nextX, double nextY) {
        int leftX = (int)(nextX / GameObject.SIZE());
        int rightX = (int)((nextX + entity.getWidth()) / GameObject.SIZE());
        int upY = (int)(nextY / GameObject.SIZE());
        int downY = (int)((nextY + entity.getHeight()) / GameObject.SIZE());
        Tile tile1 = Game.getMap().getTile(leftX, upY);
        Tile tile2 = Game.getMap().getTile(rightX, downY);
        return tile1.isSolid() || tile2.isSolid();
    }

    private void calcFriction() {
        this.speed *= friction;
    }

    public boolean isMoving() {
        return this.isMoving;
    }




    /*
        Aqui, precisará existir um sistema de movimentação que funcionará para todas as entidades, então todas
        as funções precisam lidar com a variável "entity"* que está nessa classe.

        *"entity":
            Todas as entidades carregam dentro de si uma instãncia do objeto dessa classe (Physical), pode conferir,
            inclusive todas as entidades têm uma função pública que retorna a sua própria instãncia desse objeto
            (Physical);

        Como ela deve se comportar:
            A partir da variável "entity", você tem acesso ao objeto que contem esse objeto physical:
                [ ( GameObjects ) -> Entity : Entity possui Physical como atributo ]
            *Apenas as (Entity)'s possuem física.

            Ou seja, você pode manipular o possuidor desse objeto a partir da entity! Isso precisa estar claro!

            Com isso em mente, o sistema de física deve manipular a movimentação, colisão, efeitos de movimentação
            e qualquer efeito físico das entidades.

            Quando quiser lidar com uma função que compara outra entidade, use como referência:

                função colidindo(Entity outra_entidade):
                    this.entity.colliding(outra_entidade);

            Nesse exemplo, a função colidindo() recebe uma outra entidade, e comparando a entidade principal com a
            outra entidade é possível fazer validações, ações e manipulações.

            Como ajuda, indico criar métodos separados que lidam com situações especificas, pois muitas funções
            vão precisar de constantes de execução, por exemplo, é indicado criar uma função de movimento,
            para que outras funções possam reutilizá-la.
            Preste atenção a visibilidade dos métodos, não é interessante que todos sejam realmente públicos,
            como mencionado, existiram métodos que serão utilizados apenas dentro das lógicas pertencentes a esta
            classe, com isso, esses métodos precisam ser privados. Isso ajuda a não ter confusão na hora de aplicar
            as funções no jogo e previne uso indevido de métodos fechados (métodos que são lógicos apenas dentro
            de outros métodos). Isso previne ‘bugs’!

            Todas as colisões entre entidades serão colisões entre círculos, é uma forma fácil de verificas, com
            menos chances de ocorrerem 'bugs'.
            Como ela funciona resumidamente:
                TODOS os GameObjects tem uma função estática chamada SIZE(), você pode chama-la usando:
                    GameObject.SIZE();
                essa função retorna um inteiro da largura padão das entidades, para conseguir o raio do círculo
                de colisão das entidades, basta usar:
                    double r = GameObject.SIZE()/2d;
                esse r é constante é não consegue ser valido para entidades que tenham tamanhos maiores que o
                padrão do jogo, mas pode ser usado como constante.
                Caso queira o raio verdadeiro de cada entidade, use:
                    double r = this.entity.getWidth()/2d;
                Isso retornará o raio da entidade desse physical.

            Como funciona a colisão entre círculos? Para ela ocorrer você precisa encontrar o círculo das duas entidades
            que estão em colisão, ver o quanto um círculo está dentro do outro (intersectando) e aplicar* essa diferença
            a metade para cada entidade, exemplo:

                funcão circleCollision(Entity otherEntity):
                     double distanceX = this.entity.getBounds().getCenterX() - otherEntity.getBounds().getCenterX();
                     double distanceY = this.entity.getBounds().getCenterY() - otherEntity.getBounds().getCenterY();
                     double theta = Math.sqrt((distanceX * distanceX) + (distanceY * distanceY));
                     double r1 = this.entity.getWidth()/2d;
                     double r2 = otherEntity.getWidth()/2d;
                     f (theta<= (r1 + r2))
                         return true;
                     return false;

            Essa função apenas verifica se os dois círculos estão em intersecção um com o outro. Para a colisão
            realmente funcionar é necessário saber o quanto cada círculo em intersecção um com o outro e subtrair
            essa diferença na sua posição. A lógica pode se seguir:

                double intersection = Math.abs((r1 + r2) - theta);

            Aplicar a força a metade*: com valor de 'intersection', você pode dividir o valor por dois e aplicar uma
            força inversa as duas entidades em colisão. Ou seja, essa função não bloqueia a movimentação das entidades
            e sim aplica uma força de repulsão as duas entidades em colisão, isso previne 'bugs' de movimentação.

            Qualquer dúvida sobre a aplicação das lógicas, pode acessar este link e vá para a página 13!
            https://www.researchgate.net/publication/374083225_COLLISION_DETECTION_IN_2D_GAMES

     */



}
