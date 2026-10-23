package com.retronova.game.map.arena;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.graphics.TextFx;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.items.RecompensaDoBau;
import com.retronova.game.items.RecompensaDoBau.Premio;
import com.retronova.menus.shared.Realce;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O baú aberto na tela: o jogador clica, o baú treme, a tampa abre e o prêmio sobe
 * de dentro. Substitui o caça-níquel (Roleta), que fica desligado no código.
 *
 * ---------------------------------------------------------------------------
 * POR QUE UM BAÚ E NÃO MAIS A ROLETA
 *
 * A roleta girava três rolos e sempre parava nos três iguais: o prêmio era
 * decidido antes do giro (ver RecompensaDoBau), e os rolos eram só cerimônia.
 * Uma máquina que nunca erra passa a ideia de sorte e entrega sempre o mesmo
 * resultado — o jogador aprende que ela mente. Um baú que se abre e dá um item é
 * honesto com o que acontece, e é mais curto. Medido no modo foto, da tela
 * aberta até liberar a coleta: a roleta levava 3,28 s (1,18 s pulando); o baú,
 * 0,85 s depois do clique de abrir (0,65 s pulando, contando a entrada).
 *
 * ---------------------------------------------------------------------------
 * AS FASES, em ticks de 60 Hz (o desenho interpola entre eles com Engine.alpha,
 * então a animação corre igual a 60 ou a 240 quadros por segundo)
 *
 *   ENTRADA  o véu sobe e o baú cai no meio da tela.
 *   ESPERA   parado, respirando, até o clique (ou Enter, Espaço, A). Embaixo, a
 *            PRÉVIA do que pode sair: os ícones do sorteio passam um a um. Não
 *            atrasa nada — o jogador está escolhendo a hora de abrir.
 *   TREMOR   o baú chacoalha e acende na cor do grau do prêmio, cada vez mais:
 *            é a antecipação, e a cor já diz a qualidade, como a cor do baú no
 *            Enter the Gungeon diz antes de abrir.
 *   TAMPA    os três quadros da tampa abrindo, um clarão curto e as faíscas.
 *   SUBIDA   o cartão do prêmio sobe de dentro do baú e assenta.
 *   PRONTO   o cartão parado; um clique coleta e devolve o jogo.
 *
 * Um clique durante o tremor, a tampa ou a subida PULA para o cartão, sem pular a
 * recompensa nem os sons dela. O jogo fica pausado o tempo todo, como na roleta:
 * o baú é aberto quando o jogador escolhe encostar nele, e a horda espera.
 */
public class AberturaDoBau implements Activity {

    // ----------------------------------------------------------- ajuste fino

    /** Ticks de cada fase (60 por segundo). */
    public static final int ENTRADA = 12, TREMOR = 18, TAMPA = 9, SUBIDA = 16;
    /** Ticks depois de o cartão assentar até aceitar a coleta: evita coletar no clique que pulou. */
    public static final int ATE_COLETAR = 8;
    /** Ticks que cada ícone da prévia fica aceso. */
    public static final int PREVIA_POR_ICONE = 9;
    /** Quantos ícones a prévia mostra de uma vez. */
    public static final int PREVIA_VISIVEIS = 7;
    /** O baú em pixels de arte vezes isto, por cima da escala da interface. */
    public static final int ZOOM_DO_BAU = 5;
    /** Amplitude máxima do tremor, em pixels de arte do baú. */
    public static final int TREMOR_MAXIMO = 2;
    /** Faíscas da abertura, e quantos ticks vivem. */
    public static final int FAISCAS = 46, VIDA_DA_FAISCA = 34;
    /** Ticks do clarão da tampa, e a força dele (0 a 255). */
    public static final int CLARAO = 8, FORCA_DO_CLARAO = 110;
    /**
     * O véu sobre a arena, de 0 a 255. Mais fechado que o dos menus (200): com
     * ele, o HUD aparecia por trás do baú e disputava com a prévia.
     */
    public static final int VEU = 228;

    // ----------------------------------------------------------- estado

    private enum Fase { ENTRADA, ESPERA, TREMOR, TAMPA, SUBIDA, PRONTO }

    private final Premio premio;
    private final List<Premio> previa;
    private Fase fase = Fase.ENTRADA;
    /** Ticks dentro da fase atual. */
    private int tempo;
    /** Ticks desde que a tela abriu: a respiração e a prévia correm nele. */
    private int idade;
    private boolean abriuSom, reveladoSom;
    private final Realce.Estado realce = new Realce.Estado();
    private boolean sobreOBau;

    private static BufferedImage quadros;

    private static final class Faisca {
        double x, y, vx, vy;
        int vida;
        Color cor;
    }

    private final List<Faisca> faiscas = new ArrayList<>();

    public AberturaDoBau() {
        // O PRÊMIO É DECIDIDO AQUI, ao abrir, antes de qualquer animação — a mesma
        // regra da roleta, que agora mora em RecompensaDoBau.
        this.premio = RecompensaDoBau.abrir(Game.getPlayer());
        // A PRÉVIA COM GRAU: cada melhoria do pool ganha um grau sorteado com as
        // chances de verdade (Raridade.sortear), então a fileira mostra o quanto
        // cada grau é comum — e o tic de cada ícone soa no grau dele.
        double promocao = RecompensaDoBau.promocao(Game.getPlayer());
        this.previa = new ArrayList<>();
        for (Premio p : RecompensaDoBau.possiveis(Game.getPlayer().getMelhorias())) {
            previa.add(p.carta() == null ? p : new Premio(new com.retronova.game.items.Melhorias.Carta(
                    p.carta().melhoria(), com.retronova.game.items.Raridade.sortear(promocao),
                    p.carta().jaTinha()), null, 0));
        }
        java.util.Collections.shuffle(previa, Engine.RAND);
        Sound.play(Sounds.BauAparece);
        if (quadros == null) {
            quadros = new SpriteHandler("sprites/objects/furniture", "bau_abrindo", 1).getSHEET();
        }
    }

    // ----------------------------------------------------------- tick

    @Override
    public void tick() {
        idade++;
        tempo++;
        tickFaiscas();
        boolean confirmar = KeyBoard.KeyPressed("Enter") || KeyBoard.KeyPressed("Space")
                || KeyBoard.KeyPressed("Interact")
                || Mouse.clickOn(Mouse_Button.LEFT, new Rectangle(0, 0, Engine.window.largura(), Engine.window.altura()));

        boolean sobre = (fase == Fase.ESPERA || fase == Fase.PRONTO)
                && caixaInterativa().contains(Mouse.getX(), Mouse.getY());
        if (sobre && !sobreOBau && Entrada.porMouse()) {
            Sound.play(Sounds.Hover);
        }
        sobreOBau = sobre;
        if (sobre) {
            Engine.window.pointing();
        }
        realce.tick(sobre || ((fase == Fase.ESPERA || fase == Fase.PRONTO) && Entrada.porFoco()));

        switch (fase) {
            case ENTRADA -> {
                if (tempo >= ENTRADA) {
                    mudar(Fase.ESPERA);
                }
            }
            case ESPERA -> {
                // O tic da prévia, a cada ícone que acende, no grau dele.
                if (!previa.isEmpty() && idade % PREVIA_POR_ICONE == 0) {
                    Sound.play(switch (previa.get(iconeAceso()).grau()) {
                        case COMUM -> Sounds.BauPreviaComum;
                        case RARO -> Sounds.BauPreviaRaro;
                        case EPICO -> Sounds.BauPreviaEpico;
                        case LENDARIO -> Sounds.BauPreviaLendario;
                    });
                }
                if (confirmar) {
                    Sound.play(Sounds.BauTremor);
                    mudar(Fase.TREMOR);
                }
            }
            case TREMOR -> {
                if (confirmar) {
                    pular();
                } else if (tempo >= TREMOR) {
                    abrirTampa();
                    mudar(Fase.TAMPA);
                }
            }
            case TAMPA -> {
                if (confirmar) {
                    pular();
                } else if (tempo >= TAMPA) {
                    revelar();
                    mudar(Fase.SUBIDA);
                }
            }
            case SUBIDA -> {
                if (confirmar) {
                    pular();
                } else if (tempo >= SUBIDA) {
                    mudar(Fase.PRONTO);
                }
            }
            case PRONTO -> {
                if (confirmar && tempo >= ATE_COLETAR) {
                    RecompensaDoBau.entregar(Game.getPlayer(), premio);
                    Sound.play(Sounds.Button);
                    Engine.pause(null);
                }
            }
        }
    }

    /** O ícone da prévia aceso agora: anda um a cada PREVIA_POR_ICONE ticks. */
    private int iconeAceso() {
        return (idade / PREVIA_POR_ICONE) % previa.size();
    }

    private void mudar(Fase nova) {
        fase = nova;
        tempo = 0;
    }

    /** Vai direto ao cartão, sem perder o som nem a explosão que ainda não vieram. */
    private void pular() {
        abrirTampa();
        revelar();
        mudar(Fase.PRONTO);
    }

    private void abrirTampa() {
        if (abriuSom) {
            return;
        }
        abriuSom = true;
        Sound.play(Sounds.BauAbre);
        espalharFaiscas();
    }

    private void revelar() {
        if (reveladoSom) {
            return;
        }
        reveladoSom = true;
        Sound.play(switch (premio.grau()) {
            case COMUM -> Sounds.BauPremioComum;
            case RARO -> Sounds.BauPremioRaro;
            case EPICO -> Sounds.BauPremioEpico;
            case LENDARIO -> Sounds.BauPremioLendario;
        });
    }

    private void espalharFaiscas() {
        int s = escala();
        Rectangle b = caixaDoBau(s);
        double cx = b.getCenterX(), cy = b.y + b.height * 0.35;
        Color cor = premio.cor();
        for (int i = 0; i < FAISCAS; i++) {
            Faisca f = new Faisca();
            double ang = -Math.PI / 2 + (Engine.RAND.nextDouble() - 0.5) * 2.4;
            double v = (2 + Engine.RAND.nextDouble() * 4) * s;
            f.x = cx + (Engine.RAND.nextDouble() - 0.5) * b.width * 0.5;
            f.y = cy;
            f.vx = Math.cos(ang) * v;
            f.vy = Math.sin(ang) * v;
            f.vida = VIDA_DA_FAISCA - Engine.RAND.nextInt(10);
            f.cor = i % 3 == 0 ? Palette.TEXT : cor;
            faiscas.add(f);
        }
    }

    private void tickFaiscas() {
        int s = escala();
        for (int i = faiscas.size() - 1; i >= 0; i--) {
            Faisca f = faiscas.get(i);
            f.x += f.vx;
            f.y += f.vy;
            f.vy += 0.18 * s;
            f.vx *= 0.97;
            if (--f.vida <= 0) {
                faiscas.remove(i);
            }
        }
    }

    // ----------------------------------------------------------- medidas

    /** A escala da interface, reduzida se a tela não comportar o baú e o cartão. */
    private static int escala() {
        int cabe = Math.max(1, (Engine.window.altura() - 20) / 190);
        return Math.max(1, Math.min(Configs.UiScale(), cabe));
    }

    private static Rectangle caixaDoBau(int s) {
        int lado = 16 * ZOOM_DO_BAU * s;
        int w = Engine.window.largura(), h = Engine.window.altura();
        // Um pouco abaixo do meio: em cima fica o cartão que sobe dele, embaixo a
        // prévia e a dica.
        return new Rectangle(w / 2 - lado / 2, h / 2 - lado / 2 + 12 * s, lado, lado);
    }

    private static Rectangle caixaDoCartao(int s) {
        int cw = 150 * s, ch = 52 * s;
        Rectangle b = caixaDoBau(s);
        return new Rectangle(b.x + b.width / 2 - cw / 2, b.y - ch - 6 * s, cw, ch);
    }

    /** O que o mouse aponta: o baú esperando o clique, ou o cartão esperando a coleta. */
    private Rectangle caixaInterativa() {
        int s = escala();
        return fase == Fase.PRONTO ? caixaDoCartao(s) : caixaDoBau(s);
    }

    // ----------------------------------------------------------- desenho

    @Override
    public void render(Graphics2D g) {
        int s = escala();
        int w = Engine.window.largura(), h = Engine.window.altura();
        float a = Engine.alpha();
        double t = tempo + a;

        float entrada = fase == Fase.ENTRADA ? (float) Math.min(1, t / ENTRADA) : 1f;
        g.setColor(new Color(Palette.VEIL.getRed(), Palette.VEIL.getGreen(), Palette.VEIL.getBlue(),
                (int) (VEU * entrada)));
        g.fillRect(0, 0, w, h);

        Rectangle bau = caixaDoBau(s);
        boolean aberto = fase == Fase.SUBIDA || fase == Fase.PRONTO;

        // A LUZ DO GRAU: acende no tremor e fica depois de aberto.
        float luz = switch (fase) {
            case TREMOR -> (float) Math.min(1, t / TREMOR) * 0.7f;
            case TAMPA, SUBIDA, PRONTO -> 1f;
            default -> 0f;
        };
        if (luz > 0) {
            if (aberto) {
                desenharRaios(g, (int) bau.getCenterX(), bau.y + bau.height / 3, Math.max(w, h) / 2,
                        idade + a, premio.cor());
            }
            int raio = (int) (bau.width * (1.1 + 0.4 * luz));
            desenharBrilho(g, (int) bau.getCenterX(), (int) bau.getCenterY(), raio, premio.cor(), luz);
        }

        // O BAÚ: cai na entrada, respira na espera, chacoalha no tremor.
        int dx = 0, dy = 0;
        int px = ZOOM_DO_BAU * s;   // um pixel de arte do baú, na tela
        switch (fase) {
            case ENTRADA -> dy = (int) (-(1 - suave(entrada)) * 40 * s);
            case ESPERA -> dy = (int) Math.round(Math.sin((idade + a) * 0.08) * 0.6) * px / 2;
            case TREMOR -> {
                double forca = TREMOR_MAXIMO * (0.4 + 0.6 * t / TREMOR);
                dx = (int) Math.round(Math.sin(t * 2.7) * forca) * px / 2;
                dy = -(int) Math.round(Math.abs(Math.sin(t * 1.9)) * forca * 0.5) * px / 2;
            }
            case TAMPA -> dy = -(int) Math.round(Math.sin(Math.PI * t / TAMPA) * 2) * px / 2;
            default -> { }
        }
        Rectangle desenho = new Rectangle(bau.x + dx, bau.y + dy, bau.width, bau.height);
        float hover = realce.noQuadro();
        // O realce comum do mouse e do foco, sem a cor do prêmio: ela só aparece no
        // tremor, que é a antecipação.
        if (fase == Fase.ESPERA && hover > 0) {
            Realce.brilho(g, desenho, hover, s);
        }
        int quadro = switch (fase) {
            case TAMPA -> Math.min(2, (int) (t / (TAMPA / 3.0)));
            case SUBIDA, PRONTO -> 2;
            default -> 0;
        };
        Graphics2D gb = (Graphics2D) g.create();
        gb.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gb.drawImage(quadros, desenho.x, desenho.y, desenho.x + desenho.width, desenho.y + desenho.height,
                quadro * 16, 0, quadro * 16 + 16, 16, null);
        gb.dispose();
        if (fase == Fase.ESPERA && Entrada.porFoco()) {
            Realce.marcadorDeFoco(g, desenho, s);
        }

        desenharFaiscas(g, s, a);

        if (fase == Fase.ESPERA || fase == Fase.ENTRADA) {
            desenharPrevia(g, s, bau, entrada);
            dica(g, s, "Click or press Enter to open", bau.y + bau.height + 44 * s, entrada);
        }
        if (aberto) {
            float subida = fase == Fase.SUBIDA ? (float) Math.min(1, t / SUBIDA) : 1f;
            desenharCartao(g, s, subida, hover);
            if (fase == Fase.PRONTO && tempo >= ATE_COLETAR) {
                dica(g, s, "Click or press Enter to collect", bau.y + bau.height + 16 * s, 1f);
            }
        }

        // Clarão curto na tampa: frações de segundo, e só até 110 de alfa.
        double desdeATampa = fase == Fase.TAMPA ? t : fase == Fase.SUBIDA ? TAMPA + t : Double.MAX_VALUE;
        if (desdeATampa < CLARAO) {
            int alfa = (int) (FORCA_DO_CLARAO * (1 - desdeATampa / CLARAO));
            g.setColor(new Color(255, 255, 255, Math.max(0, alfa)));
            g.fillRect(0, 0, w, h);
        }
    }

    /** Ease-out cúbico. */
    private static double suave(double x) {
        return 1 - Math.pow(1 - x, 3);
    }

    /**
     * A PRÉVIA DO QUE PODE SAIR: uma fileira de ícones do sorteio, com um aceso
     * que passa de um em um. Diz "isto é um baú de armas e melhorias" sem atrasar
     * nada — corre enquanto o jogador decide abrir.
     */
    private void desenharPrevia(Graphics2D g, int s, Rectangle bau, float entrada) {
        if (previa.isEmpty()) {
            return;
        }
        int escala = Math.max(1, s);
        int lado = com.retronova.game.items.Icone.lado(escala);
        int vao = 4 * s;
        int n = Math.min(PREVIA_VISIVEIS, previa.size());
        int total = n * lado + (n - 1) * vao;
        int x0 = (int) bau.getCenterX() - total / 2;
        int y = bau.y + bau.height + 10 * s;
        int aceso = iconeAceso();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, entrada));
        Font rotulo = FontHandler.font(FontHandler.Game, 5f * s);
        String texto = "Could contain";
        TextFx.draw(g2, texto, rotulo, (int) bau.getCenterX() - FontHandler.getWidth(texto, rotulo) / 2,
                y - 2 * s, Palette.LIGHT, s);
        for (int i = 0; i < n; i++) {
            // A janela de ícones anda junto com o aceso: todos passam pela fileira.
            int indice = (aceso - n / 2 + i + previa.size()) % previa.size();
            Premio p = previa.get(indice);
            int x = x0 + i * (lado + vao);
            boolean destaque = indice == aceso;
            // A moldura do aceso na cor do grau dele; os outros, no contorno da casa.
            g2.setColor(destaque ? p.grau().cor() : Palette.OUTLINE);
            g2.fillRect(x - s, y + s, lado + 2 * s, lado + 2 * s);
            g2.setColor(destaque ? Palette.MAIN : Palette.DEEP);
            g2.fillRect(x, y + 2 * s, lado, lado);
            // Fora do destaque, o ícone apagado nos cinzas da interface: a forma lê,
            // a cor fica para o que está aceso.
            if (destaque) {
                p.icone().desenhar(g2, x, y + 2 * s, escala);
            } else {
                p.icone().desenharApagado(g2, x, y + 2 * s, escala);
            }
        }
        g2.dispose();
    }

    /**
     * O CARTÃO DO PRÊMIO, no painel da casa (contorno, corpo fundo e a faixa do
     * grau no topo, na cor dele): ícone à esquerda, grau, nome e efeito.
     */
    private void desenharCartao(Graphics2D g, int s, float subida, float hover) {
        Rectangle fim = caixaDoCartao(s);
        Rectangle bau = caixaDoBau(s);
        double c1 = 1.4, c3 = c1 + 1, x = subida;
        double p = 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);   // sobe e assenta (ease-out-back)
        int inicioY = bau.y + bau.height / 3;
        int y = (int) Math.round(inicioY + (fim.y - inicioY) * p);
        Rectangle r = new Rectangle(fim.x, y, fim.width, fim.height);
        Color cor = premio.cor();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, subida * 2.5)));
        Realce.brilho(g2, r, Math.max(0.5f + 0.5f * premio.grau().brilho(), hover), s, cor);
        g2.setColor(Palette.OUTLINE);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(Palette.DEEP);
        g2.fillRect(r.x + s, r.y + s, r.width - 2 * s, r.height - 2 * s);
        g2.setColor(cor);
        g2.fillRect(r.x + s, r.y + s, r.width - 2 * s, 10 * s);
        g2.setColor(Palette.OUTLINE);
        g2.fillRect(r.x + s, r.y + 11 * s, r.width - 2 * s, s);

        Font grau = FontHandler.font(FontHandler.Game, 6f * s);
        String rotulo = premio.rotulo().toUpperCase();
        TextFx.draw(g2, rotulo, grau, r.x + r.width / 2 - FontHandler.getWidth(rotulo, grau) / 2,
                r.y + 9 * s, Palette.TEXT, s);

        int escala = 2 * s;
        int lado = com.retronova.game.items.Icone.lado(escala);
        int ix = r.x + 7 * s, iy = r.y + 12 * s + (r.height - 12 * s - lado) / 2;
        g2.setColor(Palette.OUTLINE);
        g2.fillRect(ix - 2 * s, iy - 2 * s, lado + 4 * s, lado + 4 * s);
        g2.setColor(Palette.MAIN);
        g2.fillRect(ix - s, iy - s, lado + 2 * s, lado + 2 * s);
        premio.icone().desenhar(g2, ix, iy, escala);

        int tx = ix + lado + 7 * s;
        int largura = r.x + r.width - 6 * s - tx;
        Font nome = FontHandler.font(FontHandler.Game, 8f * s);
        Font efeito = FontHandler.font(FontHandler.Septem, 6f * s);
        int ly = r.y + 25 * s;
        for (String linha : quebrar(premio.nome(), nome, largura)) {
            TextFx.draw(g2, linha, nome, tx, ly, Palette.TEXT, s);
            ly += 10 * s;
        }
        for (String linha : quebrar(premio.efeito(), efeito, largura)) {
            TextFx.draw(g2, linha, efeito, tx, ly, Palette.ACCENT, s);
            ly += 8 * s;
        }
        g2.dispose();
    }

    private static void dica(Graphics2D g, int s, String texto, int y, float alfa) {
        Font f = FontHandler.font(FontHandler.Game, 6f * s);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alfa))));
        TextFx.draw(g2, texto, f, Engine.window.largura() / 2 - FontHandler.getWidth(texto, f) / 2, y,
                Palette.LIGHT, s);
        g2.dispose();
    }

    private void desenharFaiscas(Graphics2D g, int s, float a) {
        for (Faisca f : faiscas) {
            int lado = (f.vida > VIDA_DA_FAISCA / 2 ? 2 : 1) * s;
            g.setColor(f.cor);
            g.fillRect((int) (f.x + f.vx * a), (int) (f.y + f.vy * a), lado, lado);
        }
    }

    /** Raios girando devagar atrás do baú aberto, na cor do grau. Discretos. */
    private static void desenharRaios(Graphics2D g, int cx, int cy, int raio, double quadro, Color cor) {
        int raios = 10;
        double rot = quadro * 0.01;
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 26));
        for (int i = 0; i < raios; i++) {
            double a0 = rot + i * Math.PI * 2 / raios;
            double a1 = a0 + Math.PI / raios;
            g.fillPolygon(new int[]{cx, cx + (int) (Math.cos(a0) * raio), cx + (int) (Math.cos(a1) * raio)},
                    new int[]{cy, cy + (int) (Math.sin(a0) * raio), cy + (int) (Math.sin(a1) * raio)}, 3);
        }
    }

    /** O brilho redondo atrás do baú: pintado uma vez por cor numa imagem pequena e ampliado. */
    private static final Map<Integer, BufferedImage> BRILHOS = new HashMap<>();

    private static void desenharBrilho(Graphics2D g, int cx, int cy, int raio, Color cor, float forca) {
        BufferedImage img = BRILHOS.computeIfAbsent(cor.getRGB(), k -> {
            int lado = 64;
            BufferedImage b = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gb = b.createGraphics();
            gb.setPaint(new RadialGradientPaint(lado / 2f, lado / 2f, lado / 2f, new float[]{0f, 1f},
                    new Color[]{new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 150),
                            new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            gb.fillRect(0, 0, lado, lado);
            gb.dispose();
            return b;
        });
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, forca))));
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, cx - raio, cy - raio, raio * 2, raio * 2, null);
        g2.dispose();
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

    @Override
    public void dispose() {
    }
}
