package com.retronova.game.map;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;

public class Camera {

    private GameObject followed;
    private final Rectangle bounds;
    private final double speed;

    /**
     * Velocidade enquanto uma cena esta acontecendo, ou zero quando nao ha cena.
     *
     * A camera normal persegue o gato a um quarto da distancia por quadro, e isso e
     * o certo para JOGAR: qualquer coisa mais lenta faz o personagem escapar da
     * tela. Mas a mesma pressa transforma um passeio ate a alavanca num CORTE — em
     * dez quadros a camera ja chegou, e o jogador nao viu o caminho, so o destino.
     *
     * Uma cena precisa do oposto: devagar o bastante para o olho acompanhar. Por
     * isso a velocidade e trocada durante a cena e devolvida no fim, em vez de o
     * jogo inteiro ficar lento.
     */
    private double velocidadeDeCena;

    /** Passa a mover a camera nesta velocidade. Zero volta ao normal. */
    public void velocidadeDeCena(double v) {
        this.velocidadeDeCena = v;
    }
    private float zoom;
    private float currentZoom;

    /**
     * Fator momentaneo por cima do zoom das opcoes.
     *
     * Fica separado do campo de configuracao de proposito. O zoom desejado e
     * recalculado a partir de Configs a cada tick — e uma preferencia do jogador, e
     * escrever por cima dela seria trocar a configuracao dele para contar uma cena.
     * Multiplicando, a cena manda enquanto dura e a preferencia volta sozinha
     * depois, seja ela qual for.
     */
    private float aproximacao = 1f;

    /** Fecha a camera. 1 e o normal; acima disso, mais perto. */
    public void aproximar(float fator) {
        this.aproximacao = Math.max(0.1f, fator);
    }
    private double x, y;

    public Camera(Rectangle bounds, double speed) {
        this.bounds = bounds;
        this.speed = speed;
        this.zoom = Configs.Zoom() / 100f;
        this.currentZoom = this.zoom;
    }

    public static AffineTransform getAt() {
        Camera cam = Game.getCam();
        AffineTransform at = new AffineTransform();
        int screenWidth = Engine.window.getWidth();
        int screenHeight = Engine.window.getHeight();
        int camX = cam.getX() + screenWidth/2;
        int camY = cam.getY() + screenHeight/2;
        double zoom = cam.currentZoom;
        double centerX = camX + screenWidth / 2.0 / zoom;
        double centerY = camY + screenHeight / 2.0 / zoom;
        at.translate(screenWidth / 2.0, screenHeight / 2.0);
        at.scale(zoom, zoom);
        at.translate(-centerX + (screenWidth/2d)/zoom, -centerY + (screenHeight/2d)/zoom);
        return at;
    }

    public int getX() {
        return (int) this.x;
    }

    public int getY() {
        return (int)this.y;
    }

    public Rectangle getBounds() {
        return this.bounds;
    }

    public void setX(int newX) {
        this.x = newX;
    }

    public void setY(int newY) {
        this.y = newY;
    }

    public void tick() {
        if(followed != null)
            follow();
        // A aproximacao vem das opcoes, e nao mais de Ctrl+= / Ctrl+-. O atalho
        // era ajuste de desenvolvimento, sem persistencia e sem limite util; com
        // uma opcao de verdade ele so brigaria com ela por quem manda no zoom.
        this.zoom = Configs.Zoom() / 100f * aproximacao;
        float def = (zoom - this.currentZoom) / 8f;
        this.currentZoom += def;
        if(Math.abs(zoom - currentZoom) < 0.001f) {
            this.currentZoom = zoom;
        }
    }

    public void setFollowed(GameObject followed) {
        // Trocar de alvo desfaz qualquer aproximacao de cena: sem isto, a camera
        // fechada da morte continuaria fechada na partida seguinte.
        this.aproximacao = 1f;
        this.followed = followed;
    }

    public GameObject getFollowed() {
        return this.followed;
    }

    /**
     * Segue o alvo, sem deixar o mapa acabar dentro da tela.
     *
     * O RECORTE E PELA AREA VISIVEL, E NAO PELA TELA. Com zoom, o que cabe na tela
     * nao e a largura dela: e largura dividida pelo zoom. O limite estava sendo
     * calculado com a largura cheia, o que so acerta no zoom um — e era por isso
     * que, ao fechar a camera na morte, o gato saia do meio: perto da borda do mapa
     * a camera travava num limite grande demais e a aproximacao acontecia no centro
     * da TELA em vez de em cima dele.
     *
     * As contas abaixo se reduzem exatamente as antigas quando o zoom e um, entao
     * o enquadramento normal do jogo nao muda.
     */
    private void follow() {
        int width = Engine.window.getWidth();
        int height = Engine.window.getHeight();
        double z = Math.max(0.01f, currentZoom);
        double visX = width / z;
        double visY = height / z;

        double targetX = followed.getBounds().getCenterX() - width / 2d;
        double targetY = followed.getBounds().getCenterY() - height / 2d;
        double passo = velocidadeDeCena > 0 ? velocidadeDeCena : speed;
        double xx = getX() + (targetX - getX()) * passo;
        double yy = getY() + (targetY - getY()) * passo;

        // O ponto do mundo que cai no meio da tela e (x + width/2); a janela vista
        // vai dele menos meia area visivel ate ele mais meia.
        double minX = bounds.x - width / 2d + visX / 2d;
        double maxX = bounds.width - width / 2d - visX / 2d;
        double minY = bounds.y - height / 2d + visY / 2d;
        double maxY = bounds.height - height / 2d - visY / 2d;

        setX((int) (minX > maxX ? (minX + maxX) / 2d : Math.min(maxX, Math.max(minX, xx))));
        setY((int) (minY > maxY ? (minY + maxY) / 2d : Math.min(maxY, Math.max(minY, yy))));
    }

}
