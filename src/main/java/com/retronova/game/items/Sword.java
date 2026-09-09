package com.retronova.game.items;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.Rotate;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.AttackTypes;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Sword extends Item {

    /**
     * De que lado vem o golpe: +1 de fora para dentro, -1 de volta.
     *
     * Alterna a cada investida, entao os golpes seguidos fazem direita-esquerda em
     * vez de repetir o mesmo movimento. E o que a referencia chama de forehand e
     * backhand: repetir o mesmo lado le como a mesma animacao tocando de novo.
     */
    private int side;

    /**
     * A INVESTIDA, no lugar da varredura antiga.
     *
     * O golpe anterior era uma volta de 180 graus em velocidade CONSTANTE, sem
     * comeco nem fim: o rad andava de PI/12 em PI/12 e recomecava. Isso quebra as
     * tres coisas que toda referencia de animacao de golpe repete — antecipacao,
     * impacto rapido e sustentacao —, e o resultado e uma arma que parece girar
     * sozinha em vez de golpear.
     *
     * A Investida ja tinha essa estrutura e ja era usada pelo machado: recua
     * devagar (antecipacao), cruza a distancia inteira em tres ticks (o impacto),
     * segue adiante quase parada (a sustentacao, que e o que da peso) e volta
     * (recuperacao). Aqui ela entra na versao LEVE, 400 ms, que e o tempo de
     * espada de uma mao — o machado usa a pesada, de 800.
     */
    // NAO e inicializador de campo: os inicializadores rodam ANTES do corpo do
    // construtor, e la o elemento ainda e nulo. Montada aqui, depois que ele
    // existe, e o unico jeito de a cadencia dele valer.
    private final Investida investida;

    @Override
    public double cadencia() {
        return elemento.cadencia();
    }

    private final BufferedImage sword_attack;
    private final double damage;
    private final Rectangle boundsAttack;


    private final Elemento elemento;

    @Override
    public Elemento elemento() {
        return this.elemento;
    }

    Sword(int id) {
        this(id, Elemento.NENHUM);
    }

    /**
     * A variante elemental. Mesma espada, mesmo golpe: so o pigmento e o tipo de
     * dano mudam — e e de proposito que o gesto seja identico, para o jogador ler
     * "a mesma espada, de fogo" em vez de aprender uma arma nova.
     */
    Sword(int id, Elemento elemento) {
        super(id, elemento.nome("Sword"), elemento.sprite("sword"));
        this.elemento = elemento;
        this.investida = Investida.leve().vezes(cadencia());
        setIndexSprite(Engine.RAND.nextInt(25));
        this.damage = elemento.dano(35);
        this.side = 1;
        this.boundsAttack = new Rectangle(GameObject.SIZE()*2, (int)(GameObject.SIZE()*3d));
        sword_attack = new SpriteHandler("sprites/items", "sword_attack", Configs.GameScale()).getSHEET();
        addSpecifications("Melee Attack", "Player damage + "+ this.damage,
                elemento == Elemento.NENHUM ? "very fast" : elemento.rotulo().toLowerCase() + " damage");
    }

    /** A arte desta familia vem dos pacotes, desenhada na diagonal. */
    @Override
    protected double grausDaArte() {
        return 45;
    }

    @Override
    public int duracaoDaReacao() {
        return investida.duracao();
    }

    @Override
    protected Porte porte() {
        return Porte.UMA_MAO;
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        setBoundsAttack(player);
        Enemy nearest = alvoVisivel(player, 3);
        if (nearest != null) {
            investida.comecar();
        }
        boolean estava = investida.ativa();
        investida.tick();
        this.atacando = investida.ativa();
        if (investida.acertaAgora() && nearest != null) {
            attack(player, nearest);
        }
        // Acabou a investida: o proximo golpe vem do outro lado.
        if (estava && !investida.ativa()) {
            side *= -1;
        }
    }

    private void setBoundsAttack(Player player) {
        int dist = (this.side == -1) ? this.boundsAttack.width * side : 0;
        double x = (player.getX() + player.getWidth()/2d) + dist;
        double y = (player.getY() + player.getHeight()/2d) - this.boundsAttack.height/2d;
        this.boundsAttack.setLocation((int)x, (int)y);
    }

    private void attack(Player player, Enemy enemy) {
        if(enemy.colliding(this.boundsAttack)) {
            enemy.strike(elemento.ataque(AttackTypes.Melee), this.damage + player.getDamage());
            double r = enemy.getAngle(player);
            enemy.getPhysical().addForce("knockback", 3, r);
        }
        Sound.play(Sounds.Sword);
    }

    public void render(Graphics2D g) {
        // Parado, a pose vem do porte, igual para todas as armas. So o golpe
        // e desenhado por aqui.
        if (!atacando) {
            naMao(g, getSprite());
            return;
        }
        // Mesmo caminho do machado: a pose manda no lugar e no arco, e o giro do
        // golpe sai de naMaoGolpeando. Antes a espada tinha uma conta so dela e
        // ignorava a pose — mexer nela no editor nao mudava o golpe.
        naMaoGolpeando(g, getSprite(), investida.avanco() * side);
        drawAttackEffect(g);
    }

    private void drawAttackEffect(Graphics2D g) {
        // O rastro so aparece no IMPACTO, os poucos ticks em que a lamina cruza a
        // distancia. Deixa-lo aceso o golpe inteiro anula o efeito: rastro que
        // dura vira parte do desenho, e nao um golpe.
        double avanco = investida.avanco();
        if (avanco > -0.35 && avanco < 0.6) {
            // O rastro sai na cor do elemento: e o que faz a espada de fogo
            // PARECER de fogo na hora do golpe, e nao so no icone da hotbar.
            BufferedImage corte = elemento == Elemento.NENHUM
                    ? this.sword_attack : tingir(this.sword_attack, elemento.cor());
            BufferedImage flipped = SpriteHandler.flip(corte, 1, side);
            int x = this.boundsAttack.x + (this.boundsAttack.width - this.sword_attack.getWidth())/2;
            int y = this.boundsAttack.y + (this.boundsAttack.height - this.sword_attack.getHeight())/2;
            g.drawImage(flipped, x, y, null);
        }
    }

}
