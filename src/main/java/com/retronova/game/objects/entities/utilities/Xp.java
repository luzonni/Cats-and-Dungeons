package com.retronova.game.objects.entities.utilities;

import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

/**
 * A experiência largada por um bicho morto.
 *
 * ELA VAI ATRÁS DO JOGADOR, e não o contrário.
 *
 * Antes o ímã só ligava a cinco tiles de distância. Numa sala pequena isso passa
 * despercebido, mas a arena tem trinta e quatro tiles de lado: quase toda morte
 * acontecia fora do alcance, e a recompensa ficava parada no chão esperando ser
 * buscada. O resultado era o jogador varrendo o campo depois de cada onda — um
 * trabalho sem decisão nenhuma, que só existe porque o item não sabe andar.
 *
 * Pior: isso briga com o resto do combate. O jogo pede que se fique em movimento
 * enquanto há bicho vivo, e ao mesmo tempo castigava quem não parasse para
 * catar. Deixando a experiência vir sozinha, matar E receber viram o mesmo gesto.
 *
 * O ESPALHAMENTO CONTINUA. Ela ainda salta do corpo por um terço de segundo antes
 * de o ímã ligar — é esse salto que faz uma morte parecer render alguma coisa. O
 * que mudou é que agora ele acaba em volta do gato, e não onde calhou de cair.
 */
public class Xp extends Utility {

    private int count;
    private double weight;
    private double speed;

    /**
     * Quadros de salto livre antes de o ímã ligar.
     *
     * Sem eles a experiência gruda no gato no mesmo quadro em que o bicho morre, e
     * a morte deixa de ter um instante próprio: some o bicho, some o prêmio, não
     * sobra nada para o olho registrar. Vinte quadros é o tempo de ver o pulinho.
     */
    private static final int ESPALHANDO = 20;

    /** Velocidade de cruzeiro, longe do gato. */
    private static final double PASSEIO = 8d;

    /** Velocidade quando já está perto — o arranco final. */
    private static final double ARRANCO = 15d;

    private int idade;

    public Xp(double x, double y) {
        super(x, y, 1);
        loadSprites("xp");
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return this.weight;
    }

    @Override
    public void tick() {
        count++;
        if(count > 15) {
            count = 0;
            getSheet().plusIndex();
        }
        Player player = Game.getPlayer();
        if(player.colliding(this)) {
            player.plusXp(getWeight());
            disappear();
            return;
        }
        if(idade < ESPALHANDO) {
            idade++;
            return;
        }
        // O ALCANCE DE COLETA DEIXOU DE SER UM PORTÃO E VIROU UMA MARCHA.
        //
        // Ele ainda serve para alguma coisa — dentro dele a experiência corre quase
        // o dobro, e é isso que faz o último pedaço do caminho parecer um puxão em
        // vez de uma aproximação. Fora dele ela não para mais; só vem mais devagar.
        //
        // Assim o orbe magnético continua fazendo diferença sem precisar existir
        // para o sistema funcionar: ele passa a ser "chega mais rápido", que é um
        // efeito honesto, em vez de "chega".
        double limite = player.getDistance(this) < player.getRangeOfColect()
                ? ARRANCO : PASSEIO;
        if(speed < limite) {
            speed += 0.25d;
        } else if(speed > limite) {
            speed = limite;
        }
        getPhysical().addForce("follow", speed, player.getAngle(this));
    }

}
