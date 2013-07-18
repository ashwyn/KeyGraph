package topicDetection;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GraphAnalyze {
	Constants constants;

	public HashMap<String, Node> graphNodes;

	public PrintStream logger = System.out;

	public GraphAnalyze(Constants cons) {
		constants = cons;
	}

	public void buildGraph(HashMap<String, Document> documents, HashMap<String, Double> DF, boolean removeDuplicates) {
		logger.println("build Graph");
		graphNodes = new HashMap<String, Node>();
		// -- add nodes -------
		for (Document d : documents.values())
			if (!removeDuplicates || !d.isDuplicate)
				for (Keyword k : d.keywords.values()) {
					Node n = null;
					if (graphNodes.containsKey(k.baseForm))
						n = graphNodes.get(k.baseForm);
					else {
						Keyword keyword = new Keyword(k.baseForm, k.word, 0, DF.get(k.baseForm), 0);
						n = new Node(keyword);
						graphNodes.put(keyword.baseForm, n);
					}
					n.keyword.documents.put(d.id, d);
					n.keyword.tf++;
				}

		// -- filter nodes ----------
		ArrayList<String> toRemoveIds = new ArrayList<String>();
		for (Node n : graphNodes.values())
			if (n.keyword.tf < constants.NODE_DF_MIN || DF.get(n.keyword.baseForm) > constants.NODE_DF_MAX * documents.size())
				toRemoveIds.add(n.keyword.baseForm);
		for (String baseForm : toRemoveIds)
			graphNodes.remove(baseForm);
		toRemoveIds.clear();

		// -- add edges --------
		for (Document d : documents.values())
			if (!removeDuplicates || !d.isDuplicate)
				for (Keyword k1 : d.keywords.values())
					if (graphNodes.containsKey(k1.baseForm)) {
						Node n1 = graphNodes.get(k1.baseForm);
						for (Keyword k2 : d.keywords.values()) {
							if (graphNodes.containsKey(k2.baseForm) && k1.baseForm.compareTo(k2.baseForm) == -1) {
								Node n2 = graphNodes.get(k2.baseForm);
								String edgeId = Edge.getId(n1, n2);
								if (!n1.edges.containsKey(edgeId)) {
									Edge e = new Edge(n1, n2, edgeId);
									n1.edges.put(edgeId, e);
									n2.edges.put(edgeId, e);
								}
								n1.edges.get(edgeId).df++;
							}
						}
					}

		// -- filter edges ---------
		ArrayList<Edge> toRemove = new ArrayList<Edge>();
		for (Node n : graphNodes.values()) {
			for (Edge e : n.edges.values()) {
				// Double
				// MI=Math.log(1.0*e.df*documents.size()/DF.get(e.n1.keyword.baseForm)/DF.get(e.n2.keyword.baseForm))/Math.log(2);
				Double MI = e.df / (e.n1.keyword.df + e.n2.keyword.df - e.df);
				if (e.df < constants.EDGE_DF_MIN || MI < constants.EDGE_CORRELATION_MIN) {
					// ||Math.max(1.0 * e.df / e.n1.keyword.df, 1.0 * e.df /
					// e.n2.keyword.df) < Constants.EDGE_CORRELATION_MIN) {
					toRemove.add(e);
				} else
					e.computeCPs();
				// e.cp1=e.cp2=e.df*documents.size()/DF.get(e.n1.keyword.baseForm)*DF.get(e.n2.keyword.baseForm);

			}
			for (Edge e : toRemove) {
				e.n1.edges.remove(e.id);
				e.n2.edges.remove(e.id);
			}
			toRemove.clear();
		}
		// -- postfilter nodes ----------
		for (Node n : graphNodes.values())
			if (n.edges.size() == 0)
				toRemoveIds.add(n.keyword.baseForm);
		for (String baseForm : toRemoveIds)
			graphNodes.remove(baseForm);
		toRemoveIds.clear();
	}

	public ArrayList<HashMap<String, Node>> extractCommunities(HashMap<String, Node> nodes) {
		logger.println("Extract Communities");
		for (Node n : nodes.values())
			n.visited = false;
		ArrayList<HashMap<String, Node>> communities = new ArrayList<HashMap<String, Node>>();
		ArrayList<HashMap<String, Node>> connectedComponents = findConnectedComponents(nodes);
		while (connectedComponents.size() != 0) {
			HashMap<String, Node> subNodes = connectedComponents.remove(0);
			// System.out.println("##########"+subNodes.size());
			if (subNodes.size() >= constants.CLUSTER_NODE_SIZE_MIN) {
				if (subNodes.size() > constants.CLUSTER_NODE_SIZE_MAX) {
					System.out.println("approaxxxxxxxxxxxxxxxxxxxxxxxxx");
					filterTopKPercentOfEdges(subNodes, 1);
					for (Node n : subNodes.values())
						n.visited = false;
					connectedComponents.addAll(0, findConnectedComponents(subNodes));
				} else if (constants.CLUSTERING_ALG.toLowerCase().equals("newman"))
					findCommunities_Newman(subNodes, communities);
				else
					findCommunities_betweenness_centrality(subNodes, communities);
			}
			// else
			// if(subNodes.size()>2)
			// communities.add(subNodes);
		}
		return communities;
	}

	private void filterTopKPercentOfEdges(HashMap<String, Node> nodes, double k) {
		// -- To compute betweenness centerality scores!
		// Edge maxEdge = findMaxEdge(nodes);

		int edgeSize = 0;
		for (Node n1 : nodes.values())
			edgeSize += n1.edges.size();
		edgeSize /= 2;
		Edge[] toRemove = new Edge[(int) (edgeSize * k / 100)];

		for (Node n1 : nodes.values()) {
			for (Edge e : n1.edges.values())
				if (n1.equals(e.n1))
					insertInto(toRemove, e);

		}
		logger.println(nodes.size() + ": " + edgeSize + ":" + toRemove.length);
		for (Edge e : toRemove) {
			e.n1.edges.remove(e.id);
			e.n2.edges.remove(e.id);

		}
	}

	public ArrayList<HashMap<String, Node>> findConnectedComponents(HashMap<String, Node> nodes) {
		ArrayList<HashMap<String, Node>> cc = new ArrayList<HashMap<String, Node>>();
		while (nodes.size() > 0) {
			Node source = nodes.values().iterator().next();
			HashMap<String, Node> subNodes = new HashMap<String, Node>();
			ArrayList<Node> q = new ArrayList<Node>();
			q.add(0, source);
			while (q.size() > 0) {
				Node n = q.remove(0);
				n.visited = true;
				nodes.remove(n.keyword.baseForm);
				subNodes.put(n.keyword.baseForm, n);
				for (Edge e : n.edges.values()) {
					Node n2 = e.opposit(n);
					if (!n2.visited) {
						n2.visited = true;
						q.add(n2);
					}
				}
			}
			cc.add(subNodes);
		}
		return cc;
	}

	public ArrayList<HashMap<String, Node>> findConnectedComponentsFromSubset(HashMap<String, Node> nodes) {
		ArrayList<HashMap<String, Node>> cc = new ArrayList<HashMap<String, Node>>();
		while (nodes.size() > 0) {
			Node source = nodes.values().iterator().next();
			HashMap<String, Node> subNodes = new HashMap<String, Node>();
			ArrayList<Node> q = new ArrayList<Node>();
			q.add(0, source);
			while (q.size() > 0) {
				Node n = q.remove(0);
				n.visited = true;
				nodes.remove(n.keyword.baseForm);
				subNodes.put(n.keyword.baseForm, n);
				for (Edge e : n.edges.values()) {
					Node n2 = e.opposit(n);
					if (!n2.visited && nodes.containsKey(n2.keyword.baseForm)) {
						n2.visited = true;
						q.add(n2);
					}
				}
			}
			cc.add(subNodes);
		}
		return cc;
	}

	public ArrayList<HashMap<String, Node>> findCommunities_betweenness_centrality(HashMap<String, Node> nodes, ArrayList<HashMap<String, Node>> communities) {
		logger.println("Find Communities: " + nodes.size());

		Edge maxEdge = findMaxEdge(nodes);
		if (getFilterStatus(nodes.size(), maxEdge)) {
			maxEdge.n1.edges.remove(maxEdge.id);
			maxEdge.n2.edges.remove(maxEdge.id);

			// -- check if still connected ----
			HashMap<String, Node> subgraph1 = findSubgraph(maxEdge.n1, nodes);
			if (subgraph1.size() == nodes.size())
				return findCommunities_betweenness_centrality(nodes, communities);
			else {
				for (String key : subgraph1.keySet())
					nodes.remove(key);

				if (maxEdge.cp1 > constants.EDGE_CP_MIN_TO_DUPLICATE) {
					Keyword k = maxEdge.n2.keyword;
					Node newn = new Node(new Keyword(k.baseForm, k.word, k.tf, k.df, 0));
					Edge e = new Edge(maxEdge.n1, newn);
					maxEdge.n1.edges.put(e.id, e);
					newn.edges.put(e.id, e);
					subgraph1.put(k.baseForm, newn);
				}
				if (maxEdge.cp2 > constants.EDGE_CP_MIN_TO_DUPLICATE) {
					Keyword k = maxEdge.n1.keyword;
					Node newn = new Node(new Keyword(k.baseForm, k.word, k.tf, k.df, 0));
					Edge e = new Edge(newn, maxEdge.n2);
					maxEdge.n2.edges.put(e.id, e);
					newn.edges.put(e.id, e);
					nodes.put(k.baseForm, newn);
				}

				findCommunities_betweenness_centrality(subgraph1, communities);
				findCommunities_betweenness_centrality(nodes, communities);
				return communities;
			}
		} else {
			// if(nodes.size()>=Constants.CLUSTER_NODE_SIZE_MIN)
			communities.add(nodes);
			return communities;
		}
	}

	public ArrayList<HashMap<String, Node>> findCommunities_Newman(HashMap<String, Node> nodes, ArrayList<HashMap<String, Node>> communities) {
		try {
			String label = "hassan";
			printGraph(nodes);
			Runtime.getRuntime().exec("cp edge.txt FastCommunity_GPL_v1.0.3/edge.pairs");
			Runtime.getRuntime().exec("rm -f edge-fc_hassan.info edge-fc_" + label + ".joins", null, new File("FastCommunity_GPL_v1.0.3/"));
			Process run = Runtime.getRuntime().exec("FastCommunityMH -f edge.pairs -l " + label, null, new File("FastCommunity_GPL_v1.0.3/"));
			DataInputStream tmp = new DataInputStream(run.getInputStream());
			while (tmp.readLine() != null)
				;

			HashMap<String, Node> nodeIds = new HashMap<String, Node>();
			for (Node n : nodes.values())
				nodeIds.put(n.id, n);
			File f = new File("FastCommunity_GPL_v1.0.3/edge-fc_" + label + ".joins");
			// System.out.println(nodes.size()+"::::"+f.exists()+"::::"+f.getAbsolutePath());
			BufferedReader in = new BufferedReader(new FileReader("FastCommunity_GPL_v1.0.3/edge-fc_" + label + ".joins"));
			String line = null;
			HashMap<String, Node> tmpNodes = new HashMap<String, Node>();
			Node n1 = null, n2 = null;
			String t1 = null, t2 = null;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split("\t");
				if (tokens[0].charAt(0) != '-') {
					t1 = tokens[0];
					t2 = tokens[1];
					n1 = new Node(new Keyword(t1, null, 0, 0, 0));
					n2 = new Node(new Keyword(t2, null, 0, 0, 0));
					if (!tmpNodes.containsKey(t1))
						tmpNodes.put(t1, n1);
					if (!tmpNodes.containsKey(t2))
						tmpNodes.put(t2, n2);
					Edge e = new Edge(n1, n2, Edge.getId(n1, n2));
					n1.edges.put(e.id, e);
					n2.edges.put(e.id, e);
				}
			}
			ArrayList<HashMap<String, Node>> ccs = findConnectedComponents(tmpNodes);
			System.out.println(tmpNodes.size() + "=>" + ccs.size());
			tmpNodes = null;
			HashMap<String, Node> cc = null;
			HashMap<String, Node> community = null;
			while (ccs.size() > 0) {
				cc = ccs.remove(0);
				community = new HashMap<String, Node>();
				for (String key : cc.keySet())
					community.put(nodeIds.get(key).keyword.baseForm, nodeIds.get(key));
				communities.add(community);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return communities;
	}

	public boolean insertInto(Edge[] toRemove, Edge e) {
		if (toRemove[toRemove.length - 1] == null) {
			int i = toRemove.length - 1;
			while (i >= 1 && (toRemove[i - 1] == null || toRemove[i - 1].compareBetweenness2(e) < 0)) {
				toRemove[i] = toRemove[i - 1];
				i--;
			}
			toRemove[i] = e;
			return true;
		} else if (toRemove[0].compareBetweenness2(e) >= 0)
			return false;
		else {
			int i = 0;
			while (i < toRemove.length - 1 && toRemove[i + 1].compareBetweenness2(e) < 0) {
				toRemove[i] = toRemove[i + 1];
				i++;
			}
			toRemove[i] = e;
			return true;
		}
	}

	private boolean getFilterStatus(int graphSize, Edge maxEdge) {
		double possiblePath = Math.min(graphSize * (graphSize - 1) / 2, constants.CLUSTER_NODE_SIZE_MAX * (constants.CLUSTER_NODE_SIZE_MAX - 1) / 2);
		// double th = Math.min(4.2 * Math.log(possiblePath) / Math.log(2) + 1,
		// 3 * graphSize);
		// double th =3 * graphSize;
		// double th= possiblePath / 8;
		double th = 4.2 * Math.log(possiblePath) / Math.log(2) + 1;
		// double th =0;
		return graphSize > constants.CLUSTER_NODE_SIZE_MIN && maxEdge != null && maxEdge.df > 0 && (maxEdge.betweennessScore > th);
	}

	public HashMap<String, Node> findSubgraph(Node source, HashMap<String, Node> nodes) {
		for (Node n : nodes.values())
			n.visited = false;
		HashMap<String, Node> subNodes = new HashMap<String, Node>();
		ArrayList<Node> q = new ArrayList<Node>();
		q.add(source);
		while (q.size() > 0) {
			Node n = q.remove(0);
			n.visited = true;
			subNodes.put(n.keyword.baseForm, n);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				if (!n2.visited) {
					n2.visited = true;
					q.add(n2);
				}
			}
		}
		return subNodes;
	}

	public Edge findMaxEdge(HashMap<String, Node> nodes) {
		// if (nodes.size() > 4 * Constants.CLUSTER_NODE_SIZE_MAX)
		// return findMaxEdgeApproximation(nodes);
		// logger.println("Node size: " + nodes.size());
		for (Node n : nodes.values()) {
			// n.clusteringInfo.lastCheked = null;
			// n.visited=false;
			for (Edge e : n.edges.values()) {
				e.betweennessScore = 0;
			}
		}
		Edge maxEdge = new Edge(null, null, null);
		maxEdge.betweennessScore = -1;
		for (Node source : nodes.values()) {
			for (Node n : nodes.values())
				n.visited = false;
			maxEdge = BFS(source, maxEdge);
			// maxEdge = dijkstra(source, maxEdge, nodes);
		}
		// maxEdge.clusteringInfo.betweennessScore /= 2;
		maxEdge.betweennessScore /= 2;
		return maxEdge;
	}

	public Edge dijkstra(Node source, Edge maxEdge, HashMap<String, Node> nodes) {
		HashSet<Node> q = new HashSet<Node>();
		HashMap<Node, Double> dist = new HashMap<Node, Double>();

		for (Node n : nodes.values()) {
			q.add(n);
			dist.put(n, 99999999999999.0);
		}
		dist.put(source, 0.0);
		q.remove(source);
		for (Edge e : source.edges.values()) {
			Node n2 = e.opposit(source);
			if (q.contains(n2)) {
				dist.put(n2, weight(e, n2));
				n2.prev = source;
			}
		}

		while (q.size() > 0) {
			Node n = findMin(dist, q);
			q.remove(n);
			maxEdge = updateCenterality(n, source, maxEdge);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				if (q.contains(n2)) {
					if (dist.get(n2) >= dist.get(n) + weight(e, n2)) {
						dist.put(n2, dist.get(n) + weight(e, n2));
						n2.prev = n;
					}
				}
			}
		}
		return maxEdge;
	}

	public double weight(Edge e, Node n2) {
		return 1.0 / e.df;
		// return 1- Math.max(e.cp1, e.cp2);
		// return (e.n2.equals(n2))? 1-e.cp1: 1-e.cp2;
	}

	public Node findMin(HashMap<Node, Double> dist, HashSet<Node> q) {
		Node min = null;
		Double minDist = -1.0;
		for (Node n : q)
			if (minDist == -1 || dist.get(n) < minDist) {
				minDist = dist.get(n);
				min = n;
			}
		return min;
	}

	public Edge BFS(Node source, Edge maxEdge) {
		ArrayList<Node> q = new ArrayList<Node>();
		q.add(source);
		// source.clusteringInfo.lastCheked = source.id;
		while (q.size() > 0) {
			Node n = q.remove(0);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				// if (!source.id.equals(n2.clusteringInfo.lastCheked)) {
				if (!n2.visited) {
					// n2.clusteringInfo.lastCheked = source.id;
					n2.visited = true;
					n2.prev = n;
					// e.clusteringInfo.betweennessScore++;
					updateCenterality(n2, source, maxEdge);
					if (e.compareBetweenness(maxEdge) > 0)
						maxEdge = e;
					q.add(n2);
				}
			}
		}
		return maxEdge;
	}

	public Edge updateCenterality(Node n, Node root, Edge maxEdge) {
		do {
			// n.betweennessScore ++;
			Edge e = n.edges.get(Edge.getId(n, n.prev));
			e.betweennessScore++;
			if (e.compareBetweenness(maxEdge) > 0)
				maxEdge = e;
			n = n.prev;
		} while (!n.equals(root));
		// root.centeralityScore += 1;
		return maxEdge;
	}

	public Edge findMaxEdgeApproximation(HashMap<String, Node> nodes) {
		logger.println("Node size: " + nodes.size());
		for (Node n : nodes.values()) {
			for (Edge e : n.edges.values())
				e.betweennessScore = 0;
		}

		Edge maxEdge = new Edge(null, null, null);
		// maxEdge.clusteringInfo.betweennessScore = -1;
		maxEdge.betweennessScore = -1;
		long milis = System.currentTimeMillis();
		for (Node source : nodes.values())
			for (Node dest : nodes.values())
				if (source.id.compareTo(dest.id) < 0 && Math.random() < Math.pow((double) constants.CLUSTER_NODE_SIZE_MAX / nodes.size(), 2)) {
					for (Node nn : nodes.values())
						nn.visited = false;
					maxEdge = BFS(source, dest, maxEdge);
				}
		logger.println("hooooy: " + (System.currentTimeMillis() - milis) / 1000);
		return maxEdge;
	}

	public Edge BFS(Node source, Node dest, Edge maxEdge) {
		ArrayList<Node> q = new ArrayList<Node>();
		q.add(source);
		source.visited = true;
		while (q.size() > 0) {
			Node n = q.remove(0);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				if (n2.equals(dest)) {
					e.betweennessScore++;
					if (e.compareBetweenness(maxEdge) > 0)
						maxEdge = e;
					return maxEdge;
				}
				if (!n2.visited) {
					n2.visited = true;
					e.betweennessScore++;
					if (e.compareBetweenness(maxEdge) > 0)
						maxEdge = e;
					q.add(n2);
				}
			}
		}
		return maxEdge;
	}

	public void printGraph(HashMap<String, Node> nodes) {
		try {
			DataOutputStream nout = new DataOutputStream(new FileOutputStream("node.txt"));
			DataOutputStream eout = new DataOutputStream(new FileOutputStream("edge.txt"));
			printGraph(nodes, nout, eout);
			nout.close();
			eout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void printGraph(ArrayList<HashMap<String, Node>> communities) {
		try {
			DataOutputStream nout = new DataOutputStream(new FileOutputStream("node.txt"));
			DataOutputStream eout = new DataOutputStream(new FileOutputStream("edge.txt"));
			for (HashMap<String, Node> nodes : communities)
				printGraph(nodes, nout, eout);
			nout.close();
			eout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void printGraph(HashMap<String, Node> nodes, DataOutputStream nout, DataOutputStream eout) throws IOException {
		for (Node n : nodes.values()) {
			nout.writeBytes(n.id + "\t\t\t\t\t\t" + n.keyword.word + "\n");
			for (Edge e : n.edges.values())
				if (e.n1.equals(n))
					eout.writeBytes(e.n1.id + "\t" + e.n2.id + "\n");
		}
	}

	public static HashMap<String, Node> mergeKeyGraphs(HashMap<String, Node> kg1, HashMap<String, Node> kg2) {
		HashMap<String, Node> kg = new HashMap<String, Node>();
		for (Node n : kg1.values())
			kg.put(n.keyword.baseForm, new Node(n.keyword));
		for (Node n : kg2.values())
			kg.put(n.keyword.baseForm, new Node(n.keyword));
		for (Node n : kg1.values())
			for (Edge e : n.edges.values())
				if (n.keyword.baseForm.compareTo(e.opposit(n).keyword.baseForm) == -1) {
					if (kg.get(e.n1.keyword.baseForm) == null || kg.get(e.n2.keyword.baseForm) == null)
						// if(e.id.equals("conserv talker_differ point"))
						System.out.print("");
					if (!kg.get(e.n1.keyword.baseForm).edges.containsKey(e.id)) {
						Node n1 = kg.get(e.n1.keyword.baseForm);
						Node n2 = kg.get(e.n2.keyword.baseForm);
						Edge ee = new Edge(n1, n2, e.id);
						n1.edges.put(ee.id, ee);
						n2.edges.put(ee.id, ee);
					}
				}
		for (Node n : kg2.values())
			for (Edge e : n.edges.values())
				if (n.keyword.baseForm.compareTo(e.opposit(n).keyword.baseForm) == -1) {
					if (!kg.get(e.n1.keyword.baseForm).edges.containsKey(e.id)) {
						Node n1 = kg.get(e.n1.keyword.baseForm);
						Node n2 = kg.get(e.n2.keyword.baseForm);
						Edge ee = new Edge(n1, n2, e.id);
						n1.edges.put(ee.id, ee);
						n2.edges.put(ee.id, ee);
					}
				}

		return kg;
	}

}
