package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;

/**
 * Convenient methods to start asynchronous HTTP tasks.
 * 
 * Tasks throw a {@link FailedHttpResponseException} when getting a response
 * with a non-successful status code.
 */
public class HttpTasks {
    private UsernamePasswordCredentials credentials = null;

    public HttpTasks setCredentials(String username, String password) {
        this.credentials = new UsernamePasswordCredentials(username, password);
        return this;
    }
    
    private HttpRequestExecutor createExecutor(String url) {
        return new HttpRequestExecutor(url).setCredentials(credentials);
    }
    
    private HttpRequestExecutor createExecutor(HttpPost request) {
        return new HttpRequestExecutor(request).setCredentials(credentials);
    }
    
    public CancellableCallable<byte[]> downloadBinaryFile(String url) {
        return downloadToBinary(createExecutor(url));
    }
    
    public CancellableCallable<String> downloadTextFile(String url) {
        return downloadToText(createExecutor(url));
    }

    private CancellableCallable<byte[]> downloadToBinary(final HttpRequestExecutor download) {
        return new CancellableCallable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                return EntityUtils.toByteArray(download.call());
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
    
    private CancellableCallable<String> downloadToText(final HttpRequestExecutor download) {
        return new CancellableCallable<String>() {
            @Override
            public String call() throws Exception {
                return EntityUtils.toString(download.call(), "UTF-8");
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
    
    public CancellableCallable<String> uploadFileForTextDownload(String url, Map<String, String> params, String fileField, byte[] data) {
        HttpPost request = makeFileUploadRequest(url, params, fileField, data);
        return downloadToText(createExecutor(request));
    }

    private HttpPost makeFileUploadRequest(String url, Map<String, String> params, String fileField, byte[] data) throws RuntimeException {
        HttpPost request = new HttpPost(url);
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, String> e : params.entrySet()) {
            try {
                entity.addPart(e.getKey(), new StringBody(e.getValue(), Charset.forName("UTF-8")));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
        entity.addPart(fileField, new ByteArrayBody(data, "file"));
        request.setEntity(entity);
        return request;
    }
}
