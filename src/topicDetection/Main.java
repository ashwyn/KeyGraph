package topicDetection;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Main {

	public static void main(String[] args) throws Exception {
		Constants constants = new Constants(args[2]);
		PrintStream out = new PrintStream(args[1]);

		double toMins = 1000 * 60;
		HashSet<String> stopwords = Utils.importStopwords();

		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		long time1 = System.currentTimeMillis();
		Porter porter = new Porter();
		System.out.println("Loading Documents...");
		new DataLoader(constants).loadDocuments(args[0], docs, stopwords, DF, porter,constants.REMOVE_DUPLICATES);
		long time2 = System.currentTimeMillis();
		System.out.println(docs.size() + " documents are loaded (after filtering)!");
		ArrayList<DocumentCluster> clusters = new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF);
		DocumentAnalyze.printTopics(clusters, out);
		out.close();

		long time3 = System.currentTimeMillis();
		System.out.println(docs.size() + "\t" + (time3 - time1) / toMins + "\t" + (time2 - time1) / toMins + "\t" + (time3 - time2)
			/ toMins + "\n");
		System.out.println("DONE!");

	}

}
