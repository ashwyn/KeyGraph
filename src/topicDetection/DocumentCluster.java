package topicDetection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class DocumentCluster {
	public static int max_id = 1;
	public int id = max_id++;
	public HashMap<String, Node> keyGraph = new HashMap<String, Node>();
	public HashMap<String, Document> docs = new HashMap<String, Document>();
	public HashMap<String, Double> similarities = new HashMap<String, Double>();
	public Document centroid;
	public double variance;
	public Topic MatchingEvent;
	public double matchingEventScore;
	static HashSet<String> stopwords = Utils.importStopwords();
	Porter porter = new Porter();

	public void serialize(PrintStream out, boolean printDocContent) {
		out.println("\n****** Community #" + id);
		DocumentAnalyze.printKeywords(this, out);
		DocumentAnalyze.printKeyGraph(keyGraph, out);
		out.println("number of docs:\t" + docs.size());
		if (!printDocContent)
			for (Document d : docs.values())
				out.println(d.publishDate + "\t" + d.id + "\t" + similarities.get(d.id));
		else
			for (Document d : docs.values())
				out.println(d.publishDate + "\t" + d.id + "\t" + similarities.get(d.id) + "\t" + d.getBody());
	}

	public boolean deserialize(BufferedReader in, boolean readDocContent) throws Exception {
		keyGraph = new HashMap<String, Node>();
		docs = new HashMap<String, Document>();

		String line = null;
		while ((line = in.readLine()) != null && !line.startsWith("****** Community #"))
			;
		if (line == null)
			return false;
		id = Integer.parseInt(line.substring(line.lastIndexOf(' ') + 2));

		// ---load old version of keywords
		// String[] keywords = in.readLine().split(",");
		// keyGraph = new HashMap<String, Node>();
		// for (int i = 0; i < keywords.length; i++) {
		// keywords[i] = keywords[i].trim();
		// if (keywords[i].length() > 0)
		// keyGraph.put(keywords[i], new Node(new
		// Keyword(DatasetUtil.getBaseForm(stopwords, porter, keywords[i]),
		// keywords[i], 1, 0, 0)));
		// }

		// ---load new version of keywords
		in.readLine();
		// in.readLine();
		String[] tokens = in.readLine().split(",");
		keyGraph = new HashMap<String, Node>();
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = tokens[i].trim();
			if (tokens[i].length() > 0) {
				String nodeid = tokens[i].split(":")[0];
				String base = tokens[i].split(":")[1];
				String word = tokens[i].split(":")[2];
				// DatasetUtil.getBaseForm(stopwords, porter, word);
				keyGraph.put(base, new Node(new Keyword(base, word, 1, 0, 0)));
			}
		}
		// ---load edges
		tokens = in.readLine().split(",");
		System.out.println(Arrays.asList(tokens));
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = tokens[i].trim();
			if (tokens[i].length() > 0) {
				String base1 = tokens[i].split("-")[0].split(":")[1];
				String base2 = tokens[i].split("-")[1].split(":")[1];
				Node n1 = keyGraph.get(base1);
				Node n2 = keyGraph.get(base2);

				String edgeId = Edge.getId(n1, n2);
				// if(edgeId.equals("liabil_model"))
				// System.out.println("hooooooooooo");

				if (!n1.edges.containsKey(edgeId)) {
					Edge e = new Edge(n1, n2, edgeId);
					n1.edges.put(edgeId, e);
					n2.edges.put(edgeId, e);
				}

			}
		}

		// ---load docs
		// in.readLine();
		line = in.readLine();
		int docsize = Integer.parseInt(line.split("\t")[1]);
		System.out.println(line);
		for (int i = 0; i < docsize; i++) {
			line = in.readLine();
			String[] terms = line.split("\t", 4);
			Document d = new Document(terms[1]);
			if (!terms[2].equals("null"))
				similarities.put(d.id, Double.parseDouble(terms[2]));
			if (!terms[0].equals("null"))
				d.publishDate = Timestamp.valueOf(terms[0]);
			if (readDocContent)
				d.setBody(tokens[3]);
			docs.put(d.id, d);
		}
		return true;
	}

	public static void serializeAll(ArrayList<DocumentCluster> dcs, PrintStream out, boolean printDocContent) throws Exception {
		for (DocumentCluster dc : dcs)
			dc.serialize(out, printDocContent);
	}

	public static ArrayList<DocumentCluster> deserializeAll(String inputFile, boolean redDocContent) throws Exception {
		ArrayList<DocumentCluster> events = new ArrayList<DocumentCluster>();
		BufferedReader in = new BufferedReader(new InputStreamReader(DocumentCluster.class.getClassLoader().getResourceAsStream(inputFile)));
		do {
			DocumentCluster dc = new DocumentCluster();
			dc.deserialize(in, redDocContent);
			events.add(dc);
		} while (in.readLine() != null);
		in.close();
		return events;
	}
}
