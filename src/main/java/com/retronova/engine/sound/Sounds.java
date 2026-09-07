package com.retronova.engine.sound;

public enum Sounds {
	
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
	Woosh("woosh");

	
	private final String ResourceName;
	
	Sounds(String resourceName) {
		this.ResourceName = resourceName;
	}
	
	public String resource() {
		return this.ResourceName;
	}

}
