package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.menus.Leitura;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Placa pregada na parede, com um pedaço da lore.
 *
 * O cenário é o primeiro lugar onde a história aparece, antes de qualquer
 * diálogo — é o que documentation/padroes/ARTE-CENARIO.md chama de contador de
 * história silencioso. As placas são a versão explícita disso: quem quiser saber
 * o que aconteceu aqui, para e lê; quem não quiser, passa direto.
 *
 * ONDE ELA FICA
 *
 * A entidade é colocada NO PRÓPRIO TILE DE PAREDE, sólido, e não no chão à
 * frente dele. É isso que a faz ler como parede em vez de móvel. Não atrapalha
 * ninguém: o tile já bloqueava passagem por ser parede, e a placa não acrescenta
 * colisão nenhuma.
 *
 * COMO SE SABE QUE DÁ PRA LER
 *
 * Três coisas somadas, porque nenhuma sozinha resolveu: a arte tem moldura,
 * rebites e sulcos de comprimentos diferentes, que é o desenho universal de
 * texto; o cursor vira patinha em cima dela; e, com o gato por perto, a tábua
 * ganha um contorno que pulsa. Sem o contorno a placa era só mais um móvel — dá
 * para clicar, mas ninguém descobre isso sozinho.
 */
public class Plate extends Furniture {

    /** Alcance de leitura, em tiles. O mesmo que o do vendedor. */
    private static final double ALCANCE = 3;

    private static final Color REALCE = new Color(0xff, 0xd9, 0x8a);

    private final String content;
    private int pulso;

    public Plate(int ID, double x, double y, String content) {
        // Atravessável: é um objeto de parede, e travar o tile de chão à frente
        // dela só atrapalharia quem está circulando junto à parede.
        super(ID, x, y, 1000, false);
        loadSprites("plate");
        this.content = content;
    }

    @Override
    public void tick() {
        pulso++;
        if (!perto()) {
            return;
        }
        if (GameMap.mouseOnRect(getBounds())) {
            Engine.window.pointing();
        }
        if (GameMap.clickOnRect(Mouse_Button.LEFT, getBounds())) {
            Leitura.abrir(content);
        }
    }

    @Override
    public void render(Graphics2D g) {
        super.render(g);
        if (!perto()) {
            return;
        }
        // Pulso lento, em seno, para o realce respirar em vez de piscar.
        double onda = (Math.sin(pulso / 22d) + 1) / 2;
        int alfa = 60 + (int) (onda * 110);
        Rectangle t = tabua();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(new BasicStroke(Math.max(1, Configs.GameScale() / 2f)));
        g2.setColor(new Color(REALCE.getRed(), REALCE.getGreen(), REALCE.getBlue(), alfa));
        g2.drawRect(t.x, t.y, t.width, t.height);
        g2.dispose();
    }

    private boolean perto() {
        return Game.getPlayer().getDistance(this) <= GameObject.SIZE() * ALCANCE;
    }

    /**
     * Retangulo da TABUA, que nao e o do tile.
     *
     * A arte e menor que a celula e fica recuada dentro dela; contornar os bounds
     * desenhava o quadro em volta do tile de parede inteiro, e o realce parecia
     * piscar na alvenaria em vez de na placa. Estes numeros sao os mesmos que
     * tools/GenKenney.java usa para desenhar a tabua.
     */
    private Rectangle tabua() {
        int e = Configs.GameScale();
        return new Rectangle((int) getX() + e, (int) getY() + 2 * e, 13 * e, 11 * e);
    }

}
