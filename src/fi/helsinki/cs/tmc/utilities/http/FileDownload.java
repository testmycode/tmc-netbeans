package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

/**
 * Downloads a single file over HTTP into memory while being cancellable.
 * 
 * <p>
 * TODO: test with a fake HTTP server (httpcore has one).
 */
/*package*/ class FileDownload implements CancellableCallable<byte[]> {
    private static final int DEFAULT_TIMEOUT = 3 * 60 * 1000;
    
    private HttpGet request;
    
    /*package*/ FileDownload(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid URL: '" + url + "'", ex);
        }
        this.request = new HttpGet(uri);
    }

    @Override
    public byte[] call() throws IOException, InterruptedException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        HttpEntity entity = download();
        entity.writeTo(buf);
        return buf.toByteArray();
    }
    
    private HttpEntity download() throws IOException, InterruptedException {
        try {
            HttpParams p = new BasicHttpParams();
            p.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, DEFAULT_TIMEOUT);
            DefaultHttpClient httpClient = new DefaultHttpClient(p);
            HttpResponse response = httpClient.execute(request);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode <= 299 && responseCode >= 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return entity;
                } else {
                    throw new IOException("No content in HTTP response");
                }
            } else {
                throw new IOException("Response code " + responseCode);
            }
        } catch (IOException ex) {
            if (request.isAborted()) {
                throw new InterruptedException();
            } else {
                throw new IOException("Download failed: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * May be called from another thead to cancel an ongoing download.
     */
    public boolean cancel() {
        request.abort();
        return true;
    }
}
