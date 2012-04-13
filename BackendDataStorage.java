import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class BackendDataStorage {
	private static Logger log = Logger.getLogger(BackendDataStorage.class);
	private static Gson g = new Gson();

	private HashMap<String, DataNode> index;
	private HashMap<String, DataNode> tempStore;
	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private static BackendDataStorage instance;

	private BackendDataStorage() {
		index = new HashMap<String, DataNode>();
		tempStore = new HashMap<String, DataNode>();
	}

	public static BackendDataStorage getInstance() {
		if (instance == null) {
			synchronized (BackendDataStorage.class) {
				if (instance == null) {
					instance = new BackendDataStorage();
				}
			}
		}
		return instance;
	}

	public void mergeTweets(HashMap<String, DataNode> h, VectorClock c) {
		lock.writeLock().lock();
		VectorClock serverClock = ServerStateManager.getMyClockVal();
		for (String hash : h.keySet()) {
			DataNode tweetList = null;

			if ((tweetList = index.get(hash)) == null) {
				index.put(hash, new DataNode());
				tweetList = index.get(hash);
			}
			ArrayList<Tweet> tweetsToAdd = h.get(hash).getTweetsList();
			for (Tweet t : tweetsToAdd) {
				tweetList.mergeTweet(t, serverClock);
			}
		}

		ServerStateManager.mergeClock(c);
		lock.writeLock().unlock();
	}

	public String getTweet(String hash, VectorClock version) {
		JSONbackendResponse response = null;
		DataNode tweet = null;
		log.info("Getting tweet for: " + hash + " version: " + version);
		lock.readLock().lock();
		if (index.containsKey(hash)) {
			tweet = index.get(hash);
			log.info("Found tweets for: " + hash + " version: "
					+ tweet.getVersion());
			if (tweet.getVersion().compareTo(version) == 0) {
				response = new JSONbackendResponse("ok", null, null);
			} else {
				response = new JSONbackendResponse("updated",
						tweet.getVersion(), tweet.getTweets());
			}

		} else {
			response = new JSONbackendResponse("error", null, null);
		}

		lock.readLock().unlock();
		return g.toJson(response, JSONbackendResponse.class);
	}

	public void addTweet(String hash, String tweet) {
		log.info("Adding '" + tweet + "' to #" + hash);
		lock.writeLock().lock();
		DataNode tweetList = null;

		if ((tweetList = index.get(hash)) == null) {
			index.put(hash, new DataNode());
			tempStore.put(hash, index.get(hash));
			tweetList = index.get(hash);
		}
		tweetList.addTweet(tweet);
		lock.writeLock().unlock();
	}

	public HashMap<String, DataNode> getTemp() {
		lock.writeLock().lock();
		HashMap<String, DataNode> retval = tempStore;
		tempStore = new HashMap<String, DataNode>();
		lock.writeLock().unlock();
		return retval;
	}

	public HashMap<String, DataNode> getReplication() {
		lock.readLock().lock();
		HashMap<String, DataNode> retval = index;
		lock.readLock().unlock();
		return retval;
	}
	
	public void replicate(HashMap<String, DataNode> h) {
		lock.writeLock().lock();
		index = h;
		lock.writeLock().unlock();
	}
}
