import java.util.ArrayList;


public class JSONfrontendResponse {
	public String query;
	public String cached;
	public ArrayList<String> tweets;
	
	public JSONfrontendResponse(String q, String c, ArrayList<String> t) {
		query = q;
		cached = c;
		tweets = t;
	}

}
