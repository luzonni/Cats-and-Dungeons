package com.retronova.game.interfaces;

import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.InventoryOutsOfBounds;
import com.retronova.game.Game;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.engine.graphics.UiSprite;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Inventory implements Activity {

    private final Slot insurer;
    private final Slot[] hotbar;
    private int lengthHotbar;
    private final Slot[] bag;
    private int lengthBag;

    private Item itemHand;

    private Point inventoryPosition;
    /** Escala do HUD com que as posicoes atuais foram calculadas. */
    private int escalaAplicada = -1;
    private final UiSprite inventory;
    /**
     * As orelhas de gato, desenhadas ACIMA do painel.
     *
     * Vem numa imagem propria em vez de assadas no PNG do painel: o painel tem
     * tamanho fixo e toda posicao de slot e contada a partir do canto dele, entao
     * crescer o arquivo empurraria a tela inteira. Ver tools/GenOrelhas.java.
     */
    private final UiSprite orelhas;

    public Inventory(int lengthBag, int lengthHotbar) {
        if(lengthBag > 15 || lengthHotbar > 5) {
            throw new InventoryOutsOfBounds("Valor de slots acima do permitido");
        }
        this.lengthBag = lengthBag;
        this.lengthHotbar = lengthHotbar;
        this.insurer = new Slot(0, 0);
        this.bag = new Slot[15];
        this.hotbar = new Slot[5];
        this.inventory = new UiSprite("ui", "inventory");
        this.orelhas = new UiSprite("ui", "ears_inventory");
        refreshPositions();
    }

    /**
     * Recoloca a moldura e os slots.
     *
     * Reage a DOIS gatilhos, e nao so a um: a janela mudou de tamanho, ou a
     * escala do HUD mudou. Antes so o primeiro era observado, entao mexer no
     * "HUD size" em Options deixava a moldura no tamanho velho e os slots nas
     * coordenadas novas — o inventario inteiro fora do lugar.
     */
    public void refreshPositions() {
        int s = Configs.HudScale();
        int X = Engine.window.getWidth() / 2 - inventory.largura() / 2;
        int Y = Engine.window.getHeight() / 2 - inventory.altura() / 2;

        boolean primeiraVez = this.inventoryPosition == null;
        boolean mudouPosicao = primeiraVez
                || this.inventoryPosition.x != X || this.inventoryPosition.y != Y;
        if (!mudouPosicao && s == this.escalaAplicada) {
            return;
        }
        this.escalaAplicada = s;
        if (primeiraVez) {
            this.inventoryPosition = new Point(X, Y);
        } else {
            this.inventoryPosition.setLocation(X, Y);
        }

        int w = 16 * s;
        int xh = X + 6 * s, yh = Y + 70 * s;
        int xi = X + 6 * s, yi = Y + 6 * s;
        for (int i = 0; i < hotbar.length; i++) {
            if (hotbar[i] == null) {
                hotbar[i] = new Slot(xh + i * w, yh);
            } else {
                hotbar[i].setPosition(xh + i * w, yh);
            }
        }
        for (int yy = 0; yy < 3; yy++) {
            for (int xx = 0; xx < 5; xx++) {
                int index = xx + yy * 5;
                int px = xi + xx * w;
                int py = yi + yy * w;
                if (bag[index] == null) {
                    bag[index] = new Slot(px, py);
                } else {
                    bag[index].setPosition(px, py);
                }
            }
        }
    }

    public void give(Item item) {
        Slot candidate = null;
        for(int i = 0; i < getHotbarSize(); i++) {
            Slot slot = hotbar[i];
            if(slot.isEmpty() || (slot.item().stackable() && slot.item().getID() == item.getID())) {
                candidate = slot;
                break;
            }
        }
        if(candidate == null)
            for(int i = 0; i < getBagSize(); i++) {
                Slot slot = bag[i];
                if(slot.isEmpty() || (slot.item().stackable() && slot.item().getID() == item.getID())) {
                    candidate = slot;
                    break;
                }
            }
        if(candidate == null)
            Game.getPlayer().dropLoot(item);
        else
            candidate.put(item);
    }

    public void setItemHand(Item item) {
        this.itemHand = item;
    }

    public Item getItemHand() {
        return this.itemHand;
    }

    public int getHotbarSize() {
        return this.lengthHotbar;
    }

    public int getBagSize() {
        return this.lengthBag;
    }

    public boolean plusBag(int amount) {
        if(this.lengthBag + amount <= 15) {
            this.lengthBag += amount;
            return true;
        }
        return false;
    }

    public boolean plusHotbar(int amount) {
        if(this.lengthHotbar + amount <= 5) {
            this.lengthHotbar += amount;
            return true;
        }
        return false;
    }

    public Slot[] getHotbar() {
        return this.hotbar;
    }

    @Override
    public void tick() {
        refreshPositions();
        // Mochila e hotbar sao percorridas separadas, e nao pela lista juntada:
        // o Shift precisa saber DE ONDE o item saiu para saber para onde manda-lo.
        for (int i = 0; i < lengthBag; i++) {
            cuidarDoSlot(bag[i], true);
        }
        for (int i = 0; i < lengthHotbar; i++) {
            cuidarDoSlot(hotbar[i], false);
        }
    }

    private void cuidarDoSlot(Slot slot, boolean naMochila) {
        // A patinha vira ponteiro em cima de casa clicavel. Casa vazia com a mao
        // cheia tambem conta: largar o item ali e uma acao.
        if (Mouse.on(slot.getBounds()) && (!slot.isEmpty() || !insurer.isEmpty())) {
            Engine.window.pointing();
        }
        if (Mouse.on(slot.getBounds()) && !slot.isEmpty()
                && KeyBoard.KeyPressing("Shift") && Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
            mandarParaOOutroLado(slot, naMochila);
            return;
        }
        interation(slot);
        if(slot.item() instanceof Consumable consumable) {
            if(Mouse.on(slot.getBounds()) && KeyBoard.KeyPressed("F")) {
                consumable.consume();
                slot.take();
            }
        }
    }

    /**
     * Shift + clique: manda o item para o outro lado do inventario.
     *
     * E o gesto que todo mundo ja tem no dedo — Minecraft, Terraria, qualquer
     * jogo com mochila — e sem ele equipar uma arma exigia tres cliques: pegar,
     * levar, largar. O destino e a primeira casa que ACEITA o item: uma pilha do
     * mesmo tipo antes de uma casa vazia, para nao espalhar dez racoes em dez
     * casas. Sem lugar do outro lado, o item fica onde esta: engolir o clique e
     * melhor que largar a arma do jogador no chao sem ele pedir.
     */
    private void mandarParaOOutroLado(Slot origem, boolean naMochila) {
        Slot[] destino = naMochila ? hotbar : bag;
        int quantas = naMochila ? lengthHotbar : lengthBag;
        Item item = origem.takeAll();
        for (int i = 0; i < quantas; i++) {
            Slot casa = destino[i];
            boolean empilha = !casa.isEmpty() && casa.item().stackable()
                    && casa.item().getID() == item.getID();
            if (empilha && casa.put(item)) {
                return;
            }
        }
        for (int i = 0; i < quantas; i++) {
            if (destino[i].isEmpty() && destino[i].put(item)) {
                return;
            }
        }
        origem.put(item);      // nao coube: volta para onde estava
    }

    private void interation(Slot slot) {
        if(Mouse.clickOn(Mouse_Button.LEFT, slot.getBounds())) {
            if(slot.isEmpty()) {
                slot.put(this.insurer.takeAll());
            }else {
                if(this.insurer.isEmpty()) {
                    this.insurer.put(slot.takeAll());
                }else {
                    replace(slot, this.insurer);
                }
            }
        }
        if(Mouse.clickOn(Mouse_Button.RIGHT, slot.getBounds())) {
            if(slot.isEmpty()) {
                slot.put(this.insurer.take());
            }else {
                if(this.insurer.isEmpty() || this.insurer.item().getID() == slot.item().getID()) {
                    this.insurer.put(slot.take());
                }else {
                    replace(slot, this.insurer);
                }
            }
        }
    }

    private void replace(Slot slot1, Slot slot2) {
        Item item = slot2.takeAll();
        if(!slot1.put(item)) {
            Item currentItem = slot1.takeAll();
            slot2.put(currentItem);
            slot1.put(item);
        }
    }

    private Slot[] merge() {
        Slot[] merged = new Slot[lengthBag + lengthHotbar];
        int index = 0;
        for(int i = 0; i < lengthBag; i++, index++) {
            merged[index] = bag[i];
        }
        for(int i = 0; i < lengthHotbar; i++, index++) {
            merged[index] = hotbar[i];
        }
        return merged;
    }

    @Override
    public void render(Graphics2D g) {
        renderInventory(g);
        renderInsurer(g);
        renderInfo(g);
    }

    private void renderInventory(Graphics2D g) {
        g.drawImage(this.orelhas.imagem(), inventoryPosition.x,
                // Desce uma linha de arte: a tira tem uma linha de costura no
                // pe justamente para encostar no painel, e sem isso ela ficava
                // pairando um pixel acima, com a fresta aparecendo no meio.
                inventoryPosition.y - orelhas.altura() + Configs.HudScale(), null);
        g.drawImage(this.inventory.imagem(), inventoryPosition.x, inventoryPosition.y, null);
        for(int i = 0; i < lengthBag; i++) {
            bag[i].render(g);
        }
        for(int i = 0; i < lengthHotbar; i++) {
            hotbar[i].render(g);
        }
    }

    // A NUMERACAO SAIU DAQUI TAMBEM.
    //
    // Ela foi tirada da hotbar da tela porque disputava o canto com o fundo de
    // raridade, mas continuava desenhada na fileira de baixo do painel — que e a
    // MESMA hotbar, so que vista de dentro do inventario. O resultado era a
    // informacao aparecendo em um lugar e nao no outro, o que le como falha.

    private void renderInsurer(Graphics2D g) {
        if(!insurer.isEmpty()) {
            int x = Mouse.getX() + 16;
            int y = Mouse.getY() + 16;
            g.drawImage(insurer.item().getSprite(), x, y, 16 * 2, 16 * 2, null);
        }
    }

    private void renderInfo(Graphics2D g) {
        Slot[] slots = merge();
        for(int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if (Mouse.on(slot.getBounds()) && !slot.isEmpty()) {
                slot.renderInfo(g);
            }
        }
    }

    @Override
    public void dispose() {
        if(!this.insurer.isEmpty())
            Game.getPlayer().dropLoot(this.insurer.takeAll());
    }

}
