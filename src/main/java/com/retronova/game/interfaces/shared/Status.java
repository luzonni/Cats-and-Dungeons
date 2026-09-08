package com.retronova.game.interfaces.shared;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawString;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.UiSprite;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class Status implements Activity {

    private final Player player;
    private final UiSprite status;
    /**
     * As orelhas de gato, desenhadas ACIMA do painel.
     *
     * Vem numa imagem propria em vez de assadas no PNG do painel: o painel tem
     * tamanho fixo e toda posicao de slot e contada a partir do canto dele, entao
     * crescer o arquivo empurraria a tela inteira. Ver tools/GenOrelhas.java.
     */
    private final UiSprite orelhas;

    private final HashMap<String, Point> points;

    /**
     * O quadro do retrato. Medido no status.png: o vao interno vai de (9,7) a
     * (22,26) — quatorze de largura por vinte de altura.
     *
     * O gato era desenhado com dezesseis de lado a partir de x=8, ou seja,
     * cobrindo de 8 a 23: um pixel PARA FORA do vao de cada lado. Era isso que o
     * fazia parecer que estava saindo da caixa. Doze de lado a partir de (10,11)
     * deixa uma folga de um pixel de arte nas laterais e centraliza na vertical,
     * que e o que faz o quadro parecer um quadro em vez de um recorte apertado.
     */
    private static final Point RETRATO = new Point(10, 11);
    private static final int LADO_DO_RETRATO = 12;

    private final Frame frame;

    public Status(Player player) {
        this.player = player;
        this.status = new UiSprite("ui", "status");
        this.orelhas = new UiSprite("ui", "ears_status");
        this.frame = new Frame(player);
        this.points = new HashMap<>();
        refreshPositions();
    }

    private void setLocation(String name, int x, int y) {
        if(!this.points.containsKey(name)) {
            this.points.put(name, new Point());
        }
        this.points.get(name).setLocation(x, y);
    }

    private void refreshPositions() {
        int x = Engine.window.getWidth()/2 - status.largura()/2;
        int y = Engine.window.getHeight()/2 - status.altura()/2;
        int s = Configs.HudScale();
        setLocation("main", x, y);
        setLocation("player", x + RETRATO.x*s, y + RETRATO.y*s);
        setLocation("life", x + 40*s, y + 7*s);
        setLocation("luck", x + 40*s, y + 20*s);
        setLocation("level", x + 91*s, y + 7*s);
        setLocation("money", x + 91*s, y + 20*s);
        setLocation("damage", x + 23*s, y + 49*s);
        setLocation("resistence", x + 23*s, y + 63*s);
        setLocation("range", x + 23*s, y + 76*s);
        frame.setLocation(x + 78*s, y + 46*s);
    }

    @Override
    public void tick() {
        refreshPositions();
        frame.tick();
    }

    @Override
    public void render(Graphics2D g) {
        g.drawImage(orelhas.imagem(), points.get("main").x,
                // Desce uma linha de arte: a tira tem uma linha de costura no
                // pe justamente para encostar no painel, e sem isso ela ficava
                // pairando um pixel acima, com a fresta aparecendo no meio.
                points.get("main").y - orelhas.altura() + Configs.HudScale(), null);
        g.drawImage(status.imagem(), points.get("main").x, points.get("main").y, null);
        int lado = LADO_DO_RETRATO * Configs.HudScale();
        g.drawImage(player.getSprite(0), points.get("player").x, points.get("player").y,
                lado, lado, Engine.window);
        renderString((int)player.getLife()+"/"+(int)player.getLifeSize(), points.get("life"), g);
        renderString((int)(player.getLuck()*100d)+"%", points.get("luck"), g);
        renderString(String.valueOf(player.getLevel()), points.get("level"), g);
        renderString("$ "+player.getMoney(), points.get("money"), g);
        renderString(String.valueOf(player.getDamage()), points.get("damage"), g);
        renderString(String.valueOf(player.getDamage()), points.get("resistence"), g);
        renderString(String.valueOf(player.getRange()), points.get("range"), g);
        frame.render(g);
    }

    private void renderString(String value, Point p, Graphics2D g){
        DrawString.draw(value, UiSprite.fonte(FontHandler.Septem, 8f), p, g);
    }

    @Override
    public void dispose() {

    }
    
}
