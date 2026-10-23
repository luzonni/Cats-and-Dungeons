package com.retronova.game.dialogos;

import com.retronova.engine.Engine;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Player;

import java.util.Random;

/**
 * A porta de entrada dos diálogos: quem quer que alguém fale chama daqui.
 *
 * TRÊS GATILHOS, um caminho só:
 *  - {@link #conversar}: o jogador interagiu com um personagem (o vendedor);
 *  - {@link #aoEvento}: algo aconteceu no jogo ("fim_de_corrida", por exemplo), e
 *    qualquer personagem com conversa para esse gatilho pode falar;
 *  - falas soltas ("barks", gatilho "fala_solta"): o personagem comenta algo
 *    durante a corrida, sem parar o jogo. A escolha é a mesma
 *    ({@link #escolher}); falta só o balão que não pausa, que não existe ainda.
 *
 * Em todos, a conversa é escolhida pelas condições e pela prioridade
 * (EscolhaDeConversa), marcada como vista na memória e mostrada na caixa.
 */
public final class Dialogos {

    private Dialogos() {
    }

    private static final Random SORTEIO = new Random();

    /** Gatilhos conhecidos. Texto livre nos dados; estes são os que o jogo dispara hoje. */
    public static final String INTERAGIR = "interagir", FIM_DE_CORRIDA = "fim_de_corrida",
            FALA_SOLTA = "fala_solta";

    /**
     * O jogador falou com {@code personagem}. Abre a conversa escolhida e devolve
     * true; sem conversa que sirva, devolve false — e quem chamou segue como
     * fazia antes (o vendedor abre a loja direto).
     *
     * @param falante a entidade que fala; os eventos do fim recebem ela
     */
    public static boolean conversar(String personagem, Object falante) {
        Conversa c = escolher(personagem, INTERAGIR);
        if (c == null) {
            return false;
        }
        mostrar(c, falante);
        return true;
    }

    /** Algo aconteceu no jogo: toca a conversa de maior prioridade de qualquer personagem. */
    public static boolean aoEvento(String gatilho, Object falante) {
        Conversa melhor = null;
        for (String personagem : CatalogoDeDialogos.get().personagens().keySet()) {
            Conversa c = escolher(personagem, gatilho);
            if (c != null && (melhor == null || c.prioridade() > melhor.prioridade())) {
                melhor = c;
            }
        }
        if (melhor == null) {
            return false;
        }
        mostrar(melhor, falante);
        return true;
    }

    /** A conversa que tocaria agora, sem mostrar nem marcar. */
    public static Conversa escolher(String personagem, String gatilho) {
        return EscolhaDeConversa.escolher(CatalogoDeDialogos.get().conversas(), personagem, gatilho,
                contexto(), SORTEIO);
    }

    /** Mostra uma conversa pelo id, sem condição e sem marcar como vista. Bancada (-Dcd.dialogo). */
    public static boolean mostrarParaTeste(String id, Object falante) {
        for (Conversa c : CatalogoDeDialogos.get().conversas()) {
            if (c.id().equals(id)) {
                Engine.pause(new CaixaDeDialogo(c, falante));
                return true;
            }
        }
        System.err.println("[dialogos] conversa nao encontrada: " + id);
        return false;
    }

    private static void mostrar(Conversa c, Object falante) {
        MemoriaDosDialogos.marcar(c);
        Engine.pause(new CaixaDeDialogo(c, falante));
    }

    /** A fotografia do jogo agora, para as condições. */
    public static ContextoDoDialogo contexto() {
        Player gato = Game.getPlayer();
        return new ContextoDoDialogo(
                gato == null ? "" : gato.getName(),
                gato == null ? 0 : gato.getMoney(),
                com.retronova.game.Meta.ouro(),
                com.retronova.game.Conquistas.progresso().vitorias(),
                MemoriaDosDialogos.dados());
    }
}
