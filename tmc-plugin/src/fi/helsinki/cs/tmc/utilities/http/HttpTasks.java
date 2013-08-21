package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
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
    
    public CancellableCallable<byte[]> getForBinary(String url) {
        return downloadToBinary(createExecutor(url));
    }
    
    public CancellableCallable<String> getForText(String url) {
        return downloadToText(createExecutor(url));
    }
    
    public CancellableCallable<byte[]> postForBinary(String url, Map<String, String> params) {
        return downloadToBinary(createExecutor(makePostRequest(url, params)));
    }
    
    public CancellableCallable<String> postForText(String url, Map<String, String> params) {
        return downloadToText(createExecutor(makePostRequest(url, params)));
    }
    
    public CancellableCallable<String> uploadFileForTextDownload(String url, Map<String, String> params, String fileField, byte[] data) {
        HttpPost request = makeFileUploadRequest(url, params, fileField, data);
        return downloadToText(createExecutor(request));
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
    
    private HttpPost makePostRequest(String url, Map<String, String> params) {
        HttpPost request = new HttpPost(url);
        
        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
        for (Map.Entry<String, String> param : params.entrySet()) {
            pairs.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, "UTF-8");
            request.setEntity(entity);
            return request;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private HttpPost makeFileUploadRequest(String url, Map<String, String> params, String fileField, byte[] data) {
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
