package fi.helsinki.cs.tmc.data;

import java.util.Collections;
import java.util.List;

public class SubmissionResult {

    public static enum Status {
        OK,
        FAIL,
        ERROR
    }
    
    private Status status;;
    private String error; // e.g. compile error
    private List<String> testFailures;
    
    public SubmissionResult() {
        status = Status.ERROR;
        error = null;
        testFailures = Collections.emptyList();
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

    public List<String> getTestFailures() {
        return testFailures;
    }

    public void setTestFailures(List<String> testFailures) {
        this.testFailures = testFailures;
    }
}
