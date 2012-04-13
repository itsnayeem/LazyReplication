import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class WebServer {
	private static Logger log = Logger.getLogger(WebServer.class);
	public static Gson g = new Gson();
	public static int delayModifier = 2;

	public static void main(String[] args) {
		int MyID = Integer.parseInt(args[0]);
		String MyIP = args[1];
		int MyPort = Integer.parseInt(args[2]);

		ServerStateManager.init(MyID, MyIP, MyPort);
		
		log.info(ServerStateManager.getSnapshot());
		
		String DataServerAddress = null;
		int DataServerPort = 0;
		
		if (MyID != 0) {
			DataServerAddress = args[3];
			DataServerPort = Integer.parseInt(args[4]);

			ServerStateManager.updateSnapshot(DataServerAddress, DataServerPort);
		}
		
		if (MyID > -1) {
			if (MyID != 0) {
				ServerStateManager.initReplication(DataServerAddress, DataServerPort);
			}
			new GossipThread().start();
		}

		log.info("Starting server type: " + ((MyID > -1) ? "backend" : "frontend") + " port: " + MyPort);

		Thread t = null;
		try {
			t = new HttpRequestListener(MyPort);
		} catch (IOException e) {
		}
		t.setDaemon(false);
		t.start();
	}
}
