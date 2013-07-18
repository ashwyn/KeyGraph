package topicDetection;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;

public class Document {
	public boolean isDuplicate=false;
	private String title, body;
	public HashSet<String> topics = new HashSet<String>();
	public String id;
	public Timestamp publishDate = null;
	public double vectorSize;
	public HashMap<String, Keyword> keywords = new HashMap<String, Keyword>();

	public Document(String id) {
		this.id = id;
		// TODO: compute vectorSize
	}

	public Document(String id, String title, String body, HashMap<String, Keyword> keywords) {
		this.id = id;
//		this.title = title;
//		this.body = body;
		this.keywords = keywords;
		// vectorSize = 0;
		// for (Keyword k : keywords.values())
		// vectorSize += k.tf * k.tf;
		// vectorSize = Math.sqrt(vectorSize);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
//		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public HashSet<String> getTopics() {
		return topics;
	}

	public void setTopics(HashSet<String> topics) {
		this.topics = topics;
	}

	public Timestamp getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Timestamp publishDate) {
		this.publishDate = publishDate;
	}

	

}
