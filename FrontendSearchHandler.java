import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class FrontendSearchHandler implements HttpRequestHandler {
	private static Gson g = new Gson();
	private static Logger log = Logger.getLogger(FrontendSearchHandler.class);

	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {

		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);

		log.info("Frontend: Handling Search; Line = "
				+ request.getRequestLine());
		if (method.equals("GET")) {
			final String target = request.getRequestLine().getUri();

			Pattern p = Pattern.compile("/search\\?q=(.*)$");
			Matcher m = p.matcher(target);
			if (m.find()) {
				String hash = m.group(1);

				FrontendDataCache dc = FrontendDataCache.getInstance();
				VectorClock currentVersion = dc.getVersion(hash);

				HttpClient httpclient = new DefaultHttpClient();
				log.info("Connecting to backend to search for #" + hash);
				HttpGet httpget = new HttpGet("http://"
						+ ServerStateManager.getMyBackendIp()
						+ ":"
						+ ServerStateManager.getMyBackendPort()
						+ "/data/query?q="
						+ hash
						+ "&v="
						+ URLEncoder.encode(
								g.toJson(currentVersion, VectorClock.class),
								"UTF-8"));
				HttpResponse r = null;
				try {
					r = httpclient.execute(httpget);
				} catch (Exception e) {
					ServerStateManager.removeDeadBackend();
					ServerStateManager.updateSnapshot(ServerStateManager.getMyBackendIp(), ServerStateManager.getMyBackendPort());
					handle(request, response, context);
					return;
				}
				HttpEntity entity = r.getEntity();
				String entityContent = null;
				try {
					entityContent = EntityUtils.toString(entity);
				} catch (ParseException e) {
				} catch (IOException e) {
				}

				log.info("Backend returns: " + entityContent);
				final String Content = dc.getTweets(hash, entityContent);
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream)
							throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(
								outstream, "UTF-8");
						writer.write(Content);
						writer.write("\n");
						writer.flush();
					}
				});
				body.setContentType("application/json; charset=UTF-8");

				response.setStatusCode(HttpStatus.SC_OK);
				response.setEntity(body);
			} else {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			}
		} else {
			throw new MethodNotSupportedException(method
					+ " method not supported\n");
		}

	}
}