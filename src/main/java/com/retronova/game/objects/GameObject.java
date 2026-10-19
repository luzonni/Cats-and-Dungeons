package com.retronova.game.objects;

import com.retronova.engine.Configs;
import studio.retrozoni.sheeter.SpriteSheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;

public abstract class GameObject {

    protected SpriteSheet sheet;

    private int depth;
    private boolean groundObject;
    private final int ID;
    private double x, y, width, height;
    private boolean solid = false;

    public static int SIZE() {
        return 16 * Configs.GameScale();
    }

    public GameObject(int ID) {
        this.ID = ID;
        this.width = SIZE();
        this.height = SIZE();
    }

    public abstract void loadSprites(String... sprites);

    protected void setSheet(SpriteSheet sheet) {
        this.sheet = sheet;
    }

    public int getID() {
        return this.ID;
    }

    /**
     * Posicao no tick anterior, para o render interpolar.
     *
     * A logica anda a 60 Hz; o quadro pode sair a 165. Desenhar sempre na posicao
     * do ultimo tick faz o jogo inteiro PARECER 60 FPS num monitor rapido, por
     * mais quadros que saiam. O Game desloca o desenho de cada objeto da posicao
     * atual para o ponto entre a anterior e a atual — ver deslocamentoNoQuadro.
     */
    private double antX = Double.NaN, antY = Double.NaN;

    public void guardarPosicaoAnterior() {
        this.antX = this.x;
        this.antY = this.y;
    }

    /**
     * Quanto mover o desenho para ele cair na posicao interpolada, {dx, dy}.
     * Zero para quem acabou de nascer ou se teletransportou (salto maior que dois
     * tiles): interpolar um teleporte desenharia o objeto riscando a tela.
     */
    public double[] deslocamentoNoQuadro(float alpha) {
        if (Double.isNaN(antX)) {
            return new double[]{0, 0};
        }
        double dx = x - antX;
        double dy = y - antY;
        double limite = 32 * com.retronova.engine.Configs.GameScale();
        if (dx * dx + dy * dy > limite * limite) {
            return new double[]{0, 0};
        }
        return new double[]{-(1 - alpha) * dx, -(1 - alpha) * dy};
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return (int)this.width;
    }

    public int getHeight() {
        return (int)this.height;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }

    public int getDepth() {
        return this.depth;
    }

    protected SpriteSheet getSheet() {
        return this.sheet;
    }

    public BufferedImage getSprite() {
        // Copia que vai para a GPU: o quadro cru do Sheeter e um recorte, e
        // recorte o Java2D desenha sempre na CPU. Ver Acelerada.
        return com.retronova.engine.graphics.Acelerada.de(this.sheet.getSprite());
    }

    public void setX(double newX) {
        this.x = newX;
    }

    public void setY(double newY) {
        this.y = newY;
    }

    public void setWidth(double scale) {
        this.width = SIZE()*scale;
    }

    public void setHeight(double scale) {
        this.height = SIZE()*scale;
    }

    public void setDepth() {
        int d = (int)getY() + getHeight();
        this.depth = groundObject ? 0 : d;
    }

    protected void setGroundObject() {
        this.groundObject = true;
    }

    protected void setSolid() {
        this.solid = true;
    }

    /** Nem toda mobília bloqueia passagem: entulho e portais são atravessáveis. */
    protected void setSolid(boolean solid) {
        this.solid = solid;
    }

    public boolean isSolid() {
        return this.solid;
    }

    public double getDistance(GameObject object) {
        double x1 = object.getX() + object.getWidth()/2d;
        double y1 = object.getY() + object.getHeight()/2d;
        double x2 = this.getX() + this.getWidth()/2d;
        double y2 = this.getY() + this.getHeight()/2d;
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public double getAngle(GameObject obj) {
        return Math.atan2(getY() - obj.getY(), getX() - obj.getX());
    }

    public double getAngle(Point point) {
        return Math.atan2(point.getY() - getY(), point.getX() - getX());
    }

    public static Comparator<GameObject> Depth = Comparator.comparingInt(GameObject::getDepth);

    public void renderSprite(BufferedImage sprite, Graphics2D g) {
        int x = ((int)getX() + (getWidth() - sprite.getWidth())/2);
        int y = ((int)getY() - (sprite.getHeight()) + getHeight());
        g.drawImage(sprite, x, y,null);
    }

    public abstract void tick();

    public void render(Graphics2D g) {
        renderSprite(getSprite(), g);
    }

    public void dispose() {

    }

    public void renderBounds(Graphics2D g) {
        g.setColor(Color.orange);
        g.setStroke(new BasicStroke(Configs.GameScale()/2));
        g.drawRect((int)this.getX(), (int)this.getY(), this.getWidth(), this.getHeight());
        g.setColor(new Color(255, 255, 255, 100));
        g.fillRect((int)this.getX(), (int)this.getY(), this.getWidth(), this.getHeight());
    }

}
