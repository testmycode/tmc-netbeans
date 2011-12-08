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
    private List<TestCaseRecord> testCases;
    
    public SubmissionResult() {
        status = Status.ERROR;
        error = null;
        testCases = Collections.emptyList();
    }

    public Status getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public List<TestCaseRecord> getTestCases() {
        return testCases;
    }
}
