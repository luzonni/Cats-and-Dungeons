package com.retronova.game.hud;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.DrawString;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.game.interfaces.Slot;
import com.retronova.game.items.Consumable;
import com.retronova.game.objects.entities.Player;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.engine.graphics.UiSprite;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;

import java.awt.*;
import java.awt.image.BufferedImage;

class Hotbar {

    private BufferedImage[] sprites;
    private Rectangle[] bounds;
    private final Player player;
    private int index;
    private char[] numbers = {'1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /**
     * As orelhas de gato, desenhadas ACIMA do painel.
     *
     * Vem numa imagem propria em vez de assadas no PNG do painel: o painel tem
     * tamanho fixo e toda posicao de slot e contada a partir do canto dele, entao
     * crescer o arquivo empurraria a tela inteira. Ver tools/GenOrelhas.java.
     */
    private UiSprite orelhas;

    private Font fontStack;
    /** Menor que a da pilha: e uma legenda, nao um dado do item. */
    private Font fontIndice;
    /** Escala do HUD com que os sprites atuais foram recortados. */
    private int escalaAplicada = -1;

    public Hotbar(Player player) {
        this.player = player;
        recriar();
    }

    /**
     * Refatia a folha na escala corrente.
     *
     * Os quadros sao recortados de uma folha ja escalada, entao nao basta trocar
     * a imagem: e preciso refazer os recortes. Sem isto, mudar "HUD size" mantinha
     * a hotbar no tamanho antigo enquanto o resto da interface crescia.
     */
    private void recriar() {
        int s = Configs.HudScale();
        this.escalaAplicada = s;
        SpriteHandler sheet = new SpriteHandler("ui", "hotbar", s);
        int sheetSize = sheet.getWidth() / 16;
        this.sprites = new BufferedImage[sheetSize];
        this.orelhas = new UiSprite("ui", "ears_hotbar");
        this.fontStack = FontHandler.font(FontHandler.Septem, s * 8);
        this.fontIndice = FontHandler.font(FontHandler.Septem, s * 6f);
        for (int i = 0; i < sheetSize; i++) {
            this.sprites[i] = sheet.getSpriteWithIndex(i, 0);
        }
        this.bounds = null;      // as posicoes sao remontadas com o tamanho novo
    }

    private void refreshPositions() {
        if (escalaAplicada != Configs.HudScale()) {
            recriar();
        }
        int hotbarWidth = this.sprites[0].getWidth() * 5;
        int x = Engine.window.getWidth()/2 - hotbarWidth/2;
        int y = Engine.window.getHeight() - this.sprites[0].getHeight() - Configs.Margin();
        if(this.bounds == null) {
            this.bounds = new Rectangle[5];
            for(int i = 0; i < 5; i++) {
                this.bounds[i] = new Rectangle(x + i * sprites[0].getWidth(), y, sprites[0].getWidth(), sprites[0].getHeight());
            }
        }
        for(int i = 0; i < 5; i++) {
            this.bounds[i].setLocation(x + i * sprites[0].getWidth(), y);
        }
    }


    public void tick() {
        refreshPositions();
        Slot slot = player.getInventory().getHotbar()[getIndexHot()];
        player.getInventory().setItemHand(slot.item());
        if(!slot.isEmpty()) {
            if(KeyBoard.KeyPressed("F")) {
                if(slot.item() instanceof Consumable consumable) {
                    consumable.consume();
                    slot.take();
                }
            }
            if(KeyBoard.KeyPressed("Q")) {
                player.dropLoot(slot.take());
            }
        }
    }

    Rectangle[] getBounds() {
        return this.bounds;
    }

    private int getIndexHot() {
        int length = player.getInventory().getHotbarSize();
        int scroll = Mouse.Scroll();
        if(scroll > 0) {
            index++;
            if(index > length-1) {
                index = 0;
            }
        }else if(scroll < 0) {
            index--;
            if(index < 0) {
                index = length-1;
            }
        }
        try {
            char keyChar = KeyBoard.getKeyChar(numbers);
            int number = Integer.parseInt(String.valueOf(keyChar)) - 1;
            if(number < length)
                this.index = number;
        }catch (Exception ignore) {}
        return index;
    }

    public void render(Graphics2D g) {
        refreshPositions();
        Slot[] items = player.getInventory().getHotbar();
        int length = player.getInventory().getHotbarSize();
        int w = bounds[0].width * bounds.length;
        int ww = bounds[0].width * length;
        int difX = (w - ww)/2;
        g.drawImage(orelhas.imagem(), bounds[0].x + difX,
                // Desce uma linha de arte: a tira tem uma linha de costura no
                // pe justamente para encostar no painel, e sem isso ela ficava
                // pairando um pixel acima, com a fresta aparecendo no meio.
                bounds[0].y - orelhas.altura() + Configs.HudScale(), null);
        for(int i = 0; i < length; i++) {
            BufferedImage sprite = index == i ? sprites[1] : sprites[0];
            g.drawImage(sprite, bounds[i].x + difX, bounds[i].y, null);
            // A NUMERACAO SAIU. Ela ocupava o mesmo canto que o fundo de raridade
            // agora usa, e as duas informacoes disputavam quatro pixels. Entre
            // lembrar a tecla — que se aprende na primeira partida e nunca mais se
            // esquece — e mostrar o quanto a arma e boa, a segunda vale mais.
            if(!items[i].isEmpty()) {
                int x = bounds[i].x + difX;
                int y = bounds[i].y;
                // O MESMO FUNDO PASTEL DO INVENTARIO.
                //
                // A hotbar e a unica lista de itens que fica na tela o tempo todo, e
                // era a unica sem a cor: o jogador aprendia a ler raridade pelo fundo
                // dentro da bolsa e perdia essa leitura justamente onde ela e mais
                // usada — na hora de escolher a arma no meio da briga.
                int ix = x + 2 * Configs.HudScale();
                int iy = y + 2 * Configs.HudScale();
                int ilado = 12 * Configs.HudScale();
                com.retronova.game.items.Raridade raridade = items[i].item().raridade();
                if (raridade != com.retronova.game.items.Raridade.COMUM) {
                    java.awt.Color c = raridade.cor();
                    g.setColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), 56));
                    g.fillRect(ix, iy, ilado, ilado);
                }
                g.drawImage(items[i].item().getSprite(), ix, iy, ilado, ilado, null);
                if(items[i].item() instanceof Consumable consumable) {
                    if(consumable.getStack() <= 1)
                        continue;
                    String stack = String.valueOf(consumable.getStack());
                    int wf = FontHandler.getWidth(stack, fontStack);
                    int hf = FontHandler.getHeight(stack, fontStack);
                    DrawString.draw(stack, fontStack, x + bounds[i].width - wf - 2 * Configs.HudScale(), y + bounds[i].height - hf - 2 * Configs.HudScale(), g);
                }
            }
        }
    }

}
