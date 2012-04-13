import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;

public class BackendDataQueryHandler implements HttpRequestHandler {
	private static Gson g = new Gson();

	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {

		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);

		System.out.println("Handling Data Query; Line = " + request.getRequestLine());
		if (method.equals("GET")) {
			final String target = request.getRequestLine().getUri();

			Pattern p = Pattern.compile("/data/query\\?q=([^&]*)&v=([^&]*)$");
			Matcher m = p.matcher(target);
			if (m.find()) {
				System.out.println("'" + target + "' matches the pattern");
				System.out.flush();
				String hash = m.group(1);
				VectorClock version = g.fromJson(URLDecoder.decode(m.group(2),"UTF-8"), VectorClock.class);
				BackendDataStorage ds = BackendDataStorage.getInstance();
				
				final String Content = ds.getTweet(hash, version);
				System.out.println("Backend JSON: " + Content);
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