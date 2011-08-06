package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

/**
 * Downloads a single file over HTTP into memory while being cancellable.
 * 
 * <p>
 * TODO: test with a local HTTP server (httpcore has one).
 */
/*package*/ class HttpRequestExecutor implements CancellableCallable<byte[]> {
    private static final int DEFAULT_TIMEOUT = 3 * 60 * 1000;
    
    private HttpUriRequest request;
    private CookieStore cookieStore;
    
    /*package*/ HttpRequestExecutor(String url) {
        this(new HttpGet(url));
    }
    
    /*package*/ HttpRequestExecutor(HttpUriRequest request) {
        this.request = request;
        this.cookieStore = null;
    }
    
    @Override
    public byte[] call() throws IOException, InterruptedException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        HttpEntity entity = executeRequest();
        entity.writeTo(buf);
        return buf.toByteArray();
    }
    
    private HttpEntity executeRequest() throws IOException, InterruptedException {
        HttpResponse response;
        try {
            HttpParams p = new BasicHttpParams();
            p.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, DEFAULT_TIMEOUT);
            DefaultHttpClient httpClient = new DefaultHttpClient(p);
            if (cookieStore == null) {
                cookieStore = httpClient.getCookieStore();
            } else {
                httpClient.setCookieStore(cookieStore);
            }
        
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
    
    private HttpEntity handleResponse(HttpResponse response) throws IOException, InterruptedException {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 302 || responseCode == 303) {
            // Need to handle redirect manually. Apparently HttpClient
            // won't do it in all cases (multipart POST requests)?
            if (response.getFirstHeader("location") != null) {
                request = new HttpGet(response.getFirstHeader("location").getValue());
                return executeRequest();
            } else {
                throw new IOException("Redirect without a location header");
            }
        } else if (200 <= responseCode && responseCode <= 299) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return entity;
            } else {
                throw new IOException("No content in HTTP response");
            }
        } else {
            throw new IOException("Response code " + responseCode);
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
