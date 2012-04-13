
public class Tweet implements Comparable<Tweet> {
	private String tweet;
	private VectorClock clock;
	
	public Tweet(String t, VectorClock c) {
		tweet = t;
		clock = c;
	}
	
	public String getTweet() {
		return tweet + "|" + clock;
	}
	
	public VectorClock getClock() {
		return clock;
	}
	
	public int compareTo(Tweet t) {
		return clock.compareTo(t.getClock());
	}

}
