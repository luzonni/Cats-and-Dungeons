package com.retronova.engine.sound;

public enum Sounds {
	
	/**
	 * Pedra pesada se movendo: a passagem abrindo.
	 *
	 * DUAS TENTATIVAS ANTES DESTA, e as duas erraram por motivos opostos. O
	 * lit_sound e um acender ambiente de CINCO SEGUNDOS — mais longo que a propria
	 * animacao, entao ele atravessava a troca de sala e continuava tocando na fase
	 * seguinte, o que le como falha e nao como cena. O crack e cascalho: descrevia a
	 * corrente da grade de ferro, que nao existe mais.
	 *
	 * O earth e grave e curto, e soa como massa se deslocando — que e o que uma
	 * passagem de pedra faz ao abrir.
	 */
	Lit("earth"),

	Zombie("zombie"),
	Skeleton("bones"),
	MouseVampire("vampire"),
	MouseExplode("explosion"),
	Slime("slime"),
	Bow("bowshoot"),
	MouseSquire("mousesquire"),
	Cat("cat"),
	/**
	 * Um miado por gato jogavel.
	 *
	 * Os tres miavam com o mesmo arquivo, entao trocar de gato na tela de selecao
	 * nao soava como trocar de personagem — soava como apertar o mesmo botao tres
	 * vezes. Sao reamostragens do proprio cat.wav, feitas por tools/GenVozes.java:
	 * o Azrael grave e arrastado, o Finn agudo e curto, o Muffin no meio.
	 */
	CatMuffin("cat_muffin"),
	CatAzrael("cat_azrael"),
	CatFinn("cat_finn"),
	/**
	 * Um gemido de dor por gato, pelo mesmo motivo do miado.
	 *
	 * Levar pancada e o som que mais se ouve numa corrida, e era o mesmo arquivo
	 * para os tres. Sao reamostragens do damage_cat.wav nas MESMAS razoes das
	 * vozes: assim o gato que mia grave tambem geme grave, e o bicho continua
	 * sendo o mesmo bicho. Ver tools/GenVozes.java.
	 */
	DamageMuffin("damage_muffin"),
	DamageAzrael("damage_azrael"),
	DamageFinn("damage_finn"),
	Crack("crack"),
	Button("button"),
	/**
	 * Passar o ponteiro por cima. Mais curto e mais baixo que o Button.
	 *
	 * Sao dois sons e nao um porque passar e acionar sao coisas diferentes: o
	 * primeiro so avisa que o botao existe, o segundo confirma que algo aconteceu.
	 * Com o mesmo som nos dois, e no volume de confirmacao, atravessar a coluna de
	 * botoes disparava tres confirmacoes seguidas.
	 */
	Hover("hover"),
	Laser("laser"),
	Sword("sword"),
	Coin("coin"),
	DamageCat("damage_cat"),
	Poison("poison"),
	Walking("walking"),
	/**
	 * O portal abrindo. O woosh esticado, e nao o woosh cru.
	 *
	 * O ARQUIVO ORIGINAL NUNCA TEVE CHANCE, e a medicao fecha o assunto: o
	 * woosh.wav tem pico de -26,9 dBFS e energia media de -43,6. O crack, que e um
	 * efeito comum do jogo, tem pico de -4,2 e media de -29,1 — vinte e tres
	 * decibeis acima, ou umas catorze vezes em amplitude. Nenhum encanamento faria
	 * aquele arquivo aparecer no meio de um combate; ele estava tocando o tempo
	 * todo, mascarado.
	 *
	 * A resposta anterior foi trocar pelo crack, o que consertou o volume e perdeu
	 * o som — cascalho quebrando nao e uma passagem se abrindo. Este e o mesmo
	 * sopro de antes com corpo grave e cauda somados por tools/GenSomDoPortal.java:
	 * pico -4,0, media -20,9, um segundo e pouco. Continua sendo o woosh que
	 * agradava, agora audivel.
	 */
	Portal("portal"),

	Woosh("woosh");

	
	private final String ResourceName;
	
	Sounds(String resourceName) {
		this.ResourceName = resourceName;
	}
	
	public String resource() {
		return this.ResourceName;
	}

}
