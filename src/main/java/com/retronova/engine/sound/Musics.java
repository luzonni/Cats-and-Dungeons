package com.retronova.engine.sound;

public enum Musics {

    // Sem uso no momento, de proposito. Tocava na selecao de personagem e foi
    // tirada de la; fica carregada e pronta para quando houver um lugar melhor.
    Geral("geral"),
    Menu("menu_principal"),
    /**
     * Leito ambiente da tela de titulo: vento e goteira, bem abaixo da musica.
     *
     * Toca JUNTO com a Menu, e nao no lugar dela. Duas faixas somadas dao a
     * profundidade que uma so nao da — a musica conduz, o ambiente diz que existe
     * um lugar em volta. O nivel esta gravado no arquivo, uns vinte decibeis
     * abaixo da trilha, para nunca disputar com ela.
     */
    Ambiente("menu_ambient"),
    // Antecamara: "Cave Theme", de Brandon75689, CC0. A faixa anterior era clara
    // e agitada, leitura de espaco e nao de masmorra. Ver docs/TERCEIROS.md.
    Room("dungeon_hall"),
    /**
     * As trilhas de arena. A escolha e do jogador, em Options > Audio.
     *
     * A original e animada demais para o que o jogo virou: casa com o gato, nao
     * com a masmorra. Em vez de troca-la e decidir pelos outros, ela ficou como
     * uma das opcoes — quem gosta continua com ela. Todas sao CC0 e estao
     * listadas em docs/TERCEIROS.md.
     */
    Fight("fight"),
    FightSpooky("fight_spooky"),
    Fight8Bit("fight_8bit"),
    FightRpg("fight_rpg"),
    /**
     * A trilha de CHEFE, e nao mais uma da lista.
     *
     * Ela nao entra no sorteio das arenas comuns de proposito: musica de chefe
     * que toca o tempo todo deixa de anunciar chefe nenhum. O que a torna
     * especial e ela nao tocar — quando entra, o jogador ja sabe o que vem.
     */
    FightBoss("fight_boss"),
    GameOver("game_over");

    /** As trilhas de arena comum, na ordem do menu. O indice vai no config.json. */
    public static final Musics[] COMBATE = {Fight, FightSpooky, Fight8Bit, FightRpg};

    /** Os nomes mostrados no menu, na mesma ordem de COMBATE. */
    public static final String[] COMBATE_NOMES = {"Upbeat", "Spooky", "8-bit", "RPG"};

    /** A trilha de arena escolhida, ou a primeira se o config vier estranho. */
    /**
     * A trilha de quando nao ha mais nada para matar.
     *
     * E A MESMA DO SAGUAO, e isso e escolha e nao economia. O saguao e o lugar
     * seguro do jogo, e o jogador ja aprendeu essa faixa como "ninguem esta te
     * atacando". Reusa-la na arena limpa faz o silencio depois da briga soar igual
     * ao da antecamara — a corrida inteira passa a ter uma sonoridade so para
     * "respire", em vez de duas maneiras diferentes de dizer a mesma coisa.
     *
     * Procurei trilha nova antes de decidir isso. As candidatas CC0 de masmorra sao
     * TENSAS, com sussurro e rangido, que e o oposto do que o momento pede; as
     * calmas sao violao e lo-fi, de outro genero; e a unica com o clima certo,
     * cordas e sintetizador, e CC-BY — e o projeto inteiro e CC0. Nenhuma delas
     * melhoraria o que ja existe aqui.
     */
    public static Musics calma() {
        return Room;
    }

    public static Musics combate() {
        int i = com.retronova.engine.Configs.BattleMusic();
        return COMBATE[i < 0 || i >= COMBATE.length ? 0 : i];
    }

    /**
     * Para todas as trilhas de briga, a de chefe inclusive.
     *
     * Sao varias e so uma toca por vez, mas quem sai da arena nao sabe qual era —
     * e trocar de faixa no meio da partida deixaria a antiga tocando para sempre
     * por baixo da musica da sala.
     */
    /** Esta faixa e uma das trilhas de briga? */
    public static boolean deCombate(Musics m) {
        if (m == FightBoss) {
            return true;
        }
        for (Musics c : COMBATE) {
            if (c == m) {
                return true;
            }
        }
        return false;
    }

    public static void pararCombate() {
        for (Musics m : COMBATE) {
            Sound.stop(m);
        }
        Sound.stop(FightBoss);
    }

    private final String ResourceName;

    Musics(String resourceName) {
        this.ResourceName = resourceName;
    }

    public String resource() {
        return this.ResourceName;
    }

}
