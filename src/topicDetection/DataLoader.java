package topicDetection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import dataset.twitter.StringDuplicate;

public class DataLoader {

	protected Constants constants;

	public DataLoader() {
	}

	public DataLoader(Constants constants) {
		this.constants = constants;
	}

	public DataInputStream openDataInputStream(String fileName) throws Exception {
		return new DataInputStream(new FileInputStream(fileName));
	}

	public boolean exists(String f) throws Exception {
		return new File(f).exists();
	}

	public String[] list(String fname) throws Exception {
		return new File(fname).list();
	}

	public void loadDocumentsForSpinn3r(String[] files, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter)
			throws Exception {
		for (int i = 0; i < files.length; i++) {
			if (i % 1000 == 0)
				System.out.println(i);
			// System.out.println(constants.DATA_KEY_NE_PATH + files[i]);
			String ff = (constants.DATA_KEYWORDS_1_PATH + files[i]);// +
			// ".txt");
			Document d = new Document(files[i]);
			loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, constants.KEYWORDS_1_WEIGHT);
			if (constants.KEYWORDS_2_ENABLE) {
				ff = (constants.DATA_KEYWORDS_2_PATH + files[i]);// +
				// ".txt");
				loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, constants.KEYWORDS_2_WEIGHT);
			}
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

	public void loadDocuments(String inputFileName, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			boolean removeDuplicates) throws Exception {
		File inputFile = new File(inputFileName);
		StringDuplicate sd = new StringDuplicate();
		if (inputFile.isDirectory()) {
			int i = 0;
			for (String file : inputFile.list())
				try {
					file = inputFileName + "/" + file;
					if (i++ % 1000 == 0)
						System.out.println(i + " documnets are loaded.");
					String id = file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("."));
					Document d = docs.containsKey(id) ? docs.get(id) : new Document(id);
					// d.setTitle(line.split(",")[0]);
					if (file.endsWith(".keywords"))
						loadDocumentKeyFile(openDataInputStream(file), stopwords, porter, d, constants.KEYWORDS_1_WEIGHT);
					else
						// if (file.endsWith(".txt"))
						loadDocumentTextFile(openDataInputStream(file), stopwords, porter, d, constants.TEXT_WEIGHT, removeDuplicates, sd);
					docs.put(id, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
		} else {
			BufferedReader in = new BufferedReader(new FileReader(inputFile));
			String line = null;
			int i = 0;
			while ((line = in.readLine()) != null)
				try {
					String[] tokens = line.split("\t", 2);
					if (i++ % 1000 == 0)
						System.out.println(i + " documnets are loaded.");
					String id = tokens[0];
					// id = i + "";
					Document d = docs.containsKey(id) ? docs.get(id) : new Document(id);
					// d.setTitle(line.split(",")[0]);
					// if (file.endsWith(".keywords"))
					// loadDocumentKeyFile(openDataInputStream(file), stopwords,
					// porter, d, constants.KEYWORDS_1_WEIGHT);
					// if (file.endsWith(".txt"))
					loadDocumentTextFile(new DataInputStream(new ByteArrayInputStream(tokens[1].getBytes("UTF-8"))), stopwords, porter, d,
							constants.TEXT_WEIGHT, removeDuplicates, sd);

					docs.put(id, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
			in.close();
		}

		System.out.println(docs.size() + " documents are loaded.");
		ArrayList<String> toRemove = new ArrayList<String>();
		for (Document d : docs.values())
			if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
				if (!removeDuplicates || !d.isDuplicate) {
					for (Keyword k : d.keywords.values())
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));

				}
			} else
				toRemove.add(d.id);

		for (String id : toRemove)
			docs.remove(id);

		System.out.println(docs.size() + " documents remaind after filterig small documents (Documents that have less than " + constants.DOC_KEYWORDS_SIZE_MIN
				+ " keywords).");

	}

	public void loadDocumentsOLD(String inputFileName, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			boolean removeDuplicates) throws Exception {
		File inputFile = new File(inputFileName);
		StringDuplicate sd = new StringDuplicate();
		if (inputFile.isDirectory()) {
			int i = 0;
			for (String file : inputFile.list())
				try {
					file = inputFileName + "/" + file;
					if (i++ % 1000 == 0)
						System.out.println(i + " documents are loaded.");
					String id = file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("."));
					Document d = docs.containsKey(id) ? docs.get(id) : new Document(id);
					// d.setTitle(line.split(",")[0]);
					if (file.endsWith(".keywords"))
						loadDocumentKeyFile(openDataInputStream(file), stopwords, porter, d, constants.KEYWORDS_1_WEIGHT);
					else
						// if (file.endsWith(".txt"))
						loadDocumentTextFile(openDataInputStream(file), stopwords, porter, d, constants.TEXT_WEIGHT, removeDuplicates, sd);
					docs.put(id, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
		} else {
			BufferedReader in = new BufferedReader(new FileReader(inputFile));
			String line = null;
			int i = 0;
			while ((line = in.readLine()) != null)
				try {
					String file = line.split(",")[1];
					if (i++ % 1000 == 0)
						System.out.println(i + " documents are loaded.");
					String id = file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("."));
					Document d = docs.containsKey(id) ? docs.get(id) : new Document(id);
					// d.setTitle(line.split(",")[0]);
					if (file.endsWith(".keywords"))
						loadDocumentKeyFile(openDataInputStream(file), stopwords, porter, d, constants.KEYWORDS_1_WEIGHT);

					if (file.endsWith(".txt"))
						loadDocumentTextFile(openDataInputStream(file), stopwords, porter, d, constants.TEXT_WEIGHT, removeDuplicates, sd);

					docs.put(id, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
			in.close();
		}

		System.out.println(docs.size() + " documents are loaded.");
		ArrayList<String> toRemove = new ArrayList<String>();
		for (Document d : docs.values())
			if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
				if (!removeDuplicates || !d.isDuplicate) {
					for (Keyword k : d.keywords.values())
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
				}
			} else
				toRemove.add(d.id);

		for (String id : toRemove)
			docs.remove(id);

		System.out.println(docs.size() + "documents remaind after filterig small documents (Documents that have less than " + constants.DOC_KEYWORDS_SIZE_MIN
				+ " keywords).");

	}

	public void loadDocumentKeyFile(DataInputStream in, HashSet<String> stopwords, Porter porter, Document d, double BoostRate) {
		// if (Constants.BREAK_NP)
		// fetchDocumentNEAndNPFileWithBreaking(f, stopwords, porter, d);
		// System.out.println("injaaaaaaaaaaaaaaaaa:"+d.id);
		try {
			// DataInputStream in = openDataInputStream(f);
			String line = null;
			while ((line = in.readLine()) != null && line.length() > 2) {
				// System.out.println(line);
				int index = line.lastIndexOf("==");
				String word = line.substring(0, index);
				double tf = Integer.parseInt(line.substring(index + 2)) * BoostRate;
				String base = getBaseForm(stopwords, porter, word);
				if (base.length() > 2)
					if (!d.keywords.containsKey(base))
						d.keywords.put(base, new Keyword(base, word, tf, 1, 0));
					else
						d.keywords.get(base).tf += tf;

			}
			in.close();
			// System.out.println("done::"+d.keywords.size());
		} catch (Exception e) {
			// System.out.println(f.getName());
			e.printStackTrace();
		}

	}

	public void loadDocumentKeyFile(ArrayList<String> words, HashSet<String> stopwords, Porter porter, Document d, double BoostRate) {
		// if (Constants.BREAK_NP)
		// fetchDocumentNEAndNPFileWithBreaking(f, stopwords, porter, d);
		// System.out.println("injaaaaaaaaaaaaaaaaa:"+d.id);
		try {
			// DataInputStream in = openDataInputStream(f);
			for (String word : words)
				if (!stopwords.contains(word) && word.length() > 2) {
					String base = getBaseForm(stopwords, porter, word);
					if (base.length() > 2)
						if (!d.keywords.containsKey(base))
							d.keywords.put(base, new Keyword(base, word, 1, 1, 0));
						else
							d.keywords.get(base).tf += 1;

				}
			// System.out.println("done::"+d.keywords.size());
		} catch (Exception e) {
			// System.out.println(f.getName());
			e.printStackTrace();
		}

	}

	public static String getBaseForm(HashSet<String> stopwords, Porter porter, String word) {
		String base = "";
		StringTokenizer stt = new StringTokenizer(word, "!' -_@0123456789.");
		// System.out.println(stt.countTokens()+"::"+stopwords+"::"+porter);
		while (stt.hasMoreTokens()) {
			String token = stt.nextToken().toLowerCase();
			if ((token.indexOf("?") == -1 && token.length() > 2 && !stopwords.contains(token)))
				base += porter.stripAffixes(token) + " ";
		}
		return base.trim();
	}

	public void loadDocumentTextFile(DataInputStream in, HashSet<String> stopwords, Porter porter, Document d, double BoostRate, boolean removeDuplicates,
			StringDuplicate sd) {
		try {
			String content = "";
			String line = "";
			while ((line = in.readLine()) != null)
				content += line;

			if (removeDuplicates)
				d.isDuplicate = sd.isDuplicate(content);

			StringTokenizer st = new StringTokenizer(content, "!?|\"' -_@0123456789.,;#$&%/\\*()<>\t");
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
			in.close();
			// System.out.println("done::"+d.keywords.size());
		} catch (Exception e) {
			// System.out.println(f.getName());
			e.printStackTrace();
		}

	}

	public boolean hasDigit(String in) {
		for (int i = 0; i < in.length(); i++)
			if (Character.isDigit(in.charAt(i)))
				return true;
		return false;
	}
}
