package fi.helsinki.cs.tmc.utilities.http;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class FailedHttpResponseException extends Exception {
    private final int statusCode;
    private final HttpEntity entity;

    public FailedHttpResponseException(int statusCode, HttpEntity entity) {
        super("Response code: " + statusCode);
        this.statusCode = statusCode;
        this.entity = entity;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public HttpEntity getEntity() {
        return entity;
    }

    public String getEntityAsString() {
        try {
            return EntityUtils.toString(entity, "UTF-8");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}