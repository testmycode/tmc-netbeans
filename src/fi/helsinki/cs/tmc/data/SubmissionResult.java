package fi.helsinki.cs.tmc.data;

import java.util.Collections;
import java.util.List;

public class SubmissionResult {

    public static enum Status {
        OK,
        FAIL,
        ERROR
    }
    
    private Status status;
    private String error; // e.g. compile error
    private List<TestCaseResult> testCases;
    
    public SubmissionResult() {
        status = Status.ERROR;
        error = null;
        testCases = Collections.emptyList();
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

    public void setError(String error) {
        this.error = error;
    }

    public List<TestCaseResult> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseResult> testCases) {
        this.testCases = testCases;
    }
}
