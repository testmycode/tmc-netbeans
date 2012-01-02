package fi.helsinki.cs.tmc.actions;

import org.apache.http.client.HttpResponseException;

class DownloadErrorHelper {
    public static String getDownloadExceptionMsg(Throwable t) {
        if (t instanceof HttpResponseException) {
            if (((HttpResponseException)t).getStatusCode() == 403) {
                return "Invalid username or password";
            }
        }

        return t.getMessage();
    }
}
