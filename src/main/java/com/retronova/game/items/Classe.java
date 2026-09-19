package com.retronova.game.items;

/**
 * A QUE TIPO DE ARMA UM GATO SE DEDICA.
 *
 * POR QUE TRAVAR, se ter tudo parece mais generoso.
 *
 * Com as vinte e tantas armas disponíveis para os três gatos, escolher o
 * personagem não escolhia nada: os atributos mudavam alguns por cento e o resto
 * da corrida era idêntico. Pior, a carta de recompensa sorteava do balaio
 * inteiro, então duas corridas com o mesmo gato divergiam mais entre si do que
 * duas corridas com gatos diferentes — a variação vinha do sorteio e não da
 * decisão.
 *
 * Limitando cada gato a uma família, a escolha do personagem passa a decidir
 * COMO a corrida é jogada, e não com que cara ela começa. É a diferença entre
 * três skins e três jogos. E é também o que permite a progressão por cartas
 * existir de verdade: uma carta de "arco atravessa dois alvos" só pode ser
 * escrita se houver a garantia de que quem a receber tem um arco na mão.
 *
 * A DERIVAÇÃO VEM DO NOME DO ID, e não de uma tabela de item para classe.
 * Tabela envelhece na primeira arma nova que alguém escrever sem lembrar de
 * atualizá-la; o nome do ID a pessoa é obrigada a escolher. É a mesma razão pela
 * qual {@link Raridade#de} deriva em vez de declarar, e o mesmo mecanismo que
 * {@code Item.variante} já usa para achar o elemento no fim do nome.
 */
public enum Classe {

    /** Espada, machado, foice, garras, escudo: quem bate na caixa de golpe. */
    CORPO_A_CORPO("Melee"),

    /** Varinha e laser: quem conjura. */
    MAGIA("Magic"),

    /** Arco, kunai, tridente, seda, bola de pelo: quem lança. */
    DISTANCIA("Ranged"),

    /**
     * Sem classe: consumíveis e utilitários.
     *
     * Não é ausência de valor, é uma resposta de verdade — comida não pertence a
     * estilo de luta nenhum, e por isso ela não é barrada para ninguém. Sem este
     * degrau, cada lugar que pergunta a classe precisaria antes perguntar se o
     * item é uma arma.
     */
    NENHUMA("Item");

    private final String rotulo;

    Classe(String rotulo) {
        this.rotulo = rotulo;
    }

    /** O nome em inglês, para a interface. */
    public String rotulo() {
        return this.rotulo;
    }

    public static Classe de(Item item) {
        ItemIDs[] ids = ItemIDs.values();
        int id = item.getID();
        return id >= 0 && id < ids.length ? de(ids[id]) : NENHUMA;
    }

    /**
     * A classe de um ID, pelo nome dele.
     *
     * O sufixo elemental é ignorado: SwordIce e Sword são a mesma espada com outro
     * pigmento, e é assim que o resto do código já as trata.
     */
    public static Classe de(ItemIDs id) {
        String base = semElemento(id.name());
        return switch (base) {
            case "Sword", "Axe", "BloodyAxe", "Sickle", "ClawBlades" -> CORPO_A_CORPO;
            // O escudo mora no ID DangerousWand por causa dos JSON de mapa, mas ele
            // orbita o gato e bate em quem encosta — é corpo a corpo, não magia.
            case "DangerousWand" -> CORPO_A_CORPO;
            case "Wand", "Laser" -> MAGIA;
            case "Bow", "BowEletric", "Kunai", "Trident", "Silk", "Furball",
                 "Bomb", "GasBomb" -> DISTANCIA;
            default -> NENHUMA;
        };
    }

    /** "SwordIce" vira "Sword"; "Sickle" fica como está. */
    private static String semElemento(String nome) {
        for (Elemento e : Elemento.values()) {
            if (e == Elemento.NENHUM) {
                continue;
            }
            String sufixo = sufixoDoNome(e);
            if (nome.endsWith(sufixo) && nome.length() > sufixo.length()) {
                return nome.substring(0, nome.length() - sufixo.length());
            }
        }
        return nome;
    }

    /** Como o elemento aparece no fim do ID: FOGO -> "Fire". Ver Item.variante. */
    private static String sufixoDoNome(Elemento e) {
        String s = e.sprite("");            // "_fire"
        return Character.toUpperCase(s.charAt(1)) + s.substring(2);
    }
}
