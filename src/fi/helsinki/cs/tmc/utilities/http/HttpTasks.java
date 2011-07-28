package fi.helsinki.cs.tmc.utilities.http;

import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

public class HttpTasks {
    public CancellableCallable<byte[]> downloadBinaryFile(String url) {
        return new HttpRequestExecutor(url);
    }
    
    public CancellableCallable<String> downloadTextFile(String url) {
        HttpRequestExecutor requestExecutor = new HttpRequestExecutor(url);
        return downloadToText(requestExecutor);
    }

    private CancellableCallable<String> downloadToText(final HttpRequestExecutor download) {
        return new CancellableCallable<String>() {
            @Override
            public String call() throws Exception {
                return new String(download.call(), "UTF-8");
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
    
    public CancellableCallable<String> uploadFileForTextResponse(String url, Map<String, String> params, String fileField, byte[] data) {
        HttpPost request = makeFileUploadRequest(url, params, fileField, data);
        HttpRequestExecutor requestExecutor = new HttpRequestExecutor(request);
        return downloadToText(requestExecutor);
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
