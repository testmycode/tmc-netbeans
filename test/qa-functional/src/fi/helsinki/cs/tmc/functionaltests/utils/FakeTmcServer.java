package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.UnsupportedEncodingException;
import org.apache.http.entity.StringEntity;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import static org.junit.Assert.*;

public class FakeTmcServer extends AdHocHttpServer {

    private int expectedApiVersion = 1;
    private String expectedUsername;
    private String expectedPassword;
    private String coursesJson = "{}";

    public FakeTmcServer() {
        setHandler(new Handler());
    }
    
    public synchronized FakeTmcServer expectUser(String username, String password) {
        this.expectedUsername = username;
        this.expectedPassword = password;
        return this;
    }
    
    public synchronized void respondWithCourses(String coursesJson) {
        this.coursesJson = coursesJson;
    }
    
    private class Handler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest req, HttpResponse resp, HttpContext hc) throws HttpException, IOException {
            synchronized (FakeTmcServer.this) {
                URI uri;
                try {
                    uri = new URI(req.getRequestLine().getUri());
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Failed to read query parameters: invalid URI: " + req.getRequestLine().getUri());
                }

                Map<String, String> params = parseQueryParameters(uri);
                authenticate(params);

                if (uri.getPath().startsWith("/courses.json")) {
                    debug("Responding with course list: " + coursesJson);
                    respondWithJson(resp, coursesJson);
                }
            }
        }

        private Map<String, String> parseQueryParameters(URI uri) {
            List<NameValuePair> pairs = URLEncodedUtils.parse(uri, "UTF-8");
            HashMap<String, String> map = new HashMap<String, String>();
            for (NameValuePair nvp : pairs) {
                map.put(nvp.getName(), nvp.getValue());
            }
            return map;
        }

        private void authenticate(Map<String, String> params) {
            assertEquals("" + expectedApiVersion, params.get("api_version"));
            if (expectedUsername != null) {
                assertEquals(expectedUsername, params.get("api_username"));
            }
            if (expectedPassword != null) {
                assertEquals(expectedPassword, params.get("api_password"));
            }
        }

        private void respondWithJson(HttpResponse resp, String json) {
            try {
                resp.setEntity(new StringEntity(json, "application/json", "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
