package com.retronova.game.items;

import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BloodyAxe extends Item {

    private double angle = 0;
    private Enemy enemy;

    private final Elemento elemento;

    BloodyAxe(int id) {
        this(id, Elemento.NENHUM);
    }

    /**
     * O machado comum: mesmo golpe, sem o roubo de vida e sem o vermelho.
     *
     * Compartilha a classe porque o movimento e o mesmo; o que muda e o efeito, e
     * um booleano diz melhor isso do que uma subclasse que so apaga um metodo.
     */
    static BloodyAxe comum(int id) {
        BloodyAxe a = new BloodyAxe(id, Elemento.NENHUM, "Axe", "axe");
        a.roubaVida = false;
        return a;
    }

    private boolean roubaVida = true;

    BloodyAxe(int id, Elemento elemento) {
        this(id, elemento,
                elemento == Elemento.NENHUM ? "Bloody Axe" : elemento.nome("Axe"),
                elemento == Elemento.NENHUM ? "bloody_axe" : elemento.sprite("axe"));
    }

    private BloodyAxe(int id, Elemento elemento, String nome, String sprite) {
        super(id, nome, sprite);
        this.elemento = elemento;
        addSpecifications("Life Leech", "Heals a percentage of damage dealt",
                elemento == Elemento.NENHUM ? "heavy swing"
                        : elemento.name().toLowerCase() + " damage");
    }

    private final Investida investida = Investida.pesada();

    /** A arte desta familia vem dos pacotes, desenhada na diagonal. */
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
        Player player = Game.getPlayer();
        this.enemy = alvoVisivel(player, player.getRange() * 0.3);
        if (this.enemy != null) {
            investida.comecar();
        }
        investida.tick();
        this.atacando = investida.ativa();
        if (investida.acertaAgora()) {
            attack(player);
        }
    }

    public void attack(Player player){
        if(enemy == null){
            return;
        }
        double damage = elemento.dano(player.getDamage());
        enemy.strike(elemento.ataque(AttackTypes.Piercing), damage);
        if (roubaVida) {
            player.setLife(player.getLife() + damage * 0.06);
        }
    }

    /**
     * O golpe pesado: recua muito, desce rápido e segue adiante.
     *
     * Antes o machado girava sem parar um doze avos de volta por tick e batia a
     * cada dezesseis — era rápido e leve, o contrário do que um machado deve
     * parecer. A referência é explícita: arma pesada se vende pelo PREPARO longo
     * e pela EXTENSÃO longa, não por bater mais.
     *
     * O arco vai de vinte graus atrás da pose até cento e quarenta à frente, e o
     * avanço acompanha — o gato joga o peso do machado para a frente em vez de
     * apenas girá-lo no lugar.
     */
    @Override
    public void render(Graphics2D g) {
        if (!atacando) {
            naMao(g, getSprite());
            return;
        }
        naMaoGolpeando(g, getSprite(), investida.avanco());
    }


}