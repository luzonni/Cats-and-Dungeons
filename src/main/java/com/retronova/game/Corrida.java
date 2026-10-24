package com.retronova.game;

import com.retronova.game.interfaces.Inventory;
import com.retronova.game.interfaces.Slot;
import com.retronova.game.items.Consumable;
import com.retronova.game.items.Item;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.arena.Waves;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.EntityIDs;
import com.retronova.game.objects.entities.Player;
import com.retronova.game.objects.entities.enemies.Chefe;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * Isto aqui é o SEGUNDO, e só ele. A meta-progressão mora no {@link Meta}, em
 * arquivo próprio; nada daqui atravessa a morte. O que a corrida ganha é a
 * possibilidade de ser jogada por quem não tem meia hora livre de uma vez.
 *
 * ---------------------------------------------------------------------------
 * POR QUE O ARQUIVO É APAGADO AO SER LIDO
 *
 * É o comportamento dos roguelikes desde sempre, e o motivo é preciso: o que
 * impede save-scum não é restringir o SALVAR, é restringir o CARREGAR. Um save
 * que sobrevive ao carregamento vira um "desfazer" — morreu, fecha o jogo, abre
 * de novo, tenta outra vez. A permadeath deixa de existir sem que ninguém tenha
 * tocado nela. A partida retomada regrava o próprio save no primeiro tick (ver
 * {@code Game.regravarSave}), então quem fecha logo depois não perde nada.
 *
 * ---------------------------------------------------------------------------
 * O QUE É GRAVADO, E O QUE NÃO É
 *
 * O GATO INTEIRO E O RELÓGIO DA CORRIDA. O gato é vida, XP, nível, inventário,
 * cartas, passivas, armas extras, evoluções e elemento. O relógio é o minuto da
 * corrida, os abates, qual chefe vem a seguir, se o Rato Rei já nasceu e o
 * compasso do cerco ({@link Waves.Estado}) — mais o que só existe por corrida: a
 * carga da Fúria, as trocas e pulos que restam nas cartas de nível, os números
 * da tela final e a vitória, se já houve.
 *
 * OS BICHOS NÃO SÃO GRAVADOS, com uma exceção. Serializar cada inimigo, cada
 * projétil, cada partícula e cada vetor de força seria muito código, frágil, e
 * quebraria de novo a cada arma nova. E não é preciso: a horda é função do
 * relógio, e se refaz em segundos a partir dele. A exceção são os CHEFES vivos,
 * gravados pelo tipo e pela fração de vida — sem eles, fechar o jogo seria um
 * jeito de pular chefe, porque o relógio retomado já passou do minuto em que
 * ele nasce.
 *
 * O campo também não é gravado. Ele é sorteado de novo ao retomar; é um campo
 * aberto, e o que muda são manchas de chão e braseiros.
 *
 * ---------------------------------------------------------------------------
 * QUANDO GRAVA
 *
 *   1. ao entrar no campo ({@code Game.changeMap});
 *   2. DEPOIS de cada escolha que não pode ser re-sorteada: o elemento, a carta
 *      de nível, o prêmio do caça-níquel;
 *   3. a cada minuto de corrida ({@code Arena}), para um fechamento à força não
 *      custar mais que isso;
 *   4. ao sair pela pausa ("Main Menu" ou "Quit").
 *
 * O segundo ponto não é detalhe. Gravando antes da escolha, fechar e reabrir o
 * jogo re-sortearia as cartas até vir a que se queria — o save viraria uma
 * máquina de re-rolagem. Gravando depois, a escolha já está no gato.
 *
 * ---------------------------------------------------------------------------
 * TRÊS ESPAÇOS, E O QUE ISSO **NÃO** SIGNIFICA
 *
 * Cada espaço guarda uma CORRIDA DIFERENTE, e não um ponto diferente da mesma
 * corrida. É a distinção que mantém tudo o que está escrito acima de pé.
 *
 * Se os três fossem pontos da mesma partida, o save-scum voltaria inteiro pela
 * porta da frente: bastaria guardar uma cópia antes de um chefe e recarregá-la
 * ao morrer. Três corridas paralelas não permitem isso — é o mesmo que ter três
 * personagens no Nethack, cada um com o próprio destino.
 *
 * O que garante a separação é o jogador NUNCA ESCOLHER onde gravar. O espaço é
 * escolhido uma vez, ao começar a corrida, e a partir daí ela grava sempre no
 * seu e em nenhum outro. Sem escolha na hora de gravar, não há como ramificar.
 */
public final class Corrida {

    /** Quantos espaços de save existem. */
    public static final int ESPACOS = 3;

    /** Onde os saves moram, ao lado do config.json. */
    private static String PASTA = com.retronova.engine.Foto.pastaDeSaves();
    private static Path legado = Paths.get("corrida.json");

    static void usarPasta(Path pasta) {
        PASTA = pasta.toString();
        legado = pasta.resolve("corrida.json");
    }

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
        if (com.retronova.engine.Foto.ligado()) {
            return;
        }
        Path antigo = legado;
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
     *
     * A VERSÃO NÃO SOBE QUANDO UM CAMPO NOVO TEM UM AUSENTE CORRETO. O andamento
     * da corrida entrou assim: um save sem ele é o de quem gravou no começo da
     * corrida, e "relógio zerado, nenhum chefe ainda" é exatamente o que ele
     * descrevia. Os campos {@code nivel} e {@code dificuldade}, do tempo das salas,
     * são ignorados ao ler.
     *
     * VERSÃO 2: o {@code id} da corrida, que a progressão permanente usa para
     * creditar as moedas uma vez só (ver save/Progresso). Um save da versão 1 é
     * lido normalmente e ganha um id novo na leitura: ele nunca foi creditado,
     * porque o crédito por id não existia.
     */
    private static final long VERSAO = 2;

    /** O id desta corrida. Ver o comentário de VERSAO. */
    private final String id;

    public String id() {
        return this.id;
    }

    /** Um chefe que estava vivo quando a corrida foi gravada. */
    public record ChefeVivo(EntityIDs tipo, double fracaoDeVida) {
    }

    /**
     * Tudo o que é da CORRIDA e não do gato.
     *
     * @param relogio         o estado do Waves
     * @param chefes          os chefes vivos no momento da gravação
     * @param niveisPendentes níveis ganhos cuja carta ainda não foi escolhida
     * @param trocas          trocas de mão que restam nas cartas de nível
     * @param pulos           pulos que restam nas cartas de nível
     * @param furia           carga da Fúria Felina, de 0 a 1
     * @param baus            baús abertos, para a tela final e as conquistas
     * @param maiorHorda      a maior horda vista, idem
     * @param venceu          o Rato Rei já caiu (o bônus já foi pago)
     * @param danoPorFonte    dano por arma, para a tela final
     */
    public record Andamento(Waves.Estado relogio, List<ChefeVivo> chefes, int niveisPendentes,
                            int trocas, int pulos, double furia, int baus, int maiorHorda,
                            boolean venceu, Map<String, Double> danoPorFonte) {
    }

    private final int gato;
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
     */
    private final List<int[]> melhorias;

    /**
     * O elemento da corrida, pelo ordinal. Campo ausente significa NENHUM, que e
     * verdade para toda corrida gravada antes de a escolha existir.
     */
    private final int elemento;

    private final Andamento andamento;

    private Corrida(String id, int gato, long segundos, double vida, double xp, int nivelDoGato,
                    int dinheiro, List<int[]> hotbar, List<int[]> mochila, List<int[]> passivos,
                    List<int[]> melhorias, int elemento, Andamento andamento) {
        this.id = id;
        this.gato = gato;
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
        this.andamento = andamento;
    }

    public int gato() {
        return this.gato;
    }

    public int nivelDoGato() {
        return this.nivelDoGato;
    }

    public long segundos() {
        return this.segundos;
    }

    public Andamento andamento() {
        return this.andamento;
    }

    /** Em que segundo do relógio da corrida ela parou. */
    public long segundosDaCorrida() {
        return andamento.relogio().ticks() / 60;
    }

    /** Como esta corrida se descreve para quem vai decidir se a retoma. */
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
        return "Level " + nivelDoGato;
    }

    /**
     * O minuto da corrida em que ela parou, que é o que diz quanto falta até o
     * Rato Rei. Um save sem relógio (gravado ao entrar no campo) mostra o tempo
     * jogado, contando a antecâmara.
     */
    public String tempo() {
        long daCorrida = segundosDaCorrida();
        return relogio(daCorrida > 0 ? daCorrida : segundos);
    }

    /**
     * O tempo, com a hora aparecendo só quando existe.
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
     * jogo segue, e o pior caso é o jogador perder a possibilidade de retomar.
     */
    public static void gravar() {
        try {
            gravarObrigatorio();
        } catch (RuntimeException falha) {
            System.err.println("[save] checkpoint nao gravado: " + falha);
        }
    }

    /** Saídas explícitas só prosseguem quando o checkpoint foi gravado. */
    public static void gravarObrigatorio() {
            Game jogo = Game.getGame();
            Player gato = Game.getPlayer();
            if (gato == null || gato.morrendo()) {
                return;             // morto não se retoma
            }
            // SÓ NO CAMPO. A retomada monta o campo direto; um save feito na
            // antecâmara pularia a antecâmara na volta. Ali a corrida ainda não
            // começou, e não há o que retomar.
            if (!(Game.getMap() instanceof Arena)) {
                return;
            }
            de(jogo, gato).escrever(jogo.getEspaco());
    }

    private static Corrida de(Game jogo, Player gato) {
        Inventory inv = gato.getInventory();
        return new Corrida(jogo.getIdDaCorrida(),
                jogo.getIndexPlayer(), jogo.getSeconds(), gato.getLife(), gato.getXp(), gato.getLevel(),
                gato.getMoney(),
                doSlots(inv.getHotbar(), inv.getHotbarSize()),
                doSlots(inv.getBag(), inv.getBagSize()),
                dosPassivos(gato),
                List.of(gato.getMelhorias().comoNumeros()),
                gato.elemento().ordinal(),
                andamentoDe(jogo, gato));
    }

    private static Andamento andamentoDe(Game jogo, Player gato) {
        Waves.Estado relogio = new Waves.Estado(0, 0, 1, false, 0);
        List<ChefeVivo> chefes = new ArrayList<>();
        if (Game.getMap() instanceof Arena arena) {
            relogio = arena.getWaves().estado();
            EntityIDs[] tipos = EntityIDs.values();
            for (Chefe chefe : arena.getEntities(Chefe.class)) {
                if (chefe.getLife() > 0 && chefe.getID() >= 0 && chefe.getID() < tipos.length) {
                    chefes.add(new ChefeVivo(tipos[chefe.getID()], chefe.getLife() / chefe.getLifeSize()));
                }
            }
        }
        return new Andamento(relogio, chefes, gato.niveisPendentes(),
                com.retronova.game.map.arena.SubirDeNivel.trocas(),
                com.retronova.game.map.arena.SubirDeNivel.pulos(),
                com.retronova.game.objects.entities.Furia.carga(),
                Estatisticas.baus(), Estatisticas.maiorHorda(), Estatisticas.venceu(),
                new LinkedHashMap<>(Estatisticas.danoPorFonte()));
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
        o.put("id", id);
        o.put("gato", (long) gato);
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
        o.put("andamento", andamentoParaJson(andamento));

        // A MESMA GRAVAÇÃO ATÔMICA DO Configs, e pelo mesmo motivo: escrever por
        // cima do arquivo real deixa um JSON pela metade se o processo morrer no
        // meio. Num arquivo de preferências isso custa as preferências; aqui
        // custaria a corrida inteira.
        Path destino = caminho(espaco);
        Path temporario = destino.resolveSibling(destino.getFileName() + ".tmp");
        try {
            com.retronova.game.save.ArquivoSeguro.gravar(destino, o.toJSONString(), Corrida::interpretar);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Falha ao gravar ponto de retomada", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject andamentoParaJson(Andamento a) {
        JSONObject o = new JSONObject();
        Waves.Estado r = a.relogio();
        o.put("ticks", r.ticks());
        o.put("abates", (long) r.abates());
        o.put("proximoChefe", (long) r.proximoChefe());
        o.put("reiNasceu", r.reiNasceu());
        o.put("ateOCerco", (long) r.ateOCerco());
        JSONArray chefes = new JSONArray();
        for (ChefeVivo c : a.chefes()) {
            JSONObject chefe = new JSONObject();
            // PELO NOME, e nao pelo ordinal: o EntityIDs perde e ganha entradas, e o
            // nome e o que os mapas ja usam para dizer qual entidade e qual.
            chefe.put("tipo", c.tipo().name());
            chefe.put("vida", c.fracaoDeVida());
            chefes.add(chefe);
        }
        o.put("chefes", chefes);
        o.put("niveisPendentes", (long) a.niveisPendentes());
        o.put("trocas", (long) a.trocas());
        o.put("pulos", (long) a.pulos());
        o.put("furia", a.furia());
        o.put("baus", (long) a.baus());
        o.put("maiorHorda", (long) a.maiorHorda());
        o.put("venceu", a.venceu());
        JSONObject dano = new JSONObject();
        dano.putAll(a.danoPorFonte());
        o.put("danoPorFonte", dano);
        return o;
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
        if (!Files.exists(arquivo) && !Files.exists(com.retronova.game.save.ArquivoSeguro.backup(arquivo))) {
            return null;
        }
        try {
            Corrida lida = com.retronova.game.save.ArquivoSeguro.ler(arquivo, Corrida::interpretar).valor();
            if (lida != null && com.retronova.game.save.Progresso.foiCreditada(lida.id)) {
                // ACABOU E JÁ FOI PAGA. O jogo fechou entre o crédito das moedas e o
                // apagar deste arquivo: retomar daria a corrida e as moedas de novo.
                apagar(espaco);
                return null;
            }
            return lida;
        } catch (com.retronova.game.save.ArquivoSeguro.VersaoIncompativel futura) {
            throw futura;
        } catch (java.io.UncheckedIOException inacessivel) {
            throw inacessivel;
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel consultar a corrida", e);
        }
    }

    /** Um id novo de corrida. */
    public static String novoId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * A corrida deste espaço foi ABANDONADA: o jogador começou outra por cima,
     * apagou o espaço ou recomeçou pela pausa. Credita a fração de abandono das
     * moedas (ver save/RegrasDeMoedas) e apaga o arquivo.
     *
     * O crédito é por id: se a corrida viva já foi creditada (o "Restart" da pausa
     * credita com os números ao vivo antes de apagar), aqui só se apaga.
     */
    public static void abandonar(int espaco) {
        Corrida c = espiar(espaco);
        if (c != null) {
            var regras = com.retronova.game.save.RegrasDeMoedas.atuais();
            int segundos = (int) c.segundosDaCorrida();
            int abates = c.andamento.relogio().abates();
            var resultado = c.andamento.venceu()
                    ? com.retronova.game.save.RegrasDeMoedas.Resultado.VITORIA
                    : com.retronova.game.save.RegrasDeMoedas.Resultado.ABANDONO;
            int moedas = regras.moedas(resultado, segundos, abates, ganancia(c.nomeDoGato()));
            com.retronova.game.save.Progresso.creditar(c.id, c.nomeDoGato(), moedas);
        }
        apagar(espaco);
    }

    /**
     * O multiplicador de moedas do gato. Um ponto só, para o espelho (etapa 2)
     * entrar aqui sem que quem credita precise saber dele.
     */
    public static double ganancia(String gato) {
        return com.retronova.game.espelho.Espelho.ganancia(gato);
    }

    /**
     * Lê e APAGA o arquivo. Ver o cabeçalho da classe para o porquê do apagar.
     *
     * O backup é tentado antes de descartar; formatos futuros e falhas de acesso
     * são preservados. Corrupção é separada em .corrompido. Um arquivo ruim na
     * próxima tentativa, e deixá-lo no disco só faria o botão de continuar aparecer
     * para sempre sem nunca funcionar.
     */
    public static Corrida consumir(int espaco) {
        if (!espacoValido(espaco)) {
            return null;
        }
        Corrida lida = espiar(espaco);
        apagar(espaco);
        return lida;
    }

    private static Corrida interpretar(String conteudo) throws Exception {
        if (conteudo.startsWith("﻿")) {
            conteudo = conteudo.substring(1);       // BOM, mesmo caso do Configs
        }
        JSONObject o = (JSONObject) new JSONParser().parse(conteudo);
        int versao = inteiro(o, "versao", -1);
        if (versao != 1 && versao != VERSAO) {
            throw new com.retronova.game.save.ArquivoSeguro.VersaoIncompativel(versao);
        }
        // Versão 1 não tinha id: ganha um agora (ver VERSAO).
        String id = o.get("id") instanceof String s && !s.isEmpty() ? s
                : java.util.UUID.nameUUIDFromBytes(conteudo.getBytes(StandardCharsets.UTF_8)).toString();
        return new Corrida(id,
                inteiro(o, "gato", 0),
                (long) inteiro(o, "segundos", 0),
                real(o, "vida", 1),
                real(o, "xp", 0),
                inteiro(o, "nivelDoGato", 0),
                inteiro(o, "dinheiro", 100),
                doJson(o.get("hotbar")),
                doJson(o.get("mochila")),
                doJson(o.get("passivos")),
                doJson(o.get("melhorias")),
                inteiro(o, "elemento", 0),
                andamentoDoJson(o.get("andamento")));
    }

    /**
     * O andamento lido, com o padrão de cada campo sendo o de uma corrida que
     * acabou de começar: um save sem ele é o de quem gravou ao entrar no campo.
     */
    private static Andamento andamentoDoJson(Object bruto) {
        JSONObject o = bruto instanceof JSONObject j ? j : new JSONObject();
        Waves.Estado relogio = new Waves.Estado(
                o.get("ticks") instanceof Number n ? n.longValue() : 0,
                inteiro(o, "abates", 0),
                inteiro(o, "proximoChefe", 1),
                Boolean.TRUE.equals(o.get("reiNasceu")),
                inteiro(o, "ateOCerco", 0));
        List<ChefeVivo> chefes = new ArrayList<>();
        if (o.get("chefes") instanceof JSONArray lista) {
            for (Object item : lista) {
                if (item instanceof JSONObject c && c.get("tipo") instanceof String nome) {
                    for (EntityIDs tipo : EntityIDs.values()) {
                        if (tipo.name().equals(nome)) {
                            chefes.add(new ChefeVivo(tipo, real(c, "vida", 1)));
                        }
                    }
                }
            }
        }
        Map<String, Double> dano = new LinkedHashMap<>();
        if (o.get("danoPorFonte") instanceof JSONObject d) {
            for (Object chave : d.keySet()) {
                if (d.get(chave) instanceof Number n) {
                    dano.put(String.valueOf(chave), n.doubleValue());
                }
            }
        }
        return new Andamento(relogio, chefes,
                inteiro(o, "niveisPendentes", 0),
                inteiro(o, "trocas", -1),
                inteiro(o, "pulos", -1),
                real(o, "furia", 0),
                inteiro(o, "baus", 0),
                inteiro(o, "maiorHorda", 0),
                Boolean.TRUE.equals(o.get("venceu")),
                dano);
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
            Files.deleteIfExists(com.retronova.game.save.ArquivoSeguro.backup(arquivo));
            Files.deleteIfExists(arquivo.resolveSibling(arquivo.getFileName() + ".tmp"));
        } catch (IOException e) {
            System.err.println("Corrida: falha ao apagar o save: " + e);
        }
    }

    // --------------------------------------------------------------- aplicar

    /**
     * Devolve o gato ao estado gravado.
     *
     * Roda DEPOIS dos bônus de início (o do gato e os upgrades permanentes, ver
     * {@code Game.retomar}). A ordem é o que impede a retomada de curar: o upgrade
     * de vida enche a barra ao ser aplicado, e só a vida gravada, reposta por
     * último, diz quanto o gato tinha de fato.
     *
     * Os IDs viram itens de novo por {@code Item.build}, e não são desserializados
     * como objetos: a arma é código, não dado. Guardar o objeto obrigaria a manter
     * a forma interna de cada classe de item estável para sempre, e uma refatoração
     * inocente invalidaria todos os saves do mundo.
     */
    public void aplicarNoGato(Player gato) {
        // O ELEMENTO ANTES DE TUDO. Ele reconstroi o inventario inteiro — a espada
        // vira a espada de gelo, com outro dano e outro desenho —, entao repor os
        // itens primeiro faria o trabalho duas vezes e, pior, a reconstrucao
        // apagaria as pilhas que acabaram de ser repostas.
        com.retronova.game.items.Elemento[] elementos =
                com.retronova.game.items.Elemento.values();
        if (elemento >= 0 && elemento < elementos.length) {
            gato.reporElemento(elementos[elemento]);
        }
        // AS MELHORIAS ANTES DA VIDA. Melhoria de vigor sobe a vida MAXIMA, e
        // {@code setLife} recusa qualquer valor acima do maximo do momento.
        gato.getMelhorias().deNumeros(gato, melhorias.toArray(new int[0][]));
        gato.setLife(vida);
        gato.setXpBruto(xp, nivelDoGato);
        gato.reporNiveisPendentes(andamento.niveisPendentes());
        gato.setMoney(dinheiro);

        Inventory inv = gato.getInventory();
        inv.esvaziar();
        repor(inv.getHotbar(), hotbar);
        repor(inv.getBag(), mochila);
        for (int[] p : passivos) {
            gato.reporPassivo(p[1], p[2]);
        }
    }

    /**
     * Devolve à corrida o que é dela: o relógio, a Fúria, as válvulas das cartas e
     * os números da tela final. Os chefes vivos voltam pelo Game, no primeiro tick
     * — nascer precisa da partida já na pilha de telas.
     */
    public void aplicarNaCorrida(Arena arena) {
        arena.getWaves().repor(andamento.relogio());
        com.retronova.game.objects.entities.Furia.repor(andamento.furia());
        com.retronova.game.map.arena.SubirDeNivel.repor(andamento.trocas(), andamento.pulos());
        Estatisticas.repor(andamento.baus(), andamento.maiorHorda(), andamento.venceu(),
                andamento.danoPorFonte());
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
