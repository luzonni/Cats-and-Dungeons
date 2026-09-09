package com.retronova.game.map;

import com.retronova.engine.Engine;
import com.retronova.engine.exceptions.EntityNotFound;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.io.Resources;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.particles.Particle;
import com.retronova.game.objects.physical.Repulsion;
import com.retronova.game.objects.tiles.Tile;
import com.retronova.game.objects.tiles.TileIDs;
import com.retronova.engine.graphics.SpriteHandler;
import com.retronova.game.objects.tiles.Void;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public abstract class GameMap {

    private int length;
    private Rectangle bounds;

    private List<Entity> entities;

    private List<Particle> particles;
    private Tile[] map;

    private final Repulsion repulsion;

    /**
     * Onde o jogador nasce, em tiles. null cai no centro do mapa.
     *
     * Vem do JSON porque o centro nao serve para toda sala: na antecamara o gato
     * chega pela porta ao norte, e nascer no meio do salao apagava a unica pista
     * de como ele entrou ali.
     */
    private Point nascimento;

    public GameMap(String mapName) {
        this.entities = new ArrayList<>();
        this.particles = new ArrayList<>();
        loadMap(mapName);
        this.repulsion = new Repulsion(this);
        this.repulsion.start();
    }

    public void addPlayer(Player player) {
        put(player);
        // ZERA O EMPURRAO QUE VEIO DA SALA ANTERIOR.
        //
        // O gato atravessa a passagem ANDANDO, e o vetor dessa caminhada sobrevive
        // a troca de mapa: a posicao nova e escrita aqui, mas no primeiro tick a
        // fisica retoma o empurrao de onde parou e o arrasta para fora do tile em
        // que ele acabou de ser posto. Era por isso que ele nao nascia no ralo do
        // centro mesmo com o mapa dizendo que o nascimento e ali.
        player.getPhysical().parar();
        if(this.nascimento != null) {
            player.setX(this.nascimento.x * (double) GameObject.SIZE());
            player.setY(this.nascimento.y * (double) GameObject.SIZE());
            return;
        }
        player.setX(getBounds().getWidth()/2);
        player.setY(getBounds().getHeight()/2);
    }

    private void loadMap(String mapName) {
        BufferedImage mapImage = new SpriteHandler("maps", mapName, 1).getSHEET();
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        this.bounds = new Rectangle(width * GameObject.SIZE(), height * GameObject.SIZE());
        int[] rgb = mapImage.getRGB(0, 0, width, height, null, 0, width);
        this.map = convertMap(rgb, width, height);
        this.length = width;
        JSONObject jsonObject = null;
        try {
            jsonObject = Resources.getJsonFile("maps", mapName);
        } catch (IOException ignore) {
            System.err.println("Arquivo não encontrado: " + mapName);
        }
        if(jsonObject != null && !jsonObject.isEmpty()) {
            this.nascimento = lerNascimento(jsonObject);
            loadEntities(width, height, jsonObject);
        }
    }

    private Tile[] convertMap(int[] rgb, int width, int height) {
        Tile[] map = new Tile[width * height];
        TileIDs[] values = TileIDs.values();
        for (int y = 0; y < height; y++) {
            line : for (int x = 0; x < width; x++) {
                int index = x + y * width;
                for (TileIDs value : values) {
                    if (value.getColor() == rgb[index]) {
                        map[index] = Tile.build(value.ordinal(), x, y);
                        continue line;
                    }
                }
                map[index] = Tile.build(TileIDs.Void.ordinal(), x, y);
            }
        }
        return map;
    }

    /** Le {@code "spawn": {"x": .., "y": ..}}, se o mapa declarar um. */
    private Point lerNascimento(JSONObject jsonObject) {
        Object bruto = jsonObject.get("spawn");
        if(!(bruto instanceof JSONObject spawn)) {
            return null;
        }
        Object x = spawn.get("x");
        Object y = spawn.get("y");
        if(!(x instanceof Number) || !(y instanceof Number)) {
            System.err.println("spawn sem x/y numericos; usando o centro do mapa.");
            return null;
        }
        return new Point(((Number) x).intValue(), ((Number) y).intValue());
    }

    private void loadEntities(int width, int height, JSONObject jsonObject) {
        List<Entity> list = new ArrayList<>();
        JSONArray arrEntity = (JSONArray) jsonObject.get("entities");
        for(int i = 0; i < arrEntity.size(); i++) {
            JSONObject obj = (JSONObject) arrEntity.get(i);
            String name = (String)obj.get("name");
            EntityIDs[] entityIDs = EntityIDs.values();
            if(obj.containsKey("chanceToAppear")) {
                double chance = ((Number)obj.get("chanceToAppear")).doubleValue() * 100;
                if(Engine.RAND.nextInt(100) > chance) {
                    continue;
                }
            }
            int id = -1;
            for(int j = 0; j < entityIDs.length; j++) {
                if(entityIDs[j].name().equalsIgnoreCase(name)) {
                    id = entityIDs[j].ordinal();
                    break;
                }
            }
            if(id == -1) {
                throw new EntityNotFound("Erro ao colocar a entidade ao mapa.");
            }
            int x = -1;
            int y = -1;
            if(obj.containsKey("position")) {
               String type = ((String)obj.get("position"));
               if(type.equalsIgnoreCase("Random")) {
                   Tile tile;
                   do {
                       x = Engine.RAND.nextInt(width);
                       y = Engine.RAND.nextInt(height);
                       tile = this.map[x + y * this.length];
                   } while (tile.isSolid() || (tile instanceof Void));
               }else if(type.equalsIgnoreCase("Center")) {
                   x = width/2;
                   y = height/2;
               }
            }else {
                x = ((Number)obj.get("x")).intValue();
                y = ((Number)obj.get("y")).intValue();
            }
            if(x == -1 || y == -1) {
                throw new EntityNotFound("Erro ao colocar a entidade ao mapa.");
            }
            JSONArray objValues = (JSONArray)obj.get("values");
            Object[] values = new Object[objValues.size() + 2];
            values[0] = x;
            values[1] = y;
            for(int j = 2; j < values.length; j++) {
                values[j] = objValues.get(j-2);
            }
            Entity e = Entity.build(id, values);
            list.add(e);
        }
        putAll(list);
    }

    public Repulsion getRepulsion() {
        return this.repulsion;
    }

    public Tile[] getMap() {
        return this.map;
    }

    /**
     * Há parede entre os dois pontos?
     *
     * Não existia nada disso, e por isso o arco mirava e atirava através de
     * alvenaria: ele só perguntava quem era o inimigo mais próximo, e distância
     * não sabe de parede. A reta é amostrada de meio tile em meio tile, que é
     * fino o bastante para não passar pela quina de um bloco e barato o bastante
     * para rodar a cada tiro.
     */
    public boolean linhaLivre(double x1, double y1, double x2, double y2) {
        // Amostra de um TERCO de tile: um passo de meio tile podia pular a quina
        // de um bloco e dizer que havia caminho onde nao ha.
        int passo = Math.max(1, GameObject.SIZE() / 3);
        double distancia = Math.hypot(x2 - x1, y2 - y1);
        int amostras = (int) Math.max(1, distancia / passo);
        for (int i = 1; i < amostras; i++) {
            double t = i / (double) amostras;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            if (solidoEm(x, y)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ha bloco solido NESTE PONTO DA TELA?
     *
     * Converte para indice de tile aqui, de forma explicita, em vez de passar
     * pixels para getTile. Aquele metodo tenta primeiro interpretar os numeros
     * como indice de tile e so cai para pixels quando o indice estoura o vetor —
     * o que quer dizer que, para coordenadas de pixel pequenas, ele acerta um
     * tile QUALQUER dentro do vetor e devolve com cara de resposta certa. Era
     * assim que a linha de visao dizia "esta livre" com uma parede no meio.
     */
    /**
     * Ha caminho para um PROJETIL daqui ate a caixa do alvo?
     *
     * Duas diferencas em relacao ao linhaLivre simples, e as duas sairam de
     * defeito visto jogando:
     *
     * 1. O RAIO PARA AO CHEGAR NO ALVO. Amostrar ate o centro do inimigo faz com
     *    que um inimigo ENCOSTADO na parede seja considerado inalcancavel para
     *    sempre: a ultima amostra cai no bloco atras dele e a resposta e "nao ha
     *    caminho". Era isso que travava a arma mirando eternamente num bicho
     *    colado no muro. Em roguelike a regra e a mesma: a casa do alvo nao
     *    bloqueia a linha ate o alvo.
     *
     * 2. O RAIO TEM LARGURA. O projetil e um quadrado de vinte e quatro pixels,
     *    nao um fio. Uma linha de espessura zero passa por frestas por onde a
     *    flecha nao cabe, e ai a arma dispara contra um vao que vai mata-la no
     *    primeiro tick.
     *
     * @param alvo    caixa do inimigo; a partir dela o resto do caminho nao importa
     * @param largura lado do projetil, em pixels de tela
     */
    public boolean caminhoDeTiro(double x1, double y1, java.awt.Rectangle alvo,
                                 double largura) {
        // Amostra de um quarto de tile: fino o bastante para nao pular a quina de
        // um bloco, grosso o bastante para nao custar caro a cada tiro.
        return caminhoDeTiro(x1, y1, alvo, largura, GameObject.SIZE() / 4d, this::solidoEm);
    }

    /**
     * Um projetil deste tamanho CABE neste ponto, sem encostar em bloco?
     *
     * E a pergunta que decide se a boca da arma serve como ponto de partida. A
     * ponta de um cajado fica acima da cabeca do gato; num corredor de um tile de
     * altura ela cai DENTRO do teto, e um tiro nascido ali morre no mesmo quadro.
     * Testar so uma linha fina ate a ponta nao pega isso — a linha passa por uma
     * fresta em que o projetil, que tem vinte e quatro pixels de lado, nao entra.
     */
    public boolean cabeEm(double x, double y, double largura) {
        double m = largura / 2d;
        return !(solidoEm(x, y)
                || solidoEm(x - m, y - m) || solidoEm(x + m, y - m)
                || solidoEm(x - m, y + m) || solidoEm(x + m, y + m));
    }

    /** Onde ha bloco solido. Existe para o teste poder montar um mapa de mentira. */
    public interface Solidez {
        boolean em(double telaX, double telaY);
    }

    /**
     * O caminhamento em si.
     *
     * Recebe o passo e a solidez de fora em vez de ler o tamanho do tile e o mapa
     * de variaveis globais: assim a regra pode ser verificada em teste, que e onde
     * ela precisa estar — foi justamente esta conta que deixou meia duzia de armas
     * inuteis contra qualquer bicho encostado num muro.
     *
     * @param passo distancia entre amostras, em pixels de tela
     */
    public static boolean caminhoDeTiro(double x1, double y1, java.awt.Rectangle alvo,
                                        double largura, double passoDesejado,
                                        Solidez solido) {
        double x2 = alvo.getCenterX();
        double y2 = alvo.getCenterY();
        double passo = Math.max(1, passoDesejado);
        double distancia = Math.hypot(x2 - x1, y2 - y1);
        int amostras = (int) Math.max(1, distancia / passo);
        double meiaLargura = largura / 2d;
        for (int i = 1; i < amostras; i++) {
            double t = i / (double) amostras;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            if (alvo.contains(x, y)) {
                return true;                 // chegou: o que vem depois e o alvo
            }
            if (solido.em(x, y)
                    || solido.em(x - meiaLargura, y - meiaLargura)
                    || solido.em(x + meiaLargura, y - meiaLargura)
                    || solido.em(x - meiaLargura, y + meiaLargura)
                    || solido.em(x + meiaLargura, y + meiaLargura)) {
                return false;
            }
        }
        return true;
    }

    private boolean solidoEm(double telaX, double telaY) {
        int tx = (int) Math.floor(telaX / GameObject.SIZE());
        int ty = (int) Math.floor(telaY / GameObject.SIZE());
        if (tx < 0 || ty < 0 || tx >= this.length) {
            return true;                 // fora do mapa conta como bloqueado
        }
        int i = tx + ty * this.length;
        Tile[] tiles = getMap();
        if (i < 0 || i >= tiles.length) {
            return true;
        }
        return tiles[i].isSolid();
    }

    public Tile getTile(int x, int y) {
        try {
            return getMap()[x + y * length];
        }catch(IndexOutOfBoundsException ignore) { }
        try {
            x /= GameObject.SIZE();
            y /= GameObject.SIZE();
            return getMap()[x + y * length];
        }catch (IndexOutOfBoundsException ignore) { }
        return Tile.build(TileIDs.Void.ordinal());
    }

    public void depth() {
        entities.sort(Entity.Depth);
        particles.sort(Particle.Depth);
    }

    public List<Entity> getEntities() {
        return List.copyOf(this.entities);
    }

    public <T extends Entity> List<T> getEntities(Class<T> type) {
        List<T> list = new ArrayList<>();
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(type.isInstance(e)) {
                list.add((T) e);
            }
        }
        return list;
    }

    public List<Particle> getParticles() {
        if (particles == null) {
            this.particles = new ArrayList<>(); // evita erro de ponteiro
        }
        return List.copyOf(this.particles);
    }

    public void put(Entity e) {
        entities.add(e);
    }

    public void putAll(List<Entity> e) {
        this.entities.addAll(e);
    }

    public boolean put(Particle p) {
        return this.particles.add(p);
    }

    public void remove(Entity e) {
        this.entities.remove(e);
    }

    public void remove(Particle p) {
        this.particles.remove(p);
    }

    public abstract void tick();

    public Rectangle getBounds() {
        return this.bounds;
    }

    public void dispose() {
        this.repulsion.dispose();
    }


    public static boolean mouseOnRect(Rectangle bounds) {
        int mouseX = Mouse.getX();
        int mouseY = Mouse.getY();

        Point2D.Float screenPoint = new Point2D.Float(mouseX, mouseY);
        Point2D.Float worldPoint = new Point2D.Float();

        try {

            AffineTransform inverse = Camera.getAt().createInverse(); // Inverso da transformação usada no render
            inverse.transform(screenPoint, worldPoint);
        } catch (NoninvertibleTransformException ex) {
            ex.printStackTrace();
        }

        return bounds.contains(worldPoint.x, worldPoint.y);
    }

    public static boolean clickOnRect(Mouse_Button button, Rectangle rec) {
        return mouseOnRect(rec) && Mouse.click(button);
    }

    public static boolean pressingOnRect(Mouse_Button button, Rectangle rec) {
        return mouseOnRect(rec) && Mouse.pressing(button);
    }

}
