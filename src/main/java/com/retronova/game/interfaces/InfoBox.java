package com.retronova.game.interfaces;

import com.retronova.engine.graphics.Palette;
import com.retronova.engine.Configs;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.game.items.Item;

import java.awt.*;

public class InfoBox {

    private String[] values;
    private Rectangle bounds;
    private final int padding;

    private final Font fontTitle, fontSpecs;

    public InfoBox() {
        this.padding = Configs.HudScale() * 5;
        this.fontTitle = FontHandler.font(FontHandler.Septem,Configs.HudScale() * 10);
        this.fontSpecs = FontHandler.font(FontHandler.Septem,Configs.HudScale() * 8);
    }

    /**
     * O item mostrado agora. Guardado inteiro, e nao so os textos dele.
     *
     * A versao anterior achatava tudo em um vetor de String logo na entrada, e por
     * isso a ficha nao tinha como saber a raridade nem o elemento na hora de
     * desenhar — informacao que existe no item e que se perdia no caminho.
     */
    private Item item;

    public void setValues(Item item) {
        if (item == null) {
            return;
        }
        if (this.item == item) {
            return;
        }
        this.item = item;

        String[] values = new String[item.getSpecifications().length + 1];
        values[0] = item.getName();
        for (int j = 1; j < values.length; j++) {
            values[j] = item.getSpecifications()[j - 1];
        }

        // O SELO ENTRA NA CONTA DA LARGURA.
        //
        // Ele e desenhado a direita do titulo, com o tamanho da altura do titulo,
        // mas a largura da caixa so somava o texto — entao o carimbo saia pela
        // borda direita e ficava boiando sobre o jogo. Era o "conteudo saindo da
        // area": nao era o texto, era o selo que a caixa nao sabia que tinha.
        int selo = FontHandler.getHeight(values[0], fontTitle);
        int largura = padding * 2 + FontHandler.getWidth(values[0], fontTitle)
                + padding + selo;
        int altura = padding * 2 + FontHandler.getHeight(values[0], fontTitle);
        // A LINHA DE CATEGORIA entra no calculo: ela e uma linha de texto como as
        // outras, e esquecer dela deixava a caixa curta e o rodape vazando.
        altura += FontHandler.getHeight("Ay", fontSpecs) + padding;
        largura = Math.max(largura, padding * 2 + FontHandler.getWidth(categoria(item), fontSpecs));
        for (int i = 1; i < values.length; i++) {
            largura = Math.max(largura, padding * 2 + FontHandler.getWidth(values[i], fontSpecs));
            altura += FontHandler.getHeight(values[i], fontSpecs) + padding;
        }
        this.bounds = new Rectangle(largura, altura);
        this.values = values;
    }

    /**
     * A linha que resume o item: raridade e elemento.
     *
     * Fica LOGO ABAIXO DO NOME, e nao no rodape. Numa ficha que se le de relance, o
     * que decide se vale continuar lendo sao essas duas palavras — deixa-las por
     * ultimo obriga o olho a percorrer a lista inteira para descobrir se aquilo
     * interessava.
     */
    private static String categoria(Item item) {
        String r = item.raridade().rotulo();
        com.retronova.game.items.Elemento e = item.elemento();
        // A LENDARIA NAO ENTRA AQUI. Ela e uma raridade morando no enum de elemento
        // por conveniencia de sufixo de arquivo, entao imprimi-la dos dois lados
        // dava "Legendary - Legendary".
        if (e == com.retronova.game.items.Elemento.NENHUM
                || e == com.retronova.game.items.Elemento.LENDARIA) {
            return r;
        }
        // O SEPARADOR E UM HIFEN, e nao o ponto do meio. A fonte do jogo nao tem o
        // glifo do ponto medio: no lugar dele saia o retangulo vazio que aparece
        // quando falta um caractere, bem no meio da unica linha de classificacao.
        return r + " - " + e.rotulo();
    }

    public boolean isEmpty() {
        return this.values == null || this.values.length == 0;
    }

    public void clean() {
        this.values = null;
        this.item = null;
    }

    /**
     * A ficha, no formato que os RPG de item usam ha muito tempo.
     *
     * TRES DECISOES, e todas vem do mesmo lugar: a ficha e lida em um segundo, com
     * o mouse parado, e tudo o que atrapalhar essa leitura e enfeite.
     *
     *  - O NOME TEM A COR DA RARIDADE. E o primeiro pixel colorido que o olho
     *    encontra, e ele ja responde "isto vale a pena?" antes de qualquer palavra
     *    ser lida. Era branco para tudo, entao a resposta so vinha depois da lista.
     *
     *  - UMA FAIXA DA MESMA COR no topo da caixa. O nome sozinho e pouca tinta para
     *    ser visto pelo canto do olho enquanto se percorre um inventario cheio.
     *
     *  - OS SELOS APARECEM AQUI TAMBEM, ao lado do nome. Sao os mesmos do slot e da
     *    carta: quem aprendeu o losango roxo no inventario nao precisa reaprender
     *    nada na loja.
     */
    public void render(Graphics2D g) {
        if (isEmpty()) {
            return;
        }
        int esc = Configs.HudScale();
        // A CAIXA NAO PASSA DA JANELA.
        //
        // Ela nascia sempre em baixo e a direita do ponteiro, o que funciona no meio
        // da tela e falha nas bordas — e a bolsa ocupa justamente a metade direita
        // do painel, entao a ficha dos itens mais usados era a que mais vazava. Nao
        // ha decisao nova aqui: ela continua indo para baixo e para a direita, e so
        // recua o quanto for preciso para caber.
        int x = Mouse.getX() + 16;
        int y = Mouse.getY() + 16;
        int larguraDaJanela = com.retronova.engine.Engine.window.getWidth();
        int alturaDaJanela = com.retronova.engine.Engine.window.getHeight();
        x = Math.max(esc, Math.min(x, larguraDaJanela - this.bounds.width - esc));
        y = Math.max(esc, Math.min(y, alturaDaJanela - this.bounds.height - esc));
        Color daRaridade = item == null ? Palette.TEXT : item.raridade().cor();

        g.setColor(Palette.MAIN);
        g.fillRect(x, y, this.bounds.width, this.bounds.height);
        g.setColor(daRaridade);
        g.fillRect(x, y, this.bounds.width, esc * 2);
        g.setColor(Palette.DEEP);
        g.setStroke(new BasicStroke(esc * 2));
        g.drawRect(x, y, this.bounds.width, this.bounds.height);

        int linha = y + padding + FontHandler.getHeight(values[0], fontTitle);
        g.setFont(fontTitle);
        g.setColor(Color.black);
        g.drawString(values[0], x + padding + esc, linha + esc);
        g.setColor(daRaridade);
        g.drawString(values[0], x + padding, linha);

        if (item != null) {
            int lado = FontHandler.getHeight(values[0], fontTitle);
            com.retronova.game.items.Selos.desenhar(g, item,
                    x + padding + FontHandler.getWidth(values[0], fontTitle) + padding,
                    linha - lado, lado);
        }

        g.setFont(fontSpecs);
        linha += FontHandler.getHeight("Ay", fontSpecs) + padding;
        if (item != null) {
            String cat = categoria(item);
            g.setColor(Color.black);
            g.drawString(cat, x + padding + esc, linha + esc);
            g.setColor(Palette.LIGHT);
            g.drawString(cat, x + padding, linha);
        }

        for (int i = 1; i < values.length; i++) {
            linha += FontHandler.getHeight(values[i], fontSpecs) + padding;
            g.setColor(Color.black);
            g.drawString(values[i], x + padding + esc, linha + esc);
            g.setColor(Palette.TEXT);
            g.drawString(values[i], x + padding, linha);
        }
    }

}
