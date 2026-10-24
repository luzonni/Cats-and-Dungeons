package com.retronova.game.dialogos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * O texto aparecendo letra a letra. Pura: anda por tick e não desenha nada.
 *
 * TEMPO, NÃO QUADRO. A velocidade é em letras por segundo e o relógio é o tick
 * fixo de 60 Hz do jogo, então a fala corre igual a 60 ou a 240 quadros.
 *
 * A PONTUAÇÃO SEGURA O RITMO: depois de ponto, exclamação e interrogação a
 * escrita para um instante, e depois de vírgula, meio instante. É o que o
 * Undertale e o Celeste fazem para a frase soar dita, e não despejada; sem a
 * pausa, duas frases na mesma fala viram uma só.
 *
 * O primeiro aperto COMPLETA (ver {@link #completar}); avançar é com quem usa.
 */
public final class MaquinaDeEscrever {

    /** Letras por segundo de cada velocidade das opções; a última é instantânea. */
    public static final int[] LETRAS_POR_SEGUNDO = {28, 48, 90, Integer.MAX_VALUE};
    /** Ticks de pausa depois do fim de frase e da vírgula. */
    public static final int PAUSA_FRASE = 9, PAUSA_VIRGULA = 4;

    private final String texto;
    private final double porTick;
    private double revelado;
    private int pausa;

    public MaquinaDeEscrever(String texto, int velocidade) {
        this.texto = texto == null ? "" : texto;
        int lps = LETRAS_POR_SEGUNDO[Math.max(0, Math.min(LETRAS_POR_SEGUNDO.length - 1, velocidade))];
        this.porTick = lps == Integer.MAX_VALUE ? Double.MAX_VALUE : lps / 60.0;
        if (lps == Integer.MAX_VALUE) {
            revelado = this.texto.length();
        }
    }

    /**
     * Anda um tick. Devolve quantas letras novas apareceram, para quem toca o
     * blip da voz decidir se toca.
     */
    public int tick() {
        if (completo()) {
            return 0;
        }
        if (pausa > 0) {
            pausa--;
            return 0;
        }
        int antes = visiveis();
        revelado = Math.min(texto.length(), revelado + porTick);
        int agora = visiveis();
        // A pausa vale para a última letra que apareceu, e só se ainda há texto depois.
        if (agora > antes && agora < texto.length()) {
            char c = texto.charAt(agora - 1);
            char seguinte = texto.charAt(agora);
            if ((c == '.' || c == '!' || c == '?') && seguinte == ' ') {
                pausa = PAUSA_FRASE;
                revelado = agora;
            } else if (c == ',' && seguinte == ' ') {
                pausa = PAUSA_VIRGULA;
                revelado = agora;
            }
        }
        return agora - antes;
    }

    /** Mostra o texto inteiro de uma vez: o primeiro clique. */
    public void completar() {
        revelado = texto.length();
        pausa = 0;
    }

    public boolean completo() {
        return visiveis() >= texto.length();
    }

    public int visiveis() {
        return (int) Math.floor(revelado);
    }

    public String texto() {
        return texto;
    }

    /** A última letra que apareceu é letra ou número (e não espaço ou pontuação)? */
    public boolean ultimaEhLetra() {
        int v = visiveis();
        return v > 0 && Character.isLetterOrDigit(texto.charAt(v - 1));
    }

    /**
     * Quebra o texto em linhas que caibam em {@code largura}, por palavra.
     *
     * @param medir largura de um trecho, na unidade de {@code largura}
     */
    public static List<String> quebrar(String texto, int largura, ToIntFunction<String> medir) {
        List<String> linhas = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String tentativa = linha.isEmpty() ? palavra : linha + " " + palavra;
            if (medir.applyAsInt(tentativa) <= largura || linha.isEmpty()) {
                linha.setLength(0);
                linha.append(tentativa);
            } else {
                linhas.add(linha.toString());
                linha.setLength(0);
                linha.append(palavra);
            }
        }
        if (!linha.isEmpty()) {
            linhas.add(linha.toString());
        }
        return linhas;
    }
}
