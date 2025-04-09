package com.retronova.menus;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.sound.Musics;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.entities.Player;
import com.retronova.engine.graphics.FontG;
import com.retronova.engine.graphics.SpriteSheet;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Personagens implements Activity {

    private Rectangle[] selecao;
    private Rectangle[] botoes;
    private ArrayList<BufferedImage> imagens;
    private int personagemSelecionado = -1;
    private Player[] players;
    private boolean[] quadradoClicadoDireito = {false, false, false};
    private String loreAtual = "";


    private float[] rotacao = {0, 0, 0};
    private boolean[] animando = {false, false, false};
    private final float VELOCIDADE_ROTACAO = 0.1f;


    private final Color[] coresGatos = {
            new Color(0x4A5364),
            new Color(0x4A5364),
            new Color(0x4A5364)
    };
    private final Color[] coresPersonagensSelecionados = {
            new Color(0x6B7A8F),
            new Color(0x6B7A8F),
            new Color(0x6B7A8F)
    };
    private final Color corTexto = Color.WHITE;


    private final Font fonteTitulo = FontG.font(FontG.Game, 22 * Configs.UiScale());
    private final Font fonteGatos = FontG.font(FontG.Game,7 * Configs.UiScale());
    private final Font fonteBotoes = FontG.font(FontG.Game,8 * Configs.UiScale());
    private final Font fonteInfoPersonagens = FontG.font(FontG.Game,4 * Configs.UiScale());
    private final Font fonteLore = FontG.font(FontG.Game,5 * Configs.UiScale());

    public Personagens() {
        imagens = new ArrayList<>();

        Sound.stopAll();
        Sound.play(Musics.Geral, true);

        String[] gatos = {"cinzento", "mago", "sortudo"};
        for (String nome : gatos) {
            BufferedImage imagem = new SpriteSheet("objects/player", nome, Configs.UiScale()).getSHEET();
            imagens.add(imagem);
        }

        players = new Player[3];
        for (int i = 0; i < players.length; i++) {
            players[i] = Player.TEMPLATES[i];
        }

        for (int i = 0; i < rotacao.length; i++) {
            rotacao[i] = 1;
        }
    }

    @Override
    public void tick() {
        for (int i = 0; i < selecao.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, selecao[i])) {
                Sound.play(Sounds.Cat);
                personagemSelecionado = (personagemSelecionado == i) ? -1 : i;
                if (personagemSelecionado != -1) {
                    loreAtual = getLore(personagemSelecionado);
                } else {
                    loreAtual = "";
                }
            }

            if (Mouse.clickOn(Mouse_Button.RIGHT, selecao[i])) {
                quadradoClicadoDireito[i] = !quadradoClicadoDireito[i];
                animando[i] = true;
                rotacao[i] = 0;
            }

            if (animando[i]) {
                rotacao[i] += VELOCIDADE_ROTACAO;
                if (rotacao[i] >= 1) {
                    rotacao[i] = 1;
                    animando[i] = false;
                }
            }
        }

        for (int i = 0; i < players.length; i++) {
            if (personagemSelecionado == i && !quadradoClicadoDireito[i]) {
                players[i].tick();
            }
        }

        for (int i = 0; i < botoes.length; i++) {
            if (Mouse.clickOn(Mouse_Button.LEFT, botoes[i])) {
                switch (i) {
                    case 0:
                        System.out.println("Clicou em Back");
                        Sound.play(Sounds.Button);
                        Engine.heapActivity(new Menu());
                        break;
                    case 1:
                        System.out.println("Clicou em Play");
                        if (personagemSelecionado != -1) {
                            Activity newGame = new Game(personagemSelecionado, new Room("beginning"));
                            Sound.play(Sounds.Button);
                            Engine.heapActivity(newGame, () -> {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            Sound.stop(Musics.Geral);
                        } else {
                            System.out.println("Selecione um personagem antes de jogar!");
                        }
                        break;
                    default:
                        System.out.println("Botão desconhecido");
                        break;
                }
            }
        }
    }

    private String getLore(int personagemSelecionado) {
        switch (personagemSelecionado) {
            case 0:
                return "Normal Cat: A common cat with balanced abilities.";
            case 1:
                return "Magic Cat: A magical cat with special powers.";
            case 2:
                return "Lucky Cat: A lucky cat with unique skills.";
            default:
                return "";
        }
    }

    private void desenharLore(Graphics2D g) {
        if (!loreAtual.isEmpty()) {
            g.setFont(fonteLore);
            FontMetrics fmLore = g.getFontMetrics();
            int x = (Engine.window.getWidth() - fmLore.stringWidth(loreAtual)) / 2;
            int y = Engine.window.getHeight() - 80;
            g.setColor(corTexto);
            g.drawString(loreAtual, x, y);
        }
    }

    @Override
    public void render(Graphics2D g) {
        atualizarPosicoes();
        desenharTitulo(g);
        desenharSelecao(g);
        desenharBotoes(g);
        desenharLore(g);
    }

    private void atualizarPosicoes() {
        selecao = new Rectangle[3];
        botoes = new Rectangle[2];

        int larguraTela = Engine.window.getWidth();
        int alturaTela = Engine.window.getHeight();

        selecao[1] = new Rectangle((larguraTela - 200) / 2, (alturaTela - 300) / 2, 200, 300);
        selecao[0] = new Rectangle(selecao[1].x - Configs.UiScale() - 220, (alturaTela - 300) / 2, 200, 300);
        selecao[2] = new Rectangle(selecao[1].x + Configs.UiScale() + 220, (alturaTela - 300) / 2, 200, 300);

        botoes[0] = new Rectangle(50, alturaTela - 120, 150, 50);
        botoes[1] = new Rectangle(larguraTela - 200, alturaTela - 120, 150, 50);
    }

    private void desenharTitulo(Graphics2D g) {
        g.setFont(fonteTitulo);
        g.setColor(corTexto);
        FontMetrics fmTitulo = g.getFontMetrics();
        int x = (Engine.window.getWidth() - fmTitulo.stringWidth("Choose your character")) / 2;
        int y = 50;
        g.drawString("Choose your character", x, y + fmTitulo.getAscent());
    }

    private void desenharSelecao(Graphics2D g) {
        String[] gatos = {"Normal Cat", "Magic Cat", "Lucky Cat"};
        g.setFont(fonteGatos);
        FontMetrics fmGatos = g.getFontMetrics();

        for (int i = 0; i < selecao.length; i++) {
            RoundRectangle2D arredondar = new RoundRectangle2D.Double(selecao[i].x, selecao[i].y, selecao[i].width, selecao[i].height, 15, 15);
            GradientPaint gradient = new GradientPaint(selecao[i].x, selecao[i].y, coresGatos[i].brighter(), selecao[i].x + selecao[i].width, selecao[i].y + selecao[i].height, coresGatos[i].darker());
            g.setPaint(gradient);
            g.fill(arredondar);

            g.setColor(corTexto);
            g.draw(arredondar);

            g.drawString(gatos[i], selecao[i].x + (selecao[i].width - fmGatos.stringWidth(gatos[i])) / 2, selecao[i].y - 10);

            AffineTransform originalTransform = g.getTransform();
            g.translate(selecao[i].x + selecao[i].width / 2, selecao[i].y + selecao[i].height / 2);

            float escala = quadradoClicadoDireito[i] ? (1 - rotacao[i]) : rotacao[i];
            g.scale(Math.max(escala, 0.1), 1);

            if (!quadradoClicadoDireito[i] || escala > 0.1) {
                BufferedImage imagem = players[i].getSprite();
                g.drawImage(imagem, (int) (-imagem.getWidth() * 2.5f / 2), (int) (-imagem.getHeight() * 2.5f / 2), (int) (imagem.getWidth() * 2.5f), (int) (imagem.getHeight() * 2.5f), null);
            }
            g.setTransform(originalTransform);

            if (quadradoClicadoDireito[i]) {
                g.translate(selecao[i].x + selecao[i].width / 2, selecao[i].y + selecao[i].height / 2);
                g.scale(rotacao[i], 1);

                Font fonteOriginal = g.getFont();
                g.setFont(fonteInfoPersonagens);
                FontMetrics fmInfo = g.getFontMetrics();
                String[] info = players[i].getInfo();
                int y = -selecao[i].height / 2 + 20;
                int espacamentoVertical = 2 * Configs.UiScale();
                for (String line : info) {
                    g.drawString(line, -selecao[i].width / 2 + 10, y);
                    y += fmInfo.getHeight() + espacamentoVertical;
                }
                g.setFont(fonteOriginal);
                g.setTransform(originalTransform);
            }

            if (personagemSelecionado == i) {
                g.setColor(coresPersonagensSelecionados[i]);
                g.setStroke(new BasicStroke(3.0f));
                g.draw(arredondar);
                g.setStroke(new BasicStroke(1.0f));
            }
        }
    }

    private void desenharBotoes(Graphics2D g) {
        String[] botoesNomes = {"Back", "Play"};
        g.setFont(fonteBotoes);
        FontMetrics fmBotoes = g.getFontMetrics();

        for (int i = 0; i < botoes.length; i++) {
            int larguraBotao = botoes[i].width;
            int alturaBotao = botoes[i].height;
            int x = botoes[i].x;
            int y = botoes[i].y;

            boolean selecionado = botoes[i].contains(Mouse.getX(), Mouse.getY());

            if (selecionado) {
                larguraBotao = (int) (botoes[i].width * 1.1);
                alturaBotao = (int) (botoes[i].height * 1.1);
                x = botoes[i].x - (larguraBotao - botoes[i].width) / 2;
                y = botoes[i].y - (alturaBotao - botoes[i].height) / 2;
            }

            g.setColor(new Color(0x4A5364));
            g.fillRect(x, y + alturaBotao - 5, larguraBotao, 5);

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g.setColor(new Color(0x000000));
            RoundRectangle2D shadowRect = new RoundRectangle2D.Double(x + 2, y + 2, larguraBotao, alturaBotao, 20, 20);
            g.fill(shadowRect);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            RoundRectangle2D roundedRect = new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25);
            g.setColor(new Color(0x6B7A8F));
            g.fill(roundedRect);

            GradientPaint lightTop = new GradientPaint(
                    x, y, new Color(255, 255, 255, 60),
                    x, y + alturaBotao / 2, new Color(255, 255, 255, 0)
            );
            g.setPaint(lightTop);
            g.fill(new RoundRectangle2D.Double(x, y, larguraBotao, alturaBotao, 25, 25));

            g.setColor(Color.WHITE);
            g.drawString(
                    botoesNomes[i],
                    x + (larguraBotao - fmBotoes.stringWidth(botoesNomes[i])) / 2,
                    y + (alturaBotao - fmBotoes.getHeight()) / 2 + fmBotoes.getAscent()
            );
        }
    }

    @Override
    public void dispose() {

    }
}