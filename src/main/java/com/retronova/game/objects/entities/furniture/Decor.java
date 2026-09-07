package com.retronova.game.objects.entities.furniture;

/**
 * Mobília decorativa. Sem comportamento: só ocupa espaço e conta história.
 *
 * A pesquisa em documentation/padroes/ARTE-CENARIO.md é direta no ponto:
 * ambientes convincentes vêm de quantidade e variedade de peças simples, não de
 * peças individualmente complexas. Uma classe por prop daria sete arquivos
 * idênticos a menos do nome do sprite, então o que varia — arte, se bloqueia
 * passagem, se fica no chão — vira parâmetro.
 *
 * Quem tiver comportamento próprio continua com classe própria: {@link Gate}
 * carrega o comando de descida, {@link Brazier} anima o fogo.
 */
public class Decor extends Furniture {

    /**
     * @param sprite  nome do arquivo em sprites/objects/furniture, sem extensão
     * @param solido  false para peças que o jogador atravessa — tapetes, cacos
     * @param noChao  true para peças rasas, que nunca devem passar na frente do
     *                gato; a ordenação por profundidade é ignorada
     */
    public Decor(int ID, double x, double y, String sprite, boolean solido, boolean noChao) {
        super(ID, x, y, 1000, solido);
        loadSprites(sprite);
        if (noChao) {
            setGroundObject();
        }
    }

    @Override
    public void tick() {
    }
}
