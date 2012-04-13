import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;

public class FrontendDataCache {
	//private static Logger log = Logger.getLogger(FrontendDataCache.class);
	private static Gson g = new Gson();
	
	private final HashMap<String, CacheNode> index;
	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private static FrontendDataCache instance;
	
	private FrontendDataCache() {
		index = new HashMap<String, CacheNode>();
	}

	public static FrontendDataCache getInstance() {
		if (instance == null) {
			synchronized (FrontendDataCache.class) {
				if (instance == null) {
					instance = new FrontendDataCache();
				}
			}
		}
		return instance;
	}

	public VectorClock getVersion(String hash) {
		VectorClock retval = null;
		CacheNode tweet = null;
		lock.readLock().lock();
		if ((tweet = index.get(hash)) != null) {
			retval = tweet.getVersion();
		}
		lock.readLock().unlock();
		if (retval == null) {
			retval = new VectorClock();
		}
		return retval;
	}

	public String getTweets(String query, String entityContent) {
		JSONbackendResponse backResp = g.fromJson(entityContent, JSONbackendResponse.class);
		JSONfrontendResponse response = null;
		lock.readLock().lock();
		if (backResp.status.equals("ok")) {
			response = new JSONfrontendResponse(query, "yes", index.get(query).getContent());
		} else if (backResp.status.equals("updated")) {
			index.put(query, new CacheNode(backResp.version, backResp.tweets));
			response = new JSONfrontendResponse(query, "no", index.get(query).getContent());
		} else {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("No content");
			response = new JSONfrontendResponse(query, "no", temp);
		}
		lock.readLock().unlock();
		return g.toJson(response, JSONfrontendResponse.class);
	}

	public void addTweet(String hash, VectorClock version, ArrayList<String> tweets) {
		lock.writeLock().lock();
		index.put(hash, new CacheNode(version, tweets));
		lock.writeLock().unlock();
	}

	private class CacheNode {
		private VectorClock version;
		private ArrayList<String> content;

		public CacheNode(VectorClock version, ArrayList<String> content) {
			this.version = version;
			this.content = content;
		}

		public VectorClock getVersion() {
			return version;
		}

		public ArrayList<String> getContent() {
			return content;
		}
	}
}
