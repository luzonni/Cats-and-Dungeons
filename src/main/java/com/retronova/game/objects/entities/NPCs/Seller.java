package com.retronova.game.objects.entities.NPCs;

import com.retronova.engine.Engine;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Seller extends NPC {

    private final Store store;
    private int countAnim;

    public Item[] stock;
    public int[] prices;

    /**
     * Peso 1000: o Physical ignora deslocamento acima de 500, entao o vendedor
     * nao sai do lugar. Com o peso antigo (60) o gato empurrava o coitado pela
     * sala inteira, e a barraca ficava para tras.
     */
    public Seller(int ID, double x, double y, JSONArray stock) {
        super(ID, x, y, 1000);
        loadStock(stock);
        this.store = new Store(this.stock, this.prices);
        loadSprites("seller");
    }

    /**
     * A prateleira inteira, sempre.
     *
     * O mapa listava dois itens — espada e racao — e era so isso que aparecia na
     * loja. Como o proposito da vitrine e servir de INVENTARIO DO PROJETO, para
     * bater o olho e ver o que ja existe e o que ainda falta, mostrar um recorte
     * derrota o objetivo: um item sem arte ficava invisivel exatamente por estar
     * faltando. Entao a loja monta o catalogo completo, e o que o mapa diz vira
     * o preco e a quantidade DAQUELES itens que ele mencionou.
     */
    private void loadStock(JSONArray stock) {
        Map<ItemIDs, int[]> doMapa = lerDoMapa(stock);
        int total = VITRINE.length * COLUNAS;
        this.stock = new Item[total];
        this.prices = new int[total];
        Set<ItemIDs> naPrateleira = new HashSet<>();

        for (int fileira = 0; fileira < VITRINE.length; fileira++) {
            ItemIDs[] familia = VITRINE[fileira];
            for (int i = 0; i < familia.length && i < COLUNAS; i++) {
                por(fileira * COLUNAS + i, familia[i], doMapa);
                naPrateleira.add(familia[i]);
            }
        }

        // Item novo que ninguem lembrou de por numa familia cai na primeira casa
        // vaga, em vez de sumir da loja sem aviso — que e justamente o defeito
        // que esta vitrine existe para pegar.
        for (ItemIDs id : ItemIDs.values()) {
            if (naPrateleira.contains(id) || EM_ESPERA.contains(id)) {
                continue;
            }
            for (int casa = 0; casa < total; casa++) {
                if (this.stock[casa] == null) {
                    por(casa, id, doMapa);
                    break;
                }
            }
        }
    }

    /** Quantidade e preco que o mapa definiu, por item. Pode vir vazio. */
    private static Map<ItemIDs, int[]> lerDoMapa(JSONArray stock) {
        Map<ItemIDs, int[]> saida = new HashMap<>();
        if (stock == null) {
            return saida;
        }
        for (Object o : stock) {
            JSONObject item = (JSONObject) o;
            String nome = (String) item.get("name");
            for (ItemIDs id : ItemIDs.values()) {
                if (id.name().equalsIgnoreCase(nome)) {
                    saida.put(id, new int[]{
                            ((Number) item.get("amount")).intValue(),
                            ((Number) item.get("price")).intValue()});
                    break;
                }
            }
        }
        return saida;
    }

    /** Quantidade e preco de fabrica, para o que o mapa nao mencionou. */
    private static final int PADRAO_QUANTIDADE = 10, PADRAO_PRECO = 10;

    private void por(int casa, ItemIDs id, Map<ItemIDs, int[]> doMapa) {
        int[] m = doMapa.get(id);
        int quantidade = m == null ? PADRAO_QUANTIDADE : m[0];
        int preco = m == null ? PADRAO_PRECO : m[1];
        this.stock[casa] = Item.build(id.ordinal(), quantidade);
        this.prices[casa] = preco;
    }

    /**
     * A vitrine: uma fileira por FAMILIA.
     *
     * A loja tem sete colunas, entao cada linha desta tabela e uma prateleira —
     * espadas, machados, arcos, varinhas e diversos. Serve para a revisao de
     * itens: com espada ao lado de espada da para ver de relance o que ja tem
     * arte nova e, principalmente, ONDE FALTA. A prateleira dos machados com um
     * item so e a forma mais honesta de dizer que o jogo tem um machado.
     *
     * As sobras ficam vazias de proposito. Nada e empurrado para preencher
     * buraco, senao a fileira perde o sentido de familia.
     *
     * A ordem NAO pode sair do enum. Ali o ordinal e o proprio identificador do
     * item, gravado em JSON de personagem e de mapa — reordenar o enum trocaria
     * o inventario inicial de todo mundo. Por isso a vitrine e uma lista a parte.
     */
    private static final ItemIDs[][] VITRINE = {
            // espadas
            {ItemIDs.Sword, ItemIDs.SwordFire, ItemIDs.SwordIce, ItemIDs.SwordWater,
             ItemIDs.SwordEarth, ItemIDs.SwordAir, ItemIDs.SwordLegend},
            // machados
            {ItemIDs.Axe, ItemIDs.AxeFire, ItemIDs.AxeIce, ItemIDs.AxeWater,
             ItemIDs.AxeEarth, ItemIDs.AxeAir, ItemIDs.AxeLegend},
            // arcos
            {ItemIDs.Bow, ItemIDs.BowFire, ItemIDs.BowIce, ItemIDs.BowWater,
             ItemIDs.BowEarth, ItemIDs.BowAir, ItemIDs.BowLegend},
            // varinhas
            {ItemIDs.Wand, ItemIDs.WandFire, ItemIDs.WandIce, ItemIDs.WandWater,
             ItemIDs.WandEarth, ItemIDs.WandAir, ItemIDs.WandLegend},
            // laminas e arremesso
            {ItemIDs.Sickle, ItemIDs.ClawBlades, ItemIDs.Kunai, ItemIDs.Trident,
             ItemIDs.BloodyAxe, ItemIDs.Silk, ItemIDs.Furball},
            // diversos
            {ItemIDs.DangerousWand, ItemIDs.Laser, ItemIDs.BowEletric},
    };

    /**
     * EM ESPERA: nao aparecem na loja.
     *
     * Sao os consumiveis e os arremessaveis de uso — cura, iscas e bombas. O foco
     * do trabalho e nas armas, e sete casas ocupadas por item de consumo eram sete
     * casas a menos para ver o arsenal de relance, que e para o que esta vitrine
     * serve. Nada foi apagado: as classes, os IDs e os saves continuam de pe, e
     * tirar um nome desta lista devolve o item a loja.
     */
    private static final java.util.Set<ItemIDs> EM_ESPERA = java.util.EnumSet.of(
            ItemIDs.Feed, ItemIDs.Acorn, ItemIDs.Catnip, ItemIDs.Watermelon,
            ItemIDs.MagneticOrb, ItemIDs.Bomb, ItemIDs.GasBomb);

    /** Colunas da loja. Tem de bater com Store.COLUNAS. */
    private static final int COLUNAS = 7;

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