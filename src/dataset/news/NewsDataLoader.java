package dataset.news;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import topicDetection.Constants;
import topicDetection.DataLoader;
import topicDetection.Document;
import topicDetection.Topic;
import topicDetection.Keyword;
import topicDetection.Porter;


public class NewsDataLoader extends DataLoader {

	public static double percent=1; 

	public NewsDataLoader(Constants constants) {
		super(constants);
	}

	public void loadDocumentKeyFilesAll(HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF) {

		try {
			Porter porter = new Porter();
			File f = new File(constants.DATA_KEYWORDS_1_PATH);
			String[] files = f.list();
			loadDocumentsForSpinn3r(files, docs, stopwords, DF, porter);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Number of documents=" + docs.size());

	}

	public void loadDocumentKeyFilesByTopics(ArrayList<Topic> events, HashMap<String, Document> docs, HashSet<String> stopwords,
			HashMap<String, Double> DF) {
		// HashMap<String, Document> docs = new HashMap<String, Document>();
		double avg = 0;
		DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmm");
		try {
			Porter porter = new Porter();
			
			String[] files = list(constants.DATA_TOPIC_PATH);
			for (int i = 0; i < files.length && events.size() < constants.TOPIC_MAX; i++) {
				// if(files[i].getName().startsWith("40039"))
				// continue;
				Topic e = loadDocumentKeyFilesByTopic(docs, stopwords, DF, avg, df, porter,openDataInputStream(constants.DATA_TOPIC_PATH+files[i]),
						files[i]);
				if (e.docs.size() >= constants.TOPIC_MIN_SIZE) {
					events.add(e);
					avg += e.docs.size();
					System.out.println(files[i] + ": " + e.docs.size());
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("AVG topic size=" + avg / events.size());
		System.out.println("Number of topics=" + events.size());
	}

	public void loadDocumentKeyFilesByTopics(ArrayList<Topic> events, HashMap<String, Document> docs, HashSet<String> stopwords,
			HashMap<String, Double> DF, Timestamp beginDate, Timestamp endDate) {
		// HashMap<String, Document> docs = new HashMap<String, Document>();
		double avg = 0;
		DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmm");
		try {
			Porter porter = new Porter();
			String[] files = list(constants.DATA_TOPIC_PATH);
			for (int i = 0; i < files.length; i++) {
				Topic e = loadDocumentKeyFilesByTopic(docs, stopwords, DF, avg, df, porter, files[i], beginDate, endDate);
				if (e.docs.size() >= constants.TOPIC_MIN_SIZE) {
					events.add(e);
					avg += e.docs.size();
					// System.out.println(files[i].getName() + ": " + j + "=> "
					// + e.docs.size());
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("AVG topic size=" + avg / events.size());
		System.out.println("Number of topics=" + events.size());
	}

	public Topic loadDocumentKeyFilesByTopic(HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, double avg,
			DateFormat df, Porter porter, DataInputStream reader, String eventId) throws Exception {
		Topic e = new Topic(eventId);

		// DataInputStream reader = new
		// DataInputStream(openDataInputStream(file));
		String line = null;
		int j = 0;
		while ((line = reader.readLine()) != null && line.trim().length() > 0) {
			if(Math.random()>percent)
				continue;
			j++;
			Document d = new Document(line);
			d.topics.add(e.id);
			try {
				d.publishDate = new Timestamp(df.parse(line.substring(3, line.lastIndexOf('.'))).getTime());
			} catch (Exception ee) {
			}
			if (constants.KEYWORDS_1_ENABLE) {
				String ff = (constants.DATA_KEYWORDS_1_PATH + line + ".txt");
				if (exists(ff))
					loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, constants.KEYWORDS_1_WEIGHT);
				else
					System.out.println(ff + " cannot be found!");
			}
			if (
			// d.keywords.size()<Constants.DOC_KEYWORDS_SIZE_MIN ||
			constants.KEYWORDS_2_ENABLE) {
				String ff = (constants.DATA_KEYWORDS_2_PATH + line + ".txt");
				if (exists(ff))
					loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, 1);
				else
					System.out.println(ff + " cannot be found!");
			}
			if (constants.TEXT_ENABLE) {
				String ff = (constants.DATA_TEXT_PATH + line + ".txt");
				if (exists(ff))
					loadDocumentTextFile(openDataInputStream(ff), stopwords, porter, d, constants.TEXT_WEIGHT,false,null);
				else
					System.out.println(ff + " cannot be found!");
			}
			
			if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
				e.docs.put(d.id, d);
			}
		}
		if (e.docs.size() >= constants.TOPIC_MIN_SIZE) {
			for (Document d : e.docs.values()) {
				if (!docs.containsKey(d.id)) {
					docs.put(d.id, d);
					for (Keyword k : d.keywords.values()) {
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
					}
				} else
					docs.get(d.id).topics.addAll(d.topics);
			}
		}
		return e;
	}

	private Topic loadDocumentKeyFilesByTopic(HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, double avg,
			DateFormat df, Porter porter, String file, Timestamp beginDate, Timestamp endDate) throws Exception {
		Topic e = new Topic(file);

		DataInputStream reader = new DataInputStream(openDataInputStream(constants.DATA_TOPIC_PATH+"/"+file));
		String line = null;
		int j = 0;
		while ((line = reader.readLine()) != null && line.trim().length() > 0) {
			j++;
			Document d = new Document(line);
			d.topics.add(e.id);
			try {
				d.publishDate = new Timestamp(df.parse(line.substring(3, line.lastIndexOf('.'))).getTime());
			} catch (Exception ee) {
			}
			if (d.publishDate.compareTo(beginDate) == -1 || d.publishDate.compareTo(endDate) == 1)
				continue;

			if (constants.KEYWORDS_1_ENABLE) {
				String ff = (constants.DATA_KEYWORDS_1_PATH + line + ".txt");
				loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, constants.KEYWORDS_1_WEIGHT);
			}
			if (constants.KEYWORDS_2_ENABLE) {
				String ff = (constants.DATA_KEYWORDS_2_PATH + line + ".txt");
				loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, 1);
			}
			if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
				e.docs.put(d.id, d);
			}
		}
		if (e.docs.size() >= constants.TOPIC_MIN_SIZE) {
			for (Document d : e.docs.values()) {
				if (!docs.containsKey(d.id)) {
					docs.put(d.id, d);
					for (Keyword k : d.keywords.values()) {
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
					}
				} else
					docs.get(d.id).topics.addAll(d.topics);
			}
		}
		return e;
	}

	public void correctXMLChars(String inDirPath, String outDirPath) throws Exception {
		// File("/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/tdt4_aem_v1_0/mttkn_sgm");
		File inDir = new File(inDirPath);
		String[] files = inDir.list();
		BufferedReader in = null;
		BufferedWriter out = null;
		String line = null;
		int i = 0;
		String[] tags = new String[] { "TEXT", "DOC", "DOCNO", "DOCTYPE", "TXTTYPE" };
		for (String fname : files) {
			i++;
			System.out.println(i + "/" + files.length + ":" + fname);
			in = new BufferedReader(new FileReader(inDirPath + "/" + fname));
			out = new BufferedWriter(new FileWriter(outDirPath + "/" + fname));

			while ((line = in.readLine()) != null)
				if (startsWithTags(line, tags))
					out.write(line + "\n");
				else
					out.write(new String(line.getBytes("ISO-8859-1")).replaceAll("[<>]", "") + "\n");

			in.close();
			out.close();
		}

	}

	private boolean startsWithTags(String line, String[] tags) {
		boolean isContent = true;
		for (String tag : tags)
			if (line.startsWith("<" + tag + ">") || line.startsWith("</" + tag + ">")) {
				isContent = false;
				break;
			}
		return !isContent;
	}

	public void extraxtText_mmtkn(boolean xmlOutput) throws Exception {
		File dir = new File("/fs/clip-clip-proj/GeoNets/EntityTDT/hassan/tdt4_aem_v1_0/mttkn");
		String[] files = dir.list();
		BufferedReader in = null;
		BufferedWriter out = null;
		String line = null;
		int i = 0;
		for (String fname : files)
			try {
				i++;
				System.out.println(i + "/" + files.length + ":" + fname);
				in = new BufferedReader(new FileReader(dir.getPath() + "/" + fname));
				String[] splits = fname.split("_");
				String docno = splits[3] + "." + splits[0] + "." + splits[1] + "." + splits[2] + ".txt";
				if (xmlOutput)
					out = new BufferedWriter(new FileWriter(dir.getPath() + "/content/xml/" + docno));
				else
					out = new BufferedWriter(new FileWriter(dir.getPath() + "/content/txt/" + docno));
				if (xmlOutput)
					out.write("<DOC>\n<DOCNO> " + docno + " </DOCNO>\n<DOCTYPE> NEWS </DOCTYPE>\n<TXTTYPE> NEWSWIRE </TXTTYPE>\n<TEXT>\n");
				line = in.readLine();
				if (!line.contains("content_lang=ENGLISH"))
					continue;
				while ((line = in.readLine()) != null) {
					int ind = line.indexOf("tr=Y> ");
					if (ind != -1) {
						String word = line.substring(ind + 5);
						out.write(word);
					}
				}
				out.write("\n");
				if (xmlOutput)
					out.write("</TEXT>\n</DOC>");

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (in != null)
					in.close();
				if (out != null)
					;
				out.close();
			}

	}

}
