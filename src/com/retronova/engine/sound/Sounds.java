package com.retronova.engine.sound;

public enum Sounds {
	
	Zombie("zombie_sound");
	
	private final String ResourceName;
	
	Sounds(String resourceName) {
		this.ResourceName = resourceName;
	}
	
	public String resource() {
		return this.ResourceName;
	}

}
