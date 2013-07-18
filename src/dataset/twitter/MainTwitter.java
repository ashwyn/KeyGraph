package dataset.twitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import topicDetection.Constants;
import topicDetection.Document;
import topicDetection.DocumentAnalyze;
import topicDetection.DocumentCluster;
import topicDetection.GraphAnalyze;
import topicDetection.Porter;
import topicDetection.Utils;

// Referenced classes of package dataset.twitter:
//            TwitterUtil

public class MainTwitter {

	public static String host = null;
	public static Constants constants;

	public MainTwitter() {
	}

	public static void main(String args[]) throws Exception {
		// String in="RT @localnatives: A friend of ours found this inside an elliot smith book in a bookstore in Brooklyn  http://plixi.com/p/50287064";
		// System.out.println(in.replaceAll("[hH][tT][tT][pP][s]?:[\\\\/][\\\\/][^ ]*\\b", " "));
		// System.exit(0);
		constants = new Constants(new BufferedReader(new InputStreamReader(new FileInputStream("conf/TwitterConstants.txt"))));
		// createDataset("/fs/clip-clip-proj/SMSite2010/BB(r)_v2/Data1/Ego_dataset_1/Data/TweetsUserContent.txt",
		// //
		// createDataset("/fs/clip-clip-proj/SMSite2010/BB(r)_v2/Data1/Ego_dataset_2/Data/protocol2_backgroundTweets.txt",
		// "local+natives", Timestamp.valueOf("2000-01-01 12:01:01"),
		// Timestamp.valueOf("2020-01-01 12:01:01"),
		// "localNatives.tweets1.txt");

		// runSlidingWindow("usdebt2011_firstWeek.txt",
		// "usdebt2011_firstWeek.txt_events");
		// System.exit(0);
		// args=new String[]{"run4website",
		// "summerSearch-sxsw.txt","summerSearch-sxsw.txt.out"};
		String usage = "Usage:\n\t1. run inputDatasetFile\n" + "\t2. run4website inputDatasetFile outputFile\n"
				+ "\t3. runQuery inputDatasetFile query StartDate(yyyy-mm-dd) endDateDate(yyyy-mm-dd) outputFile\n"
				+ "\t4. runQueryStream inputDatasetFile query StartDate(yyyy-mm-dd) endDateDate(yyyy-mm-dd) outputFile\n";
		try {

			if (args[0].equals("run4website")) {
				run4website(args[1], args[2]);
			} else if (args[0].equals("run")) {
				run(args[1], args[2]);
			} else if (args[0].equals("runQuery")) {
				runQuery(args[1], args[2].replaceAll("\"", " ").trim(), Timestamp.valueOf(args[3] + " 12:00:00"), Timestamp.valueOf(args[4] + " 12:00:00"),
						args[4]);
			} else if (args[0].equals("runQueryStream")) {
				runQueryStreamInFiles(args[1], args[2].replaceAll("\"", " ").trim(), Timestamp.valueOf(args[3] + " 12:00:00"),
						Timestamp.valueOf(args[4] + " 12:00:00"), args[5]);
			} else
				System.out.println(usage);
		} catch (Exception e) {
			System.out.println(usage);
			e.printStackTrace();
		}
	}

	public static void createDataset(String inputFile, String query, Timestamp startDate, Timestamp endDate, String outputFile) throws Exception {
		String line = null;
		BufferedReader in = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(inputFile)));
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
		int i = 0;
		while ((line = in.readLine()) != null)
			try {
				i++;
				if (i % 100000 == 0)
					System.out.println(i);
				String[] tokens = line.split("\t");
				if (tokens.length < 3)
					continue;
				String tweet = tokens[2];
				String[] datetokens = tokens[0].split(" ");
				String date = datetokens[1] + " " + datetokens[2] + ", " + datetokens[5] + " " + datetokens[3];
				Timestamp publishDate = new Timestamp(Timestamp.parse(date));
				if (startDate.before(publishDate) && endDate.after(publishDate) && TwitterDataLoader.containsQuery(tweet, query)) {
					out.write(line + "\n");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		out.close();

	}

	public static void run4website(String inputFile, String outputFile) throws Exception {
		// PrintStream out = new PrintStream(new File(outputFile));
		HashSet<String> stopwords = Utils.importStopwords();
		Porter porter = new Porter();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();

		
		(new TwitterDataLoader(constants)).fetchTweets4website(inputFile, docs, stopwords, DF, porter,constants.REMOVE_DUPLICATES);

		System.out.println("#docs: " + docs.size());
		ArrayList<DocumentCluster> clusters = (new DocumentAnalyze(constants)).clusterbyKeyGraph(docs, DF);
		DocumentAnalyze.printClustersForTheWebsite(clusters, outputFile);
		// out.close();
	}

	public static void run(String inputFile, String outputFile) throws Exception {
		runQuery(inputFile, null, null, null, outputFile);
	}

	public static void runQuery(String inputFile, String query, Timestamp startDate, Timestamp endDate, String outputFile) throws Exception {
		PrintStream out = new PrintStream(new File(outputFile));
		HashSet<String> stopwords = Utils.importStopwords();
		Porter porter = new Porter();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		if (query == null)
			(new TwitterDataLoader(constants)).fetchTweets(inputFile, docs, stopwords, DF, porter);
		else
			(new TwitterDataLoader(constants)).fetchTweetsByQuery(inputFile, docs, stopwords, DF, porter, query, startDate, endDate);

		System.out.println("#docs: " + docs.size());
		ArrayList<DocumentCluster> clusters = (new DocumentAnalyze(constants)).clusterbyKeyGraph(docs, DF);
		// DocumentAnalyze.printClusters(clusters, out, true);
		DocumentCluster.serializeAll(clusters, out, true);
		out.close();
	}

	public static void runSlidingWindow(String inputFile, String outputFile) throws Exception {
		Timestamp start = Timestamp.valueOf("2011-06-25 18:00:00");
		Timestamp end = Timestamp.valueOf("2011-07-29 11:00:00");
		int windowShiftSize = 5;

		HashSet<String> stopwords = Utils.importStopwords();
		Porter porter = new Porter();

		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		Timestamp windowStartTime = start;
		Timestamp windowEndTime = new Timestamp(windowStartTime.getTime() + windowShiftSize * 2 * 24 * 60 * 60 * 1000);
		(new TwitterDataLoader(constants)).fetchTweets(inputFile, docs, stopwords, DF, porter, windowStartTime, windowEndTime);
		DocumentAnalyze documentAnalyze = new DocumentAnalyze(constants);
		ArrayList<DocumentCluster> lastEvents = documentAnalyze.clusterbyKeyGraph(docs, DF);

		PrintStream out = new PrintStream(new File(outputFile + ".tmp"));
		for (Document d : docs.values()) {
			d.publishDate = null;
			d.setBody(null);
			d.setTitle(null);
		}

		while (windowEndTime.before(end)) {
			DF = new HashMap<String, Double>();
			docs = new HashMap<String, Document>();
			out.println();
			windowStartTime = new Timestamp(windowStartTime.getTime() + windowShiftSize * 24 * 60 * 60 * 1000);
			windowEndTime = new Timestamp(windowEndTime.getTime() + windowShiftSize * 24 * 60 * 60 * 1000);
			System.out.println(windowStartTime + "\t" + windowEndTime);
			(new TwitterDataLoader(constants)).fetchTweets(inputFile, docs, stopwords, DF, porter, windowStartTime, windowEndTime);
			System.out.println(docs.size() + " docs are loaded!");
			ArrayList<DocumentCluster> events = documentAnalyze.clusterbyKeyGraph(docs, DF);
			for (Document d : docs.values()) {
				d.publishDate = null;
				d.setBody(null);
				d.setTitle(null);
			}
			// ----------- merge events -----------
			for (DocumentCluster dc1 : lastEvents) {
				boolean isMerged = false;
				for (DocumentCluster dc2 : events)
					if (documentAnalyze.intersectDocs(dc1.docs, dc2.docs) > 3) {
						isMerged = true;
						dc2.docs.putAll(dc1.docs);
						dc2.keyGraph.putAll(dc2.keyGraph);
					}
				if (!isMerged) {
					// eventsStream.add(dc1);
					// --- Save and Print
					// DocumentAnalyze.printCluster(dc1, out, true);
					dc1.serialize(out, true);
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
			// DocumentAnalyze.printCluster(dc1, out, true);
			dc1.serialize(out, true);

			dc1.docs = null;
		}

		out.close();
		ArrayList<DocumentCluster> allEvents = DocumentCluster.deserializeAll(outputFile + ".tmp",true);
		DocumentAnalyze.printClustersForTheWebsite(allEvents, outputFile);
		System.out.println("done");
	}

	public static void runQueryStream(String inputFile, String query, Timestamp startDate, Timestamp endDate, String outputFile) throws Exception {

		PrintStream out = new PrintStream(new File(outputFile));
		long windowSize = (long) 2 * 30 * 24 * 60 * 60 * 1000;
		long windowShiftSize = windowSize / 2;

		Timestamp start = startDate;
		Timestamp end = new Timestamp(start.getTime() + windowSize);

		HashSet<String> stopwords = Utils.importStopwords();
		Porter porter = new Porter();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();

		HashSet<String> users = new TwitterDataLoader(constants).getUsers(inputFile, query, startDate, endDate);
		System.out.println(users.size() + " users");

		DocumentAnalyze documentAnalyzer = new DocumentAnalyze(constants);
		ArrayList<DocumentCluster> lastEvents = documentAnalyzer.clusterbyKeyGraph(docs, DF);

		start = new Timestamp(start.getTime() - windowShiftSize);
		while (start.before(endDate)) {
			start = new Timestamp(start.getTime() + windowShiftSize);
			end = new Timestamp(start.getTime() + windowSize);

			DF = new HashMap<String, Double>();
			docs = new HashMap<String, Document>();
			out.println();
			out.println("[" + start + " , " + end + "]");
			System.out.println("[" + start + " , " + end + "]");

			(new TwitterDataLoader(constants)).fetchTweetsByUsers(inputFile, docs, stopwords, DF, porter, users, start, end);
			out.println(docs.size() + " docs are loaded!");
			System.out.println("#docs: " + docs.size());
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
					dc1.serialize(out, true);
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
			dc1.serialize(out, true);
			dc1.docs = null;
		}
		System.out.println("done");
	}

	public static void runQueryStreamInFiles(String inputFile, String query, Timestamp startDate, Timestamp endDate, String outputFile) throws Exception {
		long windowSize = (long) 6 * 24 * 60 * 60 * 1000;
		String dirname = "tweetEvents/" + query.replaceAll("[+_]", "") + "/";
		new File(dirname).mkdir();
		findEventChunks(inputFile, query, startDate, endDate, dirname, windowSize);
		findEventStreamFromChunks(dirname, query, startDate, endDate, outputFile, windowSize);
	}

	public static void findEventChunks(String inputDir, String query, Timestamp startDate, Timestamp endDate, String outputDir, long windowSize)
			throws Exception {
		long windowShiftSize = windowSize / 2;

		Timestamp start = startDate;
		Timestamp end = new Timestamp(start.getTime() + windowSize);

		HashSet<String> stopwords = Utils.importStopwords();
		Porter porter = new Porter();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();

		HashSet<String> users = new TwitterDataLoader(constants).getUsers(inputDir, query, startDate, endDate);
		System.out.println(users.size() + " users");

		DocumentAnalyze documentAnalyzer = new DocumentAnalyze(constants);

		start = new Timestamp(start.getTime() - windowShiftSize);
		while (start.before(endDate)) {

			start = new Timestamp(start.getTime() + windowShiftSize);
			end = new Timestamp(start.getTime() + windowSize);

			PrintStream eout = new PrintStream(new File(outputDir + "/" + start.toString().split(" ")[0] + "_" + end.toString().split(" ")[0] + ".txt"));
			DF = new HashMap<String, Double>();
			docs = new HashMap<String, Document>();
			eout.println();
			eout.println("[" + start + " , " + end + "]");
			System.out.println("[" + start + " , " + end + "]");

			(new TwitterDataLoader(constants)).fetchTweetsByUsers(inputDir, docs, stopwords, DF, porter, users, start, end);
			eout.println(docs.size() + " docs are loaded!");
			System.out.println("#docs: " + docs.size());
			ArrayList<DocumentCluster> events = documentAnalyzer.clusterbyKeyGraph(docs, DF);

			for (DocumentCluster dc1 : events) {
				// eventsStream.add(dc1);
				// --- Save and Print

//				DocumentAnalyze.printCluster(dc1, eout, true);
				dc1.serialize(eout, true);
				dc1.docs = null;
			}
			eout.close();
		}

	}

	public static void findEventStreamFromChunks(String inputDir, String query, Timestamp startDate, Timestamp endDate, String outputFile, long windowSize)
			throws Exception {
		PrintStream out = new PrintStream(new File(outputFile));
		long windowShiftSize = windowSize / 2;
		DocumentAnalyze documentAnalyzer = new DocumentAnalyze(constants);

		System.out.println("[" + startDate + " , " + endDate + "]");
		Timestamp start = startDate;
		Timestamp end = new Timestamp(start.getTime() + windowSize);
		String ein = inputDir + "/" + start.toString().split(" ")[0] + "_" + end.toString().split(" ")[0] + ".txt";
		ArrayList<DocumentCluster> lastingEvents = DocumentCluster.deserializeAll(ein,true);
		while (start.before(endDate)) {
			start = new Timestamp(start.getTime() + windowShiftSize);
			end = new Timestamp(start.getTime() + windowSize);

			ein = inputDir + start.toString().split(" ")[0] + "_" + end.toString().split(" ")[0] + ".txt";
			System.out.println("[" + start + " , " + end + "]");

			ArrayList<DocumentCluster> events = DocumentCluster.deserializeAll(ein,true);

			// ----------- merge events -----------
			for (DocumentCluster dc1 : lastingEvents) {
				boolean isMerged = false;
				for (DocumentCluster dc2 : events) {
					if (dc1.docs.size() > 0 && dc2.docs.size() > 0)
						if (documentAnalyzer.intersectDocs(dc1.docs, dc2.docs) * 1.0 / Math.min(dc1.docs.size(), dc2.docs.size()) > .2) {
							isMerged = true;
							dc2.docs.putAll(dc1.docs);
							dc2.keyGraph = GraphAnalyze.mergeKeyGraphs(dc1.keyGraph, dc2.keyGraph);
						}
				}
				if (!isMerged) {
					// --- Save and Print
					if (dc1.docs.size() > 20 && isRelated(dc1, query))
//						DocumentAnalyze.printCluster(dc1, out, true);
						dc1.serialize(out, true);
					dc1.docs = null;
				}
			}
			lastingEvents = events;
			events = null;
		}
		for (DocumentCluster dc1 : lastingEvents)
			if (isRelated(dc1, query)) {
				// --- Save and Print
				out.println();
//				DocumentAnalyze.printCluster(dc1, out, true);
				dc1.serialize(out, true);
				dc1.docs = null;
			}
		System.out.println("done");
	}

	public static boolean isRelated(DocumentCluster dc, String query) {
		for (Document d : dc.docs.values())
			if (TwitterDataLoader.containsQuery(d.getBody(), query))
				return true;
		return false;
	}

}
