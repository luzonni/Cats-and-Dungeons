package com.retronova.game.save;

import com.retronova.engine.Engine;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Quanto uma corrida rende de moedas, lido de {@code progressao/espelho.json}.
 *
 * O TEMPO E OS ABATES PAGAM, como já pagavam (Meta.ouroDaCorrida, que esta
 * regra substitui com os mesmos números): seis segundos valem uma moeda, oito
 * abates valem uma, e vencer o chefe final soma um bônus.
 *
 * O RESULTADO DÁ A FRAÇÃO, e os padrões seguem o Hades: morrer não tira nada
 * (no Hades a Escuridão juntada fica com o jogador na morte; perder o que se
 * ganhou numa derrota é o que faz o jogador desistir da corrida seguinte),
 * vencer também é integral, e ABANDONAR rende metade. Metade, e não zero, porque
 * zero puniria quem precisa largar uma corrida ruim; metade, e não tudo, porque
 * tudo transformaria "abandonar logo depois de um bom começo" na estratégia
 * mais rápida de juntar moedas.
 */
public record RegrasDeMoedas(double segundosPorMoeda, double abatesPorMoeda, int bonusDeVitoria,
                             double fracaoMorte, double fracaoVitoria, double fracaoAbandono) {

    /** Os números de fábrica, iguais aos do arquivo. Servem quando ele não abre. */
    public static final RegrasDeMoedas PADRAO = new RegrasDeMoedas(6, 8, 200, 1.0, 1.0, 0.5);

    /** Como a corrida terminou. */
    public enum Resultado {
        MORTE, VITORIA, ABANDONO
    }

    private static RegrasDeMoedas atuais;

    /** As regras do arquivo, lidas uma vez. */
    public static synchronized RegrasDeMoedas atuais() {
        if (atuais == null) {
            atuais = carregar();
        }
        return atuais;
    }

    private static RegrasDeMoedas carregar() {
        try (InputStream in = RegrasDeMoedas.class.getResourceAsStream(
                Engine.resPath + "progressao/espelho.json")) {
            if (in == null) {
                return PADRAO;
            }
            JSONObject raiz = (JSONObject) new JSONParser().parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return deJson((JSONObject) raiz.get("moedas"));
        } catch (Exception falha) {
            System.err.println("[progressao] regras de moedas ilegiveis, usando as de fabrica: " + falha);
            return PADRAO;
        }
    }

    static RegrasDeMoedas deJson(JSONObject m) {
        if (m == null) {
            return PADRAO;
        }
        JSONObject f = (JSONObject) m.get("fracao");
        return new RegrasDeMoedas(
                num(m.get("segundosPorMoeda"), PADRAO.segundosPorMoeda),
                num(m.get("abatesPorMoeda"), PADRAO.abatesPorMoeda),
                (int) num(m.get("bonusDeVitoria"), PADRAO.bonusDeVitoria),
                f == null ? PADRAO.fracaoMorte : num(f.get("morte"), PADRAO.fracaoMorte),
                f == null ? PADRAO.fracaoVitoria : num(f.get("vitoria"), PADRAO.fracaoVitoria),
                f == null ? PADRAO.fracaoAbandono : num(f.get("abandono"), PADRAO.fracaoAbandono));
    }

    /**
     * As moedas de uma corrida.
     *
     * @param ganancia multiplicador vindo do espelho (1 sem a melhoria)
     */
    public int moedas(Resultado r, int segundos, int abates, double ganancia) {
        double bruto = Math.max(0, segundos) / Math.max(1e-9, segundosPorMoeda)
                + Math.max(0, abates) / Math.max(1e-9, abatesPorMoeda)
                + (r == Resultado.VITORIA ? bonusDeVitoria : 0);
        double fracao = switch (r) {
            case MORTE -> fracaoMorte;
            case VITORIA -> fracaoVitoria;
            case ABANDONO -> fracaoAbandono;
        };
        return (int) Math.floor(bruto * Math.max(0, fracao) * Math.max(0, ganancia));
    }

    private static double num(Object v, double padrao) {
        return v instanceof Number n ? n.doubleValue() : padrao;
    }
}
