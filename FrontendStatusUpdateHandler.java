import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class FrontendStatusUpdateHandler implements HttpRequestHandler {

	private static Logger log = Logger.getLogger(FrontendStatusUpdateHandler.class);

	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {

		log.info("Frontend: Handling StatusUpdate; Line = "
				+ request.getRequestLine());
		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);

		if (method.equals("POST")) {
			final String target = request.getRequestLine().getUri();
			Pattern p_uri = Pattern.compile("/status/update\\?status=(.*)$");
			Pattern p_body = Pattern.compile("/status/update");
			Matcher m_uri = p_uri.matcher(target);
			Matcher m_body = p_body.matcher(target);
			String tweet = null;
			if (m_uri.find()) {
				tweet = m_uri.group(1);
			} else if (m_body.find()) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request)
						.getEntity();
				String entityContent = EntityUtils.toString(entity);
				Pattern p = Pattern.compile("status=(.*)$");
				Matcher m = p.matcher(entityContent);
				if (m.find()) {
					tweet = m.group(1);
				}
			} else {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				return;
			}
			
			if (tweet != null) {
				tweet = URLDecoder.decode(tweet,"UTF-8");
				log.info("Pushing tweet to server: " + tweet);
				HttpClient httpclient = new DefaultHttpClient();
				List<NameValuePair> formparams = new ArrayList<NameValuePair>();
				formparams.add(new BasicNameValuePair("t", tweet));
				UrlEncodedFormEntity entity = null;
				try {
					entity = new UrlEncodedFormEntity(formparams, "UTF-8");
				} catch (UnsupportedEncodingException e) {
				}
				HttpPost httppost = new HttpPost("http://"
						+ ServerStateManager.getMyBackendIp() + ":" + ServerStateManager.getMyBackendPort()
						+ "/data/update");
				httppost.setEntity(entity);
				try {
					httpclient.execute(httppost);
				} catch (Exception e) {
					ServerStateManager.removeDeadBackend();
					ServerStateManager.updateSnapshot(ServerStateManager.getMyBackendIp(), ServerStateManager.getMyBackendPort());
					handle(request, response, context);
					return;
				}
				response.setStatusCode(HttpStatus.SC_NO_CONTENT);
			} else {
			}
		} else {
			throw new MethodNotSupportedException(method
					+ " method not supported\n");
		}

	}

}