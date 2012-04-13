import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class ServerStateManager {
	private static Logger log = Logger.getLogger(ServerStateManager.class);
	private static Gson g = new Gson();
	private static Random r = new Random();

	private static ServerState myState;
	private static ServerState myBackend;
	private static HashMap<Integer, ServerState> snapshot;

	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static void init(int ID, String ip, int port) {
		myState = new ServerState(ip, port);
		myState.setID(ID);
		snapshot = new HashMap<Integer, ServerState>();
		if (myState.getID() > -1) {
			snapshot.put(myState.getID(), myState);
		}
	}

	public static void updateSnapshot(String ip, int port) {
		lock.writeLock().lock();
		HttpClient httpclient = new DefaultHttpClient();
		log.info("Connecting to backend to request snapshot");
		HttpGet httpget = new HttpGet("http://" + ip + ":" + port
				+ "/data/snapshot");
		HttpResponse resp = null;
		try {
			resp = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
		HttpEntity entity = resp.getEntity();
		String entityContent = null;
		try {
			entityContent = EntityUtils.toString(entity);
		} catch (ParseException e) {
		} catch (IOException e) {
		}

		log.info("Backend returns: " + entityContent);
		HashMap<Integer, ServerState> response = g.fromJson(entityContent,
				JSONsnapshot.class);
		mergeInfo(response);
		log.info(snapshot);

		if (myState.getID() == -1)
			chooseBackend();
		
		VectorClock.NumElems = ServerStateManager.getSnapshot().size();
		lock.writeLock().unlock();
	}
	
	public static void removeDeadBackend() {
		lock.writeLock().lock();
		log.info("Removing Dead Backend: "+myBackend.getID());
		snapshot.remove(myBackend.getID());
		chooseBackend();
		lock.writeLock().unlock();
	}

	public static void initReplication(String ip, int port) {
		HttpClient httpclient = new DefaultHttpClient();
		log.info("Connecting to backend to request replicaiton");
		HttpGet httpget = new HttpGet("http://" + ip + ":" + port
				+ "/data/snapshot");
		HttpResponse resp = null;
		try {
			resp = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
		HttpEntity entity = resp.getEntity();
		String entityContent = null;
		try {
			entityContent = EntityUtils.toString(entity);
		} catch (ParseException e) {
		} catch (IOException e) {
		}

		log.info("Backend returns: " + entityContent);
		JSONreplication j = g.fromJson(entityContent, JSONreplication.class);
		HashMap<String, DataNode> idx = j.index;
		if (idx != null)
			BackendDataStorage.getInstance().replicate(idx);
		if (j.clock != null)
			ServerStateManager.mergeClock(j.clock);
	}

	public static void mergeInfo(HashMap<Integer, ServerState> h) {
		lock.writeLock().lock();
		for (Integer i : h.keySet()) {
			if (!snapshot.containsKey(i)) {
				snapshot.put(i, h.get(i));
			}
		}
		lock.writeLock().unlock();
	}

	public static void purgeDead() {
		lock.writeLock().lock();
		myState.updateLastSeen();
		boolean copyReqd = false;
		long time = System.currentTimeMillis();
		for (Integer i : snapshot.keySet()) {
			if (time - snapshot.get(i).getLastSeen() > (30000 * WebServer.delayModifier)) {
				copyReqd = true;
			}
		}
		if (copyReqd) {
			HashMap<Integer, ServerState> temp = new HashMap<Integer, ServerState>();
			for (Integer i : snapshot.keySet()) {
				if (time - snapshot.get(i).getLastSeen() < (30000 * WebServer.delayModifier)) {
					temp.put(i, snapshot.get(i));
				}
			}
			snapshot = temp;
		}
		lock.writeLock().unlock();
	}

	public static void chooseBackend() {
		lock.writeLock().lock();
		int b = r.nextInt(snapshot.size());
		log.info("Choosing backend number: " + b);
		myBackend = snapshot.get(b);
		log.info("MyBackend: " + myBackend);
		lock.writeLock().unlock();
	}

	public static HashMap<Integer, ServerState> getSnapshot() {
		return snapshot;
	}

	public static int getID() {
		return myState.getID();
	}

	public static String getMyBackendIp() {
		return myBackend.ip;
	}

	public static int getMyBackendPort() {
		return myBackend.port;
	}

	public static ServerState getMyState() {
		return myState;
	}

	public static VectorClock getMyClockVal() {
		VectorClock retval = null;
		lock.readLock().lock();
		retval = myState.getClockVal();
		lock.readLock().unlock();
		return retval;

	}

	public static VectorClock getCopyMyClockVal() {
		VectorClock retval = null;
		lock.readLock().lock();
		retval = myState.getClockCopy();
		lock.readLock().unlock();
		return retval;

	}

	public static void incrementMyClock() {
		lock.writeLock().lock();
		myState.incrementClock();
		lock.writeLock().unlock();
	}

	public static void mergeClock(VectorClock c) {
		lock.writeLock().lock();
		myState.mergeIn(c);
		lock.writeLock().unlock();
	}

}
