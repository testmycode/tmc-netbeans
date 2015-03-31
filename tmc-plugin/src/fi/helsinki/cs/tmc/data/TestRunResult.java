package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;
import java.util.List;

public class TestRunResult {
    public static enum Status {
        PASSED,
        COMPILE_FAILED
    }
    
    private final List<TestCaseResult> testCaseResults;
    public final Status status;

    public TestRunResult(List<TestCaseResult> testCaseResults) {
        this.status = Status.PASSED;
        this.testCaseResults = testCaseResults;
    }

    public TestRunResult(Status status) {
        this.status = status;
        this.testCaseResults = new ArrayList<TestCaseResult>();
    }

    public TestRunResult(List<TestCaseResult> testCaseResults, Status status) {
        this.testCaseResults = testCaseResults;
        this.status = status;
    }

    public List<TestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }
}
