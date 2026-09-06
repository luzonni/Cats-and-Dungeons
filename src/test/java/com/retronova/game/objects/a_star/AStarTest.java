package com.retronova.game.objects.a_star;

import com.retronova.game.objects.a_star.exceptions.OutOfRange;
import com.retronova.game.objects.a_star.exceptions.SolidNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do pathfinding.
 *
 * Tudo aqui trabalha em índices de tile — a mesma unidade que o {@link WayMap}
 * espera. Foi justamente a mistura entre pixels e tiles que fazia o A* falhar em
 * toda chamada em produção, sem nunca calcular um caminho.
 *
 * O {@link CheckerNode} é sintético, então os testes não dependem de mapa
 * carregado, de janela nem de qualquer estado global do jogo.
 */
class AStarTest {

    /** Mundo aberto: nenhuma célula é sólida. */
    private static final CheckerNode LIVRE = (x, y) -> false;

    /** Parede vertical em x = 5, com uma passagem em y = 0. */
    private static final CheckerNode PAREDE = (x, y) -> x == 5 && y != 0;

    private static List<Node> caminho(int origemX, int origemY, int destinoX, int destinoY,
                                      int alcance, CheckerNode checker) {
        Node origem = new Node(origemX, origemY);
        Node destino = new Node(destinoX, destinoY);
        return new A_Star(new WayMap(origem, destino, alcance, checker)).getPath();
    }

    @Test
    @DisplayName("acha caminho em linha reta num mapa sem obstáculos")
    void caminhoEmLinhaReta() {
        List<Node> path = caminho(0, 0, 4, 0, 10, LIVRE);

        assertFalse(path.isEmpty(), "deveria existir caminho num mapa livre");
        // O caminho é devolvido do destino para a origem, sem incluir a origem.
        assertEquals(4, path.getFirst().x, "o primeiro nó do caminho é o destino");
        assertEquals(0, path.getFirst().y);
    }

    @Test
    @DisplayName("contorna uma parede em vez de atravessá-la")
    void contornaObstaculo() {
        List<Node> path = caminho(0, 3, 9, 3, 15, PAREDE);

        assertFalse(path.isEmpty(), "existe passagem em y=0, o caminho deveria ser achado");
        for (Node no : path) {
            boolean atravessouParede = no.x == 5 && no.y != 0;
            assertFalse(atravessouParede,
                    "o caminho passou pela parede em (" + no.x + "," + no.y + ")");
        }
    }

    @Test
    @DisplayName("cada passo do caminho é adjacente ao anterior")
    void caminhoEContinuo() {
        List<Node> path = caminho(0, 0, 6, 6, 12, LIVRE);
        assertFalse(path.isEmpty());

        for (int i = 0; i < path.size() - 1; i++) {
            int dx = Math.abs(path.get(i).x - path.get(i + 1).x);
            int dy = Math.abs(path.get(i).y - path.get(i + 1).y);
            assertTrue(dx <= 1 && dy <= 1,
                    "salto de (" + dx + "," + dy + ") entre nós consecutivos");
        }
    }

    @Test
    @DisplayName("destino fora do alcance é recusado com OutOfRange")
    void destinoForaDoAlcance() {
        // Este é o caso que acontecia sempre quando se passava pixels: a diferença
        // entre origem e destino estourava o alcance medido em tiles.
        assertThrows(OutOfRange.class, () -> caminho(0, 0, 500, 500, 25, LIVRE));
    }

    @Test
    @DisplayName("destino em cima de um tile sólido é recusado com SolidNode")
    void destinoSolido() {
        assertThrows(SolidNode.class, () -> caminho(0, 3, 5, 3, 15, PAREDE));
    }

    @Test
    @DisplayName("o nó inicial é gravado na própria posição, não no canto da grade")
    void noInicialNaPosicaoCerta() {
        Node origem = new Node(7, 7);
        Node destino = new Node(9, 7);
        WayMap mapa = new WayMap(origem, destino, 10, LIVRE);

        assertEquals(origem, mapa.getNode(7, 7),
                "getNode na posição da origem deve devolver o próprio nó inicial");
    }
}
