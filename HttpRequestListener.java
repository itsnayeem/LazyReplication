import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.log4j.Logger;

public class HttpRequestListener extends Thread {
	private static Logger log = Logger.getLogger(HttpRequestListener.class);

    private final ServerSocket serversocket;
    private final HttpParams params; 
    private final HttpService httpService;
    
    public HttpRequestListener(int myPort) throws IOException {
        this.serversocket = new ServerSocket(myPort);
        this.params = new SyncBasicHttpParams();
        this.params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        // Set up the HTTP protocol processor
        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
                new ResponseDate(),
                new ResponseServer(),
                new ResponseContent(),
                new ResponseConnControl()
        });
        
        // Set up request handlers
        HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
        if (ServerStateManager.getID() > -1) {
        	reqistry.register("/data/query*", new BackendDataQueryHandler());
        	reqistry.register("/data/update*", new BackendDataUpdateHandler());
        	reqistry.register("/data/snapshot*", new BackendSnapshotRequestHandler());
        	reqistry.register("/data/replication*", new BackendReplicationRequestHandler());
        	reqistry.register("/data/gossip*", new BackendGossipHandler());
        } else {
        	reqistry.register("/search*", new FrontendSearchHandler());
        	reqistry.register("/status/update*", new FrontendStatusUpdateHandler());
        }
        reqistry.register("*", new HttpFallbackHandler());
        
        // Set up the HTTP service
        this.httpService = new HttpService(
                httpproc, 
                new DefaultConnectionReuseStrategy(), 
                new DefaultHttpResponseFactory(),
                reqistry,
                this.params);
    }
    
    public void run() {
        log.info("Listening on port " + this.serversocket.getLocalPort());
        while (!Thread.interrupted()) {
            try {
                // Set up HTTP connection
                Socket socket = this.serversocket.accept();
                DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                log.info("Incoming connection from " + socket.getInetAddress());
                conn.bind(socket, this.params);

                // Start worker thread
                HttpRunnableRequestHandler r = new HttpRunnableRequestHandler(this.httpService, conn);
                WorkQueue w = WorkQueue.getInstance();
                w.execute(r);
                
            } catch (InterruptedIOException ex) {
                break;
            } catch (IOException e) {
                break;
            }
        }
    }
}