package dataset.twitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import java.io.*;
import java.util.*;

import topicDetection.Constants;
import topicDetection.DataLoader;
import topicDetection.Document;
import topicDetection.Keyword;
import topicDetection.Porter;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.process.*;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class TwitterDataLoader extends DataLoader {
	public TwitterDataLoader(Constants constants) {
		super(constants);
	}

	public void fetchTweets4website(String datasetFile, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			boolean removeDuplicates) throws Exception {
		String line = null;
		BufferedReader in = new BufferedReader(new FileReader(datasetFile));
		int i = 0;
		StringDuplicate sd = new StringDuplicate();

		while ((line = in.readLine()) != null) {
			i++;
			if (i % 1000 == 0)
				System.out.println(i);
			String[] tokens = line.split("\t");
			Document d = new Document(Integer.parseInt(tokens[0]) + "");
			String tweet = tokens[1];
			fetchTweetContent(tweet, stopwords, porter, d);

			if (constants.REMOVE_DUPLICATES)
				d.isDuplicate = sd.isDuplicate(tweet);

			if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
				docs.put(d.id, d);
				for (Keyword k : d.keywords.values()) {
					if (DF.containsKey(k.baseForm))
						DF.put(k.baseForm, DF.get(k.baseForm) + 1);
					else
						DF.put(k.baseForm, new Double(1));
				}
			}
		}
	}

	public void fetchTweets(String datasetFile, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter)
			throws Exception {
		String line = null;
		BufferedReader in = new BufferedReader(new FileReader(datasetFile));
		int i = 0;
		while ((line = in.readLine()) != null)
			try {
				i++;
				if (i % 1000 == 0)
					System.out.println(i);
				if (i == 150000)
					break;
				String[] tokens = line.split("\t");
				// Document d = new Document(i + "");
				Document d = new Document(tokens[3]);
				String tweet = tokens[4];
				// String[] datetokens = tokens[1].split(" ");
				// String date = datetokens[1] + " " + datetokens[2] + ", " +
				// datetokens[5] + " " + datetokens[3];
				// d.publishDate = new Timestamp(Timestamp.parse(date));
				d.id = d.id + "\t" + tokens[2];
				d.setBody(tweet);
				// if(tweet.indexOf("RT @")!=-1 || tweet.indexOf("rt @")!=-1 ||
				// tweet.indexOf("RT@")!=-1 || tweet.indexOf("rt@")!=-1)
				// continue;
				fetchTweetContent(tweet, stopwords, porter, d);

				if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
					docs.put(d.id, d);
					for (Keyword k : d.keywords.values()) {
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(i + ": \"" + line + "\"");
			}
	}

	public void fetchTweets(String datasetFile, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			Timestamp start, Timestamp end) throws Exception {
		String line = null;
		BufferedReader in = new BufferedReader(new FileReader(datasetFile));
		int i = 0;
		while ((line = in.readLine()) != null)
			try {
				i++;
				if (i % 10000 == 0)
					System.out.println(i + "->" + docs.size());
				String[] tokens = line.split("\t");
				// Document d = new Document(i + "");
				Document d = new Document(tokens[0]);
				String tweet = tokens[4];
				String date = tokens[1];
				// String[] datetokens = tokens[1].split(" ");
				// String date = datetokens[1] + " " + datetokens[2] + ", " +
				// datetokens[5] + " " + datetokens[3];
				d.publishDate = Timestamp.valueOf(date);
				if (d.publishDate.compareTo(start) < 0)
					continue;
				if (d.publishDate.compareTo(end) > -1)
					break;
				// d.id = d.id + "\t" + tokens[2];
				d.setBody(tweet);
				// if(tweet.indexOf("RT @")!=-1 || tweet.indexOf("rt @")!=-1 ||
				// tweet.indexOf("RT@")!=-1 || tweet.indexOf("rt@")!=-1)
				// continue;
				fetchTweetContent(tweet, stopwords, porter, d);

				if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
					docs.put(d.id, d);
					for (Keyword k : d.keywords.values()) {
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(i + ": \"" + line + "\"");
			}
	}

	public void fetchTweetsByQuery(String datasetFile, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			String query, Timestamp startDate, Timestamp endDate) throws Exception {
		String line = null;
		BufferedReader in = new BufferedReader(new FileReader(datasetFile));
		int i = 0;
		while ((line = in.readLine()) != null)
			try {
				i++;
				if (i % 100000 == 0)
					System.out.println(i);
				Document d = new Document(i + "");
				String[] tokens = line.split("\t");
				if (tokens.length < 3)
					continue;
				String tweet = tokens[2];
				String[] datetokens = tokens[0].split(" ");
				String date = datetokens[1] + " " + datetokens[2] + ", " + datetokens[5] + " " + datetokens[3];
				d.publishDate = new Timestamp(Timestamp.parse(date));
				d.id = d.id + "\t" + tokens[1];
				if (startDate.before(d.publishDate) && endDate.after(d.publishDate) && containsQuery(tweet, query)) {
					fetchTweetContent(tweet, stopwords, porter, d);
					if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
						docs.put(d.id, d);
						for (Keyword k : d.keywords.values()) {
							if (DF.containsKey(k.baseForm))
								DF.put(k.baseForm, DF.get(k.baseForm) + 1);
							else
								DF.put(k.baseForm, new Double(1));
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

	}

	public void fetchTweetsByUsers(String datasetFile, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			HashSet<String> users, Timestamp startDate, Timestamp endDate) throws Exception {
		String line = null;
		BufferedReader in = new BufferedReader(new FileReader(datasetFile));
		int i = 0;
		while ((line = in.readLine()) != null) {
			i++;
			if (i % 100000 == 0)
				System.out.println(i + "->" + docs.size());
			Document d = new Document(i + "");
			String[] tokens = line.split("\t");
			if (tokens.length < 3)
				continue;
			String tweet = tokens[2];
			String userId = tokens[1];
			String[] datetokens = tokens[0].split(" ");
			String date = datetokens[1] + " " + datetokens[2] + ", " + datetokens[5] + " " + datetokens[3];
			d.publishDate = new Timestamp(Timestamp.parse(date));
			d.id = d.id + " " + userId;
			if (startDate.before(d.publishDate) && endDate.after(d.publishDate) && users.contains(userId)) {
				fetchTweetContent(tweet, stopwords, porter, d);
				if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
					docs.put(d.id, d);
					for (Keyword k : d.keywords.values()) {
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
					}
				}
			}
		}
	}

	public HashSet<String> getUsers(String datasetFile, String query, Timestamp startDate, Timestamp endDate) throws Exception {
		HashSet<String> users = new HashSet<String>();
		String line = null;
		BufferedReader in = new BufferedReader(new FileReader(datasetFile));
		int i = 0;
		while ((line = in.readLine()) != null) {
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
			if (startDate.before(publishDate) && endDate.after(publishDate) && containsQuery(tweet, query))
				users.add(tokens[1]);
		}
		return users;
	}

	public static boolean containsQuery(String text, String query) {
		text = text.toLowerCase();
		query = query.toLowerCase();
		String[] mustTokens = query.split("\\+");
		for (String mustToken : mustTokens) {
			boolean includes = false;
			String[] mightTokens = mustToken.split(" ");
			for (String mightToken : mightTokens)
				if (text.indexOf(mightToken.trim().replaceAll("_", " ")) != -1) {
					includes = true;
					break;
				}
			if (!includes)
				return false;
		}
		return true;
	}

	public void fetchTweetContent(String content, HashSet<String> stopwords, Porter porter, Document d) {
		if (constants.KEYWORDS_2_ENABLE)
			loadDocumentKeyFile(getNounPhrases(content), stopwords, porter, d, constants.KEYWORDS_2_WEIGHT);
		if (constants.TEXT_ENABLE)
			fetchTweetText(content, stopwords, porter, d, constants.TEXT_WEIGHT);
		ArrayList<String> toRemove = new ArrayList<String>();
		for (Keyword k : d.keywords.values())
			if (k.word.contains("@"))
				toRemove.add(k.baseForm);
		for (String base : toRemove)
			d.keywords.remove(base);

	}

	public void fetchTweetText(String content, HashSet<String> stopwords, Porter porter, Document d, double BoostRate) {
		content = content.replaceAll("[hH][tT][tT][pP][s]?:[\\\\/][\\\\/][^ ]*\\b", " ");
		StringTokenizer st = new StringTokenizer(content, "!?|\"' -_0123456789.,;#$&%/\\*()<>\t");
		d.setBody(content);
		while (st.hasMoreTokens()) {
			String word = st.nextToken();
			String token = word.toLowerCase();
			double tf = 1 * BoostRate;
			String base = "";
			if ((token.indexOf("?") == -1 && token.length() > 2 && !stopwords.contains(token)))
				base = porter.stripAffixes(token);

			if (base.length() > 2)
				if (!d.keywords.containsKey(base))
					d.keywords.put(base, new Keyword(base, word, tf, 1, 0));
				else
					d.keywords.get(base).tf += tf;

		}
	}

	public static ArrayList<String> getBigrams(String content) {
		ArrayList<String> res = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(content, "!?|\"' -_@0123456789.,;#$&%/\\*()<>\t");
		String prev = null;
		while (st.hasMoreTokens()) {
			String term = st.nextToken();
			if (term.length() > 1) {
				if (prev != null)
					res.add(prev + " " + term);
				prev = term;
			}
		}
		return res;
	}

	static LexicalizedParser lp = null;
	static TokenizerFactory tf = null;

	// TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
	public static ArrayList<String> getNounPhrases(String inputContent) {
		if (tf == null)
			tf = PTBTokenizer.factory(false, new WordTokenFactory());
		if (lp == null)
			lp = new LexicalizedParser("englishPCFG.ser.gz");
		ArrayList<String> res = new ArrayList<String>();

		String content = inputContent.replaceAll("[hH][tT][tT][pP][s]?:[\\\\/][\\\\/][^ ]*\\b", " ");
		try {
			StringTokenizer sentences = new StringTokenizer(content, "[!\\?;.]");
			while (sentences.hasMoreElements()) {
				String sentence = sentences.nextToken();
				if (sentence.trim().length() == 0)
					continue;
				// System.out.println("ORIGINAL:" + sentence);
				List tokens = tf.getTokenizer(new StringReader(sentence)).tokenize();
				lp.parse(tokens); // parse the tokens
				Tree t = lp.getBestParse(); // get the best parse tree
				res.addAll(getNounPhrases(t));
				// System.out.println(res.size());
				// System.out.println("\nPROCESSED:\n\n"); tp.printTree(t);
				// //
				// print tree
			}
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
		}
		return res;
	}

	public static ArrayList<String> getNounPhrases(Tree t) {
		ArrayList<String> res = new ArrayList<String>();
		if (!t.isLeaf())
			for (Tree child : t.getChildrenAsList()) {
				ArrayList<String> childRes = getNounPhrases(child);
				if (childRes.size() > 0)
					res.addAll(childRes);
			}
		if (res.size() == 0)
			if (t.label().value().equals("NP")) {
				String text = "";
				for (Tree tt : t.getLeaves())
					text += " " + tt.value();
				res.add(text.trim());
			}
		return res;
	}

}
