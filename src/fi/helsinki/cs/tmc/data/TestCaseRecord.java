package fi.helsinki.cs.tmc.data;

public class TestCaseRecord {
    private String name;
    private boolean successful;
    private String message;

    public String getName() {
        return name;
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * May be null.
     */
    public String getMessage() {
        return message;
    }
}
