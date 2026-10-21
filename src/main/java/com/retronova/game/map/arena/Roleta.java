package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.items.ArmaExtra;
import com.retronova.game.items.Melhorias;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

/**
 * O bau abre num caca-niquel: tres rolos verticais que param um de cada vez, ate
 * o mesmo premio cair nos tres.
 *
 * POR QUE TRES ROLOS, se o premio e um so. A fita horizontal antiga tinha um
 * unico instante de suspense — a parada. Tres rolos que travam em sequencia
 * tem tres: o primeiro mostra o que PODE vir, o segundo confirma ou frustra, e
 * o terceiro e o que se espera de olho grudado. E a mecanica inteira do slot de
 * cassino, e e por isso que ele prende.
 *
 * O premio e decidido ANTES do giro, como em toda maquina de verdade: os rolos
 * sao so a cerimonia. Nao ha escolha — a escolha mora na Recompensa de fim de
 * sala; aqui o jogador so torce.
 */
public class Roleta implements Activity {

    /** Simbolos em cada rolo. A vencedora fica perto do fim, para o giro ser longo. */
    private static final int SIMBOLOS = 28;
    private static final int VENCEDORA = SIMBOLOS - 3;
    /** A alavanca desce antes de os rolos andarem. */
    private static final int ALAVANCA = 14;
    /** Quando cada rolo para, contado depois da alavanca. */
    private static final int[] PARADA = {70, 100, 135};
    private static final int FIM = PARADA[2];
    /** Ticks entre o jackpot e o cartao do premio saltar. */
    private static final int ESPERA_DO_CARTAO = 36;

    /** O neon do letreiro. */
    private static final Color[] NEON = {
            new Color(0xFF3B6B), new Color(0xFFD23F), new Color(0x3BFF8A),
            new Color(0x3BC8FF), new Color(0xC23BFF), new Color(0xFF8A3B)
    };
    private static final Color OURO = new Color(0xFFD23F);
    private static final Color OURO_ESCURO = new Color(0xA8740E);

    private final List<List<Premio>> rolos = new ArrayList<>();
    private final Premio vencedor;

    private int tempo;
    private final boolean[] parado = new boolean[3];
    private final int[] ultimoSimbolo = {-1, -1, -1};
    private boolean revelada;
    private int desdeARevelacao;

    // ------------------------------------------------------------ moedas

    /** Moeda em coordenadas de TELA. */
    private static final class Moeda {
        double x, y, vx, vy, giro, velGiro;
    }

    private final List<Moeda> moedas = new ArrayList<>();

    /**
     * Moedas caindo ATRAS da maquina, o tempo todo, devagar.
     *
     * Sao o cenario do cassino, e nao o premio: menores, mais escuras e mais
     * lentas quanto mais "longe" estao (o campo profundidade, de 0.35 a 1).
     * Isso da a camada de fundo que faltava — a maquina ficava solta no veu.
     */
    private static final class MoedaDeFundo {
        double x, y, vy, giro, velGiro, profundidade;
    }

    private final List<MoedaDeFundo> moedasDoFundo = new ArrayList<>();

    public Roleta() {
        Player gato = Game.getPlayer();
        double promocao = gato.hasModifier(Modifiers.Fortune)
                ? gato.valueModifier(Modifiers.Fortune) : 0;
        Melhorias melhorias = gato.getMelhorias();
        // EVOLUCAO PRONTA SAI GARANTIDA: arma no maximo + passiva parceira fazem o
        // proximo bau ser dela. E o jackpot de verdade da corrida.
        com.retronova.game.items.Evolucao evolucao = melhorias.evolucaoDisponivel();
        this.vencedor = evolucao != null
                ? new Premio(null, evolucao.arma(), EVOLUCAO)
                : sortear(melhorias, promocao);
        com.retronova.game.Estatisticas.bauAberto();
        for (int r = 0; r < 3; r++) {
            List<Premio> rolo = new ArrayList<>();
            for (int i = 0; i < SIMBOLOS; i++) {
                rolo.add(i == VENCEDORA ? vencedor : sortear(melhorias, promocao));
            }
            rolos.add(rolo);
        }
        Sound.play(Sounds.Coin);
    }

    // ------------------------------------------------------------ premios

    /**
     * O que a maquina pode dar: uma melhoria de atributo (carta) ou uma arma extra
     * (ou o proximo nivel dela). Uma das duas e nula.
     */
    /** nivelNovo especial: o premio e a EVOLUCAO da arma. */
    private static final int EVOLUCAO = -1;

    private record Premio(Melhorias.Carta carta, ArmaExtra arma, int nivelNovo) {

        Color cor() {
            if (nivelNovo == EVOLUCAO) {
                return new Color(0xFFD23F);
            }
            return arma != null ? arma.cor() : carta.raridade().cor();
        }

        String rotulo() {
            if (nivelNovo == EVOLUCAO) {
                return "EVOLUTION!";
            }
            if (arma == null) {
                return carta.raridade().rotulo();
            }
            return nivelNovo <= 1 ? "NEW WEAPON" : "WEAPON LV " + nivelNovo;
        }

        String nome() {
            if (nivelNovo == EVOLUCAO) {
                return com.retronova.game.items.Evolucao.de(arma).nome();
            }
            return arma != null ? arma.nome() : carta.melhoria().nome();
        }

        String efeito() {
            if (nivelNovo == EVOLUCAO) {
                return com.retronova.game.items.Evolucao.de(arma).descricao();
            }
            return arma != null ? arma.efeito(nivelNovo) : carta.efeito();
        }

        String icone() {
            return arma != null ? arma.name() : carta.melhoria().name();
        }

        /**
         * A cor do SIMBOLO, que e do assunto e nao da raridade: um coracao comum
         * cinza sobre a fita creme sumia. A raridade vai numa faixa ao lado.
         */
        Color corDoIcone() {
            if (arma != null) {
                return arma.cor();
            }
            return switch (carta.melhoria()) {
                case VIGOR -> new Color(0xE0334F);
                case FURIA -> new Color(0xF07A22);
                case COURO -> new Color(0x5B8BD6);
                case PATAS -> new Color(0x2FB7D6);
                case FRENESI -> new Color(0xB45CF0);
                case INSTINTO -> new Color(0x3DBF52);
                case PRESAGIO -> new Color(0xE0A800);
            };
        }
    }

    /**
     * Arma ou carta. Arma sai em ~40% das vezes — 55% enquanto o gato nao tem
     * nenhuma, para a primeira chegar cedo e a corrida ganhar forma.
     */
    private static Premio sortear(Melhorias melhorias, double promocao) {
        List<ArmaExtra> armas = melhorias.armasDisponiveis();
        boolean temAlguma = java.util.Arrays.stream(ArmaExtra.values())
                .anyMatch(a -> melhorias.nivel(a) > 0);
        double chanceArma = temAlguma ? 0.40 : 0.55;   // seis armas na mesa: mais chance de sair uma
        if (!armas.isEmpty() && Engine.RAND.nextDouble() < chanceArma) {
            ArmaExtra a = armas.get(Engine.RAND.nextInt(armas.size()));
            return new Premio(null, a, melhorias.nivel(a) + 1);
        }
        Melhorias.Carta c = melhorias.oferecer(1, Game.getGame().getLevel(), promocao).getFirst();
        return new Premio(c, null, 0);
    }

    // ------------------------------------------------------------ medidas

    /**
     * Escala da maquina: a da interface, mas reduzida se nao couber na janela.
     * A maquina inteira tem uns 210 pixels de arte de altura.
     */
    private int escala() {
        int s = Configs.UiScale();
        int cabe = Math.max(1, (Engine.window.getHeight() - 20) / 215);
        return Math.max(1, Math.min(s, cabe));
    }

    private int larguraCelula(int s) {
        return 46 * s;
    }

    private int alturaCelula(int s) {
        return 40 * s;
    }

    // -------------------------------------------------------------- giro

    /**
     * Quanto o rolo r ja girou, em celulas, no instante t (ticks depois da
     * alavanca). Freia com um pequeno passo alem e volta — o "tranco" do rolo
     * travando, que e o que faz a parada ser sentida.
     */
    private double giro(int r, double t) {
        double x = Math.clamp(t / PARADA[r], 0, 1);
        double c1 = 1.1, c3 = c1 + 1;
        double p = 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);   // ease-out-back
        return p * VENCEDORA;
    }

    private double tempoDoGiro(float alpha) {
        if (revelada) {
            return FIM;
        }
        return Math.max(0, tempo + alpha - ALAVANCA);
    }

    // -------------------------------------------------------------- tick

    @Override
    public void tick() {
        tickMoedas();
        boolean confirmar = KeyBoard.KeyPressed("Enter") || KeyBoard.KeyPressed("Space")
                || Mouse.clickOn(Mouse_Button.LEFT,
                new Rectangle(0, 0, Engine.window.getWidth(), Engine.window.getHeight()));
        if (!revelada) {
            tempo++;
            // Clicar durante o giro pula para o fim.
            if (confirmar && tempo > ALAVANCA + 5) {
                tempo = ALAVANCA + FIM;
            }
            double t = tempoDoGiro(0);
            for (int r = 0; r < 3; r++) {
                if (!parado[r] && t >= PARADA[r]) {
                    parado[r] = true;
                    Sound.play(Sounds.Button);          // o rolo trava
                }
            }
            // O tique sai so do ultimo rolo (o que para por ultimo): tres tiques
            // ao mesmo tempo viravam chiado.
            if (!parado[2] && tempo > ALAVANCA) {
                int simbolo = (int) giro(2, t);
                if (simbolo != ultimoSimbolo[2]) {
                    ultimoSimbolo[2] = simbolo;
                    Sound.play(Sounds.Hover);
                }
            }
            if (parado[0] && parado[1] && parado[2]) {
                revelar();
            }
            return;
        }
        desdeARevelacao++;
        if (desdeARevelacao % 9 == 0 && desdeARevelacao < 45) {
            Sound.play(Sounds.Coin);
        }
        if (confirmar && desdeARevelacao > ESPERA_DO_CARTAO + 12) {
            Player gato = Game.getPlayer();
            if (vencedor.nivelNovo() == EVOLUCAO) {
                gato.getMelhorias().evoluir(com.retronova.game.items.Evolucao.de(vencedor.arma()));
            } else if (vencedor.arma() != null) {
                gato.getMelhorias().subirArma(vencedor.arma());
            } else {
                gato.getMelhorias().aplicar(gato, vencedor.carta().melhoria(),
                        vencedor.carta().raridade());
            }
            Sound.play(Sounds.Button);
            Engine.pause(null);
        }
    }

    private void revelar() {
        revelada = true;
        Sound.play(Sounds.Conquista);
        Sound.play(Sounds.Coin);
        int s = escala();
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        // As moedas saem da BANDEJA da maquina, como num pagamento de verdade, e
        // duas rajadas menores dos lados. Moderado: moeda demais cobre o premio.
        int bandejaY = h / 2 + 88 * s;
        jorrar(40, w / 2.0, bandejaY, 15);
        jorrar(14, w / 2.0 - 110 * s, bandejaY, 12);
        jorrar(14, w / 2.0 + 110 * s, bandejaY, 12);
    }

    private void jorrar(int quantas, double x, double y, double forca) {
        int s = escala();
        for (int i = 0; i < quantas; i++) {
            Moeda m = new Moeda();
            m.x = x + (Engine.RAND.nextDouble() - 0.5) * 30 * s;
            m.y = y;
            double ang = -Math.PI / 2 + (Engine.RAND.nextDouble() - 0.5) * 1.6;
            double v = forca * (0.55 + Engine.RAND.nextDouble() * 0.6) * s / 3.0;
            m.vx = Math.cos(ang) * v;
            m.vy = Math.sin(ang) * v;
            m.giro = Engine.RAND.nextDouble() * Math.PI * 2;
            m.velGiro = 0.15 + Engine.RAND.nextDouble() * 0.3;
            moedas.add(m);
        }
    }

    private void tickMoedasDoFundo() {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int s = escala();
        // Chovem mais no jackpot.
        double chance = revelada ? 0.9 : 0.35;
        if (moedasDoFundo.size() < 60 && Engine.RAND.nextDouble() < chance) {
            MoedaDeFundo m = new MoedaDeFundo();
            m.profundidade = 0.35 + Engine.RAND.nextDouble() * 0.65;
            m.x = Engine.RAND.nextDouble() * w;
            m.y = -10 * s;
            m.vy = (0.6 + Engine.RAND.nextDouble() * 0.6) * m.profundidade * s;
            m.giro = Engine.RAND.nextDouble() * Math.PI * 2;
            m.velGiro = (0.05 + Engine.RAND.nextDouble() * 0.1) * m.profundidade;
            moedasDoFundo.add(m);
        }
        for (int i = moedasDoFundo.size() - 1; i >= 0; i--) {
            MoedaDeFundo m = moedasDoFundo.get(i);
            m.y += m.vy;
            m.giro += m.velGiro;
            if (m.y > h + 20 * s) {
                moedasDoFundo.remove(i);
            }
        }
    }

    private void tickMoedas() {
        tickMoedasDoFundo();
        int h = Engine.window.getHeight();
        int s = escala();
        for (int i = moedas.size() - 1; i >= 0; i--) {
            Moeda m = moedas.get(i);
            m.x += m.vx;
            m.y += m.vy;
            m.vy += 0.35 * s / 3.0;
            m.giro += m.velGiro;
            if (m.y > h + 20 * s) {
                moedas.remove(i);
            }
        }
    }

    // ------------------------------------------------------------ desenho

    @Override
    public void render(Graphics2D g) {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        int s = escala();
        float a = Engine.alpha();
        double quadro = tempo + desdeARevelacao + a;

        // Fundo bem mais escuro que antes (205 -> 238) e com vinheta: o jogo
        // atras quase some, e a maquina vira o unico ponto de luz da tela.
        g.setColor(new Color(8, 4, 12, 238));
        g.fillRect(0, 0, w, h);
        desenharVinheta(g, w, h);
        desenharHolofotes(g, w, h, s, quadro);
        desenharMoedasDoFundo(g, s, a);
        if (revelada) {
            desenharRaios(g, w / 2, h / 2, Math.max(w, h), quadro);
        }

        int cw = larguraCelula(s), ch = alturaCelula(s), vao = 4 * s;
        int largRolos = 3 * cw + 2 * vao;
        int janelaH = 3 * ch;
        int cx = w / 2, cy = h / 2;
        int janelaX = cx - largRolos / 2, janelaY = cy - janelaH / 2 + 6 * s;

        desenharGabinete(g, s, cx, janelaX, janelaY, largRolos, janelaH, quadro);

        // Os rolos, recortados na janela.
        Graphics2D r = (Graphics2D) g.create();
        r.clip(new Rectangle(janelaX, janelaY, largRolos, janelaH));
        double t = tempoDoGiro(a);
        for (int i = 0; i < 3; i++) {
            int rx = janelaX + i * (cw + vao);
            double giro = giro(i, t);
            double velocidade = Math.abs(giro - giro(i, t - 1));
            desenharRolo(r, rolos.get(i), rx, janelaY, cw, ch, janelaH, giro, velocidade, s,
                    revelada, quadro);
        }
        r.dispose();
        desenharLinhaDePagamento(g, s, janelaX, janelaY + janelaH / 2, largRolos, quadro);

        desenharMoedas(g, s, a);

        if (revelada) {
            desenharPremio(g, s, cx, cy, quadro);
        }
        // Clarao no instante do jackpot.
        if (revelada && desdeARevelacao < 10) {
            int alfa = (int) (180 * (1 - (desdeARevelacao + a) / 10f));
            g.setColor(new Color(255, 255, 255, Math.max(0, alfa)));
            g.fillRect(0, 0, w, h);
        }
    }

    /** Escurece as bordas: o olho vai para o meio, onde esta a maquina. */
    private static void desenharVinheta(Graphics2D g, int w, int h) {
        // Pronta numa imagem pequena e esticada: o degrade radial de tela cheia,
        // recalculado a cada quadro, nao e acelerado pelo pipeline.
        if (vinheta == null) {
            int lado = 128;
            vinheta = new java.awt.image.BufferedImage(lado, lado, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gv = vinheta.createGraphics();
            gv.setPaint(new java.awt.RadialGradientPaint(lado / 2f, lado / 2f, lado * 0.75f,
                    new float[]{0.35f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 200)}));
            gv.fillRect(0, 0, lado, lado);
            gv.dispose();
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(vinheta, 0, 0, w, h, null);
        g2.dispose();
    }

    private static java.awt.image.BufferedImage vinheta;

    /** Fachos e pocas prontos, por cor e abertura. Ver {@link #desenharHolofotes}. */
    private static final java.util.Map<String, java.awt.image.BufferedImage> FACHOS = new java.util.HashMap<>();
    private static final int FACHO_COMPRIMENTO = 256;

    /**
     * Um facho deitado: nasce no meio da borda esquerda e abre para a direita,
     * com o nucleo mais forte, sumindo no fim. Pequeno de proposito — luz e
     * suave, e ampliada ninguem nota a resolucao.
     */
    private static java.awt.image.BufferedImage facho(Color cor, int forca, double abertura) {
        String k = cor.getRGB() + "|" + forca + "|" + abertura;
        return FACHOS.computeIfAbsent(k, kk -> {
            int L = FACHO_COMPRIMENTO;
            int meia = (int) Math.ceil(Math.tan(abertura) * L);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(L, meia * 2 + 2,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gf = img.createGraphics();
            gf.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int cy = meia + 1;
            for (int passe = 0; passe < 2; passe++) {
                double ab = passe == 0 ? abertura : abertura * 0.35;
                int f = passe == 0 ? forca : forca / 2;
                int m = (int) Math.round(Math.tan(ab) * L);
                gf.setPaint(new GradientPaint(0, 0, new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), f),
                        L, 0, new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 0)));
                gf.fillPolygon(new int[]{0, L, L}, new int[]{cy, cy - m, cy + m}, 3);
            }
            gf.dispose();
            return img;
        });
    }

    private static java.awt.image.BufferedImage poca(Color cor, int forca) {
        String k = "poca|" + cor.getRGB() + "|" + forca;
        return FACHOS.computeIfAbsent(k, kk -> {
            int lado = 64;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(lado, lado,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gp = img.createGraphics();
            gp.setPaint(new java.awt.RadialGradientPaint(lado / 2f, lado / 2f, lado / 2f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), forca),
                            new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 0)}));
            gp.fillRect(0, 0, lado, lado);
            gp.dispose();
            return img;
        });
    }

    /**
     * HOLOFOTES: fachos de luz descendo do alto da tela e varrendo devagar,
     * cruzando por tras da maquina, como num palco de cassino.
     *
     * Cada facho e um triangulo com degrade — forte no foco, sumindo no fim — e
     * deixa uma poca de luz eliptica no chao. Somados em modo aditivo leve, os
     * fachos que se cruzam clareiam o meio, que e onde a maquina esta. Antes do
     * jackpot sao brancos e frios; no jackpot ganham as cores do neon e aceleram.
     */
    private void desenharHolofotes(Graphics2D g, int w, int h, int s, double quadro) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        java.awt.geom.AffineTransform base0 = g2.getTransform();
        int fachos = 4;
        double vel = revelada ? 0.035 : 0.015;
        double chao = h * 0.92;
        for (int i = 0; i < fachos; i++) {
            // Bases espalhadas no alto; cada uma balanca com fase propria.
            double bx = w * (0.12 + 0.76 * i / (fachos - 1.0));
            double by = -20 * s;
            double balanco = Math.sin(quadro * vel + i * 1.7) * 0.55;
            // Os de fora apontam para dentro: todos cruzam perto do centro.
            double base = Math.atan2(chao - by, w / 2.0 - bx);
            double ang = base + balanco * 0.6;
            double comprimento = Math.hypot(w, h) * 1.05;
            double abertura = Math.toRadians(revelada ? 11 : 8);
            Color cor = revelada ? NEON[(i * 2 + (int) (quadro / 20)) % NEON.length] : new Color(0xC8D4FF);
            int forca = revelada ? 70 : 48;

            // O facho pronto, girado e esticado ate o comprimento: um drawImage.
            java.awt.image.BufferedImage f = facho(cor, forca, abertura);
            double escalaF = comprimento / FACHO_COMPRIMENTO;
            g2.setTransform(base0);
            g2.translate(bx, by);
            g2.rotate(ang);
            g2.scale(escalaF, escalaF);
            g2.drawImage(f, 0, -f.getHeight() / 2, null);
            g2.setTransform(base0);

            // Poca de luz onde o facho bate no chao.
            double t = (chao - by) / Math.max(1e-3, Math.sin(ang));
            if (t > 0) {
                double px = bx + Math.cos(ang) * t;
                double larg = Math.tan(abertura) * t * 2.2;
                int r = (int) Math.max(10, larg / 2);
                g2.drawImage(poca(cor, forca / 2), (int) (px - r), (int) (chao - r * 0.25), r * 2, (int) (r * 0.5), null);
            }
        }
        g2.dispose();
    }

    private void desenharMoedasDoFundo(Graphics2D g, int s, float a) {
        for (MoedaDeFundo m : moedasDoFundo) {
            int r = (int) Math.max(s, 4 * s * m.profundidade);
            int x = (int) m.x;
            int y = (int) (m.y + m.vy * a);
            // Mais longe = mais apagada: perspectiva atmosferica.
            float alfa = (float) (0.25 + 0.45 * m.profundidade);
            desenharMoeda(g, x, y, r, Math.cos(m.giro + m.velGiro * a), s, alfa);
        }
    }

    /**
     * Uma moeda com volume: a BORDA aparece como uma faixa escura quando ela gira
     * de lado, a face tem um anel em relevo e um brilho no alto. Antes era uma
     * elipse com outra dentro.
     */
    private static void desenharMoeda(Graphics2D g, int x, int y, int r, double gir, int s, float alfa) {
        Composite antes = g.getComposite();
        if (alfa < 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
        }
        int largura = Math.max(Math.max(1, s / 2), (int) (Math.abs(gir) * r * 2));
        // Espessura da borda: aparece mais quanto mais de lado a moeda esta.
        int borda = Math.max(1, (int) ((1 - Math.abs(gir)) * r * 0.6)) + Math.max(1, s / 2);
        int xb = gir >= 0 ? x - largura / 2 + borda : x - largura / 2 - borda;
        g.setColor(new Color(0x6E4300));                     // borda/lateral
        g.fillOval(Math.min(xb, x - largura / 2), y - r, largura + borda, r * 2);
        g.setColor(new Color(0x1A0E02));                     // contorno da face
        g.fillOval(x - largura / 2, y - r, largura, r * 2);
        g.setColor(gir > 0 ? OURO : new Color(0xE0A800));    // face
        int pad = Math.max(1, s / 2);
        g.fillOval(x - largura / 2 + pad, y - r + pad, Math.max(1, largura - 2 * pad), r * 2 - 2 * pad);
        if (largura > r) {
            // Anel em relevo e brilho.
            g.setColor(new Color(0xB07A00));
            int ar = (int) (r * 0.55);
            int aw = Math.max(1, (int) (largura * 0.55));
            g.drawOval(x - aw / 2, y - ar, aw, ar * 2);
            g.setColor(new Color(0xFFF4B0));
            g.fillRect(x - largura / 4, y - r / 2, Math.max(1, s), Math.max(1, r / 2));
        }
        g.setComposite(antes);
    }

    /** O corpo da maquina: gabinete, letreiro, lampadas, alavanca e bandeja. */
    private void desenharGabinete(Graphics2D g, int s, int cx, int janelaX, int janelaY,
                                  int largRolos, int janelaH, double quadro) {
        int margem = 14 * s;
        int x = janelaX - margem;
        int larg = largRolos + 2 * margem;
        int topo = janelaY - 46 * s;
        int base = janelaY + janelaH + 40 * s;
        int alt = base - topo;

        // Sombra no chao.
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(x - 10 * s, base - 4 * s, larg + 20 * s, 14 * s);

        // Gabinete. ERA VERMELHO-BOMBEIRO, a unica cor saturada da tela inteira, e
        // por isso a maquina parecia de outro jogo. Agora usa a ardosia/ameixa da
        // paleta (Palette.MAIN/DEEP) com o ouro so nos frisos, e tem volume: luz
        // de cima (chanfro claro no topo), sombra embaixo e um reflexo laqueado.
        g.setColor(new Color(0x0C0612));
        g.fillRoundRect(x - 3 * s, topo - 3 * s, larg + 6 * s, alt + 6 * s, 18 * s, 18 * s);
        g.setPaint(new GradientPaint(0, topo, new Color(0x5A4A7E), 0, topo + alt, new Color(0x1E1630)));
        g.fillRoundRect(x, topo, larg, alt, 14 * s, 14 * s);
        // Laterais mais escuras: o gabinete e uma caixa, nao uma placa.
        g.setPaint(new GradientPaint(x, 0, new Color(0, 0, 0, 90), x + 16 * s, 0, new Color(0, 0, 0, 0)));
        g.fillRoundRect(x, topo, 16 * s, alt, 14 * s, 14 * s);
        g.setPaint(new GradientPaint(x + larg - 16 * s, 0, new Color(0, 0, 0, 0), x + larg, 0, new Color(0, 0, 0, 110)));
        g.fillRoundRect(x + larg - 16 * s, topo, 16 * s, alt, 14 * s, 14 * s);
        // Chanfro de luz no topo.
        g.setColor(new Color(255, 255, 255, 45));
        g.fillRoundRect(x + 4 * s, topo + 2 * s, larg - 8 * s, 3 * s, 4 * s, 4 * s);
        // Friso dourado duplo: escuro por fora, claro por dentro, da relevo.
        g.setStroke(new java.awt.BasicStroke(2f * s));
        g.setColor(OURO_ESCURO);
        g.drawRoundRect(x + 3 * s, topo + 3 * s, larg - 6 * s, alt - 6 * s, 10 * s, 10 * s);
        g.setStroke(new java.awt.BasicStroke(Math.max(1f, s)));
        g.setColor(OURO);
        g.drawRoundRect(x + 4 * s, topo + 4 * s, larg - 8 * s, alt - 8 * s, 9 * s, 9 * s);
        // Reflexo vertical no canto esquerdo: o gabinete e laqueado.
        g.setPaint(new GradientPaint(0, topo, new Color(255, 255, 255, 50), 0, topo + alt, new Color(255, 255, 255, 0)));
        g.fillRoundRect(x + 8 * s, topo + 10 * s, 4 * s, alt - 20 * s, 4 * s, 4 * s);

        // Letreiro no alto.
        int lx = x + 10 * s, ly = topo + 8 * s, lw = larg - 20 * s, lh = 28 * s;
        g.setColor(new Color(0x14081C));
        g.fillRoundRect(lx, ly, lw, lh, 8 * s, 8 * s);
        String texto = revelada ? "JACKPOT!" : "LUCKY CAT";
        Color neon = revelada ? NEON[(int) (quadro / 4) % NEON.length] : OURO;
        Font fonte = FontHandler.font(FontHandler.Game, 13f * s);
        int tw = FontHandler.getWidth(texto, fonte);
        // Halo do neon: o texto repetido, largo e transparente, por tras.
        g.setFont(fonte);
        g.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), 70));
        for (int k = -2; k <= 2; k += 2) {
            for (int j = -2; j <= 2; j += 2) {
                g.drawString(texto, cx - tw / 2 + k * s / 2, ly + lh / 2 + 6 * s + j * s / 2);
            }
        }
        TextFx.draw(g, texto, fonte, cx - tw / 2, ly + lh / 2 + 6 * s, neon, s);

        // Moldura da janela dos rolos.
        g.setColor(OURO_ESCURO);
        g.fillRect(janelaX - 4 * s, janelaY - 4 * s, largRolos + 8 * s, janelaH + 8 * s);
        g.setColor(OURO);
        g.fillRect(janelaX - 3 * s, janelaY - 3 * s, largRolos + 6 * s, janelaH + 6 * s);
        g.setColor(new Color(0x0E0A14));
        g.fillRect(janelaX - s, janelaY - s, largRolos + 2 * s, janelaH + 2 * s);

        // Bandeja de moedas embaixo.
        int bx = cx - 50 * s, by = janelaY + janelaH + 14 * s;
        g.setColor(new Color(0x14081C));
        g.fillRoundRect(bx, by, 100 * s, 18 * s, 8 * s, 8 * s);
        g.setColor(OURO_ESCURO);
        g.fillRoundRect(bx + 4 * s, by + 10 * s, 92 * s, 5 * s, 4 * s, 4 * s);

        // Lampadas correndo pela borda do gabinete.
        desenharLampadas(g, x, topo, larg, alt, s, quadro);

        // Alavanca a direita: desce no comeco e volta.
        double puxada = tempo < ALAVANCA ? Math.sin(tempo / (double) ALAVANCA * Math.PI) : 0;
        int px = x + larg + 8 * s, py = janelaY + janelaH / 2;
        g.setColor(new Color(0x2A1A20));
        g.fillRoundRect(px - 2 * s, py - 6 * s, 8 * s, 14 * s, 4 * s, 4 * s);
        double ang = 0.2 + puxada * 1.5;
        int hx = px + 2 * s + (int) (Math.sin(ang) * 44 * s);
        int hy = py - (int) (Math.cos(ang) * 44 * s);
        g.setStroke(new java.awt.BasicStroke(3f * s, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xC9C9D6));
        g.drawLine(px + 2 * s, py, hx, hy);
        g.setColor(new Color(0xE0233A));
        g.fillOval(hx - 6 * s, hy - 6 * s, 12 * s, 12 * s);
        g.setColor(new Color(255, 255, 255, 140));
        g.fillOval(hx - 3 * s, hy - 4 * s, 4 * s, 4 * s);
    }

    private void desenharLampadas(Graphics2D g, int x, int y, int larg, int alt, int s, double quadro) {
        int espaco = 11 * s;
        int raio = 2 * s;
        List<int[]> pontos = new ArrayList<>();
        for (int px = x + espaco; px < x + larg; px += espaco) {
            pontos.add(new int[]{px, y + raio + s});
        }
        for (int py = y + espaco; py < y + alt; py += espaco) {
            pontos.add(new int[]{x + larg - raio - s, py});
        }
        for (int px = x + larg - espaco; px > x; px -= espaco) {
            pontos.add(new int[]{px, y + alt - raio - s});
        }
        for (int py = y + alt - espaco; py > y; py -= espaco) {
            pontos.add(new int[]{x + raio + s, py});
        }
        int passo = (int) (quadro * (revelada ? 0.6 : 0.4));
        boolean pisca = revelada && ((int) (quadro / 5)) % 2 == 0;
        for (int i = 0; i < pontos.size(); i++) {
            int[] p = pontos.get(i);
            boolean acesa = revelada ? (pisca ^ (i % 2 == 0)) : (i + passo) % 4 == 0;
            Color c = revelada ? NEON[(i + passo) % NEON.length] : OURO;
            if (acesa) {
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
                g.fillOval(p[0] - raio * 2, p[1] - raio * 2, raio * 4, raio * 4);
                g.setColor(Color.WHITE);
                g.fillOval(p[0] - raio, p[1] - raio, raio * 2, raio * 2);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 160));
                g.fillOval(p[0] - raio, p[1] - raio, raio * 2, raio * 2);
            } else {
                g.setColor(new Color(0x5A3A10));
                g.fillOval(p[0] - raio, p[1] - raio, raio * 2, raio * 2);
            }
        }
    }

    /**
     * Um rolo: fita creme com os simbolos descendo. Rapido, cada simbolo deixa
     * fantasmas acima de si (borrao de movimento); travado, e nitido. As pontas
     * da janela escurecem, como o tambor curvo de uma maquina de verdade.
     */
    private void desenharRolo(Graphics2D g, List<Premio> rolo, int x, int y, int cw, int ch,
                              int janelaH, double giro, double velocidade, int s,
                              boolean premiado, double quadro) {
        tambor(cw, janelaH);
        g.drawImage(tamborFundo, x, y, null);

        int centro = y + janelaH / 2;
        int celula = Math.max(1, 3 * s);
        int icone = Icones.lado(celula);
        int primeiro = (int) Math.floor(giro) - 2;
        Composite antes = g.getComposite();
        for (int k = primeiro; k <= primeiro + 5; k++) {
            if (k < 0 || k >= rolo.size()) {
                continue;
            }
            Premio p = rolo.get(k);
            // Descendo: o simbolo k esta (giro - k) celulas abaixo do centro.
            int sy = (int) (centro + (giro - k) * ch);
            int ix = x + (cw - icone) / 2;
            int iy = sy - icone / 2;
            boolean vencedora = premiado && k == VENCEDORA;
            if (vencedora) {
                double pulso = 0.5 + 0.5 * Math.sin(quadro * 0.25);
                Color c = p.cor();
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (60 + 80 * pulso)));
                g.fillRect(x, sy - ch / 2, cw, ch);
            }
            // A raridade: uma faixa vertical na borda esquerda da celula.
            g.setColor(p.cor());
            g.fillRect(x + s, sy - ch / 2 + 3 * s, 3 * s, ch - 6 * s);
            // Borrao: fantasmas acima, mais fortes e mais longos quanto mais rapido
            // o rolo. Vem pronto do cache, por faixa de velocidade (ver Icones).
            if (velocidade > 0.15) {
                int nivel = (int) Math.max(1, Math.min(Icones.NIVEIS_DE_RASTRO, Math.round(velocidade * 1.7)));
                int passo = Math.max(1, (int) Math.round(nivel / 1.7 * ch * 0.18));
                Icones.desenharComRastro(g, p.icone(), p.corDoIcone(), ix, iy, celula, nivel, passo);
            } else {
                Icones.desenhar(g, p.icone(), p.corDoIcone(), ix, iy, celula);
            }
            // Divisoria fina entre simbolos.
            g.setColor(new Color(0, 0, 0, 25));
            g.fillRect(x, sy + ch / 2 - s / 2, cw, Math.max(1, s / 2));
        }
        // Sombra curva nas pontas do tambor.
        g.drawImage(tamborSombra, x, y, null);
    }

    private java.awt.image.BufferedImage tamborFundo, tamborSombra;

    /** Fundo creme e sombra das pontas do rolo, prontos — refeitos so se o tamanho mudar. */
    private void tambor(int cw, int janelaH) {
        if (tamborFundo != null && tamborFundo.getWidth() == cw && tamborFundo.getHeight() == janelaH) {
            return;
        }
        tamborFundo = new java.awt.image.BufferedImage(cw, janelaH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gf = tamborFundo.createGraphics();
        gf.setPaint(new GradientPaint(0, 0, new Color(0xE9DFCB), cw, 0, new Color(0xFFF8EA)));
        gf.fillRect(0, 0, cw, janelaH);
        gf.dispose();
        tamborSombra = new java.awt.image.BufferedImage(cw, janelaH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gs = tamborSombra.createGraphics();
        int faixa = janelaH / 3;
        gs.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 150), 0, faixa, new Color(0, 0, 0, 0)));
        gs.fillRect(0, 0, cw, faixa);
        gs.setPaint(new GradientPaint(0, janelaH - faixa, new Color(0, 0, 0, 0), 0, janelaH,
                new Color(0, 0, 0, 150)));
        gs.fillRect(0, janelaH - faixa, cw, faixa);
        gs.dispose();
    }

    /** A linha de pagamento atravessando o meio, com setas nas pontas. */
    private void desenharLinhaDePagamento(Graphics2D g, int s, int x, int y, int larg, double quadro) {
        Color c = revelada ? NEON[(int) (quadro / 4) % NEON.length] : new Color(0xE0233A);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
        g.fillRect(x - 6 * s, y - s / 2, larg + 12 * s, Math.max(1, s));
        int seta = 6 * s;
        g.setColor(c);
        g.fillPolygon(new int[]{x - 10 * s, x - 10 * s + seta, x - 10 * s},
                new int[]{y - seta / 2 - s, y, y + seta / 2 + s}, 3);
        int dx = x + larg + 10 * s;
        g.fillPolygon(new int[]{dx, dx - seta, dx}, new int[]{y - seta / 2 - s, y, y + seta / 2 + s}, 3);
    }

    /**
     * O cartao do premio, saltando na frente da maquina depois do jackpot: os
     * rolos so mostram o simbolo, e e aqui que o jogador le o que ganhou.
     */
    private void desenharPremio(Graphics2D g, int s, int cx, int cy, double quadro) {
        // Espera ~0,6 s: primeiro o jogador VE os tres simbolos iguais piscando na
        // linha — esse e o jackpot. So entao o cartao salta para dizer o que e.
        double t = Math.min(1, (desdeARevelacao + Engine.alpha() - ESPERA_DO_CARTAO) / 14.0);
        if (t <= 0) {
            return;
        }
        double c1 = 1.7, c3 = c1 + 1;
        double escala = 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);   // salta e assenta
        int cw = 150 * s, ch = 64 * s;
        java.awt.geom.AffineTransform antes = g.getTransform();
        g.translate(cx, cy + 6 * s);
        g.scale(escala, escala);
        g.translate(-cx, -(cy + 6 * s));
        int x = cx - cw / 2, y = cy + 6 * s - ch / 2;
        Color cor = vencedor.cor();

        // Halo pulsante na cor do premio.
        double pulso = 0.5 + 0.5 * Math.sin(quadro * 0.2);
        for (int k = 3; k >= 1; k--) {
            g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), (int) ((50 + 50 * pulso) / k)));
            g.fillRoundRect(x - 3 * k * s, y - 3 * k * s, cw + 6 * k * s, ch + 6 * k * s, 12 * s, 12 * s);
        }
        g.setColor(new Color(0x14081C));
        g.fillRoundRect(x - 2 * s, y - 2 * s, cw + 4 * s, ch + 4 * s, 10 * s, 10 * s);
        // Fundo ESCURO. Era cinza-azulado claro, e o nome branco por cima sumia —
        // branco no quase-branco. Texto claro pede fundo escuro.
        g.setPaint(new GradientPaint(0, y, new Color(0x2E2546), 0, y + ch, new Color(0x15101F)));
        g.fillRoundRect(x, y, cw, ch, 8 * s, 8 * s);
        g.setColor(cor);
        g.fillRoundRect(x, y, cw, 12 * s, 8 * s, 8 * s);
        g.fillRect(x, y + 6 * s, cw, 6 * s);

        Font rotulo = FontHandler.font(FontHandler.Septem, 6f * s);
        texto(g, vencedor.rotulo(), rotulo, cx, y + 9 * s, Palette.DARKEST, false);

        // Icone grande a esquerda, texto a direita.
        int celula = 4 * s;
        int lado = Icones.lado(celula);
        int ix = x + 8 * s, iy = y + 14 * s + (ch - 14 * s - lado) / 2;
        g.setColor(new Color(0xF4EBDD));
        g.fillRoundRect(ix - 2 * s, iy - 2 * s, lado + 4 * s, lado + 4 * s, 6 * s, 6 * s);
        Icones.desenhar(g, vencedor.icone(), vencedor.corDoIcone(), ix, iy, celula);

        int tx = ix + lado + 8 * s;
        int larguraTexto = x + cw - 6 * s - tx;
        Font nome = FontHandler.font(FontHandler.Game, 10f * s);
        Font efeito = FontHandler.font(FontHandler.Septem, 7f * s);
        int ly = y + 28 * s;
        for (String linha : quebrar(vencedor.nome(), nome, larguraTexto)) {
            contornado(g, linha, nome, tx, ly, Color.WHITE, s);
            ly += 12 * s;
        }
        ly += 2 * s;
        // O efeito na cor do simbolo, clareada: e o numero que importa, e ele
        // tem de ler tao bem quanto o nome.
        Color corEfeito = clarear(vencedor.corDoIcone());
        for (String linha : quebrar(vencedor.efeito(), efeito, larguraTexto)) {
            contornado(g, linha, efeito, tx, ly, corEfeito, s);
            ly += 9 * s;
        }
        g.setTransform(antes);

        if (desdeARevelacao > ESPERA_DO_CARTAO + 12) {
            Font dica = FontHandler.font(FontHandler.Septem, 7f * s);
            double pisca = 0.6 + 0.4 * Math.sin(quadro * 0.15);
            Color cDica = new Color(255, 255, 255, (int) (255 * pisca));
            String dicaTexto = "Click or press Enter to collect";
            int dw = FontHandler.getWidth(dicaTexto, dica) + 16 * s;
            int dy = cy + 6 * s + ch / 2 + 16 * s;
            // Faixa escura atras: a dica fica por cima dos rolos claros.
            g.setColor(new Color(10, 6, 16, 210));
            g.fillRoundRect(cx - dw / 2, dy - 10 * s, dw, 14 * s, 8 * s, 8 * s);
            texto(g, dicaTexto, dica, cx, dy, cDica, false);
        }
    }

    /** Raios de cores girando atras da maquina, so no jackpot. Discretos. */
    private void desenharRaios(Graphics2D g, int cx, int cy, int raio, double quadro) {
        int raios = 12;
        double rot = quadro * 0.012;
        for (int i = 0; i < raios; i++) {
            double a0 = rot + i * Math.PI * 2 / raios;
            double a1 = a0 + Math.PI / raios;
            Color c = NEON[i % NEON.length];
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 22));
            g.fillPolygon(new int[]{cx, cx + (int) (Math.cos(a0) * raio), cx + (int) (Math.cos(a1) * raio)},
                    new int[]{cy, cy + (int) (Math.sin(a0) * raio), cy + (int) (Math.sin(a1) * raio)}, 3);
        }
    }

    private void desenharMoedas(Graphics2D g, int s, float a) {
        int r = 5 * s;
        for (Moeda m : moedas) {
            int x = (int) (m.x + m.vx * a);
            int y = (int) (m.y + m.vy * a);
            desenharMoeda(g, x, y, r, Math.cos(m.giro + m.velGiro * a), s, 1f);
        }
    }

    private static List<String> quebrar(String texto, Font fonte, int largura) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (FontHandler.getWidth(tentativa, fonte) > largura && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(tentativa);
            }
        }
        if (!atual.isEmpty()) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    /**
     * Texto com contorno escuro nas quatro direcoes e sombra embaixo: legivel em
     * qualquer fundo, que e o que a fonte de pixel fina precisa.
     */
    private static void contornado(Graphics2D g, String texto, Font fonte, int x, int y,
                                   Color cor, int s) {
        TextFx.draw(g, texto, fonte, x, y, cor, s);
    }

    private static Color clarear(Color c) {
        return new Color(Math.min(255, c.getRed() + 90), Math.min(255, c.getGreen() + 90),
                Math.min(255, c.getBlue() + 90));
    }

    /** Texto com sombra; centrado em x, ou a partir de x. */
    private static void texto(Graphics2D g, String texto, Font fonte, int x, int y, Color cor,
                              boolean aEsquerda) {
        int px = aEsquerda ? x : x - FontHandler.getWidth(texto, fonte) / 2;
        TextFx.draw(g, texto, fonte, px, y, cor, Configs.UiScale());
    }

    @Override
    public void dispose() {
    }
}
