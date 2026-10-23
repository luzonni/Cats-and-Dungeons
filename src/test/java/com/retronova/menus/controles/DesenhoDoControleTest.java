package com.retronova.menus.controles;

import com.retronova.engine.inputs.gamepad.Comando;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As garantias das linhas da visão do controle: nada se cruza, nada passa por cima de outra peça. */
class DesenhoDoControleTest {

    /** A reta de uma peça até fora do desenho, no sentido em que ela sai. */
    private static Line2D reta(DesenhoDoControle.Peca p) {
        double ax = p.ancoraX() + 0.5, ay = p.ancoraY() + 0.5;
        return switch (p.lado()) {
            case ESQUERDA -> new Line2D.Double(ax, ay, -2, ay);
            case DIREITA -> new Line2D.Double(ax, ay, DesenhoDoControle.LARGURA + 2, ay);
            case CIMA -> new Line2D.Double(ax, ay, ax, -2);
            case BAIXO -> new Line2D.Double(ax, ay, ax, DesenhoDoControle.ALTURA + 2);
        };
    }

    @Test
    @DisplayName("a reta de cada peça até fora do desenho não atravessa outra peça")
    void retasLivres() {
        for (Map.Entry<Comando, DesenhoDoControle.Peca> e : DesenhoDoControle.PECAS.entrySet()) {
            Line2D reta = reta(e.getValue());
            for (Map.Entry<Comando, DesenhoDoControle.Peca> outra : DesenhoDoControle.PECAS.entrySet()) {
                if (outra.getKey() == e.getKey()) {
                    continue;
                }
                Rectangle r = outra.getValue().area();
                assertFalse(reta.intersects(r.x, r.y, r.width, r.height),
                        "a linha de " + e.getKey() + " passa por cima de " + outra.getKey());
            }
        }
    }

    @Test
    @DisplayName("as âncoras ficam na borda da própria peça, do lado para onde a linha sai")
    void ancorasNaBorda() {
        for (Map.Entry<Comando, DesenhoDoControle.Peca> e : DesenhoDoControle.PECAS.entrySet()) {
            DesenhoDoControle.Peca p = e.getValue();
            Rectangle r = p.area();
            boolean dentroX = p.ancoraX() >= r.x && p.ancoraX() < r.x + r.width;
            boolean dentroY = p.ancoraY() >= r.y && p.ancoraY() < r.y + r.height;
            switch (p.lado()) {
                case ESQUERDA -> assertTrue(p.ancoraX() == r.x && dentroY, e.getKey() + " não sai da borda esquerda");
                case DIREITA -> assertTrue(p.ancoraX() == r.x + r.width - 1 && dentroY,
                        e.getKey() + " não sai da borda direita");
                case CIMA -> assertTrue(p.ancoraY() == r.y && dentroX, e.getKey() + " não sai da borda de cima");
                case BAIXO -> assertTrue(p.ancoraY() == r.y + r.height - 1 && dentroX,
                        e.getKey() + " não sai da borda de baixo");
            }
        }
    }

    @Test
    @DisplayName("as peças cabem no desenho e não se sobrepõem")
    void pecasSeparadas() {
        Rectangle desenho = new Rectangle(0, 0, DesenhoDoControle.LARGURA, DesenhoDoControle.ALTURA);
        for (Map.Entry<Comando, DesenhoDoControle.Peca> e : DesenhoDoControle.PECAS.entrySet()) {
            assertTrue(desenho.contains(e.getValue().area()), e.getKey() + " sai do desenho");
            for (Map.Entry<Comando, DesenhoDoControle.Peca> outra : DesenhoDoControle.PECAS.entrySet()) {
                if (outra.getKey().ordinal() > e.getKey().ordinal()) {
                    assertFalse(e.getValue().area().intersects(outra.getValue().area()),
                            e.getKey() + " encosta em " + outra.getKey());
                }
            }
        }
    }

    @Test
    @DisplayName("nenhuma linha cruza outra, em várias escalas e alturas")
    void linhasNaoSeCruzam() {
        for (int s = 1; s <= 4; s++) {
            for (int altura : new int[] {13, 7}) {
                Map<Comando, Integer> alturas = new EnumMap<>(Comando.class);
                for (Comando c : DesenhoDoControle.PECAS.keySet()) {
                    alturas.put(c, altura * s);
                }
                int topo = 0, base = 140 * s;
                Map<Comando, LigacoesDoControle.Ligacao> ligacoes =
                        LigacoesDoControle.ligar(100 * s, 30 * s, s, alturas, 10 * s, 2 * s, topo, base);
                List<Line2D> segmentos = new ArrayList<>();
                List<Comando> donos = new ArrayList<>();
                for (LigacoesDoControle.Ligacao l : ligacoes.values()) {
                    // Nas retas para cima e para baixo a dobra é a ponta: o segmento sem
                    // comprimento fica de fora, porque o Line2D o dá como cruzando tudo.
                    for (Point[] par : new Point[][] {{l.ancora(), l.dobra()}, {l.dobra(), l.ponta()}}) {
                        if (!par[0].equals(par[1])) {
                            segmentos.add(linha(par[0], par[1]));
                            donos.add(l.comando());
                        }
                    }
                }
                for (int i = 0; i < segmentos.size(); i++) {
                    for (int j = i + 1; j < segmentos.size(); j++) {
                        if (donos.get(i) != donos.get(j)) {
                            assertFalse(segmentos.get(i).intersectsLine(segmentos.get(j)),
                                    donos.get(i) + " cruza " + donos.get(j) + " em escala " + s);
                        }
                    }
                }
            }
        }
    }

    private static Line2D linha(Point a, Point b) {
        return new Line2D.Double(a.x, a.y, b.x, b.y);
    }

    @Test
    @DisplayName("rótulos da mesma coluna não se encostam e ficam dentro da faixa")
    void distribuicao() {
        int[] centros = {5, 12, 19, 27, 34, 47};
        int[] alturas = {13, 13, 13, 13, 13, 13};
        int[] y = LigacoesDoControle.distribuir(centros, alturas, 2, -40, 100);
        for (int i = 1; i < y.length; i++) {
            assertTrue(y[i] >= y[i - 1] + alturas[i - 1] + 2, "rótulo " + i + " encosta no de cima");
        }
        assertTrue(y[0] >= -40);
        assertTrue(y[y.length - 1] + alturas[y.length - 1] <= 100);
    }

    @Test
    @DisplayName("com espaço sobrando, o rótulo fica na altura da sua peça")
    void semAperto() {
        int[] y = LigacoesDoControle.distribuir(new int[] {10, 50, 90}, new int[] {10, 10, 10}, 2, 0, 200);
        assertEquals(5, y[0]);
        assertEquals(45, y[1]);
        assertEquals(85, y[2]);
    }
}
