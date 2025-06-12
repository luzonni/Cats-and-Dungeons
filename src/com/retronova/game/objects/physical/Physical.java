package com.retronova.game.objects.physical;

import com.retronova.engine.Configs;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;

import java.util.ArrayList;
import java.util.List;

public class Physical {

    private final Entity entity;
    private final List<Vector> vectors;


    private final int weight;
    private double roughness;
    private double drag;

    private boolean isMoving;
    private int[] orientation;
    private boolean crashing;

    public Physical(Entity entity, int weight) {
        this.entity = entity;

        this.weight = weight;
        this.roughness = 0.5d;

        this.vectors = new ArrayList<>();
        this.orientation = new int[] {0, 0};
    }

    double getWeight() {
        return this.weight;
    }

    /**
     * Funcionamento do physical!
     * Essa função precisa ser executado para a física da entidade funcionar.
     */
    public void moment(){
        calcFriction();
        Vector vector = calcResultant();
        if(this.crashing)
            this.vectors.clear();
        if(vector == null) {
            isMoving = false;
            return;
        }
        double vectorX = vector.getVecX();
        double vectorY = vector.getVecY();
        this.isMoving = moveSystem(vectorX, vectorY);
    }

    private Vector calcResultant() {
        if(vectors.isEmpty()) {
            return null;
        }
        double vecX = 0d;
        double vecY = 0d;
        for(int i = 0; i < vectors.size(); i++) {
            Vector vec = vectors.get(i);
            vecX += vec.getVecX();
            vecY += vec.getVecY();
        }
        double newAngle = Math.atan2(vecY, vecX);
        double newForce = Math.sqrt(Math.pow(vecX, 2) + Math.pow(vecY, 2));
        return new Vector("result", newForce, newAngle);
    }

    private void calcFriction() {
        if(vectors.isEmpty()) {
            return;
        }
        double gravity = 9.81d;
        double delta = 1 / 60d;
        double N = weight * gravity;
        double result = (roughness * N) * delta;
        for(int i = vectors.size() - 1; i >= 0; i--) {
            Vector v = vectors.get(i);
            double vForce = v.getForce() * (1 - drag);
            v.setForce(vForce - result);
            if(v.getForce() <= 0.1d) {
                vectors.remove(v);
            }
        }
        this.drag -= 0.5d*delta;
        if(drag <= 0.01d)
            drag = 0;
    }

    /**
     *
     * @param force é um double que será a força aplicada no objeto
     * @param radians é a direção que essa força será aplicada
     */
    public void addForce(String name, double force, double radians){
        force *= Configs.GameScale();
        force *= (1 - drag);
        Vector vec = new Vector(name, force, radians);
        if(vectors.contains(vec)) {
            int index = vectors.indexOf(vec);
            Vector vector = vectors.get(index);
            vector.setForce(force);
            vector.setAngle(radians);
        }else {
            this.vectors.add(vec);
        }
        int vx = (int)Math.round(vec.getVecX());
        int vy = (int)Math.round(vec.getVecY());
        this.orientation = new int[] {(int)Math.signum(vx), (int)Math.signum(vy)};
    }

    boolean moveSystem(double vectorX, double vectorY) {
        //TODO caso alguma entidade tenha mais que 500 de peso, o movesystem ignora, isso realmente eh a melhor forma?
        if(this.weight >= 500) {
            return false;
        }
        double x = entity.getX();
        double y = entity.getY();
        boolean[] colliders = colliding(x + vectorX, y + vectorY); //analisando a próxima posição
        this.crashing = colliders[0] || colliders[1];
        boolean moving = false;
        if(!colliders[0]) {
            entity.setX(x + vectorX);
            moving = (int)(x+vectorX) != (int)x || (int)(y+vectorY) != (int)y;
        }
        if(!colliders[1]) {
            entity.setY(y + vectorY);
            moving = (int)(x+vectorX) != (int)x || (int)(y+vectorY) != (int)y;
        }
        return moving;
    }

    /**
     * Calcula a soma de dois inteiros.
     *
     * @return retorna int[2] onde o primeiro valor é o X (-1, 0, 1) e o segundo é o Y (-1, 0, 1);
     */
    public int[] getOrientation() {
        return this.orientation;
    }

    public boolean crashing() {
        return this.crashing;
    }

    public void setRoughness(double friction) {
        this.roughness = friction;
    }

    public void setDrag(double drag) {
        if(drag > 1) {
            this.drag = 1;
            return;
        }
        this.drag = drag;
    }

    /**
     *
     * @return retorna o ângulo que a entidade está se movendo.
     */
    public double getAngleForce() {
        Vector vec = calcResultant();
        if(vec == null)
            return 0;
        return vec.getAngle();
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
        int leftX = (int)(nextX) / GameObject.SIZE();
        int rightX = (int)(nextX + entity.getWidth() - 1) / GameObject.SIZE();
        int upY = (int)(nextY) / GameObject.SIZE();
        int downY = (int)(nextY + entity.getHeight() - 1) / GameObject.SIZE();
        boolean leftup = Game.getMap().getTile(leftX, upY).isSolid();
        boolean leftdown = Game.getMap().getTile(leftX, downY).isSolid();
        boolean rightup = Game.getMap().getTile(rightX, upY).isSolid();
        boolean rightdown = Game.getMap().getTile(rightX, downY).isSolid();
        return leftup || leftdown || rightdown || rightup;
    }

    public boolean isMoving() {
        return this.isMoving;
    }


}
