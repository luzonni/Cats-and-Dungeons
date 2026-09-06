package com.retronova.game.objects.a_star;

import com.retronova.game.objects.GameObject;

import java.awt.*;

class Node {
	
	public int x, y;
	public Node parent;
	public double F_Cost, G_Cost, H_Cost;
	public boolean solid;
	public boolean open;
	public boolean checked;
	
	public Node(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public Node(Point position) {
		this.x = position.x / GameObject.SIZE();
		this.y = position.y / GameObject.SIZE();
	}
	
	public void setSolid(boolean solid) {
		this.solid = solid;
	}
	
	public void setAsChecked() {
		this.checked = true;
	}
	
	public void setAsOpen() {
		this.open = true;
	}
	
	public boolean isSolid() {
		return this.solid;
	}
	
}
