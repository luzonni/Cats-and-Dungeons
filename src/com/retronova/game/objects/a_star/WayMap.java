package com.retronova.game.objects.a_star;

import com.retronova.game.Game;
import com.retronova.game.objects.tiles.Tile;

class WayMap {
	
	private final Node[] nodes;
	private final int X, Y;
	private final int LENGTH;
	private final Node start_Node;
	private final Node goal_Node;
	
	public WayMap(Node start, Node end, int range) throws RuntimeException {
		this.start_Node = start;
		this.goal_Node = end;
		this.LENGTH = range*2;
		nodes = new Node[(int)Math.pow(LENGTH, 2)];
		this.X = start.x - range;
		this.Y = start.y - range;
		int xD = Math.abs(start.x - end.x);
		int yD = Math.abs(start.y - end.y);
		if(xD >= range || yD >= range)
			throw new RuntimeException("The end node not in range");
		buildMap(range);
		setNode(X, Y, start_Node);
		if(getNode(goal_Node.x, goal_Node.y).isSolid())
			throw new RuntimeException("The goal_Node is solid");
		setNode(goal_Node.x, goal_Node.y, goal_Node);
	}
	
	private void buildMap(int range) {
		int X = start_Node.x;
		int Y = start_Node.y;
		for(int yy = Y-range; yy < Y+range; yy++)
			for(int xx = X-range; xx < X+range; xx++) {
				Tile curTile;
				Node node = new Node(xx,  yy);
				try {
					curTile = Game.getMap().getTile(xx, yy);
					node.setSolid(curTile.isSolid());
				} catch (RuntimeException e) {
					node.setSolid(true);
				}
				//Colliding path with entity
//				if(!node.isSolid() || curTile != null)
//					for(int i = 0; i < Space.OBJECTS.size(); i++) {
//						Objects o = Space.OBJECTS.get(i);
//						if(o instanceof Entity || o instanceof Tile)
//							continue;
//						if(o.isCollider() && o.getHitBox().getBounds().intersects(curTile.getBounds())) {
//							node.setSolid(true);
//						}
//					}
				getCost(node);
				setNode(xx, yy, node);
			}
		
	}
	
	private void getCost(Node node) {
		node.G_Cost = getDistance(node, start_Node);
		node.H_Cost = getDistance(node, goal_Node);
		node.F_Cost = node.G_Cost + node.H_Cost;
	}
	
	private double getDistance(Node current, Node goal) {
		double dx = Math.abs(current.x - goal.x);
		double dy = Math.abs(current.y - goal.y);
		return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
	}
	
	public void setNode(int x, int y, Node node) {
		if(containsNode(x, y)) {
			int index = getIndex(x, y);
			nodes[index] = node;
		}
	}
	
	public boolean containsNode(int x, int y) {
		try {
			getIndex(x, y);
		}catch (RuntimeException e) {
			return false;
		}
		return true;
	}
	
	public Node getNode(int x, int y) {
		return nodes[getIndex(x, y)];
	}

	private int getIndex(int x, int y) {
		int xx = x - X;
		int yy = y - Y;
		if(xx < 0 || xx >= LENGTH || yy < 0 || yy >= LENGTH)
			throw new RuntimeException("Values outside bounds");
		return xx+yy*LENGTH;
	}
	
}
