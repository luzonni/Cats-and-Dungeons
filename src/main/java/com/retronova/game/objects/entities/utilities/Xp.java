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
    /** Ja entrou no ima uma vez: vem ate o gato mesmo que ele se afaste. */
    private boolean atraida;
    /** Ticks de vida, so para a animacao da gema (idade para de contar). */
    private int vivo = com.retronova.engine.Engine.RAND.nextInt(100);   // fase propria: as gemas nao flutuam em coro

    public Xp(double x, double y) {
        super(x, y, 1);
        loadSprites("xp");
    }

    /**
     * Orbe grande: o premio de quebrar um objeto. Vale bem mais e precisa PARECER
     * que vale — maior, dourado e pulsando, para ser reconhecido no meio das
     * dezenas de orbes azuis da horda.
     */
    private boolean grande;

    /** Ainda nao foi puxada pelo ima: pode receber o peso de outra gema. */
    public boolean parada() {
        return !atraida && !grande;
    }

    public Xp grande() {
        this.grande = true;
        return this;
    }

    @Override
    /**
     * O XP é uma GEMA: forma de cristal, que nenhum inimigo tem, flutuando.
     *
     * O DESENHO É O DO REGISTRO DE ÍCONES (Icone.XP, Icone.XP_GRANDE): azul do grau
     * raro, e a grande em ouro, nas famílias de cor do jogo. Era um losango
     * pintado aqui com contorno branco puro e um halo suavizado por gema, todo
     * quadro — o halo agora é imagem pronta (Brilho). O subir-e-descer de quem
     * espera ser pego continua, em pixel inteiro.
     */
    public void render(java.awt.Graphics2D g) {
        int s = com.retronova.engine.Configs.GameScale();
        double t = (vivo + com.retronova.engine.Engine.alpha()) * 0.12;
        int cx = (int) getX() + getWidth() / 2;
        int cy = (int) (getY() + getHeight() / 2 - Math.round(Math.abs(Math.sin(t)) * 1.5) * s);
        com.retronova.engine.graphics.Familia f = grande
                ? com.retronova.engine.graphics.Familia.OURO : com.retronova.engine.graphics.Familia.RARO;
        // Halo suave: diz "brilha", não "ameaça".
        com.retronova.engine.graphics.Brilho.disco(g, cx, cy, (grande ? 10 : 6) * s,
                f.tom(com.retronova.engine.graphics.Familia.LUZ, grande ? 70 : 45));
        icone().desenharCentrado(g, cx, cy, s);
    }

    private com.retronova.game.items.Icone icone() {
        return grande ? com.retronova.game.items.Icone.XP_GRANDE : com.retronova.game.items.Icone.XP;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return this.weight;
    }

    @Override
    public void tick() {
        vivo++;
        count++;
        if(count > 15) {
            count = 0;
            getSheet().plusIndex();
        }
        Player player = Game.getPlayer();
        if(player.colliding(this)) {
            player.plusXp(getWeight());
            Game.getMap().put(new com.retronova.game.objects.particles.Coleta(icone(),
                    getX() + getWidth() / 2.0, getY() + getHeight() / 2.0,
                    com.retronova.engine.Configs.GameScale()));
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
        // O IMA PASSOU A DECIDIR. Com a corrida sem fim e o campo aberto, toda gema
        // voando ate o gato de qualquer distancia tornava o ima inutil e a coleta
        // automatica. Agora, como no genero, a gema so vem quando o gato entra no
        // alcance do ima (5 tiles, mais a passiva Ima e o upgrade permanente) — e
        // uma vez puxada, vem ate o fim. Andar ate a horda morta e uma escolha, e
        // o Ima vira uma das cartas mais desejadas.
        double alcance = player.getRangeOfColect()
                * com.retronova.game.objects.entities.Bonus.ima(player);
        if (!atraida && player.getDistance(this) > alcance) {
            return;
        }
        atraida = true;
        double limite = ARRANCO;
        if(speed < limite) {
            speed += 0.25d;
        } else if(speed > limite) {
            speed = limite;
        }
        getPhysical().addForce("follow", speed, player.getAngle(this));
    }

}
