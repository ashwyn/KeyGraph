package topicDetection;

import java.util.HashMap;

public class Keyword {
	public String baseForm;
	public String word;
	public double tf;
	public double df;
	// double idf;
	// double tfidf;
	public HashMap<String, Document> documents = new HashMap<String, Document>();

	public Keyword(String base, String word, double tf, double df, double idf
	// , double tfidf
	) {
		this.baseForm = base;
		setWord(word);
		this.tf = tf;
		this.df = df;
		// this.idf = idf;
		// this.tfidf = tfidf;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}
	

}
