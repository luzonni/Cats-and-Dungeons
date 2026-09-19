package com.retronova.game.objects.a_star;

import com.retronova.game.objects.a_star.exceptions.OutOfRange;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Acha o caminho de um ponto a outro, EM TILES.
 *
 * ---------------------------------------------------------------------------
 * A UNIDADE, que era o defeito.
 *
 * Esta classe recebia um {@link Point} e o entregava a {@code new Node(Point)},
 * um construtor que DIVIDIA por {@code GameObject.SIZE()} — ou seja, esperava
 * pixels. Só que quem chamava já convertia para tiles antes, para a checagem de
 * alcance do WayMap parar de reprovar tudo. As duas correções eram razoáveis
 * sozinhas e juntas dividiam por sessenta e quatro DUAS vezes: o tile 30,20 virava
 * o nó 0,0.
 *
 * O efeito em jogo era o zumbi parado. Ele recebia como "próximo passo" um nó
 * perto da origem da grade, calculava que devia andar para o canto superior
 * esquerdo do mapa, batia na parede e ficava ali — parecendo quebrado, e estando.
 *
 * Agora a fronteira é explícita: TUDO AQUI É TILE. O construtor que convertia
 * pixels foi removido do Node, para o erro não ter como voltar.
 *
 * ---------------------------------------------------------------------------
 * POR QUE DEIXOU DE SER UMA THREAD.
 *
 * Cada {@code buildPath} abria uma {@code new Thread}, e quem chamava pedia um
 * caminho por QUADRO enquanto não tivesse um — sessenta threads por segundo, por
 * zumbi, todas escrevendo no mesmo campo e enfileiradas num método
 * {@code synchronized}. A fila crescia mais rápido do que era consumida, então na
 * prática o caminho nunca ficava pronto a tempo de servir.
 *
 * A medição desfez a premissa: uma busca numa grade de oitenta por oitenta leva
 * 0,13 ms. Seis zumbis refazendo a rota a cada vinte quadros custam menos de um
 * décimo por cento de um núcleo. Não havia nada para tirar do caminho da thread
 * principal, e a concorrência só trazia a corrida de dados de graça.
 */
public class PathFinder {

    private List<Node> path = new ArrayList<>();

    private final CheckerNode checkerNode;

    public PathFinder(CheckerNode checkerNode) {
        this.checkerNode = checkerNode;
    }

    /**
     * Calcula a rota. Silencioso quando não há: caminho vazio, e não exceção.
     *
     * @param inicio  onde o bicho está, EM TILES
     * @param fim     onde ele quer chegar, EM TILES
     * @param alcance raio da janela de busca, em tiles
     */
    public void buildPath(Point inicio, Point fim, int alcance) {
        try {
            Node start = new Node(inicio.x, inicio.y);
            Node goal = new Node(fim.x, fim.y);
            WayMap wayMap = new WayMap(start, goal, alcance, checkerNode);
            this.path = new A_Star(wayMap).getPath();
        } catch (RuntimeException semCaminho) {
            // Alvo fora da janela, ou em cima de um bloco. Acontece o tempo todo em
            // partida e não é erro: quem chamou anda direto enquanto não houver rota.
            this.path = new ArrayList<>();
        }
    }

    /** O próximo tile do caminho, ou null se não houver rota. */
    public Point getFollow() {
        if (isEmpty()) {
            return null;
        }
        Node node = path.get(path.size() - 1);
        return new Point(node.x, node.y);
    }

    /** Chegou no tile atual: o próximo passa a ser o seguinte. */
    public void arrived() {
        if (!isEmpty()) {
            path.remove(path.size() - 1);
        }
    }

    public boolean isEmpty() {
        return path == null || path.isEmpty();
    }
}
