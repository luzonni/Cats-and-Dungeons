package com.retronova.game.interfaces;

import com.retronova.engine.Configs;
import com.retronova.engine.graphics.DrawString;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.UiSprite;
import com.retronova.engine.inputs.mouse.Mouse;

import java.awt.*;

public class Slot {

    private final InfoBox info;
    private final UiSprite sprite;
    private final Rectangle bounds;
    private Item item;

    public Slot(int x, int y) {
        this.item = null;
        this.sprite = new UiSprite("ui", "slot");
        this.bounds = new Rectangle(x, y, sprite.largura(), sprite.altura());
        info = new InfoBox();
    }

    public void setPosition(int x, int y) {
        this.bounds.setLocation(x, y);
        // O tamanho acompanha a escala do HUD: sem isto, mudar "HUD size" movia
        // os slots mas mantinha a area de clique do tamanho antigo.
        this.bounds.setSize(sprite.largura(), sprite.altura());
    }

    public Item item() {
        return this.item;
    }

    public Rectangle getBounds() {
        return this.bounds;
    }

    public boolean put(Item item) {
        if(isEmpty()) {
            this.item = item;
            return true;
        }else if(this.item.getID() == item.getID() && this.item instanceof Consumable consumable) {
            int otherItemStack = ((Consumable) item).getStack();
            consumable.setStack(consumable.getStack() + otherItemStack);
            return true;
        }
        return false;
    }

    public Item take() {
        Item caught = this.item;
        if(caught instanceof Consumable consumable) {
            int currentStack = consumable.getStack();
            consumable.setStack(currentStack-1);
            if(consumable.getStack() <= 0) {
                this.item = null;
            }
            return Item.build(consumable.getID(), 1);
        }
        this.item = null;
        return caught;
    }

    public Item takeAll() {
        Item caught = this.item;
        this.item = null;
        return caught;
    }

    public boolean isEmpty() {
        return this.item == null;
    }

    public void render(Graphics2D g) {
        g.drawImage(sprite.imagem(), bounds.x, bounds.y, null);
        renderItem(g);
    }

    public void renderItem(Graphics2D g) {
        if(isEmpty())
            return;
        int overAnimPref;
        if(Mouse.on(getBounds())) {
            overAnimPref = 2;
        }else {
            overAnimPref = 0;
        }
        int x = bounds.x + overAnimPref;
        int y = bounds.y + overAnimPref;
        int width = bounds.width - overAnimPref*2;
        int height = bounds.height - overAnimPref*2;
        // O FUNDO DIZ A RARIDADE, e o selo saiu daqui.
        //
        // O carimbo no canto funcionava, mas competia com a arte: num quadrado de
        // poucos pixels, um simbolo por cima do item tapa justamente a parte que
        // identifica o item. O fundo resolve o mesmo problema sem ocupar espaco
        // nenhum — a cor esta atras, e o desenho continua inteiro na frente.
        //
        // PASTEL, E NAO A COR CHEIA. A cor da raridade e forte por natureza: ela
        // existe para gritar numa carta de recompensa, que e vista uma por vez.
        // Repetida em vinte slots lado a lado, ela vira um mosaico que come o
        // contraste dos itens. Um quinto de opacidade e o suficiente para o olho
        // separar um roxo de um azul de relance, e pouco o bastante para a lamina
        // de tres pixels continuar visivel.
        //
        // O comum nao pinta nada: se todo slot tem cor, cor deixa de ser sinal.
        com.retronova.game.items.Raridade raridade = item.raridade();
        if (raridade != com.retronova.game.items.Raridade.COMUM) {
            java.awt.Color c = raridade.cor();
            g.setColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), 56));
            g.fillRect(x, y, width, height);
        }
        g.drawImage(item.getSprite(), x, y, width, height, null);
        if(item instanceof Consumable consumable) {
            String stack = String.valueOf(consumable.getStack());
            Font fontStack = UiSprite.fonte(FontHandler.Septem, 8f);
            int wf = FontHandler.getWidth(stack, fontStack);
            int hf = FontHandler.getHeight(stack, fontStack);
            DrawString.draw(stack, fontStack, x + width - wf - Configs.HudScale(), y + height - hf - Configs.HudScale(), g);
        }
    }

    public void renderInfo(Graphics2D g) {
        if(Mouse.on(getBounds()) && !isEmpty()) {
            info.setValues(this.item);
            info.render(g);
        }
    }
}
