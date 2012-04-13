import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

public class DataNode {
	private static Logger log = Logger.getLogger(DataNode.class);
	ArrayList<Tweet> tweets;

	public DataNode() {
		tweets = new ArrayList<Tweet>();
	}

	public VectorClock getVersion() {
		Tweet max = tweets.get(0);
		for (Tweet t : tweets) {
			if (t.compareTo(max) > 0) {
				max = t;
			}
		}
		return max.getClock();
	}

	public void addTweet(String tweet) {
		ServerStateManager.incrementMyClock();
		VectorClock c = ServerStateManager.getMyClockVal();
		log.info("Adding tweet '" + tweet + "' at time: " + c);
		tweets.add(new Tweet(tweet, c));
	}
	
	public ArrayList<Tweet> getTweetsList() {
		Collections.sort(tweets);
		return tweets;
	}
	
	public void mergeTweet(Tweet t, VectorClock c) {
		log.info("Merging tweet '" + t.getTweet() + "' at time: " + t.getClock() + " with time " + c);
		VectorClock newClock = t.getClock();
		newClock.mergeIn(c);
		tweets.add(new Tweet(t.getTweet(), newClock));
	}

	public ArrayList<String> getTweets() {
		ArrayList<String> retval = new ArrayList<String>();
		for (Tweet t : tweets) {
			retval.add(t.getTweet());
		}
		Collections.sort(retval);
		return retval;
	}
}