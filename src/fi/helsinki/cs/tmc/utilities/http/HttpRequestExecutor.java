package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 * Downloads a single file over HTTP into memory while being cancellable.
 */
/*package*/ class HttpRequestExecutor implements CancellableCallable<byte[]> {
    private static final int DEFAULT_TIMEOUT = 30 * 1000;
    
    private int timeout = DEFAULT_TIMEOUT;
    private HttpUriRequest request;
    private CookieStore cookieStore;
    private UsernamePasswordCredentials credentials; // May be null
    
    /*package*/ HttpRequestExecutor(String url) {
        this(new HttpGet(url));
    }
    
    /*package*/ HttpRequestExecutor(HttpUriRequest request) {
        this.request = request;
        this.cookieStore = null;
        
        if (request.getURI().getUserInfo() != null) {
            credentials = new UsernamePasswordCredentials(request.getURI().getUserInfo());
            setRequestCredentials();
        }
    }
    
    public HttpRequestExecutor setCredentials(String username, String password) {
        return setCredentials(new UsernamePasswordCredentials(username, password));
    }
    
    public HttpRequestExecutor setCredentials(UsernamePasswordCredentials credentials) {
        this.credentials = credentials;
        setRequestCredentials();
        return this;
    }
    
    public HttpRequestExecutor setTimeout(int timeoutMs) {
        this.timeout = timeoutMs;
        return this;
    }
    
    @Override
    public byte[] call() throws IOException, InterruptedException {
        HttpClient httpClient = makeHttpClient();
        try {
            HttpEntity entity = executeRequest(httpClient);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            entity.writeTo(buf);
            return buf.toByteArray();
        } finally {
            disposeOfHttpClient(httpClient);
        }
    }
    
    private HttpClient makeHttpClient() {
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        params.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
        params.setParameter(AuthPNames.CREDENTIAL_CHARSET, "UTF-8");
        
        HttpClientParams.setRedirecting(params, true);

        DefaultHttpClient httpClient = new DefaultHttpClient(params);
        httpClient.setReuseStrategy(new NoConnectionReuseStrategy());

        if (cookieStore == null) {
            cookieStore = httpClient.getCookieStore();
        } else {
            httpClient.setCookieStore(cookieStore);
        }
        
        return httpClient;
    }
    
    private void disposeOfHttpClient(HttpClient httpClient) {
        httpClient.getConnectionManager().shutdown();
    }
    
    private HttpEntity executeRequest(HttpClient httpClient) throws IOException, InterruptedException {
        HttpResponse response;
        try {
            response = httpClient.execute(request);
        } catch (IOException ex) {
            if (request.isAborted()) {
                throw new InterruptedException();
            } else {
                throw new IOException("Download failed: " + ex.getMessage(), ex);
            }
        }
        
        return handleResponse(httpClient, response);
    }
    
    private HttpEntity handleResponse(HttpClient httpClient, HttpResponse response) throws IOException, InterruptedException {
        int responseCode = response.getStatusLine().getStatusCode();
        if (200 <= responseCode && responseCode <= 299) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return entity;
            } else {
                throw new IOException("No content in HTTP response");
            }
        } else {
            EntityUtils.consume(response.getEntity());
            throw new IOException("Response code " + responseCode);
        }
    }
    
    private void setRequestCredentials() {
        request.removeHeaders("Authorization");
        if (credentials != null) {
            request.addHeader(BasicScheme.authenticate(credentials, "UTF-8", false));
        }
    }
    
    /**
     * May be called from another thread to cancel an ongoing download.
     */
    @Override
    public boolean cancel() {
        request.abort();
        return true;
    }
}
