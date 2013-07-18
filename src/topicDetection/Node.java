package topicDetection;

import java.util.HashMap;

public class Node {
	static int MAX_ID = 1;
	String id;
	public Keyword keyword;
	public HashMap<String, Edge> edges = new HashMap<String, Edge>();
	// public double betweennessScore;
	public boolean visited;
	public Node prev;

	public Node(Keyword keyword) {
		id = MAX_ID++ + "";
		this.keyword = keyword;
	}

	public Edge insertEdge(Node n2) {
		String edgeId = Edge.getId(this, n2);
		if (!edges.containsKey(edgeId)) {
			Edge e = new Edge(this, n2, edgeId);
			edges.put(edgeId, e);
			n2.edges.put(edgeId, e);
		}
		return edges.get(edgeId);
	}
}
