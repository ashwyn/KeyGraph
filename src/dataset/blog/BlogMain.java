package dataset.blog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import topicDetection.Constants;
import topicDetection.DataLoader;
import topicDetection.Document;
import topicDetection.DocumentAnalyze;
import topicDetection.DocumentCluster;
import topicDetection.GraphAnalyze;
import topicDetection.Keyword;
import topicDetection.Node;
import topicDetection.Porter;
import topicDetection.Utils;


public class BlogMain {

	public static String host = null;
	public static Constants constants;

	public static void runLDA() throws Exception {
		PrintStream out = new PrintStream("events.mallet.txt");

		double toMins = 1000 * 60;
		HashSet<String> stopwords = Utils.importStopwords();

		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		new BlogDataLoader(constants).loadDocumentKeyFilesByDate(getDateList(1, 7), docs, stopwords, DF);

		prepareDataforLDAMallet(docs);
		long time1 = System.currentTimeMillis();
		runCommand("/fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/bin/mallet import-dir --input /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3rdata/ --output /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3r.NENP.mallet --keep-sequence");
		long time2 = System.currentTimeMillis();

		// new DocumentAnalyze(constants).clusterKeyGraph(docs, DF);
		// System.out.println("/fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/bin/mallet train-topics --input /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3r.NENP.mallet  --num-topics "+docs.size()/200+" --output-doc-topics results-mallet.txt");
		runCommand("/fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/bin/mallet train-topics --input /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3r.NENP.mallet  --num-topics "
				+
//				docs.size() /
				200 + " --output-doc-topics results-mallet.txt");
		ArrayList<DocumentCluster> clusters = new DocumentAnalyze(constants).clusterByLDA(docs, DF, "mallet");

//		DocumentAnalyze.printClusters(clusters, out);
		DocumentCluster.serializeAll(clusters, out, false);
		out.close();

		long time3 = System.currentTimeMillis();
		System.out.println(docs.size() + "\t" + (time3 - time1) / toMins + "\t" + (time2 - time1) / toMins + "\t"
				+ (time3 - time2) / toMins + "\n");
		out.close();
//		System.out.println("*********");
//		for(DocumentCluster dc:clusters)
//			System.out.println(dc.docs.size());
	}

	public static void test() throws Exception {
		PrintStream out = new PrintStream("events.keygraph.stat.txt");

		double toMins = 1000 * 60;
		HashSet<String> stopwords = Utils.importStopwords();

		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		long time1 = System.currentTimeMillis();
		new BlogDataLoader(constants).loadDocumentKeyFilesByDate(getDateList(1, 7), docs, stopwords, DF);
		long time2 = System.currentTimeMillis();

		ArrayList<DocumentCluster> clusters = new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
//		DocumentAnalyze.printClusters(clusters, out);
		DocumentCluster.serializeAll(clusters, out, false);
		out.close();

		long time3 = System.currentTimeMillis();
		System.out.println(docs.size() + "\t" + (time3 - time1) / toMins + "\t" + (time2 - time1) / toMins + "\t"
				+ (time3 - time2) / toMins + "\n");
		System.out.println("*********");
		for(DocumentCluster dc:clusters)
			System.out.println(dc.docs.size());
	}

	public static void runKeyGraphForTheWebsite(String inputFile, String outputFile) throws Exception {

		double toMins = 1000 * 60;
		HashSet<String> stopwords = Utils.importStopwords();

		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		Porter porter = new Porter();
		long time1 = System.currentTimeMillis();
		new BlogDataLoader(constants).loadDocuments(inputFile, docs, stopwords, DF, porter,constants.REMOVE_DUPLICATES);
		long time2 = System.currentTimeMillis();

		ArrayList<DocumentCluster> clusters = new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
		DocumentAnalyze.printClustersForTheWebsite(clusters, outputFile);

		long time3 = System.currentTimeMillis();
		System.out.println(docs.size() + "\t" + (time3 - time1) / toMins + "\t" + (time2 - time1) / toMins + "\t"
				+ (time3 - time2) / toMins + "\n");
	}

	public static void main(String[] args) throws Exception {
		// shanchanRun();
		constants = new Constants("conf/BlogConstants.txt");
//		 runLDA();
//		test();
//		runKeyGraphForTheWebsite(args[0]);

		// retrieveOriginakKeywords();
		 test();
		// tmp();

//		 runTimeEvaluationKeyGraph();
		// runTimeEvaluationLDA();
		// System.exit(0);

		// host = args[2];
		// findEventsChunks(Integer.parseInt(args[0]),
		// Integer.parseInt(args[1]));
		// findEventsStreamFromEventLog();
		// extractEventsForGeorgia();
		// extractEventsForISCRAM();
		// extractKeyGraphForISCRAMEvent("election");

		// extractEventsStreamThenBatch();

		// findEventsStream();
		// runParameterEstimation();
	}

	private static void extractEventsStreamThenBatch() throws Exception, FileNotFoundException {
		ArrayList<DocumentCluster> events = DocumentCluster.deserializeAll("eventsStream.txt",false);
		HashSet<String> docids = new HashSet<String>();
		for (DocumentCluster dc : events)
			if (dc.docs.size() < 700)
				docids.addAll(dc.docs.keySet());
			else
				DocumentAnalyze.printKeywords(dc, System.out);
		System.out.println("**********" + docids.size());
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		HashSet<String> stopwords = Utils.importStopwords();
		Porter porter = new Porter();
		String[] fileList = new String[docids.size()];
		new DataLoader(constants).loadDocumentsForSpinn3r(docids.toArray(fileList), docs, stopwords, DF, porter);
		ArrayList<DocumentCluster> clusters = new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
		PrintStream out = new PrintStream("eventBatch.txt");
//		DocumentAnalyze.printClusters(clusters, out);
		DocumentCluster.serializeAll(clusters, out, false);
		out.close();
	}

	public static void findEventsChunks(int start, int end) throws Exception {
		int windowSize = 2;
		int windowShiftSize = 1;

		HashSet<String> stopwords = Utils.importStopwords();
		// ArrayList<DocumentCluster> eventsStream = new
		// ArrayList<DocumentCluster>();

		HashMap<String, Double> DF = null;
		HashMap<String, Document> docs = null;

		boolean isFinished = false;
		while (!isFinished) {
			isFinished = true;

			for (int startDay = start; startDay <= end - windowSize + 1; startDay += windowShiftSize) {
				String[] dateList = getDateList(startDay, windowSize);
				File file = new File("events/events_" + dateList[0] + "_" + dateList[dateList.length - 1] + ".txt");
				if (file.exists()) {
					System.out.println(file.getName() + " exists!!!");
					continue;
				}
				DF = new HashMap<String, Double>();
				docs = new HashMap<String, Document>();
				isFinished = false;
				PrintStream out = new PrintStream(file);
				PrintStream logger = new PrintStream(new File("events/events_" + dateList[0] + "_"
						+ dateList[dateList.length - 1] + ".log"));
				logger.println("Host: " + host);
				GraphAnalyze graphAnalyzer = new GraphAnalyze(constants);
				graphAnalyzer.logger = logger;
				// out.println();
				// out.println("---");
				out.println(Arrays.toString(dateList));
				logger.println(Arrays.toString(dateList));
				new BlogDataLoader(constants).loadDocumentKeyFilesByDate(dateList, docs, stopwords, DF);
				out.println(docs.size() + " docs are loaded!");
				ArrayList<DocumentCluster> events = new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
				for (DocumentCluster dc1 : events) {
					out.println();
//					DocumentAnalyze.printCluster(dc1, out);
					dc1.serialize(out,false);
					dc1.docs = null;
				}
				out.close();
				logger.println("done");
				logger.close();
			}
		}
		System.out.println("done");
	}

	public static void findEventsStreamFromEventLog() throws Exception {
		int start = 1;
		int end = 31;
		int windowSize = 2;
		int windowShiftSize = 1;

		PrintStream out = new PrintStream("eventsStreamWithKeyGraphInfo.txt");

		String[] dateList = getDateList(start, windowSize);
		System.out.println(Arrays.toString(dateList));
		String inputFile = "eventsWithKeyGraphInfo/events_" + dateList[0] + "_" + dateList[dateList.length - 1] + ".txt";
		ArrayList<DocumentCluster> lastingEvents = DocumentCluster.deserializeAll(inputFile,false);

		for (int startDay = start + windowShiftSize; startDay <= end - windowSize; startDay += windowShiftSize) {
			out.println();
			dateList = getDateList(startDay, windowSize);
			out.println(Arrays.toString(dateList));
			System.out.println(Arrays.toString(dateList));
			inputFile = "eventsWithKeyGraphInfo/events_" + dateList[0] + "_" + dateList[dateList.length - 1] + ".txt";

			out.println("?? docs are loaded!");
			ArrayList<DocumentCluster> events = DocumentCluster.deserializeAll(inputFile,false);

			DocumentAnalyze documentAnalyzer = new DocumentAnalyze(constants);

			// ----------- merge events -----------
			for (DocumentCluster dc1 : lastingEvents) {
				boolean isMerged = false;
				for (DocumentCluster dc2 : events) {
					if (dc1.docs.size() > 0 && dc2.docs.size() > 0)
						if (documentAnalyzer.intersectDocs(dc1.docs, dc2.docs) * 1.0 / Math.min(dc1.docs.size(), dc2.docs.size()) > .2) {
							isMerged = true;
							dc2.docs.putAll(dc1.docs);
							// ---old merge
							// dc2.keyGraph.putAll(dc1.keyGraph);
							// ---new merge
							dc2.keyGraph = GraphAnalyze.mergeKeyGraphs(dc1.keyGraph, dc2.keyGraph);
							// System.out.println("haaaaaa");
						}
				}
				if (!isMerged) {
					// eventsStream.add(dc1);
					// --- Save and Print
					if (dc1.docs.size() > 30){
//						DocumentAnalyze.printCluster(dc1, out);
						dc1.serialize(out,false);
					}
					dc1.docs = null;
				}
			}
			lastingEvents = events;
			events = null;
		}
		for (DocumentCluster dc1 : lastingEvents) {
			// eventsStream.add(dc1);
			// --- Save and Print
			out.println();
//			DocumentAnalyze.printCluster(dc1, out);
			dc1.serialize(out,false);
			dc1.docs = null;
		}
		System.out.println("done");
	}

	public static void findEventsStream() throws Exception {
		int start = 1;
		int end = 31;
		int windowSize = 2;
		int windowShiftSize = 1;

		HashSet<String> stopwords = Utils.importStopwords();
		// ArrayList<DocumentCluster> eventsStream = new
		// ArrayList<DocumentCluster>();

		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		System.out.println(Arrays.toString(getDateList(start, windowSize)));
		new BlogDataLoader(constants).loadDocumentKeyFilesByDate(getDateList(start, windowSize), docs, stopwords, DF);
		DocumentAnalyze documentAnalyzer = new DocumentAnalyze(constants);
		ArrayList<DocumentCluster> lastEvents = documentAnalyzer.clusterbyKeyGraph(docs, DF);

		PrintStream out = new PrintStream(new File("events.txt"));

		for (int startDay = start + windowShiftSize; startDay <= end - windowSize; startDay += windowShiftSize) {
			DF = new HashMap<String, Double>();
			docs = new HashMap<String, Document>();
			out.println();
			out.println(Arrays.toString(getDateList(startDay, windowSize)));
			System.out.println(Arrays.toString(getDateList(startDay, windowSize)));
			new BlogDataLoader(constants).loadDocumentKeyFilesByDate(getDateList(startDay, windowSize), docs, stopwords, DF);
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
					// eventsStream.add(dc1);
					// --- Save and Print
//					DocumentAnalyze.printCluster(dc1, out);
					dc1.serialize(out,false);
					dc1.docs = null;
				}
			}
			lastEvents = events;
			events = null;
		}
		for (DocumentCluster dc1 : lastEvents) {
			// eventsStream.add(dc1);
			// --- Save and Print
			out.println();
//			DocumentAnalyze.printCluster(dc1, out);
			dc1.serialize(out,false);
			dc1.docs = null;
		}
		System.out.println("done");
	}

	public static String[] getDateList(int start, int size) {
		String[] dates = new String[size];
		for (int i = 0; i < size; i++)
			if (i + start < 10)
				dates[i] = "2008-08-0" + (i + start);
			else
				dates[i] = "2008-08-" + (i + start);

		return dates;
	}

	public static void findEvents() throws Exception {
		HashSet<String> stopwords = Utils.importStopwords();
		// Document.generateDataFiles(5, .2);
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		// ArrayList<Event> events=new ArrayList<Event>();
		// Document.fetchDocumentKeyFiles(docs,stopwords, DF);
		new BlogDataLoader(constants).loadDocumentKeyFilesByDate(getDateList(1, 4), docs, stopwords, DF);
		// Document.fetchDocumentKeyFilesByTopic(events,docs,stopwords, DF);
		// int pairs=Document.fetchDocumentTextFilesByTopic(docs,stopwords, DF);
		// GraphAnalyze g = new GraphAnalyze();
		// g.buildGraph(docs,DF);
		// g.printGraph(g.extractCommunities(g.nodes));

		// EvaluationAnalyze.evaluate(DocumentAnalyze.cluster(docs, DF), pairs);
		// EvaluationAnalyze.evaluate(events,DocumentAnalyze.cluster(docs, DF));
		new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
		System.out.println("done");
	}

	public static void runTimeEvaluationKeyGraph() throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter("runtime-keyGraph2.txt"));

		double toMins = 1000 * 60;
		for (int i = 13; i < 30; i++) {
			HashSet<String> stopwords = Utils.importStopwords();

			long time1 = System.currentTimeMillis();

			HashMap<String, Double> DF = new HashMap<String, Double>();
			HashMap<String, Document> docs = new HashMap<String, Document>();
			new BlogDataLoader(constants).loadDocumentKeyFilesByDate(getDateList(1, i), docs, stopwords, DF);
			long time2 = System.currentTimeMillis();

			new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
			long time3 = System.currentTimeMillis();
			out.write(i + "\t" + docs.size() + "\t" + (time3 - time1) / toMins + "\t" + (time2 - time1) / toMins + "\t"
					+ (time3 - time2) / toMins + "\n");
			out.flush();
		}
		System.out.println("done");
		out.close();
	}

	public static void runTimeEvaluationLDA() throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter("runtime-LDA2.txt"));

		double toMins = 1000 * 60;
		for (int i = 17; i < 30; i++) {
			HashSet<String> stopwords = Utils.importStopwords();

			HashMap<String, Double> DF = new HashMap<String, Double>();
			HashMap<String, Document> docs = new HashMap<String, Document>();
			new BlogDataLoader(constants).loadDocumentKeyFilesByDate(getDateList(1, i), docs, stopwords, DF);

			prepareDataforLDAMallet(docs);
			System.out.println("starting mallet!");
			long time1 = System.currentTimeMillis();
			runCommand("/fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/bin/mallet import-dir --input /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3rdata/ --output /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3r.NENP.mallet --keep-sequence");
			long time2 = System.currentTimeMillis();

			// new DocumentAnalyze(constants).clusterKeyGraph(docs, DF);
			// System.out.println("/fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/bin/mallet train-topics --input /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3r.NENP.mallet  --num-topics "+docs.size()/200+" --output-doc-topics results-mallet.txt");
			runCommand("/fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/bin/mallet train-topics --input /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3r.NENP.mallet  --num-topics "
					+ docs.size() / 200 + " --output-doc-topics results-mallet.txt");
			// new DocumentAnalyze(constants).clusterLDA(docs, DF,"mallet");

			long time3 = System.currentTimeMillis();
			out.write(i + "\t" + docs.size() + "\t" + (time3 - time1) / toMins + "\t" + (time2 - time1) / toMins + "\t"
					+ (time3 - time2) / toMins + "\n");
			out.flush();
		}
		System.out.println("done");
		out.close();
	}

	public static void prepareDataforLDAMallet(HashMap<String, Document> docs) throws Exception {
		// Runtime.getRuntime().exec("rm -rdf /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/tdtdata_NENP/");
		// Runtime.getRuntime().exec("mkdir /fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/tdtdata_NENP/");
		for (Document d : docs.values()) {
			String filename = "/fs/clip-clip-proj/GeoNets/hassan/source_codes/mallet-2.0.5/spinn3rdata/" + d.id + ".txt";
			if (new File(filename).exists())
				continue;
			BufferedWriter dataFile = new BufferedWriter(new FileWriter(filename));
			// dataFile.write(d.id+".txt eng");
			for (Keyword k : d.keywords.values())
				for (int z = 0; z < k.tf; z++)
					dataFile.write(" " + k.baseForm);
			dataFile.write("\n");
			dataFile.close();
		}

		// dataFile.close();
	}

	public static void runCommand(String command) throws Exception {
		System.out.println(command);
		Process run = Runtime.getRuntime().exec(command);
		DataInputStream tmp = new DataInputStream(run.getInputStream());
		String line = null;
		while ((line = tmp.readLine()) != null)
			System.out.println(line);

		tmp.close();
		System.out.println("command done!");
	}

	public static void retrieveOriginakKeywords() throws Exception {
		// String
		// eventFile="/fs/clip-clip-proj/GeoNets/hassan/source_codes/eventDetectionSpinn3r/eventsStream.filtered.txt";
		// String postDir="/fs/clip-geonets2/post/";
		Porter porter = new Porter();
		HashSet<String> stopwords = Utils.importStopwords();

		HashMap<String, String> keys = new HashMap<String, String>();
		File f = new File(constants.DATA_KEYWORDS_1_PATH);
		int i = 0;
		loadAllKeywords(porter, stopwords, keys, f);

		f = new File(constants.DATA_KEYWORDS_2_PATH);
		i = 0;
		loadAllKeywords(porter, stopwords, keys, f);

		System.out.println("**************" + keys.size());
		i = 0;
		BufferedReader in = new BufferedReader(new FileReader(
				"/fs/clip-clip-proj/GeoNets/hassan/source_codes/eventDetectionTDT/events.mallet.txt"));
		PrintStream out = new PrintStream(
				"/fs/clip-clip-proj/GeoNets/hassan/source_codes/eventDetectionSpinn3r/events.mallet.refined.txt");

		DocumentCluster dc = new DocumentCluster();
		while (dc.deserialize(in,false)) {
			for (Node n : dc.keyGraph.values()) {
				if (keys.get(n.keyword.baseForm) != null)
					n.keyword.setWord(keys.get(n.keyword.baseForm));
			}

			dc.serialize(out,false);
			if (i++ % 10000 == 0)
				System.out.println(i);
		}
		in.close();
		out.close();

	}

	private static void loadAllKeywords(Porter porter, HashSet<String> stopwords, HashMap<String, String> keys, File f)
			throws FileNotFoundException, IOException {
		String[] keyfiles = f.list();
		BufferedReader in = null;
		int i = 0;
		for (String filename : keyfiles) {
			BufferedReader in2 = new BufferedReader(new FileReader(f.getAbsolutePath() + "/" + filename));
			String line = null;
			while ((line = in2.readLine()) != null && line.length() > 2) {
				int index = line.lastIndexOf("==");
				// String token = line.substring(0, index).trim();
				String word = line.substring(0, index).trim();
				String base = "";
				StringTokenizer stt = new StringTokenizer(word, "!' -_@.");
				while (stt.hasMoreTokens()) {
					String token = stt.nextToken().toLowerCase();
					if ((token.indexOf("?") == -1 && token.length() > 2 && !stopwords.contains(token)))
						base += porter.stripAffixes(token) + " ";
				}
				base = base.trim();
				keys.put(base, word);
			}
			in2.close();
			if (i++ % 10000 == 0)
				System.out.println(i);
		}
	}

}
