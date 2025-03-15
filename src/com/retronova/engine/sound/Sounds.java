package com.retronova.engine.sound;

public enum Sounds {
	
	Zombie("zombie_sound"),
	Skeleton("bones"),
	MouseVampire("vampire_sound"),
	MouseExplode("explosion"),
	Slime("slime1"),
	Bow("bowshoot"),
	MouseSquire("mousesquire"),;
	
	private final String ResourceName;
	
	Sounds(String resourceName) {
		this.ResourceName = resourceName;
	}
	
	public String resource() {
		return this.ResourceName;
	}

}
