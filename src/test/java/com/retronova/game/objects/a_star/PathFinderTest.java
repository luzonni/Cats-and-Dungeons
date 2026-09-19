package com.retronova.game.objects.a_star;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do PathFinder — a fronteira que o AStarTest não cobria.
 *
 * POR QUE ESTA CLASSE PRECISOU EXISTIR. O A* já tinha teste, e ele passava: as
 * buscas eram montadas à mão, em tiles, direto sobre WayMap e A_Star. O que
 * nenhum teste tocava era o PathFinder — e era exatamente ali que o caminho
 * quebrava, porque ele convertia a unidade mais uma vez antes de repassar. Em
 * jogo, o zumbi recebia como "próximo passo" um nó colado na origem da grade,
 * andava para o canto do mapa e ficava preso na parede.
 *
 * É o formato de bug mais difícil de pegar por leitura: as duas pontas estavam
 * certas e só a emenda estava errada. Só um teste que atravessa a emenda o vê.
 *
 * A REGRA QUE ESTES TESTES GUARDAM: o que entra em tiles sai em tiles, e o
 * primeiro passo é SEMPRE vizinho de onde o bicho está. Um passo distante não dá
 * erro nenhum — ele só manda o inimigo andar para o lugar errado.
 */
class PathFinderTest {

    /** Arena sem parede nenhuma. */
    private static final CheckerNode ABERTO = (x, y) -> false;

    private static boolean vizinho(Point a, Point b) {
        return Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1;
    }

    @Test
    @DisplayName("o primeiro passo e vizinho de onde o bicho esta")
    void primeiroPassoEhAdjacente() {
        // Os numeros sao grandes de proposito: com tiles perto de zero, dividir a
        // coordenada por engano daria quase o mesmo resultado e o defeito passaria.
        Point onde = new Point(30, 24);
        PathFinder rota = new PathFinder(ABERTO);
        rota.buildPath(onde, new Point(36, 30), 40);

        Point passo = rota.getFollow();
        assertNotNull(passo, "nao achou caminho numa arena sem parede");
        assertTrue(vizinho(onde, passo),
                "o primeiro passo saiu em " + passo.x + "," + passo.y + " e o bicho esta em "
                        + onde.x + "," + onde.y + " — nao sao vizinhos. E o sintoma de a "
                        + "coordenada ter sido convertida de unidade no meio do caminho.");
    }

    @Test
    @DisplayName("o passo anda NA DIRECAO do alvo")
    void andaParaOAlvo() {
        Point onde = new Point(30, 24);
        Point alvo = new Point(36, 30);
        PathFinder rota = new PathFinder(ABERTO);
        rota.buildPath(onde, alvo, 40);

        Point passo = rota.getFollow();
        assertNotNull(passo);
        double antes = onde.distance(alvo);
        double depois = passo.distance(alvo);
        assertTrue(depois < antes,
                "o passo afasta do alvo: de " + antes + " para " + depois);
    }

    @Test
    @DisplayName("a rota inteira e uma corrente de vizinhos ate o alvo")
    void aRotaEhContinua() {
        Point onde = new Point(30, 24);
        Point alvo = new Point(36, 30);
        PathFinder rota = new PathFinder(ABERTO);
        rota.buildPath(onde, alvo, 40);

        Point anterior = onde;
        int passos = 0;
        while (!rota.isEmpty() && passos < 100) {
            Point passo = rota.getFollow();
            assertTrue(vizinho(anterior, passo),
                    "a rota pula de " + anterior.x + "," + anterior.y + " para "
                            + passo.x + "," + passo.y);
            anterior = passo;
            rota.arrived();
            passos++;
        }
        assertTrue(passos > 0, "a rota veio vazia");
        assertTrue(vizinho(anterior, alvo) || anterior.equals(alvo),
                "a rota terminou em " + anterior.x + "," + anterior.y + " e nao no alvo");
    }

    @Test
    @DisplayName("contorna uma parede em vez de atravessar")
    void contornaParede() {
        // Parede vertical em x=33, com uma passagem em y=28.
        CheckerNode parede = (x, y) -> x == 33 && y != 28;
        Point onde = new Point(30, 24);
        Point alvo = new Point(36, 24);
        PathFinder rota = new PathFinder(parede);
        rota.buildPath(onde, alvo, 40);

        boolean passouPelaPorta = false;
        Point anterior = onde;
        int passos = 0;
        while (!rota.isEmpty() && passos < 200) {
            Point passo = rota.getFollow();
            assertFalse(parede.check(passo.x, passo.y),
                    "a rota passou por dentro da parede em " + passo.x + "," + passo.y);
            if (passo.x == 33) {
                passouPelaPorta = true;
            }
            anterior = passo;
            rota.arrived();
            passos++;
        }
        assertTrue(passos > 0, "nao achou rota contornando a parede");
        assertTrue(passouPelaPorta, "chegou do outro lado sem passar pela unica porta");
    }

    @Test
    @DisplayName("alvo fora do alcance devolve vazio, e nao explode")
    void foraDeAlcanceEhSilencioso() {
        PathFinder rota = new PathFinder(ABERTO);
        rota.buildPath(new Point(30, 24), new Point(300, 300), 40);
        assertTrue(rota.isEmpty(), "deveria devolver vazio");
        // E quem chama tem de conseguir perguntar sem tomar excecao.
        assertTrue(rota.getFollow() == null);
    }

    @Test
    @DisplayName("alvo em cima de um bloco devolve vazio")
    void alvoSolidoEhSilencioso() {
        CheckerNode soOAlvo = (x, y) -> x == 36 && y == 30;
        PathFinder rota = new PathFinder(soOAlvo);
        rota.buildPath(new Point(30, 24), new Point(36, 30), 40);
        assertTrue(rota.isEmpty());
    }

    @Test
    @DisplayName("refazer a rota substitui a anterior")
    void refazerTrocaARota() {
        PathFinder rota = new PathFinder(ABERTO);
        rota.buildPath(new Point(30, 24), new Point(36, 30), 40);
        assertNotNull(rota.getFollow());

        // O bicho andou; a rota nova tem de sair de onde ele esta AGORA.
        Point novoLugar = new Point(50, 50);
        rota.buildPath(novoLugar, new Point(54, 54), 40);
        assertTrue(vizinho(novoLugar, rota.getFollow()),
                "a rota nova nao comeca onde o bicho esta");
    }
}
