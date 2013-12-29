package fi.helsinki.cs.tmc.model;

/**
 * Type for exceptions whose message should be shown to the user.
 */
public class UserVisibleException extends Exception {
    public UserVisibleException(String msg) {
        super(msg);
    }

    public UserVisibleException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
