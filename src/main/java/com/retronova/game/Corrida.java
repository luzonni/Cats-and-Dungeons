package com.retronova.game;

import com.retronova.game.interfaces.Inventory;
import com.retronova.game.interfaces.Slot;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.game.objects.entities.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A corrida interrompida, gravada em disco para poder ser retomada.
 *
 * ---------------------------------------------------------------------------
 * O QUE ISTO É, E O QUE NÃO É
 *
 * O gênero tem DOIS tipos de save, e confundi-los é o erro clássico. Um é a
 * META-PROGRESSÃO: o que sobrevive à morte e deixa a próxima corrida mais forte
 * — o Espelho do Hades, as plantas do Dead Cells. O outro é o SUSPEND SAVE: o
 * "salvar e sair" do Nethack e do Angband, que existe só para você poder fechar
 * o jogo e voltar depois, e não dá poder nenhum.
 *
 * Isto aqui é o SEGUNDO, e só ele. Não há moeda permanente, não há desbloqueio,
 * nada atravessa a morte. O jogo continua sendo um roguelike; o que ele ganha é
 * a possibilidade de ser jogado por quem não tem uma hora livre de uma vez.
 *
 * ---------------------------------------------------------------------------
 * POR QUE O ARQUIVO É APAGADO AO SER LIDO
 *
 * É o comportamento dos roguelikes desde sempre, e o motivo é preciso: o que
 * impede save-scum não é restringir o SALVAR, é restringir o CARREGAR. Um save
 * que sobrevive ao carregamento vira um "desfazer" — morreu, fecha o jogo, abre
 * de novo, tenta outra vez. A permadeath deixa de existir sem que ninguém tenha
 * tocado nela.
 *
 * O preço é conhecido e é assumido: se o jogo travar logo depois de retomar, a
 * corrida foi. É a mesma troca que o gênero inteiro faz.
 *
 * ---------------------------------------------------------------------------
 * POR QUE SÓ ENTRE SALAS — a decisão que torna isto barato
 *
 * Gravar no meio de uma onda obrigaria a serializar cada inimigo, cada projétil,
 * cada partícula e cada vetor de força do Physical. Seria muito código, seria
 * frágil, e quebraria de novo a cada arma nova que alguém escrevesse.
 *
 * Entre salas não é preciso nada disso, porque a arena é DETERMINÍSTICA: a
 * planta sai de {@code Arena.planta(dificuldade, nivel)}, que indexa a lista de
 * seis mapas pelo nível, e a onda sai de {@code Waves}, que lê o JSON pelo mesmo
 * índice. Não há semente aleatória em nenhum dos dois. Então o save não precisa
 * descrever o mundo — precisa dizer QUAL mundo, e o jogo o reconstrói igual.
 *
 * Sobra uma ficha de umas dez linhas: quem é o gato, em que nível e dificuldade
 * ele está, e como ele está. É a mesma escolha do Hades, que grava ao ENTRAR em
 * cada sala em vez de tentar fotografar o combate.
 *
 * O QUE FICA DE FORA, de propósito: posição dos bichos, projéteis no ar,
 * partículas e o estado do gerador aleatório. Tudo isso é re-sorteado, e a
 * consequência honesta é que fechar o jogo no meio de uma briga devolve o
 * jogador ao COMEÇO daquela sala, não ao instante em que ele parou.
 *
 * ---------------------------------------------------------------------------
 * QUANDO GRAVA
 *
 * Em dois pontos, os mesmos do Hades:
 *
 *   1. ao entrar numa sala nova ({@code Game.changeMap});
 *   2. DEPOIS de a carta de recompensa ser escolhida.
 *
 * O segundo não é detalhe. Gravando antes da escolha, fechar e reabrir o jogo
 * re-sortearia a oferta de três cartas até vir a que se queria — o save viraria
 * uma máquina de re-rolagem. Gravando depois, a escolha já está no inventário e
 * não há o que re-sortear.
 *
 * ---------------------------------------------------------------------------
 * TRÊS ESPAÇOS, E O QUE ISSO **NÃO** SIGNIFICA
 *
 * Cada espaço guarda uma CORRIDA DIFERENTE, e não um ponto diferente da mesma
 * corrida. É a distinção que mantém tudo o que está escrito acima de pé.
 *
 * Se os três fossem pontos da mesma partida, o save-scum voltaria inteiro pela
 * porta da frente: bastaria guardar uma cópia antes de uma sala perigosa e
 * recarregá-la ao morrer. Três corridas paralelas não permitem isso — é o mesmo
 * que ter três personagens no Nethack, cada um com o próprio destino.
 *
 * O que garante a separação é o jogador NUNCA ESCOLHER onde gravar. O espaço é
 * escolhido uma vez, ao começar a corrida, e a partir daí ela grava sempre no
 * seu e em nenhum outro. Sem escolha na hora de gravar, não há como ramificar.
 */
public final class Corrida {

    /** Quantos espaços de save existem. */
    public static final int ESPACOS = 3;

    /** Onde os saves moram, ao lado do config.json. */
    private static final String PASTA = "saves";

    /** O arquivo do espaço, contando de 1 como o jogador conta. */
    private static Path caminho(int espaco) {
        return Paths.get(PASTA, "corrida-" + espaco + ".json");
    }

    private static boolean espacoValido(int espaco) {
        return espaco >= 1 && espaco <= ESPACOS;
    }

    /**
     * O save da versão de um espaço só, promovido ao primeiro espaço.
     *
     * Quem estava com uma corrida em andamento quando os espaços chegaram não pode
     * perdê-la por causa de uma mudança de arrumação do disco — perder progresso
     * numa atualização é o tipo de coisa que se lembra por muito tempo. Roda uma
     * vez: depois da mudança o arquivo antigo não existe mais e a checagem custa
     * um {@code Files.exists}.
     */
    private static void migrarOSaveAntigo() {
        Path antigo = Paths.get("corrida.json");
        if (!Files.exists(antigo)) {
            return;
        }
        try {
            Path novo = caminho(1);
            Files.createDirectories(novo.getParent());
            if (Files.exists(novo)) {
                Files.delete(antigo);   // o espaço 1 já tem dono; o antigo virou lixo
                return;
            }
            Files.move(antigo, novo, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Corrida: save antigo movido para o espaço 1.");
        } catch (IOException e) {
            System.err.println("Corrida: não consegui mover o save antigo: " + e);
        }
    }

    /**
     * Formato do arquivo.
     *
     * Existe para que uma versão futura do jogo possa RECUSAR um save antigo em
     * vez de tentar lê-lo e quebrar no meio. Um save de formato desconhecido é
     * descartado em silêncio, e o jogador simplesmente não vê o botão de
     * continuar — que é muito melhor do que uma corrida retomada pela metade.
     */
    private static final long VERSAO = 1;

    private final int gato;
    private final int nivel;
    private final int dificuldade;
    private final long segundos;

    private final double vida;
    private final double xp;
    private final int nivelDoGato;
    private final int dinheiro;

    /** Pares {id do item, quantidade}. Vazio é representado por ausência. */
    private final List<int[]> hotbar;
    private final List<int[]> mochila;
    private final List<int[]> passivos;

    /**
     * As melhorias escolhidas, em {ordinal, quantidade, soma x1000}.
     *
     * A VERSAO DO FORMATO NAO SUBIU, e isso e deliberado. Um campo novo AUSENTE tem
     * um significado correto — "esta corrida nao tem melhoria nenhuma" — e e
     * exatamente o que vale para toda corrida gravada antes do sistema existir.
     * Subir a versao recusaria esses saves, e recusar o save de quem esta jogando
     * para acrescentar um campo opcional e caro sem precisar.
     */
    private final List<int[]> melhorias;

    /**
     * O elemento da corrida, pelo ordinal. Campo ausente significa NENHUM, que e
     * verdade para toda corrida gravada antes de a escolha existir.
     */
    private final int elemento;

    private Corrida(int gato, int nivel, int dificuldade, long segundos, double vida,
                    double xp, int nivelDoGato, int dinheiro, List<int[]> hotbar,
                    List<int[]> mochila, List<int[]> passivos, List<int[]> melhorias,
                    int elemento) {
        this.gato = gato;
        this.nivel = nivel;
        this.dificuldade = dificuldade;
        this.segundos = segundos;
        this.vida = vida;
        this.xp = xp;
        this.nivelDoGato = nivelDoGato;
        this.dinheiro = dinheiro;
        this.hotbar = hotbar;
        this.mochila = mochila;
        this.passivos = passivos;
        this.melhorias = melhorias;
        this.elemento = elemento;
    }

    public int gato() {
        return this.gato;
    }

    public int nivel() {
        return this.nivel;
    }

    public int dificuldade() {
        return this.dificuldade;
    }

    public long segundos() {
        return this.segundos;
    }

    /**
     * Como esta corrida se descreve para quem vai decidir se a retoma.
     *
     * MORA AQUI, e nao no menu, porque e conhecimento sobre os proprios campos: o
     * nivel comeca em zero e a sala que o jogador conta comeca em um, e a
     * dificuldade e um indice que so este lado sabe traduzir. Deixar essa conversao
     * na tela espalharia a regra por quem quer que venha a mostrar um save.
     */
    public String[] resumo() {
        return new String[]{nomeDoGato(), descricao() + "     " + tempo()};
    }

    /** Só o nome do gato. */
    public String nomeDoGato() {
        return gato >= 0 && gato < Player.TEMPLATES.length
                ? Player.TEMPLATES[gato].getName() : "?";
    }

    /** Onde a corrida parou, sem o tempo. */
    public String descricao() {
        // O SEPARADOR E UM HIFEN, e nao o ponto do meio: a fonte do jogo nao tem o
        // glifo do ponto medio, e no lugar dele sai o retangulo de caractere
        // ausente. Ja tinha acontecido na ficha de item.
        return dificuldadeEmPalavra() + " - Room " + (nivel + 1);
    }

    /** Quanto tempo esta corrida ja durou. */
    public String tempo() {
        return relogio(segundos);
    }

    private String dificuldadeEmPalavra() {
        return switch (dificuldade) {
            case 1 -> "Normal";
            case 2 -> "Hard";
            default -> "Easy";
        };
    }

    /**
     * O tempo jogado, com a hora aparecendo só quando existe.
     *
     * Sem a separação de horas, uma corrida de duas horas virava "134:12" — que se
     * lê como duas horas erradas antes de se ler como cento e trinta e quatro
     * minutos. E pôr "0:00:47" numa corrida de um minuto seria o desperdício
     * oposto: dois dígitos que são zero em quase toda partida.
     */
    private static String relogio(long segundos) {
        long total = Math.max(0, segundos);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%d:%02d", m, s);
    }

    // ---------------------------------------------------------------- gravar

    /**
     * Fotografa a partida viva e grava.
     *
     * Silenciosa por opção: um checkpoint que falha não pode interromper o jogo
     * com uma caixa de erro no meio de uma corrida. O erro vai para o console, o
     * jogo segue, e o pior caso é o jogador perder a possibilidade de retomar —
     * que é exatamente onde ele já estava antes deste arquivo existir.
     */
    public static void gravar() {
        try {
            Game jogo = Game.getGame();
            Player gato = Game.getPlayer();
            if (gato == null || gato.morrendo()) {
                return;             // morto não se retoma
            }
            de(jogo, gato).escrever(jogo.getEspaco());
        } catch (RuntimeException fora) {
            // Game.getGame() lança quando a activity atual não é o jogo. Acontece
            // em transição de tela, e ali não há o que gravar.
        }
    }

    private static Corrida de(Game jogo, Player gato) {
        Inventory inv = gato.getInventory();
        return new Corrida(
                jogo.getIndexPlayer(), jogo.getLevel(), jogo.getDifficult(),
                jogo.getSeconds(), gato.getLife(), gato.getXp(), gato.getLevel(),
                gato.getMoney(),
                doSlots(inv.getHotbar(), inv.getHotbarSize()),
                doSlots(inv.getBag(), inv.getBagSize()),
                dosPassivos(gato),
                List.of(gato.getMelhorias().comoNumeros()),
                gato.elemento().ordinal());
    }

    private static List<int[]> doSlots(Slot[] slots, int quantos) {
        List<int[]> saida = new ArrayList<>();
        for (int i = 0; i < quantos && i < slots.length; i++) {
            Slot slot = slots[i];
            if (slot == null || slot.isEmpty()) {
                continue;
            }
            Item item = slot.item();
            int pilha = item instanceof Consumable c ? c.getStack() : 1;
            saida.add(new int[]{i, item.getID(), pilha});
        }
        return saida;
    }

    private static List<int[]> dosPassivos(Player gato) {
        List<int[]> saida = new ArrayList<>();
        for (Consumable c : gato.getPassives()) {
            saida.add(new int[]{0, c.getID(), c.getStack()});
        }
        return saida;
    }

    @SuppressWarnings("unchecked")
    private void escrever(int espaco) {
        if (!espacoValido(espaco)) {
            return;
        }
        JSONObject o = new JSONObject();
        o.put("versao", VERSAO);
        o.put("gato", (long) gato);
        o.put("nivel", (long) nivel);
        o.put("dificuldade", (long) dificuldade);
        o.put("segundos", segundos);
        o.put("vida", vida);
        o.put("xp", xp);
        o.put("nivelDoGato", (long) nivelDoGato);
        o.put("dinheiro", (long) dinheiro);
        o.put("hotbar", paraJson(hotbar));
        o.put("mochila", paraJson(mochila));
        o.put("passivos", paraJson(passivos));
        o.put("melhorias", paraJson(melhorias));
        o.put("elemento", (long) elemento);

        // A MESMA GRAVAÇÃO ATÔMICA DO Configs, e pelo mesmo motivo: escrever por
        // cima do arquivo real deixa um JSON pela metade se o processo morrer no
        // meio. Num arquivo de preferências isso custa as preferências; aqui
        // custaria a corrida inteira, e justamente de quem fechou o jogo — que é o
        // único momento em que este arquivo é escrito.
        Path destino = caminho(espaco);
        Path temporario = destino.resolveSibling(destino.getFileName() + ".tmp");
        try {
            Files.createDirectories(destino.getParent());
            Files.writeString(temporario, o.toJSONString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporario, destino,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException semAtomico) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Corrida: falha ao gravar o ponto de retomada: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONArray paraJson(List<int[]> pares) {
        JSONArray fora = new JSONArray();
        for (int[] p : pares) {
            JSONArray dentro = new JSONArray();
            for (int v : p) {
                dentro.add((long) v);
            }
            fora.add(dentro);
        }
        return fora;
    }

    // ------------------------------------------------------------------ ler

    /** Há corrida guardada neste espaço? */
    public static boolean existe(int espaco) {
        migrarOSaveAntigo();
        return espacoValido(espaco) && Files.exists(caminho(espaco));
    }

    /** Algum dos espaços está ocupado? Decide se "Play" precisa avisar antes. */
    public static boolean existeAlguma() {
        for (int i = 1; i <= ESPACOS; i++) {
            if (existe(i)) {
                return true;
            }
        }
        return false;
    }

    /** O primeiro espaço livre, ou -1 se os três estiverem ocupados. */
    public static int primeiroLivre() {
        for (int i = 1; i <= ESPACOS; i++) {
            if (!existe(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Lê SEM apagar, só para mostrar na tela.
     *
     * São dois verbos diferentes de propósito. Descrever um save no diálogo de
     * confirmação não pode consumi-lo — quem abre a pergunta e responde "não" tem
     * de continuar com a corrida intacta. O consumo pertence ao momento em que a
     * partida de fato começa, e a nenhum outro.
     */
    public static Corrida espiar(int espaco) {
        if (!espacoValido(espaco)) {
            return null;
        }
        migrarOSaveAntigo();
        Path arquivo = caminho(espaco);
        if (!Files.exists(arquivo)) {
            return null;
        }
        try {
            return interpretar(Files.readString(arquivo, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;            // consumir() dirá o que houve, e apagará
        }
    }

    /**
     * Lê e APAGA o arquivo. Ver o cabeçalho da classe para o porquê do apagar.
     *
     * Apaga mesmo quando a leitura falha: um arquivo que não abre não vai abrir na
     * próxima tentativa, e deixá-lo no disco só faria o botão de continuar aparecer
     * para sempre sem nunca funcionar.
     */
    public static Corrida consumir(int espaco) {
        if (!espacoValido(espaco)) {
            return null;
        }
        migrarOSaveAntigo();
        Path arquivo = caminho(espaco);
        if (!Files.exists(arquivo)) {
            return null;
        }
        Corrida lida = null;
        try {
            String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
            lida = interpretar(conteudo);
        } catch (Exception e) {
            System.err.println("Corrida: save ilegível, descartado. " + e);
        }
        apagar(espaco);
        return lida;
    }

    private static Corrida interpretar(String conteudo) throws Exception {
        if (conteudo.startsWith("﻿")) {
            conteudo = conteudo.substring(1);       // BOM, mesmo caso do Configs
        }
        JSONObject o = (JSONObject) new JSONParser().parse(conteudo);
        if (inteiro(o, "versao", -1) != VERSAO) {
            System.err.println("Corrida: formato desconhecido, save descartado.");
            return null;
        }
        return new Corrida(
                inteiro(o, "gato", 0),
                inteiro(o, "nivel", 0),
                inteiro(o, "dificuldade", 0),
                (long) inteiro(o, "segundos", 0),
                real(o, "vida", 1),
                real(o, "xp", 0),
                inteiro(o, "nivelDoGato", 0),
                inteiro(o, "dinheiro", 100),
                doJson(o.get("hotbar")),
                doJson(o.get("mochila")),
                doJson(o.get("passivos")),
                doJson(o.get("melhorias")),
                inteiro(o, "elemento", 0));
    }

    private static int inteiro(JSONObject o, String chave, int padrao) {
        Object v = o.get(chave);
        return v instanceof Number n ? n.intValue() : padrao;
    }

    private static double real(JSONObject o, String chave, double padrao) {
        Object v = o.get(chave);
        return v instanceof Number n ? n.doubleValue() : padrao;
    }

    private static List<int[]> doJson(Object bruto) {
        List<int[]> saida = new ArrayList<>();
        if (!(bruto instanceof JSONArray fora)) {
            return saida;
        }
        for (Object linha : fora) {
            if (!(linha instanceof JSONArray dentro) || dentro.size() < 3) {
                continue;
            }
            int[] p = new int[3];
            for (int i = 0; i < 3; i++) {
                p[i] = dentro.get(i) instanceof Number n ? n.intValue() : 0;
            }
            saida.add(p);
        }
        return saida;
    }

    public static void apagar(int espaco) {
        if (!espacoValido(espaco)) {
            return;
        }
        try {
            Path arquivo = caminho(espaco);
            Files.deleteIfExists(arquivo);
            Files.deleteIfExists(arquivo.resolveSibling(arquivo.getFileName() + ".tmp"));
        } catch (IOException e) {
            System.err.println("Corrida: falha ao apagar o save: " + e);
        }
    }

    // --------------------------------------------------------------- aplicar

    /**
     * Devolve o gato ao estado gravado.
     *
     * Os IDs viram itens de novo por {@code Item.build}, e não são desserializados
     * como objetos: a arma é código, não dado. Guardar o objeto obrigaria a manter
     * a forma interna de cada classe de item estável para sempre, e uma refatoração
     * inocente invalidaria todos os saves do mundo. Guardando só o ID, um item que
     * mude por dentro continua sendo o mesmo item ao voltar.
     */
    public void aplicarNoGato(Player gato) {
        // AS MELHORIAS PRIMEIRO, E A VIDA DEPOIS.
        //
        // A ordem nao e arrumacao: melhoria de vigor sobe a vida MAXIMA, e
        // {@code setLife} recusa qualquer valor acima do maximo do momento. Repondo
        // a vida antes, um gato que tinha cento e trinta de vida por causa das
        // cartas voltaria com os oitenta de fabrica, e a corrida retomada seria
        // mais fraca do que a fechada — sem aviso nenhum.
        // O ELEMENTO ANTES DE TUDO. Ele reconstroi o inventario inteiro — a espada
        // vira a espada de gelo, com outro dano e outro desenho —, entao repor os
        // itens primeiro faria o trabalho duas vezes e, pior, a reconstrucao
        // apagaria as pilhas que acabaram de ser repostas.
        com.retronova.game.items.Elemento[] elementos =
                com.retronova.game.items.Elemento.values();
        if (elemento >= 0 && elemento < elementos.length) {
            gato.reporElemento(elementos[elemento]);
        }
        gato.getMelhorias().deNumeros(gato, melhorias.toArray(new int[0][]));
        gato.setLife(vida);
        gato.setXpBruto(xp, nivelDoGato);
        gato.setMoney(dinheiro);

        Inventory inv = gato.getInventory();
        inv.esvaziar();
        repor(inv.getHotbar(), hotbar);
        repor(inv.getBag(), mochila);
        for (int[] p : passivos) {
            gato.reporPassivo(p[1], p[2]);
        }
    }

    private static void repor(Slot[] slots, List<int[]> pares) {
        for (int[] p : pares) {
            int indice = p[0];
            if (indice < 0 || indice >= slots.length || slots[indice] == null) {
                continue;
            }
            slots[indice].put(Item.build(p[1], p[2]));
        }
    }
}
