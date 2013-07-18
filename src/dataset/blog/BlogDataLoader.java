package dataset.blog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import topicDetection.Constants;
import topicDetection.DataLoader;
import topicDetection.Document;
import topicDetection.Porter;


public class BlogDataLoader extends DataLoader {

	public BlogDataLoader(Constants constants) {
		super(constants);
	}

	public void loadDocumentKeyFilesByDate(String[] dates, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF)
			throws Exception {
		Porter porter = new Porter();
		BufferedReader in = null;
		ArrayList<String> files = new ArrayList<String>();
		for (String date : dates) {
			in = new BufferedReader(new FileReader(constants.DATA_DATE_PATH + "/" + date));
			String line = null;
			while ((line = in.readLine()) != null)
				files.add(line);
		}
		String[] fileList = new String[files.size()];
		loadDocumentsForSpinn3r(files.toArray(fileList), docs, stopwords, DF, porter);

	}

}
