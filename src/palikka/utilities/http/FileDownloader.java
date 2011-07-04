package palikka.utilities.http;

import java.io.IOException;
import java.io.InputStream;
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
 * Used to download files from an address. Other classes should never use this
 * but FileDownloaderAsync instead.
 * @author jmturpei
 */
public class FileDownloader {

    private final URI downloadUri;
    /**
     * A reference to the downloaded file is stored here.
     */
    private InputStream fileContent;
    private boolean downloadStarted;
    private int timeout;

    /**
     * Constructor
     * @param downloadAddress 
     * @throws NullPointerException If no server address was given
     * @throws Exception If the server address was invalid
     */
    public FileDownloader(String downloadAddress) throws NullPointerException, Exception {
        if (downloadAddress == null) {
            throw new NullPointerException("server address is null");
        }

        try {
            downloadUri = new URI(downloadAddress);
        } catch (URISyntaxException e) {
            throw new Exception("Server address is invalid");
        }

        this.timeout = 180000;


        downloadStarted = false;
    }

    /**
     * Method sets timeout to FileDownloader object
     * @param timeout 
     */
    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 
     * @return The downloaded file's InputStream
     */
    public synchronized InputStream getFileContent() {
        return fileContent;
    }

    /**
     * 
     * @param address server address
     * @return inputStream to downloaded file. Returns null when no file was found or an error occured.
     * @throws Exception
     */
    public synchronized void download() throws Exception {

        if (downloadStarted) {
            return;
        }
        downloadStarted = true;


        DefaultHttpClient httpClient;
        HttpResponse response;



        try {
            HttpParams p = new BasicHttpParams();
            p.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
            httpClient = new DefaultHttpClient(p);
            response = httpClient.execute(new HttpGet(downloadUri)); // may throw IOException    
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode <= 299 && responseCode >= 200) {

                HttpEntity entity = response.getEntity();
                if (entity != null) {


                    fileContent = entity.getContent();
                } else {
                    throw new Exception("Unable to deliver file");
                }
            }

        } catch (IOException e) {
            throw new Exception("Unable to connect to server.");
        }
    }
}
