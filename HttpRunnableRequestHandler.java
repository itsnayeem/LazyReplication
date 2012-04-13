import java.io.IOException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;


public class HttpRunnableRequestHandler implements Runnable {
	private final HttpService httpservice;
    private final HttpServerConnection conn;
    
    public HttpRunnableRequestHandler(
            final HttpService httpservice, 
            final HttpServerConnection conn) {
        this.httpservice = httpservice;
        this.conn = conn;
    }

    public void run() {
        HttpContext context = new BasicHttpContext(null);
        try {
            while (!Thread.interrupted() && this.conn.isOpen()) {
                this.httpservice.handleRequest(this.conn, context);
            }
        } catch (ConnectionClosedException ex) {
        } catch (IOException ex) {
        } catch (HttpException ex) {
        } finally {
            try {
                this.conn.shutdown();
            } catch (IOException ignore) {}
        }
    }
}
