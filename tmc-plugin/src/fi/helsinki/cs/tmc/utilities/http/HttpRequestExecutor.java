package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.IOException;
import java.net.ProxySelector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.openide.util.Lookup;

/**
 * Downloads a single file over HTTP into memory while being cancellable.
 *
 * If the response was not a successful one (status code 2xx) then a
 * {@link FailedHttpResponseException} with a preloaded buffered entity is
 * thrown.
 */
/*package*/ class HttpRequestExecutor implements CancellableCallable<BufferedHttpEntity> {

    private static final int DEFAULT_TIMEOUT = 30 * 1000;

    private final Object shutdownLock = new Object();

    private int timeout = DEFAULT_TIMEOUT;
    private HttpUriRequest request;
    private static final Logger log = Logger.getLogger(HttpRequestExecutor.class.getName());

    private UsernamePasswordCredentials credentials; // May be null
    private CredentialsProvider credsProvider;

    /*package*/ HttpRequestExecutor(String url) {
        this(new HttpGet(url));
    }

    /*package*/ HttpRequestExecutor(HttpUriRequest request) {
        this.request = request;
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
        CloseableHttpClient httpClient = makeHttpClient();

        try {
            return executeRequest(httpClient);
        } finally {
            synchronized (shutdownLock) {
                request = null;
                disposeOfHttpClient(httpClient);
            }
        }
    }

    private CloseableHttpClient makeHttpClient() throws IOException {
        ProxySelector proxys = Lookup.getDefault().lookup((ProxySelector.class));
        SystemDefaultRoutePlanner systemDefaultRoutePlanner = new SystemDefaultRoutePlanner(proxys);

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .useSystemProperties()
                .setRoutePlanner(systemDefaultRoutePlanner)
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy());

        CloseableHttpClient httpClient = httpClientBuilder.build();

        return httpClient;
    }

    private void disposeOfHttpClient(CloseableHttpClient httpClient) {
        try {
            httpClient.close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "dispose of httpClient failed {0}", ex);
        }
    }

    private BufferedHttpEntity executeRequest(HttpClient httpClient) throws IOException, InterruptedException, FailedHttpResponseException {

        HttpResponse response = null;

        try {
            // Dirty trick
            request.addHeader(new BasicScheme().authenticate(this.credentials, request));
            response = httpClient.execute(request);
        } catch (IOException ex) {
            log.log(Level.INFO, "catchissä");
            if (request.isAborted()) {
                throw new InterruptedException();
            } else {
                throw new IOException("Download failed: " + ex.getMessage(), ex);
            }
        } catch (AuthenticationException ex) {
            log.log(Level.INFO, "AUTH FAILED{0}", ex);
            throw new InterruptedException();
        }

        return handleResponse(response);
    }

    private BufferedHttpEntity handleResponse(HttpResponse response) throws IOException, InterruptedException, FailedHttpResponseException {
        int responseCode = response.getStatusLine().getStatusCode();
        log.log(Level.INFO, "responce:{0}", responseCode);
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

    // NOT REALLY WORKING...
    private void setRequestCredentials() {
        this.credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                AuthScope.ANY, // Would be wise to restrict to TMC server?
                this.credentials);

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
