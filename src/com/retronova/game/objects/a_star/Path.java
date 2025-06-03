package com.retronova.game.objects.a_star;

import com.retronova.engine.Configs;
import com.retronova.game.Game;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.tiles.Tile;

import java.awt.*;

public class Path implements Runnable {

	private int range;
	private A_Star aStar;
	private Entity entity;
	private Point end;
	
	public void buildPath(Entity entity, Point End, int range) {
		this.entity = entity;
		this.end = End;
		this.range = range;
		new Thread(this).start();
	}
	
	public void follow() {
		if(aStar == null || aStar.getPath() == null || aStar.getPath().isEmpty() || entity == null)
			return;
		Node last = getLastNode();
		if(TileIsSolid(last))
			aStar.getPath().clear();
		int nodeX = last.x * GameObject.SIZE();
		int nodeY = last.y * GameObject.SIZE();
		int x = (int)entity.getX();
		int y = (int)entity.getY();
		double r = Math.atan2((nodeY - y), (nodeX - x));
		entity.getPhysical().addForce("a_star", entity.getSpeed(), r);
		if(entity.getBounds().contains(nodeX + GameObject.SIZE()/2, nodeY + GameObject.SIZE()/2))
			arrived();
	}
	
	private boolean TileIsSolid(Node node) {
		Tile tile = Game.getMap().getTile(node.x, node.y);
		if(tile == null)
			return true;
		return tile.isSolid();
	}
	
	private Node getLastNode() {
		return aStar.getPath().getLast();
	}
	
	private void arrived() {
		aStar.getPath().removeLast();
	}

	public boolean isEmpty() {
		return (aStar == null || aStar.getPath() == null || aStar.getPath().isEmpty());
	}

	public void render(Graphics g) {
		if(aStar != null && aStar.getPath() != null)
			for(Node node : aStar.getPath()) {
				int xi = node.x* GameObject.SIZE() + GameObject.SIZE()/2;
				int yi = node.y*GameObject.SIZE() + GameObject.SIZE()/2;
				int xF = xi + GameObject.SIZE()/2;
				int yF = yi + GameObject.SIZE()/2;
				if(node.parent != null) {
					xF = node.parent.x*GameObject.SIZE() + GameObject.SIZE()/2;
					yF = node.parent.y*GameObject.SIZE() + GameObject.SIZE()/2;
				}
				Graphics2D g2 = (Graphics2D)g;
				g2.setStroke(new BasicStroke(Configs.GameScale()));
				g2.setColor(Color.red);
				g2.drawLine(xi, yi, xF, yF);
			}
	}

	@Override
	public synchronized void run() {
		try {
			Point middle = new Point((int) entity.getBounds().getCenterX(), (int) entity.getBounds().getCenterY());
			this.aStar = new A_Star(middle, end, range);
		} catch (RuntimeException e) {
			System.out.println("Erro!");
		}
	}

}
