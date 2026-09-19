package com.retronova.game.items;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da classificação de armas por classe.
 *
 * DUAS COISAS SÃO GUARDADAS AQUI, e nenhuma delas dá erro quando quebra — é por
 * isso que estão escritas em teste e não confiadas à atenção de quem mexer.
 *
 * A PRIMEIRA: nenhuma arma pode ficar órfã de classe. A derivação sai do nome do
 * ID, então uma arma nova com um nome que o switch não conhece cai em NENHUMA — e
 * NENHUMA significa "serve para todos", que é o oposto de travar. Uma espada nova
 * mal batizada não daria erro nenhum: ela simplesmente apareceria na carta do
 * mago, e ninguém saberia por quê.
 *
 * A SEGUNDA: nenhuma classe pode ficar sem arma. Um gato cuja classe não tem
 * nenhuma arma jogável não tem o que comprar na loja, e a corrida inteira dele
 * fica presa na arma inicial — sem erro nenhum aparecer. Ver o comentário do
 * próprio teste para a regra que este substituiu, e por quê.
 */
class ClasseTest {

    /** Todo ID que deveria ser uma arma, pelo prefixo do nome. */
    private static final String[] PREFIXOS_DE_ARMA = {
            "Sword", "Axe", "BloodyAxe", "Sickle", "ClawBlades",
            "Wand", "Laser", "DangerousWand",
            "Bow", "Kunai", "Trident", "Silk", "Furball",
    };

    private static boolean pareceArma(ItemIDs id) {
        for (String prefixo : PREFIXOS_DE_ARMA) {
            if (id.name().startsWith(prefixo)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("nenhuma arma fica sem classe")
    void nenhumaArmaOrfa() {
        List<String> orfas = new ArrayList<>();
        for (ItemIDs id : ItemIDs.values()) {
            if (pareceArma(id) && Classe.de(id) == Classe.NENHUMA) {
                orfas.add(id.name());
            }
        }
        assertTrue(orfas.isEmpty(),
                "arma sem classe cai em NENHUMA, que vale para todos os gatos — "
                        + "o oposto de travar. Sem classe: " + orfas);
    }

    @Test
    @DisplayName("nenhuma classe fica sem arma para comprar")
    void cadaClasseTemArma() {
        // ESTE TESTE EXIGIA TRES, e a regra que o justificava deixou de existir.
        //
        // Ele guardava a oferta de fim de turno, que sorteava TRES ARMAS sem
        // repetir. Duas mudancas depois, a recompensa passou a oferecer MELHORIAS —
        // ver Melhoria — e as variantes elementais sairam do catalogo, porque o
        // elemento virou escolha de corrida e Item.build ja monta a arma base com
        // ele. Nenhuma das duas mexeu nesta regra, e ela continuou verde ate a
        // segunda, quando Magia caiu para duas armas.
        //
        // Baixar o numero para o teste passar seria enfraquecer a guarda; a resposta
        // honesta e trocar a regra pela que de fato vale agora. Ela e mais simples e
        // mais grave: um gato sem NENHUMA arma da classe dele nao tem o que comprar
        // na loja, e a corrida inteira dele fica presa na arma inicial.
        //
        // Hoje: corpo a corpo 6, distancia 6, MAGIA 2. Magia esta magro e isso e
        // conteudo a escrever, nao defeito a testar.
        Map<Classe, Integer> quantas = new EnumMap<>(Classe.class);
        for (ItemIDs id : ItemIDs.values()) {
            if (id.jogavel()) {
                quantas.merge(Classe.de(id), 1, Integer::sum);
            }
        }
        for (Classe c : new Classe[]{Classe.CORPO_A_CORPO, Classe.MAGIA, Classe.DISTANCIA}) {
            assertTrue(quantas.getOrDefault(c, 0) >= 1,
                    c + " nao tem nenhuma arma jogavel; o gato dessa classe nao teria "
                            + "o que comprar na loja");
        }
    }

    @Test
    @DisplayName("o sufixo elemental nao muda a classe da arma")
    void varianteSegueABase() {
        assertEquals(Classe.CORPO_A_CORPO, Classe.de(ItemIDs.Sword));
        assertEquals(Classe.CORPO_A_CORPO, Classe.de(ItemIDs.SwordIce));
        assertEquals(Classe.CORPO_A_CORPO, Classe.de(ItemIDs.SwordLegend));
        assertEquals(Classe.MAGIA, Classe.de(ItemIDs.Wand));
        assertEquals(Classe.MAGIA, Classe.de(ItemIDs.WandEarth));
        assertEquals(Classe.DISTANCIA, Classe.de(ItemIDs.Bow));
        assertEquals(Classe.DISTANCIA, Classe.de(ItemIDs.BowAir));
    }

    @Test
    @DisplayName("as armas iniciais dos tres gatos sao de classes diferentes")
    void tresGatosTresClasses() {
        // Sao as armas que os JSON de personagem entregam: Muffin, Azrael, Finn.
        // Se duas caissem na mesma classe, dois gatos jogariam a mesma corrida.
        assertEquals(Classe.CORPO_A_CORPO, Classe.de(ItemIDs.Sword), "Muffin");
        assertEquals(Classe.MAGIA, Classe.de(ItemIDs.Wand), "Azrael");
        assertEquals(Classe.DISTANCIA, Classe.de(ItemIDs.Bow), "Finn");
    }

    @Test
    @DisplayName("consumivel e utilitario nao pertencem a classe nenhuma")
    void semClasseParaOQueNaoEArma() {
        assertEquals(Classe.NENHUMA, Classe.de(ItemIDs.Feed));
        assertEquals(Classe.NENHUMA, Classe.de(ItemIDs.Acorn));
        assertEquals(Classe.NENHUMA, Classe.de(ItemIDs.MagneticOrb));
    }

    @Test
    @DisplayName("o escudo conta como corpo a corpo, apesar do nome")
    void escudoEhMelee() {
        // O ID continua DangerousWand porque esta gravado nos JSON de mapa, mas o
        // item virou escudo: ele orbita o gato e bate em quem encosta.
        assertEquals(Classe.CORPO_A_CORPO, Classe.de(ItemIDs.DangerousWand));
    }

    @Test
    @DisplayName("os itens em espera continuam fora de jogo")
    void emEsperaNaoEhJogavel() {
        assertTrue(ItemIDs.EM_ESPERA.contains(ItemIDs.Catnip));
        assertTrue(ItemIDs.Sword.jogavel());
        assertTrue(!ItemIDs.Catnip.jogavel());
    }
}
