package topicDetection;

import java.util.HashMap;

public class Topic {
	public String id;
	public HashMap<String, Document> docs = new HashMap<String, Document>();
	public DocumentCluster matchingDC;
	public double matchingDCScore;

	public Topic(String id) {
		this.id = id;
	}
}
