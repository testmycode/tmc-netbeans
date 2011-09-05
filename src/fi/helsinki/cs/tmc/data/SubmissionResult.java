package fi.helsinki.cs.tmc.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SubmissionResult {

    public static enum Status {
        OK,
        FAIL,
        ERROR
    }
    
    private Status status;;
    private String error; // e.g. compile error
    private Map<String, List<String>> categorizedTestFailures;
    
    public SubmissionResult() {
        status = Status.ERROR;
        error = null;
        categorizedTestFailures = Collections.emptyMap();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String compileError) {
        this.error = compileError;
    }

    public Map<String, List<String>> getCategorizedTestFailures() {
        return categorizedTestFailures;
    }

    public void setCategorizedTestFailures(Map<String, List<String>> categorizedTestFailures) {
        this.categorizedTestFailures = categorizedTestFailures;
    }
}
