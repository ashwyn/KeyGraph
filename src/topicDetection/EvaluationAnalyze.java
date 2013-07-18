package topicDetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class EvaluationAnalyze {

	// public static void evaluate(ArrayList<DocumentCluster> documentClusters,
	// int groundTruthPairs) {
	// double N11 = 0;
	// int pairs = 0;
	// for (DocumentCluster dc : documentClusters) {
	// pairs += dc.docs.size() * (dc.docs.size() - 1) / 2;
	// for (Document d1 : dc.docs.values())
	// for (Document d2 : dc.docs.values())
	// if (!d2.equals(d1) && d2.topic.equals(d1.topic))
	// N11++;
	// }
	//
	// N11 /= 2;
	// System.out.println("Precision: " + N11 / pairs);
	// System.out.println("Recall: " + N11 / groundTruthPairs);
	// System.out.println("F: "
	// + Math.sqrt(N11 / pairs * N11 / groundTruthPairs));
	// }

	public static double[] evaluate(ArrayList<Topic> events, ArrayList<DocumentCluster> documentClusters) {
		events = matchEventsAndClusters(events, documentClusters);

		double groundTruthSize = 0;
		double N11 = 0;
		double mic_F1=0;
		int documentClustersSize = 0;
		for (Topic e : events) {
			groundTruthSize += e.docs.size();
			if (e.matchingDC != null) {
				documentClustersSize += e.matchingDC.docs.size();
				// N11 += intersectTopics(e.id, e.matchingDC.docs);
				double n11 = intersectTopics(e, e.matchingDC.docs);
				double p=n11/e.matchingDC.docs.size();
				double r=n11/e.docs.size();
				double f1=2*p*r/(p+r);
				System.out.println("***"+"\t"+e.id+"\t"+p+"\t"+r+"\t"+f1);
				N11 += n11;				
			}else
			System.out.println("****"+"\t"+e.id+"\t"+0+"\t"+0+"\t"+0);
		}
		double precision = N11 / documentClustersSize;
		System.out.println("Precision: " + precision);
		double recall = N11 / groundTruthSize;
		System.out.println("Recall: " + recall);
		double mac_F1 = Math.sqrt(precision * recall);
		System.out.println("Mac_F: " + mac_F1);
//		System.out.println("Mic_F: " + mic_F1);
		return new double[] { precision, recall, mac_F1,mic_F1 };
	}

	// public static void evaluateOLD(ArrayList<Event> events,
	// ArrayList<DocumentCluster> documentClusters) {
	// events = matchEventsAndClusters(events, documentClusters);
	//
	// double groundTruthPairs = 0;
	// double N11 = 0;
	// int pairs = 0;
	// for (Event e : events) {
	// groundTruthPairs += e.docs.size() * (e.docs.size() - 1) / 2;
	// if (e.matchingDC != null) {
	// pairs += e.matchingDC.docs.size() * (e.matchingDC.docs.size() - 1) / 2;
	// for (Document d1 : e.matchingDC.docs.values())
	// for (Document d2 : e.matchingDC.docs.values())
	// if (!d2.equals(d1) && intersectTopics(d2.topics, d1.topics))
	// N11++;
	// }
	// }
	//
	// N11 /= 2;
	// System.out.println("Precision: " + N11 / pairs);
	// System.out.println("Recall: " + N11 / groundTruthPairs);
	// System.out.println("F: " + Math.sqrt(N11 / pairs * N11 /
	// groundTruthPairs));
	// }

	public static ArrayList<Topic> matchEventsAndClusters(ArrayList<Topic> events, ArrayList<DocumentCluster> documentClusters) {
		boolean change = false;
		int i = 0;
		do {
			int j = 0;
			change = false;
			for (DocumentCluster dc : documentClusters) {
				for (Topic event : events)
					if (event.matchingDC == null) 
					{
						// double overlap = intersectTopics(event.id, dc.docs);
						double overlap = intersectTopics(event, dc.docs);
						double p=overlap/dc.docs.size();
						double r=overlap/event.docs.size();
						double matching = (p*r==0)?0:2*p*r/(p+r);
//						double matching = overlap / (dc.docs.size() + event.docs.size() - overlap);
						if(matching!=0)
						if (event.matchingDC == null || matching > event.matchingDCScore
								|| (matching == event.matchingDCScore && dc.docs.size() < event.matchingDC.docs.size())) {
							if (dc.MatchingEvent == null || dc.matchingEventScore < matching) {
								event.matchingDC = dc;
								event.matchingDCScore = matching;
								if (dc.MatchingEvent != null){
									dc.MatchingEvent.matchingDC = null;
									dc.MatchingEvent.matchingDCScore =0;
								}
								dc.MatchingEvent = event;
								dc.matchingEventScore = matching;
								change = true;
								j++;
							}
						}
					}
			}
			System.out.println("Evaluation iteration"+(i++) + " => " + j);
		} while (change);
		return events;
	}

	// public static int intersectTopics(String eventId, HashMap<String,
	// Document> docs) {
	// int intersect = 0;
	// for (Document d : docs.values())
	// if (d.topics.contains(eventId))
	// intersect++;
	// return intersect;
	// }

	public static int intersectTopics(Topic e, HashMap<String, Document> docs) {
		int intersect = 0;
		for (Document d : docs.values())
			if (e.docs.containsKey(d.id))
				intersect++;
		return intersect;
	}

	public static boolean intersectTopics(HashSet<String> topics1, HashSet<String> topics2) {
		for (String t1 : topics1)
			if (topics2.contains(t1))
				return true;
		return false;
	}

}
