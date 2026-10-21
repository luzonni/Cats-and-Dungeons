package com.retronova.game.map;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.gamepad.Gamepad;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;

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
        this.antZoom = this.zoom;
    }

    public static AffineTransform getAt() {
        Camera cam = Game.getCam();
        AffineTransform at = new AffineTransform();
        int screenWidth = Engine.window.getWidth();
        int screenHeight = Engine.window.getHeight();
        // Posicao e zoom INTERPOLADOS e sem arredondar. A camera arredondava para
        // pixel inteiro e so mudava a cada tick: mesmo com o mundo fluido, a tela
        // inteira andava aos degraus de 60 Hz.
        float a = Engine.pausado() ? 1f : Engine.alpha();
        double cx = Double.isNaN(cam.antX) ? cam.x : cam.antX + (cam.x - cam.antX) * a;
        double cy = Double.isNaN(cam.antY) ? cam.y : cam.antY + (cam.y - cam.antY) * a;
        // Tremor: deslocamento que oscila rapido e encolhe sozinho. Ruido de senos
        // em vez de aleatorio puro, para nao "teleportar" a tela a cada quadro.
        if (cam.tremor > 0.05) {
            double t = System.nanoTime() / 1e9;
            cx += Math.sin(t * 71) * cam.tremor;
            cy += Math.cos(t * 59) * cam.tremor;
        }
        double camX = cx + screenWidth/2d;
        double camY = cy + screenHeight/2d;
        double zoom = cam.antZoom + (cam.currentZoom - cam.antZoom) * a;
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
        this.antX = Double.NaN;   // salto: sem interpolar
        this.x = newX;
    }

    public void setY(int newY) {
        this.antY = Double.NaN;
        this.y = newY;
    }

    /** Estado do tick anterior, para o getAt interpolar. */
    private double antX = Double.NaN, antY = Double.NaN;
    private float antZoom;

    /** Amplitude atual do tremor de tela, em pixels do mundo. Decai sozinha. */
    private double tremor;

    /**
     * Sacode a tela. Golpes grandes (a Furia, uma elite morrendo) precisam de
     * PESO, e nada vende impacto tao barato quanto a camera sentir o golpe.
     * Tremores somados nao passam de um teto, para a tela nunca ficar ilegivel.
     */
    public static void tremer(double intensidade) {
        Camera cam = Game.getCam();
        cam.tremor = Math.min(18, cam.tremor + intensidade * com.retronova.engine.Configs.GameScale() / 4.0);
    }

    public void tick() {
        antX = x;
        antY = y;
        antZoom = currentZoom;
        tremor *= 0.86;
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
     * Quanto a camera pode se afastar do gato pelo analogico direito, em tiles.
     *
     * UM E MEIO, E NAO MEIA TELA. Num jogo de horda a ameaca vem de todos os lados
     * ao mesmo tempo, e uma camera que se afasta muito tira da tela justamente o
     * lado de onde o gato nao esta olhando. Era 3, e ainda espalhava demais: a
     * espiada tem de ser uma inclinada de cabeca, com o gato sempre perto do meio.
     */
    private static final double ALCANCE_DO_OLHAR = 1.5;
    /**
     * Fracao do caminho que o olhar anda por tick. Mais lento que a camera, que
     * persegue o gato a um quarto por tick: sem esta folga, um toque no analogico
     * virava um tranco na tela inteira. Era 0,12, e ainda pedia menos.
     */
    private static final double SUAVIDADE_DO_OLHAR = 0.06;

    /** Deslocamento atual do olhar, em pixels do mundo. */
    private double olharX, olharY;

    /**
     * Desloca a camera para onde o analogico direito aponta, e a traz de volta
     * quando ele e solto.
     *
     * So vale com o gato no comando: durante uma cena (a chegada, a ida ate a
     * alavanca) e na queda, a camera e da cena, e o jogador nao a disputa.
     */
    private void espiar() {
        double alvoX = 0, alvoY = 0;
        if (velocidadeDeCena <= 0 && followed instanceof Player gato
                && !gato.chegando() && !gato.morrendo()) {
            double[] olhar = Gamepad.olhar();
            // CURVA QUADRATICA: meio curso do analogico espia um quarto do
            // alcance, e nao metade. Linear, qualquer encostada no analogico ja
            // deslocava a tela; assim so quem empurra de verdade vai longe.
            double forca = Math.hypot(olhar[0], olhar[1]);
            double alcance = ALCANCE_DO_OLHAR * GameObject.SIZE() * forca;
            alvoX = olhar[0] * alcance;
            alvoY = olhar[1] * alcance;
        }
        olharX += (alvoX - olharX) * SUAVIDADE_DO_OLHAR;
        olharY += (alvoY - olharY) * SUAVIDADE_DO_OLHAR;
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

        // Em double do comeco ao fim: arredondar aqui fazia a camera andar em
        // degraus de pixel e travar a poucos pixels do alvo.
        double targetX = followed.getX() + followed.getWidth() / 2d - width / 2d;
        double targetY = followed.getY() + followed.getHeight() / 2d - height / 2d;
        espiar();
        targetX += olharX;
        targetY += olharY;
        double passo = velocidadeDeCena > 0 ? velocidadeDeCena : speed;
        double xx = this.x + (targetX - this.x) * passo;
        double yy = this.y + (targetY - this.y) * passo;

        // O ponto do mundo que cai no meio da tela e (x + width/2); a janela vista
        // vai dele menos meia area visivel ate ele mais meia.
        double minX = bounds.x - width / 2d + visX / 2d;
        double maxX = bounds.width - width / 2d - visX / 2d;
        double minY = bounds.y - height / 2d + visY / 2d;
        double maxY = bounds.height - height / 2d - visY / 2d;

        this.x = minX > maxX ? (minX + maxX) / 2d : Math.min(maxX, Math.max(minX, xx));
        this.y = minY > maxY ? (minY + maxY) / 2d : Math.min(maxY, Math.max(minY, yy));
    }

}
