package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.exceptions.TrapDoorCommandException;
import com.retronova.game.Game;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.room.Room;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.objects.GameObject;

import java.awt.Graphics2D;
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
        this(ID, x, y, command, "gate");
    }

    /**
     * @param sprite qual porta desenhar.
     *
     * A antecamara usa a grade de ferro; a cisterna usa um batente de pedra. Sao
     * lugares diferentes, e repetir a mesma porta faria a segunda sala parecer a
     * primeira. A MECANICA e a mesma — fechada, alavanca abre, atravessar leva
     * adiante — e e justamente por ser a mesma que ela nao foi duplicada.
     */
    public Gate(int ID, double x, double y, String command, String sprite) {
        super(ID, x, y, 1000, false);
        this.invisivel = "none".equalsIgnoreCase(sprite);
        loadSprites(invisivel ? "gate" : sprite);
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
        // A PASSAGEM RESPONDE NA HORA. A alavanca da o clique; sem uma resposta do
        // outro lado, o jogador ouve o clique e nao sabe se aconteceu alguma coisa.
        Sound.play(Sounds.Lit);
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
        }
        // O CASCALHO SAIU. Ele era a corrente da grade de ferro arrastando, e nao
        // ha mais grade: a passagem agora e um corredor que acende. Som de pedra
        // quebrando sobre uma luz que se acende nao descreve coisa nenhuma — sobra
        // so o sopro do fim, que e o que a luz de fato faz.
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
    /**
     * A LUZ QUE SAI DA PASSAGEM ABERTA.
     *
     * A ideia veio de olhar a porta aberta e nao acreditar nela: numa vista de
     * cima, uma folha de porta girando no chao le como um alcapao caido, porque o
     * chao nao tem para onde uma porta abrir. O que o olho aceita ali e o VAO — um
     * retangulo escuro na parede — e o vao sozinho nao diz se da para passar; ele
     * parece igual antes e depois da alavanca.
     *
     * Quem resolve isso e a luz. Passagem aberta e passagem iluminada: o clarao
     * morno derramando da abertura para dentro da sala diz "por aqui" sem nenhum
     * texto, e diz TAMBEM que alguma coisa mudou, porque ele nao existia antes.
     * E o mesmo recurso que a tocha e o braseiro ja usam nesta mesma sala, entao
     * nao e uma linguagem nova — e a que o jogo ja fala.
     *
     * A luz PULSA de leve. Um clarao parado vira mancha e o olho para de ve-lo em
     * poucos segundos; um que respira continua chamando enquanto a porta estiver
     * la esperando.
     */
    private void luzDaPassagem(Graphics2D g) {
        if (quadro <= 0) {
            return;
        }
        // Acompanha a abertura: comeca fraca no primeiro quadro e chega inteira
        // quando o vao esta livre.
        float forca = Math.min(1f, quadro / (float) Math.max(1, getSheet().size() - 1));
        respiro += 0.06;
        float pulso = (float) (0.85 + 0.15 * Math.sin(respiro));

        // A LUZ NASCE DENTRO DO CORREDOR e derrama para a sala, e nao de um ponto no
        // meio do vao. Ela tem a largura da passagem e se estende para tras dela —
        // e assim que se le "vem luz la de dentro" em vez de "ha uma lampada aqui".
        Rectangle v = vao();
        int cx = v.x + v.width / 2;
        int cy = v.y + v.height / 2;
        int raio = (int) (GameObject.SIZE() * 2.2 * forca * pulso);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        // Camadas concentricas, e nao um degrade: no mesmo resultado visual, custa
        // menos que criar uma pintura por quadro — e o jogo desenha isto sessenta
        // vezes por segundo.
        int camadas = 5;
        for (int i = camadas; i >= 1; i--) {
            int r = raio * i / camadas;
            int alfa = (int) (26 * forca * (1f - (i - 1) / (float) camadas));
            g2.setColor(new java.awt.Color(255, 214, 150, Math.max(0, alfa)));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        // Um nucleo mais quente no proprio vao, que e de onde a luz vem.
        g2.setColor(new java.awt.Color(255, 236, 200, (int) (90 * forca * pulso)));
        g2.fillRect(v.x, v.y, v.width, v.height);
        g2.dispose();
    }

    private double respiro;

    /**
     * SEM DESENHO DE PORTA — so a luz.
     *
     * A passagem daqui nao e uma porta, e um CORREDOR: um vao aberto na parede que
     * esta la desde o comeco da sala, visivel o tempo todo. Enquanto a briga corre
     * ele e um corredor escuro que nao leva a lugar nenhum; quando a alavanca e
     * puxada, a luz acende dentro dele.
     *
     * Duas tentativas anteriores erraram por insistir em desenhar a porta. Grade
     * subindo e folha girando sao gestos VERTICAIS, e numa vista de cima o chao nao
     * tem vertical: o olho le os dois como um objeto se levantando do piso. O vao
     * nao tem esse problema porque nao se move — quem muda e a luz.
     *
     * O sprite continua existindo para quem quiser uma porta de verdade algum dia;
     * pedindo "none" no mapa, nada e desenhado e sobra so o corredor de tiles e a
     * luz por cima dele.
     */
    private final boolean invisivel;

    @Override
    public void render(Graphics2D g) {
        if (!invisivel) {
            super.render(g);
        }
        luzDaPassagem(g);
    }

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
