package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.StoreException;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.graphics.UiSprite;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.engine.Debugging;
import com.retronova.game.Game;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.game.objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Store implements Activity {

    /**
     * A grade da loja: sete casas por fileira, uma fileira por FAMÍLIA de arma.
     *
     * Eram três fileiras, depois cinco, sete, e agora seis — o painel é esticado
     * em tools/GenLoja.java e os dois números têm de andar juntos. Seis porque são
     * seis famílias de ARMA: espadas, machados, arcos, varinhas, lâminas e
     * diversos. A sétima era a dos consumíveis, que saiu junto com eles. Casas vazias são de propósito: a
     * lacuna na prateleira dos machados é justamente como se vê, de relance, que
     * o jogo só tem um machado.
     */
    private static final int COLUNAS = 7, FILEIRAS = 6;

    private final UiSprite store;
    private final Point positionStore;
    private int indexSelected = -1;
    private final Slot[] slots;
    private final int[] prices;

    private final Rectangle buttonBuy;


    public Store(Item[] items, int[] prices) {
        if(items.length != prices.length) {
            throw new StoreException("A quantidade de items não bate com a quantidade de preços");
        }
        this.prices = prices;
        this.store = new UiSprite("ui", "store");
        this.slots = new Slot[COLUNAS * FILEIRAS];
        this.positionStore = new Point();
        this.buttonBuy = new Rectangle(23 * Configs.HudScale(), 14 * Configs.HudScale());
        for(int i = 0; i < slots.length; i++) {
            this.slots[i] = new Slot(0, 0);
            if(i > items.length-1 || items[i] == null)
                continue;
            if(items[i] instanceof Consumable consumable) {
                this.slots[i].put(Item.build(items[i].getID(), consumable.getStack()));
            }else {
                this.slots[i].put(Item.build(items[i].getID()));
            }
        }
        refreshPosition();
    }

    private void refreshPosition() {
        int w = Engine.window.getWidth();
        int h = Engine.window.getHeight();
        this.positionStore.setLocation(w/2 - store.largura()/2, h/2 - store.altura()/2);
        // O rodapé desceu junto com o painel: duas fileiras a mais de 16 pixels.
        int rodape = 72 + 16 * (FILEIRAS - 3);
        this.buttonBuy.setLocation(positionStore.x + 95 * Configs.HudScale(), positionStore.y + rodape * Configs.HudScale());
        for(int y = 0; y < FILEIRAS; y++)
            for(int x = 0; x < COLUNAS; x++) {
                Slot slot = this.slots[x+y*COLUNAS];
                int xx = positionStore.x + 6 * Configs.HudScale() + x * slot.getBounds().width;
                int yy = positionStore.y + 6 * Configs.HudScale() + y * slot.getBounds().height;
                slot.setPosition(xx, yy);
            }
    }

    @Override
    public void tick() {
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if(Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds()) && !slot.isEmpty()) {
                this.indexSelected = i;
                break;
            }
        }
        // Patinha de ponteiro em cima do que da para clicar: as casas com item e
        // o botao de comprar quando ha algo selecionado e dinheiro para pagar.
        for (Slot slot : slots) {
            if (Mouse.on(slot.getBounds()) && !slot.isEmpty()) {
                Engine.window.pointing();
                break;
            }
        }
        Player player = Game.getPlayer();
        if (indexSelected != -1 && Mouse.on(buttonBuy)
                && player.getMoney() >= prices[indexSelected]) {
            Engine.window.pointing();
        }
        if(indexSelected != -1 &&  Mouse.clickOn(Mouse_Button.LEFT, buttonBuy) && player.getMoney() >= prices[indexSelected]) {
            // Na vitrine a prateleira não esvazia: dá para pegar o mesmo item
            // de novo depois de trocar de arma, que é o que uma revisão exige.
            Item comprado = Debugging.VITRINE
                    ? Item.build(slots[indexSelected].item().getID(), 1)
                    : slots[indexSelected].take();
            player.getInventory().give(comprado);
            player.setMoney(player.getMoney() - prices[indexSelected]);
            if (slots[indexSelected].isEmpty()) {
                indexSelected = -1;
            }
            Sound.play(Sounds.Coin);
            Sound.play(Sounds.Cat);
        }
    }

    @Override
    public void render(Graphics2D g) {
        refreshPosition();
        renderStore(g);
        renderSlots(g);
        renderPriceSelected(g);
        renderButtonBuy(g);
        renderInfo(g);
    }

    private void renderStore(Graphics2D g) {
        g.drawImage(store.imagem(), positionStore.x, positionStore.y - 16 * Configs.HudScale(), null);
    }

    private void renderSlots(Graphics2D g) {
        Rectangle rec = null;
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            slot.render(g);
            if(i == indexSelected) {
                rec = slot.getBounds();
            }
        }
        if(rec == null)
            return;
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(Configs.HudScale()));
        g.drawRect(rec.x, rec.y, rec.width, rec.height);
    }

    private void renderPriceSelected(Graphics2D g) {
        if(indexSelected == -1)
            return;
        int x = positionStore.x + 18 * Configs.HudScale();
        int y = positionStore.y + (82 + 16 * (FILEIRAS - 3)) * Configs.HudScale();
        Font font = FontHandler.font(FontHandler.Game,Configs.HudScale() * 8);
        String value = prices[indexSelected] + "/" + Game.getPlayer().getMoney();
        g.setFont(font);
        g.setColor(Color.black);
        g.drawString(value, x + Configs.HudScale(), y + Configs.HudScale());
        g.setColor(Color.white);
        g.drawString(value, x, y);
    }

    public void renderButtonBuy(Graphics2D g) {
        if(!Mouse.on(buttonBuy))
            return;
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(Configs.HudScale()));
        g.drawRect(buttonBuy.x, buttonBuy.y, buttonBuy.width, buttonBuy.height);
    }

    private void renderInfo(Graphics2D g) {
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if (Mouse.on(slot.getBounds()) && !slot.isEmpty()) {
                slot.renderInfo(g);
            }
        }
    }

    @Override
    public void dispose() {

    }
}
