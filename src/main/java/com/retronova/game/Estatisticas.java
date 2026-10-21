package com.retronova.game;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Os numeros de uma corrida, para a tela final: dano por fonte, baus abertos,
 * maior horda. O tempo e os abates vem do Waves.
 *
 * DANO POR ARMA. Todo dano passa por Enemy.strike, mas ele nao sabe quem bateu;
 * entao quem bate de fora da mao (Arsenal, ArmasDeArea, Furia) anuncia a fonte
 * antes e a devolve depois. O que nao anunciou nada foi a arma da mao.
 */
public final class Estatisticas {

    private Estatisticas() {
    }

    public static final String MAO = "Hand weapon";

    private static String fonte = MAO;
    private static final Map<String, Double> danoPorFonte = new LinkedHashMap<>();
    private static int baus;
    private static int maiorHorda;

    public static void reiniciar() {
        com.retronova.game.map.arena.SubirDeNivel.reiniciar();
        fonte = MAO;
        danoPorFonte.clear();
        baus = 0;
        maiorHorda = 0;
        venceu = false;
    }

    // ------------------------------------------------------------ vitoria

    private static boolean venceu;

    /**
     * Registra a vitoria sobre o chefe final e DEPOSITA o bonus na hora.
     *
     * Na hora, e nao no fim da corrida: o jogador pode escolher continuar jogando,
     * e se fechar o jogo no meio disso o bonus nao pode se perder. A segunda
     * chamada nao paga de novo.
     *
     * @return true se foi a primeira vez nesta corrida
     */
    public static boolean registrarVitoria() {
        if (venceu) {
            return false;
        }
        venceu = true;
        Meta.depositar(Meta.BONUS_DE_VITORIA);
        return true;
    }

    /** O chefe final caiu nesta corrida? */
    public static boolean venceu() {
        return venceu;
    }

    /** Quem esta batendo agora. Chame com null para voltar a arma da mao. */
    public static void fonte(String quem) {
        fonte = quem == null ? MAO : quem;
    }

    public static void dano(double quanto) {
        if (quanto > 0) {
            danoPorFonte.merge(fonte, quanto, Double::sum);
        }
    }

    public static void bauAberto() {
        baus++;
    }

    public static void horda(int vivos) {
        maiorHorda = Math.max(maiorHorda, vivos);
    }

    // ------------------------------------------------------- fim da corrida

    private static int segundos, abates, nivel, ouroGanho;
    private static String gato = "";

    /**
     * Fecha a corrida: guarda os numeros e DEPOSITA o ouro na hora — se o jogador
     * fechar o jogo na tela final, o que ele ganhou ja esta salvo.
     */
    public static void encerrar(int segundosDaCorrida, int abatesDaCorrida, int nivelDoGato, String nomeDoGato) {
        segundos = segundosDaCorrida;
        abates = abatesDaCorrida;
        nivel = nivelDoGato;
        gato = nomeDoGato;
        ouroGanho = Meta.ouroDaCorrida(segundos, abates);
        Meta.depositar(ouroGanho);
        // O bonus de vitoria ja foi depositado em registrarVitoria; aqui so entra no
        // numero mostrado, para a tela final somar o que o jogador de fato ganhou.
        if (venceu) {
            ouroGanho += Meta.BONUS_DE_VITORIA;
        }
    }

    public static int segundos() {
        return segundos;
    }

    public static int abates() {
        return abates;
    }

    public static int nivel() {
        return nivel;
    }

    public static int ouroGanho() {
        return ouroGanho;
    }

    public static String gato() {
        return gato;
    }

    public static Map<String, Double> danoPorFonte() {
        return danoPorFonte;
    }

    public static int baus() {
        return baus;
    }

    public static int maiorHorda() {
        return maiorHorda;
    }
}
