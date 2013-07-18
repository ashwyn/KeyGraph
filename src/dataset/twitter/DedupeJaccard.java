package dataset.twitter;
import com.aliasi.spell.JaccardDistance;

import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

import com.aliasi.util.Files;
import com.aliasi.util.Strings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//import org.supercsv.io.CsvListReader;
//import org.supercsv.io.CsvListWriter;
//import org.supercsv.prefs.CsvPreference;

public class DedupeJaccard {

	public static void main(String[] args) throws IOException {
		//		StringBuilder sb = new StringBuilder();
		//		File dataDir = new File(args[0]);
		//		List<String> texts = parseFile(dataDir);
		//		TokenizerFactory tokFactory
		//		= new NormalizedTokenizerFactory(); //implements normalization
		File output = new File(args[1]); 
		System.out.println("Writing to: " + output.toString());
		FileOutputStream stream =  new FileOutputStream(output);
		OutputStreamWriter streamWriter 
		= new OutputStreamWriter(stream,Strings.UTF8);
		//	CsvListWriter writer 
		//	    = new CsvListWriter(streamWriter,CsvPreference.EXCEL_PREFERENCE); 
		double cutoff = .5d;
		//	filterJaccardAndWrite(texts,tokFactory,writer,cutoff);
		//	writer.close();
	}

	static void filterJaccardAndWrite(List<String> texts, TokenizerFactory tokFactory, double cutoff) throws IOException {
		ArrayList<String> row = new ArrayList<String>();
		row.add("");
		row.add("");
		row.add("");	
		row.add("Tweet");
		//	writer.write(row); //initialize the csv file with headers
		List<String> filtered = filterTweetsJaccard(texts,tokFactory,cutoff);
		for (String tweet : filtered) {
			row.clear();
			row.add("");
			row.add("");
			row.add("");
			row.add(tweet);
			//	    writer.write(row);
		}
		System.out.println("Filtered size: " + filtered.size() 
				+ " originally " + texts.size()); 	
	}

	public static List<String> filterTweetsJaccard(List<String> set,
			TokenizerFactory tokFactory,
			double cutoff) {
		JaccardDistance jaccardD = new JaccardDistance(tokFactory);
		List<String> filteredTweets = new ArrayList<String>();
		for (int i = 0; i < set.size(); ++i) {
			String targetTweet = set.get(i);
			boolean addTweet = true;
			//big research literature on making the below loop more efficient
			for (int j = 0; j < filteredTweets.size(); ++j ) {
				String comparisionTweet = filteredTweets.get(j);
				double proximity 
				= jaccardD.proximity(targetTweet,comparisionTweet);
				if (proximity >= cutoff) {
					addTweet = false;
					break; //one nod to efficency
				}
			}
			if (addTweet) {
				filteredTweets.add(targetTweet);
			}
		}
		return filteredTweets;
	}

	public static HashSet<String> filterTweetsJaccard (Set<String> set,
			TokenizerFactory tokFactory,
			double cutoff) throws IOException {

		//File for housing duplicates
//		BufferedWriter duplicates = new BufferedWriter(new FileWriter("duplicates.txt"));
//		duplicates.write("Duplicates \n\n");

		JaccardDistance jaccardD = new JaccardDistance(tokFactory);
		HashSet<String> filteredTweets = new HashSet<String>();
		Iterator<String> setIterate =   set.iterator();

		//Only use if you need to know line number

		while(setIterate.hasNext()){
			String targetTweet = setIterate.next();
			boolean addTweet = true;
			//big research literature on making the below loop more efficient
			Iterator<String> filtertedIterate =   filteredTweets.iterator();

			//Skip this tweet if it's a retweet
			while(filtertedIterate.hasNext()){
				String comparisonTweet = filtertedIterate.next();
				double proximity = jaccardD.proximity(targetTweet,comparisonTweet);
				if (proximity >= cutoff) {
//					System.out.println("Old Tweet\n	"+ set.get(comparisonTweet) + "\n");
//					System.out.println("New Tweet\n	"+ set.get(targetTweet) + "\n");
//					System.out.println("Proximity:	" + proximity + "\n\n");
					addTweet = false;			
					break; //one nod to efficiency
				}
			}
			if (addTweet) {
				filteredTweets.add(targetTweet);
			}

		}
//		duplicates.close();
		return filteredTweets;
	}
	public static HashSet<String> filterTweetsJaccard(HashSet<String> set,
			TokenizerFactory tokFactory, String comparisonTweet,
			double cutoff) throws IOException {

		//File for housing duplicates
//		BufferedWriter duplicates = new BufferedWriter(new FileWriter("duplicates.txt"));
//		duplicates.write("Duplicates \n\n");
		
		JaccardDistance jaccardD = new JaccardDistance(tokFactory);
		Iterator<String> setIterate =   set.iterator();

		//Only use if you need to know line number

		while(setIterate.hasNext()){
			String targetTweet = setIterate.next();
//			boolean addTweet = true;
			//big research literature on making the below loop more efficient
			//Skip this tweet if it's a retweet
			double proximity = jaccardD.proximity(targetTweet,comparisonTweet);
			if (proximity >= cutoff) {
				System.out.println("Old Tweet\n	"+ comparisonTweet + "\n");
				System.out.println("New Tweet\n	"+ targetTweet + "\n");
				System.out.println("Proximity:	" + proximity + "\n\n");
				return set;
			}


		}
//		duplicates.close();
		set.add(comparisonTweet);
		return set;
	}

	static List<String> parseFile(File file) throws IOException {
		List<String> tweets = new ArrayList<String>();
		FileInputStream fileInput = new FileInputStream(file);
		InputStreamReader fileReader = new InputStreamReader(fileInput,Strings.UTF8);
		//	CsvListReader reader
		//	    = new CsvListReader(fileReader, CsvPreference.EXCEL_PREFERENCE);
		//	try {
		//	    String[] header = reader.getCSVHeader(true);
		//	    List<String> csv = null;
		//	    while ((csv = reader.read()) != null) {
		//		tweets.add(csv.get(TweetWriter.TWEET_OFFSET));
		//	    }
		//	} finally {
		//	    reader.close();
		//	}
		return tweets;
	}
}
