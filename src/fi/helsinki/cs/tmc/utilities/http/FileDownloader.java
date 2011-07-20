package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.Refactored;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

/**
 * Synchronously downloads files over HTTP.
 * 
 * <p>
 * The download methods are thread-safe as long as settings are not changed
 * while they are running.
 * 
 * <p>
 * TODO: test and ensure cancellability if necessary.
 */
@Refactored
public class FileDownloader {
    private static final int DEFAULT_TIMEOUT = 3 * 60 * 1000;
    
    private int timeout = DEFAULT_TIMEOUT;
    
    /**
     * Sets the timeout of future downloads.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Downloads a file over HTTP.
     */
    public byte[] downloadFile(String url) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        download(url).writeTo(buf);
        return buf.toByteArray();
    }
    
    public String downloadTextFile(String url) throws IOException {
        return new String(downloadFile(url), "UTF-8");
    }
    
    private HttpEntity download(String url) throws IOException {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URL: '" + url + "'", ex);
        }
        
        HttpParams p = new BasicHttpParams();
        p.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        DefaultHttpClient httpClient = new DefaultHttpClient(p);
        HttpResponse response = httpClient.execute(new HttpGet(uri));

        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode <= 299 && responseCode >= 200) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return entity;
            } else {
                throw new IOException("No content in HTTP response");
            }
        } else {
            throw new IOException("Download failed: " + responseCode);
        }
    }
}
