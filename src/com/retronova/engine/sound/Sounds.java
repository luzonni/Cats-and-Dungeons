package com.retronova.engine.sound;

import com.retronova.game.objects.entities.RatExplode;

public enum Sounds {
	
	Zombie("zombie_sound"),
	Skeleton("bones"),
	MouseVampire("vampire_sound"),
	RatExplode("lit_sound"),
	Slime("slime_sound"),
	Bow("bowshoot");
	
	private final String ResourceName;
	
	Sounds(String resourceName) {
		this.ResourceName = resourceName;
	}
	
	public String resource() {
		return this.ResourceName;
	}

}
