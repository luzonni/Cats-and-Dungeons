package com.retronova.game.items;

import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;
import com.retronova.game.objects.entities.utilities.ThrownTrident;

import java.awt.*;

/**
 * O tridente. Arremessa e espera voltar.
 *
 * O voo inteiro mora em {@link ThrownTrident} — este arquivo so decide QUANDO
 * jogar. A separacao importa: enquanto o arremesso era simulado aqui dentro, a
 * arma nao tinha posicao propria, e sem posicao propria nao ha como testar
 * colisao ao longo do caminho. Era por isso que ela nao dava dano.
 */
public class Trident extends Item {

    /** Dano proprio, somado ao do gato. */
    private static final double DANO = 45.0;

    /** Ate onde o tridente voa antes de voltar. Em TILES, nao em pixels soltos. */
    private static final double ALCANCE = 6 * GameObject.SIZE();

    /**
     * Descanso entre um arremesso e o proximo, em ticks.
     *
     * Nao e so equilibrio: o gato fica de MAOS VAZIAS enquanto a arma esta no ar,
     * e sem uma pausa depois do retorno o tridente sairia de novo no mesmo quadro
     * em que voltou, e o desenho na mao nunca apareceria.
     */
    private static final int RECARGA = 24;

    private double angle;
    private ThrownTrident emVoo;
    private int recarga;

    Trident(int id) {
        super(id, "Trident", "trident");
        addSpecifications("Thrown, comes back", "player damage + " + DANO, "hits going and returning");
    }

    /** A arte vem do pacote, desenhada na diagonal. */
    @Override
    protected double grausDaArte() {
        return 45;
    }

    @Override
    protected Porte porte() {
        return Porte.DUAS_MAOS;
    }

    @Override
    public void tick() {
        if (emVoo != null) {
            if (emVoo.acabou()) {
                emVoo = null;
                recarga = RECARGA;
            }
            return;
        }
        if (recarga > 0) {
            recarga--;
            return;
        }
        Player player = Game.getPlayer();
        // O mais proximo ALCANCAVEL, e nao o mais proximo e ponto: com o vizinho
        // atras de um bloco, escolher por distancia so travava o arremesso.
        Entity alvo = alvoAlcancavel(player, player.getRange());
        if (alvo == null || player.getDistance(alvo) > ALCANCE) {
            return;
        }
        this.angle = alvo.getAngle(player);
        arremessar(player);
    }

    private void arremessar(Player player) {
        // Nasce no meio do gato, e nao no canto da caixa dele: o canto punha a
        // arma acima e atras do bicho, o mesmo defeito que a flecha tinha.
        double x = player.getX() + player.getWidth() / 2d;
        double y = player.getY() + player.getHeight() / 2d;
        // Aqui a origem E o meio do gato, entao o angulo do corpo ja e o certo:
        // nao ha deslocamento entre onde a arma nasce e de onde a mira foi medida.
        this.emVoo = new ThrownTrident(player, x, y, angle,
                DANO + player.getDamage(), ALCANCE);
        Game.getMap().put(emVoo);
    }

    @Override
    public void render(Graphics2D g) {
        // No ar, a arma nao esta na mao. Desenhar nos dois lugares deixaria o gato
        // com dois tridentes, que e o tipo de coisa que ninguem reporta como bug
        // mas todo mundo estranha.
        if (emVoo == null) {
            naMao(g, getSprite());
        }
    }
}
