package com.retronova.game.objects.a_star;

class WayMap {
	
	private final Node[] nodes;
	private final int X, Y;
	private final int LENGTH;
	final Node start, goal;
	
	public WayMap(Node start, Node end, int range, CheckerNode checker) throws RuntimeException {
		this.start = start;
		this.goal = end;
		this.LENGTH = range * 2;
		this.nodes = new Node[(int)Math.pow(LENGTH, 2)];

		this.X = start.x - range;
		this.Y = start.y - range;

		int xD = Math.abs(start.x - end.x);
		int yD = Math.abs(start.y - end.y);
		if(xD >= range || yD >= range)
			throw new RuntimeException("The end node not in range");

		buildMap(range, checker);
		setNode(X, Y, this.start);

		if(getNode(goal.x, goal.y).isSolid())
			throw new RuntimeException("The goal_Node is solid");
		setNode(goal.x, goal.y, goal);
	}
	
	private void buildMap(int range, CheckerNode checkerNode) {
		for(int y = start.y - range; y < start.y + range; y++) {
			for (int x = start.x - range; x < start.x + range; x++) {
				Node node = new Node(x, y);
				node.setSolid(checkerNode.check(x, y));
				getCost(node);
				setNode(x, y, node);
			}
		}
	}
	
	private void getCost(Node node) {
		node.G_Cost = getDistance(node, start);
		node.H_Cost = getDistance(node, goal);
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
