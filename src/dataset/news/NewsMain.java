package dataset.news;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import topicDetection.Constants;
import topicDetection.Document;
import topicDetection.DocumentAnalyze;
import topicDetection.DocumentCluster;
import topicDetection.Edge;
import topicDetection.EvaluationAnalyze;
import topicDetection.Topic;
import topicDetection.GraphAnalyze;
import topicDetection.Keyword;
import topicDetection.Node;
import topicDetection.Porter;
import topicDetection.Utils;

public class NewsMain {

	public static Constants constants;

	public static void main(String[] args) throws Exception {
		constants = new Constants("conf/NewsConstants.txt");
		// retrieveOriginakKeywords();
		// prepareDataforLDADave();
		// prepareDataforLDAMallet();
		// System.exit(0);

		// refindNEs();
		// new
		// TDTUtil(constants).correctXMLChars("/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/tkn_sgm",
		// "/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/tkn_sgm_corrected");

		// TDTUtil.extraxtText_mmtkn_sgm("/fs/clip-tdt/data/TDT4/tdt4_aem_v1_0/tkn_sgm",
		// "/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/tkn_sgm/");
		// TDTUtil.extraxtText_mmtkn_sgm("/fs/clip-tdt/data/TDT4/tdt4_aem_v1_0/mttkn_sgm",
		// "/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/mttkn_sgm/");

		// generateTDTTopicFiles();
		// generateDateFiles();
		// ideaTest();
		// runFindEventsStream();
		run();
		// keygraphGrow();
		// refindNEs();
		// test();
		// runParameterEstimation();
	}

	private static void generateTDTTopicFiles() throws Exception {
		HashMap<String, HashSet<String>> topics = new HashMap<String, HashSet<String>>();
		loadTopicFile(topics, "/fs/clip-tdt/data/TDT4/2003topic_annotations/2002/tdt2002_topic_rel.v2_1", true);
		loadTopicFile(topics, "/fs/clip-tdt/data/TDT4/2003topic_annotations/2003/tdt2003_topic_rel.v2_0", true);

		for (String topicid : topics.keySet()) {
			BufferedWriter out = new BufferedWriter(new FileWriter("./data/topic_new/" + topicid + ".txt"));
			for (String docno : topics.get(topicid))
				out.write(docno + "\n");
			out.close();
		}

		System.out.println(topics.size() + ":" + topics);
	}

	public static void refindNEs() throws Exception {
		HashSet<String> nes = new HashSet<String>();
		String[] keyfiles = new File(constants.DATA_KEYWORDS_1_PATH).list();
		BufferedReader in = null;
		for (String filename : keyfiles) {
			in = new BufferedReader(new FileReader(constants.DATA_KEYWORDS_1_PATH + filename));
			String line = null;
			while ((line = in.readLine()) != null && line.length() > 2) {
				int index = line.lastIndexOf("==");
				// String token = line.substring(0, index).trim();
				String word = line.substring(0, index).trim();
				String[] tokens = word.split(" and ");
				for (String token : tokens)
					if (token.length() > 2)
						nes.add(token.toLowerCase());
			}
			in.close();
		}

		int i = 0;
		String outdir = "data/key_NE_new/";
		String textdir = "/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/output/text/";
		for (String filename : keyfiles) {
			in = new BufferedReader(new FileReader(textdir + filename));
			String line = null;
			String content = "";
			while ((line = in.readLine()) != null)
				content += line.toLowerCase() + " ";
			// content=" "+content.replaceAll("[.,:/\\-_]"," " )+" ";
			HashMap<String, Integer> keys = new HashMap<String, Integer>();
			for (String ne : nes) {
				// if (content.indexOf(ne) != -1)
				if (content.indexOf(" " + ne + " ") != -1)
					if (keys.containsKey(ne))
						keys.put(ne, keys.get(ne) + 1);
					else
						keys.put(ne, 1);
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(outdir + filename));
			for (String key : keys.keySet())
				out.write(key + "==" + keys.get(key) + "\n");
			out.close();
			i++;
			if (i % 100 == 0)
				System.out.println(i + "/" + keyfiles.length);
		}
	}

	public static void loadTopicFile(HashMap<String, HashSet<String>> topics, String topicFile, boolean filterNonEnglish) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(topicFile));
		String line = null;
		while ((line = in.readLine()) != null)
			if (line.startsWith("<ONTOPIC")) {
				String topicid = line.substring(line.indexOf("topicid="));
				topicid = topicid.substring(topicid.indexOf('=') + 1, topicid.indexOf(' '));

				String docno = line.substring(line.indexOf("docno="));
				docno = docno.substring(docno.indexOf('=') + 1, docno.indexOf(' '));

				String fileid = line.substring(line.indexOf("fileid="));
				fileid = fileid.substring(fileid.indexOf('=') + 1, fileid.indexOf(' '));

				if (!filterNonEnglish || !(fileid.endsWith("MAN") || fileid.endsWith("ARB") || fileid.endsWith("TWN")))
					if (topics.containsKey(topicid))
						topics.get(topicid).add(docno);

					else {
						HashSet<String> docs = new HashSet<String>();
						docs.add(docno);
						topics.put(topicid, docs);
					}
			}
		in.close();
	}

	public static void ideaTest() {
		HashSet<String> stopwords = Utils.importStopwords();
		// Document.generateDataFiles(5, .2);
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		ArrayList<Topic> events = new ArrayList<Topic>();
		new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(events, docs, stopwords, DF);

		GraphAnalyze g = new GraphAnalyze(constants);
		g.buildGraph(docs, DF, constants.REMOVE_DUPLICATES);

		HashMap<Node, HashSet<Topic>> nodeEvents = new HashMap<Node, HashSet<Topic>>();
		HashMap<Topic, HashMap<String, Node>> eventNodes = new HashMap<Topic, HashMap<String, Node>>();
		for (Node n : g.graphNodes.values())
			nodeEvents.put(n, new HashSet<Topic>());
		for (Topic e : events)
			eventNodes.put(e, new HashMap<String, Node>());

		for (Topic e : events) {
			HashSet<Node> nodes = new HashSet<Node>();
			for (Document d : e.docs.values())
				for (Keyword k : d.keywords.values()) {
					Node n = g.graphNodes.get(k.baseForm);
					if (n != null)
						if (nodes.add(n)) {
							nodeEvents.get(n).add(e);
							eventNodes.get(e).put(n.keyword.baseForm, n);
						}
				}
		}

		for (Node n : nodeEvents.keySet())
			if (nodeEvents.get(n).size() > 1)
				for (HashMap<String, Node> nodes : eventNodes.values())
					nodes.remove(n.keyword.baseForm);

		// for (Node n : g.graphNodes.values()) {
		// System.out.print("##" + n.keyword.baseForm + " : ");
		// for (Event e : nodeEvents.get(n))
		// System.out.print(e.id + ", ");
		// System.out.println();
		// }

		ArrayList<HashMap<String, Node>> communities = new ArrayList<HashMap<String, Node>>();
		for (HashMap<String, Node> community : eventNodes.values())
			// communities.add(getBiggestConnectedComponnent(community));
			communities.add(community);

		EvaluationAnalyze.evaluate(events, new DocumentAnalyze(constants).extractClustersFromKeyCommunity(docs, communities, DF, docs.size(), g.graphNodes));

		System.out.println("done");

	}

	public static HashMap<String, Node> getBiggestConnectedComponnent(HashMap<String, Node> community) {
		int originalsize = community.size();
		int biggestSize = -1;
		HashMap<String, Node> biggestCommunity = new HashMap<String, Node>();
		ArrayList<HashMap<String, Node>> ccs = new GraphAnalyze(constants).findConnectedComponentsFromSubset(community);
		for (HashMap<String, Node> cc : ccs)
			if (cc.size() > biggestSize) {
				biggestSize = cc.size();
				biggestCommunity = cc;
			}

		System.out.println("new/original:::::" + biggestSize + "/" + originalsize);
		return biggestCommunity;
	}

	public static double isDense(HashMap<String, Node> community) {
		double count = 0;
		for (Node node : community.values())
			for (Node n : community.values())
				if (n.edges.containsKey(Edge.getId(node, n)))
					count++;
		return count / community.size() / (community.size() - 1);
	}

	public static void runFindEventsStream() throws Exception {
		// String[] inputs = new String[] { "2000-10", "2000-11", "2000-12",
		// "2001-01" };
		Timestamp start = Timestamp.valueOf("2000-10-01 12:00:00");
		Timestamp end = Timestamp.valueOf("2001-01-31 23:59:59");
		int windowSize = 40;
		int windowShiftSize = windowSize / 2;

		HashSet<String> stopwords = Utils.importStopwords();
		ArrayList<DocumentCluster> eventStream = new ArrayList<DocumentCluster>();
		PrintStream out = new PrintStream(new File("eventStream.txt"));

		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		long DayToMiliSeconds = (long) 24 * 60 * 60 * 1000;

		ArrayList<Topic> topics = new ArrayList<Topic>();
		System.out.println("[" + start + ", " + new Timestamp(start.getTime() + windowSize * DayToMiliSeconds) + "]");
		new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(topics, docs, stopwords, DF, start, new Timestamp(start.getTime() + windowSize
				* DayToMiliSeconds));
		DocumentAnalyze documentAnalyzer = new DocumentAnalyze(constants);
		ArrayList<DocumentCluster> lastEvents = documentAnalyzer.clusterbyKeyGraph(docs, DF);

		for (Timestamp startDay = new Timestamp(start.getTime() + windowShiftSize * DayToMiliSeconds); startDay.before(new Date(end.getTime() - windowSize
				* DayToMiliSeconds)); startDay.setTime(startDay.getTime() + windowShiftSize * DayToMiliSeconds)) {
			DF = new HashMap<String, Double>();
			docs = new HashMap<String, Document>();
			topics = new ArrayList<Topic>();
			out.println();

			System.out.println("[" + startDay + ", " + new Timestamp(startDay.getTime() + windowSize * DayToMiliSeconds) + "]");
			new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(topics, docs, stopwords, DF, startDay, new Timestamp(startDay.getTime() + windowSize
					* DayToMiliSeconds));
			out.println(docs.size() + " docs are loaded!");
			ArrayList<DocumentCluster> events = documentAnalyzer.clusterbyKeyGraph(docs, DF);

			// ----------- merge events -----------
			for (DocumentCluster dc1 : lastEvents) {
				boolean isMerged = false;
				for (DocumentCluster dc2 : events)
					if (documentAnalyzer.intersectDocs(dc1.docs, dc2.docs) > 3) {
						isMerged = true;
						dc2.docs.putAll(dc1.docs);
						dc2.keyGraph.putAll(dc2.keyGraph);
					}
				if (!isMerged) {
					eventStream.add(dc1);
					// --- Save and Print
//					documentAnalyzer.printCluster(dc1, out, false);
					dc1.serialize(out, false);
					// dc1.docs = null;
				}
			}
			lastEvents = events;
			events = null;
		}
		for (DocumentCluster dc1 : lastEvents) {
			eventStream.add(dc1);
			// --- Save and Print
			out.println();
//			documentAnalyzer.printCluster(dc1, out, false);
			dc1.serialize(out, false);
			// dc1.docs = null;
		}
		System.out.println("done");

		topics = new ArrayList<Topic>();
		new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(topics, docs, stopwords, DF);
		EvaluationAnalyze.evaluate(topics, eventStream);

	}

	public static String[] slice(String[] input, int start, int end) {
		String[] out = new String[end - start + 1];
		for (int i = 0; i < out.length; i++)
			out[i] = input[start + i];
		return out;
	}

	public static void run() throws Exception {
		HashSet<String> stopwords = Utils.importStopwords();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		ArrayList<Topic> events = new ArrayList<Topic>();
		new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(events, docs, stopwords, DF);
		System.out.println(docs.size() + " docs are loaded!");

		ArrayList<DocumentCluster> clusters = new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
		// ArrayList<DocumentCluster> clusters = new
		// DocumentAnalyze(constants).clusterByLDA(docs, DF,"tdt-mallet");
		// for(Event e:events){
		// ArrayList<String> toRemove=new ArrayList<String>();
		// for(String docid:e.docs.keySet())
		// if(!docs.containsKey(docid))
		// toRemove.add(docid);
		// for(String id:toRemove)
		// e.docs.remove(id);
		// }
		EvaluationAnalyze.evaluate(events, clusters);

//		DocumentAnalyze.printClusters(clusters, new PrintStream(new File("events.txt")));
		DocumentCluster.serializeAll(clusters, new PrintStream(new File("events.txt")), false);
		System.out.println("done");

	}

	public static void prepareDataforLDADave() throws Exception {
		HashSet<String> stopwords = Utils.importStopwords();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		ArrayList<Topic> events = new ArrayList<Topic>();
		new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(events, docs, stopwords, DF);
		System.out.println(docs.size() + " docs are loaded!");

		HashMap<String, Integer> indexes = new HashMap<String, Integer>();
		int i = 0;
		for (String key : DF.keySet())
			indexes.put(key, ++i);

		i = 0;
		BufferedWriter dataFile = new BufferedWriter(new FileWriter("tdt.dave.data.txt"));
		BufferedWriter indexFile = new BufferedWriter(new FileWriter("tdt.dave.index.txt"));
		for (Document d : docs.values()) {
			indexFile.write((++i) + " " + d.id + "\n");
			dataFile.write("" + d.keywords.size());
			for (Keyword k : d.keywords.values())
				dataFile.write(" " + indexes.get(k.baseForm) + ":" + (int) (k.tf));
			dataFile.write("\n");
		}

		dataFile.close();
		indexFile.close();
	}

	public static void prepareDataforLDAMallet() throws Exception {
		HashSet<String> stopwords = Utils.importStopwords();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		ArrayList<Topic> events = new ArrayList<Topic>();
		new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(events, docs, stopwords, DF);
		System.out.println(docs.size() + " docs are loaded!");

		// BufferedWriter dataFile = new BufferedWriter(new
		// FileWriter("tdt.mallet.data.txt"));
		for (Document d : docs.values()) {
			BufferedWriter dataFile = new BufferedWriter(new FileWriter("/nfshomes/sayyadi/Desktop/GeoNets/hassan/source_codes/mallet-2.0.5/tdtdata_new/"
					+ d.id + ".txt"));
			// dataFile.write(d.id+".txt eng");
			for (Keyword k : d.keywords.values())
				for (int z = 0; z < k.tf; z++)
					dataFile.write(" " + k.baseForm);
			dataFile.write("\n");
			dataFile.close();
		}

		// dataFile.close();
	}

	public static void robustness() throws Exception {
		HashSet<String> stopwords = Utils.importStopwords();
		DataOutputStream out = new DataOutputStream(new FileOutputStream("parameters.txt"));
		for (double i = .05; i <= .5; i += .05) {
			constants.EDGE_CORRELATION_MIN = i;

			HashMap<String, Double> DF = new HashMap<String, Double>();
			HashMap<String, Document> docs = new HashMap<String, Document>();
			ArrayList<Topic> events = new ArrayList<Topic>();
			new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(events, docs, stopwords, DF);
			try {
				double[] result = EvaluationAnalyze.evaluate(events, new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF));
				out.writeBytes(i + "\t" + result[0] + "\t" + result[1] + "\t" + result[2] + "\r\r\n");
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		out.close();

		System.out.println("done");

	}

	public static void runParameterEstimation() throws Exception {
		HashSet<String> stopwords = Utils.importStopwords();
		DataOutputStream out = new DataOutputStream(new FileOutputStream("parameters.txt"));
		for (double i = .1; i < .5; i += .05)
			for (double j = .001; j < .03; j += .005)
				for (double k = 0.25; k > .1; k -= .03)
					for (double l = .25; l <= .25; l += .05)
						for (int m = 2; m < 6; m++)
							for (int n = 3; n < 8; n++)

							{
								constants.DOC_SIM2KEYGRAPH_MIN = i;
								constants.DOC_SIM2CENTROID_MIN = j;
								constants.EDGE_CORRELATION_MIN = k;
								constants.EDGE_DF_MIN = m;
								constants.NODE_DF_MIN = n;
								constants.CLUSTER_INTERSECT_MIN = l;

								HashMap<String, Double> DF = new HashMap<String, Double>();
								HashMap<String, Document> docs = new HashMap<String, Document>();
								ArrayList<Topic> events = new ArrayList<Topic>();
								new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(events, docs, stopwords, DF);
								try {
									double[] result = EvaluationAnalyze.evaluate(events, new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF));
									out.writeBytes(constants.DOC_SIM2KEYGRAPH_MIN + "\t" + constants.DOC_SIM2CENTROID_MIN + "\t"
											+ constants.EDGE_CORRELATION_MIN + "\t" + constants.EDGE_DF_MIN + "\t" + constants.NODE_DF_MIN + "\t"
											+ constants.CLUSTER_INTERSECT_MIN + "\t" + result[0] + "\t" + result[1] + "\t" + result[2] + "\r\r\n");
									out.flush();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
		out.close();

		System.out.println("done");

	}

	// public static void generateDateFiles() {
	// DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmm");
	// try {
	// File f = new File(Constants.DATA_TOPIC_PATH);
	// File[] files = f.listFiles();
	// HashMap<String, HashSet<String>> dates = new HashMap<String,
	// HashSet<String>>();
	// for (File ff : files) {
	// DataInputStream reader = new DataInputStream(new FileInputStream(ff));
	// String line = null;
	// while ((line = reader.readLine()) != null && line.trim().length() > 0) {
	// Timestamp publishdate = new Timestamp(df.parse(line.substring(3,
	// line.lastIndexOf('.'))).getTime());
	// String key = publishdate.toString().substring(0, 7);
	// if (dates.get(key) == null)
	// dates.put(key, new HashSet<String>());
	// dates.get(key).add(line);
	// }
	// }
	// for (String key : dates.keySet()) {
	// BufferedWriter out = new BufferedWriter(new FileWriter("data/date/" +
	// key));
	// for (String file : dates.get(key))
	// out.write(file + "\n");
	// out.close();
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	//

	public static void test() throws Exception {
		// HashMap<String, Document> docs = new HashMap<String, Document>();
		BufferedReader in = null;
		String out = "$2/";
		HashSet<String> ids = new HashSet<String>();

		File f = new File("./data/topic_new/");
		File[] files = f.listFiles();
		for (int i = 0; i < files.length; i++) {
			in = new BufferedReader(new FileReader(files[i]));
			String line = null;
			while ((line = in.readLine()) != null)
				ids.add(line);
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter("test.txt"));
		for (String id : ids)
			writer.write("cp $1/" + id + ".txt " + out + "\n");

		writer.close();
	}

	public static void keygraphGrow2() throws Exception {
		HashSet<String> stopwords = Utils.importStopwords();
		Porter porter = new Porter();
		HashMap<String, Double> DF = new HashMap<String, Double>();

		DataInputStream topicin = new DataInputStream(new FileInputStream("/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/output/allfiletopics/alltopics.txt"));
		String fileName = null;
		int i = 0;
		while ((fileName = topicin.readLine()) != null) {

			try {
				DataInputStream in = new DataInputStream(new FileInputStream(constants.DATA_TEXT_PATH + fileName + ".txt"));
				String content = null;
				String line = null;
				HashSet<String> keywords = new HashSet<String>();
				while ((line = in.readLine()) != null)
					content += line;
				StringTokenizer st = new StringTokenizer(content, "!?|\"' -_@0123456789.,;#$&%/\\*()<>\t");
				while (st.hasMoreTokens()) {
					String word = st.nextToken();
					String token = word.toLowerCase();
					String base = "";
					if ((token.indexOf("?") == -1 && token.length() > 2 && !stopwords.contains(token)))
						base = porter.stripAffixes(token);

					if (base.length() > 2)
						keywords.add(base);
				}
				i++;
				in.close();
				for (String word : keywords)
					if (!DF.containsKey(word))
						DF.put(word, 1.);
					else
						DF.put(word, DF.get(word) + 1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (i % 1000 == 0)
				System.out.println(i + "\t" + sizeof(DF, i));
		}
	}

	public static int sizeof(HashMap<String, Double> DF, double size) {
		int count = 0;
		for (double df : DF.values())
			if (df < size / 4 & df >= size / 100)
				count++;
		return count;
	}

	public static void keygraphGrow() throws Exception {
		DataInputStream in = new DataInputStream(new FileInputStream("grow.txt"));
		String line = null;
		String content = "";
		while ((line = in.readLine()) != null)
			content += line + "\n";
		in.close();
		DataOutputStream out = new DataOutputStream(new FileOutputStream("grow.txt"));
		out.writeBytes(content);
		ArrayList<Integer> docscount = new ArrayList<Integer>();
		ArrayList<Integer> wordscount = new ArrayList<Integer>();
		for (double perc = .01; perc <= .3; perc += .02) {
			NewsDataLoader.percent = perc;
			HashSet<String> stopwords = Utils.importStopwords();
			HashMap<String, Double> DF = new HashMap<String, Double>();
			HashMap<String, Document> docs = new HashMap<String, Document>();
			ArrayList<Topic> events = new ArrayList<Topic>();
			new NewsDataLoader(constants).loadDocumentKeyFilesByTopics(events, docs, stopwords, DF);
			System.out.println(docs.size() + " docs are loaded!");
			GraphAnalyze keyGraph = new GraphAnalyze(constants);
			keyGraph.buildGraph(docs, DF, constants.REMOVE_DUPLICATES);
			docscount.add(docs.size());
			wordscount.add(keyGraph.graphNodes.size());
			out.writeBytes(docs.size() + "\t" + keyGraph.graphNodes.size() + "\n");
			out.flush();
		}
		System.out.println("*****");
		for (Integer s : docscount)
			System.out.println(s);

		System.out.println("*****");
		for (Integer s : wordscount)
			System.out.println(s);
		out.close();
	}

}
