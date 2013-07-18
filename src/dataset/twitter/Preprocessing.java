
package dataset.twitter;
//import com.twitter.Extractor;

import java.io.*;
import java.sql.*;
//import org.apache.commons.lang.*;
//import com.twitter.Extractor;

public class Preprocessing {
 	
	public static void main(String[] args) throws IOException, SQLException {
     BufferedReader firstReader = new BufferedReader(new FileReader("/fs/clip-sm3/temp/etsy_posts.txt"));

     StringDuplicate stringDuplicate=new StringDuplicate();
	 String line;
	 //DB db = new DB();
		
	 // set the correct DB name, userName, password
	 //Connection con = db.dbConnect("jdbc:mysql://cliptweetdb01:3306/STREAMtweet","collect","tw33tcOllec7"); 
	 //Statement stmt = con.createStatement();
	 
	 while ((line=firstReader.readLine()) != null) {
		 	
		 	String[] strArr = line.split("\t");
		 	
		 	String tweetID = strArr[0]; //has the article id
		 	String tweet = strArr[strArr.length - 1];
		 	
		 	
		 	//check status_is_retweet, if it is 1 then that is bad
		 	/*
		 	rs = stmt.executeQuery("SELECT status_is_retweet FROM status WHERE status_id="+tweetID+";");
		 	int isRetweetFromField = -1;
		 	if(rs.next()){
		 		isRetweetFromField = rs.getInt(1);
		 	}//isRetweetFromField is either a 0 (false) or a 1 (true) depending on the field in the db
		 	*/
		 	
		 	int isExplicitRT = -1;
		 	if(tweet.indexOf("RT ")==-1 && tweet.indexOf("rt ")==-1&&
		 			tweet.indexOf("Rt ")==-1 && tweet.indexOf("RT@")==-1&&
		 			tweet.indexOf("rt@ ")==-1&&tweet.indexOf("Rt@")==-1){
		 		isExplicitRT = 0;
		 	}
		 	else{
		 		isExplicitRT = 1;
		 	}
		 	
		 	boolean isRetweetFromRetweetUtil;
		 	isRetweetFromRetweetUtil = RetweetUtil.isRetweetJudgedByTweetTxt(tweet);
		 	//if this variable is true, then it is a retweet
		 	
		 	String trimTweet = tweet.trim();
			if(trimTweet.length() != 0 && trimTweet.charAt(0) == '@'){
		         int index = trimTweet.indexOf(' '); // find the postion of ' ' (space) right after  "@anyLengthName"
		         if(index < tweet.length() ){
		        	trimTweet =trimTweet.substring(index);
		            trimTweet = trimTweet.trim();
		         }
			}
			boolean isDup;		
			if (isExplicitRT==1 || isRetweetFromRetweetUtil)
				isDup=true;
			else
				isDup = stringDuplicate.isDuplicate(trimTweet);
		
		 	/*if a tweet passes all the tests, then you can either print it to a text file, as I did here
		 	 * or do something else
		 	 */
			if(isExplicitRT==0 && isRetweetFromRetweetUtil==false && isDup ==false ){
				System.out.println(line);
			}
		 	
	 	}
	 firstReader.close();

	}	
}

