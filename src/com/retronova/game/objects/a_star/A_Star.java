package com.retronova.game.objects.a_star;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class A_Star {

	private List<Node> path;
	
	private WayMap wayMap;
	private Node start_Node, goal_Node, current_Node;
	
	ArrayList<Node> openList;
	
	public A_Star(Point start, Point end, int range) throws RuntimeException {
		this.start_Node = new Node(start);
		this.current_Node = start_Node;
		this.goal_Node = new Node(end);
		path = new ArrayList<Node>();
		wayMap = new WayMap(start_Node, goal_Node, range);
		search();
	}
	
	public void search() {
		boolean goalReached = false;
		openList = new ArrayList<Node>();
		ArrayList<Node> checkedList = new ArrayList<Node>();
		while(!goalReached) {
			int x = current_Node.x;
			int y = current_Node.y;
			current_Node.setAsChecked();
			checkedList.add(current_Node);
			openList.remove(current_Node);
			
			if(wayMap.containsNode(x, y - 1))
				openNode(wayMap.getNode(x, y - 1));
			if(wayMap.containsNode(x - 1, y))
				openNode(wayMap.getNode(x - 1, y));
			if(wayMap.containsNode(x, y + 1))
				openNode(wayMap.getNode(x, y + 1));
			if(wayMap.containsNode(x + 1, y))
				openNode(wayMap.getNode(x + 1, y));
			
			//Tiles in vertical
			if(wayMap.containsNode(x, y - 1) && wayMap.containsNode(x - 1, y))
				if(!wayMap.getNode(x, y - 1).solid && !wayMap.getNode(x - 1, y).solid)
					openNode(wayMap.getNode(x-1, y-1));
			if(wayMap.containsNode(x, y - 1) && wayMap.containsNode(x + 1, y))
				if(!wayMap.getNode(x, y - 1).solid && !wayMap.getNode(x + 1, y).solid)
					openNode(wayMap.getNode(x+1, y-1));
			if(wayMap.containsNode(x, y + 1) && wayMap.containsNode(x + 1, y))
				if(!wayMap.getNode(x, y + 1).solid && !wayMap.getNode(x + 1, y).solid)
					openNode(wayMap.getNode(x+1, y+1));
			if(wayMap.containsNode(x, y + 1) && wayMap.containsNode(x - 1, y))
				if(!wayMap.getNode(x, y + 1).solid && !wayMap.getNode(x - 1, y).solid)
					openNode(wayMap.getNode(x-1, y+1));
			
			int bestNodeIndex = 0;
			double bestNodefCost = 999;
			for(int i = 0; i < openList.size(); i++) {
				if(openList.get(i).F_Cost < bestNodefCost) {
					bestNodeIndex = i;
					bestNodefCost = openList.get(i).F_Cost;
				}else if(openList.get(i).F_Cost == bestNodefCost) {
					if(openList.get(i).G_Cost < openList.get(bestNodeIndex).G_Cost) 
						bestNodeIndex = i;
				}
			}
			if(openList.isEmpty()) {
				System.err.println("No Way Found");
				throw new RuntimeException("Way not found!");
			}
			current_Node = openList.get(bestNodeIndex);
			if(current_Node == goal_Node) {
				goalReached = true;
				this.path = trackThePath();
			}
		}
	}
	
	private List<Node> trackThePath() {
		List<Node> path = new ArrayList<Node>();
		Node current = goal_Node;
		while(current != start_Node) {
			path.add(current);
			current = current.parent;
		}
		return path;
	}
	
	public List<Node> getPath(){
		return path;
	}
	
	private void openNode(Node node) {
		if(!node.open && !node.checked && !node.solid) {
			node.setAsOpen();
			node.parent = current_Node;
			openList.add(node);
		}
	}

}
