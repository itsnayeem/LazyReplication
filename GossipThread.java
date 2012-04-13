import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

public class GossipThread extends Thread {
	private static Logger log = Logger.getLogger(GossipThread.class);

	public void run() {
		while (true) {
			VectorClock.NumElems = ServerStateManager.getSnapshot().size();
			log.info(ServerStateManager.getSnapshot());
			try {
				Thread.sleep(5000 * WebServer.delayModifier);
			} catch (InterruptedException e) {
			}

			ServerStateManager.purgeDead();
			
			JSONgossipMessage j = new JSONgossipMessage();
			j.snapshot = ServerStateManager.getSnapshot();
			j.myState = ServerStateManager.getMyState();
			j.tweets = BackendDataStorage.getInstance().getTemp();

			for (Integer i : j.snapshot.keySet()) {
				log.info("Gossiping to: " + i); 
				if (ServerStateManager.getID() != i.intValue()) {
					HttpClient httpclient = new DefaultHttpClient();
					List<NameValuePair> formparams = new ArrayList<NameValuePair>();
					formparams.add(new BasicNameValuePair("s", WebServer.g
							.toJson(j, JSONgossipMessage.class)));
					UrlEncodedFormEntity entity = null;
					try {
						entity = new UrlEncodedFormEntity(formparams, "UTF-8");
					} catch (UnsupportedEncodingException e) {
					}
					HttpPost httppost = new HttpPost("http://"
							+ j.snapshot.get(i).ip + ":"
							+ j.snapshot.get(i).port + "/data/gossip");
					httppost.setEntity(entity);
					try {
						httpclient.execute(httppost);
					} catch (ClientProtocolException e) {
					} catch (IOException e) {
					}
				}
			}
		}
	}
}
