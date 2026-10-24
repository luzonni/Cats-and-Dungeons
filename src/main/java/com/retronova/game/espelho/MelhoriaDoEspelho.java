package com.retronova.game.espelho;

import java.util.List;
import java.util.Locale;

/**
 * Uma linha da tabela do espelho (progressao/espelho.json).
 *
 * @param id        nome gravado no save permanente; nunca muda depois de lançado
 * @param nome      título na tela (inglês)
 * @param descricao uma linha do que ela faz (inglês)
 * @param icone     nome no registro de ícones (Icone)
 * @param efeito    o que o código aplica (ver {@link Espelho.Efeito})
 * @param porNivel  quanto cada nível soma, em unidades do jogo
 * @param exibicao  fator para mostrar: 100 transforma 0,03 em "3"
 * @param casas     casas decimais ao mostrar
 * @param texto     o efeito na tela, com {v} no lugar do número
 * @param custos    o preço de cada nível, em ordem; o tamanho é o nível máximo
 */
public record MelhoriaDoEspelho(String id, String nome, String descricao, String icone, Espelho.Efeito efeito,
                                double porNivel, double exibicao, int casas, String texto, List<Integer> custos) {

    public MelhoriaDoEspelho {
        custos = List.copyOf(custos);
        if (id == null || id.isBlank() || efeito == null || !Double.isFinite(porNivel)
                || porNivel <= 0 || custos.isEmpty()) {
            throw new IllegalArgumentException("Melhoria do espelho invalida: " + id);
        }
        int anterior = 0;
        for (int custo : custos) {
            if (custo <= anterior) throw new IllegalArgumentException("Custos devem crescer: " + id);
            anterior = custo;
        }
    }

    public int nivelMaximo() {
        return custos.size();
    }

    /** O preço do nível seguinte a {@code nivel}, ou -1 se já está no máximo. */
    public int custo(int nivel) {
        return nivel >= 0 && nivel < custos.size() ? custos.get(nivel) : -1;
    }

    /** Quanto custaram, somados, os {@code nivel} primeiros níveis: o reembolso de redistribuir. */
    public int gasto(int nivel) {
        int soma = 0;
        for (int i = 0; i < Math.min(nivel, custos.size()); i++) {
            soma += custos.get(i);
        }
        return soma;
    }

    /** O valor somado no nível {@code nivel}, em unidades do jogo. */
    public double valor(int nivel) {
        return porNivel * Math.max(0, Math.min(nivel, nivelMaximo()));
    }

    /** O efeito no nível, como a tela escreve: "+12 max health". Nível 0 escreve o de um nível, para a prévia. */
    public String efeitoNoNivel(int nivel) {
        double v = valor(Math.max(1, nivel)) * exibicao;
        return texto.replace("{v}", String.format(Locale.ROOT, "%." + casas + "f", v));
    }
}
