package com.retronova.game.objects.a_star;

import com.retronova.game.objects.a_star.exceptions.OutOfRange;

import java.awt.*;
import java.util.List;

public class PathFinder implements Runnable {

	private List<Node> path;
	private Point start, end;
	private int range;

    private final CheckerNode checkerNode;

	public PathFinder(CheckerNode checkerNode) {
		this.checkerNode = checkerNode;
	}

    public void buildPath(Point start, Point End, int range) {
		this.start = start;
		this.end = End;
		this.range = range;
        new Thread(this).start();
	}

	public Point getFollow() {
		if(isEmpty())
			return null;
		Node node = getLastNode();
		return new Point(node.x, node.y);
	}

	public void arrived() {
		if(!isEmpty())
			path.removeLast();
	}
	
	private Node getLastNode() {
		return path.getLast();
	}

	public boolean isEmpty() {
		return (path == null ||  path.isEmpty());
	}

	@Override
	public synchronized void run() {
		try {
			Node start = new Node(this.start);
			Node goal = new Node(this.end);
			WayMap wayMap = new WayMap(start, goal, range, checkerNode);
			A_Star aStar = new A_Star(wayMap);
			this.path = aStar.getPath();
		} catch (RuntimeException e) {
			if(e instanceof OutOfRange) {
				return;
			}
			System.err.println("A* something is wrong!");
			e.printStackTrace();
		}
	}

}
