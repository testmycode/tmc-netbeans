package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.utilities.http.FailedHttpResponseException;

public class ServerErrorHelper {

    public static String getServerExceptionMsg(Throwable t) {
        if (t instanceof FailedHttpResponseException) {
            if (((FailedHttpResponseException)t).getStatusCode() == 401) {
                return "Check your username and password in TMC -> Settings.";
            }
        }
        return t.getMessage();
    }
}