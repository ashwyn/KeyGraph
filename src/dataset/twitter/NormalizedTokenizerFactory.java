package dataset.twitter;

import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.tokenizer.ModifyTokenTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;

public class NormalizedTokenizerFactory extends ModifyTokenTokenizerFactory {


    public NormalizedTokenizerFactory(TokenizerFactory factory) {
	super(factory);
    }

    public NormalizedTokenizerFactory() {
	super(new RegExTokenizerFactory("http:[^\\s]*|@[^\\s]*|\\w+")); 
    }

    public String modifyToken(String token) {
	if (token.matches("RT")) {
	    return null;
	}
	else if (token.startsWith("@")) {
	    return null;
	}
	else if (token.startsWith("http")) {
	    return null;
	}

	else {
	    return token;
	}
    }

    static final long serialVersionUID = -7949489227829523271L;

    public static void main(String[] args) {
	TokenizerFactory tokFactory = new NormalizedTokenizerFactory();
	String text = "RT @mparent77772: Should Obama's 'internet kill switch' power be curbed? http://bbc.in/hcVGoz";
	System.out.println("Tweet: " + text);
	char[] chars = text.toCharArray();
	Tokenizer tokenizer 
	    = tokFactory.tokenizer(chars,0,chars.length);
	String token;
	System.out.println("White Space :'" +  tokenizer.nextWhitespace() + "'");
	while ((token = tokenizer.nextToken()) != null) {
	    System.out.println("Token: " + token);
	    System.out.println("White Space :'" + tokenizer.nextWhitespace()+"'");
	}
    }
}
