package dataset.twitter;
/*
 * DO NOT MODIFY THIS FILE (it is already completed and should not be changed).
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import org.apache.commons.lang.*;
//import com.twitter.Extractor;
/**
 * 
 * @author Shanchan Wu
 *
 */
public class RetweetUtil {
    /**
     * whether the tweet is a retweet.
     * @param tweetTxt
     * @return
     */
    public static boolean isRetweetJudgedByTweetTxt(String tweetTxt){
        
        String AT_SIGNS_CHARS = "@\uFF20";
        Pattern AT_SIGNS = Pattern.compile("[" + AT_SIGNS_CHARS + "]");
        
        // RT at the beginning
        Pattern EXTRACT_RETWEET = Pattern.compile("^(?:[\\s])*" 
                + "RT\\s*"    + AT_SIGNS  + "([a-z0-9_]{1,20}):*\\s*(.*)", Pattern.CASE_INSENSITIVE);
        
        //RT in the middle
        Pattern EXTRACT_RETWEET2 = Pattern.compile("^.*(?:[\\s])+" 
                + "RT\\s*"    + AT_SIGNS  + "([a-z0-9_]{1,20}):*\\s*(.*)", Pattern.CASE_INSENSITIVE);
        
        if (tweetTxt == null) {
            return false;
         }

        Matcher matcher = EXTRACT_RETWEET.matcher(tweetTxt);
        Matcher matcher2 = EXTRACT_RETWEET2.matcher(tweetTxt);
        if (matcher.matches()) {
            //System.out.println( matcher.group(1));
            //String ID =  matcher.group(1);
            return true;

        } else if(matcher2.matches()) {
            //System.out.println( matcher2.group(1));      
            //String ID =  matcher2.group(1);
            return true;
        }

        return false;
     
    }
    
    
    /**
     * Get the retweet_of_user_name from the tweet text.
     * If it is not a retweet, return null.
     * @param tweetTxt
     * @return
     */
    public static String getRetweetOfUserNameFromTweetTxt(String tweetTxt){
        
        String AT_SIGNS_CHARS = "@\uFF20";
        Pattern AT_SIGNS = Pattern.compile("[" + AT_SIGNS_CHARS + "]");
        
        // RT at the beginning
        Pattern EXTRACT_RETWEET = Pattern.compile("^(?:[\\s])*" 
                + "RT\\s*"    + AT_SIGNS  + "([a-z0-9_]{1,20}):*\\s*(.*)", Pattern.CASE_INSENSITIVE);
        
        //RT in the middle
        Pattern EXTRACT_RETWEET2 = Pattern.compile("^.*(?:[\\s])+" 
                + "RT\\s*"    + AT_SIGNS  + "([a-z0-9_]{1,20}):*\\s*(.*)", Pattern.CASE_INSENSITIVE);
        
        if (tweetTxt == null) {
            return null;
         }

        Matcher matcher = EXTRACT_RETWEET.matcher(tweetTxt);
        Matcher matcher2 = EXTRACT_RETWEET2.matcher(tweetTxt);
        if (matcher.matches()) {
            //System.out.println( matcher.group(1));
            String userName =  matcher.group(1);
            return userName;

        } else if(matcher2.matches()) {
            //System.out.println( matcher2.group(1));      
            String userName =  matcher2.group(1);
            return userName;
        }

        return null;
     
    }
    

    
}