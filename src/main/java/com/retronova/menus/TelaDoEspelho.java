package com.retronova.menus;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.Palette;
import com.retronova.engine.graphics.TextFx;
import com.retronova.game.dialogos.CatalogoDeDialogos;
import com.retronova.game.dialogos.Emocao;
import com.retronova.game.dialogos.Personagem;
import com.retronova.game.dialogos.Retrato;
import com.retronova.game.objects.entities.NPCs.Seller;
import com.retronova.engine.inputs.Entrada;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.espelho.Espelho;
import com.retronova.game.espelho.MelhoriaDoEspelho;
import com.retronova.game.items.Icone;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.save.Progresso;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.Realce;
import com.retronova.menus.shared.Rodape;
import com.retronova.menus.shared.TelaDeMenu;
import com.retronova.menus.shared.EntradaLateral;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * O ESPELHO DO VENDEDOR: a tela onde o gato troca moedas por melhorias permanentes.
 *
 * Uma grade de melhorias à esquerda (ícone, nome, níveis, preço) e, à direita, o
 * detalhe da que está em foco: o que ela faz, o efeito agora e no próximo nível,
 * o preço e o motivo de não dar para comprar, quando não dá. O saldo fica no
 * alto, sempre à vista. É o desenho do Espelho da Noite do Hades e da loja de
 * Power Ups do Vampire Survivors: o jogador decide olhando o próximo passo, não
 * uma tabela.
 *
 * NAVEGAÇÃO: mouse (passar escolhe, clicar compra), setas ou D-pad entre as
 * melhorias, Enter ou A compra, e descer da última linha chega ao "Refund all".
 * Esc ou B volta para a antecâmara.
 *
 * O RETORNO DA COMPRA: o som de moeda, o quadro clareando, o pip novo acendendo e
 * o preço subindo como "-120". Sem moedas: o som seco do botão, o quadro tremendo
 * e o preço piscando no vermelho de aviso. A compra vale na hora (Espelho.comprar
 * soma no gato da antecâmara, que é o da corrida que vai começar).
 */
public class TelaDoEspelho extends TelaDeMenu {

    // ------------------------------------------------------------ ajuste fino
    /** Colunas da grade e tamanho de cada quadro, em pixels nativos. */
    static final int COLUNAS = 3, QUADRO_W = 118, QUADRO_H = 42, VAO = 6;
    /** Largura do painel de detalhe e o vão até a grade. */
    static final int DETALHE_W = 196, VAO_DO_DETALHE = 10;
    /** Altura da faixa do saldo, acima da grade. */
    static final int FAIXA = 80;
    /** A mesma folha de emoções dos diálogos, em escala inteira. */
    static final int RETRATO = 64, BOCA = 6, PISCADA = 180, FALA_DURA = 180;
    /** Ticks do clarão da compra, do tremor sem moedas e do "-preço" subindo. */
    static final int CLARAO = 18, TREMOR = 10, PRECO_SOBE = 36;
    /** Lado dos pips de nível, em pixels nativos. */
    static final int PIP = 4;

    private final Player gato;
    private final List<MelhoriaDoEspelho> tabela = Espelho.tabela();
    private final List<Rectangle> quadros = new ArrayList<>();
    private final List<Realce.Estado> realces = new ArrayList<>();
    private final Button reembolso;
    private Rectangle detalhe, faixa;
    /** O quadro sendo desenhado, reaproveitado (o tremor o desloca sem mexer no layout). */
    private final Rectangle desenho = new Rectangle();

    /** Melhoria em foco (a do detalhe); {@link #NO_BOTAO} com o foco no reembolso. */
    private int foco;
    private static final int NO_BOTAO = -2;

    private int clarao, tremor, precoSubindo;
    private int ultimaComprada = -1, ultimaRecusada = -1, ultimoPreco;
    private final Personagem vendedor = CatalogoDeDialogos.get().personagem(Seller.PERSONAGEM);
    private final Retrato retratoVendedor = Retrato.de(vendedor.retrato());
    private int tempoVendedor, falaRestante;
    private String fala = CatalogoDeDialogos.get().texto("vendedor.loja.esperando");
    private Emocao emocao = Emocao.NEUTRO;
    private final EntradaLateral entradaDaLoja = EntradaLateral.daDireita();

    private void dizer(String chave, Emocao expressao) {
        fala = CatalogoDeDialogos.get().texto(chave);
        emocao = expressao;
        falaRestante = FALA_DURA;
    }

    public TelaDoEspelho(Player gato) {
        super("Merchant's Mirror");
        this.gato = gato;
        for (int i = 0; i < tabela.size(); i++) {
            quadros.add(new Rectangle());
            realces.add(new Realce.Estado());
        }
        this.reembolso = new Button(0, 0, 0, 0, "Refund all", b -> perguntarReembolso());
    }

    private String nome() {
        return gato.getName();
    }

    // ---------------------------------------------------------------- layout

    /**
     * A escala da interface, ou a maior menor que caiba: a grade e o detalhe somam
     * 572 x 266 pixels nativos, incluindo o vendedor; a 1280x720 a escala 3 não cabe. Mesmo cálculo da
     * TelaDeConquistas (regra de interface: telas que podem nao caber se ajustam).
     */
    private static int escala() {
        Rectangle area = areaDoConteudo();
        int linhas = (Espelho.tabela().size() + COLUNAS - 1) / COLUNAS;
        int largura = COLUNAS * QUADRO_W + (COLUNAS - 1) * VAO + VAO_DO_DETALHE + DETALHE_W;
        int altura = FAIXA + linhas * QUADRO_H + (linhas - 1) * VAO;
        int porLargura = (Engine.window.largura() - 20) / largura;
        int porAltura = area.height / altura;
        return Math.max(1, Math.min(Configs.UiScale(), Math.min(porLargura, porAltura)));
    }

    @Override
    protected void posicionar() {
        int s = escala();
        Rectangle area = areaDoConteudo();
        int linhas = (tabela.size() + COLUNAS - 1) / COLUNAS;
        int gradeW = (COLUNAS * QUADRO_W + (COLUNAS - 1) * VAO) * s;
        int gradeH = (linhas * QUADRO_H + (linhas - 1) * VAO) * s;
        int total = gradeW + (VAO_DO_DETALHE + DETALHE_W) * s;
        int x0 = area.x + (area.width - total) / 2;
        int altura = FAIXA * s + gradeH;
        int y0 = area.y + Math.max(0, (area.height - altura) / 2);
        faixa = new Rectangle(x0, y0, total, FAIXA * s - 4 * s);
        int topoDaGrade = y0 + FAIXA * s;
        for (int i = 0; i < tabela.size(); i++) {
            int c = i % COLUNAS, l = i / COLUNAS;
            quadros.get(i).setBounds(x0 + c * (QUADRO_W + VAO) * s, topoDaGrade + l * (QUADRO_H + VAO) * s,
                    QUADRO_W * s, QUADRO_H * s);
        }
        int bw = 78 * s, bh = 22 * s;
        detalhe = new Rectangle(x0 + gradeW + VAO_DO_DETALHE * s, topoDaGrade, DETALHE_W * s, gradeH - bh - VAO * s);
        reembolso.setBounds(new Rectangle(detalhe.x + (detalhe.width - bw) / 2, detalhe.y + detalhe.height + VAO * s,
                bw, bh));
    }

    // ---------------------------------------------------------------- tick

    @Override
    protected void atualizar() {
        entradaDaLoja.tick();
        if (entradaDaLoja.ativa()) return; // O comando que fecha a conversa não compra.
        tempoVendedor++;
        if (falaRestante > 0 && --falaRestante == 0) {
            fala = CatalogoDeDialogos.get().texto("vendedor.loja.esperando");
            emocao = Emocao.NEUTRO;
        }
        if (clarao > 0) clarao--;
        if (tremor > 0) tremor--;
        if (precoSubindo > 0) precoSubindo--;
        int sob = -1;
        if (Entrada.porMouse()) {
            for (int i = 0; i < quadros.size(); i++) {
                if (Mouse.on(quadros.get(i))) {
                    sob = i;
                    foco = i;
                    Engine.window.pointing();
                }
            }
        }
        if (sob >= 0 && Mouse.clickOn(Mouse_Button.LEFT, quadros.get(sob))) {
            comprar(sob);
        }
        navegar();
        reembolso.tick();
        if (reembolso.isHovered()) {
            foco = NO_BOTAO;
        }
        reembolso.setFocused(foco == NO_BOTAO && Entrada.porFoco());
        for (int i = 0; i < realces.size(); i++) {
            realces.get(i).tick(i == foco);
        }
    }

    private void navegar() {
        int n = tabela.size();
        if (KeyBoard.KeyPressed("Right")) {
            foco = foco == NO_BOTAO ? n - 1 : Math.min(n - 1, foco + 1);
        } else if (KeyBoard.KeyPressed("Left")) {
            foco = foco == NO_BOTAO ? n - 1 : Math.max(0, foco - 1);
        } else if (KeyBoard.KeyPressed("Down")) {
            if (foco == NO_BOTAO) {
                return;
            }
            foco = foco + COLUNAS < n ? foco + COLUNAS : NO_BOTAO;
        } else if (KeyBoard.KeyPressed("Up")) {
            foco = foco == NO_BOTAO ? n - 1 : Math.max(0, foco - COLUNAS);
        } else if (KeyBoard.KeyPressed("Enter")) {
            if (foco == NO_BOTAO) {
                reembolso.activate();
            } else {
                comprar(foco);
            }
        }
    }

    private void comprar(int i) {
        MelhoriaDoEspelho m = tabela.get(i);
        int preco = m.custo(Espelho.nivel(nome(), m));
        switch (Espelho.comprar(gato, m)) {
            case FEITA -> {
                dizer("vendedor.loja.compra", Emocao.FELIZ);
                Sound.play(Sounds.Coin);
                clarao = CLARAO;
                precoSubindo = PRECO_SOBE;
                ultimaComprada = i;
                ultimoPreco = preco;
            }
            case ERRO_AO_SALVAR -> confirmacao.perguntar("Could not save",
                    new String[]{"Your coins and upgrades were kept.", "Check disk access and try again."}, () -> {});
            case SEM_MOEDAS, NO_MAXIMO -> {
                dizer(nivelMaximo(m) ? "vendedor.loja.maximo" : "vendedor.loja.sem_moedas",
                        nivelMaximo(m) ? Emocao.MALICIOSO : Emocao.CONFUSO);
                Sound.play(Sounds.Button);
                tremor = TREMOR;
                ultimaRecusada = i;
            }
        }
    }

    private boolean nivelMaximo(MelhoriaDoEspelho m) {
        return Espelho.nivel(nome(), m) >= m.nivelMaximo();
    }

    private void perguntarReembolso() {
        int gasto = Espelho.gasto(nome());
        if (gasto <= 0) {
            Sound.play(Sounds.Button);
            return;
        }
        confirmacao.perguntar("Refund all upgrades?",
                new String[]{"Get back " + gasto + " coins and reset " + nome() + "'s upgrades.",
                        "You can buy them again at any time."},
                () -> {
                    FalhaDeSave.tentar(() -> {
                        Espelho.redistribuir(gato);
                        Sound.play(Sounds.Coin);
                        dizer("vendedor.loja.reembolso", Emocao.FELIZ);
                    });
                });
    }

    // ---------------------------------------------------------------- desenho

    @Override
    protected List<Rodape.Dica> dicas() {
        return List.of(new Rodape.Dica("Arrows", "D-pad", "Choose"),
                new Rodape.Dica("Enter", "A", "Buy"), Rodape.VOLTAR);
    }

    @Override
    protected void desenharConteudo(Graphics2D g) {
        Graphics2D conteudo = (Graphics2D) g.create();
        entradaDaLoja.aplicar(conteudo, Engine.alpha());
        int s = escala();
        desenharFaixa(conteudo, s);
        for (int i = 0; i < tabela.size(); i++) {
            desenharQuadro(conteudo, i, s);
        }
        desenharDetalhe(conteudo, s);
        reembolso.render(conteudo);
        conteudo.dispose();
    }

    /** O saldo e de quem são as melhorias. */
    private void desenharFaixa(Graphics2D g, int s) {
        TelaDeMenu.desenharPainel(g, faixa);
        Graphics2D retrato = (Graphics2D) g.create();
        retrato.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int px = faixa.x + 6 * s, py = faixa.y + 5 * s;
        retrato.setColor(Palette.OUTLINE);
        retrato.fillRect(px, py, RETRATO * s, RETRATO * s);
        int quadro = tempoVendedor % PISCADA < BOCA ? Retrato.PISCANDO
                : falaRestante > FALA_DURA / 2 && (tempoVendedor / BOCA) % 2 == 0 ? Retrato.FALANDO : Retrato.ABERTO;
        if (retratoVendedor != null) retrato.drawImage(retratoVendedor.quadro(emocao, quadro, 2 * s), px, py, null);
        retrato.dispose();
        Font grande = FontHandler.font(FontHandler.Game, 9f * s);
        Font fina = FontHandler.font(FontHandler.Septem, 7f * s);
        int tx = faixa.x + (RETRATO + 14) * s;
        TextFx.draw(g, vendedor.nome(), grande, tx, faixa.y + 19 * s, Palette.TEXT, s);
        int fy = faixa.y + 36 * s;
        int limiteFala = faixa.width - (RETRATO + 132) * s;
        for (String linha : quebrar(fala, fina, limiteFala)) {
            TextFx.draw(g, linha, fina, tx, fy, Palette.ACCENT, s);
            fy += 10 * s;
        }
        int base = faixa.y + 24 * s;
        String saldo = String.valueOf(Progresso.moedas(nome()));
        int w = FontHandler.getWidth(saldo, grande);
        int xSaldo = faixa.x + faixa.width - 8 * s - w;
        TextFx.draw(g, saldo, grande, xSaldo, base, Palette.TEXT, s);
        String dono = nome() + "'s upgrades";
        TextFx.draw(g, dono, fina, faixa.x + faixa.width - 8 * s - FontHandler.getWidth(dono, fina),
                faixa.y + 43 * s, Palette.LIGHT, s);
        Icone.MOEDA.desenhar(g, xSaldo - (Icone.LADO + 3) * s, base - Icone.LADO * s + 2 * s, s);
        // O "-preço" da última compra sobe do saldo e some.
        if (precoSubindo > 0) {
            float f = (precoSubindo - Engine.alpha()) / PRECO_SOBE;
            int sobe = Math.round((1 - f) * 12 * s);
            Graphics2D c = (Graphics2D) g.create();
            c.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                    Math.max(0f, Math.min(1f, f * 1.5f))));
            TextFx.draw(c, "-" + ultimoPreco, fina, xSaldo - (Icone.LADO + 32) * s, base - sobe, Palette.WARN, s);
            c.dispose();
        }
    }

    private void desenharQuadro(Graphics2D g, int i, int s) {
        MelhoriaDoEspelho m = tabela.get(i);
        int nivel = Espelho.nivel(nome(), m);
        boolean cheio = nivel >= m.nivelMaximo();
        int preco = m.custo(nivel);
        boolean podeComprar = !cheio && Progresso.moedas(nome()) >= preco;
        Rectangle r = desenho;
        r.setBounds(quadros.get(i));
        if (tremor > 0 && ultimaRecusada == i) {
            r.x += ((tremor / 2) % 2 == 0 ? 1 : -1) * s;
        }
        float realce = realces.get(i).noQuadro();
        if (realce > 0) {
            Realce.brilho(g, r, realce, s);
        }
        // O quadro como as linhas da casa: contorno, corpo MAIN (DEEP quando não dá
        // para comprar) e o fio claro no topo.
        g.setColor(Palette.OUTLINE);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(podeComprar || cheio ? Palette.MAIN : Palette.DEEP);
        g.fillRect(r.x + s, r.y + s, r.width - 2 * s, r.height - 2 * s);
        g.setColor(cheio ? Palette.ACCENT : podeComprar ? Palette.LIGHT : Palette.MAIN);
        g.fillRect(r.x + s, r.y + s, r.width - 2 * s, s);
        if (realce > 0) {
            Realce.clarear(g, r, realce * 0.5f);
        }
        if (clarao > 0 && ultimaComprada == i) {
            Realce.clarear(g, r, (clarao - Engine.alpha()) / CLARAO);
        }
        if (i == foco && Entrada.porFoco()) {
            Realce.marcadorDeFoco(g, r, s);
        }

        // Ícone, apagado enquanto o gato não tem nenhum nível.
        Icone icone = Icone.pelaChave(m.icone());
        int ix = r.x + 5 * s, iy = r.y + (r.height - Icone.LADO * s) / 2;
        if (nivel > 0) {
            icone.desenhar(g, ix, iy, s);
        } else {
            icone.desenharApagado(g, ix, iy, s);
        }
        int tx = ix + (Icone.LADO + 5) * s;
        Font nomeF = FontHandler.font(FontHandler.Septem, 7f * s);
        TextFx.draw(g, m.nome(), nomeF, tx, r.y + 14 * s, Palette.TEXT, s);

        // Os níveis em pips: aceso o que já foi comprado.
        for (int k = 0; k < m.nivelMaximo(); k++) {
            int px = tx + k * (PIP + 2) * s, py = r.y + 19 * s;
            g.setColor(Palette.OUTLINE);
            g.fillRect(px, py, PIP * s, PIP * s);
            if (k < nivel) {
                g.setColor(Palette.TEXT);
                g.fillRect(px + s, py + s, (PIP - 2) * s, (PIP - 2) * s);
            }
        }

        // O preço numa plaquinha escura, com a moeda: claro se dá, vermelho se não dá.
        Font precoF = FontHandler.font(FontHandler.Game, 6f * s);
        String texto = cheio ? "MAX" : String.valueOf(preco);
        int pw = FontHandler.getWidth(texto, precoF);
        int placaW = pw + (cheio ? 6 : 6 + Icone.LADO / 2 + 2) * s;
        int placaX = r.x + r.width - placaW - 4 * s, placaY = r.y + r.height - 13 * s;
        g.setColor(Palette.OUTLINE);
        g.fillRect(placaX, placaY, placaW, 10 * s);
        boolean piscando = tremor > 0 && ultimaRecusada == i && (tremor / 3) % 2 == 0;
        Color cor = cheio ? Palette.ACCENT : podeComprar ? Palette.TEXT : Palette.WARN;
        if (piscando) {
            cor = Palette.TEXT;
        }
        TextFx.draw(g, texto, precoF, placaX + 3 * s, placaY + 8 * s, cor, s);
        if (!cheio) {
            // A moeda pequena: o ícone em meio tamanho não existe em escala inteira
            // abaixo de 2, então só aparece a partir dela.
            if (s >= 2) {
                Icone.MOEDA.desenhar(g, placaX + placaW - (Icone.LADO / 2 + 2) * s, placaY + s, s / 2);
            }
        }
    }

    /** O detalhe da melhoria em foco. */
    private void desenharDetalhe(Graphics2D g, int s) {
        TelaDeMenu.desenharPainel(g, detalhe);
        int i = foco == NO_BOTAO ? -1 : foco;
        Font titulo = FontHandler.font(FontHandler.Game, 8f * s);
        Font texto = FontHandler.font(FontHandler.Septem, 7f * s);
        Font miudo = FontHandler.font(FontHandler.Septem, 6f * s);
        int x = detalhe.x + 8 * s, w = detalhe.width - 16 * s;
        int y = detalhe.y + 16 * s;
        if (i < 0) {
            TextFx.draw(g, "Refund all", titulo, x, y, Palette.TEXT, s);
            y += 14 * s;
            int gasto = Espelho.gasto(nome());
            for (String l : quebrar(gasto > 0 ? "Get back every coin spent on " + nome()
                    + "'s upgrades (" + gasto + ") and choose again. Free."
                    : "Nothing to refund yet.", texto, w)) {
                TextFx.draw(g, l, texto, x, y, Palette.ACCENT, s);
                y += 10 * s;
            }
            return;
        }
        MelhoriaDoEspelho m = tabela.get(i);
        int nivel = Espelho.nivel(nome(), m);
        boolean cheio = nivel >= m.nivelMaximo();
        Icone.pelaChave(m.icone()).desenhar(g, x, y - 12 * s, s);
        TextFx.draw(g, m.nome(), titulo, x + (Icone.LADO + 4) * s, y, Palette.TEXT, s);
        TextFx.draw(g, "Level " + nivel + " / " + m.nivelMaximo(), miudo,
                x + (Icone.LADO + 4) * s, y + 9 * s, Palette.LIGHT, s);
        y += 24 * s;
        for (String l : quebrar(m.descricao(), texto, w)) {
            TextFx.draw(g, l, texto, x, y, Palette.ACCENT, s);
            y += 10 * s;
        }
        y += 6 * s;
        TextFx.draw(g, "Now", miudo, x, y, Palette.LIGHT, s);
        TextFx.draw(g, nivel == 0 ? "-" : m.efeitoNoNivel(nivel), texto, x + 30 * s, y, Palette.TEXT, s);
        y += 12 * s;
        TextFx.draw(g, "Next", miudo, x, y, Palette.LIGHT, s);
        TextFx.draw(g, cheio ? "Max level" : m.efeitoNoNivel(nivel + 1), texto, x + 30 * s, y,
                cheio ? Palette.ACCENT : Palette.TEXT, s);
        y += 16 * s;
        if (!cheio) {
            int preco = m.custo(nivel);
            int faltam = preco - Progresso.moedas(nome());
            TextFx.draw(g, "Cost " + preco, texto, x, y, faltam > 0 ? Palette.WARN : Palette.TEXT, s);
            if (faltam > 0) {
                TextFx.draw(g, faltam + " more coins needed", miudo, x, y + 10 * s, Palette.WARN, s);
            }
        }
    }

    private static List<String> quebrar(String texto, Font fonte, int largura) {
        List<String> linhas = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String p : texto.split(" ")) {
            String t = linha.isEmpty() ? p : linha + " " + p;
            if (FontHandler.getWidth(t, fonte) > largura && !linha.isEmpty()) {
                linhas.add(linha.toString());
                linha.setLength(0);
                linha.append(p);
            } else {
                linha.setLength(0);
                linha.append(t);
            }
        }
        if (!linha.isEmpty()) {
            linhas.add(linha.toString());
        }
        return linhas;
    }

    @Override
    public void dispose() {
    }
}
