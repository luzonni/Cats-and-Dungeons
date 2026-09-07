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
    Fight("fight"),
    GameOver("game_over");

    private final String ResourceName;

    Musics(String resourceName) {
        this.ResourceName = resourceName;
    }

    public String resource() {
        return this.ResourceName;
    }

}
