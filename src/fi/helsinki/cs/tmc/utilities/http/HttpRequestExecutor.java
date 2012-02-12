package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 * Downloads a single file over HTTP into memory while being cancellable.
 * 
 * If the response was not a successful one (status code 2xx) then a
 * {@link FailedHttpResponseException} with a preloaded buffered entity is thrown.
 */
/*package*/ class HttpRequestExecutor implements CancellableCallable<BufferedHttpEntity> {
    private static final int DEFAULT_TIMEOUT = 30 * 1000;
    
    private final Object shutdownLock = new Object();
    
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
    public BufferedHttpEntity call() throws IOException, InterruptedException, FailedHttpResponseException {
        HttpClient httpClient = makeHttpClient();
        try {
            return executeRequest(httpClient);
        } finally {
            synchronized (shutdownLock) {
                request = null;
                disposeOfHttpClient(httpClient);
            }
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
    
    private BufferedHttpEntity executeRequest(HttpClient httpClient) throws IOException, InterruptedException, FailedHttpResponseException {
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
        
        return handleResponse(response);
    }
    
    private BufferedHttpEntity handleResponse(HttpResponse response) throws IOException, InterruptedException, FailedHttpResponseException {
        int responseCode = response.getStatusLine().getStatusCode();
        if (response.getEntity() == null) {
            throw new IOException("HTTP " + responseCode + " with no response");
        }
        
        BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
        EntityUtils.consume(entity); // Ensure it's loaded into memory
        if (200 <= responseCode && responseCode <= 299) {
            return entity;
        } else {
            throw new FailedHttpResponseException(responseCode, entity);
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
        synchronized (shutdownLock) {
            if (request != null) {
                request.abort();
            }
        }
        return true;
    }
}
