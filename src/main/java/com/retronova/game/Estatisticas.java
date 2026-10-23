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
        conquistasDaCorrida.clear();
    }

    /**
     * Retomada: os numeros que a corrida ja tinha juntado.
     *
     * A VITORIA VOLTA SEM PAGAR DE NOVO. O bonus foi depositado no Meta quando o
     * Rato Rei caiu; aqui so se lembra que ele caiu, para a tela final somar o
     * bonus no que mostra e para uma segunda queda nao contar.
     */
    public static void repor(int bausGravados, int maiorHordaGravada, boolean venceuGravado,
                             Map<String, Double> danoGravado) {
        baus = Math.max(0, bausGravados);
        maiorHorda = Math.max(0, maiorHordaGravada);
        venceu = venceuGravado;
        danoPorFonte.clear();
        danoPorFonte.putAll(danoGravado);
    }

    // ------------------------------------------------------------ vitoria

    private static boolean venceu;

    /**
     * Registra a vitoria sobre o chefe final.
     *
     * O BONUS NAO E PAGO AQUI. Ele era depositado na hora, para nao se perder se o
     * jogo fechasse depois da vitoria; com o credito unico por corrida (ver
     * encerrar e save/Progresso), a vitoria fica gravada no save da corrida e o
     * bonus entra junto com o resto quando ela acaba. Pagar aqui e no fim seria
     * pagar duas vezes.
     *
     * @return true se foi a primeira vez nesta corrida
     */
    public static boolean registrarVitoria(String gato) {
        if (venceu) {
            return false;
        }
        venceu = true;
        conquistasDaCorrida.addAll(Conquistas.registrarVitoria(gato));
        return true;
    }

    /** O que esta corrida desbloqueou, na ordem, para a tela final avisar. */
    private static final java.util.List<Conquistas.Conquista> conquistasDaCorrida = new java.util.ArrayList<>();

    public static java.util.List<Conquistas.Conquista> conquistasDaCorrida() {
        return java.util.Collections.unmodifiableList(conquistasDaCorrida);
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
        encerrar(null, segundosDaCorrida, abatesDaCorrida, nivelDoGato, nomeDoGato);
    }

    /**
     * Fecha a corrida e CREDITA AS MOEDAS no save permanente, uma vez só por id
     * (ver save/Progresso): se a morte for processada de novo, ou o jogo fechar e
     * a corrida for lida outra vez, o segundo pedido não paga nada.
     *
     * O bônus de vitória entra aqui, junto com o resto, e não mais na hora da
     * vitória: o crédito é um só, no fim. Se o jogo fechar depois de vencer, a
     * corrida fica gravada com a vitória e o bônus vem quando ela acabar (ou
     * pelo abandono, se o espaço for apagado).
     *
     * @param idDaCorrida null só na bancada, que não credita nada
     */
    public static void encerrar(String idDaCorrida, int segundosDaCorrida, int abatesDaCorrida,
                                int nivelDoGato, String nomeDoGato) {
        segundos = segundosDaCorrida;
        abates = abatesDaCorrida;
        nivel = nivelDoGato;
        gato = nomeDoGato;
        var resultado = venceu ? com.retronova.game.save.RegrasDeMoedas.Resultado.VITORIA
                : com.retronova.game.save.RegrasDeMoedas.Resultado.MORTE;
        ouroGanho = com.retronova.game.save.RegrasDeMoedas.atuais()
                .moedas(resultado, segundos, abates, Corrida.ganancia(nomeDoGato));
        if (idDaCorrida != null) {
            com.retronova.game.save.Progresso.creditar(idDaCorrida, nomeDoGato, ouroGanho);
        }
        conquistasDaCorrida.addAll(Conquistas.registrarCorrida(segundos, abates, nivel, baus, maiorHorda));
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
