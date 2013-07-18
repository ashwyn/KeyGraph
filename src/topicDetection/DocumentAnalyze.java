package topicDetection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class DocumentAnalyze {
	Constants constants;

	public DocumentAnalyze(Constants cons) {
		constants = cons;
	}

	public ArrayList<DocumentCluster> clusterByLDA(HashMap<String, Document> docs, HashMap<String, Double> DF, String model) {
		ArrayList<DocumentCluster> documentClusters = new ArrayList<DocumentCluster>();
		try {

			HashMap<Integer, DocumentCluster> clusters = new HashMap<Integer, DocumentCluster>();

			BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("results-" + model + ".txt")));
			in.readLine();
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split(" ");
				String docid = tokens[1];
				if (docid.lastIndexOf("/") != -1)
					docid = docid.substring(docid.lastIndexOf("/") + 1);
				docid = docid.substring(0, docid.lastIndexOf("."));
				Integer clusterid = Integer.parseInt(tokens[2]);
				Document d = docs.get(docid);
				if (d == null) {
					d = new Document(docid);
					docs.put(d.id, d);
				}
				// System.out.println(docid + "\t" + d + "\t" + clusterid);
				System.out.print(tokens[3] + "\t");
				if (!clusters.containsKey(clusterid))
					clusters.put(clusterid, new DocumentCluster());
				clusters.get(clusterid).docs.put(d.id, d);
			}
			System.out.println();
			if (model.startsWith("mallet")) {

				in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("results-" + model + ".topickeys.txt")));
				// in.readLine();
				line = null;
				while ((line = in.readLine()) != null) {
					String[] tokens = line.split("\t");
					DocumentCluster dc = clusters.get(Integer.parseInt(tokens[0]));
					dc.keyGraph = new HashMap<String, Node>();
					for (String keyword : tokens[2].split(" "))
						dc.keyGraph.put(keyword, new Node(new Keyword(keyword, keyword, 0, 0, 0)));
				}
			}
			for (Integer clusterid : clusters.keySet())
				documentClusters.add(clusters.get(clusterid));

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Document Clusters (final) :::::::::" + documentClusters.size());

		return documentClusters;
	}

	public ArrayList<DocumentCluster> clusterbyKeyGraph(HashMap<String, Document> docs, HashMap<String, Double> DF) {
		GraphAnalyze g = new GraphAnalyze(constants);
		g.buildGraph(docs, DF, constants.REMOVE_DUPLICATES);
		// g.printGraph(g.graphNodes);
		ComputeDocumentVectorSize(docs, DF, g.graphNodes);
		ArrayList<HashMap<String, Node>> communities = g.extractCommunities(g.graphNodes);
		System.out.println(communities.size());
		return extractClustersFromKeyCommunity(docs, communities, DF, docs.size(), g.graphNodes);
	}

	public void ComputeDocumentVectorSize(HashMap<String, Document> docs, HashMap<String, Double> DF, HashMap<String, Node> graphNodes) {
		// ArrayList<String> toRemove=new ArrayList<String>();
		for (Document d : docs.values()) {
			d.vectorSize = 0;
			for (Keyword k : d.keywords.values())
				if (graphNodes.containsKey(k.baseForm))
					d.vectorSize += Math.pow(TFIDF(k.tf, idf(DF.get(k.baseForm), docs.size())), 2);
			d.vectorSize = Math.sqrt(d.vectorSize);

			// if(d.vectorSize==0)
			// toRemove.add(d.id);
		}

		// for(String id: toRemove)
		// docs.remove(id);
	}

	public double TFIDF(double tf, double idf) {
		if (tf == 0 || idf == 0)
			return 0;
		return tf * idf;
	}

	public ArrayList<DocumentCluster> extractClustersFromKeyCommunity(HashMap<String, Document> docs, ArrayList<HashMap<String, Node>> communities,
			HashMap<String, Double> DF, int docSize, HashMap<String, Node> graphNodes) {

		ArrayList<DocumentCluster> documentClusters = new ArrayList<DocumentCluster>();
		for (HashMap<String, Node> c : communities) {
			DocumentCluster dc = new DocumentCluster();
			// -- find related documents -----------
			dc.keyGraph = c;
			for (Node n : c.values())
				for (Document d : n.keyword.documents.values())
					if (!dc.docs.containsKey(d.id)) {
						double cosineSimilarity = cosineSimilarity(c, d, DF, docSize);
						if (cosineSimilarity > constants.DOC_SIM2KEYGRAPH_MIN) {
							dc.docs.put(d.id, d);
							dc.similarities.put(d.id, cosineSimilarity);
						}
					}

			// -- filter clusters -------------
			// dc.variance = variance(dc, DF, docSize, graphNodes);
			// if (dc.centroid.vectorSize == 0 || dc.variance <=
			// constants.CLUSTER_VAR_MAX)
			{
				ArrayList<String> toRemove = new ArrayList<String>();
				// System.out.println("\n****** Community #" +
				// documentClusters.size());
				// printKeywords(dc);
				// for (Document d : dc.docs.values()) {
				// if (cosineSimilarity(dc.centroid, d, DF, docSize) <
				// constants.DOC_SIM2CENTROID_MIN)
				// toRemove.add(d.id);
				// // else
				// // System.out.println(d.topic + ": " + d.id);
				// }
				// -- time based filtering -----------
				// if (dc.docs.size() > 0){
				// DocumentCluster[] dcs = filterTimeBased(dc, toRemove);
				// // if(dcs[0].docs.size() >= constants.TOPIC_MIN_SIZE)
				// // documentClusters.add(dcs[0]);
				// // if(dcs[1].docs.size() >= constants.TOPIC_MIN_SIZE)
				// // documentClusters.add(dcs[1]);
				// }
				if (dc.docs.size() - toRemove.size() >= constants.TOPIC_MIN_SIZE) {
					documentClusters.add(dc);
					for (String id : toRemove) {
						dc.docs.remove(id);
						dc.similarities.remove(id);
					}
				}

			}
		}

		System.out.println("Keyword Communities :::::::::" + communities.size());
		System.out.println("Document Clusters (initial) :::::::::" + documentClusters.size());
		// printClusters(documentClusters);
		mergeSimilarClusters(documentClusters);
		// printClusters(documentClusters);

		if (constants.HARD_CLUSTERING)
			hardClustering(docs, DF, docSize, documentClusters);

		System.out.println("Document Clusters (final) :::::::::" + documentClusters.size());

		return documentClusters;
	}

	public ArrayList<DocumentCluster> extractClustersFromKeyCommunity2(HashMap<String, Document> docs, ArrayList<HashMap<String, Node>> communities,
			HashMap<String, Double> DF, int docSize, HashMap<String, Node> graphNodes) {

		ArrayList<DocumentCluster> tmpdocumentClusters = new ArrayList<DocumentCluster>();
		ArrayList<DocumentCluster> documentClusters = new ArrayList<DocumentCluster>();

		for (HashMap<String, Node> c : communities) {
			DocumentCluster dc = new DocumentCluster();
			dc.keyGraph = c;
			tmpdocumentClusters.add(dc);
		}

		for (Document d : docs.values()) {
			double maxSim = 0;
			DocumentCluster maxDC = null;
			for (DocumentCluster dc : tmpdocumentClusters) {
				// double sim = cosineSimilarity(dc.keyGraph, d, DF, docSize);
				double sim = dc.similarities.get(d.id);
				if (sim > maxSim) {
					System.out.println("siiiim:: " + sim);
					maxSim = sim;
					maxDC = dc;
				}
			}
			if (maxSim > constants.DOC_SIM2KEYGRAPH_MIN)
				maxDC.docs.put(d.id, d);
		}

		for (DocumentCluster dc : tmpdocumentClusters)
		// -- filter clusters -------------
		// dc.variance = variance(dc, DF, docSize, graphNodes);
		// if (dc.centroid.vectorSize == 0 || dc.variance <=
		// constants.CLUSTER_VAR_MAX)
		{
			ArrayList<String> toRemove = new ArrayList<String>();
			// System.out.println("\n****** Community #" +
			// documentClusters.size());
			// printKeywords(dc);
			// for (Document d : dc.docs.values()) {
			// if (cosineSimilarity(dc.centroid, d, DF, docSize) <
			// constants.DOC_SIM2CENTROID_MIN)
			// toRemove.add(d.id);
			// // else
			// // System.out.println(d.topic + ": " + d.id);
			// }
			// -- time based filtering -----------
			// if (dc.docs.size() > 0){
			// DocumentCluster[] dcs = filterTimeBased(dc, toRemove);
			// // if(dcs[0].docs.size() >= constants.TOPIC_MIN_SIZE)
			// // documentClusters.add(dcs[0]);
			// // if(dcs[1].docs.size() >= constants.TOPIC_MIN_SIZE)
			// // documentClusters.add(dcs[1]);
			// }
			if (dc.docs.size() - toRemove.size() >= constants.TOPIC_MIN_SIZE) {
				documentClusters.add(dc);
				for (String id : toRemove) {
					dc.docs.remove(id);
					dc.similarities.remove(id);
				}
			}
		}

		System.out.println("Keyword Communities :::::::::" + communities.size());
		System.out.println("Document Clusters (initial) :::::::::" + documentClusters.size());
		// printClusters(documentClusters);
		mergeSimilarClusters(documentClusters);
		// printClusters(documentClusters);

		if (constants.HARD_CLUSTERING)
			hardClustering(docs, DF, docSize, documentClusters);

		System.out.println("Document Clusters (final) :::::::::" + documentClusters.size());

		return documentClusters;
	}

	public DocumentCluster[] filterTimeBased(DocumentCluster dc, ArrayList<String> toRemove) {
		long time = 0;
		HashMap<String, Document> docs = dc.docs;
		for (int i = 0; i < 5 && docs.size() > 0; i++) {
			time = 0;
			toRemove.clear();
			for (Document d : docs.values())
				time += d.publishDate.getTime();
			time /= docs.size();
			docs = new HashMap<String, Document>();
			for (Document d : dc.docs.values())
				if (Math.abs(d.publishDate.getTime() - time) > ((long) 15) * 24 * 60 * 60 * 1000)
					toRemove.add(d.id);
				else
					docs.put(d.id, d);
		}

		DocumentCluster[] dcs = new DocumentCluster[] { new DocumentCluster(), new DocumentCluster() };
		dcs[0].keyGraph = dc.keyGraph;
		dcs[1].keyGraph = dc.keyGraph;
		for (String id : toRemove) {
			Document doc = dc.docs.get(id);
			if (doc.publishDate.after(new Timestamp(time)))
				dcs[1].docs.put(id, doc);
			else
				dcs[0].docs.put(id, doc);
		}
		return dcs;
	}

	private void hardClustering(HashMap<String, Document> docs, HashMap<String, Double> DF, int docSize, ArrayList<DocumentCluster> documentClusters) {
		int ii = 0;
		for (Document d : docs.values()) {
			boolean isAssigned = false;
			for (DocumentCluster dc : documentClusters)
				if (dc.docs.containsKey(d.id)) {
					isAssigned = true;
					break;
				}
			if (!isAssigned) {
				double max_sim = 0;
				DocumentCluster bestDC = null;
				for (DocumentCluster dc : documentClusters)
					// if (cosineSimilarity(dc.keyGraph, d, DF, docSize) >
					// max_sim) {
					if (dc.similarities.containsKey(d.id) && dc.similarities.get(d.id) > max_sim) {
						max_sim = cosineSimilarity(dc.keyGraph, d, DF, docSize);
						bestDC = dc;
					}
				if (max_sim > constants.DOC_SIM2KEYGRAPH_MIN / 3.5)
					bestDC.docs.put(d.id, d);
				else
					ii++;
			}
		}
		System.out.println("Off topic documents:" + ii + " out of " + docs.size());
	}

	public static void printTopics(Collection<DocumentCluster> clusters, PrintStream out) {
		for (DocumentCluster dc : clusters) {
			out.print("KEYWORDS:\t");
			printKeywords(dc, out);
			out.print("\nDOCUMNETS:\t");
			for (Document d : dc.docs.values())
				out.print(d.id + ",");
			out.print("\nKEYGRAPH_NODES:\t");
			for (Node n : dc.keyGraph.values())
				out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.getWord().replaceAll("[,'\"]", " ") + ",");
			out.println("\nKEYGRAPH_EDGES:\t");
			for (Node n : dc.keyGraph.values()) {
				for (Edge e : n.edges.values())
					if (e.n1.equals(n))
						out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ",");
			}
			out.println("\n");
			// out.println("~" + dc.docs.size() / 10 + "0: " + dc.docs.size() +
			// " docs");

		}

	}

	// public static void printClusters(Collection<DocumentCluster> clusters,
	// PrintStream out) {
	// // printClusters(clusters, out, false);
	// for (DocumentCluster dc : clusters)
	// dc.serialize(out);
	// }

	// public static void printClusters(Collection<DocumentCluster> clusters,
	// PrintStream out, boolean printDocContent) {
	// for (DocumentCluster dc : clusters) {
	// printCluster(dc, out, printDocContent);
	// }
	// }

	// public static void printCluster(DocumentCluster dc, PrintStream out) {
	// printCluster(dc, out, false);
	// }

	// public static void printCluster(DocumentCluster dc, PrintStream out,
	// boolean printDocContent) {
	//
	// out.println("\n****** Community #" + dc.id);
	// printKeywords(dc, out);
	// printKeyGraph(dc.keyGraph, out);
	// out.println("~" + dc.docs.size() / 10 + "0: " + dc.docs.size() +
	// " docs");
	// for (Document d : dc.docs.values())
	// // out.println(d.topics + ": " + d.publishDate + " " + d.id);
	// if (printDocContent)
	// out.println(d.publishDate + "\t" + d.id + "\t" + d.getBody());
	// else
	// out.println(d.publishDate + "\t" + d.id);
	// }

	public static void printClustersForTheWebsite(Collection<DocumentCluster> clusters, String outputFileName) throws Exception {

		PrintStream out = new PrintStream(outputFileName + ".event_document");
		for (DocumentCluster dc : clusters) {
			out.print(dc.id + "\t");
			// out.println("~" + dc.docs.size() / 10 + "0: " + dc.docs.size() +
			// " docs");
			for (Document d : dc.docs.values())
				out.print(d.id + ":" + (d.isDuplicate ? 0 : 1) + ",");
			out.println();
		}
		out.close();
		out = new PrintStream(outputFileName + ".event_keyGraph_nodes");
		for (DocumentCluster dc : clusters) {
			out.print(dc.id + "\t");
			for (Node n : dc.keyGraph.values())
				out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.getWord().replaceAll("[,'\"]", " ") + ",");
			out.println();
		}
		out.close();
		out = new PrintStream(outputFileName + ".event_keyGraph_edges");
		for (DocumentCluster dc : clusters) {
			out.print(dc.id + "\t");
			for (Node n : dc.keyGraph.values()) {
				for (Edge e : n.edges.values())
					if (e.n1.equals(n))
						out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ",");
			}
			out.println();
		}
		out.close();

		// printKeywords(dc, out);
		// printKeyGraph(dc.keyGraph, out);

	}

	public static void printKeywords(DocumentCluster dc, PrintStream out) {
		for (Node n : dc.keyGraph.values())
			out.print(n.keyword.getWord().replaceAll("[,'\"]", " ") + ",");
		// out.println();
	}

	public static void printKeyGraph(HashMap<String, Node> keyGraph, PrintStream out) {
		for (Node n : keyGraph.values())
			out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.getWord().replaceAll("[,'\"]", " ") + ",");
		out.println();
		for (Node n : keyGraph.values()) {
			for (Edge e : n.edges.values())
				if (e.n1.equals(n))
					out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ",");
		}
		out.println();
	}

	private void mergeSimilarClusters(ArrayList<DocumentCluster> documentClusters) {
		ArrayList<DocumentCluster> topics = new ArrayList<DocumentCluster>();
		while (documentClusters.size() > 0) {
			DocumentCluster dc1 = documentClusters.remove(0);
			ArrayList<DocumentCluster> toRemove = new ArrayList<DocumentCluster>();
			boolean isChanged = false;
			do {
				isChanged = false;
				for (DocumentCluster dc2 : documentClusters) {
					double intersect = intersectDocs(dc1.docs, dc2.docs);
					if (intersect / Math.min(dc1.docs.size(), dc2.docs.size()) >= constants.CLUSTER_INTERSECT_MIN) {
						mergeClusters(dc1, dc2);
						isChanged = true;
						toRemove.add(dc2);
					}
				}
				documentClusters.removeAll(toRemove);
			} while (isChanged);
			topics.add(dc1);
		}
		documentClusters.addAll(topics);
	}

	public int intersectDocs(HashMap dc1, HashMap dc2) {
		int intersect = 0;
		for (Object key : dc1.keySet())
			if (dc2.containsKey(key))
				intersect++;
		return intersect;
	}

	public void mergeClusters(DocumentCluster dc1, DocumentCluster dc2) {
		for (Document d : dc2.docs.values())
			if (!dc1.docs.containsKey(d.id)) {
				dc1.docs.put(d.id, d);
				dc1.similarities.put(d.id, dc2.similarities.get(d.id));
			} else if (dc1.similarities.get(d.id) < dc2.similarities.get(d.id))
				dc1.similarities.put(d.id, dc2.similarities.get(d.id));
		dc1.keyGraph.putAll(dc2.keyGraph);
	}

	public double cosineSimilarity(Document d1, Document d2, HashMap<String, Double> DF, int docSize) {
		double sim = 0;
		for (Keyword k1 : d1.keywords.values()) {
			if (d2.keywords.containsKey(k1.baseForm)) {
				Double df = DF.get(k1.baseForm);
				double tf1 = k1.tf;
				double tf2 = d2.keywords.get(k1.baseForm).tf;
				sim += TFIDF(tf1, idf(df, docSize)) * TFIDF(tf2, idf(df, docSize));
			}
		}
		return sim / d1.vectorSize / d2.vectorSize;
	}

	public double cosineSimilarity(HashMap<String, Node> community, Document d2, HashMap<String, Double> DF, int docSize) {
		double sim = 0;
		double vectorSize1 = 0;
		int numberOfKeywordsInCCommon = 0;
		for (Node n : community.values()) {
			double nTF = 0;
			for (Edge e : n.edges.values())
				// nTF += e.df;
				nTF += Math.max(e.cp1, e.cp2);
			// nkeywordtf += (n.equals(e.n2)) ? e.cp1 : e.cp2;
			n.keyword.tf = nTF / n.edges.size();
			vectorSize1 += Math.pow(TFIDF(n.keyword.tf, idf(DF.get(n.keyword.baseForm), docSize)), 2);

			if (d2.keywords.containsKey(n.keyword.baseForm)) {
				numberOfKeywordsInCCommon++;
				sim += TFIDF(n.keyword.tf, idf(DF.get(n.keyword.baseForm), docSize))
						* TFIDF(d2.keywords.get(n.keyword.baseForm).tf, idf(DF.get(n.keyword.baseForm), docSize));
			}
		}
		vectorSize1 = Math.sqrt(vectorSize1);
		if (numberOfKeywordsInCCommon > 2)
			return sim / vectorSize1 / d2.vectorSize;
		else
			return 0;
	}

	public double variance(DocumentCluster dc, HashMap<String, Double> DF, int docSize, HashMap<String, Node> graphNodes) {
		double var = 0;
		if (dc.centroid == null)
			dc.centroid = centroid(dc.docs, DF, graphNodes);
		for (Document d : dc.docs.values()) {
			double diff = 1 - cosineSimilarity(dc.centroid, d, DF, docSize);
			var += diff * diff;
		}
		return var / dc.docs.size();
	}

	public Document centroid(HashMap<String, Document> docs, HashMap<String, Double> DF, HashMap<String, Node> graphNodes) {
		Document centroid = new Document("-1");
		for (Document d : docs.values())
			for (Keyword k : d.keywords.values()) {
				// if (graphNodes.containsKey(k.baseForm))
				if (centroid.keywords.containsKey(k.baseForm)) {
					Keyword kk = centroid.keywords.get(k.baseForm);
					kk.tf += k.tf;
					kk.df++;
				} else
					centroid.keywords.put(k.baseForm, new Keyword(k.baseForm, k.getWord(), k.tf, k.df, 0));
			}
		for (Keyword k : centroid.keywords.values())
			if (idf(k.df, docs.size()) != 0) {// DF.get(k.baseForm) >
				// if (idf(DF.get(k.baseForm), 2) != 0) {// DF.get(k.baseForm) >
				// Constants.KEYWORD_DF_MIN)
				// {
				k.tf /= docs.size();
				centroid.vectorSize += Math.pow(TFIDF(k.tf, DF.get(k.baseForm)), 2);
			} else
				k.tf = 0;
		centroid.vectorSize = Math.sqrt(centroid.vectorSize);
		return centroid;
	}

	public double idf(double df, int size) {
		// if (df < constants.SIMILARITY_KEYWORD_DF_MIN || df >
		// constants.NODE_DF_MAX * size)
		// return 0;
		return Math.log(size / df) / Math.log(2);
	}
}
