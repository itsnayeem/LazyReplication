import java.util.ArrayList;


public class JSONbackendResponse {
	public String status;
	public VectorClock version;
	public ArrayList<String> tweets;
	
	public JSONbackendResponse(String s, VectorClock v, ArrayList<String> t) {
		status = s;
		version = v;
		tweets = t;
	}

}
