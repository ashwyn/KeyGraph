package topicDetection;

public class Edge {
	public Node n1, n2;
	public String id;
	public int df;
	public double cp1, cp2;
	public double betweennessScore;

	public Edge(Node n1, Node n2, String id) {
		this.n1 = n1;
		this.n2 = n2;
		this.id = id;
	}

	public Edge(Node n1, Node n2) {
		this(n1, n2, getId(n1, n2));
	}

	public static String getId(Node n1, Node n2) {
		if (n1.keyword.baseForm.compareTo(n2.keyword.baseForm) < 1)
			return n1.keyword.baseForm + "_" + n2.keyword.baseForm;
		else
			return n2.keyword.baseForm + "_" + n1.keyword.baseForm;
	}

	public void computeCPs() {
		cp1 = 1.0 * df / n1.keyword.tf;
		cp2 = 1.0 * df / n2.keyword.tf;
	}

	public Node opposit(Node n) {
		if (n1.keyword.baseForm.equals(n.keyword.baseForm))
			return n2;
		if (n2.keyword.baseForm.equals(n.keyword.baseForm))
			return n1;
		return null;
	}

	public int compareBetweenness(Edge e) {
		if (n1.edges.size() < 2 || n2.edges.size() < 2 || betweennessScore < e.betweennessScore)
			return -1;
		if (betweennessScore > e.betweennessScore)
			return 1;
		if (betweennessScore == e.betweennessScore)
			if (df > e.df)
				return -1;
		if (df < e.df)
			return 1;
		return 0;
	}

	public int compareBetweenness2(Edge e) {
		double ecp = Math.max(cp1, cp2);
		double cp = Math.max(e.cp1, e.cp2);

		if (cp < ecp)
			return -1;
		if (cp > ecp)
			return 1;
		if (cp == ecp)
			if (df > e.df)
				return -1;
		if (df < e.df)
			return 1;
		return 0;
	}
}
