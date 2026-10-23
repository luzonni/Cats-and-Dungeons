package com.retronova.game.items;

import com.retronova.engine.Engine;
import com.retronova.game.Corrida;
import com.retronova.game.Estatisticas;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * O que um baú dá, e como isso chega ao gato. Só a regra: nada aqui desenha.
 *
 * ---------------------------------------------------------------------------
 * POR QUE ISTO SAIU DA ROLETA
 *
 * O sorteio, a evolução garantida, a contagem nas estatísticas e a entrega
 * moravam dentro do caça-níquel (Roleta), misturados ao desenho dos rolos. Trocar
 * a apresentação pelo baú (AberturaDoBau) exigia mexer na regra junto. Separadas,
 * a tela pode mudar de novo sem tocar no que o jogador recebe.
 *
 * ---------------------------------------------------------------------------
 * A REGRA (a mesma da roleta, sem mudança)
 *
 *   1. Evolução pronta (arma no máximo + passiva parceira) sai garantida.
 *   2. Senão, arma extra em 40% das vezes — 55% enquanto o gato não tem
 *      nenhuma —, sorteada entre as disponíveis, no próximo nível.
 *   3. Senão, uma melhoria de atributo, com o grau sorteado à parte
 *      (Melhorias.oferecer, Raridade.sortear), promovida pela sorte de
 *      Modifiers.Fortune.
 *
 * O prêmio é decidido ao abrir, antes de qualquer animação: a apresentação só
 * mostra o que já saiu, e é por isso que a roleta "sempre dava jackpot".
 */
public final class RecompensaDoBau {

    private RecompensaDoBau() {
    }

    /** Chance de arma extra quando o gato já tem alguma, e quando ainda não tem nenhuma. */
    public static final double CHANCE_DE_ARMA = 0.40, CHANCE_DE_ARMA_SEM_NENHUMA = 0.55;

    /** nivelNovo especial: o prêmio é a EVOLUÇÃO da arma. */
    public static final int EVOLUCAO = -1;

    /**
     * Um prêmio: uma melhoria de atributo (carta) ou uma arma extra (ou o próximo
     * nível dela, ou a evolução). Uma das duas é nula.
     */
    public record Premio(Melhorias.Carta carta, ArmaExtra arma, int nivelNovo) {

        public boolean evolucao() {
            return nivelNovo == EVOLUCAO;
        }

        /** A cor do prêmio: a da raridade, a da arma, ou o ouro da evolução. */
        public Color cor() {
            if (evolucao()) {
                return Raridade.LENDARIO.cor();
            }
            return arma != null ? arma.cor() : carta.raridade().cor();
        }

        /**
         * O grau que a apresentação usa para o brilho e para o som. A carta tem o
         * dela; a arma não tem raridade, então vale pelo peso: evolução é lendária,
         * arma nova é épica, nível de arma é raro.
         */
        public Raridade grau() {
            if (evolucao()) {
                return Raridade.LENDARIO;
            }
            if (arma != null) {
                return nivelNovo <= 1 ? Raridade.EPICO : Raridade.RARO;
            }
            return carta.raridade();
        }

        public String rotulo() {
            if (evolucao()) {
                return "EVOLUTION!";
            }
            if (arma == null) {
                return carta.raridade().rotulo();
            }
            return nivelNovo <= 1 ? "NEW WEAPON" : "WEAPON LV " + nivelNovo;
        }

        public String nome() {
            if (evolucao()) {
                return Evolucao.de(arma).nome();
            }
            return arma != null ? arma.nome() : carta.melhoria().nome();
        }

        public String efeito() {
            if (evolucao()) {
                return Evolucao.de(arma).descricao();
            }
            return arma != null ? arma.efeito(nivelNovo) : carta.efeito();
        }

        /** O ícone do prêmio (ver Icone): o da arma ou o da melhoria. */
        public Icone icone() {
            return arma != null ? Icone.de(arma) : Icone.de(carta.melhoria());
        }

        /**
         * A cor do SÍMBOLO, que é do assunto e não da raridade: um coração comum
         * cinza sumia no fundo. A raridade vai na moldura.
         */
        public Color corDoIcone() {
            if (arma != null) {
                return arma.cor();
            }
            return switch (carta.melhoria()) {
                case VIGOR -> new Color(0xE0334F);
                case FURIA -> new Color(0xF07A22);
                case COURO -> new Color(0x5B8BD6);
                case PATAS -> new Color(0x2FB7D6);
                case FRENESI -> new Color(0xB45CF0);
                case INSTINTO -> new Color(0x3DBF52);
                case PRESAGIO -> new Color(0xE0A800);
            };
        }
    }

    /** A chance de cada carta subir um degrau, da sorte de Modifiers.Fortune. */
    public static double promocao(Player gato) {
        return gato.hasModifier(Modifiers.Fortune) ? gato.valueModifier(Modifiers.Fortune) : 0;
    }

    /**
     * O prêmio de um baú aberto agora, e o baú contado nas estatísticas. Chame uma
     * vez por baú, ao abrir.
     */
    public static Premio abrir(Player gato) {
        Melhorias melhorias = gato.getMelhorias();
        Estatisticas.bauAberto();
        // EVOLUÇÃO PRONTA SAI GARANTIDA: arma no máximo + passiva parceira fazem o
        // próximo baú ser dela. É o jackpot de verdade da corrida.
        Evolucao evolucao = melhorias.evolucaoDisponivel();
        if (evolucao != null) {
            return new Premio(null, evolucao.arma(), EVOLUCAO);
        }
        return sortear(melhorias, promocao(gato));
    }

    /**
     * Um prêmio possível, sem contar o baú: o sorteio da regra 2 e 3. A roleta o
     * usava também para encher os rolos.
     */
    public static Premio sortear(Melhorias melhorias, double promocao) {
        List<ArmaExtra> armas = melhorias.armasDisponiveis();
        boolean temAlguma = Arrays.stream(ArmaExtra.values()).anyMatch(a -> melhorias.nivel(a) > 0);
        double chanceArma = temAlguma ? CHANCE_DE_ARMA : CHANCE_DE_ARMA_SEM_NENHUMA;
        if (!armas.isEmpty() && Engine.RAND.nextDouble() < chanceArma) {
            ArmaExtra a = armas.get(Engine.RAND.nextInt(armas.size()));
            return new Premio(null, a, melhorias.nivel(a) + 1);
        }
        Melhorias.Carta c = melhorias.oferecer(1, promocao).getFirst();
        return new Premio(c, null, 0);
    }

    /**
     * Uma amostra de cada coisa que PODERIA sair deste baú — as armas disponíveis
     * no próximo nível e cada melhoria de atributo —, para a prévia da tela. Só
     * leitura: não sorteia nem conta. As melhorias vêm como comuns; o grau só se
     * sabe ao abrir.
     */
    public static List<Premio> possiveis(Melhorias melhorias) {
        List<Premio> amostras = new ArrayList<>();
        for (ArmaExtra a : melhorias.armasDisponiveis()) {
            amostras.add(new Premio(null, a, melhorias.nivel(a) + 1));
        }
        for (Melhoria m : Melhoria.values()) {
            amostras.add(new Premio(new Melhorias.Carta(m, Raridade.COMUM, melhorias.quantas(m)), null, 0));
        }
        return amostras;
    }

    /** Entrega o prêmio ao gato e grava a corrida. */
    public static void entregar(Player gato, Premio premio) {
        if (premio.evolucao()) {
            gato.getMelhorias().evoluir(Evolucao.de(premio.arma()));
        } else if (premio.arma() != null) {
            gato.getMelhorias().subirArma(premio.arma());
        } else {
            gato.getMelhorias().aplicar(gato, premio.carta().melhoria(), premio.carta().raridade());
        }
        Corrida.gravar();
    }
}
