package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.exceptions.TrapDoorCommandException;
import com.retronova.game.Game;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.room.Room;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.objects.GameObject;

import java.awt.Rectangle;

/**
 * Portão principal da dungeon. Três tiles de largura, encaixado na parede sul.
 *
 * É o ponto de não-retorno da antecâmara, e a única saída dela — assumiu o papel
 * que era dos três alçapões.
 *
 * Três decisões importantes:
 *
 *  - ESTÁ DENTRO DA PAREDE, não desenhado em cima do chão à frente dela. O vão é
 *    recortado no mapa e as jambas do sprite coincidem com os tiles de parede que
 *    sobraram dos dois lados, então o gato entra no vão em vez de pisar sobre um
 *    desenho de porta.
 *  - É objeto de chão ({@code setGroundObject}), então tem profundidade zero e é
 *    desenhado antes de todas as entidades. Sem isso o gato ficava atrás do
 *    portão ao se aproximar, porque a base do portão está mais ao sul que a dele
 *    e a ordenação por profundidade colocava o portão na frente.
 *  - COMEÇA FECHADO, E QUEM ABRE É A ALAVANCA. Antes, pisar no vão abria uma
 *    pergunta de confirmação — num lugar por onde se passa o tempo todo para
 *    chegar ao vendedor e ao acampamento, e por isso mesmo uma pergunta que se
 *    aprendia a fechar sem ler. Agora a grade está baixada e barra a passagem;
 *    descer virou uma coisa que se faz de propósito, puxando a {@link Lever} ao
 *    lado. A confirmação continua existindo, só que é a própria grade subindo.
 */
public class Gate extends Furniture {

    /** Largura do vão, em tiles. Só o meio do portão leva a algum lugar. */
    private static final double LARGURA_VAO = 1.5;

    /**
     * Ticks por quadro da subida.
     *
     * Oito quadros a seis ticks dão cerca de meio segundo — tempo de ver a grade
     * subir sem que ela vire uma espera. É o mesmo intervalo mínimo de leitura
     * que a tela de carregamento usa, e pela mesma razão: rápido demais não é
     * lido como acontecimento nenhum.
     */
    private static final int TICKS_POR_QUADRO = 6;

    private final String command;

    private int quadro;
    private int contador;
    private boolean subindo;
    /** O vão já foi fechado no mapa. Ver {@link #travar(boolean)}. */
    private boolean travou;

    public Gate(int ID, double x, double y, String command) {
        super(ID, x, y, 1000, false);
        loadSprites("gate");
        setGroundObject();
        this.command = command;
        getSheet().setIndex(0);       // quadro zero: a grade cravada na soleira
    }

    /** A grade terminou de subir e o vão está livre. */
    public boolean aberto() {
        return quadro >= getSheet().size() - 1;
    }

    /**
     * Levanta a grade. Chamada pela alavanca.
     *
     * Idempotente de propósito: puxar de novo, ou uma segunda alavanca, não
     * reinicia a animação nem fecha o portão de volta.
     */
    public void abrir() {
        if (aberto() || subindo) {
            return;
        }
        this.subindo = true;
    }

    @Override
    public void tick() {
        if (!travou) {
            travou = true;
            travar(!aberto());
        }
        if (subindo) {
            subir();
        }
        if (!aberto()) {
            return;
        }
        if (command == null || command.equals("None")) {
            return;
        }
        if (Game.getPlayer().colliding(vao())) {
            executar();
        }
    }

    private void subir() {
        contador++;
        if (contador < TICKS_POR_QUADRO) {
            return;
        }
        contador = 0;
        quadro++;
        getSheet().setIndex(quadro);
        if (aberto()) {
            subindo = false;
            travar(false);
            Sound.play(Sounds.Woosh);
        } else if (quadro % 2 == 0) {
            // A cada dois quadros, e não a cada um: a corrente da grade tem um
            // ritmo de catraca, e um estalo por quadro vira uma metralhadora.
            Sound.play(Sounds.Crack);
        }
    }

    /**
     * Fecha e abre a passagem no MAPA, e não empurrando o gato.
     *
     * Enquanto a grade está baixada, os quatro tiles do vão — a soleira e as três
     * células recortadas na parede acima dela — passam a ser sólidos, e o gato
     * bate neles exatamente como bateria na alvenaria ao lado. A primeira versão
     * corrigia a posição do jogador no tick do portão e não funcionava: as
     * entidades andam no próprio preTick, e o do jogador roda depois do tick do
     * portão, então o empurrão era desfeito no mesmo frame e dava para entrar.
     *
     * Feito no primeiro tick, e não no construtor: as entidades são criadas
     * enquanto o mapa ainda está sendo montado, e ali {@code Game.getMap()} ainda
     * não é este mapa.
     */
    private void travar(boolean travado) {
        int tile = GameObject.SIZE();
        int tx = (int) Math.round(getX() / tile);
        int ty = (int) Math.round(getY() / tile);
        for (int dx = -1; dx <= 1; dx++) {
            Game.getMap().getTile(tx + dx, ty - 1).bloquear(travado);
        }
        Game.getMap().getTile(tx, ty).bloquear(travado);
    }

    /**
     * Retângulo do vão: a soleira e a passagem, no centro do portão.
     *
     * Sobe um tile acima da célula do portão porque a soleira fica na linha de
     * cima da parede — é ali que o gato para, emoldurado pelas jambas, quando a
     * pergunta aparece.
     */
    private Rectangle vao() {
        int tile = GameObject.SIZE();
        int largura = (int) (tile * LARGURA_VAO);
        return new Rectangle(
                (int) getX() + getWidth() / 2 - largura / 2,
                (int) getY() - tile,
                largura,
                tile * 2);
    }

    /**
     * Mesma gramática de comandos do alçapão:
     * {@code LOAD ARENA <nome>}, {@code LOAD ROOM <nome>}, {@code NEXT ARENA}.
     */
    private void executar() {
        String[] partes = this.command.split(" ");
        if (partes[0].equalsIgnoreCase("LOAD") && partes.length == 3) {
            String nome = partes[2];
            if (partes[1].equalsIgnoreCase("ARENA")) {
                String[] tipos = {"EASY", "NORMAL", "HARD"};
                for (int i = 0; i < tipos.length; i++) {
                    if (tipos[i].equalsIgnoreCase(nome)) {
                        Game.getGame().setDifficult(i);
                        Game.getGame().changeMap(new Arena(i));
                        return;
                    }
                }
                throw new TrapDoorCommandException("Arena type not found: " + nome);
            }
            if (partes[1].equalsIgnoreCase("ROOM")) {
                Game.getGame().changeMap(new Room(nome));
            }
        } else if (partes[0].equalsIgnoreCase("NEXT") && partes.length == 2
                && partes[1].equalsIgnoreCase("ARENA")) {
            Game.getGame().plusLevel();
            Game.getGame().changeMap(new Arena(Game.getGame().getDifficult()));
        }
    }
}
