package fi.helsinki.cs.tmc.utilities.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

/**
 * This class is used to upload files. It shouldn't be used directly but
 * through FileUploaderAsync instead.
 * 
 * The upload is done with POST method and basically it has a list of files
 * and fields with String values in it.
 * @author jmturpei
 */
public class FileUploader {

    /**
     * This is where we save the response we get.
     */
    private InputStream response;
    /**
     * true if the upload has been started.
     */
    private boolean uploadStarted;
    /**
     * When the upload should be aborted if we can't establish a connection.
     */
    private int timeout;
    /**
     * Store all the text fields and their values here that we want to send
     * with the files.
     */
    private final ConcurrentHashMap<String, String> stringKeyValues;
    /**
     * Maps each filename with a FileEntry.
     * FileEntry is a private class within this class.
     */
    private final ConcurrentHashMap<String, FileEntry> files;
    /**
     * The address of the server where we wish to upload to.
     */
    private final String serverAddress;

    /**
     * Constructor
     * @param serverAddress The target server
     */
    public FileUploader(String serverAddress) {
        if (serverAddress == null) {
            throw new NullPointerException("server address or student id is null");
        }

        timeout = 60000;
        this.serverAddress = serverAddress;
        uploadStarted = false;
        stringKeyValues = new ConcurrentHashMap<String, String>();
        files = new ConcurrentHashMap<String, FileEntry>();
    }

    /**
     * Add a new field with a value to the form being sent.
     * @param key Name of the field
     * @param value Value of the field
     */
    public void AddStringKeyValuePart(String key, String value) {
        stringKeyValues.put(key, value);
    }

    /**
     * Adds a file to this uploader.
     * @param fileContent File in byte[] format
     * @param filename Name of the file
     * @param formKey Key for this file in the form being sent
     */
    public void AddFile(byte[] fileContent, String filename, String formKey) {
        files.put(formKey, new FileEntry((fileContent), filename));
    }

    /**
     * Method sets timeout to Fileuploader object
     * @param timeout 
     */
    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Sends a give file in byte[] format to given address.
     * @param fileContent File to send
     * @param serverAddress The address where to send the file. Must include the protocol (http://).
     * @throws Exception If fails to send file.
     */
    public synchronized void send() throws Exception {

        if (uploadStarted) {
            return;
        }
        uploadStarted = true;

        HttpParams p = new BasicHttpParams();
        p.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        HttpClient httpclient = new DefaultHttpClient(p);
        HttpPost httppost = new HttpPost(serverAddress);



        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

        Enumeration<String> keys = stringKeyValues.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            reqEntity.addPart(new FormBodyPart(key, new StringBody(stringKeyValues.get(key))));

        }


        keys = files.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            FileEntry entry = files.get(key);

            ByteArrayBody bin = new ByteArrayBody(entry.getFileContent(), entry.getFilename());
            reqEntity.addPart(key, bin);

        }


        httppost.setEntity(reqEntity);
        HttpResponse httpResponse;

        try {
            httpResponse = httpclient.execute(httppost);

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode <= 302 && statusCode >= 200) {
            } else {
                throw new Exception("Unable to send file!\n"
                        + "Server returned a message: " + httpResponse.getStatusLine().toString());
            }
        } catch (IOException ioex) {
            throw new Exception("Unable to send file.");
        }


        try {
            this.response = httpResponse.getEntity().getContent();
        } catch (Exception e) {
            throw new Exception("Unable to get results from server");
        }


    }

    /**
     * Used to get the response from the server
     * @return InputStream of the response
     */
    public synchronized InputStream getResponse() {
        return response;
    }

    /**
     * This is used to represent a file.
     */
    private final class FileEntry {

        /**
         * The actual file in byte[] form.
         */
        private final byte[] fileContent;
        
        /**
         * The name of the file.
         */
        private final String filename;

        /**
         * Constructor
         * @param fileContent The actual file
         * @param filename 
         */
        public FileEntry(byte[] fileContent, String filename) {
            if (fileContent == null) {
                throw new NullPointerException("file content is null");
            }
            if (filename == null) {
                throw new NullPointerException("filaname is null");
            }
            if (filename.length() == 0) {
                throw new IllegalArgumentException("filename is empty");
            }


            this.filename = filename;

            byte[] arr = new byte[fileContent.length];
            System.arraycopy(fileContent, 0, arr, 0, fileContent.length);

            this.fileContent = arr;
        }

        /**
         * @return the fileContent
         */
        public byte[] getFileContent() {

            return fileContent;
        }

        /**
         * @return the filename
         */
        public String getFilename() {
            return filename;
        }
    }
}
