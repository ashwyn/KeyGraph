package dataset.twitter;

/*
 * DO NOT MODIFY THIS FILE (it is already completed and should not be changed).
 */
import java.io.IOException;
import java.util.HashSet;
import com.aliasi.tokenizer.TokenizerFactory;

public class StringDuplicate {

	HashSet<String> set = new HashSet<String>();

//	public HashMap<String, String> mapPrune(HashMap<String, String> strings) throws IOException {
//		TokenizerFactory tokFactory = new NormalizedTokenizerFactory();
//		HashSet<String> res = DedupeJaccard.filterTweetsJaccard(strings.keySet(), tokFactory, .80);
//		for (String tweet : res) {
//			strings.remove(tweet);
//		}
//		return strings;
//	}

	// The first parameter is the tweet + metadata, the second component is just
	// the tweet.
	public boolean isDuplicate(String tweet) throws IOException {
		TokenizerFactory tokFactory = new NormalizedTokenizerFactory();
		int size = set.size();
		set = DedupeJaccard.filterTweetsJaccard(set, tokFactory, tweet, .80);
		if (set.size() > size) {
			return true;
		}
		return false;
	}

	/*
	 * Simple way to test whether a series of strings contains duplicate
	 * strings. This method simply uses java HashSet<String>. Each time calling
	 * this method will add the string to the HashSet if there was not a
	 * duplicate string in the HashSet. All strings are treated as case
	 * insensitive.
	 * 
	 * @param str the string to test
	 * 
	 * @return true if there was a duplicate string before this string or if it
	 * is null,, otherwise false.
	 */

	// public boolean isDuplicate(String str){
	//
	// if(str == null){
	// return true;
	// }
	//
	// str = str.trim();
	// str = str.toLowerCase();
	// if(set.contains(str)){
	// return true;
	// }
	//
	// set.add(str);
	// return false;
	//
	// }

	/**
	 * test for this class
	 */
	public static void main(String[] args) throws Exception{
		StringDuplicate dp = new StringDuplicate();

		boolean b1 = dp.isDuplicate("We are friends");
		boolean b2 = dp.isDuplicate("we ARE friends");
		boolean b3 = dp.isDuplicate("We are friends2");

		System.out.println(b1);
		System.out.println(b2);
		System.out.println(b3);

	}

}