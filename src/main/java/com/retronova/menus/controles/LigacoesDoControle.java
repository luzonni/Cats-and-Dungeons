package com.retronova.menus.controles;

import com.retronova.engine.inputs.gamepad.Comando;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Onde fica cada rótulo da visão do controle, e o caminho da linha até ele.
 *
 * QUATRO SAÍDAS, UM ÂNGULO NO MÁXIMO. A maior parte das peças sai para uma das
 * colunas: reta até a borda do desenho, na altura da peça, e dali em diagonal até
 * o rótulo. As que não têm caminho livre para o lado — View e Menu ficam na altura
 * do analógico esquerdo, o X fica cercado pelos outros botões de face — saem por
 * uma reta só, para cima ou para baixo, até uma fileira de rótulos acima ou abaixo
 * do desenho.
 *
 * As retas nunca cruzam outra peça porque cada âncora foi posta numa altura (ou
 * numa coluna de pixels) livre — ver DesenhoDoControle.PECAS. As diagonais de uma
 * coluna nunca se cruzam porque os rótulos ficam na MESMA ORDEM das âncoras. As
 * colunas ficam entre as duas fileiras, e por isso nenhum rótulo encosta em outro.
 *
 * Contas puras, sem desenho: o DesenhoDoControleTest confere as garantias.
 */
public final class LigacoesDoControle {

    private LigacoesDoControle() {
    }

    /**
     * O rótulo de uma peça já posicionado: topo, altura e os três pontos da linha
     * (numa reta só, a dobra e a ponta coincidem). {@code aEsquerda} diz de que
     * lado da ponta fica o texto.
     */
    public record Ligacao(Comando comando, DesenhoDoControle.Lado lado, boolean aEsquerda, int topo, int altura,
                          Point ancora, Point dobra, Point ponta) {
        public int centro() {
            return topo + altura / 2;
        }
    }

    /**
     * Põe cada rótulo o mais perto possível da altura da âncora, sem um encostar
     * no outro e dentro de [topo, base]. Os rótulos chegam na ordem de cima para
     * baixo e saem nela.
     *
     * @param centros a altura ideal do meio de cada rótulo
     * @return o topo de cada rótulo
     */
    public static int[] distribuir(int[] centros, int[] alturas, int vao, int topo, int base) {
        int n = centros.length;
        int[] y = new int[n];
        for (int i = 0; i < n; i++) {
            y[i] = centros[i] - alturas[i] / 2;
        }
        // Desce o que encosta no de cima; depois sobe o que passou da base; e de
        // novo desce, para o de cima não sair do topo (só se não couber mesmo).
        for (int i = 0; i < n; i++) {
            int minimo = i == 0 ? topo : y[i - 1] + alturas[i - 1] + vao;
            y[i] = Math.max(y[i], minimo);
        }
        for (int i = n - 1; i >= 0; i--) {
            int maximo = i == n - 1 ? base - alturas[i] : y[i + 1] - vao - alturas[i];
            y[i] = Math.min(y[i], maximo);
        }
        for (int i = 0; i < n; i++) {
            int minimo = i == 0 ? topo : y[i - 1] + alturas[i - 1] + vao;
            y[i] = Math.max(y[i], minimo);
        }
        return y;
    }

    /** A altura da fileira de cima (ou de baixo): o rótulo mais alto dela, ou zero se não há nenhum. */
    public static int fileira(Map<Comando, Integer> alturas, DesenhoDoControle.Lado lado) {
        int h = 0;
        for (Map.Entry<Comando, Integer> e : alturas.entrySet()) {
            if (DesenhoDoControle.PECAS.get(e.getKey()).lado() == lado) {
                h = Math.max(h, e.getValue());
            }
        }
        return h;
    }

    /**
     * As ligações de todas as peças com rótulo, em pixels de tela.
     *
     * @param x0, y0     canto de cima à esquerda do desenho na tela
     * @param s          a escala do desenho
     * @param alturas    a altura do rótulo de cada peça (só as que têm rótulo)
     * @param folga      a distância da borda do desenho até a ponta da linha, nas colunas
     * @param topo, base a faixa vertical de tudo: fileira de cima, colunas e fileira de baixo
     */
    public static Map<Comando, Ligacao> ligar(int x0, int y0, int s, Map<Comando, Integer> alturas,
                                              int folga, int vao, int topo, int base) {
        Map<Comando, Ligacao> saida = new EnumMap<>(Comando.class);
        int cima = fileira(alturas, DesenhoDoControle.Lado.CIMA);
        int baixo = fileira(alturas, DesenhoDoControle.Lado.BAIXO);

        // As fileiras: uma reta só, da peça até o meio do rótulo.
        for (Map.Entry<Comando, Integer> e : alturas.entrySet()) {
            DesenhoDoControle.Peca p = DesenhoDoControle.PECAS.get(e.getKey());
            if (p.lado() != DesenhoDoControle.Lado.CIMA && p.lado() != DesenhoDoControle.Lado.BAIXO) {
                continue;
            }
            int h = e.getValue();
            int topoDoRotulo = p.lado() == DesenhoDoControle.Lado.CIMA ? topo + (cima - h) / 2
                    : base - baixo + (baixo - h) / 2;
            int ax = x0 + p.ancoraX() * s + s / 2;
            Point ancora = new Point(ax, y0 + p.ancoraY() * s + (p.lado() == DesenhoDoControle.Lado.BAIXO ? s : 0));
            Point ponta = new Point(ax, topoDoRotulo + h / 2);
            saida.put(e.getKey(), new Ligacao(e.getKey(), p.lado(), p.textoAEsquerda(), topoDoRotulo, h,
                    ancora, ponta, ponta));
        }

        // As colunas, entre as duas fileiras.
        int topoDasColunas = topo + (cima > 0 ? cima + vao : 0);
        int baseDasColunas = base - (baixo > 0 ? baixo + vao : 0);
        for (DesenhoDoControle.Lado lado : new DesenhoDoControle.Lado[] {
                DesenhoDoControle.Lado.ESQUERDA, DesenhoDoControle.Lado.DIREITA}) {
            boolean esquerda = lado == DesenhoDoControle.Lado.ESQUERDA;
            List<Comando> coluna = new ArrayList<>();
            for (Map.Entry<Comando, DesenhoDoControle.Peca> e : DesenhoDoControle.PECAS.entrySet()) {
                if (e.getValue().lado() == lado && alturas.containsKey(e.getKey())) {
                    coluna.add(e.getKey());
                }
            }
            coluna.sort(Comparator.comparingInt(c -> DesenhoDoControle.PECAS.get(c).ancoraY()));
            int[] centros = new int[coluna.size()], hs = new int[coluna.size()];
            for (int i = 0; i < coluna.size(); i++) {
                centros[i] = y0 + DesenhoDoControle.PECAS.get(coluna.get(i)).ancoraY() * s + s / 2;
                hs[i] = alturas.get(coluna.get(i));
            }
            int[] topos = distribuir(centros, hs, vao, topoDasColunas, baseDasColunas);
            int borda = esquerda ? x0 - 2 * s : x0 + DesenhoDoControle.LARGURA * s + 2 * s;
            int pontaX = esquerda ? borda - folga : borda + folga;
            for (int i = 0; i < coluna.size(); i++) {
                DesenhoDoControle.Peca p = DesenhoDoControle.PECAS.get(coluna.get(i));
                int ay = y0 + p.ancoraY() * s + s / 2;
                Point ancora = new Point(x0 + p.ancoraX() * s + (esquerda ? 0 : s), ay);
                Point dobra = new Point(borda, ay);
                Point ponta = new Point(pontaX, topos[i] + hs[i] / 2);
                saida.put(coluna.get(i), new Ligacao(coluna.get(i), lado, esquerda, topos[i], hs[i],
                        ancora, dobra, ponta));
            }
        }
        return saida;
    }
}
