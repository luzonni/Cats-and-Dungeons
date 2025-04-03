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
	Crack("crack"),
	Button("button"),
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
