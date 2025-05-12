package com.retronova.game.objects.entities.NPCs;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.interfaces.Store;
import com.retronova.game.items.Item;
import com.retronova.game.items.ItemIDs;
import com.retronova.game.map.GameMap;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.particles.Word;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Seller extends NPC {

    private final Store store;
    private int countAnim;

    public Item[] stock;
    public int[] prices;

    public Seller(int ID, double x, double y, JSONArray stock) {
        super(ID, x, y, 60);
        loadStock(stock);
        this.store = new Store(this.stock, this.prices);
        loadSprites("seller");
    }

    private void loadStock(JSONArray stock) {
        if(stock == null)
            throw new RuntimeException("Stock null");
        if(stock.isEmpty()) {
            loadFullStock();
            return;
        }
        this.stock = new Item[stock.size()];
        this.prices = new int[stock.size()];
        for(int i = 0; i < stock.size(); i++) {
            JSONObject item = (JSONObject) stock.get(i);
            ItemIDs[] values = ItemIDs.values();
            int id = -1;
            int amount = ((Number)item.get("amount")).intValue();
            int price = ((Number)item.get("price")).intValue();
            for(int j = 0; j < values.length; j++) {
                if(((String)item.get("name")).equalsIgnoreCase(values[j].name())) {
                    id = j;
                    break;
                }
            }
            this.stock[i] = Item.build(id, amount);
            this.prices[i] = price;
        }
    }

    private void loadFullStock() {
        ItemIDs[] values = ItemIDs.values();
        this.stock = new Item[values.length];
        this.prices = new int[values.length];
        for(int i = 0; i < values.length; i++) {
            Item item = Item.build(values[i].ordinal(), 10);
            this.stock[i] = item;
            this.prices[i] = 10;
        }
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        countAnim++;
        if (player.getDistance(this) <= GameObject.SIZE() * 3) {
            if (GameMap.clickOnRect(Mouse_Button.LEFT, this.getBounds())) {
                Game.getInter().put("store", this.store, true);
                Game.getInter().open("store");
            }
            if (countAnim >= 10) {
                countAnim = 0;
                getSheet().plusIndex();
            }
        }else {
            getSheet().setIndex(2);
            if (countAnim >= 60) {
                countAnim = 0;
                double x = getX() + Engine.RAND.nextInt(getWidth());
                double y = getY() + Engine.RAND.nextInt(getHeight());
                String z = Engine.RAND.nextBoolean() ? "Z" : "z";
                Particle p = new Word(z, x, y, 1);
                Game.getMap().put(p);
            }
        }
    }
}