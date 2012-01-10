package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.ObsoleteClientException;
import fi.helsinki.cs.tmc.utilities.http.FailedHttpResponseException;

class DownloadErrorHelper {
    public static String getDownloadExceptionMsg(Throwable t) {
        if (t instanceof FailedHttpResponseException) {
            if (((FailedHttpResponseException)t).getStatusCode() == 403) {
                return "Invalid username or password";
            }
        }

        return t.getMessage();
    }
}
