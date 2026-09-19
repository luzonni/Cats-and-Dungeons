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
	
	// O CONSTRUTOR QUE RECEBIA PIXELS FOI REMOVIDO.
	//
	// Ele dividia por GameObject.SIZE() para virar tile, e era o unico lugar do A*
	// que falava em pixels. Bastou quem chamava passar a converter antes — coisa
	// razoavel de se fazer — para a divisao acontecer duas vezes e todo caminho
	// nascer colado na origem da grade. Tirando o construtor, a unidade deixa de
	// ser uma convencao que alguem precisa lembrar: aqui so existe tile.
	
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
