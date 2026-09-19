package com.retronova.game.objects.entities;

public enum Modifiers {
    //Modificadores são valores que alteram os atributos das entidades.
    // True = valores que são somados conforme forem adicionados as entidades.
    // False = Um valor unico, não amontoavel, que pode ser apenas subistituido.
    Life(true),
    Range(true),
    Damage(true),
    Speed(true),
    /**
     * Intervalo entre golpes, em ticks. MAIOR E MAIS LENTO.
     *
     * Passou a somar. Era de substituicao, e substituicao nao serve a um sistema de
     * cartas: a segunda carta de cadencia apagaria a primeira em vez de somar com
     * ela, e o jogador veria a melhoria que acabou de escolher nao acontecer.
     */
    AttackSpeed(true),
    /**
     * Quanto do dano recebido e descontado, de 0 a 1.
     *
     * NAO EXISTIA. O jogo ja tinha resistencia POR TIPO DE DANO — e assim que o
     * esqueleto resiste a fogo —, mas nada dava ao jogador uma reducao geral, e o
     * mapa de resistencias dele nascia e morria vazio. Sem isto, "Resistencia" nao
     * tinha onde ser escrita.
     */
    Resistance(true),
    /**
     * Chance de uma carta futura vir um grau acima, de 0 a 1.
     *
     * E O UNICO MODIFICADOR QUE NAO E ATRIBUTO DE COMBATE, e vale dizer por que ele
     * mora aqui mesmo assim: este enum ja resolve empilhamento, ja e lido pelo
     * Entity e ja viaja no save da corrida. Dar a esta chance uma casa propria
     * significaria reescrever as tres coisas para um campo so — e o gato de fato
     * carrega isto pela corrida inteira, como carrega vida e dano.
     */
    Fortune(true),
    Luck(true),
    Dodge(false),
    Dash(false),
    Poisoner(false);


    private final boolean heapable;

    Modifiers(boolean heapable) {
        this.heapable = heapable;
    }

    public boolean getHeapable() {
        return this.heapable;
    }

}
