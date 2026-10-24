import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * OS ÍCONES DE BUFF, PASSIVA, ARMA EXTRA, ELEMENTO E RARIDADE, numa linguagem só.
 *
 * ---------------------------------------------------------------------------
 * POR QUE TROCAR
 *
 * Os anteriores eram matrizes de 9x9 com cor saturada, brilho branco puro e
 * sombra preta (game/map/arena/Icones), e os selos de elemento e de raridade eram
 * outros 9x9 com contorno 0x222222 (GenIconesElementais). Feitos às pressas, num
 * tamanho de pixel que nenhum outro sprite do jogo usa, e em cores que a ilustração
 * fria do título não tem.
 *
 * ---------------------------------------------------------------------------
 * DE ONDE VÊM
 *
 * BASE: "16x16 RPG Items (DB32)", de ARoachIFoundOnMyPillow (CC0), o mesmo pacote
 * de onde já saem as varinhas de água e de fogo (GenElementais). Fica, fora do
 * repositório, em
 * tools/assets/fonte/db32. É pixel art de 16x16 com contorno escuro e três a
 * quatro tons por material e luz do alto — o mesmo jeito dos itens que o gato
 * segura. Escudo, armadura, botas, espada, poção, orbe, livro, folha, taça, flecha
 * e gemas vêm dele (ver "Do pacote" abaixo).
 *
 * O QUE O PACOTE NÃO TEM É DESENHADO AQUI, no mesmo molde: silhueta escrita à mão
 * em 16x16, e o sombreado e o contorno calculados pela mesma regra para todos
 * ({@link #sombrear}) — luz do alto à esquerda, borda de cima clara, de baixo
 * escura, contorno de um pixel. Assim um coração desenhado aqui e uma espada do
 * pacote saem com a mesma mão.
 *
 * ---------------------------------------------------------------------------
 * A COR É UM MAPEAMENTO, NÃO UM AJUSTE NO OLHO
 *
 * Cada ícone diz que FAMÍLIA de cor cada material dele usa (metal, vida, fogo...).
 * Cada família é uma rampa de cinco tons gerada da MESMA fórmula a partir de uma
 * cor oficial do jogo, puxada um quinto para o tom frio da paleta (Palette.MAIN):
 *
 *     âncora = mistura(cor oficial, MAIN, 0,2)
 *     rampa  = [âncora→OUTLINE 60%, →OUTLINE 30%, âncora, →TEXT 30%, →TEXT 60%]
 *
 * As cores oficiais são as que o jogo já usa: Elemento.cor (fogo, gelo, água,
 * terra, ar), Raridade.cor (raro, épico, lendário), a vida da barra do chefe e as
 * cores das cartas (verde da área, rosa do novelo, azul do raio). O metal é a
 * própria escada da interface (DEEP, MAIN, LIGHT, ACCENT, TEXT). O contorno é
 * sempre Palette.OUTLINE.
 *
 * Nos ícones do pacote, cada cor da paleta DawnBringer 32 pertence a um material
 * (DB32_MATERIAL); dentro do material, as cores do ícone são ordenadas por
 * luminância e caem nos degraus da rampa ({@link #degraus}). O branco puro é
 * sempre o brilho.
 *
 *     java tools/GenIcones.java [pasta-da-prévia]
 *
 * Saída: src/main/resources/com/retronova/resources/icones/<nome>.png, 16x16.
 * Lidos pelo registro central game/items/Icone.
 */
public class GenIcones {

    private static final String FONTE = "tools/assets/fonte/db32/";
    private static final String SAIDA = "src/main/resources/com/retronova/resources/icones/";
    private static final int LADO = 16;

    // ------------------------------------------------------------- paleta

    private static final int OUTLINE = 0x211A2B, DEEP = 0x3E4160, MAIN = 0x525779, LIGHT = 0x7E849C,
            ACCENT = 0xABB1C1, TEXT = 0xFCFCFC;

    /** As famílias de cor, cada uma uma rampa de cinco tons do escuro ao brilho. */
    enum Familia {
        ACO(new int[] {DEEP, MAIN, LIGHT, ACCENT, TEXT}),
        VIDA(0xC23A4E),      // BarraDeChefe.VIDA
        FOGO(0xDF864D),      // Elemento.FOGO
        GELO(0x85C4D4),      // Elemento.GELO
        AGUA(0x79A8D4),      // Elemento.AGUA
        TERRA(0xB07E41),     // Elemento.TERRA (também a madeira e o couro)
        AR(0xD7FFE4),        // Elemento.AR
        OURO(0xFFD850),      // Raridade.LENDARIO
        RARO(0x4FA3D9),      // Raridade.RARO
        EPICO(0xA05FD0),     // Raridade.EPICO
        VERDE(0x5BE07A),     // Passiva.AREA, ArmaExtra.AURA
        ROSA(0xFF6FA8),      // ArmaExtra.NOVELO
        ELETRICO(0x7FD8FF);  // ArmaExtra.RAIO

        final int[] rampa;

        Familia(int[] rampa) {
            this.rampa = rampa;
        }

        Familia(int oficial) {
            int a = misturar(oficial, MAIN, 0.2);
            this.rampa = new int[] {misturar(a, OUTLINE, 0.6), misturar(a, OUTLINE, 0.3), a,
                    misturar(a, TEXT, 0.3), misturar(a, TEXT, 0.6)};
        }
    }

    // ------------------------------------------------------------- DB32

    /** Os materiais da paleta DawnBringer 32, pelo papel de cada cor nos ícones do pacote. */
    enum Material { NEUTRO, MADEIRA, AZUL, ROXO, VERMELHO, VERDE, OURO, LARANJA, BRILHO }

    private static final Map<Integer, Material> DB32_MATERIAL = new TreeMap<>(Map.ofEntries(
            Map.entry(0x595652, Material.NEUTRO), Map.entry(0x696A6A, Material.NEUTRO),
            Map.entry(0x847E87, Material.NEUTRO), Map.entry(0x9BADB7, Material.NEUTRO),
            Map.entry(0xCBDBFC, Material.NEUTRO),
            Map.entry(0x45283C, Material.MADEIRA), Map.entry(0x663931, Material.MADEIRA),
            Map.entry(0x8F563B, Material.MADEIRA), Map.entry(0xD9A066, Material.MADEIRA),
            Map.entry(0xEEC39A, Material.MADEIRA),
            Map.entry(0x3F3F74, Material.AZUL), Map.entry(0x306082, Material.AZUL),
            Map.entry(0x5B6EE1, Material.AZUL), Map.entry(0x639BFF, Material.AZUL),
            Map.entry(0x5FCDE4, Material.AZUL),
            Map.entry(0x76428A, Material.ROXO), Map.entry(0xD77BBA, Material.ROXO),
            Map.entry(0xAC3232, Material.VERMELHO), Map.entry(0xD95763, Material.VERMELHO),
            Map.entry(0x4B692F, Material.VERDE), Map.entry(0x37946E, Material.VERDE),
            Map.entry(0x6ABE30, Material.VERDE), Map.entry(0x99E550, Material.VERDE),
            Map.entry(0xFBF236, Material.OURO),
            Map.entry(0xDF7126, Material.LARANJA),
            Map.entry(0xFFFFFF, Material.BRILHO)));

    /** Um ícone do pacote: o arquivo, a família de cada material e pixels a acrescentar por cima. */
    record DoPacote(String arquivo, Map<Material, Familia> cores, Familia resto, String[] acrescimo,
                    Map<Character, Familia> coresDoAcrescimo) {
    }

    // ------------------------------------------------------------- desenhados

    /**
     * Um ícone desenhado: a silhueta em 16x16 e a família de cada letra.
     *
     * Na matriz, '.' é vazio; uma letra minúscula é corpo daquele material, com o
     * tom calculado pela luz ({@link #sombrear}); a maiúscula é o brilho do material
     * e o dígito '0' é o tom mais escuro do material 'a' (risco, fresta). O contorno
     * não se escreve: ele é calculado em volta de tudo o que for corpo.
     */
    record Desenhado(Map<Character, Familia> cores, String[] forma) {
    }

    private static Map<Character, Familia> cores(Object... pares) {
        Map<Character, Familia> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put((Character) pares[i], (Familia) pares[i + 1]);
        }
        return m;
    }

    private static final Map<String, Object> ICONES = new LinkedHashMap<>();

    static {
        // ------------------------------------------------------------ do pacote
        // Fúria (dano): a espada do pacote, lâmina de metal e cabo de couro.
        ICONES.put("furia", new DoPacote("sword_iron.png",
                Map.of(Material.NEUTRO, Familia.ACO), Familia.TERRA, null, null));
        // Couro (resistência): o escudo de madeira com o aro de metal.
        ICONES.put("couro", new DoPacote("shield_wood.png",
                Map.of(Material.NEUTRO, Familia.ACO), Familia.TERRA, null, null));
        // Armadura (dano recebido): o peitoral de ferro.
        ICONES.put("armadura", new DoPacote("armor_chest_iron.png", Map.of(), Familia.ACO, null, null));
        // Regeneração: a poção vermelha, vidro claro e rolha de madeira.
        ICONES.put("regeneracao", new DoPacote("potion_red.png",
                Map.of(Material.VERMELHO, Familia.VIDA, Material.ROXO, Familia.VIDA,
                        Material.NEUTRO, Familia.ACO), Familia.TERRA, null, null));
        // Presságio (chance de carta melhor): a bola de cristal, vidro roxo e base de ouro.
        ICONES.put("presagio", new DoPacote("item_orb.png",
                Map.of(Material.AZUL, Familia.EPICO, Material.OURO, Familia.OURO), Familia.TERRA, null, null));
        // Grimório: o livro de capa roxa, com as bordas das páginas em ouro.
        ICONES.put("grimorio", new DoPacote("item_book_purple.png",
                Map.of(Material.ROXO, Familia.EPICO, Material.AZUL, Familia.EPICO), Familia.OURO, null, null));
        // Aura de catnip: a folha.
        ICONES.put("aura", new DoPacote("item_leaf.png", Map.of(), Familia.VERDE, null, null));
        // Troféu das conquistas: a taça.
        ICONES.put("trofeu", new DoPacote("item_goblet_empty.png", Map.of(), Familia.OURO, null, null));
        // Raridades: uma gema por grau, na cor do grau.
        ICONES.put("comum", new DoPacote("item_gem_quartz.png", Map.of(), Familia.ACO, null, null));
        ICONES.put("raro", new DoPacote("item_gem_sapphire.png", Map.of(), Familia.RARO, null, null));
        ICONES.put("epico", new DoPacote("item_gem_amethyst.png", Map.of(), Familia.EPICO, null, null));
        ICONES.put("lendario", new DoPacote("item_gem_amber.png", Map.of(), Familia.OURO, null, null));

        // ------------------------------------------------------------ adaptados
        // Patas Ligeiras (velocidade): as botas do pacote em couro, com dois riscos
        // de vento atrás — sem os riscos, botas leriam como armadura.
        ICONES.put("patas", new DoPacote("armor_boots_iron.png", Map.of(), Familia.TERRA, new String[] {
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................"}, cores()));
        // Frenesi (cadência): DUAS espadas cruzadas — a da fúria e o espelho dela.
        // A primeira tentativa (a mesma espada com riscos de vento) diferia da fúria
        // em seis pixels de silhueta: sem a cor, eram o mesmo ícone (IconeTest).
        ICONES.put("frenesi", new Cruzado(new DoPacote("sword_iron.png",
                Map.of(Material.NEUTRO, Familia.ACO), Familia.TERRA, null, null)));
        // Retaliação (Muffin): o escudo de metal com a onda de choque saindo dos dois lados.
        ICONES.put("retaliacao", new DoPacote("shield_metal.png", Map.of(), Familia.ACO, new String[] {
                "................",
                "................",
                "................",
                "................",
                ".a............a.",
                "a..............a",
                "a..............a",
                "a..............a",
                "a..............a",
                "a..............a",
                "a..............a",
                ".a............a.",
                "................",
                "................",
                "................",
                "................"}, cores('a', Familia.VERDE)));

        // ------------------------------------------------------------ desenhados
        // Vigor (vida máxima): o coração.
        ICONES.put("vigor", new Desenhado(cores('a', Familia.VIDA), new String[] {
                "................",
                "................",
                "...aaa....aaa...",
                "..aAAaa..aaaaa..",
                ".aAAaaaaaaaaaaa.",
                ".aAaaaaaaaaaaaa.",
                ".aaaaaaaaaaaaaa.",
                ".aaaaaaaaaaaaaa.",
                "..aaaaaaaaaaaa..",
                "...aaaaaaaaaa...",
                "....aaaaaaaa....",
                ".....aaaaaa.....",
                "......aaaa......",
                ".......aa.......",
                "................",
                "................"}));
        // Instinto (sorte): o trevo de quatro folhas, com o cabinho.
        ICONES.put("instinto", new Desenhado(cores('a', Familia.VERDE, 'b', Familia.TERRA), new String[] {
                "................",
                "...aaa....aaa...",
                "..aAaaa..aAaaa..",
                "..aaaaaa.aaaaa..",
                "..aaaaaaaaaaaa..",
                "...aaaaaaaaaa...",
                "....aaa00aaa....",
                "...aaaa00aaaa...",
                "..aaaaaaaaaaaa..",
                "..aaaaa.aaaaaa..",
                "..aaaa...aaaa...",
                "...aa.....bb....",
                "...........bb...",
                "............b...",
                "................",
                "................"}));
        // Ímã (alcance de coleta): a ferradura vermelha com as pontas de metal.
        ICONES.put("ima", new Desenhado(cores('a', Familia.VIDA, 'b', Familia.ACO), new String[] {
                "................",
                "................",
                "...aaaaaaaaaa...",
                "..aAAaaaaaaaaa..",
                ".aAaaaaaaaaaaaa.",
                ".aAaaa....aaaaa.",
                ".aaaa......aaaa.",
                ".aaaa......aaaa.",
                ".aaaa......aaaa.",
                ".aaaa......aaaa.",
                ".bbbb......bbbb.",
                ".bBbb......bBbb.",
                ".bbbb......bbbb.",
                "................",
                "................",
                "................"}));
        // Pressa (recarga): a ampulheta, moldura de madeira e areia de ouro.
        ICONES.put("pressa", new Desenhado(cores('a', Familia.TERRA, 'b', Familia.OURO, 'c', Familia.GELO),
                new String[] {
                "................",
                "..aaaaaaaaaaaa..",
                "..aAAaaaaaaaaa..",
                "...cccccccccc...",
                "...cBbbbbbbbc...",
                "....cbbbbbbc....",
                ".....cbbbbc.....",
                "......cbbc......",
                "......cbbc......",
                ".....cccbcc.....",
                "....cccbbbcc....",
                "...ccbbbbbbbc...",
                "...cbbbbbbbbc...",
                "..aaaaaaaaaaaa..",
                "..aaaaaaaaaaaa..",
                "................"}));
        // Crítico: o estouro de pontas, o mesmo da partícula de dano crítico.
        ICONES.put("critico", new Desenhado(cores('a', Familia.OURO), new String[] {
                "................",
                ".......aa.......",
                "..a....aa....a..",
                "...a...aa...a...",
                "....a.aaaa.a....",
                ".....aaAAaa.....",
                "....aaAaaaaa....",
                ".aaaaaaaaaaaaaa.",
                ".aaaaaaaaaaaaaa.",
                "....aaaaaaaa....",
                ".....aaaaaa.....",
                "....a.aaaa.a....",
                "...a...aa...a...",
                "..a....aa....a..",
                ".......aa.......",
                "................"}));
        // Área: três anéis em volta de um centro.
        ICONES.put("area", new Desenhado(cores('a', Familia.VERDE), new String[] {
                "................",
                ".....aaaaaa.....",
                "...aaA....aaa...",
                "..aA........aa..",
                ".aA...aaaa...aa.",
                ".a...aa..aa...a.",
                "aa..aa....aa..aa",
                "a...a..aa..a...a",
                "a...a..aa..a...a",
                "aa..aa....aa..aa",
                ".a...aa..aa...a.",
                ".aa...aaaa...aa.",
                "..aa........aa..",
                "...aaa....aaa...",
                ".....aaaaaa.....",
                "................"}));
        // Crescimento (XP): o broto com a seta para cima.
        ICONES.put("crescimento", new Desenhado(cores('a', Familia.VERDE, 'b', Familia.OURO), new String[] {
                "................",
                "...........b....",
                "..........bbb...",
                ".........bBbbb..",
                "...........b....",
                "...aaa.....b....",
                "..aAaaa.........",
                "..aaaaaa..aaa...",
                "...aaaaa.aAaaa..",
                ".....aaaaaaaa...",
                "......aaaaa.....",
                ".......aa.......",
                ".......aa.......",
                "......aaaa......",
                "................",
                "................"}));
        // Voleio (Finn): três flechas lado a lado, saindo juntas na mesma direção.
        ICONES.put("voleio", new Desenhado(cores('a', Familia.ACO, 'x', Familia.TERRA, 'y', Familia.AR),
                new String[] {
                "................",
                "................",
                ".......aA.......",
                ".......aa.......",
                "......x.........",
                ".....x....aA....",
                "....x.....aa....",
                "...x.....x......",
                ".yx.....x....aA.",
                "..y....x.....aa.",
                "......x.....x...",
                "....yx.....x....",
                ".....y....x.....",
                ".........x......",
                ".......yx.......",
                "........y......."}));
        // Explosão arcana (Azrael): a estrela de quatro pontas, longa e fina.
        ICONES.put("explosao_arcana", new Desenhado(cores('a', Familia.EPICO), new String[] {
                "................",
                ".......a........",
                ".......a........",
                "......aAa.......",
                "......aaa.......",
                ".....aaaaa......",
                "....aaAaaaa.....",
                ".aaaaaaaaaaaaaa.",
                "....aaaaaaa.....",
                ".....aaaaa......",
                "......aaa.......",
                "......aaa....a..",
                ".......a....aAa.",
                ".......a.....a..",
                "................",
                "................"}));
        // Novelo: a bola de lã com as voltas do fio e a ponta solta.
        ICONES.put("novelo", new Desenhado(cores('a', Familia.ROSA), new String[] {
                "................",
                ".....aaaaa......",
                "...aaAAa0aaa....",
                "..aAaa0aaa0aa...",
                "..aaa0aaa0aaa...",
                ".aaa0aaa0aaaaa..",
                ".aa0aaa0aaa0aa..",
                ".a0aaa0aaa0aaa..",
                ".aaaa0aaa0aaaa..",
                ".aaa0aaa0aaa0a..",
                "..aaaaa0aaa0a...",
                "..aaa0aaa0aaa...",
                "...aaaaaaaaa....",
                ".....aaaaa.aa...",
                "............aa..",
                ".............a.."}));
        // Raio: o relâmpago em zigue-zague.
        ICONES.put("raio", new Desenhado(cores('a', Familia.ELETRICO), new String[] {
                "................",
                ".........aaaa...",
                "........aAaa....",
                ".......aAaa.....",
                "......aaaa......",
                ".....aaaa.......",
                "....aaaaaaaaa...",
                "...aaaaaaaaa....",
                ".........aaa....",
                "........aaa.....",
                ".......aaa......",
                "......aaa.......",
                ".....aa.........",
                "....aa..........",
                "...a............",
                "................"}));
        // Tremor: o chão rachado com as lascas pulando do impacto.
        ICONES.put("tremor", new Desenhado(cores('a', Familia.TERRA), new String[] {
                "................",
                ".......aa.......",
                ".......aa.......",
                "..aa........aa..",
                "..aa........aa..",
                "................",
                "................",
                "................",
                "..aaaaa..aaaaa..",
                ".aAAaaa..aaaaaa.",
                ".aAaaaaa.aaaaaa.",
                ".aaaaaaaa0aaaaa.",
                ".aaaaaaaaa0aaaa.",
                ".aaaaaaaaaaaaaa.",
                "................",
                "................"}));
        // Chuva de meteoros: a pedra em brasa com o rastro.
        ICONES.put("meteoro", new Desenhado(cores('a', Familia.FOGO, 'b', Familia.OURO), new String[] {
                "................",
                ".b..............",
                "..b...b.........",
                "...b...b........",
                "..b.b...b.......",
                "...b.b.aaaa.....",
                "....b.aaAaaa....",
                ".....aaAbbaaa...",
                "......abbbbaa...",
                "......aabbbaa...",
                ".......aaaaaa...",
                "........aaaa....",
                "................",
                "................",
                "................",
                "................"}));
        // XP: a gema que o bicho larga, pequena, azul do grau raro. Forma de
        // cristal, que nenhum inimigo tem: coletável lê como coletável de relance.
        ICONES.put("xp", new Desenhado(cores('a', Familia.RARO), new String[] {
                "................",
                "................",
                "................",
                "................",
                ".......a........",
                "......aAa.......",
                ".....aAaaa......",
                ".....Aaaaa......",
                ".....aaaaa......",
                "......aaa.......",
                ".......a........",
                "................",
                "................",
                "................",
                "................",
                "................"}));
        // XP grande: o prêmio do objeto quebrado, maior e em ouro.
        ICONES.put("xp_grande", new Desenhado(cores('a', Familia.OURO), new String[] {
                "................",
                "................",
                ".......a........",
                "......aAa.......",
                ".....aAaaa......",
                "....aAaaaaa.....",
                "...aAaaaaaaa....",
                "...Aaaaaaaaa....",
                "...aaaaaaaaa....",
                "....aaaaaaa.....",
                ".....aaaaa......",
                "......aaa.......",
                ".......a........",
                "................",
                "................",
                "................"}));
        // Desconhecido: o ponto de interrogação, para chave sem desenho.
        ICONES.put("desconhecido", new Desenhado(cores('a', Familia.ACO), new String[] {
                "................",
                "................",
                ".....aaaaaa.....",
                "....aAA...aa....",
                "....aa.....aa...",
                "...........aa...",
                "..........aa....",
                ".........aa.....",
                "........aa......",
                ".......aa.......",
                ".......aa.......",
                "................",
                ".......aa.......",
                ".......aa.......",
                "................",
                "................"}));

        // ------------------------------------------------------------ elementos
        ICONES.put("fogo", new Desenhado(cores('a', Familia.FOGO, 'b', Familia.OURO), new String[] {
                "................",
                ".......a........",
                ".......aa.......",
                "......aaa.......",
                "......aAaa..a...",
                ".....aaAaa.aa...",
                "..a..aaaaaaaa...",
                "..aa.aaabaaaa...",
                "..aaaaabbbaaaa..",
                "..aaaaabbbbaaa..",
                "..aaaabbBbbaaa..",
                "..aaabbbbbbbaa..",
                "...aabbbbbbaa...",
                "....aaaaaaaa....",
                "................",
                "................"}));
        ICONES.put("gelo", new Desenhado(cores('a', Familia.GELO), new String[] {
                "................",
                ".......aa.......",
                ".....a.aa.a.....",
                "......aaaa......",
                "..a....aa....a..",
                "...aa..aa..aa...",
                "..aaaa.aa.aaaa..",
                ".aAaaaaaaaaaaaa.",
                ".aaaaaaaaaaaaaa.",
                "..aaaa.aa.aaaa..",
                "...aa..aa..aa...",
                "..a....aa....a..",
                "......aaaa......",
                ".....a.aa.a.....",
                ".......aa.......",
                "................"}));
        ICONES.put("agua", new Desenhado(cores('a', Familia.AGUA), new String[] {
                "................",
                ".......aa.......",
                ".......aa.......",
                "......aaaa......",
                "......aaaa......",
                ".....aaaaaa.....",
                "....aAaaaaaa....",
                "....aAaaaaaa....",
                "...aAAaaaaaaa...",
                "...aAaaaaaaaa...",
                "...aaaaaaaaaa...",
                "...aaaaaaaaaa...",
                "....aaaaaaaa....",
                ".....aaaaaa.....",
                "................",
                "................"}));
        ICONES.put("terra", new Desenhado(cores('a', Familia.TERRA, 'b', Familia.VERDE), new String[] {
                "................",
                "................",
                "................",
                "......bbb.......",
                "....bbbbbbb.....",
                "...aaAAaaaaa....",
                "..aAAaaaa0aaa...",
                "..aAaaaa0aaaaa..",
                ".aaaaaaa0aaaaaa.",
                ".aaaa0aaaaaaaaa.",
                ".aaaaa0aaaaa0aa.",
                ".aaaaaaaaaa0aaa.",
                "..aaaaaaaaaaaa..",
                "................",
                "................",
                "................"}));
        ICONES.put("ar", new Desenhado(cores('a', Familia.AR), new String[] {
                "................",
                "................",
                ".......aaaa.....",
                "......a....a....",
                "...........a....",
                ".aaaaaaaaaa.....",
                "................",
                ".aaaaaaaaaaaaa..",
                "..............a.",
                "..............a.",
                "...aaaaaaaaaaa..",
                "................",
                "..aaaaaaa.......",
                ".........a......",
                "......aaa.......",
                "................"}));

        // ------------------------------------------------------------ espelho
        // As melhorias permanentes do vendedor que não tinham ícone: as outras
        // reaproveitam os de atributo e passiva (vida = VIGOR, dano = FURIA...).
        // Alcance: a luneta do pacote, em metal e couro.
        ICONES.put("alcance", new DoPacote("telescope1.png",
                Map.of(Material.NEUTRO, Familia.ACO), Familia.TERRA, null, null));
        // Ganância (mais moedas por corrida): o saco do pacote, em ouro.
        ICONES.put("ganancia", new DoPacote("item_sack.png", Map.of(), Familia.OURO, null, null));
        // Reroll: o anel de "girar de novo", com a ponta de seta no fim do arco.
        ICONES.put("reroll", new Desenhado(cores('a', Familia.RARO), new String[] {
                "................",
                "................",
                "......aaaa..a...",
                "....aaaaaaaaa...",
                "...aaa....aaa...",
                "...aa....aaaa...",
                "..aaa...........",
                "..aa............",
                "..aa........aa..",
                "..aaa.......aa..",
                "...aa......aaa..",
                "...aaa....aaa...",
                "....aaaaaaaa....",
                "......aaaa......",
                "................",
                "................"}));
        // Cartas extras: três cartas em leque, a da frente com o "+".
        ICONES.put("cartas", new Desenhado(cores('a', Familia.ACO, 'b', Familia.ACO, 'c', Familia.EPICO), new String[] {
                "................",
                "................",
                "........cccccc..",
                ".....aaacccccc..",
                "..bbbaaacccccc..",
                "..bbbaaacccCcc..",
                "..bbbaaaccCCCc..",
                "..bbbaaacccCcc..",
                "..bbbaaacccccc..",
                "..bbbaaacccccc..",
                "..bbbaaacccccc..",
                "..bbbaaacccccc..",
                "..bbbaaaaaa.....",
                "..bbbbbb........",
                "................",
                "................"}));
        // Moeda: o saldo do espelho. Aro, miolo marcado e brilho em cima à esquerda.
        ICONES.put("moeda", new Desenhado(cores('a', Familia.OURO), new String[] {
                "................",
                "................",
                ".....aaaaaa.....",
                "....aAAaaaaa....",
                "...aAaaaaaaaa...",
                "..aaa000000aaa..",
                "..aa0aaaaaa0aa..",
                "..aa0aa00aa0aa..",
                "..aa0aa00aa0aa..",
                "..aa0aaaaaa0aa..",
                "..aaa000000aaa..",
                "...aaaaaaaaaa...",
                "....aaaaaaaa....",
                ".....aaaaaa.....",
                "................",
                "................"}));
    }

    public static void main(String[] args) throws Exception {
        if (!new File(FONTE + "sword_iron.png").exists()) {
            // O pacote fica fora do repositório, como os outros (ver .gitignore).
            System.err.println("Falta o DB32 em " + FONTE + ": baixe db32_rpg_items_0.zip de "
                    + "https://opengameart.org/content/16x16-rpg-items-db32 e descompacte ali.");
            System.err.println("Ver docs/TERCEIROS.md para de onde baixar.");
            System.exit(1);
        }
        new File(SAIDA).mkdirs();
        List<BufferedImage> todos = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        for (Map.Entry<String, Object> e : ICONES.entrySet()) {
            BufferedImage img = switch (e.getValue()) {
                case DoPacote p -> doPacote(p);
                case Cruzado c -> cruzado(c);
                default -> desenhado((Desenhado) e.getValue());
            };
            ImageIO.write(img, "png", new File(SAIDA + e.getKey() + ".png"));
            todos.add(img);
            nomes.add(e.getKey());
        }
        System.out.println(todos.size() + " ícones em " + SAIDA);
        for (Map.Entry<String, Familia> t : TIRAS.entrySet()) {
            recolorirTira(t.getKey(), t.getValue());
        }
        System.out.println(TIRAS.size() + " tiras de partícula em " + SAIDA_TIRAS);
        if (args.length > 0) {
            previa(todos, args[0]);
        }
    }

    // ------------------------------------------------------------- partículas

    /**
     * AS TIRAS DE PARTÍCULA DOS EFEITOS NO TEMPO, na família do efeito: a brasa do
     * fogo, o coração da regeneração, a bolha do veneno e o faiscar do golpe. Eram
     * de outro conjunto, saturadas e com o preto puro de contorno; o selo de fogo
     * e a brasa no bicho falavam cores diferentes. Os originais estão em
     * tools/assets/particulas_original; o veneno vai para a família épica,
     * roxa como o número de veneno, para não se confundir com a cura verde.
     */
    private static final String FONTE_TIRAS = "tools/assets/particulas_original/";
    private static final String SAIDA_TIRAS = "src/main/resources/com/retronova/resources/sprites/objects/particle/";
    private static final Map<String, Familia> TIRAS = new LinkedHashMap<>(Map.of(
            "fire", Familia.FOGO, "heart", Familia.VIDA, "poison", Familia.EPICO, "damagemobs", Familia.OURO));

    /**
     * Recolore uma tira: as cores escuras (valor abaixo de um quarto) viram o
     * contorno da paleta; as outras são ordenadas por luminância e caem nos
     * degraus da rampa da família, como nos ícones do pacote.
     */
    private static void recolorirTira(String nome, Familia f) throws Exception {
        BufferedImage fonte = ImageIO.read(new File(FONTE_TIRAS + nome + ".png"));
        List<Integer> cores = new ArrayList<>();
        for (int y = 0; y < fonte.getHeight(); y++) {
            for (int x = 0; x < fonte.getWidth(); x++) {
                int p = fonte.getRGB(x, y);
                if ((p >>> 24) >= 128 && !escura(p & 0xFFFFFF) && !cores.contains(p & 0xFFFFFF)) {
                    cores.add(p & 0xFFFFFF);
                }
            }
        }
        cores.sort((a, b) -> Double.compare(luminancia(a), luminancia(b)));
        // O FOGO TEM MIOLO DE OURO: só na família do fogo, a chama saía bege nos
        // tons claros. As duas cores mais claras vão para o ouro (base e luz), como
        // o miolo do ícone de fogo; o resto fica no fogo.
        int quentes = f == Familia.FOGO ? Math.min(2, cores.size() - 1) : 0;
        int[] degraus = degraus(cores.size() - quentes);
        BufferedImage saida = new BufferedImage(fonte.getWidth(), fonte.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < fonte.getHeight(); y++) {
            for (int x = 0; x < fonte.getWidth(); x++) {
                int p = fonte.getRGB(x, y);
                if ((p >>> 24) < 128) {
                    continue;
                }
                int rgb = p & 0xFFFFFF;
                int i = escura(rgb) ? -1 : cores.indexOf(rgb);
                int nova = i < 0 ? OUTLINE
                        : i >= cores.size() - quentes ? Familia.OURO.rampa[2 + (i - (cores.size() - quentes))]
                        : f.rampa[degraus[i]];
                saida.setRGB(x, y, 0xFF000000 | nova);
            }
        }
        ImageIO.write(saida, "png", new File(SAIDA_TIRAS + nome + ".png"));
    }

    private static boolean escura(int rgb) {
        int max = Math.max(rgb >> 16 & 255, Math.max(rgb >> 8 & 255, rgb & 255));
        return max < 64;
    }

    // ------------------------------------------------------------- do pacote

    private static BufferedImage doPacote(DoPacote p) throws Exception {
        BufferedImage fonte = ImageIO.read(new File(FONTE + p.arquivo()));
        // Agrupa as cores do ícone por família e ordena cada grupo por luminância.
        Map<Familia, List<Integer>> grupos = new LinkedHashMap<>();
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                int c = fonte.getRGB(x, y);
                if ((c >>> 24) < 128 || (c & 0xFFFFFF) == 0 || (c & 0xFFFFFF) == 0xFFFFFF) {
                    continue;
                }
                Familia f = familiaDe(c & 0xFFFFFF, p);
                List<Integer> g = grupos.computeIfAbsent(f, k -> new ArrayList<>());
                if (!g.contains(c & 0xFFFFFF)) {
                    g.add(c & 0xFFFFFF);
                }
            }
        }
        for (List<Integer> g : grupos.values()) {
            g.sort((a, b) -> Double.compare(luminancia(a), luminancia(b)));
        }
        BufferedImage saida = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                int c = fonte.getRGB(x, y);
                if ((c >>> 24) < 128) {
                    continue;
                }
                int rgb = c & 0xFFFFFF;
                int nova;
                if (rgb == 0) {
                    nova = OUTLINE;
                } else if (rgb == 0xFFFFFF) {
                    nova = familiaDoBrilho(fonte, x, y, p).rampa[4];
                } else {
                    Familia f = familiaDe(rgb, p);
                    List<Integer> g = grupos.get(f);
                    nova = f.rampa[degraus(g.size())[g.indexOf(rgb)]];
                }
                saida.setRGB(x, y, 0xFF000000 | nova);
            }
        }
        if (p.acrescimo() != null) {
            acrescentar(saida, p.acrescimo(), p.coresDoAcrescimo());
        }
        return saida;
    }

    private static Familia familiaDe(int rgb, DoPacote p) {
        Material m = DB32_MATERIAL.get(rgb);
        if (m == null) {
            throw new IllegalStateException(p.arquivo() + ": cor fora da DB32 " + Integer.toHexString(rgb));
        }
        return p.cores().getOrDefault(m, p.resto());
    }

    /** O branco puro é o brilho do material vizinho: de vidro no vidro, de ouro no ouro. */
    private static Familia familiaDoBrilho(BufferedImage fonte, int x, int y, DoPacote p) {
        for (int r = 1; r <= 2; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= LADO || ny >= LADO) {
                        continue;
                    }
                    int c = fonte.getRGB(nx, ny) & 0xFFFFFF;
                    if ((fonte.getRGB(nx, ny) >>> 24) >= 128 && c != 0 && c != 0xFFFFFF) {
                        return familiaDe(c, p);
                    }
                }
            }
        }
        return p.resto();
    }

    /**
     * Os degraus da rampa para N cores de um material, do escuro ao claro. Duas
     * cores pegam sombra e luz; quatro, os quatro de cima; cinco, a rampa toda.
     */
    static int[] degraus(int n) {
        return switch (n) {
            case 1 -> new int[] {2};
            case 2 -> new int[] {1, 3};
            case 3 -> new int[] {1, 2, 3};
            case 4 -> new int[] {1, 2, 3, 4};
            default -> {
                int[] d = new int[n];
                for (int i = 0; i < n; i++) {
                    d[i] = (int) Math.round(i * 4.0 / (n - 1));
                }
                yield d;
            }
        };
    }

    /** Pixels por cima de um ícone do pacote, com o mesmo sombreado e contorno dos desenhados. */
    private static void acrescentar(BufferedImage img, String[] forma, Map<Character, Familia> cores) {
        validar(forma);
        BufferedImage extra = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        sombrear(extra, forma, cores, false);
        Graphics2D g = img.createGraphics();
        g.drawImage(extra, 0, 0, null);
        g.dispose();
    }

    /** Um ícone do pacote com o espelho dele por trás, em X: as duas espadas cruzadas. */
    record Cruzado(DoPacote base) {
    }

    private static BufferedImage cruzado(Cruzado c) throws Exception {
        BufferedImage frente = doPacote(c.base());
        BufferedImage saida = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                saida.setRGB(LADO - 1 - x, y, frente.getRGB(x, y));
            }
        }
        Graphics2D g = saida.createGraphics();
        g.drawImage(frente, 0, 0, null);
        g.dispose();
        return saida;
    }

    // ------------------------------------------------------------- desenhados

    private static BufferedImage desenhado(Desenhado d) {
        validar(d.forma());
        BufferedImage img = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        sombrear(img, d.forma(), d.cores(), true);
        return img;
    }

    /**
     * O sombreado comum a todos os desenhados: luz do alto à esquerda. A borda de
     * cima (sem corpo acima) fica clara, a quina de cima à esquerda mais clara
     * ainda; a de baixo e a da direita ficam na sombra; o miolo é o tom base. O
     * contorno de um pixel cerca a silhueta inteira.
     */
    private static void sombrear(BufferedImage img, String[] forma, Map<Character, Familia> cores, boolean contorno) {
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                char c = forma[y].charAt(x);
                if (c == '.') {
                    if (contorno && vizinho(forma, x, y)) {
                        img.setRGB(x, y, 0xFF000000 | OUTLINE);
                    }
                    continue;
                }
                Familia f;
                int degrau;
                if (fino(c)) {
                    // TRAÇO FINO NÃO TEM CONTORNO: uma linha de um pixel cercada de
                    // contorno vira um borrão escuro (a haste da flecha, o risco de
                    // vento). Fica num tom só, um degrau abaixo do base, que se lê
                    // sobre o medalhão claro e sobre a casa escura.
                    f = cores.get(c);
                    degrau = 1;
                } else if (c == '0') {
                    f = cores.get('a');
                    degrau = 0;
                } else if (Character.isUpperCase(c)) {
                    f = cores.get(Character.toLowerCase(c));
                    degrau = 4;
                } else {
                    f = cores.get(c);
                    boolean cima = vazio(forma, x, y - 1), esquerda = vazio(forma, x - 1, y);
                    boolean baixo = vazio(forma, x, y + 1), direita = vazio(forma, x + 1, y);
                    degrau = cima && esquerda ? 4 : cima ? 3 : baixo ? 1 : direita ? 1 : 2;
                }
                if (f == null) {
                    throw new IllegalStateException("letra sem cor: " + c);
                }
                img.setRGB(x, y, 0xFF000000 | f.rampa[degrau]);
            }
        }
        if (!contorno) {
            // Acréscimo sobre um ícone do pacote: contorna só por fora do que já existe.
            for (int y = 0; y < LADO; y++) {
                for (int x = 0; x < LADO; x++) {
                    if (forma[y].charAt(x) == '.' && (img.getRGB(x, y) >>> 24) == 0 && vizinho(forma, x, y)) {
                        img.setRGB(x, y, 0xFF000000 | OUTLINE);
                    }
                }
            }
        }
    }

    /** Fora da silhueta para a luz e para o contorno: o vazio e o traço fino. */
    private static boolean vazio(String[] forma, int x, int y) {
        return x < 0 || y < 0 || x >= LADO || y >= LADO || forma[y].charAt(x) == '.' || fino(forma[y].charAt(x));
    }

    /** As letras x, y e z são traço fino: sem contorno e num tom só (ver sombrear). */
    private static boolean fino(char c) {
        return c == 'x' || c == 'y' || c == 'z';
    }

    private static boolean vizinho(String[] forma, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && !vazio(forma, x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void validar(String[] forma) {
        if (forma.length != LADO) {
            throw new IllegalStateException("forma com " + forma.length + " linhas");
        }
        for (String linha : forma) {
            if (linha.length() != LADO) {
                throw new IllegalStateException("linha com " + linha.length() + " colunas: " + linha);
            }
        }
    }

    // ------------------------------------------------------------- utilidades

    private static double luminancia(int rgb) {
        return 0.299 * (rgb >> 16 & 255) + 0.587 * (rgb >> 8 & 255) + 0.114 * (rgb & 255);
    }

    static int misturar(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    /** Uma folha com todos os ícones em 1x, 2x e 4x sobre o painel do jogo, para conferir. */
    private static void previa(List<BufferedImage> todos, String pasta) throws Exception {
        int[] escalas = {1, 2, 4};
        int cel = LADO * 4 + 8;
        BufferedImage o = new BufferedImage(todos.size() * cel + 8, cel * 3 + 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = o.createGraphics();
        g.setColor(new Color(DEEP));
        g.fillRect(0, 0, o.getWidth(), o.getHeight());
        for (int i = 0; i < todos.size(); i++) {
            for (int e = 0; e < escalas.length; e++) {
                int z = escalas[e];
                g.drawImage(todos.get(i), 8 + i * cel, 8 + e * cel, LADO * z, LADO * z, null);
            }
        }
        g.dispose();
        new File(pasta).mkdirs();
        ImageIO.write(o, "png", new File(pasta, "previa_icones.png"));
    }
}
