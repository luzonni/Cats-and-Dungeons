package com.retronova.engine.sound;

public enum Sounds {
	
	Click("click"), Poft("poft"), Clear("clear"), Place("place"), Die("die");
	
	private final String ResourceName;
	
	Sounds(String resourceName) {
		this.ResourceName = resourceName;
	}
	
	public String resource() {
		return this.ResourceName;
	}

}
